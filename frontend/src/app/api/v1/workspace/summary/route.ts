import { fail, ok } from '@/server/codex/http';
import { buildWorkspaceSummary, getResolvedSessionContext } from '@/server/codex/session-context';

export const runtime = 'nodejs';

export async function GET() {
  const resolved = await getResolvedSessionContext();
  if (!resolved) return fail('Sessao nao encontrada', 401);

  const summary = await buildWorkspaceSummary(resolved.context.workspace.id);
  if (!summary) return fail('Workspace nao encontrado', 404);

  return ok(summary);
}
