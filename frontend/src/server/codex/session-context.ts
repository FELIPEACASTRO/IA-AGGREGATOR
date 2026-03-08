import { cookies } from 'next/headers';
import type { Task, Workspace } from '@prisma/client';
import type {
  SessionContext,
  SessionUser,
  TaskDto,
  WorkspaceDto,
  WorkspaceMembershipDto,
  WorkspaceSummary,
} from '@/lib/contracts/platform';
import { codexDb } from '@/server/codex/db';
import { getBackendUserProfile, getServerSession } from '@/server/codex/auth';
import { ensureWorkspaceForUser, WORKSPACE_COOKIE_NAME } from '@/server/codex/seed';

function mapWorkspace(workspace: Workspace): WorkspaceDto {
  return {
    id: workspace.id,
    slug: workspace.slug,
    name: workspace.name,
    createdAt: workspace.createdAt.toISOString(),
    updatedAt: workspace.updatedAt.toISOString(),
  };
}

function mapTask(task: Pick<Task, 'id' | 'workspaceId' | 'title' | 'prompt' | 'mode' | 'status' | 'createdAt' | 'updatedAt' | 'completedAt' | 'failedAt' | 'archivedAt'>): TaskDto {
  return {
    id: task.id,
    workspaceId: task.workspaceId,
    projectId: null,
    title: task.title,
    prompt: task.prompt,
    mode: task.mode,
    status: task.status,
    createdAt: task.createdAt.toISOString(),
    updatedAt: task.updatedAt.toISOString(),
    completedAt: task.completedAt?.toISOString() ?? null,
    failedAt: task.failedAt?.toISOString() ?? null,
    archivedAt: task.archivedAt?.toISOString() ?? null,
  };
}

function buildSessionUser(
  session: NonNullable<Awaited<ReturnType<typeof getServerSession>>>,
  profile: Awaited<ReturnType<typeof getBackendUserProfile>>
): SessionUser {
  return {
    id: profile?.id || session.userId,
    email: profile?.email || session.email,
    fullName: profile?.fullName || session.name,
    avatarUrl: profile?.avatarUrl ?? null,
    role: profile?.role || 'MEMBER',
    status: profile?.status || 'ACTIVE',
  };
}

export async function getSelectedWorkspaceIdFromCookie() {
  const cookieStore = await cookies();
  return cookieStore.get(WORKSPACE_COOKIE_NAME)?.value ?? null;
}

export async function getResolvedSessionContext() {
  const session = await getServerSession();
  if (!session) return null;

  const selectedWorkspaceId = await getSelectedWorkspaceIdFromCookie();
  const profile = await getBackendUserProfile(session.accessToken);
  const organizationId = profile?.organizationId ?? null;
  const resolvedWorkspaceContext = await ensureWorkspaceForUser({
    userId: session.userId,
    email: session.email,
    name: session.name,
    selectedWorkspaceId,
    authOrgId: organizationId,
  });

  const workspaces: WorkspaceMembershipDto[] = resolvedWorkspaceContext.memberships.map((membership) => ({
    membershipId: membership.id,
    role: membership.role,
    workspace: mapWorkspace(membership.workspace),
  }));

  const currentWorkspace = mapWorkspace(resolvedWorkspaceContext.workspace);
  const sessionContext: SessionContext = {
    user: buildSessionUser(session, profile),
    workspaces,
    currentWorkspace,
    currentRole: resolvedWorkspaceContext.currentMembership.role,
    hasWorkspace: workspaces.length > 0,
    organizationId,
    organizationSlug:
      resolvedWorkspaceContext.workspace.authOrgId === organizationId
        ? resolvedWorkspaceContext.workspace.slug
        : null,
  };

  return {
    session,
    profile,
    context: resolvedWorkspaceContext,
    sessionContext,
  };
}

export async function buildWorkspaceSummary(workspaceId: string): Promise<WorkspaceSummary | null> {
  const workspace = await codexDb.workspace.findUnique({
    where: { id: workspaceId },
  });

  if (!workspace) {
    return null;
  }

  const [members, repositories, environments, github, slack, linear, credits, recentTasks, taskStatusCounts] = await Promise.all([
    codexDb.membership.count({ where: { workspaceId } }),
    codexDb.gitRepository.count({ where: { workspaceId } }),
    codexDb.environment.count({ where: { workspaceId } }),
    codexDb.gitHubInstallation.count({ where: { workspaceId, status: 'CONNECTED' } }),
    codexDb.slackInstall.count({ where: { workspaceId, status: 'CONNECTED' } }),
    codexDb.linearInstall.count({ where: { workspaceId, status: 'CONNECTED' } }),
    codexDb.creditBalance.findUnique({ where: { workspaceId } }),
    codexDb.task.findMany({
      where: { workspaceId },
      orderBy: { updatedAt: 'desc' },
      take: 5,
    }),
    codexDb.task.groupBy({
      by: ['status'],
      where: { workspaceId },
      _count: {
        status: true,
      },
    }),
  ]);

  const countByStatus = taskStatusCounts.reduce<Record<string, number>>((acc, entry) => {
    acc[entry.status] = entry._count.status;
    return acc;
  }, {});

  return {
    workspace: mapWorkspace(workspace),
    members,
    repositories,
    environments,
    tasks: {
      total: taskStatusCounts.reduce((sum, entry) => sum + entry._count.status, 0),
      queued: countByStatus.queued ?? 0,
      running:
        (countByStatus.preparing_environment ?? 0) +
        (countByStatus.downloading_repository ?? 0) +
        (countByStatus.cloning_repository ?? 0) +
        (countByStatus.running_setup ?? 0) +
        (countByStatus.running_maintenance ?? 0) +
        (countByStatus.running_agent ?? 0) +
        (countByStatus.validating ?? 0) +
        (countByStatus.generating_diff ?? 0) +
        (countByStatus.pr_ready ?? 0),
      completed: countByStatus.completed ?? 0,
      failed: countByStatus.failed ?? 0,
    },
    connectors: {
      github,
      slack,
      linear,
    },
    credits: {
      balance: credits?.balance ?? 0,
      includedUsageLeft: credits?.includedUsageLeft ?? 0,
    },
    recentTasks: recentTasks.map(mapTask),
  };
}
