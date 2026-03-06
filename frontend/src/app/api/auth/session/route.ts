import { getServerSession } from '@/server/codex/auth';
import { fail, ok } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET() {
  const session = await getServerSession();
  if (!session) return fail('Sessao nao encontrada', 401);
  return ok(session);
}

