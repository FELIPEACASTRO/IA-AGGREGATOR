import { fail, requireCodexContext } from '@/server/codex/http';
import { codexDb } from '@/server/codex/db';

export const runtime = 'nodejs';

type RouteContext = {
  params: Promise<{
    jobId: string;
  }>;
};

export async function GET(_: Request, context: RouteContext) {
  const resolved = await requireCodexContext();
  if ('error' in resolved) return resolved.error;

  const { jobId } = await context.params;
  const workspaceId = resolved.context.workspace.id;

  const job = await codexDb.complianceExportJob.findFirst({
    where: {
      id: jobId,
      workspaceId,
    },
  });

  if (!job) {
    return fail('Export job nao encontrado', 404);
  }

  let payload: unknown;
  if (job.exportType === 'tasks') {
    payload = await codexDb.task.findMany({
      where: { workspaceId },
      orderBy: { createdAt: 'desc' },
      take: 500,
    });
  } else if (job.exportType === 'logs') {
    payload = await codexDb.taskLogChunk.findMany({
      where: {
        task: {
          workspaceId,
        },
      },
      orderBy: { createdAt: 'desc' },
      take: 2000,
    });
  } else if (job.exportType === 'audit') {
    payload = await codexDb.auditLog.findMany({
      where: { workspaceId },
      orderBy: { createdAt: 'desc' },
      take: 2000,
    });
  } else {
    payload = await codexDb.usageEntry.findMany({
      where: { workspaceId },
      orderBy: { createdAt: 'desc' },
      take: 2000,
    });
  }

  const body = JSON.stringify(
    {
      jobId: job.id,
      exportType: job.exportType,
      generatedAt: new Date().toISOString(),
      workspaceId,
      payload,
    },
    null,
    2
  );

  return new Response(body, {
    status: 200,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      'Content-Disposition': `attachment; filename=\"compliance-${job.exportType}-${job.id}.json\"`,
      'Cache-Control': 'no-store',
    },
  });
}
