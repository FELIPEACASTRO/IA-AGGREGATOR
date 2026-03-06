import { codexDb } from '@/server/codex/db';
import { requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

function encodeEvent(data: unknown) {
  return `data: ${JSON.stringify(data)}\n\n`;
}

export async function GET(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;

  const { id } = await params;
  const task = await codexDb.task.findUnique({
    where: { id },
    select: {
      id: true,
      workspaceId: true,
      status: true,
      updatedAt: true,
    },
  });
  if (!task || task.workspaceId !== context.context.workspace.id) {
    return new Response('Not found', { status: 404 });
  }

  const url = new URL(request.url);
  const since = url.searchParams.get('since');
  let cursor = since ? new Date(since) : new Date(Date.now() - 60_000);

  const stream = new ReadableStream({
    start(controller) {
      let closed = false;
      const writer = (chunk: string) => controller.enqueue(new TextEncoder().encode(chunk));

      const loop = async () => {
        while (!closed) {
          const events = await codexDb.taskEvent.findMany({
            where: {
              taskId: id,
              createdAt: {
                gt: cursor,
              },
            },
            orderBy: { createdAt: 'asc' },
            take: 200,
          });

          for (const event of events) {
            cursor = event.createdAt;
            writer(
              encodeEvent({
                id: event.id,
                taskId: event.taskId,
                eventType: event.eventType,
                status: event.status,
                message: event.message,
                metadata: event.metadata,
                createdAt: event.createdAt,
              })
            );
          }

          const latestTask = await codexDb.task.findUnique({
            where: { id },
            select: { status: true, updatedAt: true },
          });
          writer(
            encodeEvent({
              heartbeat: true,
              taskStatus: latestTask?.status,
              timestamp: new Date().toISOString(),
            })
          );

          await new Promise((resolve) => setTimeout(resolve, 1000));
        }
      };

      loop().catch((error) => {
        writer(
          encodeEvent({
            error: true,
            message: error instanceof Error ? error.message : 'SSE loop failed',
          })
        );
        controller.close();
      });

      request.signal.addEventListener('abort', () => {
        closed = true;
        controller.close();
      });
    },
  });

  return new Response(stream, {
    headers: {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache, no-transform',
      Connection: 'keep-alive',
    },
  });
}

