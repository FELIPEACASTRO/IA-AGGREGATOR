import { codexDb } from '@/server/codex/db';
import { appendTaskEvent } from '@/server/codex/events';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

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
      status: 'completed',
      archivedAt: null,
    },
  });

  await appendTaskEvent({
    taskId: task.id,
    eventType: 'task.unarchived',
    status: 'completed',
    message: 'Task restaurada do arquivo',
  });

  return ok({ taskId: task.id, status: 'completed' });
}

