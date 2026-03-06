import { fail, ok, requireCodexContext } from '@/server/codex/http';
import { loadTaskSummary } from '@/server/codex/task-runner';

export const runtime = 'nodejs';

export async function GET(_: Request, { params }: { params: Promise<{ id: string }> }) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;

  const { id } = await params;
  const task = await loadTaskSummary(id);
  if (!task || task.workspaceId !== context.context.workspace.id) {
    return fail('Task nao encontrada', 404);
  }

  return ok(task);
}
