import { readFile } from 'node:fs/promises';
import { codexDb } from '@/server/codex/db';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const { id } = await params;

  const task = await codexDb.task.findUnique({
    where: { id },
    include: {
      artifacts: {
        orderBy: { createdAt: 'desc' },
      },
    },
  });
  if (!task || task.workspaceId !== context.context.workspace.id) {
    return fail('Task nao encontrada', 404);
  }

  const url = new URL(request.url);
  const artifactId = url.searchParams.get('artifactId');
  const download = url.searchParams.get('download');
  if (artifactId && download === '1') {
    const artifact = task.artifacts.find((item) => item.id === artifactId);
    if (!artifact) return fail('Artifact nao encontrado', 404);
    try {
      const content = await readFile(artifact.url, 'utf-8');
      return new Response(content, {
        headers: {
          'Content-Type': artifact.contentType || 'text/plain',
          'Content-Disposition': `attachment; filename="${artifact.title.replace(/\s+/g, '_')}"`,
        },
      });
    } catch (error) {
      return fail('Falha ao carregar artifact', 500, {
        message: error instanceof Error ? error.message : 'Erro desconhecido',
      });
    }
  }

  return ok(task.artifacts);
}

