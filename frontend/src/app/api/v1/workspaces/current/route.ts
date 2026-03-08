import { NextResponse } from 'next/server';
import { z } from 'zod';
import { fail } from '@/server/codex/http';
import { codexDb } from '@/server/codex/db';
import { getResolvedSessionContext } from '@/server/codex/session-context';
import { WORKSPACE_COOKIE_NAME } from '@/server/codex/seed';

export const runtime = 'nodejs';

const schema = z.object({
  workspaceId: z.string().min(1),
});

export async function PATCH(request: Request) {
  const resolved = await getResolvedSessionContext();
  if (!resolved) return fail('Sessao nao encontrada', 401);

  const parsed = schema.safeParse(await request.json());
  if (!parsed.success) {
    return fail('Payload invalido para troca de workspace', 400, parsed.error.flatten());
  }

  const membership = resolved.sessionContext.workspaces.find(
    (item) => item.workspace.id === parsed.data.workspaceId
  );

  if (!membership) {
    return fail('Workspace nao pertence ao usuario autenticado', 403);
  }

  await codexDb.auditLog.create({
    data: {
      workspaceId: membership.workspace.id,
      userId: resolved.session.userId,
      action: 'workspace.selected',
      targetType: 'workspace',
      targetId: membership.workspace.id,
      metadata: {
        role: membership.role,
      },
    },
  });

  const response = NextResponse.json({
    success: true,
    data: {
      workspaceId: membership.workspace.id,
    },
    timestamp: new Date().toISOString(),
  });

  response.cookies.set(WORKSPACE_COOKIE_NAME, membership.workspace.id, {
    sameSite: 'lax',
    path: '/',
    maxAge: 60 * 60 * 24 * 30,
  });

  return response;
}
