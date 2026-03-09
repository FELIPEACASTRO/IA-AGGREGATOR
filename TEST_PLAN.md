# TEST_PLAN — Estrategia e Plano de Testes

> Cobertura atual, gap analysis, frameworks e roadmap de automacao.
> Versao: 2.0.0 | Ultima atualizacao: 2026-03-08

---

## Frameworks e Ferramentas

| Ferramenta | Uso | Configuracao |
|-----------|-----|-------------|
| Jest 29 | Unit tests frontend (stores, libs, pages) | `frontend/jest.config.js` |
| Testing Library | Render e interacao com componentes React | `@testing-library/react` v16 |
| Playwright 1.53 | E2E tests + visual regression + a11y | `frontend/playwright.config.ts` |
| JUnit 5 | Unit tests backend (use cases, domain, infra) | Maven Surefire |
| Mockito | Mocks backend | `MockitoExtension` |
| MockWebServer | Testes HTTP para AI providers | OkHttp MockWebServer |
| ArchUnit | Validacao arquitetural (Clean Architecture) | Maven Verify |
| TestContainers | Testes com PostgreSQL real (profile `test`) | `testcontainers` 1.20.3 |

---

## Cobertura Atual

### Frontend — Unit/Integration (Jest)

| Arquivo | Describe Blocks | Tests | Tipo |
|---------|----------------|-------|------|
| `src/lib/analytics.test.ts` | 1 | 3 | Lib utility |
| `src/lib/api.test.ts` | 1 | 5 | API error handling |
| `src/stores/chat-store.test.ts` | 1 | 4 | Store Zustand |
| `src/stores/toast-store.test.ts` | 1 | 1 | Store Zustand |
| `src/app/billing/page.test.tsx` | 1 | 1 | Page component |
| `src/app/chat/page.test.tsx` | 1 | 4 | Page component |
| `src/app/library/page.test.tsx` | 1 | 4 | Page component |
| `src/app/login/page.test.tsx` | 1 | 3 | Page component |
| `src/app/prompts/page.test.tsx` | 1 | 1 | Page component |
| `src/app/register/page.test.tsx` | 1 | 4 | Page component |
| `src/app/settings/page.test.tsx` | 1 | 2 | Page component |
| `src/app/settings/analytics/page.test.tsx` | 1 | 15 | Page component |
| **Total** | **12** | **47** | — |

### Frontend — E2E (Playwright)

| Arquivo | Tests | Tipo |
|---------|-------|------|
| `e2e/ai-provider.integration.spec.ts` | 1 | Integracao IA (condicional) |
| `e2e/analytics-filters.spec.ts` | 1 | Filtros analytics |
| `e2e/analytics.spec.ts` | 1 | Relatorios analytics |
| `e2e/auth-navigation.spec.ts` | 1 | Registro + navegacao |
| `e2e/chat.spec.ts` | 1 | Chat com assistente |
| `e2e/quality-gates.spec.ts` | 4 | Visual + keyboard |
| **Total** | **9** | — |

**Configuracao Playwright**:
- Browser: Chromium apenas
- Workers: 1 (execucao serial)
- Timeout: 60s
- Retries: 0 local / 2 CI
- Artifacts: HTML report, traces, screenshots e videos em falha
- Base URL: `http://localhost:3001`

### Backend — Unit Tests (JUnit 5)

| Camada | Arquivo | Tests | Tipo |
|--------|---------|-------|------|
| Domain | `UserTest.java` | 6+ | Entidade |
| Domain | `AuthProviderTest.java` | 2+ | Value Object |
| Domain | `UserRoleTest.java` | 2+ | Value Object |
| Domain | `UserStatusTest.java` | 2+ | Value Object |
| Application | `ChatUseCaseImplTest.java` | 3 | Use Case |
| Application | `LoginUseCaseImplTest.java` | 3+ | Use Case |
| Application | `RegisterUserUseCaseImplTest.java` | 3+ | Use Case |
| Application | `GetCurrentUserUseCaseImplTest.java` | 2+ | Use Case |
| Application | `RefreshTokenUseCaseImplTest.java` | 2+ | Use Case |
| Infra | 17x `*ProviderMockWebServerTest.java` | 3+ cada | AI Provider |
| Infra | `ConfigurableOutputGuardrailAdapterTest.java` | 3+ | Guardrail |
| Infra | `ConfigurablePromptGuardrailAdapterTest.java` | 3+ | Guardrail |
| Infra | `MicrometerAiRoutingTelemetryAdapterTest.java` | 2+ | Telemetria |
| Infra | `AuthProviderConverterTest.java` | 2+ | Converter |
| Infra | `UserRoleConverterTest.java` | 2+ | Converter |
| Infra | `UserStatusConverterTest.java` | 2+ | Converter |
| Infra | `UserPersistenceMapperTest.java` | 2+ | Mapper |
| Infra | `UserDetailsServiceImplTest.java` | 2+ | Security |
| Presentation | `AiChatControllerTest.java` | 4 | Controller |
| Presentation | `AnalyticsControllerTest.java` | 3+ | Controller |
| Presentation | `AnalyticsIngestionServiceTest.java` | 2+ | Service |
| Presentation | `AuthControllerTest.java` | 4+ | Controller |
| Presentation | `GlobalExceptionHandlerTest.java` | 3+ | Exception |
| **Total** | **39 arquivos** | **~100+** | — |

---

## Resumo Consolidado

| Area | Unit | Integration | E2E | Visual | Total |
|------|------|------------|-----|--------|-------|
| Frontend (libs) | 8 | 0 | 0 | 0 | 8 |
| Frontend (stores) | 5 | 0 | 0 | 0 | 5 |
| Frontend (pages) | 34 | 0 | 0 | 0 | 34 |
| Frontend (E2E) | 0 | 0 | 5 | 4 | 9 |
| Backend (domain) | 12+ | 0 | 0 | 0 | 12+ |
| Backend (application) | 13+ | 0 | 0 | 0 | 13+ |
| Backend (infra) | 65+ | 0 | 0 | 0 | 65+ |
| Backend (presentation) | 16+ | 0 | 0 | 0 | 16+ |
| **Total** | **153+** | **0** | **5** | **4** | **162+** |

---

## Gap Analysis

### Areas Sem Cobertura

| Area | Tipo Necessario | Prioridade | Impacto |
|------|----------------|-----------|---------|
| API Routes Codex (`/api/tasks`, `/api/environments`, etc.) | Integration | CRITICO | 44+ endpoints sem teste |
| Webhook handlers (`/api/webhooks/*`) | Integration | CRITICO | Payloads nao validados |
| Auth middleware (`codex/auth.ts`) | Unit | CRITICO | JWT sem verificacao |
| Task Runner (`task-runner.ts`) | Integration | ALTO | Pipeline de execucao |
| SSE endpoint (`/api/tasks/{id}/events`) | Integration | ALTO | Realtime nao testado |
| Prisma queries (all stores) | Integration | ALTO | Data layer |
| Zustand stores (`auth-store`) | Unit | MEDIO | Auth store sem teste |
| Codex UI components | Component | MEDIO | Shell, composer, timeline |
| BullMQ queue (`queue.ts`) | Integration | MEDIO | Enqueue/dequeue |
| RBAC por role | Integration | MEDIO | Autorizacao incompleta |
| Acessibilidade (WCAG 2.1) | A11y | BAIXO | Nao-conformidade |
| Performance (LCP, FID) | Performance | BAIXO | Sem baseline |

### Cobertura por Capacidade (PARITY_MATRIX)

| Capacidade | Tem Testes? | Gap |
|-----------|------------|-----|
| Tasks cloud (cap. 1) | Nao | Integration tests |
| Modos Ask/Code (cap. 2) | Nao | Unit tests |
| SSE realtime (cap. 3) | Nao | E2E tests |
| Logs com fases (cap. 4) | Nao | Integration tests |
| Review de diff (cap. 5) | Nao | Integration tests |
| Evidencia de testes (cap. 6) | Nao | Integration tests |
| Artefatos (cap. 7) | Nao | Integration tests |
| PR create/update (cap. 8) | Nao | Integration tests |
| Follow-up (cap. 9) | Nao | Integration tests |
| Retry/Cancel/Archive (cap. 10) | Nao | Integration tests |
| Environments CRUD (cap. 11) | Nao | Integration tests |
| Internet policy (cap. 12) | Nao | Unit tests |
| GitHub review (cap. 14) | Nao | Integration tests |
| Slack delegation (cap. 15) | Nao | Integration tests |
| Linear delegation (cap. 16) | Nao | Integration tests |
| Usage dashboard (cap. 17) | Nao | Integration tests |
| Credits flow (cap. 18) | Nao | Integration tests |
| Analytics (cap. 19) | Sim (15 unit + 2 E2E) | — |
| Chat multi-provider (cap. 22) | Sim (3 use case + 17 provider) | — |
| Auth JWT (cap. 23) | Sim (4 use case + 4 E2E) | — |

---

## Quality Gates (CI)

```bash
# Pipeline completo frontend
npm run quality:ci
# Equivale a:
# 1. npm run lint              → ESLint + token validation
# 2. npm run type-check        → TypeScript strict
# 3. npm run test -- --runInBand → Jest unit tests
# 4. npm run build             → Next.js production build
# 5. npm run perf:budget       → Route size budgets
# 6. npm run test:quality      → Visual regression + keyboard a11y

# Pipeline backend
mvn clean verify
# Inclui: compile → test → ArchUnit → package
```

### Visual Regression

Snapshots baseline em `e2e/quality-gates.spec.ts-snapshots/`:
- `landing-desktop.png`
- `login-desktop.png`
- `register-desktop.png`
- `home-desktop.png`
- `analytics-desktop.png`

### Keyboard Accessibility

- Teste de smoke em pagina de login
- Verifica navegacao via Tab e ativacao via Enter

---

## Mocks e Test Helpers

### Frontend

| Arquivo | Descricao |
|---------|-----------|
| `frontend/test/mocks/rehype-highlight.ts` | Mock para syntax highlighting |
| `frontend/test/mocks/remark-gfm.ts` | Mock para GitHub Flavored Markdown |
| `frontend/jest.setup.ts` | Setup global: jest-dom, scroll mock, next-intl |
| `frontend/e2e/support/auth.ts` | Helpers: `createRandomUser`, `mockAuthApi`, `registerUserViaUi`, `loginUserViaUi` |

### Backend

| Ferramenta | Uso |
|-----------|-----|
| MockitoExtension | Mocks de dependencias em use cases |
| MockWebServer | HTTP mock para 17 AI providers |
| @WebMvcTest | Testes de controller isolados |
| TestContainers | PostgreSQL real em profile `test` |

---

## Roadmap de Automacao

### Sprint 1-2: Fundacao (Prioridade CRITICA)

| Teste | Tipo | Arquivo Alvo |
|-------|------|-------------|
| Auth middleware verifica JWT | Unit | `codex/auth.ts` |
| Webhook HMAC validation | Unit | `webhooks/*/route.ts` |
| Task CRUD endpoints | Integration | `/api/tasks/route.ts` |
| Environment CRUD endpoints | Integration | `/api/environments/route.ts` |
| Task status transitions | Unit | State machine helpers |

### Sprint 3-4: Pipeline (Prioridade ALTA)

| Teste | Tipo | Arquivo Alvo |
|-------|------|-------------|
| Task Runner pipeline completo | Integration | `task-runner.ts` |
| BullMQ enqueue/dequeue | Integration | `queue.ts` |
| SSE event stream | Integration | `/api/tasks/{id}/events` |
| PR create/update | Integration | `/api/tasks/{id}/pull-request` |
| Webhook → Task creation | Integration | `webhooks/github/route.ts` |

### Sprint 5-6: Cobertura (Prioridade MEDIA)

| Teste | Tipo | Arquivo Alvo |
|-------|------|-------------|
| Codex UI components | Component | Shell, Composer, Timeline |
| Auth store | Unit | `auth-store.ts` |
| Credits/billing flow | Integration | `/api/credits/*` |
| Code review policy | Integration | `/api/code-review/*` |
| WCAG 2.1 AA compliance | A11y (axe) | Todas as paginas |

### Sprint 7-8: Robustez (Prioridade BAIXA)

| Teste | Tipo | Arquivo Alvo |
|-------|------|-------------|
| Performance baselines (LCP) | Performance | Lighthouse CI |
| Load testing | Performance | k6 ou Artillery |
| Contract tests (API) | Contract | Pact ou similar |
| Chaos testing (Redis down) | Resilience | Task Runner |

---

## Metricas de Sucesso

| Metrica | Atual | Meta Q2 | Meta Q4 |
|---------|-------|---------|---------|
| Unit tests (frontend) | 47 | 100+ | 200+ |
| Unit tests (backend) | 100+ | 150+ | 250+ |
| Integration tests | 0 | 30+ | 80+ |
| E2E tests | 9 | 20+ | 40+ |
| Visual snapshots | 5 | 15+ | 30+ |
| A11y tests | 1 (keyboard) | 10+ | 25+ |
| Cobertura linhas (frontend) | ~15% | 50% | 80% |
| Cobertura linhas (backend) | ~30% | 60% | 85% |
| Tempo CI pipeline | ~10min | <8min | <6min |

---

## Executando Testes

```bash
# Frontend — Unit
npm --prefix frontend run test                    # Todos
npm --prefix frontend run test -- --watch         # Watch mode
npm --prefix frontend run test -- path/to/test    # Arquivo especifico

# Frontend — E2E
npm --prefix frontend run test:e2e                # Headless
npm --prefix frontend run test:e2e:headed         # Com browser visivel
npm --prefix frontend run test:e2e:ui             # Playwright UI mode

# Frontend — Quality Gates
npm --prefix frontend run test:quality            # Visual + keyboard
npm --prefix frontend run test:quality:update     # Atualizar snapshots

# Backend
cd backend && mvn test                            # Todos
cd backend && mvn test -pl ia-aggregator-application  # Modulo especifico
cd backend && mvn verify                          # Testes + ArchUnit
```

---

## Referencias

- [PARITY_MATRIX.md](./PARITY_MATRIX.md) — Status de cobertura por capacidade
- [ROADMAP.md](./ROADMAP.md) — Gaps de teste priorizados
- [CONTRIBUTING.md](./CONTRIBUTING.md) — Como escrever testes
- [DEPLOYMENT.md](./DEPLOYMENT.md) — CI pipelines
