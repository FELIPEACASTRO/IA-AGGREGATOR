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

  const eventType = String(payload?.action ?? payload?.type ?? 'unknown');
  await codexDb.linearEventLog.create({
    data: {
      workspaceId: workspace.id,
      eventType,
      rawPayload: payload,
      status: 'received',
    },
  });

  const issueTitle = String(payload?.data?.title ?? '');
  const commentBody = String(payload?.data?.body ?? '');
  const shouldDelegate =
    issueTitle.toLowerCase().includes('@agent') ||
    commentBody.toLowerCase().includes('@agent') ||
    eventType.toLowerCase().includes('assigned');

  if (shouldDelegate) {
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
      const prompt = commentBody || issueTitle || 'Linear delegated task';
      const task = await codexDb.task.create({
        data: {
          workspaceId: workspace.id,
          repositoryId: repository.id,
          environmentId: environment.id,
          createdById: owner.userId,
          title: prompt.slice(0, 80),
          prompt,
          mode: prompt.toLowerCase().includes('code') ? 'CODE' : 'ASK',
          status: 'queued',
          baseBranch: repository.defaultBranch,
          resultBranch: `codex/linear-${Date.now()}`,
          internetMode: environment.internetMode,
          sourceRef: `linear:${payload?.data?.identifier ?? 'unknown'}`,
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

