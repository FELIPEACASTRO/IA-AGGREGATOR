import { NextResponse } from 'next/server';
import { getServerSession } from '@/server/codex/auth';
import { ensureWorkspaceForUser } from '@/server/codex/seed';

export async function requireCodexContext() {
  const session = await getServerSession();
  if (!session) {
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

  const context = await ensureWorkspaceForUser(session);
  return {
    session,
    context,
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

