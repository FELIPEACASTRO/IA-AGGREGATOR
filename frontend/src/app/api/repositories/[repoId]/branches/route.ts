import { codexDb } from '@/server/codex/db';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET(_: Request, { params }: { params: Promise<{ repoId: string }> }) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const { repoId } = await params;

  const repo = await codexDb.gitRepository.findUnique({
    where: { id: repoId },
  });
  if (!repo || repo.workspaceId !== context.context.workspace.id) {
    return fail('Repositorio nao encontrado', 404);
  }

  const branches = await codexDb.repositoryBranchCache.findMany({
    where: { repositoryId: repo.id },
    orderBy: { syncedAt: 'desc' },
  });

  if (branches.length === 0) {
    const seed = await codexDb.repositoryBranchCache.create({
      data: {
        repositoryId: repo.id,
        branchName: repo.defaultBranch,
        commitSha: null,
      },
    });
    return ok([seed]);
  }

  return ok(branches);
}

