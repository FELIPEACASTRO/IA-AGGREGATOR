/** @jest-environment node */

jest.mock('@/server/codex/http', () => ({
  requireCodexContext: jest.fn(),
  ok: jest.fn((data: unknown) => ({ success: true, data })),
  fail: jest.fn((message: string, status: number, details?: unknown) => ({
    success: false,
    message,
    status,
    details,
  })),
}));

jest.mock('@/server/codex/db', () => ({
  codexDb: {
    projectContext: { findMany: jest.fn() },
    costLedgerEntry: { groupBy: jest.fn() },
  },
}));

jest.mock('@/server/codex/projects', () => ({
  resolveProjectContext: jest.fn(),
}));

import { requireCodexContext, ok, fail } from '@/server/codex/http';
import { codexDb } from '@/server/codex/db';
import { resolveProjectContext } from '@/server/codex/projects';
import { GET, POST } from '@/app/api/projects/route';

type ProjectsGetResponse = {
  success: boolean;
  data: Array<{
    economics: {
      amount: number;
      quantity: number;
    };
  }>;
};

type ProjectsPostErrorResponse = {
  success: boolean;
  status: number;
};

describe('api/projects routes', () => {
  const requireContextMock = requireCodexContext as unknown as jest.Mock;
  const okMock = ok as unknown as jest.Mock;
  const failMock = fail as unknown as jest.Mock;
  const resolveProjectMock = resolveProjectContext as unknown as jest.Mock;
  const db = codexDb as unknown as {
    projectContext: { findMany: jest.Mock };
    costLedgerEntry: { groupBy: jest.Mock };
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('GET returns projects with non-null economics', async () => {
    requireContextMock.mockResolvedValue({
      session: { userId: 'u1' },
      context: { workspace: { id: 'ws1' }, repository: { id: 'repo-1' } },
    });

    db.projectContext.findMany.mockResolvedValue([
      {
        id: 'proj-1',
        name: 'Project 1',
        slug: 'project-1',
        status: 'ACTIVE',
        repository: { id: 'repo-1', fullName: 'org/repo', defaultBranch: 'main' },
        _count: { tasks: 2 },
      },
      {
        id: 'proj-2',
        name: 'Project 2',
        slug: 'project-2',
        status: 'ACTIVE',
        repository: null,
        _count: { tasks: 0 },
      },
    ]);
    db.costLedgerEntry.groupBy.mockResolvedValue([
      {
        projectContextId: 'proj-1',
        _sum: { amount: 12.5, quantity: 2048 },
      },
    ]);

    const response = (await GET()) as unknown as ProjectsGetResponse;

    expect(okMock).toHaveBeenCalledTimes(1);
    expect(response.success).toBe(true);
    expect(response.data).toHaveLength(2);
    expect(response.data[0].economics).toEqual({ amount: 12.5, quantity: 2048 });
    expect(response.data[1].economics).toEqual({ amount: 0, quantity: 0 });
  });

  it('POST returns 400 for invalid payload', async () => {
    requireContextMock.mockResolvedValue({
      session: { userId: 'u1' },
      context: { workspace: { id: 'ws1' }, repository: { id: 'repo-1' } },
    });

    const response = (await POST(
      new Request('http://localhost/api/projects', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: 'x' }),
      })
    )) as unknown as ProjectsPostErrorResponse;

    expect(failMock).toHaveBeenCalled();
    expect(response).toMatchObject({
      success: false,
      status: 400,
    });
    expect(resolveProjectMock).not.toHaveBeenCalled();
  });
});
