import { z } from 'zod';
import { fail, ok, requireCodexContext } from '@/server/codex/http';
import {
  deleteConversation,
  listConversations,
  updateConversation,
} from '@/server/chat/service';

export const runtime = 'nodejs';

const updateConversationSchema = z.object({
  title: z.string().trim().optional(),
  pinned: z.boolean().optional(),
  model: z.string().min(1).optional(),
  clearMessages: z.boolean().optional(),
});

type RouteContext = {
  params: Promise<{
    conversationId: string;
  }>;
};

export async function GET(_: Request, context: RouteContext) {
  const resolved = await requireCodexContext();
  if ('error' in resolved) return resolved.error;

  const { conversationId } = await context.params;
  const conversations = await listConversations({
    workspaceId: resolved.context.workspace.id,
    userId: resolved.session.userId,
  });
  const conversation = conversations.find((item) => item.id === conversationId);
  if (!conversation) {
    return fail('Conversa nao encontrada', 404);
  }

  return ok({ conversation });
}

export async function PATCH(request: Request, context: RouteContext) {
  const resolved = await requireCodexContext();
  if ('error' in resolved) return resolved.error;

  const parsed = updateConversationSchema.safeParse(await request.json());
  if (!parsed.success) {
    return fail('Payload invalido para atualizar conversa', 400, parsed.error.flatten());
  }

  const { conversationId } = await context.params;

  try {
    const conversation = await updateConversation({
      workspaceId: resolved.context.workspace.id,
      userId: resolved.session.userId,
      conversationId,
      ...parsed.data,
    });
    return ok({ conversation });
  } catch (error) {
    return fail(error instanceof Error ? error.message : 'Falha ao atualizar conversa', 404);
  }
}

export async function DELETE(_: Request, context: RouteContext) {
  const resolved = await requireCodexContext();
  if ('error' in resolved) return resolved.error;

  const { conversationId } = await context.params;

  try {
    await deleteConversation({
      workspaceId: resolved.context.workspace.id,
      userId: resolved.session.userId,
      conversationId,
    });
    return ok({ deleted: true });
  } catch (error) {
    return fail(error instanceof Error ? error.message : 'Falha ao excluir conversa', 404);
  }
}
