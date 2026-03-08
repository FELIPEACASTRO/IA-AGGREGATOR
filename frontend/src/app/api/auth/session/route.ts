import { fail, ok } from '@/server/codex/http';
import { getResolvedSessionContext } from '@/server/codex/session-context';

export const runtime = 'nodejs';

export async function GET() {
  const resolved = await getResolvedSessionContext();
  if (!resolved) return fail('Sessao nao encontrada', 401);
  return ok(resolved.sessionContext);
}

