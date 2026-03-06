import { z } from 'zod';
import { codexDb } from '@/server/codex/db';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

const schema = z.object({
  exportType: z.enum(['tasks', 'logs', 'audit', 'usage']).default('tasks'),
});

export async function GET() {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const workspaceId = context.context.workspace.id;

  const jobs = await codexDb.complianceExportJob.findMany({
    where: { workspaceId },
    orderBy: { createdAt: 'desc' },
    take: 100,
  });

  return ok(jobs);
}

export async function POST(request: Request) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const parsed = schema.safeParse(await request.json());
  if (!parsed.success) return fail('Payload invalido', 400, parsed.error.flatten());
  const workspaceId = context.context.workspace.id;
  const exportType = parsed.data.exportType;

  const job = await codexDb.complianceExportJob.create({
    data: {
      workspaceId,
      exportType,
      status: 'completed',
      requestedBy: context.session.userId,
      finishedAt: new Date(),
      downloadUrl: `/api/compliance/exports?mockDownload=${exportType}`,
    },
  });

  return ok(job, 201);
}

