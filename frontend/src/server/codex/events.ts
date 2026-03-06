import { Prisma, TaskStatus } from '@prisma/client';
import { codexDb } from '@/server/codex/db';

export async function appendTaskEvent(input: {
  taskId: string;
  eventType: string;
  status?: TaskStatus;
  message?: string;
  metadata?: Record<string, unknown>;
}) {
  const event = await codexDb.taskEvent.create({
    data: {
      taskId: input.taskId,
      eventType: input.eventType,
      status: input.status,
      message: input.message,
      metadata: input.metadata as Prisma.InputJsonValue | undefined,
    },
  });

  return event;
}

export async function appendTaskLog(input: {
  taskId: string;
  phase: 'provisioning' | 'repo_download' | 'setup' | 'maintenance' | 'agent' | 'validation' | 'pr_push';
  line: string;
  lineNumber: number;
  isError?: boolean;
}) {
  return codexDb.taskLogChunk.create({
    data: {
      taskId: input.taskId,
      phase: input.phase,
      line: input.line,
      lineNumber: input.lineNumber,
      isError: Boolean(input.isError),
    },
  });
}

export async function setTaskStatus(taskId: string, status: TaskStatus, patch?: Prisma.TaskUpdateInput) {
  await codexDb.task.update({
    where: { id: taskId },
    data: {
      status,
      ...(patch ?? {}),
    },
  });
}
