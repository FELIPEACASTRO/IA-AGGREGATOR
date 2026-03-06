import { codexDb } from '@/server/codex/db';
import { ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET() {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const workspaceId = context.context.workspace.id;

  const [entries, taskUsage, repositoryUsage, creditBalance] = await Promise.all([
    codexDb.usageEntry.findMany({
      where: { workspaceId },
      orderBy: { createdAt: 'desc' },
      take: 200,
    }),
    codexDb.usageEntry.groupBy({
      by: ['taskId'],
      where: {
        workspaceId,
      },
      _sum: {
        amount: true,
      },
      orderBy: {
        _sum: {
          amount: 'desc',
        },
      },
      take: 20,
    }),
    codexDb.usageEntry.groupBy({
      by: ['repositoryId'],
      where: {
        workspaceId,
      },
      _sum: {
        amount: true,
      },
      orderBy: {
        _sum: {
          amount: 'desc',
        },
      },
      take: 20,
    }),
    codexDb.creditBalance.findUnique({
      where: { workspaceId },
    }),
  ]);

  return ok({
    entries,
    taskUsage,
    repositoryUsage,
    creditBalance,
  });
}

