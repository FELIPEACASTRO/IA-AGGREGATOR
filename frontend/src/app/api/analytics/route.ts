import { startOfDay, subDays } from 'date-fns';
import { codexDb } from '@/server/codex/db';
import { ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET() {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const workspaceId = context.context.workspace.id;
  const since = startOfDay(subDays(new Date(), 30));

  const [taskCounts, reviewFindings, usage, routing, costs, precomputed] = await Promise.all([
    codexDb.task.groupBy({
      by: ['status'],
      where: { workspaceId, createdAt: { gte: since } },
      _count: {
        status: true,
      },
    }),
    codexDb.reviewFinding.groupBy({
      by: ['severity'],
      _count: {
        severity: true,
      },
    }),
    codexDb.usageEntry.groupBy({
      by: ['metric'],
      where: { workspaceId, createdAt: { gte: since } },
      _sum: { amount: true },
    }),
    codexDb.routeDecision.groupBy({
      by: ['serviceClass'],
      where: { workspaceId, createdAt: { gte: since } },
      _count: { serviceClass: true },
      _sum: { estimatedCost: true },
    }),
    codexDb.costLedgerEntry.groupBy({
      by: ['category'],
      where: { workspaceId, createdAt: { gte: since } },
      _sum: { amount: true, quantity: true },
    }),
    codexDb.analyticsAggregate.findMany({
      where: { workspaceId },
      orderBy: { metricDate: 'desc' },
      take: 90,
    }),
  ]);

  return ok({
    taskCounts,
    reviewFindings,
    usage,
    routing,
    costs,
    precomputed,
  });
}

