# IA-AGGREGATOR / Lume Codex Cloud

> Plataforma web para execucao de tasks de codificacao em nuvem com execucao paralela/background, evidencias revisaveis, e delegacao via conectores (GitHub, Slack, Linear). Integra 17 provedores de IA com fallback automatico.

---

## Arquitetura

```
  Browser (React 19)
     │
     ▼
  ┌──────────────────────────────────────────────┐
  │  Next.js 15 (App Router)       :3000/:3001   │
  │  ├── Pages/UI (Tailwind v4, Zustand, cmdk)   │
  │  ├── API Routes (/api/tasks, /api/webhooks)   │
  │  ├── Prisma ORM → PostgreSQL (schema: codex)  │
  │  └── BullMQ Worker → Redis (task execution)   │
  └─────────────────────┬────────────────────────┘
                        │ /api/v1/* (rewrite)
                        ▼
  ┌──────────────────────────────────────────────┐
  │  Spring Boot 3.4 (Java 21)          :8080    │
  │  ├── Auth (JWT + Refresh Token + CORS)        │
  │  ├── AI Gateway (17 providers + circuit break)│
  │  ├── Analytics (ingestion + reports)           │
  │  └── Flyway → PostgreSQL (schemas: auth, ...)  │
  └──────────────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
  ┌──────────┐   ┌───────────┐   ┌───────────┐
  │PostgreSQL│   │   Redis   │   │ AI APIs   │
  │16+pgvec. │   │   7.x     │   │ 17 provid.│
  │  :5432   │   │  :6379    │   │           │
  └──────────┘   └───────────┘   └───────────┘
```

---

## Stack Tecnologica

| Camada | Tecnologias |
|--------|------------|
| Frontend | Next.js 15, React 19, TypeScript 5.7, Tailwind CSS v4 |
| State | Zustand 5, React Query 5, React Hook Form 7 |
| Backend | Spring Boot 3.4.3, Java 21, Maven, Flyway 10 |
| Banco | PostgreSQL 16 (pgvector), Prisma 6, Hibernate/JPA |
| Queue | BullMQ 5, Redis 7, ioredis 5 |
| IA | 17 providers (OpenAI, Anthropic, Gemini, DeepSeek, Groq, Mistral, ...) |
| Resiliencia | Resilience4j (circuit breakers por provider) |
| Observabilidade | Micrometer, Actuator, Prometheus endpoints |
| Testes | Jest 29, Playwright 1.53, JUnit 5, Mockito, ArchUnit |
| CI/CD | GitHub Actions (frontend-ci.yml, backend-ci.yml) |

---

## Quick Start

### Pre-Requisitos

- Node.js 22+, Java 21+, Maven 3.9+, Docker Desktop

### Setup Automatizado (Windows)

```powershell
# Boot completo
powershell -ExecutionPolicy Bypass -File .\scripts\start-solution.ps1

# Sem rebuild
powershell -ExecutionPolicy Bypass -File .\scripts\start-solution.ps1 -SkipBuild

# Parar
powershell -ExecutionPolicy Bypass -File .\scripts\stop-solution.ps1
```

### Setup Manual

```bash
# 1. Infraestrutura
docker compose up -d

# 2. Backend
cd backend
mvn clean verify -DskipTests
java -jar ia-aggregator-presentation/target/ia-aggregator-presentation-1.0.0-SNAPSHOT.jar &

# 3. Frontend
cd frontend
npm install
export CODEX_DATABASE_URL='postgresql://ia_aggregator:ia_aggregator@localhost:5432/ia_aggregator?schema=codex'
export CODEX_REDIS_URL='redis://localhost:6379'
export NEXT_PUBLIC_API_URL='http://localhost:8080'
npm run codex:bootstrap
npm run dev
```

### Endpoints

| Servico | URL |
|---------|-----|
| Frontend | http://localhost:3000 (dev) / http://localhost:3001 (prod) |
| Backend | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| API (proxied) | http://localhost:3000/api/v1/* |

---

## Variaveis de Ambiente

### Frontend (`frontend/.env.local`)

| Variavel | Obrigatoria | Descricao |
|----------|------------|-----------|
| `NEXT_PUBLIC_API_URL` | Sim | URL do backend (default: `http://localhost:8080`) |
| `CODEX_DATABASE_URL` | Sim | PostgreSQL connection string (schema codex) |
| `CODEX_REDIS_URL` | Sim | Redis URL para BullMQ |
| `CODEX_TASK_CONCURRENCY` | Nao | Workers simultaneos (default: 2) |
| `GITHUB_CLIENT_ID` | Nao | OAuth App GitHub |
| `GITHUB_CLIENT_SECRET` | Nao | OAuth App GitHub |
| `GITHUB_OAUTH_REDIRECT_URI` | Nao | Callback OAuth |

### Backend (env vars ou application.yml)

| Variavel | Obrigatoria | Descricao |
|----------|------------|-----------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | Sim | PostgreSQL (default: localhost:5432/ia_aggregator) |
| `REDIS_HOST` / `REDIS_PORT` | Sim | Redis (default: localhost:6379) |
| `JWT_SECRET` | Sim (prod) | Base64-encoded secret para JWT |
| `OPENAI_API_KEY` | Recomendado | Chave API OpenAI (provider padrao) |
| `ANTHROPIC_API_KEY` | Recomendado | Chave API Anthropic (fallback) |
| `GEMINI_API_KEY` | Recomendado | Chave API Gemini (fallback) |

Ver [RUNBOOK.md](./RUNBOOK.md) para lista completa.

---

## Principais Rotas

| Rota | Descricao |
|------|-----------|
| `/codex` | Dashboard principal — lista de tasks |
| `/codex/tasks/[taskId]` | Detalhe da task (summary, logs, diff, tests, PR) |
| `/codex/settings/environments` | CRUD de ambientes |
| `/codex/settings/connectors` | GitHub, Slack, Linear |
| `/codex/settings/code-review` | Politicas de review |
| `/codex/settings/usage` | Dashboard de uso e creditos |
| `/codex/settings/analytics` | Metricas e relatorios |
| `/admin/settings` | Configuracoes administrativas |
| `/chat` | Chat multi-provider com IA |
| `/login` | Autenticacao |
| `/register` | Registro de conta |

---

## Quality Gates

### Frontend

```bash
npm --prefix frontend run quality:ci
# Executa: lint → type-check → test → build → perf:budget → test:quality
```

| Gate | Comando | Descricao |
|------|---------|-----------|
| Lint | `npm run lint` | ESLint + validacao tokens Tailwind |
| Types | `npm run type-check` | TypeScript strict |
| Unit Tests | `npm run test` | Jest (47 tests) |
| Build | `npm run build` | Next.js production |
| Perf Budget | `npm run perf:budget` | Route size limits |
| Visual + a11y | `npm run test:quality` | Playwright snapshots + keyboard |
| E2E | `npm run test:e2e` | Playwright (9 specs) |

### Backend

```bash
cd backend && mvn clean verify
# Executa: compile → test → ArchUnit → package
```

---

## Provedores de IA

| # | Provider | Modelo Padrao | Status |
|---|---------|--------------|--------|
| 1 | OpenAI | gpt-4o-mini | Ativo (default) |
| 2 | Anthropic | claude-3-5-haiku | Ativo (fallback 1) |
| 3 | Google Gemini | gemini-1.5-flash | Ativo (fallback 2) |
| 4 | OpenRouter | multi-model | Ativo |
| 5 | Cohere | command-r | Ativo |
| 6 | DeepSeek | deepseek-chat | Ativo |
| 7 | Groq | llama-3.1 | Ativo |
| 8 | Mistral | mistral-small | Ativo |
| 9 | Perplexity | sonar | Ativo |
| 10 | Together AI | llama variants | Ativo |
| 11 | Fireworks AI | llama variants | Ativo |
| 12 | XAI/Grok | grok-2-latest | Ativo |
| 13 | Azure OpenAI | configuravel | Ativo |
| 14 | NVIDIA | configuravel | Ativo |
| 15 | Cerebras | configuravel | Ativo |
| 16 | SambaNova | configuravel | Ativo |
| 17 | Novita | configuravel | Ativo |

Cada provider possui circuit breaker Resilience4j (window=20, threshold=50%, wait=20s).

---

## Documentacao Tecnica

| Documento | Descricao |
|-----------|-----------|
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Diagrama de sistema, fluxos, ADRs |
| [PRODUCT_SPEC.md](./PRODUCT_SPEC.md) | Visao, personas, escopo, roadmap |
| [ROADMAP.md](./ROADMAP.md) | 26 gaps priorizados em 5 fases |
| [PARITY_MATRIX.md](./PARITY_MATRIX.md) | Status de 26 capacidades |
| [DATA_MODEL.md](./DATA_MODEL.md) | ERD, campos, indices |
| [API_CONTRACT.md](./API_CONTRACT.md) | Endpoints, exemplos, error codes |
| [EVENT_MODEL.md](./EVENT_MODEL.md) | Eventos SSE, payloads |
| [STATE_MACHINES.md](./STATE_MACHINES.md) | Task (13 estados), Env Cache, PR |
| [ROUTES.md](./ROUTES.md) | Rotas UI e API com auth |
| [SECURITY.md](./SECURITY.md) | STRIDE, CVSS, hardening |
| [CONNECTORS.md](./CONNECTORS.md) | GitHub, Slack, Linear |
| [ASSUMPTIONS.md](./ASSUMPTIONS.md) | Premissas validadas |
| [TEST_PLAN.md](./TEST_PLAN.md) | Cobertura, gap analysis |
| [PERFORMANCE.md](./PERFORMANCE.md) | Bundle, cache, queries |
| [RUNBOOK.md](./RUNBOOK.md) | Bootstrap, monitoring, DR |
| [DEPLOYMENT.md](./DEPLOYMENT.md) | CI/CD, Docker, checklist |
| [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) | 28 problemas comuns |
| [CONTRIBUTING.md](./CONTRIBUTING.md) | Standards, PR process |
| [CHANGELOG_IMPLEMENTATION.md](./CHANGELOG_IMPLEMENTATION.md) | Historico de versoes |

---

## Status do Projeto

| Metrica | Valor |
|---------|-------|
| Vulnerabilidades criticas | 4 (GAP-001 a GAP-004) |
| Capacidades "Complete" | 0/26 |
| Capacidades "Partial" | 22/26 |
| Unit tests (frontend) | 47 |
| Unit tests (backend) | ~100+ |
| E2E tests | 9 |
| Integration tests | 0 |

Ver [ROADMAP.md](./ROADMAP.md) para plano de correcao e [PARITY_MATRIX.md](./PARITY_MATRIX.md) para status detalhado.

---

## Licenca

Proprietario - Todos os direitos reservados.
