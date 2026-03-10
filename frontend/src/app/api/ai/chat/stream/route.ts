import { cookies } from 'next/headers';
import { z } from 'zod';
import { fail } from '@/server/codex/http';

export const runtime = 'nodejs';

const requestSchema = z.object({
  prompt: z.string().min(1),
  preferredModel: z.string().min(1).optional(),
});

function buildStreamingHeaders(upstream: Response) {
  const headers = new Headers();
  headers.set('Content-Type', upstream.headers.get('content-type') || 'text/event-stream');
  headers.set('Cache-Control', 'no-cache, no-transform');
  return headers;
}

export async function POST(request: Request) {
  const cookieStore = await cookies();
  const accessToken = cookieStore.get('access_token')?.value;
  if (!accessToken) {
    return fail('Sessao invalida. Faca login novamente.', 401);
  }

  const rawBody = await request.json().catch(() => null);
  const parsed = requestSchema.safeParse(rawBody);
  if (!parsed.success) {
    return fail('Payload invalido', 400, parsed.error.flatten());
  }

  const backendUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
  const upstream = await fetch(`${backendUrl}/api/v1/ai/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(parsed.data),
    cache: 'no-store',
  }).catch(() => null);

  if (!upstream) {
    return fail('Falha de conexao com backend de streaming', 502);
  }

  if (!upstream.ok) {
    const details = await upstream.text().catch(() => '');
    return fail('Falha no stream de chat', upstream.status, details);
  }

  return new Response(upstream.body, {
    status: upstream.status,
    headers: buildStreamingHeaders(upstream),
  });
}
