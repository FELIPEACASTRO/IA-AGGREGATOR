import { ReviewSeverity } from '@prisma/client';
import { z } from 'zod';
import { codexDb } from '@/server/codex/db';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

const schema = z.object({
  repositoryId: z.string(),
  enabled: z.boolean().default(true),
  automaticReviews: z.boolean().default(false),
  minSeverity: z.nativeEnum(ReviewSeverity).default(ReviewSeverity.P2),
  agentsMdPrecedence: z.boolean().default(true),
  oneOffFocusEnabled: z.boolean().default(true),
});

export async function GET() {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;

  const policies = await codexDb.codeReviewPolicy.findMany({
    where: { workspaceId: context.context.workspace.id },
    include: {
      repository: {
        select: {
          id: true,
          fullName: true,
        },
      },
    },
    orderBy: { updatedAt: 'desc' },
  });

  return ok(policies);
}

export async function POST(request: Request) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const parsed = schema.safeParse(await request.json());
  if (!parsed.success) return fail('Payload invalido', 400, parsed.error.flatten());

  const data = parsed.data;
  const policy = await codexDb.codeReviewPolicy.upsert({
    where: {
      workspaceId_repositoryId: {
        workspaceId: context.context.workspace.id,
        repositoryId: data.repositoryId,
      },
    },
    update: data,
    create: {
      workspaceId: context.context.workspace.id,
      ...data,
    },
  });

  return ok(policy, 201);
}

