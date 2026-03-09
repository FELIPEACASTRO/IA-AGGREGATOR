# PARITY_MATRIX — Matriz de Paridade de Funcionalidades

> Rastreamento de completude de cada capacidade da plataforma IA-AGGREGATOR / Lume Codex Cloud.
> Versao: 2.0.0 | Ultima atualizacao: 2026-03-08

---

## Legenda

- **Complete**: Producao-ready com testes
- **Partial**: Caminho principal implementado, falta hardening/testes
- **Behind flag**: Scaffold/modelo apenas
- **Not started**: Nao iniciado

---

## Matriz de Capacidades

| # | Capacidade | UI | API | Data Model | Testes | Bloqueadores | Status |
|---|-----------|-----|-----|-----------|--------|-------------|--------|
| 1 | Tasks cloud paralelas/background | /codex, /codex/tasks/:id | POST /api/tasks + BullMQ | Task, TaskRun, TaskEvent | 0 integration | Sem timeout global | Partial |
| 2 | Modos Ask/Code | Composer + detalhes | POST /api/tasks | Task.mode, TaskInput | 0 unit | — | Partial |
| 3 | Eventos realtime (SSE) | Task details/logs | GET /events (SSE) | TaskEvent | 0 e2e | Polling 2s ao banco | Partial |
| 4 | Logs com fases | /tasks/:id/logs | GET /logs | TaskLogChunk | 0 integration | — | Partial |
| 5 | Review de diff | /tasks/:id/diff | GET /diff | DiffSnapshot/File/Hunk | 0 integration | — | Partial |
| 6 | Evidencia de testes | /tasks/:id/tests | GET /tests | TaskLogChunk (validation) | 0 integration | — | Partial |
| 7 | Artefatos | /tasks/:id/artifacts | GET /artifacts | TaskArtifact | 0 integration | — | Partial |
| 8 | PR create/update | /tasks/:id/pull-request | POST/PATCH PRs | PullRequest | 0 integration | OAuth mock (GAP-004) | Partial |
| 9 | Follow-up | Task summary | POST /followups | TaskFollowUp | 0 integration | — | Partial |
| 10 | Retry/Cancel/Archive | Task list/actions | /retry /cancel /archive | TaskArchiveRecord | 0 integration | — | Partial |
| 11 | Environments CRUD | /settings/environments* | /api/environments* | Environment + related | 0 integration | — | Partial |
| 12 | Internet policy | Env forms + badges | env create/update | InternetPolicy | 0 unit | Network sandbox pendente | Partial |
| 13 | AGENTS.md support | N/A (planejado) | N/A | ReviewGuidelineSource | 0 | Feature flag nao conectada | Behind flag |
| 14 | GitHub review policy | /settings/code-review | /api/code-review/* | CodeReviewPolicy, ReviewRun | 0 integration | — | Partial |
| 15 | Slack delegation | Connectors + webhook | POST /webhooks/slack | Slack* models | 0 integration | Sem HMAC (GAP-003) | Partial |
| 16 | Linear delegation | Connectors + webhook | POST /webhooks/linear | Linear* models | 0 integration | Sem HMAC (GAP-003) | Partial |
| 17 | Usage dashboard | /settings/usage | GET /api/usage | UsageEntry, CreditBalance | 0 integration | — | Partial |
| 18 | Credits flow | /settings/usage/credits | GET/POST /credits* | CreditLedger, BillingIntent | 0 integration | Sem Stripe (GAP-021) | Partial |
| 19 | Analytics/admin | /settings/analytics, /admin | /api/analytics, configs* | AnalyticsAgg, ManagedConfig | 0 integration | — | Partial |
| 20 | Shortcuts + deep links | /codex/shortcuts + params | N/A | N/A | 0 e2e | — | Partial |
| 21 | Voice + image inputs | Composer | POST /api/tasks | TaskInput.image/voice | 0 e2e | — | Partial |
| 22 | Chat multi-provider | /chat | POST /api/v1/ai/chat | — (backend) | 5 unit (backend) | Streaming fake (GAP-007) | Partial |
| 23 | Autenticacao JWT | /login, /register | /api/v1/auth/* | auth.users (Flyway) | 4 unit (backend) | JWT sem verificacao (GAP-001) | Partial |
| 24 | OAuth GitHub | /oauth/github/* | /api/oauth/* | OAuthConnection | 0 | Mock token (GAP-004) | Partial |
| 25 | Billing/Pagamentos | /billing | /api/credits/purchase | BillingIntent | 0 | Sem Stripe (GAP-021) | Not started |
| 26 | LGPD compliance | — | — | consent_records, erasure | 0 | Sem workflows (GAP-025) | Behind flag |

---

## Resumo de Cobertura de Testes

| Area | Unit | Integration | E2E | Total |
|------|------|------------|-----|-------|
| Frontend (stores) | 4 | 0 | 0 | 4 |
| Frontend (libs) | 2 | 0 | 0 | 2 |
| Frontend (pages) | 0 | 0 | 6 | 6 |
| Backend (use cases) | 5 | 0 | 0 | 5 |
| Backend (providers) | 0 | 0 | 0 | 0 |
| API Routes (Codex) | 0 | 0 | 0 | 0 |
| **Total** | **11** | **0** | **6** | **17** |

> Meta: 80% de cobertura em caminhos criticos. Ver [TEST_PLAN.md](./TEST_PLAN.md).

---

## Principais Bloqueadores para "Complete"

1. **Seguranca** (GAP-001 a GAP-004): JWT, cookies, webhooks, OAuth
2. **Testes**: Zero integration tests em todo o projeto
3. **Integracao real**: Billing (Stripe), OAuth real, webhook HMAC
4. **LGPD**: Workflows de consentimento e erasure

---

## Referencias

- [ROADMAP.md](./ROADMAP.md) — Plano de correcao dos gaps
- [TEST_PLAN.md](./TEST_PLAN.md) — Estrategia de testes
- [SECURITY.md](./SECURITY.md) — Vulnerabilidades conhecidas
