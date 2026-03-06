import { codexDb } from '@/server/codex/db';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET(_: Request, { params }: { params: Promise<{ id: string }> }) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const { id } = await params;

  const task = await codexDb.task.findUnique({
    where: { id },
    select: {
      id: true,
      workspaceId: true,
      status: true,
      logs: {
        where: {
          phase: 'validation',
        },
        orderBy: [{ createdAt: 'asc' }, { lineNumber: 'asc' }],
      },
    },
  });
  if (!task || task.workspaceId !== context.context.workspace.id) {
    return fail('Task nao encontrada', 404);
  }

  const summary = {
    lint: task.logs.some((line) => line.line.includes('lint')) ? 'executed' : 'not-detected',
    typecheck: task.logs.some((line) => line.line.includes('type')) ? 'executed' : 'not-detected',
    tests: task.status === 'failed' ? 'failed' : 'passed',
  };

  return ok({
    summary,
    logs: task.logs,
  });
}

