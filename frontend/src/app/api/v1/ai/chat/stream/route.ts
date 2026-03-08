import { relayBackendResponse } from '@/server/backend-proxy';

export const runtime = 'nodejs';

export async function POST(request: Request) {
  const body = await request.text();
  const backendUrl = `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/v1/ai/chat/stream`;

  const response = await fetch(backendUrl, {
    method: 'POST',
    body,
    headers: {
      'Content-Type': request.headers.get('content-type') || 'application/json',
      Accept: 'text/event-stream',
    },
    cache: 'no-store',
  });

  return relayBackendResponse(response);
}
