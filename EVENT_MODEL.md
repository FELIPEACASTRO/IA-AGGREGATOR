# EVENT_MODEL — Modelo de Eventos

> Especificacao do sistema de eventos em tempo real da plataforma IA-AGGREGATOR / Lume Codex Cloud.
> Versao: 2.0.0 | Ultima atualizacao: 2026-03-08

---

## Visao Geral

Eventos suportam acompanhamento em tempo real da execucao de tasks via **Server-Sent Events (SSE)**. Persistidos em `TaskEvent` e streamados via `/api/tasks/:id/events`.

## Transporte

| Componente | Descricao |
|-----------|-----------|
| **Persistencia** | Tabela `TaskEvent` com indice `[taskId, createdAt]` |
| **Stream** | SSE via `GET /api/tasks/:id/events` |
| **Protocolo** | `text/event-stream` |
| **Polling** | Consulta ao banco a cada 2 segundos |
| **Heartbeat** | `:heartbeat\n\n` a cada 15 segundos |

### Formato SSE

```
data: {"eventType":"task.created","status":"queued","message":"Task criada","metadata":{}}

:heartbeat

data: {"eventType":"agent.progress","status":"running_agent","message":"Analisando...","metadata":{"phase":"agent","progress":0.45}}

data: {"eventType":"task.completed","status":"completed","message":"Finalizada","metadata":{"durationMs":90000}}
```

---

## Eventos Emitidos

| Evento | Status Resultante | Fase | Descricao |
|--------|------------------|------|-----------|
| `task.created` | `queued` | — | Task criada e enfileirada |
| `task.queued` | `queued` | — | Na fila BullMQ |
| `task.started` | `preparing_environment` | provisioning | Worker iniciou |
| `provisioning.started` | `preparing_environment` | provisioning | Criando sandbox |
| `repository.cloned` | `cloning_repository` | repo_download | Repo clonado |
| `setup.completed` | `running_setup` | setup | Setup OK |
| `agent.progress` | `running_agent` | agent | Progresso (multiplos) |
| `validation.completed` | `validating` | validation | Testes finalizados |
| `diff.ready` | `generating_diff` | pr_push | Diff salvo |
| `pr.created` | `pr_ready` | pr_push | PR criado |
| `pr.updated` | — | pr_push | PR atualizado |
| `task.completed` | `completed` | — | Sucesso |
| `task.failed` | `failed` | — | Falhou |
| `task.cancelled` | `cancelled` | — | Cancelada |
| `task.archived` | `archived` | — | Arquivada |
| `task.unarchived` | `completed` | — | Restaurada |
| `task.retry` | `queued` | — | Reenfileirada |
| `task.followup_created` | — | — | Follow-up adicionado |

### Payload (TaskEvent)

```json
{
  "id": "cuid",
  "taskId": "cuid",
  "eventType": "agent.progress",
  "status": "running_agent",
  "message": "Analisando modulo de autenticacao...",
  "metadata": { "phase": "agent", "progress": 0.65 },
  "createdAt": "2026-03-08T10:01:30.000Z"
}
```

---

## Fases de Log (TaskLogChunk)

| Fase | Descricao | Status |
|------|-----------|--------|
| `provisioning` | Criacao sandbox | `preparing_environment` |
| `repo_download` | Clone Git | `downloading_repository` |
| `setup` | Setup script | `running_setup` |
| `maintenance` | Maintenance script | `running_maintenance` |
| `agent` | Agente IA | `running_agent` |
| `validation` | Testes/linting | `validating` |
| `pr_push` | Diff + PR | `generating_diff` |

### Mapeamento (task-runner.ts)

```typescript
const PHASE_TO_STATUS = {
  provisioning: 'preparing_environment',
  repo_download: 'downloading_repository',
  setup: 'running_setup',
  maintenance: 'running_maintenance',
  agent: 'running_agent',
  validation: 'validating',
  pr_push: 'generating_diff',
};
```

---

## Semanticas de Retry

| Aspecto | Valor |
|---------|-------|
| Retry automatico (BullMQ) | `attempts: 1` (sem retry) |
| Retry manual | `POST /api/tasks/:id/retry` |
| Dead letter queue | Nao implementado |
| Idempotencia | Por `taskId` |

---

## Garantias de Ordenacao

- Eventos ordenados por `createdAt` ASC dentro de cada `taskId`
- Indice `@@index([taskId, createdAt])` otimiza consulta
- Duplicacao possivel em reconexao SSE; cliente deve tratar por `id`

---

## Diagrama Temporal

```
Tempo ────────────────────────────────────────────────────►

  task.created    task.started      agent.progress (Nx)    task.completed
       │               │                │    │    │              │
  ┌────────┐  ┌──────────────┐  ┌────────────────────┐  ┌───────────┐
  │ queued │  │provisioning  │  │   running_agent    │  │ completed │
  │ ~0-5s  │  │repo/setup    │  │ (maior duracao)    │  │ diff + PR │
  │        │  │ ~5-30s       │  │ ~30s-5min          │  │ ~5-15s    │
  └────────┘  └──────────────┘  └────────────────────┘  └───────────┘
```

---

## Referencias

- [STATE_MACHINES.md](./STATE_MACHINES.md) — Transicoes de estado
- [API_CONTRACT.md](./API_CONTRACT.md) — Endpoint SSE
- [DATA_MODEL.md](./DATA_MODEL.md) — TaskEvent, TaskLogChunk
- **Implementacao**: `frontend/src/server/codex/task-runner.ts`
