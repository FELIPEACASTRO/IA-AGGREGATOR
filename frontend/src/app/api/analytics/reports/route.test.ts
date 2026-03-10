/** @jest-environment node */

jest.mock('next/headers', () => ({
  cookies: jest.fn(),
}));

import { cookies } from 'next/headers';
import { GET } from '@/app/api/analytics/reports/route';

describe('GET /api/analytics/reports', () => {
  const cookiesMock = cookies as unknown as jest.Mock;

  beforeEach(() => {
    jest.resetAllMocks();
    process.env.NEXT_PUBLIC_API_URL = 'http://localhost:8081';
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('returns 401 when cookie access token is missing', async () => {
    cookiesMock.mockResolvedValue({
      get: () => undefined,
    });

    const response = await GET(new Request('http://localhost/api/analytics/reports?page=0&limit=20'));
    expect(response.status).toBe(401);
  });

  it('forwards query and bearer token to backend analytics reports', async () => {
    cookiesMock.mockResolvedValue({
      get: (name: string) => (name === 'access_token' ? { value: 'access-123' } : undefined),
    });
    const fetchMock = jest.spyOn(global, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ success: true, data: [] }), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    );

    const response = await GET(new Request('http://localhost/api/analytics/reports?page=1&limit=50&sortBy=receivedAt'));

    expect(fetchMock).toHaveBeenCalledWith(
      new URL('http://localhost:8081/api/v1/analytics/reports?page=1&limit=50&sortBy=receivedAt'),
      expect.objectContaining({
        method: 'GET',
        headers: expect.objectContaining({
          Authorization: 'Bearer access-123',
        }),
      })
    );
    expect(response.status).toBe(200);
  });
});
