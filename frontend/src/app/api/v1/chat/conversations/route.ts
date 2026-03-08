import { z } from 'zod';
import { fail, ok, requireCodexContext } from '@/server/codex/http';
import { createConversation, listConversations } from '@/server/chat/service';

export const runtime = 'nodejs';

const createConversationSchema = z.object({
  title: z.string().trim().optional(),
  model: z.string().min(1),
});

export async function GET() {
  const resolved = await requireCodexContext();
  if ('error' in resolved) return resolved.error;

  const conversations = await listConversations({
    workspaceId: resolved.context.workspace.id,
    userId: resolved.session.userId,
  });

  return ok({ conversations });
}

export async function POST(request: Request) {
  const resolved = await requireCodexContext();
  if ('error' in resolved) return resolved.error;

  const parsed = createConversationSchema.safeParse(await request.json());
  if (!parsed.success) {
    return fail('Payload invalido para criar conversa', 400, parsed.error.flatten());
  }

  const conversation = await createConversation({
    workspaceId: resolved.context.workspace.id,
    userId: resolved.session.userId,
    title: parsed.data.title,
    model: parsed.data.model,
  });

  return ok({ conversation }, 201);
}
