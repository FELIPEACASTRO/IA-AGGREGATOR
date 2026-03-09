# ARCHITECTURE — Arquitetura da Solução

> Documento técnico detalhado da arquitetura do IA-AGGREGATOR / Lume Codex Cloud.
> Versão: 1.0.0 | Última atualização: 2026-03-08

---

## Visão Geral do Sistema

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            BROWSER (Cliente)                            │
│  React 19 + Next.js 15 App Router + Zustand + TanStack Query           │
└─────────────────────────┬───────────────────────────────────────────────┘
                          │ HTTPS (porta 3000)
                          ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     NEXT.JS 15 FRONTEND (:3000)                         │
│                                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────────────┐ │
│  │  App Router   │  │  API Routes  │  │  Server-Side (codex runtime)  │ │
│  │  Pages/Layout │  │  /api/*      │  │  - auth.ts (JWT decode)       │ │
│  │  Components   │  │  - tasks     │  │  - http.ts (workspace ctx)    │ │
│  │  Stores       │  │  - envs      │  │  - queue.ts (BullMQ)          │ │
│  │  (Zustand)    │  │  - webhooks  │  │  - task-runner.ts (worker)    │ │
│  │               │  │  - auth      │  │  - redis.ts (IORedis)         │ │
│  └──────────────┘  └──────┬───────┘  └───────────┬───────────────────┘ │
│                           │                       │                     │
│                    Prisma ORM              BullMQ + SSE                 │
│                           │                       │                     │
└───────────────────────────┼───────────────────────┼─────────────────────┘
                            │                       │
          ┌─────────────────┼───────────────────────┼──────────────────┐
          │                 ▼                       ▼                  │
          │  ┌──────────────────────┐  ┌────────────────────────┐     │
          │  │  PostgreSQL 16 (:5432)│  │    Redis 7 (:6379)     │     │
          │  │  (pgvector)          │  │    (Alpine)             │     │
          │  │                      │  │                         │     │
          │  │  Schema: codex       │  │  - BullMQ queues        │     │
          │  │  (Prisma)            │  │  - Refresh token store  │     │
          │  │                      │  │  - Cache (pendente)     │     │
          │  │  Schemas: auth,      │  │                         │     │
          │  │  billing, analytics  │  └────────────────────────┘     │
          │  │  (Flyway/JPA)        │                                  │
          │  └──────────────────────┘                                  │
          │              INFRAESTRUTURA (Docker Compose)                │
          └────────────────────────────────────────────────────────────┘
                            │
          Rewrite /api/v1/* │
                            ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                   SPRING BOOT BACKEND (:8080)                           │
│                                                                         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌────────────────────────┐ │
│  │  Presentation   │  │  Application    │  │  Infrastructure        │ │
│  │  (Controllers)  │  │  (Use Cases)    │  │  (Adapters)            │ │
│  │                 │  │                 │  │                        │ │
│  │  AuthController │  │  LoginUseCase   │  │  UserRepositoryImpl    │ │
│  │  ChatController │  │  ChatUseCase    │  │  JwtTokenProvider      │ │
│  │  AnalyticsCtrl  │  │  RegisterUC     │  │  BcryptEncoder         │ │
│  │                 │  │  RefreshTokenUC │  │  RedisTokenStore       │ │
│  │  GlobalException│  │  AnalyticsUC    │  │  17x AI Providers      │ │
│  │  Handler        │  │                 │  │  MicrometerTelemetry   │ │
│  └─────────────────┘  └─────────────────┘  └────────────────────────┘ │
│                                                                         │
│  ┌─────────────────┐  ┌─────────────────────────────────────────────┐ │
│  │  Domain         │  │  Common                                     │ │
│  │  (Entities)     │  │  (Exceptions, ErrorCodes)                   │ │
│  │  User, Token    │  │  BusinessException, TechnicalException      │ │
│  │  ChatMessage    │  │  49 ErrorCodes (AUTH_*, AI_*, CHAT_*, ...)  │ │
│  └─────────────────┘  └─────────────────────────────────────────────┘ │
│                                                                         │
│                   Hexagonal / Clean Architecture                        │
│                   5 módulos Maven                                       │
└─────────────────────────┬───────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      PROVEDORES DE IA (17 Providers)                    │
│                                                                         │
│  OpenAI  │ Anthropic │ Gemini  │ OpenRouter │ Cohere │ DeepSeek        │
│  Groq    │ Mistral   │ Perplexity │ Together │ Fireworks │ xAI (Grok) │
│  Azure   │ NVIDIA    │ Cerebras │ SambaNova │ Novita                   │
│                                                                         │
│  Circuit Breaker (Resilience4j): window=20, threshold=50%              │
│  Retry: 2 tentativas, backoff 250ms                                    │
│  Timeout: 15s por provider                                              │
│  Fallback: modelo preferido -> padrão -> lista de fallback             │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Stack Tecnológico

### Frontend
| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| Next.js | 15.x | Framework React com App Router e Server Actions |
| React | 19.0.0 | Biblioteca UI |
| TypeScript | 5.7 | Tipagem estática |
| Tailwind CSS | 4.0.0 | Sistema de design utility-first |
| Zustand | 5.0.0 | Gerenciamento de estado (4 stores) |
| TanStack Query | 5.90 | Cache e sincronização de dados do servidor |
| Prisma | 6.16.2 | ORM para PostgreSQL (schema codex) |
| BullMQ | 5.70.4 | Fila de tarefas com Redis |
| Axios | 1.7 | Cliente HTTP com interceptors JWT |
| Zod | 3.24 | Validação de schemas em runtime |
| React Hook Form | 7.54 | Gerenciamento de formulários |
| next-intl | 4.4.0 | Internacionalização |
| Framer Motion | 11.18.2 | Animações |
| Playwright | 1.53.2 | Testes E2E |
| Jest | 29.7.0 | Testes unitários |

### Backend
| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| Spring Boot | 3.4.3 | Framework Java |
| Java | 21 | Linguagem |
| Spring Security | 6.x | Autenticação e autorização |
| JJWT | 0.12.6 | Geração e validação de JWT |
| Resilience4j | 2.2.0 | Circuit breaker e retry |
| Flyway | 10.20.1 | Migrações de banco de dados |
| Spring Data JPA | 3.x | Acesso a dados com Hibernate |
| MapStruct | 1.6.3 | Mapeamento de objetos |
| Micrometer | 1.13.6 | Métricas (Prometheus) |
| SpringDoc | 2.6.0 | Documentação OpenAPI/Swagger |

### Infraestrutura
| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| PostgreSQL | 16 (pgvector) | Banco de dados principal |
| Redis | 7 (Alpine) | Filas BullMQ + refresh tokens |
| Docker Compose | — | Orquestração local |
| GitHub Actions | — | CI/CD |

---

## Fluxos de Dados Principais

### 1. Fluxo de Autenticação

```
[Browser]
  │ POST /login (email, password)
  ▼
[Next.js Page] → [Zustand auth-store.login()]
  │ POST /api/v1/auth/login
  ▼
[Next.js Rewrite] → proxies to Spring Boot
  │
  ▼
[Spring Boot AuthController]
  │ LoginUseCaseImpl:
  │  1. Valida credenciais (BCrypt)
  │  2. Verifica brute-force lock (5 tentativas → 30min)
  │  3. Gera accessToken (JWT, 15min)
  │  4. Gera refreshToken (JWT, 7d) + salva ID no Redis
  │  5. Retorna tokens
  ▼
[Next.js API Route /api/auth/login]
  │ Define cookies: access_token + refresh_token
  │ ⚠️ ATUALMENTE: httpOnly=false, secure=false (GAP-002)
  ▼
[Browser]
  │ localStorage: tokens (via auth-store)
  │ Cookies: access_token + refresh_token
  │
  │ GET /api/v1/auth/me (com Bearer token)
  ▼
[Spring Boot] → Retorna perfil do usuário
  │
  ▼
[Zustand auth-store] → user, isAuthenticated=true
```

### 2. Fluxo de Criação de Task (Codex)

```
[Browser - TaskComposer]
  │ Formulário: título, prompt, modo (ASK/CODE), environmentId, repositoryId
  ▼
[codexApi.createTask(payload)]
  │ POST /api/tasks
  │ Cookie: access_token
  ▼
[Next.js API Route /api/tasks]
  │ 1. requireCodexContext() → valida JWT, extrai workspace
  │ 2. Zod validation do body
  │ 3. prisma.task.create() → status: 'queued'
  │ 4. taskQueue.add('run-task', { taskId })
  ▼
[BullMQ Queue (Redis)]
  │ Job enqueued com taskId
  ▼
[BullMQ Worker (task-runner.ts)]
  │ Execução sequencial por fases:
  │
  │ FASE 1: provisioning
  │   → Cria diretório sandbox em .codex-runtime/tasks/{taskId}
  │   → Emite TaskEvent: provisioning.started
  │
  │ FASE 2: repo_download
  │   → git clone do repositório (se configurado)
  │   → Emite TaskEvent: repository.cloned
  │
  │ FASE 3: setup
  │   → Executa setupScript do Environment
  │   → Emite TaskEvent: setup.completed
  │
  │ FASE 4: maintenance
  │   → Executa maintenanceScript
  │
  │ FASE 5: agent
  │   → Executa o agente de IA com o prompt
  │   → Emite TaskEvent: agent.progress (múltiplos)
  │
  │ FASE 6: validation
  │   → Roda testes/linting
  │   → Emite TaskEvent: validation.completed
  │
  │ FASE 7: pr_push
  │   → Gera diff, cria PR se configurado
  │   → Emite TaskEvent: diff.ready, pr.created
  │
  │ → task.status = 'completed'
  │ → Emite TaskEvent: task.completed
  ▼
[SSE Endpoint /api/tasks/:id/events]
  │ ReadableStream com polling do TaskEvent
  │ Heartbeat: ':heartbeat\n\n' a cada 15s
  ▼
[Browser - LiveLogViewer / TaskTimeline]
  │ EventSource escuta eventos em tempo real
  │ UI atualiza automaticamente
```

### 3. Fluxo de Chat com IA (Multi-Provider)

```
[Browser - ChatCanvasBoard]
  │ Mensagem do usuário
  ▼
[Zustand chat-store.sendMessage()]
  │ POST /api/v1/ai/chat
  │ Body: { prompt, preferredModel }
  ▼
[Spring Boot ChatController]
  │
  ▼
[ChatUseCaseImpl]
  │ 1. Prompt Guardrails
  │    → Regex para jailbreak, injection (max 5000 chars)
  │    → Se bloqueado: throw BusinessException
  │
  │ 2. Routing Policy
  │    → resolveOrderedModels(): preferido → padrão → fallbacks
  │    → Ex: [gpt-4o-mini, claude-3-5-haiku, gemini-1.5-flash]
  │
  │ 3. Para cada modelo na lista:
  │    3a. Encontra providers que suportam o modelo
  │    3b. Para cada provider:
  │        → CircuitBreaker.decorateSupplier()
  │        → executeWithRetry(2 tentativas, 250ms backoff)
  │        → HTTP POST para API do provider
  │        → Se sucesso: break
  │        → Se falha: telemetry.recordFailure(), próximo provider
  │    3c. Se todos providers falharam: próximo modelo
  │
  │ 4. Output Guardrails
  │    → Regex para api keys, tokens (max 8000 chars)
  │
  │ 5. Telemetry
  │    → recordSuccess(model, provider, fallbackUsed, attempts)
  │
  │ 6. Retorna: { content, modelUsed, providerUsed, fallbackUsed, attempts }
  ▼
[chat-store]
  │ ⚠️ ATUALMENTE: Simula streaming (4 chars/12ms) (GAP-007)
  │ Deveria usar SSE/ReadableStream
  ▼
[Browser - MessageContent + AssistantMarkdown]
```

### 4. Fluxo de Webhook (GitHub/Slack/Linear)

```
[GitHub/Slack/Linear]
  │ POST /api/webhooks/{provider}
  │ Headers: x-hub-signature-256 (GitHub) / x-slack-signature (Slack)
  │ ⚠️ ASSINATURA NAO VERIFICADA (GAP-003)
  ▼
[Next.js API Route /api/webhooks/{provider}]
  │ 1. Parse do JSON body
  │ 2. Identifica tipo de evento:
  │    GitHub: pull_request, push, installation
  │    Slack: app_mention, message
  │    Linear: Issue.create, Comment.create
  │ 3. Busca workspace/environment associado
  │ 4. Cria Task automaticamente (se aplicável)
  │ 5. Registra evento em {Provider}EventLog
  ▼
[Prisma] → Persiste task + event log
[BullMQ] → Enfileira task para execução
```

---

## Hierarquia de Componentes React

```
App Root (layout.tsx)
├── ThemeProvider (Zustand: theme-store)
├── AnalyticsProvider (lib/analytics.ts)
├── ToastViewport (Zustand: toast-store)
│
├── [Público] AuthShell (auth-shell.tsx)
│   ├── /login → Login Page
│   ├── /register → Register Page
│   └── /logout → Logout Handler
│
├── [Protegido] AppShell (app-shell.tsx)
│   ├── Navigation Sidebar
│   │
│   ├── /codex → CodexShell (codex-shell.tsx)
│   │   ├── TaskComposer (task-composer.tsx)
│   │   │   └── React Hook Form + Zod
│   │   ├── TaskList (task-list.tsx)
│   │   │   └── Filtros, paginação, ações
│   │   └── /codex/tasks/[taskId] → Task Detail
│   │       ├── TaskTimeline (task-timeline.tsx)
│   │       ├── /logs → LiveLogViewer (live-log-viewer.tsx)
│   │       ├── /diff → DiffViewer (diff-viewer.tsx)
│   │       ├── /tests → Test Results
│   │       ├── /artifacts → Artifacts List
│   │       └── /pull-request → PullRequestPanel (pull-request-panel.tsx)
│   │
│   ├── /chat → ChatCanvasBoard (chat-canvas-board.tsx) [lazy loaded]
│   │   ├── MessageContent (message-content.tsx)
│   │   └── AssistantMarkdown (assistant-markdown.tsx)
│   │
│   ├── /codex/settings/* → Configurações
│   │   ├── /connectors → GitHub/Slack/Linear Setup
│   │   ├── /environments → CRUD de Ambientes
│   │   ├── /code-review → Políticas de Review
│   │   ├── /usage → Dashboard de Uso
│   │   └── /analytics → Relatórios
│   │
│   └── /admin/settings → Painel Admin
│
└── UI Components (14 componentes reutilizáveis)
    ├── Button, Input, Textarea, FormField
    ├── Modal (com focus-trap), Dropdown
    ├── Alert, Badge, Avatar, Tooltip
    ├── CodeBlock, ProgressBar, Skeleton
    └── CommandPalette (Cmd+K)
```

---

## Stores Zustand (Estado Client-Side)

| Store | Persistência | Estado Principal | Métodos Chave |
|-------|-------------|-----------------|---------------|
| `auth-store` | Sessão (não persistido) | `user`, `isAuthenticated`, `isLoading` | `login()`, `register()`, `logout()`, `fetchUser()` |
| `chat-store` | localStorage | `conversations[]`, `activeConversationId`, `selectedModel` | `sendMessage()`, `createConversation()`, `stopGenerating()` |
| `theme-store` | localStorage | `isDarkMode` | `toggleTheme()` |
| `toast-store` | Sessão | `toasts[]` | `add()`, `success()`, `error()`, `info()` |

---

## Modelo de Dados (Dual Schema)

### Schema `codex` (Prisma — Frontend)

Gerenciado por `frontend/prisma/schema.prisma` com `npx prisma db push`.

**50+ modelos** organizados em 8 categorias:
- **Identidade**: User, Workspace, Membership, Session, OAuthConnection
- **GitHub**: GitHubInstallation, GitRepository, RepositoryPermission, RepositoryBranchCache
- **Ambiente**: Environment, EnvironmentRepoMap, EnvironmentRuntimePin, EnvironmentVariable, EnvironmentSecret, EnvironmentCache, InternetPolicy, EnvironmentExecutionHistory
- **Execução**: Task, TaskInput, TaskRun, TaskFollowUp, TaskEvent, TaskLogChunk
- **Evidência**: TaskArtifact, TaskEvidenceCitation, DiffSnapshot, DiffFile, DiffHunk, PullRequest, TaskArchiveRecord
- **Review**: CodeReviewPolicy, GitHubReviewRun, ReviewFinding, ReviewGuidelineSource
- **Conectores**: SlackInstall, SlackWorkspaceBinding, SlackEventLog, LinearInstall, LinearWorkspaceBinding, LinearEventLog, TriageRule
- **Billing/Admin**: UsageEntry, CreditBalance, CreditLedgerEntry, BillingIntent, AuditLog, AnalyticsAggregate, ComplianceExportJob, ManagedConfig, AssumptionLog

### Schemas `auth`, `analytics`, etc. (Flyway — Backend)

Gerenciado por `backend/.../db/migration/V*.sql` com Flyway.

**Tabelas implementadas**:
- `auth.users`, `auth.organizations`, `auth.user_identities`, `auth.sessions`
- `auth.api_keys`, `auth.consent_records`, `auth.erasure_requests`
- `auth.password_reset_tokens`, `auth.email_verification_tokens`
- `auth.notification_preferences`, `auth.feature_flags`
- `analytics.event_reports`, `analytics.ingested_events`

> **ATENÇÃO (GAP-005)**: Não há coordenação entre migrações Prisma e Flyway. Modelos de `User` existem em ambos os schemas.

---

## Decisões Arquiteturais (ADRs)

### ADR-001: Monorepo com Frontend como BFF
- **Decisão**: Next.js serve como Backend-for-Frontend (BFF) com Prisma para o módulo Codex, enquanto Spring Boot serve como backend de autenticação e IA.
- **Motivação**: Velocidade de desenvolvimento com Server Components e API Routes.
- **Trade-off**: Dois ORMs para o mesmo banco (Prisma + JPA), risco de schema drift.

### ADR-002: BullMQ para Execução de Tasks
- **Decisão**: Tasks executam em worker BullMQ dentro do processo Next.js.
- **Motivação**: Simplicidade; evita microsserviço separado no MVP.
- **Trade-off**: Worker compartilha memória com o servidor web; não escala horizontalmente sem refatoração.

### ADR-003: 17 Provedores de IA com Fallback
- **Decisão**: Cada modelo pode ser servido por múltiplos providers com circuit breaker e retry.
- **Motivação**: Alta disponibilidade; se OpenAI cai, Anthropic assume automaticamente.
- **Trade-off**: Complexidade de configuração (17 API keys); custo de manutenção.

### ADR-004: JWT Stateless + Redis para Refresh
- **Decisão**: Access token é JWT stateless (15min); refresh token tem ID rastreado no Redis (7 dias).
- **Motivação**: Baixa latência para autenticação; revogação de refresh tokens.
- **Trade-off**: Access token não pode ser revogado antes da expiração.

### ADR-005: SSE para Eventos de Task em Tempo Real
- **Decisão**: Server-Sent Events via `ReadableStream` com polling do banco.
- **Motivação**: Simplicidade vs WebSockets; suporte nativo em browsers.
- **Trade-off**: Polling do banco a cada 2s; não escala para milhares de conexões simultâneas.

---

## Segurança — Visão Geral

Ver [SECURITY.md](./SECURITY.md) para detalhes completos.

| Camada | Implementado | Pendente |
|--------|-------------|----------|
| Autenticação | JWT + BCrypt + Refresh tokens | Verificação de assinatura no frontend (GAP-001) |
| Autorização | Workspace scoping | RBAC por role (GAP-013) |
| Cookies | SameSite=Lax | HttpOnly + Secure (GAP-002) |
| Webhooks | Parse de eventos | Verificação HMAC (GAP-003) |
| Input Validation | Zod schemas + Jakarta Bean | Sanitização HTML (GAP-010) |
| Rate Limiting | — | Implementação server-side (GAP-008) |
| Secrets | EnvironmentSecret | Encryption at-rest |
| LGPD | Tabelas de consentimento | Workflows automatizados (GAP-025) |

---

## Observabilidade

| Componente | Ferramenta | Status |
|-----------|-----------|--------|
| Métricas Backend | Micrometer + Prometheus | Implementado |
| Métricas AI Routing | MicrometerAiRoutingTelemetryAdapter | Implementado |
| Health Checks | Spring Actuator (`/actuator/health`) | Implementado |
| API Docs | SpringDoc OpenAPI (`/swagger-ui`) | Implementado |
| Analytics Frontend | Custom (localStorage → flush) | Implementado |
| Logging Backend | SLF4J + Logback | Implementado |
| Alerting | — | Pendente |
| Tracing Distribuído | — | Pendente |

---

## Referências

- [PRODUCT_SPEC.md](./PRODUCT_SPEC.md) — Visão do produto e personas
- [DATA_MODEL.md](./DATA_MODEL.md) — Modelo de dados detalhado
- [API_CONTRACT.md](./API_CONTRACT.md) — Contratos de API
- [SECURITY.md](./SECURITY.md) — Controles de segurança
- [ROADMAP.md](./ROADMAP.md) — Gaps e melhorias priorizados
- [DEPLOYMENT.md](./DEPLOYMENT.md) — Guia de implantação
