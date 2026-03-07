import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

// Auth gate disabled — all routes are public for now
// eslint-disable-next-line @typescript-eslint/no-unused-vars
export function middleware(_request: NextRequest) {
  return NextResponse.next();
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
};
