import { fetchBackend, relayBackendResponse } from '@/server/backend-proxy';

export const runtime = 'nodejs';

export async function POST(request: Request) {
  const response = await fetchBackend('/api/v1/analytics/events', {
    method: 'POST',
    headers: {
      'Content-Type': request.headers.get('content-type') || 'application/json',
    },
    body: await request.text(),
  });

  return relayBackendResponse(response);
}
