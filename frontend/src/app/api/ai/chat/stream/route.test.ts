/** @jest-environment node */

jest.mock('next/headers', () => ({
  cookies: jest.fn(),
}));

import { cookies } from 'next/headers';
import { POST } from '@/app/api/ai/chat/stream/route';

describe('POST /api/ai/chat/stream', () => {
  const cookiesMock = cookies as unknown as jest.Mock;

  beforeEach(() => {
    jest.resetAllMocks();
    process.env.NEXT_PUBLIC_API_URL = 'http://localhost:8081';
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('returns 401 when access token cookie is missing', async () => {
    cookiesMock.mockResolvedValue({
      get: () => undefined,
    });

    const response = await POST(
      new Request('http://localhost/api/ai/chat/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt: 'hello', preferredModel: 'gpt-4.1' }),
      })
    );

    expect(response.status).toBe(401);
  });

  it('forwards request to backend stream with bearer from cookie', async () => {
    cookiesMock.mockResolvedValue({
      get: (name: string) => (name === 'access_token' ? { value: 'access-123' } : undefined),
    });
    const upstreamResponse = new Response('data: {"content":"ok"}\n\n', {
      status: 200,
      headers: { 'content-type': 'text/event-stream' },
    });
    const fetchMock = jest.spyOn(global, 'fetch').mockResolvedValue(upstreamResponse);

    const response = await POST(
      new Request('http://localhost/api/ai/chat/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt: 'hello', preferredModel: 'gpt-4.1' }),
      })
    );

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8081/api/v1/ai/chat/stream',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          Authorization: 'Bearer access-123',
          Accept: 'text/event-stream',
        }),
      })
    );
    expect(response.status).toBe(200);
    expect(response.headers.get('Content-Type')).toContain('text/event-stream');
  });
});
