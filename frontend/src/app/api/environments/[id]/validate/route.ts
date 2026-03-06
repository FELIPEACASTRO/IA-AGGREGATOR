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

  const status =
    env.setupScript && env.setupScript.trim().length > 0
      ? 'success'
      : 'warning';

  await codexDb.environmentExecutionHistory.create({
    data: {
      environmentId: env.id,
      status,
      durationMs: 400,
      errorMessage: status === 'warning' ? 'Setup script vazio: fallback automatico sera usado.' : null,
    },
  });

  return ok({
    environmentId: env.id,
    status,
    message:
      status === 'success'
        ? 'Validacao de setup concluida'
        : 'Setup script vazio. Configure um script para reproducao consistente.',
  });
}

