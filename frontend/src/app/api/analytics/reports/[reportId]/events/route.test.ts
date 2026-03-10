/** @jest-environment node */

jest.mock('next/headers', () => ({
  cookies: jest.fn(),
}));

import { cookies } from 'next/headers';
import { GET } from '@/app/api/analytics/reports/[reportId]/events/route';

describe('GET /api/analytics/reports/[reportId]/events', () => {
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

    const response = await GET(
      new Request('http://localhost/api/analytics/reports/rep-1/events?offset=0&limit=50'),
      { params: Promise.resolve({ reportId: 'rep-1' }) }
    );
    expect(response.status).toBe(401);
  });

  it('forwards report events request with bearer token', async () => {
    cookiesMock.mockResolvedValue({
      get: (name: string) => (name === 'access_token' ? { value: 'access-123' } : undefined),
    });
    const fetchMock = jest.spyOn(global, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ success: true, data: [] }), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    );

    const response = await GET(
      new Request('http://localhost/api/analytics/reports/rep-1/events?offset=10&limit=50&category=chat'),
      { params: Promise.resolve({ reportId: 'rep-1' }) }
    );

    expect(fetchMock).toHaveBeenCalledWith(
      new URL('http://localhost:8081/api/v1/analytics/reports/rep-1/events?offset=10&limit=50&category=chat'),
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
