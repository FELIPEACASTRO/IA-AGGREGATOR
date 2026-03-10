import { createHmac } from 'node:crypto';
import {
  verifyGitHubWebhookSignature,
  verifyLinearWebhookSignature,
  verifySlackWebhookSignature,
} from '@/server/codex/webhook-signature';

describe('webhook signature verification', () => {
  it('validates GitHub sha256 signature', () => {
    const payload = JSON.stringify({ action: 'opened' });
    const secret = 'github-secret';
    const digest = createHmac('sha256', secret).update(payload, 'utf-8').digest('hex');

    const valid = verifyGitHubWebhookSignature({
      payload,
      signatureHeader: `sha256=${digest}`,
      secret,
    });

    expect(valid).toBe(true);
  });

  it('rejects invalid GitHub signature', () => {
    const valid = verifyGitHubWebhookSignature({
      payload: '{}',
      signatureHeader: 'sha256=deadbeef',
      secret: 'github-secret',
    });

    expect(valid).toBe(false);
  });

  it('validates Slack signature and timestamp window', () => {
    const payload = '{"type":"event_callback"}';
    const secret = 'slack-secret';
    const timestamp = '1700000000';
    const baseString = `v0:${timestamp}:${payload}`;
    const digest = createHmac('sha256', secret).update(baseString, 'utf-8').digest('hex');

    const valid = verifySlackWebhookSignature({
      payload,
      signatureHeader: `v0=${digest}`,
      timestampHeader: timestamp,
      secret,
      nowUnixSeconds: 1700000000,
    });

    expect(valid).toBe(true);
  });

  it('rejects stale Slack webhook timestamps', () => {
    const payload = '{"type":"event_callback"}';
    const secret = 'slack-secret';
    const timestamp = '1700000000';
    const baseString = `v0:${timestamp}:${payload}`;
    const digest = createHmac('sha256', secret).update(baseString, 'utf-8').digest('hex');

    const valid = verifySlackWebhookSignature({
      payload,
      signatureHeader: `v0=${digest}`,
      timestampHeader: timestamp,
      secret,
      nowUnixSeconds: 1700000501,
    });

    expect(valid).toBe(false);
  });

  it('accepts Linear bearer token or HMAC signature', () => {
    const payload = '{"type":"Issue"}';
    const secret = 'linear-secret';
    const digest = createHmac('sha256', secret).update(payload, 'utf-8').digest('hex');

    const bearerValid = verifyLinearWebhookSignature({
      payload,
      signatureHeader: null,
      authorizationHeader: 'Bearer linear-token',
      token: 'linear-token',
    });
    expect(bearerValid).toBe(true);

    const signatureValid = verifyLinearWebhookSignature({
      payload,
      signatureHeader: `sha256=${digest}`,
      authorizationHeader: null,
      secret,
    });
    expect(signatureValid).toBe(true);
  });
});
