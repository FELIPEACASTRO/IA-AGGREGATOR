import { z } from 'zod';
import { codexDb } from '@/server/codex/db';
import { appendTaskEvent } from '@/server/codex/events';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

const schema = z.object({
  title: z.string().min(3).max(200),
  body: z.string().min(3),
  draft: z.boolean().default(true),
});

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const { id } = await params;

  const task = await codexDb.task.findUnique({ where: { id } });
  if (!task || task.workspaceId !== context.context.workspace.id) {
    return fail('Task nao encontrada', 404);
  }

  const parsed = schema.safeParse(await request.json());
  if (!parsed.success) return fail('Payload invalido de PR', 400, parsed.error.flatten());

  const pr = await codexDb.pullRequest.upsert({
    where: { taskId: task.id },
    update: {
      title: parsed.data.title,
      body: parsed.data.body,
      status: parsed.data.draft ? 'draft' : 'open',
      branch: task.resultBranch || `codex/${task.id}`,
      url: `https://github.com/${task.repositoryId ?? 'local/repo'}/pull/${Math.floor(Math.random() * 10000)}`,
    },
    create: {
      taskId: task.id,
      repositoryId: task.repositoryId,
      title: parsed.data.title,
      body: parsed.data.body,
      status: parsed.data.draft ? 'draft' : 'open',
      branch: task.resultBranch || `codex/${task.id}`,
      url: `https://github.com/${task.repositoryId ?? 'local/repo'}/pull/${Math.floor(Math.random() * 10000)}`,
      externalNumber: Math.floor(Math.random() * 10000),
    },
  });

  await appendTaskEvent({
    taskId: task.id,
    eventType: 'pr.created',
    status: 'pr_ready',
    message: 'PR criado/atualizado',
    metadata: { prId: pr.id, prStatus: pr.status, prUrl: pr.url },
  });

  return ok(pr, 201);
}

export async function GET(_: Request, { params }: { params: Promise<{ id: string }> }) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const { id } = await params;
  const task = await codexDb.task.findUnique({
    where: { id },
    include: { pullRequest: true },
  });
  if (!task || task.workspaceId !== context.context.workspace.id) {
    return fail('Task nao encontrada', 404);
  }
  return ok(task.pullRequest);
}
