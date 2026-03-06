import { Queue, Worker, QueueEvents, Job } from 'bullmq';
import { executeTask } from '@/server/codex/task-runner';
import { appendTaskEvent } from '@/server/codex/events';

const QUEUE_NAME = 'codex-task-queue';

declare global {
  var __codexQueue: Queue | undefined;
  var __codexWorker: Worker | undefined;
  var __codexQueueEvents: QueueEvents | undefined;
}

type TaskJobPayload = {
  taskId: string;
};

function isTaskJobPayload(value: unknown): value is TaskJobPayload {
  return Boolean(value) && typeof value === 'object' && 'taskId' in (value as Record<string, unknown>);
}

const redisConnection = {
  host: process.env.CODEX_REDIS_HOST || '127.0.0.1',
  port: Number(process.env.CODEX_REDIS_PORT || 6379),
};

async function ensureWorker() {
  if (!global.__codexQueue) {
    global.__codexQueue = new Queue(QUEUE_NAME, {
      connection: redisConnection,
      defaultJobOptions: {
        removeOnComplete: 200,
        removeOnFail: 200,
        attempts: 1,
      },
    });
  }

  if (!global.__codexQueueEvents) {
    global.__codexQueueEvents = new QueueEvents(QUEUE_NAME, {
      connection: redisConnection,
    });
  }

  if (!global.__codexWorker) {
    global.__codexWorker = new Worker(
      QUEUE_NAME,
      async (job: Job<TaskJobPayload>) => {
        if (!isTaskJobPayload(job.data)) return;
        await appendTaskEvent({
          taskId: job.data.taskId,
          eventType: 'task.started',
          status: 'queued',
          message: 'Worker iniciou execucao da task',
          metadata: { jobId: job.id },
        });
        await executeTask(job.data.taskId);
      },
      {
        connection: redisConnection,
        concurrency: Number(process.env.CODEX_TASK_CONCURRENCY || 2),
      }
    );

    global.__codexWorker.on('failed', async (job, err) => {
      const taskId = job?.data?.taskId;
      if (!taskId) return;
      await appendTaskEvent({
        taskId,
        eventType: 'task.worker_failed',
        status: 'failed',
        message: err.message,
      });
    });
  }
}

export async function enqueueTask(taskId: string) {
  await ensureWorker();
  const queue = global.__codexQueue!;
  await queue.add('execute-task', { taskId });
}
