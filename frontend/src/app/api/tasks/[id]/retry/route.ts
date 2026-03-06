import { codexDb } from '@/server/codex/db';
import { appendTaskEvent, setTaskStatus } from '@/server/codex/events';
import { fail, ok, requireCodexContext } from '@/server/codex/http';
import { enqueueTask } from '@/server/codex/queue';

export const runtime = 'nodejs';

export async function POST(_: Request, { params }: { params: Promise<{ id: string }> }) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const { id } = await params;
  const task = await codexDb.task.findUnique({ where: { id } });
  if (!task || task.workspaceId !== context.context.workspace.id) {
    return fail('Task nao encontrada', 404);
  }

  await codexDb.task.update({
    where: { id: task.id },
    data: {
      status: 'queued',
      errorMessage: null,
      failedAt: null,
      archivedAt: null,
      completedAt: null,
      attemptIndex: (task.attemptIndex || 1) + 1,
    },
  });
  await setTaskStatus(task.id, 'queued');
  await appendTaskEvent({
    taskId: task.id,
    eventType: 'task.retry',
    status: 'queued',
    message: 'Retry solicitado pelo usuario',
  });

  await enqueueTask(task.id);
  return ok({ taskId: task.id, status: 'queued' });
}

