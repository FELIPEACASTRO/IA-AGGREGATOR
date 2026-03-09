# STATE_MACHINES — Maquinas de Estado

> Especificacao das maquinas de estado da plataforma IA-AGGREGATOR / Lume Codex Cloud.
> Versao: 2.0.0 | Ultima atualizacao: 2026-03-08

---

## Task Status (13 estados)

### Diagrama

```
  ┌───────┐  enqueue   ┌────────┐  worker    ┌──────────────────┐
  │ draft │──────────→ │ queued │──────────→ │preparing_environ.│
  └───────┘            └────────┘            └────────┬─────────┘
                                                      │
                       ┌──────────────────────────────┘
                       ▼
                ┌──────────────────┐    ┌────────────────────┐
                │downloading_repo  │──→ │cloning_repository  │
                └──────────────────┘    └────────┬───────────┘
                                                 │
                       ┌─────────────────────────┘
                       ▼
                ┌──────────────┐    ┌────────────────────┐
                │running_setup │──→ │running_maintenance │
                └──────────────┘    └────────┬───────────┘
                                             │
                       ┌─────────────────────┘
                       ▼
                ┌──────────────┐    ┌────────────┐
                │running_agent │──→ │ validating  │
                └──────────────┘    └──────┬─────┘
                                           │
                       ┌───────────────────┘
                       ▼
                ┌─────────────────┐    ┌──────────┐    ┌──────────┐
                │generating_diff  │──→ │ pr_ready │──→ │completed │
                └─────────────────┘    └──────────┘    └──────────┘

  TRANSICOES ALTERNATIVAS:
  - qualquer running → failed     (erro no worker)
  - qualquer running → cancelled  (usuario cancela)
  - completed ←→ archived         (arquivar / restaurar)
  - failed → queued               (retry manual)
```

### Tabela de Transicoes

| De | Para | Trigger | Guard | Evento |
|---|------|---------|-------|--------|
| `draft` | `queued` | POST /api/tasks | Zod OK | `task.created` |
| `queued` | `preparing_environment` | Worker pick | Job disponivel | `task.started` |
| `preparing_environment` | `downloading_repository` | Sandbox criado | Dir existe | `provisioning.started` |
| `downloading_repository` | `cloning_repository` | Clone iniciado | URL valida | — |
| `cloning_repository` | `running_setup` | Clone completo | Repo no disco | `repository.cloned` |
| `running_setup` | `running_maintenance` | Setup OK | Exit 0 | `setup.completed` |
| `running_maintenance` | `running_agent` | Maintenance OK | Exit 0 | — |
| `running_agent` | `validating` | Agente finalizado | Resposta OK | `agent.progress` |
| `validating` | `generating_diff` | Validacao OK | — | `validation.completed` |
| `generating_diff` | `pr_ready` | Diff salvo | Snapshot criado | `diff.ready` |
| `pr_ready` | `completed` | PR criado/skip | — | `task.completed` |
| *running* | `failed` | Erro | Exception | `task.failed` |
| *running* | `cancelled` | POST /cancel | User action | `task.cancelled` |
| `completed` | `archived` | POST /archive | — | `task.archived` |
| `archived` | `completed` | POST /unarchive | — | `task.unarchived` |
| `failed` | `queued` | POST /retry | — | `task.retry` |

### Timeouts (LACUNA IDENTIFICADA)

Nenhum timeout configurado. Tasks podem ficar presas indefinidamente em qualquer estado running. **Recomendacao**: Timeout global de 10min com transicao automatica para `failed`.

### Recuperacao de Erro

| Cenario | Estado | Recuperacao |
|---------|--------|------------|
| Setup script falha | `failed` | Corrigir script, POST /retry |
| Git clone falha | `failed` | Verificar URL, POST /retry |
| AI provider timeout | `failed` | Aguardar, POST /retry |
| Redis desconectado | `failed` | Reiniciar Redis, POST /retry |
| Worker crash | `queued` (permanece) | Reiniciar servidor |

---

## Environment Cache Status (5 estados)

```
  ┌──────┐  bootstrap   ┌───────────┐  sucesso   ┌──────┐
  │ cold │────────────→ │ preparing │──────────→ │ warm │
  └──────┘              └───────────┘            └──┬───┘
                              ▲                     │ config change
                              │                     ▼
                        ┌─────────────┐    ┌──────────────┐
                        │reset_pending│←───│ invalidated  │
                        └─────────────┘    └──────────────┘

  qualquer → failed (erro grave)
```

| De | Para | Trigger |
|---|------|---------|
| `cold` | `preparing` | Primeira task usa este env |
| `preparing` | `warm` | Setup + maintenance OK |
| `warm` | `invalidated` | PATCH /environments/:id |
| `invalidated` | `reset_pending` | POST /reset-cache |
| `reset_pending` | `preparing` | Proxima task executa |
| *qualquer* | `failed` | Erro grave |

---

## Pull Request Status (7 estados)

```
  ┌──────┐  diff    ┌───────┐  push   ┌──────┐
  │ none │────────→ │ draft │───────→ │ open │
  └──────┘          └───────┘         └──┬───┘
                                         │
                         ┌───────────────┼──────────┐
                         ▼               ▼          ▼
                   ┌──────────┐  ┌────────────┐ ┌────────┐
                   │  merged  │  │update_pend.│ │ closed │
                   └──────────┘  └──────┬─────┘ └────────┘
                                        └→ open (update feito)

  qualquer → failed (erro GitHub API)
```

---

## Regra Global

Toda transicao DEVE:
1. Emitir `TaskEvent` com novo status
2. Atualizar `task.updatedAt` via `@updatedAt`
3. Atualizar `task.status`
4. Se terminal (completed/failed/cancelled): definir `task.completedAt`

---

## Referencias

- [EVENT_MODEL.md](./EVENT_MODEL.md) — Eventos por transicao
- [DATA_MODEL.md](./DATA_MODEL.md) — Enums de status
- [API_CONTRACT.md](./API_CONTRACT.md) — Endpoints de transicao
