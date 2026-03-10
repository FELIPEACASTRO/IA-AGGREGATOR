import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';
import { z } from 'zod';
import { applyAuthCookies } from '@/server/codex/auth-cookies';
import { fail } from '@/server/codex/http';

export const runtime = 'nodejs';

const schema = z.object({
  refreshToken: z.string().min(1).optional(),
});

export async function POST(request: Request) {
  const cookieStore = await cookies();
  const rawBody = await request.json().catch(() => ({}));
  const parsed = schema.safeParse(rawBody);
  if (!parsed.success) return fail('Payload invalido', 400, parsed.error.flatten());

  const refreshToken = parsed.data.refreshToken || cookieStore.get('refresh_token')?.value;
  if (!refreshToken) {
    return fail('Refresh token ausente', 401);
  }

  const backendUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
  const response = await fetch(`${backendUrl}/api/v1/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });

  if (!response.ok) {
    const text = await response.text();
    return fail('Falha ao renovar sessao', response.status, text);
  }

  const payload = (await response.json()) as {
    data?: {
      accessToken?: string;
      refreshToken?: string;
      expiresIn?: number;
    };
  };

  const accessToken = payload?.data?.accessToken;
  const nextRefreshToken = payload?.data?.refreshToken;
  if (!accessToken || !nextRefreshToken) {
    return fail('Backend nao retornou tokens esperados', 502);
  }

  const res = NextResponse.json({
    success: true,
    data: payload.data,
    timestamp: new Date().toISOString(),
  });
  applyAuthCookies(res, { accessToken, refreshToken: nextRefreshToken, expiresIn: payload?.data?.expiresIn });
  return res;
}
