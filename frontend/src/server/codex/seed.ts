import { codexDb } from '@/server/codex/db';

export async function ensureWorkspaceForUser(input: {
  userId: string;
  email: string;
  name: string;
}) {
  const user = await codexDb.user.upsert({
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

  const workspaceSlug = `ws-${input.email.split('@')[0].replace(/[^a-zA-Z0-9-]/g, '-').toLowerCase()}`;
  const workspace = await codexDb.workspace.upsert({
    where: { slug: workspaceSlug },
    update: {
      name: `${input.name} Workspace`,
    },
    create: {
      slug: workspaceSlug,
      name: `${input.name} Workspace`,
    },
  });

  await codexDb.membership.upsert({
    where: {
      workspaceId_userId: {
        workspaceId: workspace.id,
        userId: user.id,
      },
    },
    update: {
      role: 'OWNER',
    },
    create: {
      workspaceId: workspace.id,
      userId: user.id,
      role: 'OWNER',
    },
  });

  const repository = await codexDb.gitRepository.upsert({
    where: {
      fullName: `local/${workspace.slug}/ia-aggregator`,
    },
    update: {
      isAuthorized: true,
      defaultBranch: 'main',
    },
    create: {
      workspaceId: workspace.id,
      fullName: `local/${workspace.slug}/ia-aggregator`,
      provider: 'github',
      defaultBranch: 'main',
      cloneUrl: process.env.CODEX_LOCAL_REPO_CLONE_URL || '',
      isAuthorized: true,
    },
  });

  const environment = await codexDb.environment.upsert({
    where: {
      workspaceId_name: {
        workspaceId: workspace.id,
        name: 'Default Cloud',
      },
    },
    update: {},
    create: {
      workspaceId: workspace.id,
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
    where: { workspaceId: workspace.id },
    update: {},
    create: {
      workspaceId: workspace.id,
      balance: 0,
      includedUsageLeft: 100,
    },
  });

  return { user, workspace, repository, environment };
}

