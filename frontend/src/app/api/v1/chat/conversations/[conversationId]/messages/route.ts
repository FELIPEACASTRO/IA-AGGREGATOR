import { z } from 'zod';
import { fail, ok, requireCodexContext } from '@/server/codex/http';
import { sendConversationMessage } from '@/server/chat/service';

export const runtime = 'nodejs';

const createMessageSchema = z.object({
  prompt: z.string().trim().min(1),
  preferredModel: z.string().optional(),
  agentId: z.string().optional(),
});

type RouteContext = {
  params: Promise<{
    conversationId: string;
  }>;
};

export async function POST(request: Request, context: RouteContext) {
  const resolved = await requireCodexContext();
  if ('error' in resolved) return resolved.error;

  const parsed = createMessageSchema.safeParse(await request.json());
  if (!parsed.success) {
    return fail('Payload invalido para mensagem', 400, parsed.error.flatten());
  }

  const { conversationId } = await context.params;

  try {
    const result = await sendConversationMessage({
      workspaceId: resolved.context.workspace.id,
      userId: resolved.session.userId,
      conversationId,
      ...parsed.data,
    });

    return ok(result, 201);
  } catch (error) {
    return fail(error instanceof Error ? error.message : 'Falha ao enviar mensagem', 503);
  }
}
