import { z } from 'zod';
import { codexDb } from '@/server/codex/db';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

const schema = z.object({
  accountLogin: z.string().min(1),
  installationExternalId: z.string().min(1),
});

export async function POST(request: Request) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const parsed = schema.safeParse(await request.json());
  if (!parsed.success) return fail('Payload invalido', 400, parsed.error.flatten());

  const install = await codexDb.gitHubInstallation.upsert({
    where: {
      installationExternalId: parsed.data.installationExternalId,
    },
    update: {
      accountLogin: parsed.data.accountLogin,
      workspaceId: context.context.workspace.id,
      status: 'CONNECTED',
    },
    create: {
      workspaceId: context.context.workspace.id,
      accountLogin: parsed.data.accountLogin,
      installationExternalId: parsed.data.installationExternalId,
      status: 'CONNECTED',
    },
  });

  return ok(install, 201);
}

