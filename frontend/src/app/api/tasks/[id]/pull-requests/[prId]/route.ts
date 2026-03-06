import { z } from 'zod';
import { codexDb } from '@/server/codex/db';
import { appendTaskEvent } from '@/server/codex/events';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

const schema = z.object({
  title: z.string().optional(),
  body: z.string().optional(),
  status: z.enum(['draft', 'open', 'merged', 'closed', 'update_pending', 'failed']).optional(),
});

export async function PATCH(
  request: Request,
  { params }: { params: Promise<{ id: string; prId: string }> }
) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const { id, prId } = await params;

  const task = await codexDb.task.findUnique({ where: { id } });
  if (!task || task.workspaceId !== context.context.workspace.id) {
    return fail('Task nao encontrada', 404);
  }

  const currentPr = await codexDb.pullRequest.findUnique({ where: { id: prId } });
  if (!currentPr || currentPr.taskId !== task.id) {
    return fail('PR nao encontrado', 404);
  }

  const parsed = schema.safeParse(await request.json());
  if (!parsed.success) return fail('Payload invalido', 400, parsed.error.flatten());

  const pr = await codexDb.pullRequest.update({
    where: { id: prId },
    data: parsed.data,
  });

  await appendTaskEvent({
    taskId: task.id,
    eventType: 'pr.updated',
    status: 'pr_ready',
    message: 'PR atualizado',
    metadata: {
      prId: pr.id,
      status: pr.status,
    },
  });

  return ok(pr);
}

