import { codexDb } from '@/server/codex/db';
import { ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET() {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;

  const repos = await codexDb.gitRepository.findMany({
    where: {
      workspaceId: context.context.workspace.id,
    },
    orderBy: { fullName: 'asc' },
  });

  return ok(repos);
}

