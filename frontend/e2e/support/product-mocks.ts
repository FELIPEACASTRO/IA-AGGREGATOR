import type { Page, Route } from '@playwright/test';

export type E2EUser = {
  fullName: string;
  email: string;
  password: string;
  workspaceName?: string;
};

export type MockConversation = {
  id: string;
  title: string;
  model: string;
  pinned: boolean;
  messages: Array<{
    id: string;
    role: 'user' | 'assistant' | 'error';
    content: string;
    modelUsed?: string;
    providerUsed?: string;
    fallbackUsed?: boolean;
    attempts?: number;
    timestamp: number;
  }>;
  createdAt: number;
  updatedAt: number;
};

type MockTaskListItem = {
  id: string;
  title: string;
  prompt: string;
  mode: 'ASK' | 'CODE';
  status: string;
  baseBranch?: string | null;
  resultBranch?: string | null;
  createdAt: string;
  updatedAt: string;
  completedAt?: string | null;
  failedAt?: string | null;
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

type MockTaskBundle = {
  task: MockTaskListItem;
  detail: {
    id: string;
    title: string;
    prompt: string;
    mode: 'ASK' | 'CODE';
    status: string;
    baseBranch?: string | null;
    resultBranch?: string | null;
    repository?: { fullName: string } | null;
    environment?: { name: string; internetMode: string } | null;
    summaryText?: string;
    events: Array<{
      id: string;
      eventType: string;
      status?: string | null;
      message?: string | null;
      createdAt: string;
    }>;
    pullRequest?: {
      id: string;
      status: string;
      title: string;
      body: string;
      url?: string | null;
    } | null;
  };
  logs: Array<{
    id: string;
    phase: string;
    line: string;
    lineNumber: number;
    isError: boolean;
    createdAt: string;
  }>;
  diff: {
    id: string;
    summary?: string | null;
    patch?: string | null;
    files: Array<{
      id: string;
      path: string;
      changeType: string;
      additions: number;
      deletions: number;
      hunks: Array<{ id: string; header: string; content: string }>;
    }>;
  };
  tests: {
    summary: {
      lint: string;
      typecheck: string;
      tests: string;
    };
    logs: Array<{
      id: string;
      line: string;
      lineNumber: number;
      isError: boolean;
    }>;
  };
  artifacts: Array<{
    id: string;
    artifactType: string;
    title: string;
    contentType: string;
    url: string;
    createdAt: string;
  }>;
};

type InstallProductMocksOptions = {
  authenticated?: boolean;
  user?: E2EUser;
  conversations?: MockConversation[];
};

const DEFAULT_WORKSPACES: Array<{
  membershipId: string;
  role: string;
  workspace: {
    id: string;
    slug: string;
    name: string;
    createdAt: string;
    updatedAt: string;
  };
}> = [
  {
    membershipId: 'membership-ops',
    role: 'OWNER',
    workspace: {
      id: 'ws-ops',
      slug: 'ops',
      name: 'Lume Ops',
      createdAt: '2026-03-01T10:00:00.000Z',
      updatedAt: '2026-03-08T12:00:00.000Z',
    },
  },
  {
    membershipId: 'membership-growth',
    role: 'ADMIN',
    workspace: {
      id: 'ws-growth',
      slug: 'growth',
      name: 'Lume Growth',
      createdAt: '2026-03-02T10:00:00.000Z',
      updatedAt: '2026-03-08T12:00:00.000Z',
    },
  },
];

const DEFAULT_REPOSITORIES: Array<{
  id: string;
  fullName: string;
  defaultBranch: string;
}> = [
  { id: 'repo-web', fullName: 'lume/web-app', defaultBranch: 'main' },
  { id: 'repo-api', fullName: 'lume/api-core', defaultBranch: 'main' },
];

const nowIso = () => new Date().toISOString();

const jsonSuccess = (data: unknown) => JSON.stringify({ success: true, data });
const jsonError = (message: string) => JSON.stringify({ success: false, message });

const fulfillJson = async (
  route: Route,
  status: number,
  body: string,
  headers?: Record<string, string>,
) => {
  await route.fulfill({
    status,
    contentType: 'application/json',
    body,
    headers,
  });
};

const parseBody = (route: Route): Record<string, unknown> => {
  const raw = route.request().postData();
  if (!raw) return {};
  try {
    return JSON.parse(raw) as Record<string, unknown>;
  } catch {
    return {};
  }
};

const toSentence = (prompt: string) => prompt.trim().replace(/\s+/g, ' ').slice(0, 72);

export const createRandomUser = (): E2EUser => {
  const suffix = `${Date.now()}${Math.floor(Math.random() * 10000)}`;
  return {
    fullName: `E2E User ${suffix}`,
    email: `e2e.${suffix}@example.com`,
    password: 'E2ePass!2345',
  };
};

export const createConversationFixture = (
  partial: Partial<MockConversation> & Pick<MockConversation, 'title'>,
): MockConversation => {
  const timestamp = Date.now();
  return {
    id: partial.id ?? `conv-${timestamp}-${Math.random().toString(36).slice(2, 8)}`,
    model: partial.model ?? 'gpt-4o-mini',
    pinned: partial.pinned ?? false,
    messages: partial.messages ?? [],
    createdAt: partial.createdAt ?? timestamp,
    updatedAt: partial.updatedAt ?? timestamp,
    title: partial.title,
  };
};

function buildSession(user: E2EUser, currentWorkspaceId: string) {
  const currentMembership =
    DEFAULT_WORKSPACES.find((membership) => membership.workspace.id === currentWorkspaceId)
    ?? DEFAULT_WORKSPACES[0];

  return {
    user: {
      id: 'user-e2e-1',
      email: user.email,
      fullName: user.fullName,
      role: 'admin',
      status: 'active',
      avatarUrl: null,
    },
    workspaces: DEFAULT_WORKSPACES.map((membership) => ({ ...membership })),
    currentWorkspace: { ...currentMembership.workspace },
    currentRole: currentMembership.role,
    hasWorkspace: true,
  };
}

function buildSearchItems(taskId: string) {
  return [
    {
      id: 'search-chat',
      kind: 'workspace',
      title: 'Chat',
      description: 'Conversa principal com IA',
      href: '/chat',
    },
    {
      id: 'search-billing',
      kind: 'setting',
      title: 'Plano',
      description: 'Uso, limites e assinatura',
      href: '/billing',
    },
    {
      id: 'search-codex-task',
      kind: 'task',
      title: 'Task de deploy canario',
      description: 'Execucao cloud com diff e PR',
      href: `/codex/tasks/${taskId}`,
    },
    {
      id: 'search-environment',
      kind: 'environment',
      title: 'Environment web-prod',
      description: 'Runtime do repositorio principal',
      href: '/codex/settings/environments',
    },
  ];
}

function buildTaskBundle(
  args: Partial<MockTaskBundle['task']> & {
    id: string;
    prompt: string;
    mode: 'ASK' | 'CODE';
    repositoryId?: string;
    environmentId?: string;
  },
): MockTaskBundle {
  const repository =
    DEFAULT_REPOSITORIES.find((item) => item.id === args.repositoryId)
    ?? DEFAULT_REPOSITORIES[0];
  const environmentId = args.environmentId ?? 'env-web';
  const environmentName = environmentId === 'env-api' ? 'API Hardened' : 'Web Standard';
  const internetMode = environmentId === 'env-api' ? 'LIMITED' : 'OFF';
  const title = args.title ?? toSentence(args.prompt);
  const createdAt = args.createdAt ?? nowIso();
  const updatedAt = args.updatedAt ?? createdAt;
  const resultBranch = args.resultBranch ?? `codex/${args.id}`;
  const status = args.status ?? (args.mode === 'CODE' ? 'completed' : 'queued');
  const pullRequest =
    args.mode === 'CODE'
      ? {
          id: `pr-${args.id}`,
          status: 'draft',
          title: `feat(codex): ${title}`,
          body: '## Summary\n\n- generated by codex cloud task',
          url: `https://example.com/pr/${args.id}`,
        }
      : null;

  return {
    task: {
      id: args.id,
      title,
      prompt: args.prompt,
      mode: args.mode,
      status,
      baseBranch: args.baseBranch ?? repository.defaultBranch,
      resultBranch,
      createdAt,
      updatedAt,
      completedAt: status === 'completed' ? updatedAt : null,
      failedAt: null,
      archivedAt: args.archivedAt ?? null,
      repository,
      environment: {
        id: environmentId,
        name: environmentName,
        internetMode,
      },
      pullRequest: pullRequest
        ? {
            id: pullRequest.id,
            status: pullRequest.status,
            url: pullRequest.url,
          }
        : null,
    },
    detail: {
      id: args.id,
      title,
      prompt: args.prompt,
      mode: args.mode,
      status,
      baseBranch: args.baseBranch ?? repository.defaultBranch,
      resultBranch,
      repository: { fullName: repository.fullName },
      environment: { name: environmentName, internetMode },
      summaryText:
        args.mode === 'CODE'
          ? 'Resumo executivo da execucao: patch preparado, testes consolidados e PR rascunhado.'
          : 'Resumo executivo da execucao: resposta estruturada, riscos apontados e proximos passos sugeridos.',
      events: [
        {
          id: `event-${args.id}-1`,
          eventType: 'task.created',
          status: 'completed',
          message: 'Task recebida pelo orquestrador.',
          createdAt,
        },
        {
          id: `event-${args.id}-2`,
          eventType: args.mode === 'CODE' ? 'agent.completed' : 'agent.queued',
          status,
          message:
            args.mode === 'CODE'
              ? 'Execucao concluida com diff, testes e artefatos.'
              : 'Task aguardando slot de execucao.',
          createdAt: updatedAt,
        },
      ],
      pullRequest,
    },
    logs: [
      {
        id: `log-${args.id}-1`,
        phase: 'bootstrap',
        line: 'Loading repository metadata',
        lineNumber: 1,
        isError: false,
        createdAt,
      },
      {
        id: `log-${args.id}-2`,
        phase: 'agent',
        line: args.mode === 'CODE' ? 'Generating patch bundle' : 'Preparing answer summary',
        lineNumber: 2,
        isError: false,
        createdAt: updatedAt,
      },
    ],
    diff: {
      id: `diff-${args.id}`,
      summary: '1 arquivo alterado, 12 insercoes e 2 remocoes.',
      patch: null,
      files: [
        {
          id: `file-${args.id}-1`,
          path: 'src/app/chat/page.tsx',
          changeType: 'modified',
          additions: 12,
          deletions: 2,
          hunks: [
            {
              id: `hunk-${args.id}-1`,
              header: '@@ -1,5 +1,15 @@',
              content: '+ const releaseDecision = \"go-with-conditions\";\\n- const releaseDecision = \"go\";',
            },
          ],
        },
      ],
    },
    tests: {
      summary: {
        lint: 'passed',
        typecheck: 'passed',
        tests: args.mode === 'CODE' ? 'passed' : 'not_applicable',
      },
      logs: [
        { id: `test-${args.id}-1`, line: 'npm run lint', lineNumber: 1, isError: false },
        { id: `test-${args.id}-2`, line: 'npm run type-check', lineNumber: 2, isError: false },
        { id: `test-${args.id}-3`, line: 'npm test -- --runInBand', lineNumber: 3, isError: false },
      ],
    },
    artifacts: [
      {
        id: `artifact-${args.id}-1`,
        artifactType: 'report',
        title: 'Release dossier preview',
        contentType: 'text/markdown',
        url: `/artifacts/${args.id}/dossier.md`,
        createdAt: updatedAt,
      },
    ],
  };
}

export async function seedBrowserState(
  page: Page,
  options: {
    authenticated: boolean;
    conversations?: MockConversation[];
    activeConversationId?: string | null;
    selectedModel?: string;
  },
) {
  await page.addInitScript(() => {
    window.localStorage.clear();
  });

  if (options.authenticated) {
    await page.context().addCookies([
      {
        name: 'access_token',
        value: 'mock-access-token',
        url: 'http://127.0.0.1:3001',
      },
      {
        name: 'refresh_token',
        value: 'mock-refresh-token',
        url: 'http://127.0.0.1:3001',
      },
    ]);
  }
}

export async function installProductMocks(
  page: Page,
  options: InstallProductMocksOptions = {},
) {
  const user = options.user ?? createRandomUser();
  const seededConversations = options.conversations ?? [];
  let isAuthenticated = options.authenticated ?? true;
  let currentWorkspaceId: string = DEFAULT_WORKSPACES[0].workspace.id;

  const taskBundles = new Map<string, MockTaskBundle>();
  const defaultTasks = [
    buildTaskBundle({
      id: 'task-ask-seeded',
      prompt: 'Revise o plano de rollout e destaque riscos operacionais.',
      mode: 'ASK',
      status: 'queued',
    }),
    buildTaskBundle({
      id: 'task-code-seeded',
      prompt: 'Implemente o fluxo de auditoria de release com testes automatizados.',
      mode: 'CODE',
      status: 'completed',
    }),
  ];

  for (const bundle of defaultTasks) {
    taskBundles.set(bundle.task.id, bundle);
  }

  let environments: Array<{
    id: string;
    name: string;
    description: string;
    defaultBranch: string;
    baseImage: string;
    internetMode: string;
    automaticSetup: boolean;
    setupScript: string;
    maintenanceScript: string;
    domainAllowlist: string[];
    allowedHttpMethods: string[];
    caches: Array<{ id: string; status: string; updatedAt: string; invalidationReason: string | null }>;
    executions: Array<{ id: string; status: string; createdAt: string; errorMessage: string | null }>;
    repoMap: Array<{ repository: { fullName: string } }>;
  }> = [
    {
      id: 'env-web',
      name: 'Web Standard',
      description: 'Runtime do frontend principal',
      defaultBranch: 'main',
      baseImage: 'node:20-bullseye',
      internetMode: 'OFF',
      automaticSetup: true,
      setupScript: 'npm ci',
      maintenanceScript: 'npm cache verify',
      domainAllowlist: [] as string[],
      allowedHttpMethods: ['GET', 'HEAD'],
      caches: [{ id: 'cache-web-1', status: 'warm', updatedAt: nowIso(), invalidationReason: null }],
      executions: [{ id: 'exec-web-1', status: 'validated', createdAt: nowIso(), errorMessage: null }],
      repoMap: [{ repository: { fullName: DEFAULT_REPOSITORIES[0].fullName } }],
    },
    {
      id: 'env-api',
      name: 'API Hardened',
      description: 'Ambiente com politica de internet limitada',
      defaultBranch: 'main',
      baseImage: 'eclipse-temurin:21-jdk',
      internetMode: 'LIMITED',
      automaticSetup: false,
      setupScript: './mvnw -q test',
      maintenanceScript: './mvnw -q -DskipTests package',
      domainAllowlist: ['api.github.com'],
      allowedHttpMethods: ['GET', 'POST'],
      caches: [{ id: 'cache-api-1', status: 'warm', updatedAt: nowIso(), invalidationReason: null }],
      executions: [{ id: 'exec-api-1', status: 'validated', createdAt: nowIso(), errorMessage: null }],
      repoMap: [{ repository: { fullName: DEFAULT_REPOSITORIES[1].fullName } }],
    },
  ];

  let reviewPolicies: Array<{
    id: string;
    repositoryId: string;
    enabled: boolean;
    automaticReviews: boolean;
    minSeverity: string;
    agentsMdPrecedence: boolean;
    oneOffFocusEnabled: boolean;
    repository: { id: string; fullName: string; defaultBranch: string };
  }> = [
    {
      id: 'policy-1',
      repositoryId: DEFAULT_REPOSITORIES[0].id,
      enabled: true,
      automaticReviews: true,
      minSeverity: 'P1',
      agentsMdPrecedence: true,
      oneOffFocusEnabled: true,
      repository: { ...DEFAULT_REPOSITORIES[0] },
    },
  ];

  let connectors = {
    github: [
      { id: 'gh-1', accountLogin: 'lume-org', status: 'connected', installationExternalId: '1001' },
    ],
    slack: [
      { id: 'slack-1', teamName: 'Lume Ops', status: 'connected', teamId: 'T001' },
    ],
    linear: [
      { id: 'linear-1', organizationName: 'Lume Product', status: 'connected', organizationId: 'ORG001' },
    ],
  };

  let credits = {
    balance: { balance: 180, includedUsageLeft: 62 },
    ledger: [
      {
        id: 'credit-ledger-1',
        type: 'purchase',
        amount: 40,
        description: 'Top-up manual',
        createdAt: nowIso(),
      },
    ],
  };

  let managedConfigs: Array<{
    id: string;
    configKey: string;
    configValue: Record<string, unknown>;
    isLocked: boolean;
  }> = [
    {
      id: 'config-1',
      configKey: 'workspace.cloudTasksEnabled',
      configValue: { value: true },
      isLocked: false,
    },
  ];

  const usagePayload = {
    entries: [
      { id: 'usage-1', metric: 'agent_minutes', amount: 42, unit: 'min', period: '30d', createdAt: nowIso() },
      { id: 'usage-2', metric: 'tokens', amount: 12840, unit: 'tok', period: '30d', createdAt: nowIso() },
    ],
    taskUsage: [{ taskId: 'task-code-seeded', _sum: { amount: 22 } }],
    repositoryUsage: [{ repositoryId: 'repo-web', _sum: { amount: 31 } }],
    creditBalance: credits.balance,
  };

  const codexAnalyticsPayload = {
    taskCounts: [{ status: 'completed', _count: { status: 3 } }, { status: 'queued', _count: { status: 1 } }],
    reviewFindings: [{ severity: 'P1', _count: { severity: 2 } }, { severity: 'P2', _count: { severity: 4 } }],
    usage: [{ metric: 'agent_minutes', _sum: { amount: 42 } }, { metric: 'tokens', _sum: { amount: 12840 } }],
  };

  const billingUsagePayload = {
    plans: [
      {
        id: 'starter',
        name: 'Starter',
        price: 'R$ 49',
        desc: 'Essencial para times pequenos.',
        tokens: 20000,
        models: 4,
        current: false,
        features: ['20k tokens/mês', '2 workspaces', 'Modelos essenciais'],
      },
      {
        id: 'pro',
        name: 'Pro',
        price: 'R$ 149',
        desc: 'Plano atual para squads de produto.',
        tokens: 80000,
        models: 10,
        current: true,
        features: ['80k tokens/mês', 'Analytics avançado', 'Workspaces ilimitados'],
      },
      {
        id: 'enterprise',
        name: 'Enterprise',
        price: 'Sob consulta',
        desc: 'Governança e suporte dedicados.',
        tokens: 250000,
        models: 20,
        current: false,
        features: ['SLA dedicado', 'Conectores gerenciados', 'Suporte enterprise'],
      },
    ],
    monthlyUsage: [
      { day: 'Seg', tokens: 4000 },
      { day: 'Ter', tokens: 5200 },
      { day: 'Qua', tokens: 6100 },
      { day: 'Qui', tokens: 7000 },
      { day: 'Sex', tokens: 5600 },
    ],
    current: {
      tokensUsed: 31200,
      monthlyLimit: 80000,
      pct: 39,
      estimatedFromRuns: false,
    },
  };

  const analyticsReports: Array<{
    id: string;
    source: string;
    generatedAt: string;
    receivedAt: string;
    totalEvents: number;
    counters: Record<string, number>;
  }> = [
    {
      id: 'report-1',
      source: 'frontend-web',
      generatedAt: '2026-03-07T12:00:00.000Z',
      receivedAt: '2026-03-07T12:00:05.000Z',
      totalEvents: 16,
      counters: {
        page_view: 5,
        onboarding_start: 2,
        onboarding_complete: 1,
        settings_save_preferences: 1,
        chat_send_start: 4,
        chat_send_success: 3,
      },
    },
  ];

  const reportEvents = new Map<string, Array<Record<string, unknown>>>([
    [
      'report-1',
      [
        {
          id: 'report-event-1',
          reportId: 'report-1',
          eventName: 'settings_save_preferences',
          eventCategory: 'system',
          eventTimestamp: '2026-03-07T12:00:00.000Z',
          createdAt: '2026-03-07T12:00:05.000Z',
          metadata: { locale: 'pt-BR' },
        },
        {
          id: 'report-event-2',
          reportId: 'report-1',
          eventName: 'chat_send_success',
          eventCategory: 'chat',
          eventTimestamp: '2026-03-07T12:01:00.000Z',
          createdAt: '2026-03-07T12:01:05.000Z',
          metadata: { model: 'gpt-4o-mini' },
        },
      ],
    ],
  ]);

  const updateTaskStatus = (taskId: string, status: string) => {
    const bundle = taskBundles.get(taskId);
    if (!bundle) return;

    bundle.task.status = status;
    bundle.task.updatedAt = nowIso();
    bundle.task.completedAt = status === 'completed' ? bundle.task.updatedAt : null;
    bundle.detail.status = status;
    bundle.detail.events.unshift({
      id: `event-${taskId}-${Date.now()}`,
      eventType: `task.${status}`,
      status,
      message: `Task atualizada para ${status}.`,
      createdAt: bundle.task.updatedAt,
    });

    if (status === 'archived') {
      bundle.task.archivedAt = bundle.task.updatedAt;
    }
    if (status !== 'archived') {
      bundle.task.archivedAt = null;
    }
  };

  const listTasks = (query: URLSearchParams) => {
    const requestedStatus = query.get('status');
    const requestedTab = query.get('tab');

    let items = [...taskBundles.values()].map((bundle) => bundle.task);

    if (requestedTab === 'archived') {
      items = items.filter((task) => Boolean(task.archivedAt) || task.status === 'archived');
    } else if (requestedStatus) {
      items = items.filter((task) => task.status === requestedStatus);
    } else {
      items = items.filter((task) => task.status !== 'archived');
    }

    return items.sort((left, right) => right.updatedAt.localeCompare(left.updatedAt));
  };

  await seedBrowserState(page, {
    authenticated: isAuthenticated,
    conversations: seededConversations,
  });

  await page.route('**/api/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const { pathname, searchParams } = url;
    const method = request.method();

    if (pathname === '/api/auth/login' && method === 'POST') {
      isAuthenticated = true;
      await fulfillJson(
        route,
        200,
        jsonSuccess({
          authenticated: true,
          expiresIn: 3600,
        }),
        {
          'Set-Cookie': [
            'access_token=mock-access-token; Path=/; HttpOnly; SameSite=Lax',
            'refresh_token=mock-refresh-token; Path=/; HttpOnly; SameSite=Lax',
          ].join(', '),
        },
      );
      return;
    }

    if (pathname === '/api/auth/logout' && method === 'POST') {
      isAuthenticated = false;
      await fulfillJson(route, 200, jsonSuccess({ ok: true }), {
        'Set-Cookie': [
          'access_token=; Path=/; HttpOnly; Max-Age=0; SameSite=Lax',
          'refresh_token=; Path=/; HttpOnly; Max-Age=0; SameSite=Lax',
        ].join(', '),
      });
      return;
    }

    if (pathname === '/api/v1/auth/refresh' && method === 'POST') {
      if (!isAuthenticated) {
        await fulfillJson(route, 401, jsonError('Unauthorized'));
        return;
      }

      await fulfillJson(
        route,
        200,
        jsonSuccess({
          accessToken: 'mock-access-token',
          refreshToken: 'mock-refresh-token',
        }),
      );
      return;
    }

    if (pathname === '/api/v1/session' && method === 'GET') {
      if (!isAuthenticated) {
        await fulfillJson(route, 401, jsonError('Unauthorized'));
        return;
      }

      await fulfillJson(route, 200, jsonSuccess(buildSession(user, currentWorkspaceId)));
      return;
    }

    if (pathname === '/api/v1/workspaces' && method === 'GET') {
      await fulfillJson(route, 200, jsonSuccess(DEFAULT_WORKSPACES.map((membership) => membership.workspace)));
      return;
    }

    if (pathname === '/api/v1/workspaces/current' && method === 'PATCH') {
      const body = parseBody(route);
      const workspaceId = typeof body.workspaceId === 'string' ? body.workspaceId : DEFAULT_WORKSPACES[0].workspace.id;
      currentWorkspaceId = workspaceId;
      await fulfillJson(route, 200, jsonSuccess({ workspaceId }));
      return;
    }

    if (pathname === '/api/v1/workspace/summary' && method === 'GET') {
      const currentWorkspace =
        DEFAULT_WORKSPACES.find((membership) => membership.workspace.id === currentWorkspaceId)?.workspace
        ?? DEFAULT_WORKSPACES[0].workspace;
      await fulfillJson(
        route,
        200,
        jsonSuccess({
          workspace: currentWorkspace,
          members: DEFAULT_WORKSPACES.length,
          repositories: DEFAULT_REPOSITORIES.length,
          environments: environments.length,
          tasks: {
            total: taskBundles.size,
            queued: [...taskBundles.values()].filter((bundle) => bundle.task.status === 'queued').length,
            running: [...taskBundles.values()].filter((bundle) => bundle.task.status === 'running_agent').length,
            completed: [...taskBundles.values()].filter((bundle) => bundle.task.status === 'completed').length,
            failed: [...taskBundles.values()].filter((bundle) => bundle.task.status === 'failed').length,
          },
          connectors: {
            github: connectors.github.length,
            slack: connectors.slack.length,
            linear: connectors.linear.length,
          },
          credits: {
            balance: credits.balance.balance,
            includedUsageLeft: credits.balance.includedUsageLeft,
          },
          recentTasks: listTasks(new URLSearchParams()).slice(0, 3),
        }),
      );
      return;
    }

    if (pathname === '/api/v1/search' && method === 'GET') {
      const query = (searchParams.get('q') ?? '').toLowerCase();
      const items = buildSearchItems('task-code-seeded').filter((item) => {
        if (!query) return true;
        return `${item.title} ${item.description ?? ''}`.toLowerCase().includes(query);
      });
      await fulfillJson(route, 200, jsonSuccess({ query, items }));
      return;
    }

    if (pathname === '/api/v1/ai/chat' && method === 'POST') {
      const body = parseBody(route);
      const prompt = typeof body.prompt === 'string' ? body.prompt : '';
      const preferredModel = typeof body.preferredModel === 'string' ? body.preferredModel : 'gpt-4o-mini';
      const content = prompt.toLowerCase().includes('stream')
        ? Array.from({ length: 120 }, (_, index) => `chunk-${index + 1}`).join(' ')
        : `Resposta simulada para: ${prompt}`;
      await fulfillJson(
        route,
        200,
        jsonSuccess({
          content,
          modelUsed: preferredModel,
          providerUsed: preferredModel.includes('claude') ? 'Anthropic' : 'OpenAI',
          fallbackUsed: false,
          attempts: 1,
        }),
      );
      return;
    }

    if (pathname === '/api/billing/usage' && method === 'GET') {
      await fulfillJson(route, 200, jsonSuccess(billingUsagePayload));
      return;
    }

    if (pathname === '/api/v1/analytics/events' && method === 'POST') {
      const body = parseBody(route);
      const reportId = `report-${Date.now()}`;
      const generatedAt = typeof body.generatedAt === 'string' ? body.generatedAt : nowIso();
      const source = typeof body.source === 'string' ? body.source : 'frontend-web';
      const counters =
        body.counters && typeof body.counters === 'object'
          ? (body.counters as Record<string, number>)
          : {};
      const events = Array.isArray(body.events) ? body.events : [];

      analyticsReports.unshift({
        id: reportId,
        source,
        generatedAt,
        receivedAt: nowIso(),
        totalEvents: Number(body.totalEvents ?? events.length ?? 0),
        counters,
      });

      reportEvents.set(
        reportId,
        events.map((event, index) => {
          const item = event as Record<string, unknown>;
          return {
            id: `${reportId}-event-${index + 1}`,
            reportId,
            eventName: String(item.eventName ?? 'unknown_event'),
            eventCategory: String(item.eventCategory ?? 'system'),
            eventTimestamp: String(item.eventTimestamp ?? generatedAt),
            createdAt: nowIso(),
            metadata: item.metadata ?? {},
          };
        }),
      );

      await fulfillJson(route, 200, jsonSuccess({ accepted: true }));
      return;
    }

    if (pathname === '/api/v1/analytics/reports' && method === 'GET') {
      await fulfillJson(route, 200, jsonSuccess(analyticsReports));
      return;
    }

    if (pathname.startsWith('/api/v1/analytics/reports/') && pathname.endsWith('/events') && method === 'GET') {
      const reportId = pathname.split('/')[5];
      await fulfillJson(route, 200, jsonSuccess(reportEvents.get(reportId) ?? []));
      return;
    }

    if (pathname === '/api/repositories' && method === 'GET') {
      await fulfillJson(route, 200, jsonSuccess(DEFAULT_REPOSITORIES));
      return;
    }

    if (pathname === '/api/environments' && method === 'GET') {
      await fulfillJson(route, 200, jsonSuccess(environments));
      return;
    }

    if (pathname === '/api/environments' && method === 'POST') {
      const body = parseBody(route);
      const id = `env-${Date.now()}`;
      const repository = DEFAULT_REPOSITORIES.find((item) => item.id === body.repositoryId) ?? DEFAULT_REPOSITORIES[0];
      const created = {
        id,
        name: String(body.name ?? 'Novo Environment'),
        description: body.description ? String(body.description) : '',
        defaultBranch: String(body.defaultBranch ?? repository.defaultBranch),
        baseImage: String(body.baseImage ?? 'node:20-bullseye'),
        internetMode: String(body.internetMode ?? 'OFF'),
        automaticSetup: true,
        setupScript: String(body.setupScript ?? 'npm ci'),
        maintenanceScript: String(body.maintenanceScript ?? 'npm cache verify'),
        domainAllowlist: Array.isArray(body.domainAllowlist) ? body.domainAllowlist.map(String) : [],
        allowedHttpMethods: Array.isArray(body.allowedHttpMethods) ? body.allowedHttpMethods.map(String) : ['GET', 'HEAD'],
        caches: [{ id: `cache-${id}`, status: 'cold', updatedAt: nowIso(), invalidationReason: 'created' }],
        executions: [],
        repoMap: [{ repository: { fullName: repository.fullName } }],
      };
      environments = [created, ...environments];
      await fulfillJson(route, 200, jsonSuccess({ id }));
      return;
    }

    if (pathname.startsWith('/api/environments/') && method === 'GET') {
      const environmentId = pathname.split('/')[3];
      const environment = environments.find((item) => item.id === environmentId);
      if (!environment) {
        await fulfillJson(route, 404, jsonError('Environment not found'));
        return;
      }
      await fulfillJson(route, 200, jsonSuccess(environment));
      return;
    }

    if (pathname.startsWith('/api/environments/') && method === 'PATCH') {
      const environmentId = pathname.split('/')[3];
      const body = parseBody(route);
      environments = environments.map((item) =>
        item.id === environmentId
          ? {
              ...item,
              name: String(body.name ?? item.name),
              description: body.description === undefined ? item.description : String(body.description),
              defaultBranch: String(body.defaultBranch ?? item.defaultBranch),
              baseImage: String(body.baseImage ?? item.baseImage),
              setupScript: String(body.setupScript ?? item.setupScript),
              maintenanceScript: String(body.maintenanceScript ?? item.maintenanceScript),
              internetMode: String(body.internetMode ?? item.internetMode),
              domainAllowlist: Array.isArray(body.domainAllowlist) ? body.domainAllowlist.map(String) : item.domainAllowlist,
              allowedHttpMethods: Array.isArray(body.allowedHttpMethods) ? body.allowedHttpMethods.map(String) : item.allowedHttpMethods,
            }
          : item,
      );
      const updated = environments.find((item) => item.id === environmentId);
      await fulfillJson(route, 200, jsonSuccess(updated));
      return;
    }

    if (pathname.startsWith('/api/environments/') && method === 'DELETE') {
      const environmentId = pathname.split('/')[3];
      environments = environments.filter((item) => item.id !== environmentId);
      await fulfillJson(route, 200, jsonSuccess({ deleted: true }));
      return;
    }

    if (pathname.startsWith('/api/environments/') && pathname.endsWith('/validate') && method === 'POST') {
      const environmentId = pathname.split('/')[3];
      environments = environments.map((item) =>
        item.id === environmentId
          ? {
              ...item,
              executions: [
                { id: `exec-${Date.now()}`, status: 'validated', createdAt: nowIso(), errorMessage: null },
                ...item.executions,
              ],
            }
          : item,
      );
      await fulfillJson(route, 200, jsonSuccess({ ok: true }));
      return;
    }

    if (pathname.startsWith('/api/environments/') && pathname.endsWith('/reset-cache') && method === 'POST') {
      const environmentId = pathname.split('/')[3];
      environments = environments.map((item) =>
        item.id === environmentId
          ? {
              ...item,
              caches: [
                { id: `cache-${Date.now()}`, status: 'cold', updatedAt: nowIso(), invalidationReason: 'manual-reset' },
                ...item.caches,
              ],
            }
          : item,
      );
      await fulfillJson(route, 200, jsonSuccess({ ok: true }));
      return;
    }

    if (pathname === '/api/code-review/policies' && method === 'GET') {
      await fulfillJson(route, 200, jsonSuccess(reviewPolicies));
      return;
    }

    if (pathname === '/api/code-review/policies' && method === 'POST') {
      const body = parseBody(route);
      const repo = DEFAULT_REPOSITORIES.find((item) => item.id === body.repositoryId) ?? DEFAULT_REPOSITORIES[0];
      const policy = {
        id: `policy-${Date.now()}`,
        repositoryId: repo.id,
        enabled: Boolean(body.enabled ?? true),
        automaticReviews: Boolean(body.automaticReviews ?? true),
        minSeverity: String(body.minSeverity ?? 'P2'),
        agentsMdPrecedence: Boolean(body.agentsMdPrecedence ?? true),
        oneOffFocusEnabled: Boolean(body.oneOffFocusEnabled ?? true),
        repository: { ...repo },
      };
      reviewPolicies = [policy, ...reviewPolicies.filter((item) => item.repositoryId !== repo.id)];
      await fulfillJson(route, 200, jsonSuccess(policy));
      return;
    }

    if (pathname.startsWith('/api/code-review/policies/') && method === 'PATCH') {
      const policyId = pathname.split('/')[4];
      const body = parseBody(route);
      reviewPolicies = reviewPolicies.map((item) => (item.id === policyId ? { ...item, ...body } : item));
      await fulfillJson(route, 200, jsonSuccess(reviewPolicies.find((item) => item.id === policyId)));
      return;
    }

    if (pathname === '/api/integrations/status' && method === 'GET') {
      await fulfillJson(route, 200, jsonSuccess(connectors));
      return;
    }

    if (pathname === '/api/integrations/github/install' && method === 'POST') {
      const body = parseBody(route);
      connectors.github = [
        {
          id: `gh-${Date.now()}`,
          accountLogin: String(body.accountLogin ?? 'unknown'),
          status: 'connected',
          installationExternalId: String(body.installationExternalId ?? Date.now()),
        },
        ...connectors.github,
      ];
      await fulfillJson(route, 200, jsonSuccess(connectors.github[0]));
      return;
    }

    if (pathname === '/api/integrations/slack/install' && method === 'POST') {
      const body = parseBody(route);
      connectors.slack = [
        {
          id: `slack-${Date.now()}`,
          teamName: String(body.teamName ?? 'Slack Team'),
          status: 'connected',
          teamId: String(body.teamId ?? Date.now()),
        },
        ...connectors.slack,
      ];
      await fulfillJson(route, 200, jsonSuccess(connectors.slack[0]));
      return;
    }

    if (pathname === '/api/integrations/linear/install' && method === 'POST') {
      const body = parseBody(route);
      connectors.linear = [
        {
          id: `linear-${Date.now()}`,
          organizationName: String(body.organizationName ?? 'Linear Org'),
          status: 'connected',
          organizationId: String(body.organizationId ?? Date.now()),
        },
        ...connectors.linear,
      ];
      await fulfillJson(route, 200, jsonSuccess(connectors.linear[0]));
      return;
    }

    if (pathname === '/api/usage' && method === 'GET') {
      await fulfillJson(route, 200, jsonSuccess(usagePayload));
      return;
    }

    if (pathname === '/api/credits' && method === 'GET') {
      await fulfillJson(route, 200, jsonSuccess(credits));
      return;
    }

    if (pathname === '/api/credits/purchase' && method === 'POST') {
      const body = parseBody(route);
      const amount = Number(body.amount ?? 0);
      credits = {
        balance: {
          balance: credits.balance.balance + amount,
          includedUsageLeft: credits.balance.includedUsageLeft,
        },
        ledger: [
          {
            id: `credit-ledger-${Date.now()}`,
            type: 'purchase',
            amount,
            description: 'Purchase via UI',
            createdAt: nowIso(),
          },
          ...credits.ledger,
        ],
      };
      await fulfillJson(route, 200, jsonSuccess({ purchased: amount }));
      return;
    }

    if (pathname === '/api/analytics' && method === 'GET') {
      await fulfillJson(route, 200, jsonSuccess(codexAnalyticsPayload));
      return;
    }

    if (pathname === '/api/managed-configs' && method === 'GET') {
      await fulfillJson(route, 200, jsonSuccess(managedConfigs));
      return;
    }

    if (pathname === '/api/managed-configs' && method === 'POST') {
      const body = parseBody(route);
      const config = {
        id: `config-${Date.now()}`,
        configKey: String(body.configKey ?? 'workspace.unknown'),
        configValue: (body.configValue as Record<string, unknown>) ?? { value: null },
        isLocked: Boolean(body.isLocked ?? false),
      };
      managedConfigs = [config, ...managedConfigs];
      await fulfillJson(route, 200, jsonSuccess(config));
      return;
    }

    if (pathname === '/api/tasks' && method === 'GET') {
      await fulfillJson(route, 200, jsonSuccess(listTasks(searchParams)));
      return;
    }

    if (pathname === '/api/tasks' && method === 'POST') {
      const body = parseBody(route);
      const id = `task-${Date.now()}`;
      const mode = body.mode === 'CODE' ? 'CODE' : 'ASK';
      const prompt = String(body.prompt ?? 'Nova task');
      const bundle = buildTaskBundle({
        id,
        prompt,
        mode,
        repositoryId: typeof body.repositoryId === 'string' ? body.repositoryId : undefined,
        environmentId: typeof body.environmentId === 'string' ? body.environmentId : undefined,
        status: mode === 'CODE' ? 'completed' : 'queued',
      });
      taskBundles.set(id, bundle);
      await fulfillJson(route, 200, jsonSuccess(bundle.task));
      return;
    }

    if (pathname.startsWith('/api/tasks/') && pathname.endsWith('/followups') && method === 'POST') {
      const taskId = pathname.split('/')[3];
      const bundle = taskBundles.get(taskId);
      const body = parseBody(route);
      if (!bundle) {
        await fulfillJson(route, 404, jsonError('Task not found'));
        return;
      }
      const prompt = String(body.prompt ?? '');
      bundle.detail.summaryText = `${bundle.detail.summaryText}\n\nFollow-up: ${prompt}`;
      bundle.detail.events.unshift({
        id: `followup-${Date.now()}`,
        eventType: 'task.followup',
        status: 'completed',
        message: prompt,
        createdAt: nowIso(),
      });
      bundle.task.updatedAt = nowIso();
      await fulfillJson(route, 200, jsonSuccess({ ok: true }));
      return;
    }

    if (pathname.startsWith('/api/tasks/') && pathname.endsWith('/retry') && method === 'POST') {
      updateTaskStatus(pathname.split('/')[3], 'queued');
      await fulfillJson(route, 200, jsonSuccess({ ok: true }));
      return;
    }

    if (pathname.startsWith('/api/tasks/') && pathname.endsWith('/cancel') && method === 'POST') {
      updateTaskStatus(pathname.split('/')[3], 'cancelled');
      await fulfillJson(route, 200, jsonSuccess({ ok: true }));
      return;
    }

    if (pathname.startsWith('/api/tasks/') && pathname.endsWith('/archive') && method === 'POST') {
      updateTaskStatus(pathname.split('/')[3], 'archived');
      await fulfillJson(route, 200, jsonSuccess({ ok: true }));
      return;
    }

    if (pathname.startsWith('/api/tasks/') && pathname.endsWith('/unarchive') && method === 'POST') {
      updateTaskStatus(pathname.split('/')[3], 'completed');
      await fulfillJson(route, 200, jsonSuccess({ ok: true }));
      return;
    }

    if (pathname.startsWith('/api/tasks/') && pathname.endsWith('/logs') && method === 'GET') {
      const bundle = taskBundles.get(pathname.split('/')[3]);
      await fulfillJson(route, bundle ? 200 : 404, bundle ? jsonSuccess(bundle.logs) : jsonError('Task not found'));
      return;
    }

    if (pathname.startsWith('/api/tasks/') && pathname.endsWith('/diff') && method === 'GET') {
      const bundle = taskBundles.get(pathname.split('/')[3]);
      await fulfillJson(route, bundle ? 200 : 404, bundle ? jsonSuccess(bundle.diff) : jsonError('Task not found'));
      return;
    }

    if (pathname.startsWith('/api/tasks/') && pathname.endsWith('/tests') && method === 'GET') {
      const bundle = taskBundles.get(pathname.split('/')[3]);
      await fulfillJson(route, bundle ? 200 : 404, bundle ? jsonSuccess(bundle.tests) : jsonError('Task not found'));
      return;
    }

    if (pathname.startsWith('/api/tasks/') && pathname.endsWith('/artifacts') && method === 'GET') {
      const bundle = taskBundles.get(pathname.split('/')[3]);
      await fulfillJson(route, bundle ? 200 : 404, bundle ? jsonSuccess(bundle.artifacts) : jsonError('Task not found'));
      return;
    }

    if (pathname.startsWith('/api/tasks/') && pathname.endsWith('/pull-requests') && method === 'POST') {
      const taskId = pathname.split('/')[3];
      const bundle = taskBundles.get(taskId);
      const body = parseBody(route);
      if (!bundle) {
        await fulfillJson(route, 404, jsonError('Task not found'));
        return;
      }
      bundle.detail.pullRequest = {
        id: `pr-${Date.now()}`,
        status: Boolean(body.draft) ? 'draft' : 'open',
        title: String(body.title ?? bundle.detail.title),
        body: String(body.body ?? ''),
        url: `https://example.com/pr/${taskId}`,
      };
      bundle.task.pullRequest = {
        id: bundle.detail.pullRequest.id,
        status: bundle.detail.pullRequest.status,
        url: bundle.detail.pullRequest.url,
      };
      await fulfillJson(route, 200, jsonSuccess(bundle.detail.pullRequest));
      return;
    }

    if (pathname.startsWith('/api/tasks/') && pathname.includes('/pull-requests/') && method === 'PATCH') {
      const parts = pathname.split('/');
      const taskId = parts[3];
      const bundle = taskBundles.get(taskId);
      const body = parseBody(route);
      if (!bundle || !bundle.detail.pullRequest) {
        await fulfillJson(route, 404, jsonError('Pull request not found'));
        return;
      }
      bundle.detail.pullRequest = {
        ...bundle.detail.pullRequest,
        title: String(body.title ?? bundle.detail.pullRequest.title),
        body: String(body.body ?? bundle.detail.pullRequest.body),
        status: String(body.status ?? bundle.detail.pullRequest.status),
      };
      bundle.task.pullRequest = {
        id: bundle.detail.pullRequest.id,
        status: bundle.detail.pullRequest.status,
        url: bundle.detail.pullRequest.url,
      };
      await fulfillJson(route, 200, jsonSuccess(bundle.detail.pullRequest));
      return;
    }

    if (pathname.startsWith('/api/tasks/') && method === 'GET') {
      const bundle = taskBundles.get(pathname.split('/')[3]);
      await fulfillJson(route, bundle ? 200 : 404, bundle ? jsonSuccess(bundle.detail) : jsonError('Task not found'));
      return;
    }

    await fulfillJson(route, 501, jsonError(`Unhandled mock for ${method} ${pathname}`));
  });

  return {
    user,
    get authenticated() {
      return isAuthenticated;
    },
  };
}
