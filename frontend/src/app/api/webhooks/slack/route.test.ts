/** @jest-environment node */

jest.mock('@/server/codex/webhook-signature', () => ({
  verifySlackWebhookSignature: jest.fn(),
}));

jest.mock('@/server/codex/db', () => ({
  codexDb: {
    workspace: { findFirst: jest.fn() },
    slackEventLog: { create: jest.fn() },
    gitRepository: { findFirst: jest.fn() },
    environment: { findFirst: jest.fn() },
    membership: { findFirst: jest.fn() },
    task: { create: jest.fn() },
  },
}));

jest.mock('@/server/codex/queue', () => ({
  enqueueTask: jest.fn(),
}));

import { verifySlackWebhookSignature } from '@/server/codex/webhook-signature';
import { enqueueTask } from '@/server/codex/queue';
import { codexDb } from '@/server/codex/db';
import { POST } from '@/app/api/webhooks/slack/route';

describe('POST /api/webhooks/slack', () => {
  const verifyMock = verifySlackWebhookSignature as unknown as jest.Mock;
  const enqueueTaskMock = enqueueTask as unknown as jest.Mock;
  const db = codexDb as unknown as {
    workspace: { findFirst: jest.Mock };
    slackEventLog: { create: jest.Mock };
    task: { create: jest.Mock };
  };

  beforeEach(() => {
    jest.resetAllMocks();
  });

  it('returns 401 and avoids side effects on invalid signature', async () => {
    verifyMock.mockReturnValue(false);

    const response = await POST(
      new Request('http://localhost/api/webhooks/slack', {
        method: 'POST',
        headers: {
          'content-type': 'application/json',
          'x-slack-signature': 'v0=invalid',
          'x-slack-request-timestamp': '1700000000',
        },
        body: JSON.stringify({ type: 'event_callback' }),
      })
    );
    const payload = await response.json();

    expect(response.status).toBe(401);
    expect(payload?.success).toBe(false);
    expect(db.workspace.findFirst).not.toHaveBeenCalled();
    expect(db.slackEventLog.create).not.toHaveBeenCalled();
    expect(db.task.create).not.toHaveBeenCalled();
    expect(enqueueTaskMock).not.toHaveBeenCalled();
  });
});
