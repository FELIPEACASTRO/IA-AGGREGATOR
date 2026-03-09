# RUNBOOK — Guia Operacional

> Procedimentos de bootstrap, operacao, monitoramento e recuperacao de desastres.
> Versao: 2.0.0 | Ultima atualizacao: 2026-03-08

---

## Pre-Requisitos

| Software | Versao Minima | Verificacao |
|----------|--------------|-------------|
| Node.js | 22.x | `node -v` |
| npm | 10.x | `npm -v` |
| Java (Temurin) | 21 | `java -version` |
| Maven | 3.9+ | `mvn -v` |
| Docker Desktop | 4.x | `docker --version` |
| Git | 2.40+ | `git --version` |
| PowerShell | 7.x (Windows) | `$PSVersionTable` |

---

## Bootstrap Completo

### 1. Infraestrutura (Docker)

```powershell
# Subir PostgreSQL 16 (pgvector) + Redis 7
docker compose up -d

# Verificar saude dos containers
docker compose ps
# ia-aggregator-db     → healthy
# ia-aggregator-redis  → healthy
```

**Portas**:
- PostgreSQL: `localhost:5432` (user: `ia_aggregator`, pass: `ia_aggregator`, db: `ia_aggregator`)
- Redis: `localhost:6379`

### 2. Backend (Spring Boot)

```powershell
cd backend
mvn clean verify -DskipTests   # Build rapido (sem testes)
# OU
mvn clean verify               # Build completo com testes

# Iniciar
java -jar ia-aggregator-presentation/target/ia-aggregator-presentation-1.0.0-SNAPSHOT.jar
```

**Endpoint**: `http://localhost:8080`
**Swagger**: `http://localhost:8080/swagger-ui.html`

### 3. Frontend (Next.js)

```powershell
cd frontend

# Configurar variaveis de ambiente
$env:CODEX_DATABASE_URL='postgresql://ia_aggregator:ia_aggregator@localhost:5432/ia_aggregator?schema=codex'
$env:CODEX_REDIS_URL='redis://localhost:6379'
$env:NEXT_PUBLIC_API_URL='http://localhost:8080'

# Instalar e bootstrap
npm install
npm run codex:bootstrap    # prisma:generate + prisma:push

# Desenvolvimento
npm run dev                # http://localhost:3000

# Producao local
npm run build && npm run start
```

### 4. Script Automatizado (Windows)

```powershell
# Boot completo (build + start)
powershell -ExecutionPolicy Bypass -File .\scripts\start-solution.ps1

# Sem rebuild
powershell -ExecutionPolicy Bypass -File .\scripts\start-solution.ps1 -SkipBuild

# Parar tudo
powershell -ExecutionPolicy Bypass -File .\scripts\stop-solution.ps1
```

O script `start-solution.ps1`:
1. Carrega chaves de IA de `IA\local.txt`
2. Inicia Docker Desktop se necessario (timeout 90s)
3. `docker compose up -d` com health check (timeout 60s)
4. Build backend (Maven) + frontend (npm)
5. Inicia servicos com logs em `.run/`
6. Smoke tests em `/actuator/health`, `/`, `/chat`, `/settings`
7. Salva PIDs em `.run/solution-pids.json`

---

## Variaveis de Ambiente

### Frontend (.env.local)

| Variavel | Padrao | Descricao |
|----------|--------|-----------|
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080` | URL do backend Spring Boot |
| `CODEX_DATABASE_URL` | — | Connection string PostgreSQL (schema codex) |
| `CODEX_REDIS_URL` | `redis://localhost:6379` | URL do Redis para BullMQ |
| `CODEX_TASK_CONCURRENCY` | `2` | Workers BullMQ simultaneos |
| `CODEX_LOCAL_REPO_CLONE_URL` | — | URL padrao para clone de repos |
| `GITHUB_CLIENT_ID` | — | OAuth App GitHub |
| `GITHUB_CLIENT_SECRET` | — | OAuth App GitHub |
| `GITHUB_OAUTH_REDIRECT_URI` | `http://localhost:3001/oauth/github/callback` | Callback OAuth |

### Backend (application.yml / env)

| Variavel | Padrao | Descricao |
|----------|--------|-----------|
| `DB_HOST` | `localhost` | Host do PostgreSQL |
| `DB_PORT` | `5432` | Porta do PostgreSQL |
| `DB_NAME` | `ia_aggregator` | Nome do banco |
| `DB_USER` | `ia_aggregator` | Usuario do banco |
| `DB_PASSWORD` | `ia_aggregator` | Senha do banco |
| `REDIS_HOST` | `localhost` | Host do Redis |
| `REDIS_PORT` | `6379` | Porta do Redis |
| `JWT_SECRET` | — | Secret para assinatura JWT (base64) |
| `SERVER_PORT` | `8080` | Porta do servidor |
| `OPENAI_API_KEY` | — | Chave API OpenAI |
| `ANTHROPIC_API_KEY` | — | Chave API Anthropic |
| `GEMINI_API_KEY` | — | Chave API Google Gemini |

---

## Task Worker (BullMQ)

- Worker inicializado **lazy** no primeiro enqueue dentro do runtime Next.js
- Redis obrigatorio em `CODEX_REDIS_URL`
- Concorrencia configuravel via `CODEX_TASK_CONCURRENCY` (padrao: 2)
- Sandbox de execucao em `.codex-runtime/tasks/{taskId}`

### Monitoramento de Filas

```bash
# Via Redis CLI
redis-cli -h localhost -p 6379
> KEYS bull:*          # Listar filas
> LLEN bull:codex:wait # Tasks aguardando
> ZCARD bull:codex:delayed # Tasks atrasadas
```

---

## Health Checks

### Backend

| Endpoint | Metodo | Descricao |
|----------|--------|-----------|
| `/actuator/health` | GET | Saude geral (DB, Redis, disk) |
| `/actuator/info` | GET | Versao e metadata |
| `/actuator/metrics` | GET | Metricas JVM e aplicacao |
| `/actuator/prometheus` | GET | Metricas formato Prometheus |

### Frontend

| Endpoint | Metodo | Descricao |
|----------|--------|-----------|
| `/api/auth/session` | GET | Sessao do usuario |
| `/api/tasks` | GET | Listagem de tasks (requer auth) |
| `/api/environments` | GET | Listagem de ambientes (requer auth) |
| `/api/tasks/{id}/events` | GET (SSE) | Stream de eventos realtime |

---

## Monitoramento

### Metricas Disponíveis (Actuator)

| Metrica | Tipo | Descricao |
|---------|------|-----------|
| `jvm.memory.used` | Gauge | Memoria JVM em uso |
| `jvm.threads.live` | Gauge | Threads ativas |
| `http.server.requests` | Timer | Latencia de requests HTTP |
| `resilience4j.circuitbreaker.*` | Gauge | Estado dos circuit breakers |
| `hikaricp.connections.active` | Gauge | Conexoes DB ativas |
| `hikaricp.connections.idle` | Gauge | Conexoes DB ociosas |

### Alertas Recomendados

| Condicao | Severidade | Acao |
|----------|-----------|------|
| Circuit breaker OPEN > 2 providers | CRITICO | Verificar conectividade com APIs de IA |
| HikariCP active > 18 (de 20) | ALTO | Investigar queries lentas, considerar pool maior |
| JVM heap > 85% | ALTO | Ajustar -Xmx ou investigar memory leak |
| Redis unreachable | CRITICO | Verificar container Redis, reiniciar se necessario |
| PostgreSQL unreachable | CRITICO | Verificar container PostgreSQL, verificar disk space |
| HTTP 5xx rate > 5% | ALTO | Verificar logs do backend |
| BullMQ wait queue > 50 | MEDIO | Aumentar CODEX_TASK_CONCURRENCY |

---

## Logs

### Localizacao

| Componente | Local | Formato |
|-----------|-------|---------|
| Backend (dev) | stdout | Logback padrao |
| Backend (script) | `.run/backend.out.log`, `.run/backend.err.log` | Logback |
| Frontend (dev) | stdout | Next.js padrao |
| Frontend (script) | `.run/frontend.out.log`, `.run/frontend.err.log` | Next.js |
| Docker (PostgreSQL) | `docker logs ia-aggregator-db` | PostgreSQL |
| Docker (Redis) | `docker logs ia-aggregator-redis` | Redis |

### Niveis de Log

| Profile | Root | Aplicacao | Security |
|---------|------|-----------|----------|
| default (dev) | INFO | DEBUG | INFO |
| prod | WARN | INFO | WARN |
| test | INFO | DEBUG | DEBUG |

---

## Procedures de Incidente

### Fila BullMQ Presa

```bash
# 1. Verificar tasks travadas
redis-cli KEYS "bull:codex:active*"

# 2. Reiniciar worker (reiniciar Next.js)
# Windows:
taskkill /F /PID <pid_frontend>
npm --prefix frontend run dev

# 3. Se persistir, limpar fila
redis-cli DEL bull:codex:active
redis-cli DEL bull:codex:stalled
```

### Schema Drift (Prisma vs Flyway)

```bash
# 1. Verificar estado do Prisma
npx prisma db pull --schema frontend/prisma/schema.prisma

# 2. Resincrronizar schema codex
npm --prefix frontend run codex:bootstrap

# 3. Verificar migracao Flyway
# Flyway roda automaticamente no boot do Spring Boot
# Verificar em: /actuator/flyway
```

### PostgreSQL Sem Espaco

```bash
# 1. Verificar tamanho
docker exec ia-aggregator-db psql -U ia_aggregator -c "SELECT pg_size_pretty(pg_database_size('ia_aggregator'));"

# 2. Vacuum
docker exec ia-aggregator-db psql -U ia_aggregator -c "VACUUM FULL;"

# 3. Se necessario, aumentar volume Docker
```

### Redis Memoria Cheia

```bash
# 1. Verificar memoria
redis-cli INFO memory

# 2. Limpar jobs completos antigos
redis-cli KEYS "bull:codex:completed*" | xargs redis-cli DEL

# 3. Configurar eviction policy se necessario
redis-cli CONFIG SET maxmemory-policy allkeys-lru
```

---

## Disaster Recovery

### Backup

| Componente | Metodo | Frequencia Recomendada |
|-----------|--------|----------------------|
| PostgreSQL | `pg_dump -U ia_aggregator ia_aggregator > backup.sql` | Diario |
| Redis | RDB snapshots (configurar `save` no redis.conf) | Horario |
| Codigo | Git (ja versionado) | Continuo |
| Env vars | Backup de `.env.local` e secrets | A cada mudanca |

### Restauracao

```bash
# PostgreSQL
docker exec -i ia-aggregator-db psql -U ia_aggregator ia_aggregator < backup.sql

# Redis (se usando RDB)
docker cp dump.rdb ia-aggregator-redis:/data/dump.rdb
docker restart ia-aggregator-redis

# Frontend schema
npm --prefix frontend run codex:bootstrap
```

### RTO/RPO Estimados (Desenvolvimento)

| Cenario | RTO | RPO |
|---------|-----|-----|
| Container crash | 2 min (docker compose up) | 0 (volumes persistentes) |
| Corrupcao de banco | 15 min (restore backup) | Ultimo backup |
| Perda total de host | 30 min (setup completo) | Ultimo commit + backup |

---

## Escalamento

### Horizontal (Futuro)

| Componente | Estrategia |
|-----------|-----------|
| Frontend (Next.js) | Multiplas instancias atras de load balancer |
| Backend (Spring Boot) | Multiplas instancias, sessao stateless (JWT) |
| BullMQ Workers | Aumentar `CODEX_TASK_CONCURRENCY` ou instancias |
| PostgreSQL | Read replicas para queries pesadas |
| Redis | Redis Cluster ou Sentinel |

### Vertical (Imediato)

| Componente | Recurso | Recomendacao Dev | Recomendacao Prod |
|-----------|---------|-----------------|-------------------|
| PostgreSQL | Memoria | 512MB | 2GB+ |
| Redis | Memoria | 256MB | 1GB+ |
| Backend JVM | Heap | 512MB | 2GB+ |
| Frontend Node | Memoria | 1GB | 2GB+ |

---

## Checklist de Operacao Diaria

- [ ] Verificar `/actuator/health` retorna `UP`
- [ ] Verificar containers Docker `healthy`
- [ ] Verificar fila BullMQ sem tasks presas (>10min)
- [ ] Verificar logs de erro no backend
- [ ] Verificar circuit breakers (nenhum em OPEN)
- [ ] Verificar espaco em disco (>20% livre)

---

## Referencias

- [DEPLOYMENT.md](./DEPLOYMENT.md) — CI/CD e deploy
- [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) — Problemas comuns
- [ARCHITECTURE.md](./ARCHITECTURE.md) — Visao geral do sistema
- [SECURITY.md](./SECURITY.md) — Seguranca operacional
