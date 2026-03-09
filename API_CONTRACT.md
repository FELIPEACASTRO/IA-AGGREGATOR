# API_CONTRACT — Contratos de API

> Especificacao completa dos endpoints da plataforma IA-AGGREGATOR / Lume Codex Cloud.
> Versao: 2.0.0 | Ultima atualizacao: 2026-03-08

---

## Convencoes

- **Base URL Frontend (Codex)**: `/api/*` (Next.js API Routes)
- **Base URL Backend**: `/api/v1/*` (Spring Boot, via rewrite no next.config.js)
- **Autenticacao**: Cookie `access_token` com JWT (exceto endpoints publicos)
- **Content-Type**: `application/json` (exceto SSE: `text/event-stream`)
- **Resposta padrao (Codex)**: `{ success: boolean, data?: T, message?: string }`
- **Resposta padrao (Backend)**: `{ code: string, message: string, data?: T, errors?: FieldError[] }`

---

## Auth (Backend via Rewrite)

### POST /api/v1/auth/login
Autentica usuario e retorna tokens JWT.
- **Auth**: Publica
- **Body**: `{ "email": "user@example.com", "password": "senhaSegura123" }`
- **Validacao**: email (formato valido), password (min 8 chars)
- **Response 200**: `{ "code": "AUTH_000", "data": { "accessToken": "eyJ...", "refreshToken": "eyJ...", "user": { "id", "email", "fullName", "role" } } }`
- **Erros**: AUTH_001 (401), AUTH_003 (403), AUTH_004 (423)

### POST /api/v1/auth/register
- **Auth**: Publica
- **Body**: `{ "email": "...", "password": "...", "fullName": "..." }`
- **Response 201**: Mesmo formato do login
- **Erros**: AUTH_007 (422), AUTH_008 (422)

### POST /api/v1/auth/refresh
- **Auth**: Publica (requer refresh token)
- **Body**: `{ "refreshToken": "eyJ..." }`
- **Response 200**: `{ "data": { "accessToken": "novo_token" } }`

### GET /api/v1/auth/me
- **Auth**: Bearer token
- **Response 200**: `{ "data": { "id", "email", "fullName", "avatarUrl", "role", "status" } }`

---

## Auth (Frontend - Cookie Management)

| Metodo | Rota | Auth | Descricao |
|--------|------|------|-----------|
| POST | `/api/auth/login` | Publica | Proxy backend + set cookies |
| POST | `/api/auth/logout` | Cookie | Clear cookies |
| GET | `/api/auth/session` | Cookie | Verifica sessao |

---

## OAuth

| Metodo | Rota | Auth | Descricao |
|--------|------|------|-----------|
| POST | `/api/oauth/github/connect` | Cookie | Inicia OAuth |
| GET | `/api/oauth/github/callback` | Publica | Callback OAuth (mock - GAP-004) |

---

## Repositories

| Metodo | Rota | Auth | Descricao |
|--------|------|------|-----------|
| GET | `/api/repositories` | Cookie | Lista repos do workspace |
| GET | `/api/repositories/:repoId/branches` | Cookie | Lista branches |

---

## Environments

| Metodo | Rota | Auth | Descricao |
|--------|------|------|-----------|
| GET | `/api/environments` | Cookie | Lista ambientes |
| POST | `/api/environments` | Cookie | Cria ambiente |
| GET | `/api/environments/:id` | Cookie | Detalhes |
| PATCH | `/api/environments/:id` | Cookie | Atualiza |
| DELETE | `/api/environments/:id` | Cookie | Remove |
| POST | `/api/environments/:id/validate` | Cookie | Valida config |
| POST | `/api/environments/:id/reset-cache` | Cookie | Invalida cache |

**Body POST**: `{ "name": "Production", "description": "...", "defaultBranch": "main", "baseImage": "node:20", "setupScript": "npm install", "internetMode": "LIMITED", "domainAllowlist": ["npmjs.org"] }`

---

## Tasks

| Metodo | Rota | Auth | Descricao |
|--------|------|------|-----------|
| GET | `/api/tasks` | Cookie | Lista (paginado: ?page=0&limit=20) |
| POST | `/api/tasks` | Cookie | Cria + enfileira |
| GET | `/api/tasks/:id` | Cookie | Detalhes |
| GET | `/api/tasks/:id/events` | Cookie | SSE (text/event-stream) |
| GET | `/api/tasks/:id/logs` | Cookie | Logs por fase |
| GET | `/api/tasks/:id/diff` | Cookie | Diff de codigo |
| GET | `/api/tasks/:id/tests` | Cookie | Resultados de testes |
| GET | `/api/tasks/:id/artifacts` | Cookie | Artefatos |
| POST | `/api/tasks/:id/followups` | Cookie | Follow-up |
| POST | `/api/tasks/:id/cancel` | Cookie | Cancela |
| POST | `/api/tasks/:id/retry` | Cookie | Retry |
| POST | `/api/tasks/:id/archive` | Cookie | Arquiva |
| POST | `/api/tasks/:id/unarchive` | Cookie | Restaura |

**Body POST /api/tasks**: `{ "title": "...", "prompt": "...", "mode": "CODE", "environmentId": "uuid", "repositoryId": "uuid", "internetMode": "LIMITED" }`

**Body POST followups**: `{ "prompt": "...", "mode": "CODE" }`

---

## Pull Requests

| Metodo | Rota | Auth | Descricao |
|--------|------|------|-----------|
| GET | `/api/tasks/:id/pull-requests` | Cookie | Lista PRs |
| POST | `/api/tasks/:id/pull-requests` | Cookie | Cria PR |
| PATCH | `/api/tasks/:id/pull-requests/:prId` | Cookie | Atualiza |

---

## Code Review

| Metodo | Rota | Auth | Descricao |
|--------|------|------|-----------|
| GET | `/api/code-review/policies` | Cookie | Lista politicas |
| POST | `/api/code-review/policies` | Cookie | Cria politica |
| PATCH | `/api/code-review/policies/:id` | Cookie | Atualiza |

---

## Integrations + Webhooks

| Metodo | Rota | Auth | Descricao |
|--------|------|------|-----------|
| POST | `/api/integrations/github/install` | Cookie | Instala GitHub |
| POST | `/api/integrations/slack/install` | Cookie | Instala Slack |
| POST | `/api/integrations/linear/install` | Cookie | Instala Linear |
| GET | `/api/integrations/status` | Cookie | Status conectores |
| POST | `/api/webhooks/github` | Publica* | Webhook GitHub |
| POST | `/api/webhooks/slack` | Publica* | Webhook Slack |
| POST | `/api/webhooks/linear` | Publica* | Webhook Linear |

> *Webhooks sem verificacao HMAC (GAP-003)

---

## Usage/Credits

| Metodo | Rota | Auth | Descricao |
|--------|------|------|-----------|
| GET | `/api/usage` | Cookie | Dashboard uso |
| GET | `/api/credits` | Cookie | Saldo |
| POST | `/api/credits` | Cookie | Operacao manual |
| POST | `/api/credits/purchase` | Cookie | Compra (mock - GAP-021) |

---

## Analytics/Admin

| Metodo | Rota | Auth | Descricao |
|--------|------|------|-----------|
| GET | `/api/analytics` | Cookie | Metricas |
| GET | `/api/compliance/exports` | Cookie | Lista exports |
| POST | `/api/compliance/exports` | Cookie | Solicita export |
| GET | `/api/managed-configs` | Cookie | Lista configs |
| POST | `/api/managed-configs` | Cookie | Cria config |
| PATCH | `/api/managed-configs/:id` | Cookie | Atualiza |

---

## Backend (Spring Boot - via Rewrite /api/v1/*)

| Metodo | Rota | Auth | Descricao |
|--------|------|------|-----------|
| POST | `/api/v1/ai/chat` | Bearer | Chat multi-provider (17 providers) |
| POST | `/api/v1/analytics/events` | Publica | Ingestao de eventos |
| GET | `/api/v1/analytics/reports` | Bearer | Relatorios |
| GET | `/api/v1/analytics/reports/:id/events` | Bearer | Eventos do report |

---

## Codigos de Erro

| Codigo | HTTP | Descricao |
|--------|------|-----------|
| AUTH_001 | 401 | Credenciais invalidas |
| AUTH_003 | 403 | Conta desativada |
| AUTH_004 | 423 | Conta bloqueada |
| AUTH_006 | 401 | Token invalido/expirado |
| AUTH_007 | 422 | Email ja cadastrado |
| AI_001 | 400 | Prompt invalido |
| AI_002 | 503 | Provider indisponivel |
| AI_003 | 429 | Rate limit |
| AI_004 | 400 | Guardrail bloqueou |
| AI_006 | 500 | Nenhum provider |
| GEN_001 | 500 | Erro interno |
| GEN_003 | 404 | Nao encontrado |

---

## Referencias

- [ROUTES.md](./ROUTES.md) — Rotas de paginas
- [DATA_MODEL.md](./DATA_MODEL.md) — Modelos de dados
- [EVENT_MODEL.md](./EVENT_MODEL.md) — Eventos SSE
- [SECURITY.md](./SECURITY.md) — Autenticacao
