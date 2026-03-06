import { z } from 'zod';
import { Prisma } from '@prisma/client';
import { codexDb } from '@/server/codex/db';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

const schema = z.object({
  configKey: z.string().min(2),
  configValue: z.record(z.string(), z.unknown()),
  isLocked: z.boolean().default(false),
});

export async function GET() {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;

  const configs = await codexDb.managedConfig.findMany({
    where: { workspaceId: context.context.workspace.id },
    orderBy: { configKey: 'asc' },
  });
  return ok(configs);
}

export async function POST(request: Request) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const parsed = schema.safeParse(await request.json());
  if (!parsed.success) return fail('Payload invalido', 400, parsed.error.flatten());

  const data = parsed.data;
  const config = await codexDb.managedConfig.upsert({
    where: {
      workspaceId_configKey: {
        workspaceId: context.context.workspace.id,
        configKey: data.configKey,
      },
    },
    update: {
      configValue: data.configValue as Prisma.InputJsonValue,
      isLocked: data.isLocked,
    },
    create: {
      workspaceId: context.context.workspace.id,
      configKey: data.configKey,
      configValue: data.configValue as Prisma.InputJsonValue,
      isLocked: data.isLocked,
    },
  });

  return ok(config, 201);
}
