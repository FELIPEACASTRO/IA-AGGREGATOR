/** @jest-environment node */

jest.mock('next/headers', () => ({
  cookies: jest.fn(),
}));

jest.mock('@/server/codex/auth-cookies', () => ({
  clearAuthCookies: jest.fn(),
}));

import { cookies } from 'next/headers';
import { clearAuthCookies } from '@/server/codex/auth-cookies';
import { POST } from '@/app/api/auth/logout/route';

describe('POST /api/auth/logout', () => {
  const cookiesMock = cookies as unknown as jest.Mock;
  const clearAuthCookiesMock = clearAuthCookies as unknown as jest.Mock;

  beforeEach(() => {
    jest.resetAllMocks();
    process.env.NEXT_PUBLIC_API_URL = 'http://localhost:8081';
  });

  it('forwards bearer and refresh token to backend logout', async () => {
    cookiesMock.mockResolvedValue({
      get: (name: string) => {
        if (name === 'access_token') return { value: 'access-123' };
        if (name === 'refresh_token') return { value: 'refresh-123' };
        return undefined;
      },
    });

    const fetchMock = jest.spyOn(global, 'fetch').mockResolvedValue({ ok: true } as unknown as Response);

    const response = await POST(
      new Request('http://localhost/api/auth/logout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({}),
      })
    );
    const payload = await response.json();

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8081/api/v1/auth/logout',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
          Authorization: 'Bearer access-123',
        }),
        body: JSON.stringify({ refreshToken: 'refresh-123' }),
      })
    );
    expect(payload?.data?.revoked).toBe(true);
    expect(clearAuthCookiesMock).toHaveBeenCalledWith(response);
  });

  it('returns revoked=false when access token is missing', async () => {
    cookiesMock.mockResolvedValue({
      get: (name: string) => {
        if (name === 'refresh_token') return { value: 'refresh-123' };
        return undefined;
      },
    });

    const fetchMock = jest.spyOn(global, 'fetch');

    const response = await POST(
      new Request('http://localhost/api/auth/logout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({}),
      })
    );
    const payload = await response.json();

    expect(fetchMock).not.toHaveBeenCalled();
    expect(payload?.data?.revoked).toBe(false);
    expect(clearAuthCookiesMock).toHaveBeenCalledWith(response);
  });
});
