import { z } from 'zod';
import { codexDb } from '@/server/codex/db';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

const schema = z.object({
  organizationId: z.string().min(1),
  organizationName: z.string().min(1),
});

export async function POST(request: Request) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const parsed = schema.safeParse(await request.json());
  if (!parsed.success) return fail('Payload invalido', 400, parsed.error.flatten());

  const install = await codexDb.linearInstall.upsert({
    where: {
      workspaceId_organizationId: {
        workspaceId: context.context.workspace.id,
        organizationId: parsed.data.organizationId,
      },
    },
    update: {
      organizationName: parsed.data.organizationName,
      status: 'CONNECTED',
    },
    create: {
      workspaceId: context.context.workspace.id,
      organizationId: parsed.data.organizationId,
      organizationName: parsed.data.organizationName,
      status: 'CONNECTED',
    },
  });

  return ok(install, 201);
}

