import { randomBytes } from 'node:crypto';
import { NextResponse } from 'next/server';
import { fail, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function POST() {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;

  const clientId = process.env.GITHUB_CLIENT_ID;
  const redirectUri =
    process.env.GITHUB_OAUTH_REDIRECT_URI || 'http://localhost:3001/oauth/github/callback';
  if (!clientId) {
    return fail('GITHUB_CLIENT_ID nao configurado', 400);
  }
  const state = randomBytes(24).toString('hex');

  const url =
    `https://github.com/login/oauth/authorize?client_id=${encodeURIComponent(clientId)}` +
    `&redirect_uri=${encodeURIComponent(redirectUri)}` +
    `&state=${encodeURIComponent(state)}` +
    '&scope=repo,read:user,user:email';
  const response = NextResponse.json({
    success: true,
    data: {
      authorizeUrl: url,
      state,
    },
    timestamp: new Date().toISOString(),
  });
  response.cookies.set('github_oauth_state', state, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    path: '/api/oauth/github/callback',
    maxAge: 60 * 10,
  });
  return response;
}

