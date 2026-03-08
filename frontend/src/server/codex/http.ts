import { NextResponse } from 'next/server';
import { getResolvedSessionContext } from '@/server/codex/session-context';

export async function requireCodexContext() {
  const resolved = await getResolvedSessionContext();
  if (!resolved) {
    return {
      error: NextResponse.json(
        {
          success: false,
          message: 'Sessao invalida. Faca login novamente.',
        },
        { status: 401 }
      ),
    } as const;
  }

  return {
    session: resolved.session,
    context: resolved.context,
    sessionContext: resolved.sessionContext,
  } as const;
}

export function ok<T>(data: T, status = 200) {
  return NextResponse.json(
    {
      success: true,
      data,
      timestamp: new Date().toISOString(),
    },
    { status }
  );
}

export function fail(message: string, status = 400, details?: unknown) {
  return NextResponse.json(
    {
      success: false,
      message,
      details,
      timestamp: new Date().toISOString(),
    },
    { status }
  );
}

