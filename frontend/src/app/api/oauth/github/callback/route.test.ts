/** @jest-environment node */

jest.mock('next/headers', () => ({
  cookies: jest.fn(),
}));

jest.mock('@/server/codex/auth', () => ({
  getServerSession: jest.fn(),
}));

jest.mock('@/server/codex/seed', () => ({
  ensureWorkspaceForUser: jest.fn(),
}));

jest.mock('@/server/codex/db', () => ({
  codexDb: {
    gitHubInstallation: { upsert: jest.fn() },
    oAuthConnection: { upsert: jest.fn() },
  },
}));

import { cookies } from 'next/headers';
import { getServerSession } from '@/server/codex/auth';
import { ensureWorkspaceForUser } from '@/server/codex/seed';
import { codexDb } from '@/server/codex/db';
import { GET } from '@/app/api/oauth/github/callback/route';

describe('GET /api/oauth/github/callback', () => {
  const cookiesMock = cookies as unknown as jest.Mock;
  const sessionMock = getServerSession as unknown as jest.Mock;
  const workspaceMock = ensureWorkspaceForUser as unknown as jest.Mock;
  const db = codexDb as unknown as {
    gitHubInstallation: { upsert: jest.Mock };
    oAuthConnection: { upsert: jest.Mock };
  };

  beforeEach(() => {
    jest.clearAllMocks();
    sessionMock.mockResolvedValue({
      userId: 'user-1',
      email: 'user@example.com',
      name: 'User',
    });
    workspaceMock.mockResolvedValue({
      workspace: { id: 'workspace-1' },
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('rejects callback when state does not match', async () => {
    cookiesMock.mockResolvedValue({
      get: (name: string) => {
        if (name === 'github_oauth_state') return { value: 'expected-state' };
        return undefined;
      },
    });
    const fetchMock = jest.spyOn(global, 'fetch');

    const response = await GET(
      new Request('http://localhost/api/oauth/github/callback?state=invalid-state&code=abc123')
    );

    expect(response.status).toBe(307);
    expect(response.headers.get('location')).toContain('github=invalid_state');
    expect(fetchMock).not.toHaveBeenCalled();
    expect(db.gitHubInstallation.upsert).not.toHaveBeenCalled();
    expect(db.oAuthConnection.upsert).not.toHaveBeenCalled();
  });

  it('rejects callback when code is missing even with valid state', async () => {
    cookiesMock.mockResolvedValue({
      get: (name: string) => {
        if (name === 'github_oauth_state') return { value: 'valid-state' };
        return undefined;
      },
    });
    const fetchMock = jest.spyOn(global, 'fetch');

    const response = await GET(
      new Request('http://localhost/api/oauth/github/callback?state=valid-state')
    );

    expect(response.status).toBe(307);
    expect(response.headers.get('location')).toContain('github=missing_code');
    expect(fetchMock).not.toHaveBeenCalled();
    expect(db.gitHubInstallation.upsert).not.toHaveBeenCalled();
    expect(db.oAuthConnection.upsert).not.toHaveBeenCalled();
  });
});
