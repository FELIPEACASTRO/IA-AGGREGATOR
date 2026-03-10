import { TaskMode, TaskStatus } from '@prisma/client';
import { z } from 'zod';
import { codexDb } from '@/server/codex/db';
import { enqueueTask } from '@/server/codex/queue';
import { fail, ok, requireCodexContext } from '@/server/codex/http';
import { appendTaskEvent } from '@/server/codex/events';
import { resolveProjectContext } from '@/server/codex/projects';

export const runtime = 'nodejs';

const createTaskSchema = z.object({
  prompt: z.string().min(3),
  mode: z.nativeEnum(TaskMode),
  repositoryId: z.string().optional(),
  environmentId: z.string().optional(),
  baseBranch: z.string().optional(),
  bestOfN: z.number().int().min(1).max(5).optional(),
  sourceRef: z.string().optional(),
  projectContextId: z.string().optional(),
  projectName: z.string().optional(),
  projectSlug: z.string().optional(),
  projectDescription: z.string().optional(),
  imageInputs: z.array(z.string()).default([]),
  attachments: z.array(z.string()).default([]),
  voiceTranscript: z.string().optional(),
});

export async function GET(request: Request) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;

  const url = new URL(request.url);
  const status = url.searchParams.get('status');
  const tab = url.searchParams.get('tab');
  const mode = url.searchParams.get('mode') as TaskMode | null;
  const repositoryId = url.searchParams.get('repositoryId');
  const environmentId = url.searchParams.get('environmentId');

  const where: {
    workspaceId: string;
    status?: TaskStatus;
    mode?: TaskMode;
    repositoryId?: string;
    environmentId?: string;
    archivedAt?: null | { not: null };
  } = {
    workspaceId: context.context.workspace.id,
  };

  if (status) where.status = status as TaskStatus;
  if (mode) where.mode = mode;
  if (repositoryId) where.repositoryId = repositoryId;
  if (environmentId) where.environmentId = environmentId;
  if (tab === 'archived') where.archivedAt = { not: null };
  if (tab !== 'archived') where.archivedAt = null;

  const tasks = await codexDb.task.findMany({
    where,
    orderBy: {
      updatedAt: 'desc',
    },
    include: {
      repository: {
        select: { id: true, fullName: true, defaultBranch: true },
      },
      environment: {
        select: { id: true, name: true, internetMode: true },
      },
      projectContext: {
        select: { id: true, slug: true, name: true, status: true },
      },
      pullRequest: {
        select: { id: true, status: true, url: true },
      },
    },
    take: 200,
  });

  return ok(tasks);
}

export async function POST(request: Request) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;

  const payload = await request.json();
  const parsed = createTaskSchema.safeParse(payload);
  if (!parsed.success) {
    return fail('Payload invalido para criacao de task', 400, parsed.error.flatten());
  }

  const {
    prompt,
    mode,
    repositoryId,
    environmentId,
    baseBranch,
    bestOfN = 1,
    sourceRef,
    projectContextId,
    projectName,
    projectSlug,
    projectDescription,
    imageInputs,
    attachments,
    voiceTranscript,
  } = parsed.data;

  const fallbackRepositoryId = repositoryId || context.context.repository.id;
  const fallbackEnvironmentId = environmentId || context.context.environment.id;
  const title = prompt.slice(0, 80);
  let projectContext = context.context.projectContext;

  try {
    const shouldResolveCustomProject =
      Boolean(projectContextId || projectName || projectSlug || projectDescription)
      || fallbackRepositoryId !== context.context.repository.id;

    if (shouldResolveCustomProject) {
      projectContext = await resolveProjectContext({
        workspaceId: context.context.workspace.id,
        repositoryId: fallbackRepositoryId,
        ownerUserId: context.session.userId,
        projectContextId,
        projectName,
        projectSlug,
        projectDescription,
        sourceRef,
      });
    }
  } catch (error) {
    return fail(error instanceof Error ? error.message : 'Falha ao resolver projeto da task', 400);
  }

  const task = await codexDb.task.create({
    data: {
      workspaceId: context.context.workspace.id,
      repositoryId: fallbackRepositoryId,
      environmentId: fallbackEnvironmentId,
      projectContextId: projectContext?.id,
      createdById: context.session.userId,
      title,
      prompt,
      mode,
      status: 'queued',
      baseBranch: baseBranch || context.context.repository.defaultBranch,
      bestOfN,
      sourceRef,
      internetMode: context.context.environment.internetMode,
      resultBranch: `codex/${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
      input: {
        create: {
          attachments,
          imageInputs,
          voiceTranscript,
        },
      },
    },
  });

  await appendTaskEvent({
    taskId: task.id,
    eventType: 'task.created',
    status: 'queued',
    message: 'Task criada e pronta para execucao',
    metadata: {
      mode: task.mode,
      bestOfN,
      projectContextId: projectContext?.id ?? null,
    },
  });

  await enqueueTask(task.id);

  return ok(task, 201);
}

