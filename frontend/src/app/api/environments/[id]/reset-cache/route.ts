import { codexDb } from '@/server/codex/db';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function POST(_: Request, { params }: { params: Promise<{ id: string }> }) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const { id } = await params;
  const env = await codexDb.environment.findUnique({ where: { id } });
  if (!env || env.workspaceId !== context.context.workspace.id) {
    return fail('Environment nao encontrado', 404);
  }

  await codexDb.environmentCache.create({
    data: {
      environmentId: env.id,
      status: 'reset_pending',
      cacheKey: `reset-${Date.now()}`,
      invalidationReason: 'Manual reset',
    },
  });

  return ok({
    environmentId: env.id,
    status: 'reset_pending',
  });
}

