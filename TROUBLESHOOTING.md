# TROUBLESHOOTING — Guia de Resolucao de Problemas

> FAQ com 25+ problemas comuns, diagnostico e solucao.
> Versao: 2.0.0 | Ultima atualizacao: 2026-03-08

---

## Indice

1. [Bootstrap e Setup](#bootstrap-e-setup)
2. [Banco de Dados](#banco-de-dados)
3. [Redis e Filas](#redis-e-filas)
4. [Frontend (Next.js)](#frontend-nextjs)
5. [Backend (Spring Boot)](#backend-spring-boot)
6. [Autenticacao](#autenticacao)
7. [Tasks e Worker](#tasks-e-worker)
8. [Conectores e Webhooks](#conectores-e-webhooks)
9. [Chat e IA](#chat-e-ia)
10. [Testes](#testes)
11. [Build e CI](#build-e-ci)

---

## Bootstrap e Setup

### 1. Docker Desktop nao inicia

**Sintoma**: Script `start-solution.ps1` falha com timeout de 90s esperando Docker.

**Diagnostico**:
```powershell
docker info    # Verifica se Docker esta respondendo
```

**Solucao**:
- Abrir Docker Desktop manualmente e aguardar inicializacao completa
- Verificar se Hyper-V ou WSL2 esta habilitado
- No Windows: `wsl --update` se usando backend WSL2

### 2. `npm install` falha com erros de permissao

**Sintoma**: `EPERM`, `EACCES` durante install.

**Solucao**:
```powershell
# Limpar cache npm
npm cache clean --force

# Remover node_modules e lockfile
rm -rf frontend/node_modules frontend/package-lock.json
npm --prefix frontend install
```

### 3. `codex:bootstrap` falha

**Sintoma**: `prisma db push` retorna erro de conexao.

**Diagnostico**:
```powershell
# Verificar se PostgreSQL esta acessivel
docker exec ia-aggregator-db pg_isready -U ia_aggregator
```

**Solucao**:
- Verificar que `CODEX_DATABASE_URL` esta exportada corretamente
- Formato correto: `postgresql://ia_aggregator:ia_aggregator@localhost:5432/ia_aggregator?schema=codex`
- Verificar container: `docker compose ps`

---

## Banco de Dados

### 4. Schema drift entre Prisma e Flyway

**Sintoma**: Erros de coluna/tabela nao encontrada.

**Causa**: Dois ORMs operam no mesmo banco sem coordenacao (GAP-005).

**Solucao**:
```bash
# Re-sincronizar schema codex (Prisma)
npm --prefix frontend run codex:bootstrap

# Verificar estado do Flyway (via Actuator)
curl http://localhost:8080/actuator/flyway
```

### 5. PostgreSQL nao inicia (volume corrompido)

**Sintoma**: Container reinicia em loop, log mostra `FATAL: data directory has wrong ownership`.

**Solucao**:
```powershell
# CUIDADO: Perde dados locais!
docker compose down -v       # Remove volumes
docker compose up -d         # Recria do zero
npm --prefix frontend run codex:bootstrap
```

### 6. Conexao recusada ao banco

**Sintoma**: `ECONNREFUSED 127.0.0.1:5432`

**Diagnostico**:
```powershell
docker compose ps            # Container rodando?
docker logs ia-aggregator-db # Erros no log?
```

**Solucao**:
- Verificar se porta 5432 nao esta ocupada: `netstat -an | findstr 5432`
- Reiniciar container: `docker compose restart ia-aggregator-db`

---

## Redis e Filas

### 7. Redis nao conecta

**Sintoma**: `Error: connect ECONNREFUSED 127.0.0.1:6379`

**Solucao**:
```powershell
docker compose restart ia-aggregator-redis
redis-cli ping    # Deve retornar PONG
```

### 8. Tasks presas na fila (nunca executam)

**Sintoma**: Tasks ficam em status `queued` indefinidamente.

**Causa**: Worker BullMQ nao inicializado (lazy init) ou Redis desconectado.

**Diagnostico**:
```bash
redis-cli LLEN bull:codex:wait       # Tasks aguardando
redis-cli LLEN bull:codex:active     # Tasks ativas
```

**Solucao**:
1. Verificar conexao Redis
2. Reiniciar frontend (Next.js) — worker re-inicializa
3. Se persistir, limpar filas:
```bash
redis-cli DEL bull:codex:stalled
redis-cli DEL bull:codex:stalled-check
```

### 9. Tasks presas em estado `running_*` (timeout)

**Sintoma**: Task em `running_agent` ha mais de 30min.

**Causa**: Sem timeout global configurado (lacuna documentada em STATE_MACHINES.md).

**Solucao temporaria**:
1. POST `/api/tasks/{id}/cancel` para cancelar manualmente
2. Se API nao responder, marcar no banco:
```sql
UPDATE codex."Task" SET status = 'failed', "completedAt" = NOW() WHERE id = '<taskId>';
```

---

## Frontend (Next.js)

### 10. Pagina em branco / erro de hydration

**Sintoma**: Console mostra `Hydration failed because the server rendered HTML didn't match the client`.

**Solucao**:
- Limpar cache do Next.js: `rm -rf frontend/.next`
- Rebuild: `npm --prefix frontend run build`
- Verificar se `next-intl` esta configurado corretamente

### 11. API retorna 404 para rotas `/api/v1/*`

**Sintoma**: Requests para `/api/v1/auth/login` retornam 404.

**Causa**: Rewrite do Next.js nao funciona porque backend esta offline.

**Diagnostico**:
```bash
curl http://localhost:8080/actuator/health    # Backend rodando?
```

**Solucao**:
- Verificar que backend esta rodando em `:8080`
- Verificar `NEXT_PUBLIC_API_URL` no `.env.local`

### 12. Tailwind CSS nao aplica estilos

**Sintoma**: Classes Tailwind nao funcionam, layout quebrado.

**Solucao**:
- Verificar versao Tailwind v4 (sem `tailwind.config.js` — usa CSS nativo)
- Limpar cache: `rm -rf frontend/.next`
- Reiniciar dev server

### 13. Erro `Module not found: Can't resolve 'prisma'`

**Sintoma**: Build falha ao tentar importar Prisma client.

**Solucao**:
```bash
npm --prefix frontend run prisma:generate
```

---

## Backend (Spring Boot)

### 14. Backend nao inicia (porta ocupada)

**Sintoma**: `Web server failed to start. Port 8080 was already in use.`

**Solucao**:
```powershell
# Encontrar processo na porta
netstat -ano | findstr :8080

# Matar processo
taskkill /F /PID <pid>

# Ou alterar porta
$env:SERVER_PORT='8081'
```

### 15. Flyway migration falha

**Sintoma**: `FlywayException: Validate failed` no startup.

**Causa**: Migration ja aplicada foi modificada, ou migration fora de ordem.

**Solucao**:
```bash
# Verificar estado
curl http://localhost:8080/actuator/flyway

# Se dev local, reparar (CUIDADO em prod!)
# Adicionar na application.yml: flyway.repair-on-migrate: true
```

### 16. Circuit breaker aberto para provider de IA

**Sintoma**: Requests para um provider retornam erro imediatamente.

**Diagnostico**:
```bash
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state
```

**Solucao**:
- Aguardar 20 segundos (half-open automatico)
- Verificar API key do provider
- Verificar status do provider (status page do OpenAI, Anthropic, etc.)

---

## Autenticacao

### 17. Login retorna 401

**Sintoma**: Credenciais corretas mas login falha.

**Diagnostico**:
- Verificar se usuario existe no banco (schema `auth`)
- Verificar se `UserStatus` esta `ACTIVE`

**Solucao**:
```sql
-- Verificar usuario
SELECT email, status, email_verified FROM auth.users WHERE email = 'user@example.com';
```

### 18. JWT invalido / sessao expira rapido

**Sintoma**: Redirecionado para login frequentemente.

**Causa**: Access token expira em 15min, refresh token em 7 dias.

**Solucao**:
- Verificar que refresh token esta sendo renovado automaticamente
- Verificar que cookies estao sendo enviados (SameSite policy)
- NOTA: JWT nao verifica assinatura no frontend (GAP-001)

### 19. OAuth GitHub nao funciona

**Sintoma**: Callback OAuth retorna token invalido.

**Causa**: Mock OAuth ativo (GAP-004) — gera token deterministico fake.

**Solucao**: Aguardar implementacao real do OAuth. Atual: `token-from-${code.slice(0,8)}`

---

## Conectores e Webhooks

### 20. Webhook nao recebido

**Sintoma**: Provider envia webhook mas nenhum evento aparece.

**Diagnostico**:
1. Verificar URL do webhook no provider (GitHub/Slack/Linear)
2. Verificar logs do frontend para requests em `/api/webhooks/*`
3. Testar com curl:
```bash
curl -X POST http://localhost:3000/api/webhooks/github \
  -H "Content-Type: application/json" \
  -H "x-github-event: push" \
  -d '{"installation":{"id":123}}'
```

**Solucao**:
- Para desenvolvimento local: usar ngrok ou similar para expor porta
- NOTA: Webhooks nao verificam HMAC atualmente (GAP-003)

### 21. Task nao criada via Slack mention

**Sintoma**: Bot mencionado no Slack mas task nao aparece.

**Causa**: `SlackWorkspaceBinding` nao configurado para o canal.

**Solucao**:
1. Verificar binding: `GET /api/integrations/slack/install`
2. Verificar `channelId` e `defaultEnvironmentId` configurados
3. Verificar que o evento e `app_mention` (nao `message`)

---

## Chat e IA

### 22. Chat nao retorna resposta

**Sintoma**: Mensagem enviada mas sem resposta do assistente.

**Diagnostico**:
```bash
# Verificar backend responde
curl -X POST http://localhost:8080/api/v1/ai/chat \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"message":"hello","model":"gpt-4o-mini"}'
```

**Solucao**:
- Verificar API keys configuradas (pelo menos uma)
- Verificar circuit breakers nao estao abertos
- Verificar guardrails nao estao bloqueando (prompt > 5000 chars?)

### 23. Streaming do chat parece artificial

**Sintoma**: Resposta aparece caractere por caractere de forma mecanica.

**Causa**: Streaming e simulado — chunks de 4 chars a cada 12ms (GAP-007).

**Solucao**: Aguardar implementacao de SSE real no backend.

---

## Testes

### 24. Jest tests falham com `Cannot find module`

**Sintoma**: Testes nao encontram modulos mockados.

**Solucao**:
```bash
# Verificar mocks em frontend/test/mocks/
# Verificar moduleNameMapper em jest.config.js
npm --prefix frontend run test -- --clearCache
```

### 25. Playwright E2E falha localmente

**Sintoma**: Testes timeout ou nao encontram elementos.

**Solucao**:
```bash
# Instalar browsers
npx playwright install chromium

# Verificar app rodando
curl http://localhost:3001

# Executar com UI para debug
npm --prefix frontend run test:e2e:ui
```

---

## Build e CI

### 26. `npm run build` falha com erro de tipo

**Sintoma**: `Type error: Property 'X' does not exist on type 'Y'`.

**Solucao**:
```bash
# Verificar erros de tipo primeiro
npm --prefix frontend run type-check

# Se for tipo do Prisma, regenerar
npm --prefix frontend run prisma:generate
```

### 27. `perf:budget` falha

**Sintoma**: Route budget excedido.

**Diagnostico**:
```bash
npm --prefix frontend run perf:budget
# Mostra quais rotas excedem o budget
```

**Solucao**:
- Verificar imports desnecessarios na rota
- Usar dynamic imports (`next/dynamic`) para componentes pesados
- Ver [PERFORMANCE.md](./PERFORMANCE.md)

### 28. ESLint reporta erros de token Tailwind

**Sintoma**: `lint:tokens` falha com valores arbitrarios.

**Solucao**:
- Usar tokens do design system em vez de valores arbitrarios (`bg-[#123]` → `bg-primary`)
- Script de validacao: `frontend/scripts/validate-tailwind-arbitrary-values.mjs`

---

## Diagnostico Rapido

```bash
# Estado geral da solucao
docker compose ps                                    # Infra
curl -s http://localhost:8080/actuator/health | jq   # Backend
curl -s http://localhost:3001 -o /dev/null -w "%{http_code}"  # Frontend

# Logs recentes
docker logs --tail 50 ia-aggregator-db               # PostgreSQL
docker logs --tail 50 ia-aggregator-redis             # Redis
# Backend: .run/backend.err.log
# Frontend: .run/frontend.err.log

# Redis status
redis-cli INFO server | grep uptime
redis-cli INFO memory | grep used_memory_human
redis-cli DBSIZE

# PostgreSQL status
docker exec ia-aggregator-db psql -U ia_aggregator -c "SELECT count(*) FROM codex.\"Task\";"
```

---

## Referencias

- [RUNBOOK.md](./RUNBOOK.md) — Procedimentos operacionais
- [DEPLOYMENT.md](./DEPLOYMENT.md) — Deploy e CI/CD
- [SECURITY.md](./SECURITY.md) — Problemas de seguranca
- [ARCHITECTURE.md](./ARCHITECTURE.md) — Visao geral do sistema
