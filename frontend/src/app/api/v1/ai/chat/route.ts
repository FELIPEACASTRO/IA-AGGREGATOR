import { z } from 'zod';
import { invokeChatGateway } from '@/server/ai/gateway';
import { fail, ok } from '@/server/codex/http';

export const runtime = 'nodejs';

const chatSchema = z.object({
  prompt: z.string().min(1),
  preferredModel: z.string().optional(),
  agentId: z.string().optional(),
});

export async function POST(request: Request) {
  const parsed = chatSchema.safeParse(await request.json());
  if (!parsed.success) {
    return fail('Payload invalido para chat', 400, parsed.error.flatten());
  }

  const result = await invokeChatGateway(parsed.data);
  return ok(result);
}
