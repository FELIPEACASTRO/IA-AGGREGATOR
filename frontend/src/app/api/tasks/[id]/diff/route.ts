import { codexDb } from '@/server/codex/db';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET(_: Request, { params }: { params: Promise<{ id: string }> }) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const { id } = await params;

  const task = await codexDb.task.findUnique({
    where: { id },
    include: {
      diffSnapshot: {
        include: {
          files: {
            include: {
              hunks: true,
            },
          },
        },
      },
    },
  });
  if (!task || task.workspaceId !== context.context.workspace.id) {
    return fail('Task nao encontrada', 404);
  }

  return ok(task.diffSnapshot);
}

