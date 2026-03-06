import { ReviewSeverity } from '@prisma/client';
import { z } from 'zod';
import { codexDb } from '@/server/codex/db';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

const schema = z.object({
  enabled: z.boolean().optional(),
  automaticReviews: z.boolean().optional(),
  minSeverity: z.nativeEnum(ReviewSeverity).optional(),
  agentsMdPrecedence: z.boolean().optional(),
  oneOffFocusEnabled: z.boolean().optional(),
});

export async function PATCH(
  request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const { id } = await params;

  const policy = await codexDb.codeReviewPolicy.findUnique({ where: { id } });
  if (!policy || policy.workspaceId !== context.context.workspace.id) {
    return fail('Politica nao encontrada', 404);
  }

  const parsed = schema.safeParse(await request.json());
  if (!parsed.success) return fail('Payload invalido', 400, parsed.error.flatten());

  const updated = await codexDb.codeReviewPolicy.update({
    where: { id: policy.id },
    data: parsed.data,
  });

  return ok(updated);
}

