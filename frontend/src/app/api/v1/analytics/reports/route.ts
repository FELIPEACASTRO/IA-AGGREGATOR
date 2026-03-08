import { fail } from '@/server/codex/http';
import { fetchBackend, getBackendAccessToken, relayBackendResponse } from '@/server/backend-proxy';

export const runtime = 'nodejs';

export async function GET(request: Request) {
  const token = await getBackendAccessToken();
  if (!token) return fail('Sessao nao encontrada', 401);

  const url = new URL(request.url);
  const response = await fetchBackend(`/api/v1/analytics/reports${url.search}`);
  return relayBackendResponse(response);
}
