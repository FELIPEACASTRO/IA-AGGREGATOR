# DATA_MODEL — Modelo de Dados

> Documentação completa do modelo de dados da plataforma IA-AGGREGATOR / Lume Codex Cloud.
> Versão: 2.0.0 | Última atualização: 2026-03-08

---

## Visão Geral

A plataforma utiliza **dois sistemas de gerenciamento de schema** sobre o mesmo banco PostgreSQL 16 (pgvector):

| Sistema | Schema(s) | ORM | Migração | Responsável |
|---------|----------|-----|----------|-------------|
| Prisma | `codex` | Prisma 6.16.2 | `prisma db push` | Frontend (Next.js) |
| Flyway/JPA | `auth`, `analytics`, `billing`*, `chat`*, `ai_gateway`*, `partners`*, `content`*, `teams`*, `audit`* | Spring Data JPA + Hibernate | Flyway 10.20.1 | Backend (Spring Boot) |

> *Schemas com `*` têm estrutura DDL criada mas sem tabelas populadas.
> **ATENÇÃO (GAP-005)**: Não há coordenação entre migrações. Ver [ROADMAP.md](./ROADMAP.md).

---

## Diagrama de Relacionamentos (ERD)

```
┌─────────────────────────────────────────────────────────────────────┐
│                        SCHEMA: codex (Prisma)                       │
│                                                                     │
│  ┌──────────┐     ┌───────────┐     ┌──────────────┐              │
│  │   User   │────<│ Membership │>────│  Workspace   │              │
│  └────┬─────┘     └───────────┘     └──────┬───────┘              │
│       │ createdBy                           │ workspaceId          │
│       ▼                                     ▼                      │
│  ┌──────────┐  ┌───────────┐  ┌──────────────┐  ┌─────────────┐  │
│  │ Session  │  │OAuthConn  │  │ Environment  │  │GitRepository│  │
│  └──────────┘  └───────────┘  └──────┬───────┘  └──────┬──────┘  │
│                                      │                  │          │
│       ┌──────────────────────────────┤                  │          │
│       │              │               │                  │          │
│       ▼              ▼               ▼                  │          │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐         │          │
│  │   Task   │  │ EnvSecret│  │ EnvVariable  │         │          │
│  └────┬─────┘  └──────────┘  └──────────────┘         │          │
│       │                                                 │          │
│       ├── TaskEvent    ├── TaskLogChunk                 │          │
│       ├── TaskInput    ├── TaskFollowUp                │          │
│       ├── TaskRun      ├── TaskArtifact                │          │
│       ├── DiffSnapshot ──── DiffFile ──── DiffHunk    │          │
│       └── PullRequest ─────────────────────────────────┘          │
│                                                                     │
│  SlackInstall ──── SlackBinding    CodeReviewPolicy               │
│  LinearInstall ──── LinearBinding   GitHubReviewRun ── Finding    │
│  UsageEntry    CreditBalance    CreditLedgerEntry                 │
│  AuditLog      AnalyticsAggregate  ComplianceExportJob            │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  SCHEMA: auth (Flyway)         │  SCHEMA: analytics (Flyway)       │
│  auth.users ──── sessions      │  event_reports ──── ingested_evts │
│       ├── user_identities      │                                    │
│       ├── api_keys             │                                    │
│       ├── consent_records      │                                    │
│       └── erasure_requests     │                                    │
│  auth.organizations            │                                    │
│  auth.feature_flags            │                                    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Modelos Schema `codex` (Prisma) — Detalhamento

### Identidade e Acesso

#### User
| Campo | Tipo | Constraints | Descrição |
|-------|------|------------|-----------|
| id | String | PK, cuid() | Identificador |
| email | String | UNIQUE | Email |
| name | String | NOT NULL | Nome completo |
| avatarUrl | String? | — | Avatar |
| createdAt | DateTime | now() | Criação |
| updatedAt | DateTime | @updatedAt | Atualização |
**Relações**: memberships[], sessions[], tasks[], oauthConnections[]

#### Workspace
| Campo | Tipo | Constraints | Descrição |
|-------|------|------------|-----------|
| id | String | PK | Identificador |
| slug | String | UNIQUE | Slug |
| name | String | NOT NULL | Nome |
**Relações**: memberships[], environments[], tasks[], gitRepos[], slackInstalls[], linearInstalls[]

#### Membership
| Campo | Tipo | Constraints | Descrição |
|-------|------|------------|-----------|
| workspaceId | String | FK → Workspace | Workspace |
| userId | String | FK → User | Usuário |
| role | MembershipRole | OWNER/ADMIN/MEMBER | Papel |
**Index**: `@@unique([workspaceId, userId])`

---

### Execução de Tasks

#### Task (modelo central)
| Campo | Tipo | Constraints | Descrição |
|-------|------|------------|-----------|
| id | String | PK | Identificador |
| workspaceId | String | FK → Workspace | Workspace |
| repositoryId | String? | FK → GitRepository | Repo alvo |
| environmentId | String? | FK → Environment | Ambiente |
| createdById | String | FK → User | Criador |
| title | String | NOT NULL | Título |
| prompt | String | NOT NULL | Instrução |
| mode | TaskMode | ASK/CODE | Modo |
| status | TaskStatus | 13 estados | Status atual |
| sourceRef | String? | — | Branch/ref origem |
| baseBranch | String? | — | Branch base |
| resultBranch | String? | — | Branch resultado |
| baseCommitSha | String? | — | Commit base |
| internetMode | InternetMode | OFF/LIMITED/UNRESTRICTED | Política rede |
| bestOfN | Int | default 1 | Tentativas |
| attemptIndex | Int | default 0 | Índice atual |
| startedAt | DateTime? | — | Início execução |
| completedAt | DateTime? | — | Fim execução |
**Índices**: `[workspaceId, status]`, `[createdById]`

#### TaskStatus (Enum)
`draft` → `queued` → `preparing_environment` → `downloading_repository` → `cloning_repository` → `running_setup` → `running_maintenance` → `running_agent` → `validating` → `generating_diff` → `pr_ready` → `completed` | `failed` | `cancelled` | `archived`

#### TaskEvent
| Campo | Tipo | Descrição |
|-------|------|-----------|
| taskId | String | FK → Task |
| eventType | String | Ex: `task.created`, `agent.progress` |
| status | String? | Status no momento |
| message | String? | Mensagem |
| metadata | Json? | Dados extras |
**Índice**: `[taskId, createdAt]` — Otimizado para SSE

#### TaskLogChunk
| Campo | Tipo | Descrição |
|-------|------|-----------|
| taskId | String | FK → Task |
| phase | String | provisioning/repo_download/setup/maintenance/agent/validation/pr_push |
| line | String | Conteúdo |
| lineNumber | Int | Número sequencial |
| isError | Boolean | Flag de erro |
**Índice**: `[taskId, phase, lineNumber]`

---

### Evidências

#### DiffSnapshot → DiffFile → DiffHunk
Hierarquia de 3 níveis para armazenar diffs de código:
- **DiffSnapshot**: summary + patch completo por task
- **DiffFile**: path, changeType (add/modify/delete), additions, deletions
- **DiffHunk**: header + content por arquivo

#### PullRequest
| Campo | Tipo | Descrição |
|-------|------|-----------|
| taskId | String | FK → Task |
| repositoryId | String | FK → GitRepository |
| externalNumber | Int? | # no GitHub |
| title/body | String | Conteúdo |
| url | String? | URL do PR |
| branch | String | Branch |
| status | PullRequestStatus | none/draft/open/merged/closed/update_pending/failed |

---

### Ambientes

#### Environment
| Campo | Tipo | Descrição |
|-------|------|-----------|
| name | String | Nome do ambiente |
| description | String? | Descrição |
| defaultBranch | String? | Branch padrão |
| baseImage | String? | Imagem Docker |
| setupScript | String? | Script de setup |
| maintenanceScript | String? | Script de manutenção |
| internetMode | InternetMode | Política de rede |
| domainAllowlist | String[] | Domínios permitidos |
**Relações**: repoMaps[], runtimePins[], variables[], secrets[], cache?

#### EnvironmentSecret
| Campo | Tipo | Descrição |
|-------|------|-----------|
| key | String | Nome |
| encryptedValue | String | Valor criptografado |
| setupOnly | Boolean | Só durante setup |

---

### Conectores e Billing

**SlackInstall/LinearInstall**: Instalação por workspace com bindings por canal/time e event logs.

**CreditBalance**: Saldo único por workspace (UNIQUE). **CreditLedgerEntry**: Histórico com tipos PURCHASE/CONSUMPTION/ADJUSTMENT/REFUND.

**BillingIntent**: Intenções de pagamento com status PENDING/COMPLETED/FAILED/CANCELLED e `providerRef` (Stripe ID — pendente integração).

---

## Tabelas Schema `auth` (Flyway)

### auth.users (principal)
| Campo | Tipo | Índices | Descrição |
|-------|------|---------|-----------|
| id | UUID | PK | ID |
| email | VARCHAR(320) | UNIQUE | Email |
| password_hash | TEXT | — | BCrypt hash |
| full_name | VARCHAR(255) | — | Nome |
| role | user_role | INDEX | super_admin/admin/user/viewer/api_only |
| status | user_status | INDEX | active/inactive/suspended/pending_verification/deleted |
| auth_provider | auth_provider | — | email/google/github/microsoft/apple |
| locale | VARCHAR(10) | — | default 'pt-BR' |
| failed_login_count | INTEGER | — | Tentativas falhadas |
| locked_until | TIMESTAMPTZ | — | Lock por brute-force |
| deleted_at | TIMESTAMPTZ | INDEX (parcial) | Soft delete |

### auth.feature_flags
| Campo | Tipo | Descrição |
|-------|------|-----------|
| name | VARCHAR | UNIQUE — nome da flag |
| is_enabled | BOOLEAN | Ativada globalmente |
| enabled_for_users | UUID[] | Usuários específicos |
| rollout_percentage | SMALLINT | 0-100 |

> **GAP-026**: Sem runtime evaluation no frontend.

---

## Extensões PostgreSQL

| Extensão | Propósito |
|----------|-----------|
| uuid-ossp | Geração de UUIDs v4 |
| pgcrypto | Funções criptográficas |
| pg_trgm | Trigram matching (busca fuzzy) |
| vector | pgvector — embeddings para busca semântica |

---

## Referências

- **Schema Prisma**: `frontend/prisma/schema.prisma`
- **Migrações Flyway**: `backend/ia-aggregator-infrastructure/src/main/resources/db/migration/`
- [STATE_MACHINES.md](./STATE_MACHINES.md) — Transições de estado
- [API_CONTRACT.md](./API_CONTRACT.md) — Endpoints que manipulam modelos
- [ROADMAP.md](./ROADMAP.md) — GAP-005 (schema drift)
