import { codexDb } from '@/server/codex/db';
import { ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET() {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const workspaceId = context.context.workspace.id;

  const [entries, taskUsage, repositoryUsage, userUsage, projectUsage, routeUsage, modelUsage, costSummary, recentCosts, creditBalance] = await Promise.all([
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
    codexDb.task.groupBy({
      by: ['createdById'],
      where: {
        workspaceId,
      },
      _count: {
        id: true,
      },
      orderBy: {
        _count: {
          id: 'desc',
        },
      },
      take: 20,
    }),
    codexDb.costLedgerEntry.groupBy({
      by: ['projectContextId'],
      where: {
        workspaceId,
        projectContextId: { not: null },
      },
      _sum: {
        amount: true,
        quantity: true,
      },
      orderBy: {
        _sum: {
          amount: 'desc',
        },
      },
      take: 20,
    }),
    codexDb.routeDecision.groupBy({
      by: ['serviceClass'],
      where: {
        workspaceId,
      },
      _count: {
        serviceClass: true,
      },
      _sum: {
        estimatedCost: true,
      },
      orderBy: {
        _count: {
          serviceClass: 'desc',
        },
      },
    }),
    codexDb.modelRun.groupBy({
      by: ['provider', 'model'],
      where: {
        workspaceId,
      },
      _count: {
        model: true,
      },
      _sum: {
        inputTokens: true,
        outputTokens: true,
        actualCost: true,
      },
      orderBy: {
        _sum: {
          actualCost: 'desc',
        },
      },
      take: 20,
    }),
    codexDb.costLedgerEntry.groupBy({
      by: ['category', 'currency'],
      where: {
        workspaceId,
      },
      _sum: {
        amount: true,
        quantity: true,
      },
      orderBy: {
        _sum: {
          amount: 'desc',
        },
      },
    }),
    codexDb.costLedgerEntry.findMany({
      where: { workspaceId },
      orderBy: { createdAt: 'desc' },
      take: 100,
    }),
    codexDb.creditBalance.findUnique({
      where: { workspaceId },
    }),
  ]);

  return ok({
    entries,
    taskUsage,
    repositoryUsage,
    userUsage,
    projectUsage,
    routeUsage,
    modelUsage,
    costSummary,
    recentCosts,
    creditBalance,
  });
}

