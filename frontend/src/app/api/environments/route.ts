import { InternetMode } from '@prisma/client';
import { z } from 'zod';
import { codexDb } from '@/server/codex/db';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

const schema = z.object({
  name: z.string().min(2),
  description: z.string().optional(),
  defaultBranch: z.string().default('main'),
  baseImage: z.string().default('node:20-bullseye'),
  automaticSetup: z.boolean().default(true),
  setupScript: z.string().optional(),
  maintenanceScript: z.string().optional(),
  internetMode: z.nativeEnum(InternetMode).default(InternetMode.OFF),
  domainAllowlist: z.array(z.string()).default([]),
  allowedHttpMethods: z.array(z.string()).default(['GET', 'HEAD']),
  repositoryId: z.string().optional(),
});

export async function GET() {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const environments = await codexDb.environment.findMany({
    where: { workspaceId: context.context.workspace.id },
    include: {
      repoMap: {
        include: {
          repository: true,
        },
      },
      caches: {
        orderBy: { updatedAt: 'desc' },
        take: 1,
      },
    },
    orderBy: { updatedAt: 'desc' },
  });
  return ok(environments);
}

export async function POST(request: Request) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const parsed = schema.safeParse(await request.json());
  if (!parsed.success) return fail('Payload invalido', 400, parsed.error.flatten());

  const payload = parsed.data;
  const repositoryId = payload.repositoryId ?? context.context.repository.id;
  const environment = await codexDb.environment.create({
    data: {
      workspaceId: context.context.workspace.id,
      name: payload.name,
      description: payload.description,
      defaultBranch: payload.defaultBranch,
      baseImage: payload.baseImage,
      automaticSetup: payload.automaticSetup,
      setupScript: payload.setupScript,
      maintenanceScript: payload.maintenanceScript,
      internetMode: payload.internetMode,
      domainAllowlist: payload.domainAllowlist,
      allowedHttpMethods: payload.allowedHttpMethods,
      repoMap: {
        create: {
          repositoryId,
          branch: payload.defaultBranch,
          priority: 0,
        },
      },
      caches: {
        create: {
          status: 'cold',
          cacheKey: `env-${Date.now()}`,
        },
      },
      internetPolicies: {
        create: {
          mode: payload.internetMode,
          domainAllowlist: payload.domainAllowlist,
          allowedHttpMethods: payload.allowedHttpMethods,
        },
      },
    },
  });

  return ok(environment, 201);
}

