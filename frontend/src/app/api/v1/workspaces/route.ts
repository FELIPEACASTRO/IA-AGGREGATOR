import { NextResponse } from 'next/server';
import { z } from 'zod';
import { fail, ok } from '@/server/codex/http';
import { getResolvedSessionContext } from '@/server/codex/session-context';
import { createWorkspaceForUser, WORKSPACE_COOKIE_NAME } from '@/server/codex/seed';

export const runtime = 'nodejs';

const createWorkspaceSchema = z.object({
  name: z.string().trim().min(3).max(80),
});

export async function GET() {
  const resolved = await getResolvedSessionContext();
  if (!resolved) return fail('Sessao nao encontrada', 401);
  return ok(resolved.sessionContext.workspaces);
}

export async function POST(request: Request) {
  const resolved = await getResolvedSessionContext();
  if (!resolved) return fail('Sessao nao encontrada', 401);

  const parsed = createWorkspaceSchema.safeParse(await request.json());
  if (!parsed.success) {
    return fail('Payload invalido para criacao de workspace', 400, parsed.error.flatten());
  }

  const workspace = await createWorkspaceForUser({
    userId: resolved.session.userId,
    email: resolved.session.email,
    name: resolved.session.name,
    workspaceName: parsed.data.name,
  });

  const response = NextResponse.json({
    success: true,
    data: {
      workspaceId: workspace.id,
      slug: workspace.slug,
      name: workspace.name,
    },
    timestamp: new Date().toISOString(),
  });

  response.cookies.set(WORKSPACE_COOKIE_NAME, workspace.id, {
    sameSite: 'lax',
    path: '/',
    maxAge: 60 * 60 * 24 * 30,
  });

  return response;
}
