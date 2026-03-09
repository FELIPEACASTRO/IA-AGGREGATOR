# ROUTES — Rotas da Aplicação

> Mapa completo de rotas de páginas e API da plataforma IA-AGGREGATOR / Lume Codex Cloud.
> Versão: 2.0.0 | Última atualização: 2026-03-08

---

## Rotas de Páginas (App Router)

### Públicas (sem autenticação)

| Rota | Componente | Descrição |
|------|-----------|-----------|
| `/` | `page.tsx` | Redirect para `/codex` ou `/login` |
| `/login` | `login/page.tsx` | Página de login |
| `/register` | `register/page.tsx` | Página de cadastro |
| `/logout` | `logout/page.tsx` | Handler de logout (limpa cookies) |
| `/oauth/github/callback` | `oauth/github/callback/page.tsx` | Callback OAuth GitHub |
| `/error` | `error/page.tsx` | Página de erro genérico |
| `/not-found` | — | Página 404 |

### Protegidas — Codex Core

| Rota | Auth | Descrição |
|------|------|-----------|
| `/codex` | Sim | Dashboard principal + lista de tasks |
| `/codex/get-started` | Sim | Tela de onboarding inicial |
| `/codex/onboarding` | Sim | Wizard de configuração |
| `/codex/tasks/[taskId]` | Sim | Detalhe da task (summary) |
| `/codex/tasks/[taskId]/logs` | Sim | Logs de execução em tempo real |
| `/codex/tasks/[taskId]/diff` | Sim | Visualização de diff de código |
| `/codex/tasks/[taskId]/tests` | Sim | Resultados de testes/validação |
| `/codex/tasks/[taskId]/artifacts` | Sim | Artefatos gerados |
| `/codex/tasks/[taskId]/pull-request` | Sim | Gerenciamento de PR |
| `/codex/tasks?tab=archived` | Sim | Tasks arquivadas |
| `/codex/shortcuts` | Sim | Templates de prompts rápidos |

### Protegidas — Configurações Codex

| Rota | Auth | Descrição |
|------|------|-----------|
| `/codex/settings/connectors` | Sim | Setup GitHub/Slack/Linear |
| `/codex/settings/environments` | Sim | Lista de ambientes |
| `/codex/settings/environments/new` | Sim | Criar novo ambiente |
| `/codex/settings/environments/[environmentId]` | Sim | Editar ambiente |
| `/codex/settings/code-review` | Sim | Políticas de code review |
| `/codex/settings/usage` | Sim | Dashboard de uso |
| `/codex/settings/usage/credits` | Sim | Compra de créditos |
| `/codex/settings/analytics` | Sim | Relatórios de analytics |
| `/codex/settings/managed-configs` | Sim | Configurações admin |
| `/codex/settings/apireference` | Sim | Documentação da API |

### Protegidas — Auxiliares

| Rota | Auth | Descrição |
|------|------|-----------|
| `/chat` | Sim | Interface de chat com IA |
| `/library` | Sim | Histórico de conversas |
| `/prompts` | Sim | Biblioteca de prompts |
| `/billing` | Sim | Gerenciamento de billing |
| `/settings` | Sim | Preferências do usuário |
| `/settings/analytics/debug` | Sim | Debug de analytics |
| `/home` | Sim | Landing page pós-login |
| `/admin/settings` | Sim | Painel administrativo |

---

## Rotas de API (Next.js API Routes)

### Autenticação

| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| POST | `/api/auth/login` | Pública | Login + set cookies |
| POST | `/api/auth/logout` | Cookie | Logout + clear cookies |
| GET | `/api/auth/session` | Cookie | Verifica sessão |

### OAuth

| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| POST | `/api/oauth/github/connect` | Cookie | Inicia OAuth GitHub |
| GET | `/api/oauth/github/callback` | Pública | Callback OAuth |

### Repositories

| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| GET | `/api/repositories` | Cookie | Lista repos |
| GET | `/api/repositories/:repoId/branches` | Cookie | Lista branches |

### Environments

| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| GET | `/api/environments` | Cookie | Lista ambientes |
| POST | `/api/environments` | Cookie | Cria ambiente |
| GET | `/api/environments/:id` | Cookie | Detalhes |
| PATCH | `/api/environments/:id` | Cookie | Atualiza |
| DELETE | `/api/environments/:id` | Cookie | Remove |
| POST | `/api/environments/:id/validate` | Cookie | Valida config |
| POST | `/api/environments/:id/reset-cache` | Cookie | Invalida cache |

### Tasks

| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| GET | `/api/tasks` | Cookie | Lista tasks (paginado) |
| POST | `/api/tasks` | Cookie | Cria task + enfileira |
| GET | `/api/tasks/:id` | Cookie | Detalhes da task |
| GET | `/api/tasks/:id/events` | Cookie | SSE eventos (text/event-stream) |
| GET | `/api/tasks/:id/logs` | Cookie | Logs por fase |
| GET | `/api/tasks/:id/diff` | Cookie | Diff de código |
| GET | `/api/tasks/:id/tests` | Cookie | Resultados de testes |
| GET | `/api/tasks/:id/artifacts` | Cookie | Artefatos |
| POST | `/api/tasks/:id/followups` | Cookie | Cria follow-up |
| POST | `/api/tasks/:id/cancel` | Cookie | Cancela task |
| POST | `/api/tasks/:id/retry` | Cookie | Retry task falhada |
| POST | `/api/tasks/:id/archive` | Cookie | Arquiva task |
| POST | `/api/tasks/:id/unarchive` | Cookie | Restaura task |

### Pull Requests

| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| GET | `/api/tasks/:id/pull-requests` | Cookie | Lista PRs |
| POST | `/api/tasks/:id/pull-requests` | Cookie | Cria PR |
| PATCH | `/api/tasks/:id/pull-requests/:prId` | Cookie | Atualiza PR |

### Code Review

| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| GET | `/api/code-review/policies` | Cookie | Lista políticas |
| POST | `/api/code-review/policies` | Cookie | Cria política |
| PATCH | `/api/code-review/policies/:id` | Cookie | Atualiza política |

### Integrações e Webhooks

| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| POST | `/api/integrations/github/install` | Cookie | Instala GitHub |
| POST | `/api/integrations/slack/install` | Cookie | Instala Slack |
| POST | `/api/integrations/linear/install` | Cookie | Instala Linear |
| GET | `/api/integrations/status` | Cookie | Status dos conectores |
| POST | `/api/webhooks/github` | Pública* | Webhook GitHub |
| POST | `/api/webhooks/slack` | Pública* | Webhook Slack |
| POST | `/api/webhooks/linear` | Pública* | Webhook Linear |

> *Webhooks são públicos mas **devem** verificar assinatura HMAC (ver [ROADMAP.md](./ROADMAP.md) GAP-003)

### Usage/Credits

| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| GET | `/api/usage` | Cookie | Dashboard de uso |
| GET | `/api/credits` | Cookie | Saldo |
| POST | `/api/credits` | Cookie | Operação manual |
| POST | `/api/credits/purchase` | Cookie | Compra |

### Analytics/Admin

| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| GET | `/api/analytics` | Cookie | Métricas |
| GET | `/api/compliance/exports` | Cookie | Lista exports |
| POST | `/api/compliance/exports` | Cookie | Solicita export |
| GET | `/api/managed-configs` | Cookie | Lista configs |
| POST | `/api/managed-configs` | Cookie | Cria config |
| PATCH | `/api/managed-configs/:id` | Cookie | Atualiza config |

### Health e Modelos

| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| GET | `/api/health/status` | Pública | Health check |
| GET | `/api/models/capabilities` | Cookie | Modelos disponíveis |

---

## Rotas Backend (Spring Boot — via Rewrite)

Acessadas via rewrite `next.config.js`: `/api/v1/*` → `NEXT_PUBLIC_API_URL/api/v1/*`

| Método | Rota | Auth | Descrição |
|--------|------|------|-----------|
| POST | `/api/v1/auth/register` | Pública | Registro |
| POST | `/api/v1/auth/login` | Pública | Login |
| POST | `/api/v1/auth/refresh` | Pública | Refresh token |
| GET | `/api/v1/auth/me` | Bearer | Perfil do usuário |
| POST | `/api/v1/ai/chat` | Bearer | Chat multi-provider |
| POST | `/api/v1/analytics/events` | Pública | Ingestão de eventos |
| GET | `/api/v1/analytics/reports` | Bearer | Relatórios |
| GET | `/api/v1/analytics/reports/:id/events` | Bearer | Eventos do report |

---

## Middleware de Autenticação

**Arquivo**: `frontend/src/middleware.ts`

```
Rotas públicas: '/', '/login', '/register'
Todas as demais: requer cookie 'access_token'
Redirect sem token: /login?redirect={pathname}
```

---

## Referências

- [API_CONTRACT.md](./API_CONTRACT.md) — Detalhes de request/response
- [SECURITY.md](./SECURITY.md) — Autenticação e autorização
- [ARCHITECTURE.md](./ARCHITECTURE.md) — Fluxos de dados
