import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';
import { clearAuthCookies } from '@/server/codex/auth-cookies';

export const runtime = 'nodejs';

export async function POST(request: Request) {
  const cookieStore = await cookies();
  const payload = (await request.json().catch(() => ({}))) as { refreshToken?: string };
  const refreshToken = payload.refreshToken || cookieStore.get('refresh_token')?.value;
  const accessToken = cookieStore.get('access_token')?.value;

  let revoked = false;
  if (refreshToken && accessToken) {
    const backendUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
    const response = await fetch(`${backendUrl}/api/v1/auth/logout`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify({ refreshToken }),
    }).catch(() => null);
    revoked = Boolean(response?.ok);
  }

  const response = NextResponse.json({
    success: true,
    data: {
      loggedOut: true,
      revoked,
    },
    timestamp: new Date().toISOString(),
  });
  clearAuthCookies(response);
  return response;
}

