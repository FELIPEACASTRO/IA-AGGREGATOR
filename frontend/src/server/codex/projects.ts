import { codexDb } from '@/server/codex/db';

function slugify(value: string) {
  const normalized = value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');

  return normalized || 'general';
}

export async function ensureDefaultProjectContext(input: {
  workspaceId: string;
  repositoryId?: string | null;
  ownerUserId: string;
  workspaceName: string;
}) {
  return codexDb.projectContext.upsert({
    where: {
      workspaceId_slug: {
        workspaceId: input.workspaceId,
        slug: 'general',
      },
    },
    update: {
      repositoryId: input.repositoryId ?? undefined,
      ownerUserId: input.ownerUserId,
      name: 'General Engineering',
      description: `Projeto padrao do workspace ${input.workspaceName}`,
      status: 'ACTIVE',
    },
    create: {
      workspaceId: input.workspaceId,
      repositoryId: input.repositoryId ?? undefined,
      ownerUserId: input.ownerUserId,
      slug: 'general',
      name: 'General Engineering',
      description: `Projeto padrao do workspace ${input.workspaceName}`,
      status: 'ACTIVE',
    },
  });
}

export async function resolveProjectContext(input: {
  workspaceId: string;
  repositoryId?: string | null;
  ownerUserId: string;
  projectContextId?: string | null;
  projectName?: string | null;
  projectSlug?: string | null;
  projectDescription?: string | null;
  sourceRef?: string | null;
}) {
  if (input.projectContextId) {
    const existing = await codexDb.projectContext.findFirst({
      where: {
        id: input.projectContextId,
        workspaceId: input.workspaceId,
      },
    });

    if (existing) return existing;
    throw new Error('Project context nao encontrado para este workspace');
  }

  const repository = input.repositoryId
    ? await codexDb.gitRepository.findFirst({
        where: {
          id: input.repositoryId,
          workspaceId: input.workspaceId,
        },
      })
    : null;

  const slug =
    input.projectSlug?.trim()
      ? slugify(input.projectSlug)
      : input.projectName?.trim()
        ? slugify(input.projectName)
        : repository?.fullName
          ? slugify(repository.fullName.split('/').slice(-1)[0] ?? repository.fullName)
          : 'general';

  const name =
    input.projectName?.trim()
      || (repository ? `Repo: ${repository.fullName}` : 'General Engineering');

  const description =
    input.projectDescription?.trim()
      || (repository ? `Contexto persistente para ${repository.fullName}` : 'Projeto padrao de engenharia assistida');

  return codexDb.projectContext.upsert({
    where: {
      workspaceId_slug: {
        workspaceId: input.workspaceId,
        slug,
      },
    },
    update: {
      repositoryId: input.repositoryId ?? repository?.id ?? undefined,
      ownerUserId: input.ownerUserId,
      name,
      description,
      sourceRef: input.sourceRef ?? undefined,
      status: 'ACTIVE',
    },
    create: {
      workspaceId: input.workspaceId,
      repositoryId: input.repositoryId ?? repository?.id ?? undefined,
      ownerUserId: input.ownerUserId,
      slug,
      name,
      description,
      sourceRef: input.sourceRef ?? undefined,
      status: 'ACTIVE',
    },
  });
}
