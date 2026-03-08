import { fail } from '@/server/codex/http';
import { fetchBackend, getBackendAccessToken, relayBackendResponse } from '@/server/backend-proxy';

export const runtime = 'nodejs';

export async function GET() {
  const token = await getBackendAccessToken();
  if (!token) return fail('Sessao nao encontrada', 401);

  const response = await fetchBackend('/api/v1/preferences/me');
  return relayBackendResponse(response);
}

export async function PUT(request: Request) {
  const token = await getBackendAccessToken();
  if (!token) return fail('Sessao nao encontrada', 401);

  const response = await fetchBackend('/api/v1/preferences/me', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: await request.text(),
  });

  return relayBackendResponse(response);
}
