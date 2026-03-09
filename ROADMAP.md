# ROADMAP DE IMPLEMENTAÇÃO — IA-AGGREGATOR PLATFORM
> Baseado na análise rigorosa do `prompt-mestre-claude-opus-4.6.txt`
> Data: 2026-03-08 | Versão: 2.0.0 (substitui roadmap anterior)

---

## Resumo Executivo

Foram identificados **26 gaps** organizados em 5 fases de prioridade. A plataforma possui uma base arquitetural sólida (Clean Architecture no backend, App Router no frontend, 50+ modelos Prisma, 17 provedores de IA), porém apresenta **vulnerabilidades críticas de segurança**, **lacunas em testes** e **funcionalidades de negócio pendentes** que precisam ser endereçadas antes de um lançamento em produção.

| Severidade | Quantidade | Área Principal |
|------------|-----------|----------------|
| Critico | 4 | Segurança |
| Alto | 6 | Arquitetura e Integração |
| Médio | 5 | Qualidade e Testes |
| Baixo | 5 | Polish e Documentação |
| Negócio | 6 | Funcionalidades Pendentes |

---

## FASE 1 — CRITICO (Segurança e Integridade) — Sprints 1-2

### GAP-001: JWT Sem Verificação de Assinatura no Frontend

- **Severidade**: CRITICA
- **Arquivo**: `frontend/src/server/codex/auth.ts`
- **Descrição**: A função `decodeJwtPayload()` apenas faz base64-decode da parte payload do JWT sem verificar a assinatura HMAC-SHA256. Qualquer JWT fabricado com `sub`, `email` e `name` arbitrários será aceito como válido.
- **Impacto**: Um atacante pode forjar tokens JWT e obter acesso total ao sistema sem credenciais.
- **Evidência**:
  ```typescript
  function decodeJwtPayload(token: string) {
    const parts = token.split('.');
    const payload = Buffer.from(parts[1], 'base64').toString('utf-8');
    return JSON.parse(payload);
  }
  ```
- **Correção Proposta**:
  1. Instalar dependência `jose` (compatível com Edge Runtime)
  2. Usar `jwtVerify()` com a mesma chave secreta do backend (`JWT_SECRET`)
  3. Validar claims `exp`, `iss` e `sub`
  4. Adicionar variável `JWT_SECRET` ao `.env.local.example`
- **Critério de Aceite**: JWT forjado retorna 401; JWT válido assinado pelo backend funciona normalmente.

---

### GAP-002: Cookies Sem Flags HttpOnly e Secure

- **Severidade**: CRITICA
- **Arquivo**: `frontend/src/app/api/auth/login/route.ts`
- **Descrição**: Os cookies `access_token` e `refresh_token` são definidos com `httpOnly: false` e `secure: false`, permitindo que scripts JavaScript client-side leiam os tokens via `document.cookie`.
- **Impacto**: Ataques XSS podem roubar tokens de autenticação. Sem `secure`, tokens trafegam em texto claro via HTTP.
- **Evidência**:
  ```typescript
  res.cookies.set('access_token', accessToken, {
    httpOnly: false,   // VULNERAVEL: deveria ser true
    secure: false,     // VULNERAVEL: deveria ser true em producao
    sameSite: 'lax',
    path: '/',
    maxAge: 900,
  });
  ```
- **Correção Proposta**:
  1. Alterar `httpOnly: true` para ambos os cookies
  2. Alterar `secure: process.env.NODE_ENV === 'production'`
  3. Considerar `sameSite: 'strict'` para o `refresh_token`
  4. Remover qualquer leitura client-side de cookies (`document.cookie`)
- **Critério de Aceite**: Browser DevTools mostra flags HttpOnly e Secure ativadas; XSS payload não consegue ler cookies.

---

### GAP-003: Webhooks Sem Verificação de Assinatura HMAC

- **Severidade**: CRITICA
- **Arquivos**:
  - `frontend/src/app/api/webhooks/github/route.ts`
  - `frontend/src/app/api/webhooks/slack/route.ts`
  - `frontend/src/app/api/webhooks/linear/route.ts`
- **Descrição**: Os três handlers de webhook aceitam qualquer payload JSON sem verificar assinatura HMAC. O GitHub envia `x-hub-signature-256`, o Slack envia `x-slack-signature`, e o Linear envia `linear-signature`, porém nenhum é validado.
- **Impacto**: Atacantes podem enviar payloads falsos que criam tasks, review runs e event logs fraudulentos.
- **Correção Proposta**:
  1. Criar módulo `frontend/src/server/codex/webhook-verify.ts`
  2. Implementar verificação HMAC com `crypto.timingSafeEqual()` para cada provider
  3. Adicionar variáveis: `GITHUB_WEBHOOK_SECRET`, `SLACK_SIGNING_SECRET`, `LINEAR_WEBHOOK_SECRET`
  4. Retornar HTTP 401 se assinatura inválida
- **Critério de Aceite**: Request sem assinatura retorna 401; request com assinatura inválida retorna 401; com assinatura válida processa normalmente.

---

### GAP-004: GitHub OAuth com Mock Determinístico

- **Severidade**: CRITICA
- **Arquivo**: `frontend/src/app/api/oauth/github/callback/route.ts`
- **Descrição**: O token de acesso GitHub é gerado como `token-from-${code.slice(0, 8)}` — valor determinístico sem chamada real ao `https://github.com/login/oauth/access_token`.
- **Impacto**: OAuth não funciona em produção. Tokens são previsíveis e não dão acesso real à API do GitHub.
- **Correção Proposta**:
  1. Implementar troca real de code por token via POST para `https://github.com/login/oauth/access_token`
  2. Usar `GITHUB_CLIENT_ID` e `GITHUB_CLIENT_SECRET` existentes no `.env`
  3. Armazenar tokens reais criptografados na tabela `OAuthConnection`
  4. Adicionar tratamento de erros (code expirado, permissão negada)
- **Critério de Aceite**: Fluxo OAuth real funciona com conta GitHub de teste; tokens permitem chamadas à API GitHub.

---

## FASE 2 — ALTO (Arquitetura e Integração) — Sprints 3-5

### GAP-005: Dois Sistemas de Banco de Dados Separados

- **Severidade**: ALTA
- **Arquivos**: `frontend/prisma/schema.prisma` + `backend/.../db/migration/V*.sql`
- **Descrição**: Backend usa Flyway com 9 schemas PostgreSQL (`auth`, `billing`, `chat`, `ai_gateway`, `partners`, `content`, `teams`, `analytics`, `audit`) enquanto frontend usa Prisma com schema `codex`. Ambos apontam para o mesmo banco `ia_aggregator` sem coordenação de migrações.
- **Impacto**: Schema drift potencial, dados duplicados (ex: `auth.users` vs `codex.User`), migrações podem conflitar.
- **Correção Proposta**:
  1. Documentar mapa de equivalência: `auth.users` <-> `codex.User`
  2. Criar script de validação de schema drift no CI
  3. Definir estratégia unificada: Prisma para codex, Flyway para o resto
  4. Long-term: criar views ou foreign data wrappers para eliminar duplicação

---

### GAP-006: Zero Testes de Integração no Backend

- **Severidade**: ALTA
- **Área**: `backend/ia-aggregator-application/src/test/`
- **Descrição**: Apenas 5 unit tests existem no backend Java. Zero testes de integração para os 17 provedores de IA, circuit breakers, retry logic e fallback routing.
- **Impacto**: Falhas silenciosas em providers, regressões não detectadas.
- **Correção Proposta**:
  1. Configurar TestContainers com PostgreSQL e Redis
  2. Implementar integration tests com WireMock para cada provider
  3. Testar circuit breaker, fallback e retry
  4. Meta: cobertura de 80% nos use cases

---

### GAP-007: Streaming Simulado no Chat

- **Severidade**: ALTA
- **Arquivo**: `frontend/src/stores/chat-store.ts` (linhas ~230-253)
- **Descrição**: Após receber resposta COMPLETA via `api.post('/ai/chat')`, o código simula streaming com loop artificial de 4 caracteres a cada 12ms.
- **Impacto**: UX artificial; latência desnecessária; usuário espera resposta completa antes de ver qualquer texto.
- **Nota**: O SSE de tasks do Codex (`/api/tasks/:id/events`) funciona com streaming REAL via `ReadableStream`.
- **Correção Proposta**:
  1. Verificar se backend `/api/v1/ai/chat` suporta SSE
  2. Alterar `sendMessage` para usar `fetch` com `ReadableStream` ou `EventSource`
  3. Processar chunks reais do provider em tempo real
  4. Manter fallback para providers que não suportam streaming

---

### GAP-008: Sem Rate Limiting Server-Side

- **Severidade**: ALTA
- **Descrição**: Não há implementação de rate limiting no servidor. Apenas tratamento client-side do status 429.
- **Impacto**: APIs vulneráveis a abuso, scraping e DDoS de baixa escala.
- **Correção Proposta**:
  1. Criar middleware usando Redis (já disponível via `codexRedis`)
  2. Implementar sliding window com chave `rl:{userId}:{endpoint}`
  3. Limites: 60 req/min APIs normais, 10 req/min tasks, 5 req/min webhooks
  4. Retornar headers `X-RateLimit-*`

---

### GAP-009: Redis Disponível Sem Estratégia de Cache

- **Severidade**: ALTA
- **Arquivo**: `frontend/src/server/codex/redis.ts`
- **Descrição**: Redis está configurado e funcional (IORedis com lazy connect), mas é usado APENAS pelo BullMQ. Nenhuma rota de API implementa cache.
- **Impacto**: Queries repetidas ao banco para dados que mudam raramente (repos, environments, integrations status).
- **Correção Proposta**:
  1. Criar helper `cache.ts` com `cacheGet`, `cacheSet`, `cacheInvalidate`
  2. Aplicar em rotas de leitura frequente: `/api/repositories`, `/api/environments`, `/api/integrations/status`
  3. TTL: 30s para listagens, 5min para configurações
  4. Invalidação automática em operações de escrita

---

### GAP-010: Dependência `rehype-raw` (Risco XSS via Markdown)

- **Severidade**: ALTA
- **Arquivo**: `frontend/package.json`
- **Descrição**: `rehype-raw` está como dependência, o que permite renderização de HTML bruto em Markdown. Embora o componente `assistant-markdown.tsx` não use diretamente, a dependência está disponível para import.
- **Impacto**: Se alguém importar `rehype-raw` sem sanitização, há risco de injeção HTML/XSS em respostas de IA.
- **Correção Proposta**:
  1. Se não usado em nenhum componente, remover do `package.json`
  2. Se necessário, adicionar `rehype-sanitize` como plugin posterior
  3. Adicionar teste unitário com payload XSS para validar sanitização

---

## FASE 3 — MEDIO (Qualidade e Testes) — Sprints 6-8

### GAP-011: Cobertura de Testes Insuficiente

- **Severidade**: MEDIA
- **Descrição**: Frontend possui apenas 4 unit tests e 6 E2E specs. Nenhuma rota API Codex (tasks, environments, webhooks) tem teste.
- **Cobertura Atual**:
  | Tipo | Quantidade | Arquivos |
  |------|-----------|----------|
  | Unit (frontend) | 4 | analytics.test.ts, api.test.ts, chat-store.test.ts, toast-store.test.ts |
  | E2E (Playwright) | 6 | quality-gates, auth-navigation, chat, analytics, filters, ai-provider |
  | Unit (backend) | 5 | ChatUseCaseImpl, Login, Register, RefreshToken, GetCurrentUser |
  | Integration | 0 | — |
- **Meta**: Cobertura de 80% em caminhos críticos (auth, tasks, environments)

---

### GAP-012: Sem Testes de Contrato de API

- **Severidade**: MEDIA
- **Descrição**: `API_CONTRACT.md` documenta 44+ endpoints, mas não há validação automatizada de que a implementação segue o contrato.
- **Correção**: Implementar contract tests usando supertest ou Playwright API testing.

---

### GAP-013: RBAC Incompleto

- **Severidade**: MEDIA
- **Descrição**: `requireCodexContext()` verifica sessão e workspace, mas não verifica role do membro (`OWNER`/`ADMIN`/`MEMBER`). Qualquer membro autenticado pode acessar endpoints administrativos.
- **Correção**: Adicionar wrapper `requireRole('ADMIN')` para rotas administrativas.

---

### GAP-014: Zero Testes de Acessibilidade (a11y)

- **Severidade**: MEDIA
- **Descrição**: Componente `skip-to-content.tsx` existe, mas não há testes a11y (axe-core, pa11y). Não-conformidade com WCAG 2.1 AA.
- **Correção**: Integrar `@axe-core/playwright` nos testes E2E.

---

### GAP-015: Rotas API com Dados Mock/Hardcoded

- **Severidade**: MEDIA
- **Descrição**: Vários endpoints retornam dados mock em vez de integração real:
  - Billing plans: dados hardcoded em `PLAN_DEFS`
  - GitHub OAuth: token determinístico
  - Analytics: agregações mock
- **Correção**: Mapear todas as rotas mock e criar plano de migração para integração real.

---

## FASE 4 — BAIXO (Polish e Documentação) — Sprints 9-10

### GAP-016: Inconsistência de Idioma na Documentação

- **Severidade**: BAIXA
- **Descrição**: Mix de pt-BR e inglês nos documentos `.md`. `AI_PROVIDER_CONFIGURATION.md` está em pt-BR, demais em inglês.
- **Correção**: Padronizar toda documentação em pt-BR.

---

### GAP-017: Faltam Documentos Operacionais

- **Severidade**: BAIXA
- **Descrição**: Não existem: guia de deploy, runbook de migrações, docs de monitoring/alerting, security threat model.
- **Correção**: Criar `DEPLOYMENT.md`, `TROUBLESHOOTING.md`, `PERFORMANCE.md`, `CONTRIBUTING.md`.

---

### GAP-018: Sem Guia de Performance/Profiling

- **Severidade**: BAIXA
- **Descrição**: Route budgets existem (`check-route-budgets.mjs`) mas sem documentação de como interpretar, otimizar ou monitorar.
- **Correção**: Criar `PERFORMANCE.md` com análise de bundle, queries e cache.

---

### GAP-019: Console.logs em Produção

- **Severidade**: BAIXA
- **Descrição**: Alguns logs condicionais existem (`if (process.env.NODE_ENV !== 'production')`), mas verificação completa de todo o código não foi feita.
- **Correção**: Audit de console.log/warn/error em todo o `src/`.

---

### GAP-020: Dependências Pesadas no Bundle

- **Severidade**: BAIXA
- **Descrição**: `framer-motion` (100KB+), stack completo de `rehype/remark` (40KB+), `@dnd-kit` podem inflar o bundle.
- **Correção**: Analisar com `@next/bundle-analyzer`, avaliar lazy loading ou alternativas mais leves.

---

## FASE 5 — GAPS DE NEGOCIO — Sprints 11-14

### GAP-021: Billing/Pagamentos Não Implementado

- **Severidade**: NEGOCIO
- **Descrição**: Schema Prisma define `BillingIntent`, `CreditBalance`, `CreditLedgerEntry`. Código define `PLAN_DEFS` com planos Starter/Pro/Enterprise. Porém nenhuma integração Stripe, sem processamento real de pagamentos.
- **Correção**: Integrar Stripe Checkout + Billing API + Webhooks.

---

### GAP-022: Verificação de Email Não Funciona

- **Severidade**: NEGOCIO
- **Descrição**: Backend tem tabela `auth.email_verification_tokens` (migração V2), mas nenhum endpoint ou serviço implementa o envio de email ou verificação do token.
- **Correção**: Implementar envio de email (SES/SendGrid), endpoint de verificação e fluxo no frontend.

---

### GAP-023: Reset de Senha Não Funciona

- **Severidade**: NEGOCIO
- **Descrição**: Backend tem tabela `auth.password_reset_tokens` (migração V2), mas nenhum fluxo implementado.
- **Correção**: Implementar solicitação de reset, envio de email com token, e página de redefinição.

---

### GAP-024: Multi-Tenant/Times Não Implementado

- **Severidade**: NEGOCIO
- **Descrição**: Modelos `Workspace`, `Membership` com roles existem no Prisma e `organizations` no Flyway, mas não há UI de gerenciamento de times, convites, ou troca de workspace.
- **Correção**: Implementar CRUD de workspace, convites, switch de organização.

---

### GAP-025: LGPD Parcial (Tabelas Sem Workflows)

- **Severidade**: NEGOCIO
- **Descrição**: Backend tem `auth.consent_records` e `auth.erasure_requests` (migração V2), mas sem automação para coleta de consentimento, exportação de dados ou execução de erasure.
- **Impacto**: Risco regulatório sob Lei Geral de Proteção de Dados.
- **Correção**: Implementar workflows de consentimento, data export (DSAR), e erasure automatizado.

---

### GAP-026: Feature Flags Sem Runtime Evaluation

- **Severidade**: NEGOCIO
- **Descrição**: Backend tem tabela `auth.feature_flags` com `rollout_percentage`, `enabled_for_users`, `enabled_for_orgs`, `enabled_for_tiers`. Frontend não tem nenhuma referência a feature flags.
- **Correção**: Criar SDK de feature flags no frontend, endpoint de avaliação, e integração com componentes.

---

## Cronograma Sugerido

| Sprint | Gaps | Foco |
|--------|------|------|
| S1 | GAP-001, GAP-002 | Segurança: JWT + Cookies |
| S2 | GAP-003, GAP-004 | Segurança: Webhooks + OAuth |
| S3 | GAP-008, GAP-009 | Infra: Rate Limit + Cache |
| S4 | GAP-010, GAP-007 | Integração: XSS + Streaming |
| S5 | GAP-005 | Arquitetura: Estratégia de DB |
| S6 | GAP-013 | Qualidade: RBAC |
| S7 | GAP-011 | Qualidade: Testes core |
| S8 | GAP-012 | Qualidade: Contract tests |
| S9 | GAP-006 | Qualidade: Backend integration tests |
| S10 | GAP-016 a GAP-020 | Polish |
| S11-S14 | GAP-021 a GAP-026 | Funcionalidades de negócio |

---

## Métricas de Sucesso

| Métrica | Atual | Meta |
|---------|-------|------|
| Vulnerabilidades Críticas | 4 | 0 |
| Cobertura de Testes (Frontend) | ~15% | 80% |
| Cobertura de Testes (Backend) | ~10% | 80% |
| Endpoints com Contract Test | 0/44 | 44/44 |
| Documentos Atualizados | 0/20 | 20/20 |
| Funcionalidades em Produção | Parcial | 22/26 capacidades |
