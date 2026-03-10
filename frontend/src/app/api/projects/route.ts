import { z } from 'zod';
import { codexDb } from '@/server/codex/db';
import { fail, ok, requireCodexContext } from '@/server/codex/http';
import { resolveProjectContext } from '@/server/codex/projects';

export const runtime = 'nodejs';

const createProjectSchema = z.object({
  repositoryId: z.string().optional(),
  projectContextId: z.string().optional(),
  name: z.string().min(2).max(120).optional(),
  slug: z.string().min(2).max(120).optional(),
  description: z.string().max(500).optional(),
  sourceRef: z.string().max(255).optional(),
});

export async function GET() {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;

  const workspaceId = context.context.workspace.id;
  const [projects, costSummary] = await Promise.all([
    codexDb.projectContext.findMany({
      where: { workspaceId, status: 'ACTIVE' },
      orderBy: { updatedAt: 'desc' },
      include: {
        repository: {
          select: { id: true, fullName: true, defaultBranch: true },
        },
        _count: {
          select: { tasks: true },
        },
      },
      take: 100,
    }),
    codexDb.costLedgerEntry.groupBy({
      by: ['projectContextId'],
      where: {
        workspaceId,
        projectContextId: { not: null },
      },
      _sum: {
        amount: true,
        quantity: true,
      },
    }),
  ]);

  const costByProject = new Map(
    costSummary.map((entry) => [
      entry.projectContextId,
      {
        amount: entry._sum.amount ?? 0,
        quantity: entry._sum.quantity ?? 0,
      },
    ])
  );

  return ok(
    projects.map((project) => ({
      ...project,
      economics: costByProject.get(project.id) ?? { amount: 0, quantity: 0 },
    }))
  );
}

export async function POST(request: Request) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;

  const parsed = createProjectSchema.safeParse(await request.json());
  if (!parsed.success) return fail('Payload invalido para projeto', 400, parsed.error.flatten());

  try {
    const project = await resolveProjectContext({
      workspaceId: context.context.workspace.id,
      repositoryId: parsed.data.repositoryId ?? context.context.repository.id,
      ownerUserId: context.session.userId,
      projectContextId: parsed.data.projectContextId,
      projectName: parsed.data.name,
      projectSlug: parsed.data.slug,
      projectDescription: parsed.data.description,
      sourceRef: parsed.data.sourceRef,
    });

    return ok(project, 201);
  } catch (error) {
    return fail(error instanceof Error ? error.message : 'Falha ao materializar projeto', 400);
  }
}
