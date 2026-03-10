/** @jest-environment node */

jest.mock('@/server/codex/webhook-signature', () => ({
  verifyGitHubWebhookSignature: jest.fn(),
}));

jest.mock('@/server/codex/db', () => ({
  codexDb: {
    workspace: { findFirst: jest.fn() },
    auditLog: { create: jest.fn() },
    gitRepository: { findFirst: jest.fn() },
    gitHubReviewRun: { create: jest.fn() },
    reviewFinding: { create: jest.fn() },
  },
}));

import { verifyGitHubWebhookSignature } from '@/server/codex/webhook-signature';
import { codexDb } from '@/server/codex/db';
import { POST } from '@/app/api/webhooks/github/route';

describe('POST /api/webhooks/github', () => {
  const verifyMock = verifyGitHubWebhookSignature as unknown as jest.Mock;
  const db = codexDb as unknown as {
    workspace: { findFirst: jest.Mock };
    auditLog: { create: jest.Mock };
    gitHubReviewRun: { create: jest.Mock };
  };

  beforeEach(() => {
    jest.resetAllMocks();
  });

  it('returns 401 and avoids side effects on invalid signature', async () => {
    verifyMock.mockReturnValue(false);

    const response = await POST(
      new Request('http://localhost/api/webhooks/github', {
        method: 'POST',
        headers: {
          'content-type': 'application/json',
          'x-hub-signature-256': 'sha256=invalid',
        },
        body: JSON.stringify({ repository: { full_name: 'org/repo' } }),
      })
    );
    const payload = await response.json();

    expect(response.status).toBe(401);
    expect(payload?.success).toBe(false);
    expect(db.workspace.findFirst).not.toHaveBeenCalled();
    expect(db.auditLog.create).not.toHaveBeenCalled();
    expect(db.gitHubReviewRun.create).not.toHaveBeenCalled();
  });
});
