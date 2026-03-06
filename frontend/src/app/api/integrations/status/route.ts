import { codexDb } from '@/server/codex/db';
import { ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET() {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const workspaceId = context.context.workspace.id;

  const [github, slack, linear] = await Promise.all([
    codexDb.gitHubInstallation.findMany({
      where: { workspaceId },
      orderBy: { updatedAt: 'desc' },
      take: 5,
    }),
    codexDb.slackInstall.findMany({
      where: { workspaceId },
      orderBy: { updatedAt: 'desc' },
      take: 5,
    }),
    codexDb.linearInstall.findMany({
      where: { workspaceId },
      orderBy: { updatedAt: 'desc' },
      take: 5,
    }),
  ]);

  return ok({ github, slack, linear });
}

