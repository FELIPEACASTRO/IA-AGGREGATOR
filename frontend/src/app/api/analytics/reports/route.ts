import { cookies } from 'next/headers';
import { fail } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET(request: Request) {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get('access_token')?.value;
  if (!accessToken) {
    return fail('Sessao invalida. Faca login novamente.', 401);
  }

  const backendUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
  const url = new URL(request.url);
  const upstreamUrl = new URL(`${backendUrl}/api/v1/analytics/reports`);
  url.searchParams.forEach((value, key) => upstreamUrl.searchParams.set(key, value));

  const upstream = await fetch(upstreamUrl, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      Accept: 'application/json',
    },
    cache: 'no-store',
  }).catch(() => null);

  if (!upstream) {
    return fail('Falha de conexao com backend analytics', 502);
  }

  const body = await upstream.text().catch(() => '');
  return new Response(body, {
    status: upstream.status,
    headers: {
      'Content-Type': upstream.headers.get('content-type') || 'application/json',
      'Cache-Control': 'no-store',
    },
  });
}
