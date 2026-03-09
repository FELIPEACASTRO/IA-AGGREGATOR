# DEPLOYMENT — Guia de Deploy e CI/CD

> Procedimentos de build, deploy, CI/CD pipelines e checklist de lancamento.
> Versao: 2.0.0 | Ultima atualizacao: 2026-03-08

---

## Visao Geral

```
  Desenvolvedor
       │
       ▼
  git push (main/develop)
       │
       ├─── .github/workflows/frontend-ci.yml
       │         │
       │         ├── lint + type-check
       │         ├── build
       │         ├── perf:budget
       │         ├── jest tests
       │         └── playwright E2E (chromium)
       │
       └─── .github/workflows/backend-ci.yml
                  │
                  ├── mvn clean verify
                  ├── testes unitarios
                  └── ArchUnit validation
```

---

## CI/CD Pipelines

### Frontend CI (`.github/workflows/frontend-ci.yml`)

**Triggers**: Push/PR para `main` ou `develop` em `frontend/**`

| Step | Comando | Timeout |
|------|---------|---------|
| Setup | Node.js 22, `npm ci` | 5min |
| Lint | `npm run lint` (ESLint + tokens) | 2min |
| Type Check | `npm run type-check` (tsc --noEmit) | 3min |
| Build | `npm run build` | 5min |
| Perf Budget | `npm run perf:budget` | 1min |
| Unit Tests | `npm run test -- --runInBand` | 5min |
| E2E Tests | `npx playwright test --project=chromium` | 10min |
| Quality Gates | `npm run test:quality` (visual + keyboard) | 5min |

### Backend CI (`.github/workflows/backend-ci.yml`)

**Triggers**: Push/PR para `main` ou `develop` em `backend/**`

**Servicos**: PostgreSQL pgvector:pg16, Redis 7-alpine

| Step | Comando | Timeout |
|------|---------|---------|
| Setup | Java 21 (Temurin), Maven cache | 2min |
| Build + Test | `mvn clean verify` | 10min |
| ArchUnit | Incluido no verify | — |
| Artifacts | Test reports (retencao: 7 dias) | — |

**Variaveis CI**:
```yaml
DB_HOST: localhost
DB_PORT: 5432
DB_NAME: ia_aggregator_test
DB_USER: test_user
DB_PASSWORD: test_password
REDIS_HOST: localhost
REDIS_PORT: 6379
JWT_SECRET: <base64-encoded>
```

---

## Build Local

### Frontend

```bash
cd frontend

# Desenvolvimento
npm run dev                    # Hot reload em http://localhost:3000

# Build de producao
npm run build                  # Gera .next/

# Verificacao completa (mesmo que CI)
npm run quality:ci
# Equivale a:
#   lint → type-check → test → build → perf:budget → test:quality
```

### Backend

```bash
cd backend

# Build completo
mvn clean verify              # Compila + testes + ArchUnit

# Build rapido (sem testes)
mvn clean package -DskipTests

# JAR de producao
ls ia-aggregator-presentation/target/ia-aggregator-presentation-1.0.0-SNAPSHOT.jar
```

---

## Estrutura de Build

### Frontend (Next.js 15)

```
frontend/
├── .next/                    # Build output
│   ├── server/               # Server-side bundles
│   ├── static/               # Client-side assets
│   └── cache/                # Build cache
├── prisma/
│   └── schema.prisma         # Schema codex (50+ modelos)
└── public/                   # Assets estaticos
```

### Backend (Maven Multi-Module)

```
backend/
├── ia-aggregator-common/         # DTOs, utils compartilhados
├── ia-aggregator-domain/         # Entidades, value objects, eventos
├── ia-aggregator-application/    # Use cases, portas
├── ia-aggregator-infrastructure/ # Adapters (17 AI providers, DB, Redis)
└── ia-aggregator-presentation/   # Controllers, config Spring Boot
    └── target/
        └── ia-aggregator-presentation-1.0.0-SNAPSHOT.jar
```

---

## Deploy para Producao (Futuro)

### Arquitetura de Deploy Recomendada

```
  Internet
     │
     ▼
  [CDN / WAF]
     │
     ▼
  [Load Balancer]
     │
     ├──→ [Next.js Container(s)]  ←── CODEX_DATABASE_URL, CODEX_REDIS_URL
     │         │
     │         └──→ [PostgreSQL] (schema: codex)
     │
     └──→ [Spring Boot Container(s)]  ←── DB_*, JWT_SECRET, AI_KEYS
               │
               ├──→ [PostgreSQL] (schemas: auth, billing, ...)
               └──→ [Redis] (sessions, cache)
```

### Docker Producao (Recomendado)

**Frontend Dockerfile** (a criar):
```dockerfile
FROM node:22-alpine AS builder
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci --production=false
COPY frontend/ ./
RUN npx prisma generate --schema prisma/schema.prisma
RUN npm run build

FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
COPY --from=builder /app/public ./public
EXPOSE 3000
CMD ["node", "server.js"]
```

**Backend Dockerfile** (a criar):
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY backend/ia-aggregator-presentation/target/ia-aggregator-presentation-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Checklist Pre-Deploy

- [ ] Todos os testes passando (`quality:ci` + `mvn verify`)
- [ ] Variaveis de ambiente de producao configuradas
- [ ] JWT_SECRET gerado com entropia suficiente (256+ bits)
- [ ] Cookies com `httpOnly: true`, `secure: true`, `sameSite: strict`
- [ ] Webhook HMAC secrets configurados (GAP-003 resolvido)
- [ ] OAuth real configurado (GAP-004 resolvido)
- [ ] Rate limiting habilitado
- [ ] CORS restrito aos dominios de producao
- [ ] Logs em nivel WARN/INFO (nao DEBUG)
- [ ] Health checks configurados no orquestrador
- [ ] Backup automatico de banco configurado
- [ ] SSL/TLS configurado no load balancer

### Checklist Pos-Deploy

- [ ] `/actuator/health` retorna `UP`
- [ ] Frontend carrega sem erros no console
- [ ] Login/registro funciona
- [ ] Task creation funciona
- [ ] SSE events chegam no browser
- [ ] Webhooks respondem com 200
- [ ] Metricas Prometheus coletando

---

## Rollback

### Frontend

```bash
# Se usando containers
docker rollback <container_id>

# Se usando Vercel/similar
# Revert para deploy anterior via dashboard

# Se manual
git checkout <commit_anterior>
npm run build && npm run start
```

### Backend

```bash
# Voltar para JAR anterior
java -jar ia-aggregator-presentation-<versao-anterior>.jar

# Flyway: NAO faz rollback automatico
# Se necessario, aplicar migration de reversao manual
```

### Banco de Dados

```bash
# CUIDADO: Flyway nao tem undo nativo
# 1. Restaurar backup antes da migration
pg_restore -U ia_aggregator -d ia_aggregator backup_pre_deploy.dump

# 2. Resincrronizar Prisma
npm --prefix frontend run codex:bootstrap
```

---

## Ambientes

| Ambiente | Frontend | Backend | Banco | Redis |
|----------|---------|---------|-------|-------|
| Local | localhost:3000 | localhost:8080 | localhost:5432 | localhost:6379 |
| CI | Efemero | Efemero | Container CI | Container CI |
| Staging | TBD | TBD | TBD | TBD |
| Producao | TBD | TBD | TBD | TBD |

---

## Seguranca no Deploy

### Secrets que NUNCA devem ir no repositorio

| Secret | Onde Configurar |
|--------|----------------|
| `JWT_SECRET` | Variavel de ambiente / Vault |
| `GITHUB_CLIENT_SECRET` | Variavel de ambiente / Vault |
| `OPENAI_API_KEY` | Variavel de ambiente / Vault |
| `ANTHROPIC_API_KEY` | Variavel de ambiente / Vault |
| Todas as `*_API_KEY` | Variavel de ambiente / Vault |
| `DB_PASSWORD` (prod) | Variavel de ambiente / Vault |
| `GITHUB_WEBHOOK_SECRET` | Variavel de ambiente / Vault |
| `SLACK_SIGNING_SECRET` | Variavel de ambiente / Vault |
| `LINEAR_WEBHOOK_SECRET` | Variavel de ambiente / Vault |

### Rotacao de Secrets

| Secret | Frequencia | Impacto da Rotacao |
|--------|-----------|-------------------|
| JWT_SECRET | Trimestral | Invalida todos os tokens ativos |
| API Keys (IA) | Semestral | Nenhum (hot swap) |
| DB Password | Trimestral | Requer restart dos servicos |
| Webhook Secrets | Semestral | Reconfigurar no provider |

---

## Referencias

- [RUNBOOK.md](./RUNBOOK.md) — Operacao diaria
- [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) — Resolucao de problemas
- [SECURITY.md](./SECURITY.md) — Requisitos de seguranca
- [ARCHITECTURE.md](./ARCHITECTURE.md) — Visao geral do sistema
