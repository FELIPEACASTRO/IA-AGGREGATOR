import { codexDb } from '@/server/codex/db';

export const WORKSPACE_COOKIE_NAME = 'lume_workspace';

function slugifyWorkspaceName(value: string) {
  return value
    .normalize('NFKD')
    .replace(/[^\w\s-]/g, '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '');
}

async function buildUniqueWorkspaceSlug(baseSlug: string) {
  let candidate = baseSlug;
  let suffix = 1;

  while (await codexDb.workspace.findUnique({ where: { slug: candidate } })) {
    suffix += 1;
    candidate = `${baseSlug}-${suffix}`;
  }

  return candidate;
}

export async function syncCodexUser(input: {
  userId: string;
  email: string;
  name: string;
}) {
  return codexDb.user.upsert({
    where: { id: input.userId },
    update: {
      email: input.email,
      name: input.name,
    },
    create: {
      id: input.userId,
      email: input.email,
      name: input.name,
    },
  });
}

export async function createWorkspaceForUser(input: {
  userId: string;
  email: string;
  name: string;
  workspaceName?: string;
}) {
  const preferredName = input.workspaceName?.trim() || `${input.name.split(' ')[0] || 'Lume'} Workspace`;
  const baseSlug = slugifyWorkspaceName(`ws-${preferredName}`) || `ws-${input.userId}`;
  const slug = await buildUniqueWorkspaceSlug(baseSlug);

  const workspace = await codexDb.workspace.create({
    data: {
      slug,
      name: preferredName,
      memberships: {
        create: {
          userId: input.userId,
          role: 'OWNER',
        },
      },
    },
  });

  await codexDb.auditLog.create({
    data: {
      workspaceId: workspace.id,
      userId: input.userId,
      action: 'workspace.created',
      targetType: 'workspace',
      targetId: workspace.id,
      metadata: {
        slug: workspace.slug,
        source: 'workspace-bootstrap',
      },
    },
  });

  return workspace;
}

export async function listWorkspacesForUser(userId: string) {
  return codexDb.membership.findMany({
    where: { userId },
    include: {
      workspace: true,
    },
    orderBy: {
      createdAt: 'asc',
    },
  });
}

export async function ensureWorkspaceDefaults(input: {
  workspaceId: string;
  workspaceSlug: string;
}) {
  const repository = await codexDb.gitRepository.upsert({
    where: {
      fullName: `local/${input.workspaceSlug}/ia-aggregator`,
    },
    update: {
      isAuthorized: true,
      defaultBranch: 'main',
    },
    create: {
      workspaceId: input.workspaceId,
      fullName: `local/${input.workspaceSlug}/ia-aggregator`,
      provider: 'github',
      defaultBranch: 'main',
      cloneUrl: process.env.CODEX_LOCAL_REPO_CLONE_URL || '',
      isAuthorized: true,
    },
  });

  const environment = await codexDb.environment.upsert({
    where: {
      workspaceId_name: {
        workspaceId: input.workspaceId,
        name: 'Default Cloud',
      },
    },
    update: {},
    create: {
      workspaceId: input.workspaceId,
      name: 'Default Cloud',
      description: 'Environment padrao para tasks cloud',
      defaultBranch: 'main',
      baseImage: 'node:20-bullseye',
      automaticSetup: true,
      setupScript: 'npm ci',
      maintenanceScript: 'npm cache verify',
      internetMode: 'OFF',
      domainAllowlist: [],
      allowedHttpMethods: ['GET', 'HEAD'],
    },
  });

  const repoMapCount = await codexDb.environmentRepoMap.count({
    where: {
      environmentId: environment.id,
      repositoryId: repository.id,
    },
  });

  if (repoMapCount === 0) {
    await codexDb.environmentRepoMap.create({
      data: {
        environmentId: environment.id,
        repositoryId: repository.id,
        branch: 'main',
        priority: 0,
      },
    });
  }

  await codexDb.creditBalance.upsert({
    where: { workspaceId: input.workspaceId },
    update: {},
    create: {
      workspaceId: input.workspaceId,
      balance: 0,
      includedUsageLeft: 100,
    },
  });

  return { repository, environment };
}

export async function ensureWorkspaceForUser(input: {
  userId: string;
  email: string;
  name: string;
  selectedWorkspaceId?: string | null;
}) {
  const user = await syncCodexUser(input);

  let memberships = await listWorkspacesForUser(user.id);
  if (memberships.length === 0) {
    await createWorkspaceForUser({
      userId: input.userId,
      email: input.email,
      name: input.name,
    });
    memberships = await listWorkspacesForUser(user.id);
  }

  const currentMembership =
    memberships.find((membership) => membership.workspaceId === input.selectedWorkspaceId) ??
    memberships[0];

  const workspace = currentMembership.workspace;
  const { repository, environment } = await ensureWorkspaceDefaults({
    workspaceId: workspace.id,
    workspaceSlug: workspace.slug,
  });

  return { user, workspace, memberships, currentMembership, repository, environment };
}

