import { z } from 'zod';
import { codexDb } from '@/server/codex/db';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

const schema = z.object({
  teamId: z.string().min(1),
  teamName: z.string().min(1),
  botUserId: z.string().optional(),
  postFinalReply: z.boolean().default(true),
});

export async function POST(request: Request) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const parsed = schema.safeParse(await request.json());
  if (!parsed.success) return fail('Payload invalido', 400, parsed.error.flatten());
  const payload = parsed.data;

  const install = await codexDb.slackInstall.upsert({
    where: {
      workspaceId_teamId: {
        workspaceId: context.context.workspace.id,
        teamId: payload.teamId,
      },
    },
    update: {
      teamName: payload.teamName,
      botUserId: payload.botUserId,
      status: 'CONNECTED',
      postFinalReply: payload.postFinalReply,
    },
    create: {
      workspaceId: context.context.workspace.id,
      teamId: payload.teamId,
      teamName: payload.teamName,
      botUserId: payload.botUserId,
      status: 'CONNECTED',
      postFinalReply: payload.postFinalReply,
    },
  });

  return ok(install, 201);
}

