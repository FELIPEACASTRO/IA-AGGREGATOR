import { InternetMode } from '@prisma/client';
import { z } from 'zod';
import { codexDb } from '@/server/codex/db';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

const patchSchema = z.object({
  name: z.string().min(2).optional(),
  description: z.string().optional(),
  defaultBranch: z.string().optional(),
  baseImage: z.string().optional(),
  automaticSetup: z.boolean().optional(),
  setupScript: z.string().optional(),
  maintenanceScript: z.string().optional(),
  internetMode: z.nativeEnum(InternetMode).optional(),
  domainAllowlist: z.array(z.string()).optional(),
  allowedHttpMethods: z.array(z.string()).optional(),
});

export async function GET(_: Request, { params }: { params: Promise<{ id: string }> }) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const { id } = await params;
  const environment = await codexDb.environment.findUnique({
    where: { id },
    include: {
      repoMap: {
        include: {
          repository: true,
        },
      },
      runtimePins: true,
      variables: true,
      secrets: {
        select: {
          id: true,
          key: true,
          setupOnly: true,
          createdAt: true,
          updatedAt: true,
        },
      },
      internetPolicies: {
        orderBy: { createdAt: 'desc' },
      },
      executions: {
        orderBy: { createdAt: 'desc' },
        take: 20,
      },
      caches: {
        orderBy: { updatedAt: 'desc' },
        take: 5,
      },
    },
  });
  if (!environment || environment.workspaceId !== context.context.workspace.id) {
    return fail('Environment nao encontrado', 404);
  }

  return ok(environment);
}

export async function PATCH(request: Request, { params }: { params: Promise<{ id: string }> }) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const { id } = await params;

  const env = await codexDb.environment.findUnique({ where: { id } });
  if (!env || env.workspaceId !== context.context.workspace.id) {
    return fail('Environment nao encontrado', 404);
  }

  const parsed = patchSchema.safeParse(await request.json());
  if (!parsed.success) return fail('Payload invalido', 400, parsed.error.flatten());
  const data = parsed.data;

  const environment = await codexDb.environment.update({
    where: { id: env.id },
    data: {
      ...data,
      ...(data.internetMode || data.domainAllowlist || data.allowedHttpMethods
        ? {
            internetPolicies: {
              create: {
                mode: data.internetMode ?? env.internetMode,
                domainAllowlist: data.domainAllowlist ?? env.domainAllowlist,
                allowedHttpMethods: data.allowedHttpMethods ?? env.allowedHttpMethods,
              },
            },
          }
        : {}),
    },
  });

  if (
    data.setupScript !== undefined ||
    data.maintenanceScript !== undefined ||
    data.baseImage !== undefined ||
    data.domainAllowlist !== undefined ||
    data.allowedHttpMethods !== undefined
  ) {
    await codexDb.environmentCache.create({
      data: {
        environmentId: env.id,
        status: 'invalidated',
        cacheKey: `invalidate-${Date.now()}`,
        invalidationReason: 'Environment configuration changed',
      },
    });
  }

  return ok(environment);
}

export async function DELETE(_: Request, { params }: { params: Promise<{ id: string }> }) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const { id } = await params;
  const env = await codexDb.environment.findUnique({ where: { id } });
  if (!env || env.workspaceId !== context.context.workspace.id) {
    return fail('Environment nao encontrado', 404);
  }

  await codexDb.environment.delete({ where: { id: env.id } });
  return ok({ id: env.id, deleted: true });
}

