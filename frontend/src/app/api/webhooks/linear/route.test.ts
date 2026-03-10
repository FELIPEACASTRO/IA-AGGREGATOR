/** @jest-environment node */

jest.mock('@/server/codex/webhook-signature', () => ({
  verifyLinearWebhookSignature: jest.fn(),
}));

jest.mock('@/server/codex/db', () => ({
  codexDb: {
    workspace: { findFirst: jest.fn() },
    linearEventLog: { create: jest.fn() },
    gitRepository: { findFirst: jest.fn() },
    environment: { findFirst: jest.fn() },
    membership: { findFirst: jest.fn() },
    task: { create: jest.fn() },
  },
}));

jest.mock('@/server/codex/queue', () => ({
  enqueueTask: jest.fn(),
}));

import { verifyLinearWebhookSignature } from '@/server/codex/webhook-signature';
import { enqueueTask } from '@/server/codex/queue';
import { codexDb } from '@/server/codex/db';
import { POST } from '@/app/api/webhooks/linear/route';

describe('POST /api/webhooks/linear', () => {
  const verifyMock = verifyLinearWebhookSignature as unknown as jest.Mock;
  const enqueueTaskMock = enqueueTask as unknown as jest.Mock;
  const db = codexDb as unknown as {
    workspace: { findFirst: jest.Mock };
    linearEventLog: { create: jest.Mock };
    task: { create: jest.Mock };
  };

  beforeEach(() => {
    jest.resetAllMocks();
  });

  it('returns 401 and avoids side effects on invalid signature', async () => {
    verifyMock.mockReturnValue(false);

    const response = await POST(
      new Request('http://localhost/api/webhooks/linear', {
        method: 'POST',
        headers: {
          'content-type': 'application/json',
          'linear-signature': 'invalid',
        },
        body: JSON.stringify({ type: 'Issue' }),
      })
    );
    const payload = await response.json();

    expect(response.status).toBe(401);
    expect(payload?.success).toBe(false);
    expect(db.workspace.findFirst).not.toHaveBeenCalled();
    expect(db.linearEventLog.create).not.toHaveBeenCalled();
    expect(db.task.create).not.toHaveBeenCalled();
    expect(enqueueTaskMock).not.toHaveBeenCalled();
  });
});
