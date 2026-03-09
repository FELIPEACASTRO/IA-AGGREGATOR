# ASSUMPTIONS — Premissas de Implementacao

> Premissas adotadas na implementacao com status de validacao.
> Versao: 2.0.0 | Ultima atualizacao: 2026-03-08

---

| # | Premissa | Status | Evidencia | Observacao |
|---|---------|--------|-----------|-----------|
| 1 | Execucao de tasks roda em worker BullMQ + Redis com sandbox por task em `.codex-runtime/tasks/{taskId}` | **Valida** | `frontend/src/server/codex/task-runner.ts`, `queue.ts` | Worker lazy-init no servidor Next.js |
| 2 | Integracoes GitHub/Slack/Linear implementadas como surfaces reais com estado persistido | **Parcial** | Webhooks existem mas sem verificacao HMAC (GAP-003) | Hardening de assinatura pendente |
| 3 | Workspace/repository/environment padrao auto-seeded da sessao do usuario | **Valida** | `frontend/src/server/codex/seed.ts` | Reduz fricao no onboarding |
| 4 | Se nao ha clone URL, worker inicializa workspace git local e produz logs/diff | **Valida** | `task-runner.ts` fallback path | Funciona offline |
| 5 | Billing abstrato via `BillingIntent` + ledger; purchase grava intents manuais | **Valida (limitada)** | `frontend/src/app/api/credits/purchase/route.ts` | Sem Stripe real (GAP-021) |
| 6 | AGENTS.md precedence engine atras de feature flag | **Valida** | `ReviewGuidelineSource` placeholder | Feature flag sem runtime (GAP-026) |
| 7 | OAuth callback suporta fluxo deterministico local, upgradeable | **INVALIDA para producao** | `oauth/github/callback/route.ts` usa `token-from-${code.slice(0,8)}` | Mock deve ser substituido (GAP-004) |
| 8 | Internet policy enforced no nivel de config/UX; sandbox de rede diferido | **Valida** | `InternetPolicy` model, badges no composer | Container-level enforcement pendente |
| 9 | Backend auth e source-of-truth para login; Codex usa JWT em cookies | **Parcial** | Login via Spring Boot, JWT em cookies | JWT sem verificacao de assinatura no frontend (GAP-001) |

---

## Novas Premissas Identificadas na Auditoria

| # | Premissa | Status | Evidencia |
|---|---------|--------|-----------|
| 10 | Dois schemas de banco coexistem sem coordenacao (Prisma `codex` + Flyway `auth`) | **Risco** | Ambos no mesmo PostgreSQL sem migration sync (GAP-005) |
| 11 | Streaming de chat e simulado (4 chars/12ms apos resposta completa) | **Valida (problema)** | `chat-store.ts` linhas ~230-253 (GAP-007) |
| 12 | Rate limiting depende inteiramente do backend; frontend nao implementa | **Valida (lacuna)** | Nenhum middleware rate limit no Next.js (GAP-008) |
| 13 | Redis disponivel mas usado apenas para BullMQ, sem cache de API | **Valida (lacuna)** | `redis.ts` conectado, sem cache helper (GAP-009) |
| 14 | Cookies de autenticacao nao usam HttpOnly/Secure | **INVALIDA (seguranca)** | `auth/login/route.ts` define `httpOnly: false` (GAP-002) |

---

## Referencias

- [ROADMAP.md](./ROADMAP.md) — Plano de correcao para premissas invalidas
- [SECURITY.md](./SECURITY.md) — Implicacoes de seguranca
- [ARCHITECTURE.md](./ARCHITECTURE.md) — Decisoes arquiteturais
