import { fail, ok } from '@/server/codex/http';
import { codexDb } from '@/server/codex/db';
import { getResolvedSessionContext } from '@/server/codex/session-context';
import type { SearchResponse, SearchResultItem } from '@/lib/contracts/platform';

export const runtime = 'nodejs';

export async function GET(request: Request) {
  const resolved = await getResolvedSessionContext();
  if (!resolved) return fail('Sessao nao encontrada', 401);

  const url = new URL(request.url);
  const query = url.searchParams.get('q')?.trim() ?? '';

  if (query.length < 2) {
    const payload: SearchResponse = { query, items: [] };
    return ok(payload);
  }

  const workspaceId = resolved.context.workspace.id;
  const contains = {
    contains: query,
    mode: 'insensitive' as const,
  };

  const [tasks, repositories, environments, settings, conversations, prompts] = await Promise.all([
    codexDb.task.findMany({
      where: {
        workspaceId,
        OR: [{ title: contains }, { prompt: contains }],
      },
      take: 5,
      orderBy: { updatedAt: 'desc' },
    }),
    codexDb.gitRepository.findMany({
      where: {
        workspaceId,
        fullName: contains,
      },
      take: 5,
      orderBy: { updatedAt: 'desc' },
    }),
    codexDb.environment.findMany({
      where: {
        workspaceId,
        OR: [{ name: contains }, { description: contains }],
      },
      take: 5,
      orderBy: { updatedAt: 'desc' },
    }),
    codexDb.managedConfig.findMany({
      where: {
        workspaceId,
        configKey: contains,
      },
      take: 5,
      orderBy: { updatedAt: 'desc' },
    }),
    codexDb.chatConversation.findMany({
      where: {
        workspaceId,
        archivedAt: null,
        OR: [{ title: contains }, { messages: { some: { content: contains } } }],
        participants: {
          some: {
            userId: resolved.session.userId,
          },
        },
      },
      take: 5,
      orderBy: [{ pinned: 'desc' }, { updatedAt: 'desc' }],
    }),
    codexDb.promptTemplate.findMany({
      where: {
        isActive: true,
        OR: [
          { scope: 'SYSTEM', OR: [{ title: contains }, { description: contains }, { prompt: contains }] },
          { scope: 'WORKSPACE', workspaceId, OR: [{ title: contains }, { description: contains }, { prompt: contains }] },
        ],
      },
      take: 5,
      orderBy: [{ scope: 'asc' }, { title: 'asc' }],
    }),
  ]);

  const items: SearchResultItem[] = [
    {
      id: resolved.context.workspace.id,
      kind: 'workspace',
      title: resolved.context.workspace.name,
      description: `Workspace atual (${resolved.context.currentMembership.role})`,
      href: '/codex',
    },
    ...tasks.map((task) => ({
      id: task.id,
      kind: 'task' as const,
      title: task.title,
      description: task.status,
      href: `/codex/tasks/${task.id}`,
      metadata: {
        mode: task.mode,
      },
    })),
    ...repositories.map((repository) => ({
      id: repository.id,
      kind: 'repository' as const,
      title: repository.fullName,
      description: repository.defaultBranch,
      href: '/codex',
      metadata: {
        provider: repository.provider,
      },
    })),
    ...environments.map((environment) => ({
      id: environment.id,
      kind: 'environment' as const,
      title: environment.name,
      description: environment.description ?? environment.internetMode,
      href: `/codex/settings/environments/${environment.id}`,
      metadata: {
        internetMode: environment.internetMode,
      },
    })),
    ...settings.map((setting) => ({
      id: setting.id,
      kind: 'setting' as const,
      title: setting.configKey,
      description: 'Managed config',
      href: '/codex/settings/managed-configs',
    })),
    ...conversations.map((conversation) => ({
      id: conversation.id,
      kind: 'conversation' as const,
      title: conversation.title,
      description: conversation.model,
      href: `/chat?conversationId=${conversation.id}`,
      metadata: {
        pinned: conversation.pinned,
      },
    })),
    ...prompts.map((prompt) => ({
      id: prompt.id,
      kind: 'prompt' as const,
      title: prompt.title,
      description: prompt.description,
      href: '/prompts',
      metadata: {
        category: prompt.category,
        scope: prompt.scope,
      },
    })),
  ];

  const payload: SearchResponse = {
    query,
    items,
  };

  return ok(payload);
}
