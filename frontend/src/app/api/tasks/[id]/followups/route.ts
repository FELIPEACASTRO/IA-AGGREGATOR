import { TaskMode } from '@prisma/client';
import { z } from 'zod';
import { codexDb } from '@/server/codex/db';
import { appendTaskEvent, setTaskStatus } from '@/server/codex/events';
import { fail, ok, requireCodexContext } from '@/server/codex/http';
import { enqueueTask } from '@/server/codex/queue';

export const runtime = 'nodejs';

const followupSchema = z.object({
  prompt: z.string().min(2),
  mode: z.nativeEnum(TaskMode).optional(),
});

export async function POST(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const { id } = await params;

  const task = await codexDb.task.findUnique({ where: { id } });
  if (!task || task.workspaceId !== context.context.workspace.id) {
    return fail('Task nao encontrada', 404);
  }

  const body = await request.json();
  const parsed = followupSchema.safeParse(body);
  if (!parsed.success) return fail('Follow-up invalido', 400, parsed.error.flatten());

  const mode = parsed.data.mode || task.mode;
  await codexDb.taskFollowUp.create({
    data: {
      taskId: task.id,
      prompt: parsed.data.prompt,
      mode,
      createdById: context.session.userId,
    },
  });

  await codexDb.task.update({
    where: { id: task.id },
    data: {
      prompt: parsed.data.prompt,
      mode,
      status: 'queued',
      errorMessage: null,
      failedAt: null,
      archivedAt: null,
      completedAt: null,
      updatedAt: new Date(),
      bestOfN: 1,
      attemptIndex: (task.attemptIndex || 1) + 1,
    },
  });
  await setTaskStatus(task.id, 'queued');

  await appendTaskEvent({
    taskId: task.id,
    eventType: 'task.followup_created',
    status: 'queued',
    message: 'Follow-up criado e enfileirado',
    metadata: {
      mode,
    },
  });
  await enqueueTask(task.id);

  return ok({ taskId: task.id });
}

