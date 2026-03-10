/** @jest-environment node */

jest.mock('@/server/codex/http', () => ({
  requireCodexContext: jest.fn(),
  ok: jest.fn((data: unknown) => ({ success: true, data })),
}));

jest.mock('@/server/codex/db', () => ({
  codexDb: {
    usageEntry: { findMany: jest.fn(), groupBy: jest.fn() },
    task: { groupBy: jest.fn() },
    costLedgerEntry: { groupBy: jest.fn(), findMany: jest.fn() },
    routeDecision: { groupBy: jest.fn() },
    modelRun: { groupBy: jest.fn() },
    creditBalance: { findUnique: jest.fn() },
  },
}));

import { requireCodexContext, ok } from '@/server/codex/http';
import { codexDb } from '@/server/codex/db';
import { GET } from '@/app/api/usage/route';

type UsageRouteResponse = {
  success: boolean;
  data: {
    entries: unknown[];
    taskUsage: unknown[];
    repositoryUsage: unknown[];
    userUsage: unknown[];
    projectUsage: unknown[];
    routeUsage: Array<{ serviceClass: string }>;
    modelUsage: unknown[];
    costSummary: unknown[];
    recentCosts: unknown[];
  };
};

describe('GET /api/usage', () => {
  const requireContextMock = requireCodexContext as unknown as jest.Mock;
  const okMock = ok as unknown as jest.Mock;
  const db = codexDb as unknown as {
    usageEntry: { findMany: jest.Mock; groupBy: jest.Mock };
    task: { groupBy: jest.Mock };
    costLedgerEntry: { groupBy: jest.Mock; findMany: jest.Mock };
    routeDecision: { groupBy: jest.Mock };
    modelRun: { groupBy: jest.Mock };
    creditBalance: { findUnique: jest.Mock };
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('returns non-null usage aggregations for a workspace', async () => {
    requireContextMock.mockResolvedValue({
      session: { userId: 'u1' },
      context: { workspace: { id: 'ws1' } },
    });

    db.usageEntry.findMany.mockResolvedValue([{ id: 'entry-1', amount: 10 }]);
    db.usageEntry.groupBy
      .mockResolvedValueOnce([{ taskId: 'task-1', _sum: { amount: 10 } }])
      .mockResolvedValueOnce([{ repositoryId: 'repo-1', _sum: { amount: 10 } }]);
    db.task.groupBy.mockResolvedValue([{ createdById: 'u1', _count: { id: 1 } }]);
    db.costLedgerEntry.groupBy
      .mockResolvedValueOnce([{ projectContextId: 'proj-1', _sum: { amount: 5, quantity: 100 } }])
      .mockResolvedValueOnce([{ category: 'MODEL_EXECUTION', currency: 'USD', _sum: { amount: 5, quantity: 100 } }]);
    db.routeDecision.groupBy.mockResolvedValue([
      { serviceClass: 'BALANCED', _count: { serviceClass: 1 }, _sum: { estimatedCost: 2 } },
    ]);
    db.modelRun.groupBy.mockResolvedValue([
      { provider: 'openai', model: 'gpt-4.1', _count: { model: 1 }, _sum: { inputTokens: 10, outputTokens: 20, actualCost: 3 } },
    ]);
    db.costLedgerEntry.findMany.mockResolvedValue([{ id: 'cost-1', amount: 5 }]);
    db.creditBalance.findUnique.mockResolvedValue({ workspaceId: 'ws1', balance: 20 });

    const response = (await GET()) as unknown as UsageRouteResponse;

    expect(okMock).toHaveBeenCalledTimes(1);
    expect(response).toMatchObject({
      success: true,
      data: expect.objectContaining({
        entries: expect.any(Array),
        taskUsage: expect.any(Array),
        repositoryUsage: expect.any(Array),
        userUsage: expect.any(Array),
        projectUsage: expect.any(Array),
        routeUsage: expect.any(Array),
        modelUsage: expect.any(Array),
        costSummary: expect.any(Array),
        recentCosts: expect.any(Array),
      }),
    });
    expect(response.data.entries).toHaveLength(1);
    expect(response.data.routeUsage[0].serviceClass).toBe('BALANCED');
  });
});
