import { codexDb } from '@/server/codex/db';
import { enqueueTask } from '@/server/codex/queue';
import { ok } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function POST(request: Request) {
  const payload = await request.json().catch(() => ({}));
  const workspace = await codexDb.workspace.findFirst({
    orderBy: { createdAt: 'asc' },
  });
  if (!workspace) return ok({ received: true, ignored: true });

  await codexDb.slackEventLog.create({
    data: {
      workspaceId: workspace.id,
      eventType: payload?.type ?? 'unknown',
      rawPayload: payload,
      status: 'received',
    },
  });

  const text = String(payload?.event?.text ?? '');
  const isMention = text.includes('@agent') || text.includes('@codex');
  if (isMention) {
    const repository = await codexDb.gitRepository.findFirst({
      where: { workspaceId: workspace.id },
      orderBy: { updatedAt: 'desc' },
    });
    const environment = await codexDb.environment.findFirst({
      where: { workspaceId: workspace.id },
      orderBy: { updatedAt: 'desc' },
    });
    const owner = await codexDb.membership.findFirst({
      where: { workspaceId: workspace.id },
      orderBy: { createdAt: 'asc' },
    });

    if (repository && environment && owner) {
      const task = await codexDb.task.create({
        data: {
          workspaceId: workspace.id,
          repositoryId: repository.id,
          environmentId: environment.id,
          createdById: owner.userId,
          title: text.slice(0, 80) || 'Slack task',
          prompt: text,
          mode: text.toLowerCase().includes('code') ? 'CODE' : 'ASK',
          status: 'queued',
          baseBranch: repository.defaultBranch,
          resultBranch: `codex/slack-${Date.now()}`,
          internetMode: environment.internetMode,
          sourceRef: `slack:${payload?.event?.channel ?? 'unknown'}`,
          input: {
            create: {
              attachments: [],
              imageInputs: [],
            },
          },
        },
      });
      await enqueueTask(task.id);
      return ok({ received: true, taskId: task.id });
    }
  }

  return ok({ received: true, taskCreated: false });
}

