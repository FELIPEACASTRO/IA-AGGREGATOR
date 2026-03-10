import { createHmac, timingSafeEqual } from 'node:crypto';

function normalizeHeaderValue(value: string | null | undefined) {
  return value?.trim() || '';
}

function secureEqual(left: string, right: string) {
  const a = Buffer.from(left, 'utf-8');
  const b = Buffer.from(right, 'utf-8');
  if (a.length !== b.length) return false;
  return timingSafeEqual(a, b);
}

function hmacSha256Hex(secret: string, payload: string) {
  return createHmac('sha256', secret).update(payload, 'utf-8').digest('hex');
}

export function verifyGitHubWebhookSignature(input: {
  payload: string;
  signatureHeader: string | null;
  secret?: string;
}) {
  const secret = input.secret?.trim();
  const signatureHeader = normalizeHeaderValue(input.signatureHeader);
  if (!secret || !signatureHeader.startsWith('sha256=')) {
    return false;
  }

  const expected = `sha256=${hmacSha256Hex(secret, input.payload)}`;
  return secureEqual(expected, signatureHeader.toLowerCase());
}

export function verifySlackWebhookSignature(input: {
  payload: string;
  signatureHeader: string | null;
  timestampHeader: string | null;
  secret?: string;
  nowUnixSeconds?: number;
}) {
  const secret = input.secret?.trim();
  const signatureHeader = normalizeHeaderValue(input.signatureHeader);
  const timestampHeader = normalizeHeaderValue(input.timestampHeader);
  if (!secret || !signatureHeader || !timestampHeader) {
    return false;
  }

  const timestamp = Number(timestampHeader);
  if (!Number.isFinite(timestamp)) {
    return false;
  }

  const now = input.nowUnixSeconds ?? Math.floor(Date.now() / 1000);
  if (Math.abs(now - timestamp) > 60 * 5) {
    return false;
  }

  const baseString = `v0:${timestamp}:${input.payload}`;
  const expected = `v0=${hmacSha256Hex(secret, baseString)}`;
  return secureEqual(expected, signatureHeader.toLowerCase());
}

function extractBearerToken(authorizationHeader: string | null) {
  const header = normalizeHeaderValue(authorizationHeader);
  if (!header.toLowerCase().startsWith('bearer ')) {
    return null;
  }
  return header.slice(7).trim();
}

export function verifyLinearWebhookSignature(input: {
  payload: string;
  signatureHeader: string | null;
  authorizationHeader: string | null;
  secret?: string;
  token?: string;
}) {
  const token = input.token?.trim();
  if (token) {
    const bearer = extractBearerToken(input.authorizationHeader);
    if (bearer && secureEqual(bearer, token)) {
      return true;
    }
  }

  const secret = input.secret?.trim();
  const signatureHeader = normalizeHeaderValue(input.signatureHeader).toLowerCase();
  if (!secret || !signatureHeader) {
    return false;
  }

  const digest = hmacSha256Hex(secret, input.payload);
  const accepted = [digest, `sha256=${digest}`, `v0=${digest}`];
  return accepted.some((candidate) => secureEqual(candidate, signatureHeader));
}
