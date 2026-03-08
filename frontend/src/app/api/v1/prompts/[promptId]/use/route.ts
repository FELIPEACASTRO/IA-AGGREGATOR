import { fail, ok, requireCodexContext } from '@/server/codex/http';
import { codexDb } from '@/server/codex/db';
import type { PromptTemplateDto } from '@/lib/contracts/platform';

export const runtime = 'nodejs';

type RouteContext = {
  params: Promise<{
    promptId: string;
  }>;
};

function mapPromptTemplate(template: {
  id: string;
  workspaceId: string | null;
  scope: 'SYSTEM' | 'WORKSPACE';
  category: string;
  title: string;
  description: string;
  prompt: string;
  tag: string | null;
  isActive: boolean;
  createdAt: Date;
  updatedAt: Date;
}): PromptTemplateDto {
  return {
    id: template.id,
    workspaceId: template.workspaceId,
    scope: template.scope,
    category: template.category,
    title: template.title,
    description: template.description,
    prompt: template.prompt,
    tag: template.tag,
    isActive: template.isActive,
    createdAt: template.createdAt.toISOString(),
    updatedAt: template.updatedAt.toISOString(),
  };
}

export async function POST(_: Request, context: RouteContext) {
  const resolved = await requireCodexContext();
  if ('error' in resolved) return resolved.error;

  const { promptId } = await context.params;
  const workspaceId = resolved.context.workspace.id;
  const userId = resolved.session.userId;

  const template = await codexDb.promptTemplate.findFirst({
    where: {
      id: promptId,
      isActive: true,
      OR: [
        { scope: 'SYSTEM' },
        { scope: 'WORKSPACE', workspaceId },
      ],
    },
  });

  if (!template) {
    return fail('Template nao encontrado', 404);
  }

  await codexDb.promptUsage.create({
    data: {
      templateId: template.id,
      workspaceId,
      userId,
    },
  });

  return ok({
    template: mapPromptTemplate(template),
  });
}
