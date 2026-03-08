import { fail } from '@/server/codex/http';
import { fetchBackend, getBackendAccessToken, relayBackendResponse } from '@/server/backend-proxy';

export const runtime = 'nodejs';

type Params = {
  params: Promise<{
    reportId: string;
  }>;
};

export async function GET(request: Request, { params }: Params) {
  const token = await getBackendAccessToken();
  if (!token) return fail('Sessao nao encontrada', 401);

  const { reportId } = await params;
  const url = new URL(request.url);
  const response = await fetchBackend(`/api/v1/analytics/reports/${reportId}/events${url.search}`);
  return relayBackendResponse(response);
}
