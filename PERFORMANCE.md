# PERFORMANCE — Guia de Performance

> Bundle analysis, cache strategy, query optimization e route budgets.
> Versao: 2.0.0 | Ultima atualizacao: 2026-03-08

---

## Visao Geral

| Metrica | Alvo | Status | Ferramenta |
|---------|------|--------|-----------|
| LCP (Largest Contentful Paint) | < 2.5s | Nao medido | Lighthouse |
| FID (First Input Delay) | < 100ms | Nao medido | Lighthouse |
| CLS (Cumulative Layout Shift) | < 0.1 | Nao medido | Lighthouse |
| Bundle size (JS initial) | < 200KB gz | Nao medido | `perf:budget` |
| TTFB (Time to First Byte) | < 800ms | Nao medido | Lighthouse |
| API P95 latency | < 500ms | Nao medido | Actuator/Prometheus |

---

## Bundle Analysis

### Route Budgets

O script `frontend/scripts/check-route-budgets.mjs` valida o tamanho de cada rota.

```bash
# Executar verificacao de budget
npm --prefix frontend run perf:budget
```

### Dependencias Pesadas

| Dependencia | Tamanho Estimado | Uso | Recomendacao |
|------------|-----------------|-----|-------------|
| `framer-motion` | ~150KB | Animacoes | Considerar CSS animations ou motion-lite |
| `react-markdown` + rehype stack | ~100KB | Render Markdown | Lazy load via `next/dynamic` |
| `@dnd-kit/core` | ~50KB | Drag and drop | Lazy load (uso pontual) |
| `cmdk` | ~20KB | Command palette (Cmd+K) | Lazy load |
| `@tanstack/react-query` | ~40KB | Data fetching | Necessario — manter |
| `zustand` | ~3KB | State management | Leve — manter |
| `zod` | ~15KB | Validacao | Necessario — manter |

### Otimizacoes Implementadas

| Otimizacao | Status | Descricao |
|-----------|--------|-----------|
| Dynamic imports (next/dynamic) | Parcial | Alguns componentes pesados ja usam |
| React.lazy + Suspense | Parcial | Chat components |
| Image optimization (next/image) | Ativo | Remote patterns configurados |
| Tree shaking | Ativo | Webpack 5 default |
| Code splitting por rota | Ativo | Next.js App Router default |

### Otimizacoes Recomendadas

| Otimizacao | Impacto | Esforco | Prioridade |
|-----------|---------|---------|-----------|
| Lazy load framer-motion | -150KB initial | Baixo | ALTO |
| Lazy load react-markdown stack | -100KB initial | Medio | ALTO |
| Lazy load @dnd-kit | -50KB initial | Baixo | MEDIO |
| Prefetch rotas criticas | +UX percebido | Baixo | MEDIO |
| Service Worker para cache | +Offline/cache | Alto | BAIXO |

---

## Cache Strategy

### Estado Atual

| Camada | Cache | Status |
|--------|-------|--------|
| Browser | HTTP Cache-Control headers | Parcial (assets estaticos) |
| CDN | Nao configurado | Nao implementado |
| API (Redis) | Disponivel mas nao usado | GAP-009 |
| Database | Query cache PostgreSQL | Default |
| BullMQ | Job results em Redis | Ativo |

### Estrategia Recomendada

```
Browser ──→ CDN ──→ Next.js ──→ Redis Cache ──→ PostgreSQL
  │           │         │            │              │
  │ static    │ ISR     │ API cache  │ TTL based   │ source
  │ assets    │ pages   │ middleware │ invalidation │ of truth
```

### Redis Cache (a implementar — GAP-009)

| Endpoint | TTL | Chave | Invalidacao |
|----------|-----|-------|-------------|
| GET /api/tasks | 30s | `tasks:{workspaceId}:{page}` | POST/PATCH/DELETE task |
| GET /api/environments | 60s | `envs:{workspaceId}` | PATCH /environments |
| GET /api/usage | 300s | `usage:{workspaceId}` | POST /credits |
| GET /api/integrations/status | 120s | `integrations:{workspaceId}` | POST /install |
| GET /api/analytics | 600s | `analytics:{workspaceId}:{range}` | Novo evento |

### HTTP Cache Headers Recomendados

| Recurso | Cache-Control |
|---------|--------------|
| Static assets (JS/CSS/images) | `public, max-age=31536000, immutable` |
| HTML pages | `private, no-cache` |
| API responses (publicas) | `public, max-age=60, stale-while-revalidate=30` |
| API responses (auth) | `private, no-store` |
| SSE streams | `no-cache, no-store` |

---

## Database Performance

### Queries Criticas

| Query | Frequencia | Indice Existente? | Recomendacao |
|-------|-----------|-------------------|-------------|
| Tasks por workspace + status | Muito alta | Sim (workspaceId) | Adicionar indice composto (workspaceId, status) |
| TaskEvents por taskId + ordem | Alta | Sim (taskId) | OK |
| Environments por workspaceId | Media | Sim (workspaceId) | OK |
| PullRequests por taskId | Media | Sim (taskId) | OK |
| AuditLog por workspace + data | Baixa | Nao | Adicionar indice (workspaceId, createdAt) |
| UsageEntry por workspace + periodo | Media | Nao | Adicionar indice (workspaceId, periodStart) |

### Pool de Conexoes (HikariCP)

| Parametro | Valor Dev | Valor Prod Recomendado |
|-----------|----------|----------------------|
| `minimum-idle` | 5 | 10 |
| `maximum-pool-size` | 20 | 50 |
| `idle-timeout` | 300s | 300s |
| `max-lifetime` | 600s | 1800s |
| `connection-timeout` | 30s | 10s |

### PostgreSQL Tuning Recomendado (Producao)

| Parametro | Dev | Prod |
|-----------|-----|------|
| `shared_buffers` | 128MB | 2GB (25% RAM) |
| `effective_cache_size` | 512MB | 6GB (75% RAM) |
| `work_mem` | 4MB | 64MB |
| `maintenance_work_mem` | 64MB | 512MB |
| `max_connections` | 100 | 200 |

---

## Backend Performance

### AI Provider Latency

| Provider | Timeout | Retries | Backoff | Circuit Breaker |
|---------|---------|---------|---------|----------------|
| Todos | 15000ms | 2 | 250ms | window=20, threshold=50% |

### Resilience4j Tuning

| Parametro | Valor | Descricao |
|-----------|-------|-----------|
| `slidingWindowSize` | 20 | Janela de avaliacao |
| `minimumNumberOfCalls` | 10 | Minimo para avaliar |
| `failureRateThreshold` | 50% | Threshold para abrir |
| `waitDurationInOpenState` | 20s | Tempo em OPEN |
| `permittedNumberOfCallsInHalfOpenState` | 5 | Calls em HALF_OPEN |

### JVM Tuning Recomendado (Producao)

```bash
java -jar app.jar \
  -Xms1g -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp/heapdump.hprof
```

---

## Frontend Performance

### SSE (Server-Sent Events)

| Parametro | Valor Atual | Recomendado |
|-----------|------------|-------------|
| Polling interval | 2s (ao banco) | SSE real (push) |
| Heartbeat | 30s | 15s |
| Reconnect | Automatico (EventSource) | Com backoff exponencial |

### Streaming do Chat (GAP-007)

| Parametro | Atual | Ideal |
|-----------|-------|-------|
| Mecanismo | Fake (4 chars / 12ms) | SSE real do backend |
| Latencia percebida | Artificial | Real |
| Memory | Acumula string completa | Chunks incrementais |

### React Performance

| Tecnica | Status | Recomendacao |
|---------|--------|-------------|
| React.memo | Parcial | Aplicar em listas de tasks |
| useMemo/useCallback | Parcial | Aplicar em handlers de eventos |
| Virtualizacao de listas | Nao implementado | `react-window` para task list |
| Suspense boundaries | Parcial | Adicionar em rotas pesadas |

---

## Metricas e Monitoramento

### Backend (Actuator + Prometheus)

```bash
# Metricas HTTP
curl http://localhost:8080/actuator/metrics/http.server.requests

# Metricas JVM
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Metricas HikariCP
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

# Circuit breakers
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state
```

### Frontend (a implementar)

| Metrica | Ferramenta | Status |
|---------|-----------|--------|
| Core Web Vitals | Lighthouse CI | Nao implementado |
| Bundle size tracking | `perf:budget` | Ativo |
| Error tracking | Sentry (recomendado) | Nao implementado |
| Real User Monitoring | Vercel Analytics (recomendado) | Nao implementado |

---

## Checklist de Performance Review

### Antes de Merge

- [ ] `npm run perf:budget` passa
- [ ] Nenhum import sincrono de dependencia >50KB
- [ ] Queries novas tem indice adequado
- [ ] Nenhum N+1 query introduzido
- [ ] Componentes de lista usam `key` adequado

### Antes de Release

- [ ] Lighthouse score > 90 (Performance)
- [ ] Bundle size nao cresceu >5% vs release anterior
- [ ] P95 latency de APIs criticas < 500ms
- [ ] Sem memory leaks em profiling de 30min
- [ ] Load test: 100 usuarios concorrentes sem degradacao

---

## Referencias

- [ARCHITECTURE.md](./ARCHITECTURE.md) — Stack e componentes
- [DEPLOYMENT.md](./DEPLOYMENT.md) — Configuracao de producao
- [ROADMAP.md](./ROADMAP.md) — GAP-007 (streaming), GAP-009 (cache)
- [TEST_PLAN.md](./TEST_PLAN.md) — Testes de performance
