import { NextResponse } from 'next/server';

export const runtime = 'nodejs';

export async function POST() {
  const response = NextResponse.json({
    success: true,
    data: {
      loggedOut: true,
    },
    timestamp: new Date().toISOString(),
  });
  response.cookies.delete('access_token');
  response.cookies.delete('refresh_token');
  return response;
}

