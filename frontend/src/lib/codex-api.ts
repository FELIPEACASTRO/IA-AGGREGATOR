import { TaskMode } from '@prisma/client';

export type CodexTask = {
  id: string;
  title: string;
  prompt: string;
  mode: TaskMode;
  status: string;
  baseBranch?: string | null;
  resultBranch?: string | null;
  createdAt: string;
  updatedAt: string;
  completedAt?: string | null;
  failedAt?: string | null;
  errorMessage?: string | null;
  archivedAt?: string | null;
  repository?: {
    id: string;
    fullName: string;
    defaultBranch: string;
  } | null;
  environment?: {
    id: string;
    name: string;
    internetMode: string;
  } | null;
  pullRequest?: {
    id: string;
    status: string;
    url?: string | null;
  } | null;
};

type ApiResult<T> = {
  success: boolean;
  data: T;
  message?: string;
};

async function jsonFetch<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
    credentials: 'include',
    cache: 'no-store',
  });
  const payload = (await response.json()) as ApiResult<T>;
  if (!response.ok || !payload.success) {
    throw new Error(payload.message || 'Codex API request failed');
  }
  return payload.data;
}

export const codexApi = {
  listTasks: (query = '') => jsonFetch<CodexTask[]>(`/api/tasks${query ? `?${query}` : ''}`),
  getTask: (id: string) => jsonFetch(`/api/tasks/${id}`),
  createTask: (payload: Record<string, unknown>) =>
    jsonFetch<CodexTask>(`/api/tasks`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  postFollowup: (id: string, payload: { prompt: string; mode?: TaskMode }) =>
    jsonFetch(`/api/tasks/${id}/followups`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  cancelTask: (id: string) =>
    jsonFetch(`/api/tasks/${id}/cancel`, {
      method: 'POST',
    }),
  retryTask: (id: string) =>
    jsonFetch(`/api/tasks/${id}/retry`, {
      method: 'POST',
    }),
  archiveTask: (id: string) =>
    jsonFetch(`/api/tasks/${id}/archive`, {
      method: 'POST',
    }),
  unarchiveTask: (id: string) =>
    jsonFetch(`/api/tasks/${id}/unarchive`, {
      method: 'POST',
    }),
  getTaskLogs: (id: string) => jsonFetch(`/api/tasks/${id}/logs`),
  getTaskDiff: (id: string) => jsonFetch(`/api/tasks/${id}/diff`),
  getTaskTests: (id: string) => jsonFetch(`/api/tasks/${id}/tests`),
  getTaskArtifacts: (id: string) => jsonFetch(`/api/tasks/${id}/artifacts`),
  getTaskPr: (id: string) => jsonFetch(`/api/tasks/${id}/pull-requests`),
  createTaskPr: (id: string, payload: { title: string; body: string; draft?: boolean }) =>
    jsonFetch(`/api/tasks/${id}/pull-requests`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  updateTaskPr: (id: string, prId: string, payload: Record<string, unknown>) =>
    jsonFetch(`/api/tasks/${id}/pull-requests/${prId}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }),
  listRepositories: () => jsonFetch(`/api/repositories`),
  listEnvironments: () => jsonFetch(`/api/environments`),
  getEnvironment: (id: string) => jsonFetch(`/api/environments/${id}`),
  createEnvironment: (payload: Record<string, unknown>) =>
    jsonFetch(`/api/environments`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  updateEnvironment: (id: string, payload: Record<string, unknown>) =>
    jsonFetch(`/api/environments/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }),
  deleteEnvironment: (id: string) =>
    jsonFetch(`/api/environments/${id}`, {
      method: 'DELETE',
    }),
  validateEnvironment: (id: string) =>
    jsonFetch(`/api/environments/${id}/validate`, {
      method: 'POST',
    }),
  resetEnvironmentCache: (id: string) =>
    jsonFetch(`/api/environments/${id}/reset-cache`, {
      method: 'POST',
    }),
  getReviewPolicies: () => jsonFetch(`/api/code-review/policies`),
  upsertReviewPolicy: (payload: Record<string, unknown>) =>
    jsonFetch(`/api/code-review/policies`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  patchReviewPolicy: (id: string, payload: Record<string, unknown>) =>
    jsonFetch(`/api/code-review/policies/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }),
  getConnectorsStatus: () => jsonFetch(`/api/integrations/status`),
  installGitHub: (payload: Record<string, unknown>) =>
    jsonFetch(`/api/integrations/github/install`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  installSlack: (payload: Record<string, unknown>) =>
    jsonFetch(`/api/integrations/slack/install`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  installLinear: (payload: Record<string, unknown>) =>
    jsonFetch(`/api/integrations/linear/install`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  getUsage: () => jsonFetch(`/api/usage`),
  getCredits: () => jsonFetch(`/api/credits`),
  purchaseCredits: (payload: { amount: number; currency?: string }) =>
    jsonFetch(`/api/credits/purchase`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  getAnalytics: () => jsonFetch(`/api/analytics`),
  getComplianceExports: () => jsonFetch(`/api/compliance/exports`),
  createComplianceExport: (payload: { exportType: 'tasks' | 'logs' | 'audit' | 'usage' }) =>
    jsonFetch(`/api/compliance/exports`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  getManagedConfigs: () => jsonFetch(`/api/managed-configs`),
  createManagedConfig: (payload: Record<string, unknown>) =>
    jsonFetch(`/api/managed-configs`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  patchManagedConfig: (id: string, payload: Record<string, unknown>) =>
    jsonFetch(`/api/managed-configs/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    }),
};

