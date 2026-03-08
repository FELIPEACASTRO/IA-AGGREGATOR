import { z } from 'zod';
import { invokeChatGateway } from '@/server/ai/gateway';
import { fail, ok } from '@/server/codex/http';

export const runtime = 'nodejs';

const chatSchema = z.object({
  prompt: z.string().min(1),
  preferredModel: z.string().optional(),
  agentId: z.string().optional(),
  provider: z.string().optional(),
  systemPrompt: z.string().optional(),
  temperature: z.number().optional(),
  maxTokens: z.number().int().positive().optional(),
  fallbackProviders: z.array(z.string()).optional(),
});

export async function POST(request: Request) {
  const parsed = chatSchema.safeParse(await request.json());
  if (!parsed.success) {
    return fail('Payload invalido para chat', 400, parsed.error.flatten());
  }

  try {
    const result = await invokeChatGateway(parsed.data);
    return ok(result);
  } catch (error) {
    return fail(
      error instanceof Error ? error.message : 'Falha ao executar gateway de chat',
      503
    );
  }
}
