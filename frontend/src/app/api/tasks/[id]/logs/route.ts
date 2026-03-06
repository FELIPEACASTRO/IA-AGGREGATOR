import { LogPhase } from '@prisma/client';
import { codexDb } from '@/server/codex/db';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const { id } = await params;
  const url = new URL(request.url);
  const phase = url.searchParams.get('phase');

  const task = await codexDb.task.findUnique({
    where: { id },
    select: { id: true, workspaceId: true },
  });
  if (!task || task.workspaceId !== context.context.workspace.id) {
    return fail('Task nao encontrada', 404);
  }

  const logs = await codexDb.taskLogChunk.findMany({
    where: {
      taskId: id,
      ...(phase ? { phase: phase as LogPhase } : {}),
    },
    orderBy: [{ createdAt: 'asc' }, { lineNumber: 'asc' }],
    take: 5000,
  });

  return ok(logs);
}
