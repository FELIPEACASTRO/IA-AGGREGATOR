import { z } from 'zod';
import { Prisma } from '@prisma/client';
import { codexDb } from '@/server/codex/db';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

const schema = z.object({
  configValue: z.any().optional(),
  isLocked: z.boolean().optional(),
});

export async function PATCH(
  request: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const { id } = await params;
  const config = await codexDb.managedConfig.findUnique({ where: { id } });
  if (!config || config.workspaceId !== context.context.workspace.id) {
    return fail('Managed config nao encontrada', 404);
  }

  const parsed = schema.safeParse(await request.json());
  if (!parsed.success) return fail('Payload invalido', 400, parsed.error.flatten());

  const data: Prisma.ManagedConfigUpdateInput = {};
  if (parsed.data.configValue !== undefined) {
    data.configValue = parsed.data.configValue as Prisma.InputJsonValue;
  }
  if (parsed.data.isLocked !== undefined) {
    data.isLocked = parsed.data.isLocked;
  }

  const updated = await codexDb.managedConfig.update({
    where: { id: config.id },
    data,
  });

  return ok(updated);
}
