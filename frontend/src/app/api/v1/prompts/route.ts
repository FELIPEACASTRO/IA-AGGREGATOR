import { ok, requireCodexContext } from '@/server/codex/http';
import { codexDb } from '@/server/codex/db';
import { ensureSystemPromptTemplates } from '@/server/chat/service';
import type { PromptTemplateDto } from '@/lib/contracts/platform';

export const runtime = 'nodejs';

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

export async function GET() {
  const resolved = await requireCodexContext();
  if ('error' in resolved) return resolved.error;

  const workspaceId = resolved.context.workspace.id;
  await ensureSystemPromptTemplates(workspaceId);

  const templates = await codexDb.promptTemplate.findMany({
    where: {
      isActive: true,
      OR: [
        { scope: 'SYSTEM' },
        { scope: 'WORKSPACE', workspaceId },
      ],
    },
    orderBy: [{ scope: 'asc' }, { category: 'asc' }, { title: 'asc' }],
  });

  return ok({
    templates: templates.map(mapPromptTemplate),
  });
}
