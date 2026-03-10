import { NextResponse } from 'next/server';

type AuthTokenPayload = {
  accessToken: string;
  refreshToken: string;
  expiresIn?: number;
};

function isProduction() {
  return process.env.NODE_ENV === 'production';
}

export function applyAuthCookies(response: NextResponse, tokens: AuthTokenPayload) {
  response.cookies.set('access_token', tokens.accessToken, {
    httpOnly: true,
    secure: isProduction(),
    sameSite: 'lax',
    path: '/',
    maxAge: tokens.expiresIn ?? 900,
  });

  response.cookies.set('refresh_token', tokens.refreshToken, {
    httpOnly: true,
    secure: isProduction(),
    sameSite: 'lax',
    path: '/',
    maxAge: 60 * 60 * 24 * 7,
  });
}

export function clearAuthCookies(response: NextResponse) {
  response.cookies.delete('access_token');
  response.cookies.delete('refresh_token');
}
