# CONNECTORS — Integracoes com Conectores

> Documentacao das integracoes GitHub, Slack e Linear da plataforma IA-AGGREGATOR / Lume Codex Cloud.
> Versao: 2.0.0 | Ultima atualizacao: 2026-03-08

---

## Visao Geral

| Conector | Instalacao | Webhook | Modelos | Status |
|----------|-----------|---------|---------|--------|
| GitHub | POST /api/integrations/github/install | POST /api/webhooks/github | GitHubInstallation, GitRepository, RepositoryPermission | Partial |
| Slack | POST /api/integrations/slack/install | POST /api/webhooks/slack | SlackInstall, SlackWorkspaceBinding, SlackEventLog | Partial |
| Linear | POST /api/integrations/linear/install | POST /api/webhooks/linear | LinearInstall, LinearWorkspaceBinding, LinearEventLog | Partial |

**Status Agregado**: `GET /api/integrations/status`

> **ALERTA (GAP-003)**: Nenhum webhook verifica assinatura HMAC. Ver [ROADMAP.md](./ROADMAP.md).

---

## GitHub

### Fluxo de Instalacao

```
1. Usuario acessa /codex/settings/connectors
2. Clica "Conectar GitHub"
3. POST /api/integrations/github/install
   → Cria GitHubInstallation no workspace
4. POST /api/oauth/github/connect
   → Retorna URL de autorizacao OAuth
5. Redirect para GitHub → usuario autoriza
6. GitHub redireciona para /oauth/github/callback?code=xxx&state=yyy
7. GET /api/oauth/github/callback
   → Troca code por token (⚠️ MOCK ATUAL - GAP-004)
   → Cria OAuthConnection
   → Redirect para /codex/settings/connectors
```

### Webhook GitHub

**Endpoint**: `POST /api/webhooks/github`

**Headers esperados**:
- `x-github-event`: Tipo do evento (push, pull_request, installation)
- `x-hub-signature-256`: HMAC SHA-256 do body (⚠️ NAO VERIFICADO)
- `x-github-delivery`: ID unico do delivery

**Eventos suportados**:
| Evento | Acao | Descricao |
|--------|------|-----------|
| `push` | Cria task automatica | Novo commit no branch monitorado |
| `pull_request` | Atualiza PullRequest | PR aberto/mergeado/fechado |
| `installation` | Atualiza GitHubInstallation | App instalada/removida |

**Fluxo de processamento**:
```
Webhook recebido
  → Parse JSON body
  → Identifica workspace via installationId
  → Switch por event type:
    push → Cria Task com prompt do commit message
    pull_request → Atualiza PullRequest status
    installation → Atualiza GitHubInstallation status
  → Persiste AuditLog
```

### Review Policy

- `CodeReviewPolicy`: Habilitada por repositorio
- `GitHubReviewRun`: Execucao de review com findings
- `ReviewFinding`: Achado individual com severidade (P0-P3)
- Configuravel em `/codex/settings/code-review`

---

## Slack

### Fluxo de Instalacao

```
1. Usuario acessa /codex/settings/connectors
2. Clica "Conectar Slack"
3. POST /api/integrations/slack/install
   → Cria SlackInstall com teamId, botUserId
4. Configura SlackWorkspaceBinding:
   → channelId + channelName
   → defaultEnvironmentId (ambiente para tasks criadas via Slack)
```

### Webhook Slack

**Endpoint**: `POST /api/webhooks/slack`

**Headers esperados**:
- `x-slack-signature`: HMAC SHA-256 (⚠️ NAO VERIFICADO)
- `x-slack-request-timestamp`: Timestamp do request

**Eventos suportados**:
| Evento | Acao | Descricao |
|--------|------|-----------|
| `app_mention` | Cria task | Bot mencionado no canal |
| `message` | Log apenas | Mensagem no canal vinculado |

**Fluxo de mencao → task**:
```
@bot corrigir bug no login
  → Webhook recebido
  → Parse do texto da mencao
  → Encontra SlackWorkspaceBinding pelo channelId
  → Cria Task:
    title: "corrigir bug no login"
    prompt: texto completo da mencao
    environmentId: binding.defaultEnvironmentId
  → Enfileira no BullMQ
  → (Opcional) postFinalReply: responde no Slack ao completar
```

### Opcoes Configuráveis

| Campo | Tipo | Descricao |
|-------|------|-----------|
| `postFinalReply` | Boolean | Responder no canal quando task completar |
| `defaultEnvironmentId` | String | Ambiente padrao para tasks via Slack |

---

## Linear

### Fluxo de Instalacao

```
1. Usuario acessa /codex/settings/connectors
2. Clica "Conectar Linear"
3. POST /api/integrations/linear/install
   → Cria LinearInstall com organizationId
4. Configura LinearWorkspaceBinding:
   → teamId
   → defaultEnvironmentId
```

### Webhook Linear

**Endpoint**: `POST /api/webhooks/linear`

**Headers esperados**:
- `linear-signature`: HMAC (⚠️ NAO VERIFICADO)

**Eventos suportados**:
| Evento | Acao | Descricao |
|--------|------|-----------|
| `Issue.create` | Cria task | Nova issue criada |
| `Comment.create` | Cria task | Comentario com trigger |

**Fluxo**:
```
Issue criada no Linear
  → Webhook recebido
  → Parse do payload
  → Encontra LinearWorkspaceBinding pelo teamId
  → Cria Task a partir do titulo/descricao da issue
  → Persiste LinearEventLog
```

---

## Triage Rules

O modelo `TriageRule` permite roteamento automatico de webhooks para ambientes especificos:

| Campo | Tipo | Descricao |
|-------|------|-----------|
| `source` | String | github/slack/linear |
| `condition` | Json | Condicao de match (regex, label, etc.) |
| `targetEnvironmentId` | String | Ambiente destino |
| `isEnabled` | Boolean | Ativa/desativa regra |

---

## Troubleshooting

| Problema | Causa Provavel | Solucao |
|----------|---------------|---------|
| Webhook nao recebido | URL nao configurada no provider | Verificar webhook URL no GitHub/Slack/Linear |
| Task nao criada via webhook | Binding nao configurado | Verificar SlackWorkspaceBinding/LinearWorkspaceBinding |
| OAuth falha | Mock token ativo | Aguardar implementacao OAuth real (GAP-004) |
| Evento duplicado | Retry do provider | Verificar por delivery ID no log |
| Conector mostra "desconectado" | Token expirado | Reconectar via /settings/connectors |

---

## Variaveis de Ambiente

| Variavel | Conector | Descricao |
|----------|----------|-----------|
| `GITHUB_CLIENT_ID` | GitHub | Client ID do OAuth App |
| `GITHUB_CLIENT_SECRET` | GitHub | Client Secret do OAuth App |
| `GITHUB_OAUTH_REDIRECT_URI` | GitHub | URL de callback |
| `GITHUB_WEBHOOK_SECRET` | GitHub | Secret para HMAC (PENDENTE) |
| `SLACK_SIGNING_SECRET` | Slack | Secret para HMAC (PENDENTE) |
| `LINEAR_WEBHOOK_SECRET` | Linear | Secret para HMAC (PENDENTE) |

---

## Referencias

- [API_CONTRACT.md](./API_CONTRACT.md) — Endpoints de integracao
- [DATA_MODEL.md](./DATA_MODEL.md) — Modelos de conectores
- [SECURITY.md](./SECURITY.md) — Webhook HMAC (GAP-003)
- [ROADMAP.md](./ROADMAP.md) — Plano de correcao
