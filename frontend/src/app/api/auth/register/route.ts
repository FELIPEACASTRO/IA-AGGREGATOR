import { NextResponse } from 'next/server';
import { z } from 'zod';
import { fail, ok } from '@/server/codex/http';
import { applyAuthCookies } from '@/server/codex/auth-cookies';

export const runtime = 'nodejs';

const schema = z.object({
  email: z.string().email(),
  password: z.string().min(8),
  fullName: z.string().min(2).max(100),
  referralCode: z.string().max(64).optional(),
});

export async function POST(request: Request) {
  const parsed = schema.safeParse(await request.json());
  if (!parsed.success) return fail('Payload invalido', 400, parsed.error.flatten());

  const backendUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
  const response = await fetch(`${backendUrl}/api/v1/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(parsed.data),
  });

  if (!response.ok) {
    const text = await response.text();
    return fail('Falha ao registrar usuario', response.status, text);
  }

  const payload = (await response.json()) as {
    data?: {
      accessToken?: string;
      refreshToken?: string;
      expiresIn?: number;
    };
  };

  const accessToken = payload?.data?.accessToken;
  const refreshToken = payload?.data?.refreshToken;
  if (!accessToken || !refreshToken) {
    return fail('Backend nao retornou tokens esperados', 502);
  }

  const res = NextResponse.json({
    success: true,
    data: payload.data,
    timestamp: new Date().toISOString(),
  });
  applyAuthCookies(res, { accessToken, refreshToken, expiresIn: payload?.data?.expiresIn });
  return res;
}

export async function GET() {
  return ok({
    info: 'Use POST /api/auth/register',
  });
}
