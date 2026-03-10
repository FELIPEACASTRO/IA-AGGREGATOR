# Auditoria 360 - IA-AGGREGATOR / Lume Codex Cloud

Data de fechamento: 2026-03-09
Escopo: backend Spring Boot, frontend Next.js, dados, contratos, documentacao raiz, documentos estrategicos `.docx` e benchmark externo em fontes oficiais.
Regra de evidencia: codigo executavel e comandos rodados valem mais do que `.md`; `.md` e `.docx` entram como intencao, posicionamento e governanca; auditorias antigas em `output/doc` foram usadas apenas como contexto historico.

## 0. Addendum - Double Check 100x (2026-03-10)

Este addendum substitui os pontos que estavam desatualizados no fechamento inicial de 2026-03-09.

### 0.1 Validacao tecnica atualizada (executada em 2026-03-10)

| Comando | Resultado |
|---|---|
| `frontend\\npm run lint` | **PASSA** |
| `frontend\\npm run type-check` | **PASSA** |
| `frontend\\npm test -- --runInBand --passWithNoTests` | **PASSA** (24 suites, 67 testes) |
| `frontend\\npm run build` | **PASSA** |
| `backend\\mvn -q -f backend/pom.xml test` | **PASSA** |
| `npx prisma validate --schema prisma/schema.prisma` (com `CODEX_DATABASE_URL`) | **PASSA** |
| `npx prisma migrate diff --from-empty --to-schema-datamodel prisma/schema.prisma --script` | **PASSA** (executado fora do sandbox) |

### 0.2 Correcoes confirmadas no ciclo hibrido

- Logout ponta a ponta foi alinhado:
  - backend exposto em `POST /api/v1/auth/logout` autenticado;
  - rota Next `/api/auth/logout` encaminha `Authorization: Bearer <access_token>` e `refreshToken` para revogacao real.
- Callback OAuth GitHub deixou de ser sintetico:
  - validacao de `state`;
  - bloqueio sem `code`;
  - troca real de `code` por token em `https://github.com/login/oauth/access_token`;
  - persistencia segura de credenciais.
- Webhooks GitHub/Slack/Linear agora exigem assinatura valida e retornam `401` sem efeitos colaterais quando invalidos.
- Runtime de task recebeu hardening FinOps:
  - bootstrap inicial de `RouteDecision`, `ModelRun` e custo estimado em bloco transacional;
  - reconciliacao para `FAILED` em falha precoce com fechamento de `taskRun`.
- Sessao frontend deixou de depender de token em `localStorage` para auth:
  - `auth-store` usa login/register/logout/session via rotas Next;
  - `streaming.ts` passou a usar proxy server-side em `/api/ai/chat/stream`;
  - analytics reports/events passaram a usar proxy server-side (`/api/analytics/reports` e `/api/analytics/reports/{id}/events`).

### 0.3 Evidencias de teste adicionadas no double check

- `frontend/src/app/api/auth/logout/route.test.ts`
- `frontend/src/app/api/oauth/github/callback/route.test.ts`
- `frontend/src/app/api/webhooks/github/route.test.ts`
- `frontend/src/app/api/webhooks/slack/route.test.ts`
- `frontend/src/app/api/webhooks/linear/route.test.ts`
- `frontend/src/app/api/usage/route.test.ts`
- `frontend/src/app/api/projects/route.test.ts`
- `frontend/src/app/api/ai/chat/stream/route.test.ts`
- `frontend/src/app/api/analytics/reports/route.test.ts`
- `frontend/src/app/api/analytics/reports/[reportId]/events/route.test.ts`
- `frontend/src/server/codex/task-runner.test.ts`
- `backend/ia-aggregator-presentation/src/test/java/com/ia/aggregator/presentation/auth/AuthControllerTest.java` (caso sem autenticacao em logout)
- `backend/ia-aggregator-application/src/test/java/com/ia/aggregator/application/auth/usecase/AuthSessionRevocationFlowTest.java`

### 0.4 Riscos residuais que continuam bloqueando go-live externo

- Validacao de migracao em banco real (apply + rollback em staging) ainda depende de ambiente com Docker/PostgreSQL ativo; no ambiente atual houve bloqueio operacional do daemon Docker.

## 1. Veredito Executivo

**Veredito**: a solucao nao esta pronta para lancamento pago publico nem para venda enterprise B2B. Ela esta pronta para **alpha tecnico controlado** e para **POCs dirigidas**, porque ja possui um backend relativamente maduro e um runtime Codex funcional, mas ainda carrega fragilidades criticas de sessao no frontend, conectores mockados/sinteticos e inconsistencias fortes entre produto prometido, contrato documentado e comportamento real.

**Tese central**: hoje existem **tres narrativas de produto sobrepostas**:

1. **Backend agregador de IA** relativamente amplo, com auth JWT real, catalogo de modelos, billing, analytics, marketplace, execution e endpoints multimodais.
2. **Frontend Lume Codex Cloud** focado em tasks de engenharia, SSE, evidencias, repositorios e ambientes.
3. **Plano de negocio "agregador brasileiro de IA"** com 220 funcionalidades, localizacao BR, Pix/boleto, LGPD nativa e suporte PT-BR 7/7.

Essas tres narrativas ainda nao convergiram em um unico produto operacional.

### Scorecard

| Dimensao | Nota | Leitura |
|---|---:|---|
| Arquitetura backend | 7.5/10 | Boa base modular, camadas coerentes, varias capacidades reais |
| Seguranca backend | 7.0/10 | JWT assinado, refresh em Redis, rate limiting Redis e HMAC de webhooks existem |
| Seguranca frontend | 3.0/10 | Sessao baseada em decode sem verificacao, tokens em `localStorage`, middleware valida apenas presenca de cookie |
| Completude funcional | 5.5/10 | Ha muita superficie implementada, mas varias rotas ainda sao hibridas ou sinteticas |
| Consistencia contrato x runtime | 4.0/10 | Documentacao raiz esta materialmente desatualizada em varios pontos |
| Prontidao comercial BR/B2B | 3.5/10 | Tese comercial existe; operacao, monetizacao local e governanca enterprise ainda nao |
| Testabilidade | 5.5/10 | Backend tem base melhor que a documentada; frontend ainda quebra em suites basicas |

### Classificacao das capacidades centrais

| Capacidade | Classificacao | Evidencia principal |
|---|---|---|
| Auth JWT e refresh no backend | **Real** | `JwtTokenProvider`, `SecurityConfig`, `AuthController` |
| Sessao server-side do frontend | **Parcial com falha critica** | `src/server/codex/auth.ts` decodifica JWT sem verificar assinatura |
| Login seguro por cookie httpOnly | **Implementado mas fora do fluxo principal** | `src/app/api/auth/login/route.ts` seta cookie seguro, mas `auth-store` nao usa essa rota |
| Logout ponta a ponta | **Inconsistente** | frontend tenta `POST /api/v1/auth/logout`, mas backend nao expoe `/logout` |
| OAuth GitHub | **Mock/prototipo** | callback grava `token-from-{code}` e marca status `CONNECTED` |
| Webhooks backend Spring | **Real** | `WebhookVerifier` + `WebhookController` validam HMAC |
| Webhooks frontend Next | **Parcial/inseguro** | 3 rotas publicas aceitam payload sem HMAC |
| Catalogo de modelos backend | **Real** | `ModelCatalogRepositoryImpl`, `ModelCatalogController`, `V7__ai_gateway_module.sql` |
| Catalogo de modelos frontend | **Parcial/hibrido** | busca backend, mas faz fallback para catalogo estatico |
| Chat streaming | **Real no backend, parcial no frontend** | existe `/api/v1/ai/chat/stream`; testes do store estao desatualizados |
| Runtime Codex (tasks, logs, diff, SSE) | **Real como scaffold operacional** | Prisma + BullMQ + `task-runner.ts` + SSE por `ReadableStream` |
| PR lifecycle no frontend | **Mock persistido** | URL e numero de PR sao gerados artificialmente |
| Billing backend | **Real** | controller, use cases, adapter Stripe e migracao V5 |
| Billing frontend | **Parcial/hibrido** | usa backend quando consegue, mas aplica heuristicas e fallbacks locais |
| Compliance export | **Mock persistido** | job criado como `completed` com `downloadUrl` sintetica |
| Analytics | **Real no backend e separado no frontend** | backend recebe reports; frontend agrega Prisma local |
| Marketplace/execution/platform | **Parcial** | schemas e controllers existem; pouco acoplamento com frontend e baixa evidencia de uso real |

## 2. Metodo e Evidencias Primarias

### Fontes internas auditadas

- Codigo-fonte backend e frontend, excluindo artefatos gerados como fonte primaria.
- Documentos raiz auditados: `README.md`, `ARCHITECTURE.md`, `PRODUCT_SPEC.md`, `PARITY_MATRIX.md`, `ROADMAP.md`, `SECURITY.md`, `API_CONTRACT.md`, `DATA_MODEL.md`, `EVENT_MODEL.md`, `TEST_PLAN.md`.
- Documentos estrategicos auditados em `docs/`:
  - `Especificacao_Funcional_v2_Agil_Completo.docx`
  - `Plano_de_Negocio_Plataforma_IA.docx`
  - `Especificacao_Funcional_Concorrentes_IA.docx`
  - `Plano_Mestre_Consolidado_v3.docx`

### Validacao executada em 2026-03-09

| Comando | Resultado | Leitura |
|---|---|---|
| `backend\\mvn test -q` | **PASSA** | backend testado com sucesso |
| `frontend\\npm test -- --runInBand --passWithNoTests` | **FALHA** | 2 suites quebradas: `src/stores/chat-store.test.ts` e `src/app/prompts/page.test.tsx` |
| `frontend\\npm run type-check` | **FALHA em estado limpo** | `tsconfig.json` inclui `.next/types/**/*.ts`; sem build previo, o type-check quebra |
| `frontend\\npm run build` | **PASSA** | build Next 15.5.12 concluido com sucesso |
| `frontend\\npm run type-check` apos build | **PASSA** | evidencia de dependencia indevida da ordem `build -> type-check` |

### Falhas de teste observadas

- `src/stores/chat-store.test.ts`: o teste ainda espera `api.post('/ai/chat', ...)`, mas o store passou a usar `streamChat()` e o mock ficou obsoleto.
- `src/app/prompts/page.test.tsx`: quebra com `ReferenceError: fetch is not defined`, porque a pagina faz `fetch()` em `useEffect` e o teste nao mocka o ambiente.

## 3. Mapa Arquitetural Real

### Backend Spring

| Modulo Maven | Main `.java` | Test `.java` | Leitura |
|---|---:|---:|---|
| `ia-aggregator-application` | 208 | 5 | use cases, DTOs, ports |
| `ia-aggregator-common` | 4 | 0 | erros e excepcoes compartilhadas |
| `ia-aggregator-domain` | 50 | 4 | entidades, enums, regras centrais |
| `ia-aggregator-infrastructure` | 248 | 25 | adapters, JPA, seguranca, providers, Redis |
| `ia-aggregator-presentation` | 50 | 5 | controllers HTTP e DTOs de borda |
| **Total** | **560** | **39** | **599 arquivos Java auditados por inventario estrutural** |

### Frontend Next / Codex

| Area | Arquivos | Leitura |
|---|---:|---|
| `frontend/src/app` | 98 | paginas App Router + `route.ts` |
| `frontend/src/components` | 33 | shell, componentes UI e codex |
| `frontend/src/lib` | 10 | HTTP, analytics, streaming, catalogo |
| `frontend/src/stores` | 6 | Zustand |
| `frontend/src/server` | 11 | runtime Codex server-side |
| `frontend/prisma` | 1 | schema `codex` |
| `frontend/e2e` | 7 | Playwright |

### Superficie publica

- Backend: **41 controllers**.
- Frontend: **47 API routes** e **38 paginas `page.tsx`**.

### Dados

- **Prisma** controla o schema `codex`.
- **Flyway/JPA** controla os schemas `auth`, `billing`, `chat`, `ai_gateway`, `content`, `teams`, `analytics`, `audit`, `execution`, `platform`, `marketplace`.
- Isso cria **dois sistemas de verdade operando sobre o mesmo banco PostgreSQL**.

## 4. Auditoria Backend

### 4.1 O que esta realmente pronto no backend

- A organizacao em `domain -> application -> infrastructure -> presentation` esta, no nivel macro, coerente com arquitetura hexagonal.
- A autenticacao backend e real:
  - `AuthController` expoe `register`, `login`, `refresh`, `me`.
  - `JwtTokenProvider` assina e valida JWT com JJWT.
  - refresh token e revogacao usam Redis.
- O hardening backend existe:
  - `SecurityConfig` usa JWT filter, CORS e politica stateless.
  - `RateLimitFilter` aplica limite por usuario/IP com Redis.
  - `WebhookVerifier` suporta GitHub, Slack e Linear com comparacao em tempo constante.
- O catalogo de modelos e real:
  - `ModelCatalogRepositoryImpl` le `ai_gateway.models/providers`.
  - `V7__ai_gateway_module.sql` semeia providers e modelos default.
- Billing backend tambem existe de forma concreta:
  - `BillingController` expoe planos, assinatura, uso e budgets.
  - `StripePaymentGatewayAdapter` fala com Stripe por HTTP e valida webhook.
  - `V5__billing_module.sql` cria planos, subscriptions, usage, credit transactions, invoices e budgets.
- Streaming real existe no backend:
  - `AiStreamingChatController` entrega `text/event-stream`.

### 4.2 O que esta parcial no backend

- `analytics/events` e publico por design, o que facilita ingestao, mas aumenta superficie de abuso e dados ruidosos.
- `execution`, `platform` e `marketplace` possuem schema e controllers, mas pouca evidencia de uso real integrado pelo frontend; estao mais proximos de **capacidade parcial** do que de produto pronto.
- Ha grande amplitude de controllers de IA multimodal, mas a solucao comercial ainda nao demonstrou coerencia de produto em volta deles.

### 4.3 Achados backend

1. **Seguranca backend esta melhor do que a documentacao afirma**. Os docs raizes ainda falam em "sem HMAC" e "sem rate limiting", mas o codigo atual ja implementa ambos no backend.
2. **Nao existe endpoint backend de logout**, apesar de o frontend assumir esse contrato.
3. **Billing backend esta mais avancado do que `PRODUCT_SPEC.md` e `PARITY_MATRIX.md` reconhecem**.
4. **A area de execution/platform/marketplace esta tecnicamente presente, mas comercialmente nao foi transformada em experiencia de produto coerente**.

## 5. Auditoria Frontend e Runtime Codex

### 5.1 O que esta realmente pronto no frontend

- O runtime Codex tem nucleo funcional:
  - `POST /api/tasks` cria task, persiste entrada e enfileira job.
  - `task-runner.ts` provisiona sandbox, prepara repo, executa setup/manutencao, gera artefatos e eventos.
  - `GET /api/tasks/[id]/events` implementa SSE com `ReadableStream`.
  - `events.ts` persiste `TaskEvent` e `TaskLogChunk`.
- O frontend compila em producao.
- O chat passou a consumir um helper de streaming real (`src/lib/streaming.ts`) e o backend oferece `/api/v1/ai/chat/stream`.

### 5.2 O que esta parcial, hibrido ou enganoso

- A camada de sessao e o maior gap:
  - `src/server/codex/auth.ts` aceita qualquer JWT que consiga ser decodificado, sem verificar assinatura.
  - `middleware.ts` so confere se o cookie existe.
  - `requireCodexContext()` usa essa sessao para **auto-provisionar** usuario, workspace, repositorio, environment e saldo em `ensureWorkspaceForUser()`.
- O caminho seguro `POST /api/auth/login` existe, mas o **fluxo principal de UI nao o usa**:
  - `auth-store.ts` faz login direto contra o backend via `src/lib/api.ts`.
  - o store grava tokens em `localStorage` e em cookies JS-legiveis por `document.cookie`.
- Logout tambem e inconsistente:
  - a rota Next `/api/auth/logout` apenas limpa cookies;
  - o store tenta chamar `/api/v1/auth/logout`, que nao existe no backend, e so depois limpa armazenamento local.
- OAuth GitHub continua mockado:
  - `src/app/api/oauth/github/callback/route.ts` grava `token-from-{code}` e marca instalacao como `CONNECTED`.
- Conectores Slack/Linear/GitHub sao sinteticos:
  - as rotas de install fazem `upsert` direto em Prisma sem handshake externo real.
- Webhooks Next nao validam assinatura:
  - GitHub, Slack e Linear aceitam JSON direto e criam side effects.
- Billing frontend e hibrido:
  - busca planos do backend quando consegue;
  - se falhar, cai para definicoes estaticas;
  - uso mensal pode ser inferido por heuristica de task count.
- Compliance e compra de creditos ainda sao mock persistido:
  - export job nasce `completed` com URL sintetica;
  - compra de creditos cria `billingIntent` `COMPLETED` sem pagamento externo.
- PR lifecycle no frontend tambem e sintetico:
  - URL e `externalNumber` sao gerados artificialmente.

### 5.3 Achados frontend

1. **Existe uma falsa sensacao de seguranca porque ha uma rota Next que seta cookie `httpOnly`, mas o fluxo principal de login a contorna completamente**.
2. **A sessao do Codex e confiada a um decode local de JWT**, nao a uma verificacao criptografica.
3. **Os conectores aparentam integracao real na UI, mas varias rotas apenas marcam entidades como `CONNECTED` no banco**.
4. **O runtime de tasks e o ativo mais forte do frontend**; o restante do produto ainda tem muito comportamento de demo persistida.
5. **O frontend sofre de arquivos monoliticos em areas centrais**, por exemplo:
   - `src/app/settings/analytics/debug/diagnostics-page.tsx`: 1095 linhas
   - `src/app/chat/page.tsx`: 570 linhas
   - `src/server/codex/task-runner.ts`: 509 linhas
   - `src/stores/chat-store.ts`: 348 linhas

## 6. Dados, Contratos e Documentacao

### 6.1 Mismatches de contrato

| Tema | Contrato/documentacao | Runtime real |
|---|---|---|
| Logout backend | frontend e docs assumem `POST /api/v1/auth/logout` | endpoint nao existe no backend |
| Login seguro por cookie | `API_CONTRACT.md` sugere rota frontend de cookie management | fluxo principal usa `auth-store` + `localStorage` + cookies JS |
| Cookies inseguros | `SECURITY.md` ainda aponta `httpOnly: false, secure: false` na rota Next | a rota Next ja foi corrigida, mas o store reintroduz risco ao persistir tokens no client |
| Webhooks sem HMAC | docs dizem gap geral | no backend Spring isso ja foi implementado; no frontend Next continua vulneravel |
| Sem rate limiting | docs dizem pendente | backend possui `RateLimitFilter` Redis |
| Billing nao iniciado | `PRODUCT_SPEC.md` e `PARITY_MATRIX.md` minimizam billing | backend ja tem controller, adapter Stripe e schema |
| Cobertura de testes | `PARITY_MATRIX.md` fala em 17 testes totais | codigo atual contem 39 testes Java, 12 testes frontend e 6 specs E2E |

### 6.2 Prisma vs Flyway

**Diagnostico**: o risco de drift e real e ja e estrutural.

| Ponto | Evidencia | Impacto |
|---|---|---|
| Identidade duplicada | `auth.users` no backend e `codex.User` no frontend | duas representacoes de usuario |
| Billing duplicado | backend usa `billing.*`; frontend usa `BillingIntent`, `CreditBalance`, `CreditLedgerEntry` no `codex` | risco de dupla contabilidade |
| Analytics duplo | backend ingere reports em `analytics.*`; frontend agrega `UsageEntry`/`AnalyticsAggregate` em `codex` | dashboards podem divergir |
| Sessao contextual | Next usa `ensureWorkspaceForUser()` para materializar workspace default em `codex` | tenant bootstrap nao conversa nativamente com `auth.organizations` |

**Leitura**: hoje a arquitetura de dados funciona melhor como **duas plataformas coladas no mesmo banco** do que como um unico produto modelado de ponta a ponta.

### 6.3 O que os `.docx` prometem versus o que o codigo entrega

| Documento | Promessa central | Estado real |
|---|---|---|
| `Especificacao_Funcional_v2_Agil_Completo.docx` | "9 concorrentes | 198 funcionalidades | 22 inovacoes exclusivas" | backlog/intencao; nao corresponde a capacidade entregue de ponta a ponta |
| `Plano_de_Negocio_Plataforma_IA.docx` | assinatura unica de `R$ 99/mes` ou `R$ 1.000/ano` para 30+ modelos | billing real de producao ainda nao esta conectado a uma operacao comercial BR completa |
| `Especificacao_Funcional_Concorrentes_IA.docx` | Inner AI lider brasileiro, 50+ modelos, suporte PT-BR; espaco para nova plataforma nacional | a tese de mercado faz sentido, mas a execucao ainda nao sustenta essa ambicao |
| `Plano_Mestre_Consolidado_v3.docx` | "Nubank da IA", Pix/boleto, LGPD nativa, suporte PT-BR 7/7 | praticamente todo esse pacote ainda e **doc-only** |

**Observacao critica**: varios documentos raiz carregam data de 2026-03-08 e mesmo assim ja estao materialmente desatualizados em 2026-03-09. Isso aponta problema de governanca documental, nao apenas atraso pontual.

## 7. Benchmark Externo - SaaS IA BR/B2B

Data de consulta externa: 2026-03-09.
Principio: fontes oficiais sempre que possivel.

| Player | Sinal oficial observado | Implicacao competitiva para o IA-AGGREGATOR |
|---|---|---|
| **Inner AI** | blogs e help center oficiais sustentam narrativa de suporte em portugues, foco local e LGPD | segue mais pronto para Brasil operacional e suporte humano do que o IA-AGGREGATOR |
| **OpenRouter** | paginas oficiais de `pricing` e `enterprise` mostram embalagem comercial madura para gateway/model routing | o IA-AGGREGATOR tem backend amplo, mas ainda nao empacotou governanca/operacao nesse nivel |
| **TypingMind** | pricing oficial + docs oficiais de data ownership reforcam BYOK, propriedade de dados e opcao power-user/pro | IA-AGGREGATOR tem mais ambicao de plataforma, mas menos clareza de ownership e deployment posture |
| **You.com** | pagina oficial de planos sustenta oferta comercial com trilha enterprise dedicada | IA-AGGREGATOR ainda nao possui motion enterprise equivalente nem controles empacotados |
| **Poe** | paginas oficiais de `about` e assinatura sustentam breadth de modelos/bots e distribuicao de produto | IA-AGGREGATOR nao tem ecossistema, distribuicao ou rede de criadores comparavel |

### Conclusao competitiva

- **Contra a Inner AI**, o gap principal e operacional/comercial: localizacao BR, pagamentos, suporte e posicionamento.
- **Contra OpenRouter**, o gap principal e embalagem enterprise de gateway e governanca.
- **Contra TypingMind**, o gap principal e clareza de ownership/deployment e consistencia do produto.
- **Contra You.com**, o gap principal e oferta enterprise de verdade, com politica, seguranca e suporte empacotados.
- **Contra Poe**, o gap principal e escala de produto, ecossistema e distribuicao.

### Fontes oficiais consultadas

- Inner AI:
  - https://blog.innerai.com/seguranca-e-protecao-de-dados-plataforma-de-ia-com-suporte-ao-cliente-em-portugues/
  - https://blog.innerai.com/clique-para-conversar-com-a-ia-mais-emocionante-do-mundo-e-em-portugues/
- OpenRouter:
  - https://openrouter.ai/enterprise
  - https://openrouter.ai/pricing
- TypingMind:
  - https://www.typingmind.com/pricing
  - https://docs.typingmind.com/privacy/data-ownership-and-storage
- You.com:
  - https://you.com/plans
- Poe:
  - https://poe.com/about
  - https://help.poe.com/hc/en-us/articles/19945140063636-What-is-the-Poe-Subscription

## 8. Backlog Priorizado P0-P3

| Prioridade | Achado | Evidencia | Impacto | Esforco | Natureza |
|---|---|---|---|---|---|
| P0 | Verificar assinatura JWT no frontend | `src/server/codex/auth.ts` | account spoofing + bootstrap indevido de workspace | Medio | Seguranca/produto |
| P0 | Remover tokens de `localStorage` e de cookies JS | `src/stores/auth-store.ts`, `src/lib/api.ts` | roubo de sessao via XSS e fluxo de auth inconsistente | Medio | Seguranca |
| P0 | Fechar contrato de logout | frontend chama `/api/v1/auth/logout`; backend nao expoe rota | sessao inconsistente e revogacao incompleta | Baixo | Integracao |
| P0 | Implementar OAuth GitHub real | `src/app/api/oauth/github/callback/route.ts` | PRs e conectores continuam demo-only | Medio | Integracao/negocio |
| P0 | Assinar e validar webhooks Next ou desativar superficie publica | `src/app/api/webhooks/*/route.ts` | task injection e logs fraudulentos | Medio | Seguranca |
| P1 | Escolher fonte de verdade para billing | backend `billing.*` vs frontend `codex` | risco contabil, produto confuso e precificacao opaca | Alto | Monetizacao/operacao |
| P1 | Escolher fonte de verdade para analytics | backend `analytics.*` vs frontend `AnalyticsAggregate` | dashboards divergentes | Medio | Produto/operacao |
| P1 | Introduzir testes de contrato e integracao nas API routes Next | falhas atuais de Jest + ausencia de integration tests | regressao silenciosa | Medio | Qualidade |
| P1 | Productizar PR lifecycle real | `pull-requests/route.ts` gera URL/numero aleatorios | feature vende mais do que entrega | Medio | Integracao/produto |
| P1 | Atualizar docs raiz com estado real | docs 2026-03-08 ja estao stale | decisao executiva baseada em premissas erradas | Baixo | Governanca |
| P2 | Unificar identidade e tenant model entre `auth.*` e `codex.*` | dois sistemas de usuario/org/workspace | drift e integracao cara | Alto | Dados/arquitetura |
| P2 | Transformar compliance export e credit purchase em fluxos reais | rotas criam registros `completed` artificiais | risco legal e comercial | Medio | Operacao/monetizacao |
| P2 | Separar melhor produto "Codex engineering" de "agregador geral" | docs e codigo contam historias diferentes | marketing difuso e roadmap disperso | Medio | Negocio |
| P3 | Consolidar execution/platform/marketplace com um front real ou reduzir escopo | backend amplo, frontend sem aderencia equivalente | sobrecarga de manutencao | Alto | Estrategia |

## 9. Recomendacao Final

### Go / no-go

- **No-go** para lancamento publico pago.
- **No-go** para venda enterprise B2B.
- **Go** para alpha tecnico fechado, demo de engenharia assistida e validacao controlada do runtime Codex.

### Recomendacao estrategica

A solucao precisa escolher uma aposta principal:

1. **Virar produto de engenharia assistida (Lume Codex Cloud)**.
   - Este e o caminho mais crivel no curto prazo.
   - O ativo diferencial atual e o runtime de tasks com SSE, logs, diff e evidencias.

2. **Virar agregador brasileiro de IA horizontal**.
   - Exige bem mais do que o codigo atual entrega.
   - Precisa de operacao comercial local, pagamentos BR, compliance LGPD operacional, suporte humano, conectores reais e governanca enterprise.

**Nao recomendo tentar vender os dois ao mesmo tempo agora.**

## 10. Apendice A - Inventario arquivo a arquivo das superficies publicas do backend

### Controllers backend auditados

- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/Ai3DGenerationController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/AiAvatarVideoController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/AiChatController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/AiDocumentParsingController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/AiEmbeddingController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/AiGroundedChatController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/AiImageController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/AiOcrController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/AiProviderCatalogController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/AiRerankController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/AiResponsesController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/AiSearchController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/AiSpeechController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/AiStreamingChatController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/AiThreatIntelController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/AiTranslationController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/AiVideoController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/ModelCatalogController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/analytics/AnalyticsController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/artifact/ArtifactController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/asset/AssetLibraryController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/auth/ApiKeyController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/auth/AuditController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/auth/AuthController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/auth/SsoController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/billing/BillingController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/chat/CompareModeController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/chat/ConversationController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/compliance/ComplianceController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/execution/AgentController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/execution/ToolController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/execution/WorkflowController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/gateway/GatewayRoutingController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/knowledge/KnowledgeBaseController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/marketplace/MarketplaceController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/platform/ApiMetaController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/platform/BatchApiController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/platform/OpenAiCompatibleController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/platform/RequestTracingController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/platform/VirtualKeyController.java`
- `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/webhook/WebhookController.java`

### Arquivos backend mais criticos

| Arquivo | Responsabilidade | Status real | Dependencias-chave | Cobertura / risco | Observacao de negocio |
|---|---|---|---|---|---|
| `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/auth/AuthController.java` | login/register/refresh/me | Real, sem logout | use cases auth | alto risco contratual | auth backend mais maduro que frontend |
| `backend/ia-aggregator-infrastructure/src/main/java/com/ia/aggregator/infrastructure/config/SecurityConfig.java` | matriz de exposicao e protecao | Real | Spring Security, JWT filter, rate limit filter | alto impacto | define o que pode ser publico |
| `backend/ia-aggregator-infrastructure/src/main/java/com/ia/aggregator/infrastructure/auth/security/JwtTokenProvider.java` | assinatura e validacao JWT | Real | JJWT, Redis | alto impacto | base confiavel para sessao backend |
| `backend/ia-aggregator-infrastructure/src/main/java/com/ia/aggregator/infrastructure/security/RateLimitFilter.java` | rate limiting Redis | Real | Redis, Lua | medio risco fail-open | docs ainda dizem que nao existe |
| `backend/ia-aggregator-infrastructure/src/main/java/com/ia/aggregator/infrastructure/security/WebhookVerifier.java` | HMAC GitHub/Slack/Linear | Real | HMAC SHA-256 | alto impacto | backend esta mais pronto que frontend |
| `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/webhook/WebhookController.java` | ingestao de webhooks | Real | verifier | alto impacto | assinatura invalida retorna 401 |
| `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/analytics/AnalyticsController.java` | ingestao e reports | Real | service analytics | medio risco | `events` publico aumenta superficie |
| `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/billing/BillingController.java` | planos, uso, budgets, assinatura | Real | billing use cases | alto impacto | mais avancado que a narrativa do produto |
| `backend/ia-aggregator-infrastructure/src/main/java/com/ia/aggregator/infrastructure/billing/StripePaymentGatewayAdapter.java` | integracao Stripe | Real | HTTP client, Stripe API | medio risco | monetizacao backend existe, frontend nao acompanha |
| `backend/ia-aggregator-infrastructure/src/main/java/com/ia/aggregator/infrastructure/ai/persistence/ModelCatalogRepositoryImpl.java` | catalogo de modelos ativo/default | Real | JPA providers/models | medio risco | arquivo central do catalogo de IA |
| `backend/ia-aggregator-application/src/main/java/com/ia/aggregator/application/ai/usecase/ModelCatalogUseCaseImpl.java` | orquestra catalogo | Real | repository port | baixo risco | simples e coerente |
| `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/ModelCatalogController.java` | expose `/api/v1/ai/models` | Real | use case | medio risco | frontend depende desse endpoint |
| `backend/ia-aggregator-presentation/src/main/java/com/ia/aggregator/presentation/ai/AiStreamingChatController.java` | chat streaming SSE | Real | `StreamingChatUseCase` | medio risco | prova de que streaming deixou de ser so promessa |
| `backend/ia-aggregator-presentation/src/main/resources/db/migration/V5__billing_module.sql` | billing schema | Real | PostgreSQL/Flyway | alto risco estrutural | confirma billing de producao no backend |
| `backend/ia-aggregator-presentation/src/main/resources/db/migration/V7__ai_gateway_module.sql` | AI gateway schema | Real | PostgreSQL/Flyway | alto impacto | catalogo seedado e persistido |
| `backend/ia-aggregator-presentation/src/main/resources/db/migration/V11__execution_platform_marketplace_tables.sql` | execution/platform/marketplace | Parcial | PostgreSQL/Flyway | medio/alto | amplitude grande, conversao em produto ainda baixa |

## 11. Apendice B - Inventario arquivo a arquivo das API routes do frontend

### API routes frontend auditadas

- `frontend/src/app/api/analytics/route.ts`
- `frontend/src/app/api/auth/login/route.ts`
- `frontend/src/app/api/auth/logout/route.ts`
- `frontend/src/app/api/auth/session/route.ts`
- `frontend/src/app/api/billing/plans/route.ts`
- `frontend/src/app/api/billing/usage/route.ts`
- `frontend/src/app/api/billing/usage/current/route.ts`
- `frontend/src/app/api/billing/usage/weekly/route.ts`
- `frontend/src/app/api/code-review/policies/route.ts`
- `frontend/src/app/api/code-review/policies/[id]/route.ts`
- `frontend/src/app/api/compliance/exports/route.ts`
- `frontend/src/app/api/credits/route.ts`
- `frontend/src/app/api/credits/purchase/route.ts`
- `frontend/src/app/api/environments/route.ts`
- `frontend/src/app/api/environments/[id]/route.ts`
- `frontend/src/app/api/environments/[id]/reset-cache/route.ts`
- `frontend/src/app/api/environments/[id]/validate/route.ts`
- `frontend/src/app/api/health/status/route.ts`
- `frontend/src/app/api/integrations/github/install/route.ts`
- `frontend/src/app/api/integrations/linear/install/route.ts`
- `frontend/src/app/api/integrations/slack/install/route.ts`
- `frontend/src/app/api/integrations/status/route.ts`
- `frontend/src/app/api/managed-configs/route.ts`
- `frontend/src/app/api/managed-configs/[id]/route.ts`
- `frontend/src/app/api/models/capabilities/route.ts`
- `frontend/src/app/api/oauth/github/callback/route.ts`
- `frontend/src/app/api/oauth/github/connect/route.ts`
- `frontend/src/app/api/repositories/route.ts`
- `frontend/src/app/api/repositories/[repoId]/branches/route.ts`
- `frontend/src/app/api/tasks/route.ts`
- `frontend/src/app/api/tasks/[id]/route.ts`
- `frontend/src/app/api/tasks/[id]/archive/route.ts`
- `frontend/src/app/api/tasks/[id]/artifacts/route.ts`
- `frontend/src/app/api/tasks/[id]/cancel/route.ts`
- `frontend/src/app/api/tasks/[id]/diff/route.ts`
- `frontend/src/app/api/tasks/[id]/events/route.ts`
- `frontend/src/app/api/tasks/[id]/followups/route.ts`
- `frontend/src/app/api/tasks/[id]/logs/route.ts`
- `frontend/src/app/api/tasks/[id]/pull-requests/route.ts`
- `frontend/src/app/api/tasks/[id]/pull-requests/[prId]/route.ts`
- `frontend/src/app/api/tasks/[id]/retry/route.ts`
- `frontend/src/app/api/tasks/[id]/tests/route.ts`
- `frontend/src/app/api/tasks/[id]/unarchive/route.ts`
- `frontend/src/app/api/usage/route.ts`
- `frontend/src/app/api/webhooks/github/route.ts`
- `frontend/src/app/api/webhooks/linear/route.ts`
- `frontend/src/app/api/webhooks/slack/route.ts`

### Arquivos frontend mais criticos

| Arquivo | Responsabilidade | Status real | Dependencias-chave | Cobertura / risco | Observacao de negocio |
|---|---|---|---|---|---|
| `frontend/src/server/codex/auth.ts` | sessao server-side | Parcial com falha critica | cookies, decode JWT | risco maximo | aceita identidade sem verificacao criptografica |
| `frontend/src/server/codex/http.ts` | `requireCodexContext` | Parcial | sessao + `ensureWorkspaceForUser` | alto risco | toda rota Codex herda esta confianca |
| `frontend/src/server/codex/seed.ts` | bootstrap automatico de tenant | Real como seed/demo | Prisma | alto risco funcional | forged session pode materializar workspace default |
| `frontend/src/app/api/auth/login/route.ts` | login via cookie httpOnly | Implementado, pouco usado | fetch backend | medio | caminho bom, mas contornado pelo fluxo principal |
| `frontend/src/stores/auth-store.ts` | fluxo principal de auth da UI | Hibrido e inseguro | axios, localStorage | alto risco | bypassa a rota segura de login |
| `frontend/src/lib/api.ts` | cliente HTTP principal | Hibrido e inseguro | axios, localStorage, `document.cookie` | alto risco | concentra o problema de sessao |
| `frontend/src/middleware.ts` | gate de paginas | Parcial | cookie presence | alto risco | nao valida autenticidade da sessao |
| `frontend/src/app/api/auth/session/route.ts` | sessao no App Router | Parcial | `getServerSession` | alto risco | reflete JWT nao validado |
| `frontend/src/app/api/auth/logout/route.ts` | limpeza de cookies | Parcial | NextResponse | medio | nao revoga refresh no backend |
| `frontend/src/app/api/oauth/github/connect/route.ts` | inicia OAuth | Parcial | env vars GitHub | medio | gera URL real, mas fluxo final continua mockado |
| `frontend/src/app/api/oauth/github/callback/route.ts` | callback OAuth | Mock/prototipo | Prisma | alto risco | grava token sintetico |
| `frontend/src/app/api/integrations/github/install/route.ts` | install GitHub | Sintetico | Prisma | medio | marca `CONNECTED` sem install real |
| `frontend/src/app/api/integrations/slack/install/route.ts` | install Slack | Sintetico | Prisma | medio | upsert direto no banco |
| `frontend/src/app/api/integrations/linear/install/route.ts` | install Linear | Sintetico | Prisma | medio | upsert direto no banco |
| `frontend/src/app/api/webhooks/github/route.ts` | webhook GitHub | Parcial/inseguro | Prisma | alto risco | sem HMAC |
| `frontend/src/app/api/webhooks/slack/route.ts` | webhook Slack | Parcial/inseguro | Prisma, enqueueTask | alto risco | sem HMAC |
| `frontend/src/app/api/webhooks/linear/route.ts` | webhook Linear | Parcial/inseguro | Prisma, enqueueTask | alto risco | sem HMAC |
| `frontend/src/app/api/tasks/route.ts` | cria/lista tasks | Real | Prisma, queue, auth context | medio | nucleo mais forte do produto atual |
| `frontend/src/app/api/tasks/[id]/events/route.ts` | SSE task events | Real | Prisma, `ReadableStream` | medio | polling no banco, nao push nativo |
| `frontend/src/server/codex/events.ts` | persistencia de eventos e logs | Real | Prisma | baixo | base do evidence-first workflow |
| `frontend/src/server/codex/task-runner.ts` | worker da task | Real como scaffold | fs, git, Prisma, spawn | alto | ativo mais promissor da solucao |
| `frontend/src/server/codex/billing.ts` | compose billing UI | Hibrido | backend proxy, Prisma, heuristicas | medio | mistura fato e estimativa |
| `frontend/src/app/api/credits/route.ts` | saldo e compra manual | Mock persistido | Prisma | medio | dinheiro sem gateway real |
| `frontend/src/app/api/compliance/exports/route.ts` | export job | Mock persistido | Prisma | medio | compliance ainda teatralizado |
| `frontend/src/lib/model-catalog.ts` | catalogo de modelos no client | Hibrido | fetch backend + fallback | baixo/medio | resiliente, mas mascara indisponibilidade |
| `frontend/src/lib/streaming.ts` | streaming chat | Real com fallback | fetch, SSE | medio | mostra evolucao positiva do chat |
| `frontend/src/stores/chat-store.ts` | estado de chat | Parcial | Zustand, streaming | medio | store evoluiu mais rapido que os testes |
| `frontend/src/app/prompts/page.tsx` | biblioteca de templates | Parcial | fetch backend + fallback | baixo | boa UX, baixa cobertura de testes |
| `frontend/src/stores/chat-store.test.ts` | teste de store | Obsoleto | Jest | risco de regressao | denuncia refactor sem manutencao do teste |
| `frontend/src/app/prompts/page.test.tsx` | teste de pagina | Quebrado | Jest | risco de regressao | ambiente de teste nao reflete o runtime |

## 12. Apendice C - Sintese final por camada

| Camada | Diagnostico curto |
|---|---|
| Backend | mais maduro, mais seguro e mais amplo do que a documentacao reconhece |
| Frontend | mais polido visualmente do que seguro/consistente operacionalmente |
| Dados | duas plataformas modeladas no mesmo banco |
| Contratos | divergem do runtime em auth, billing, webhooks e testes |
| Negocio | tese forte, produto ainda difuso |
| Melhor ativo hoje | runtime Codex de tasks com evidencias |
| Maior risco hoje | sessao/frontend e narrativas de produto conflitantes |

## 13. Apendice D - Mapa de volume por package no backend

### 13.1 Distribuicao por package raiz

| Modulo | Package raiz | Arquivos | Leitura |
|---|---|---:|---|
| application | `application.ai` | 118 | maior concentracao de casos de uso, DTOs e ports |
| application | `application.auth` | 24 | auth, SSO, API keys e permissoes |
| application | `application.billing` | 13 | assinatura, uso, budgets e planos |
| application | `application.execution` | 10 | workflows, tools e agent execution |
| application | `application.platform` | 9 | batch, traces e virtual keys |
| application | `application.knowledge` | 8 | documentos e retrieval |
| domain | `domain.auth` | 9 | user, roles, permissions e eventos |
| domain | `domain.execution` | 6 | workflow, run, agent execution, tool definition |
| domain | `domain.ai` | 6 | tipos/capabilities/strategy/status |
| domain | `domain.knowledge` | 5 | documents, chunks e retrieval |
| infrastructure | `infrastructure.ai` | 146 | providers, adapters, persistence e routing |
| infrastructure | `infrastructure.auth` | 15 | security, converters, persistence e SSO |
| infrastructure | `infrastructure.billing` | 15 | repositorios, persistence e Stripe |
| infrastructure | `infrastructure.execution` | 12 | persistence e orquestracao execution |
| infrastructure | `infrastructure.platform` | 9 | traces, keys e batch persistence |
| presentation | `presentation.ai` | 18 | maior superficie HTTP do backend |
| presentation | `presentation.analytics` | 6 | ingestao e reports |
| presentation | `presentation.platform` | 5 | API meta, batch, traces, virtual keys, OpenAI-compatible |
| presentation | `presentation.auth` | 4 | auth, SSO, audit, api keys |

### 13.2 Hotspot do pacote de IA

| Subpackage | Arquivos | Leitura |
|---|---:|---|
| `application.ai.dto` | 46 | payloads e respostas das capacidades de IA |
| `application.ai.port` | 53 | numero alto de interfaces e ports de integracao |
| `application.ai.usecase` | 19 | orquestracao central |
| `infrastructure.ai.provider` | 114 | 17 providers + classes auxiliares |
| `infrastructure.ai.auth` | 11 | auth por provider |
| `infrastructure.ai.persistence` | 7 | catalogo e entidades JPA |
| `infrastructure.ai.routing` | 5 | regras e decisao de roteamento |
| `presentation.ai` | 18 | breadth grande de superficie HTTP |

**Leitura**: o backend e claramente um produto "AI platform first"; o frontend ainda nao expande toda essa largura funcional.

### 13.3 Inventario do dominio backend

- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/chat/ConversationFork.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/chat/ChatMode.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/gateway/RoutingRule.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/gateway/RoutingDecision.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/gateway/RoutingContext.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/billing/UsageRecord.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/billing/SubscriptionStatus.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/billing/PlanTier.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/billing/BudgetAlert.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/asset/Asset.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/asset/AgentDefinition.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/execution/ToolDefinition.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/execution/AutonomyLevel.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/execution/AgentExecution.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/execution/WorkflowStep.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/execution/WorkflowRun.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/execution/Workflow.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/artifact/ArtifactVersion.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/artifact/Artifact.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/ai/ProviderStatus.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/ai/Capability.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/ai/AuthType.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/ai/RoutingStrategy.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/ai/ProviderType.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/ai/SyncMode.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/audit/AuditEvent.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/compliance/DataErasureRequest.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/compliance/ConsentRecord.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/marketplace/CatalogReview.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/marketplace/CatalogEntry.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/knowledge/DocumentStatus.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/knowledge/DocumentChunk.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/knowledge/ChunkingStrategy.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/knowledge/KnowledgeDocument.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/platform/RequestTrace.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/platform/BatchJob.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/platform/VirtualKey.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/knowledge/RetrievalResult.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/shared/entity/BaseEntity.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/auth/event/UserRegisteredEvent.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/auth/vo/RolePermissions.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/auth/vo/Permission.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/auth/vo/AuthProvider.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/auth/vo/UserRole.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/auth/vo/UserStatus.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/auth/service/PasswordEncoder.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/auth/entity/User.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/auth/repository/UserRepository.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/shared/event/BaseDomainEvent.java`
- `backend/ia-aggregator-domain/src/main/java/com/ia/aggregator/domain/shared/event/DomainEvent.java`

## 14. Apendice E - Inventario de paginas e runtime do frontend

### 14.1 Paginas e layouts auditados

- `frontend/src/app/layout.tsx`
- `frontend/src/app/page.tsx`
- `frontend/src/app/home/page.tsx`
- `frontend/src/app/welcome/page.tsx`
- `frontend/src/app/error/page.tsx`
- `frontend/src/app/login/page.tsx`
- `frontend/src/app/logout/page.tsx`
- `frontend/src/app/register/page.tsx`
- `frontend/src/app/chat/page.tsx`
- `frontend/src/app/library/page.tsx`
- `frontend/src/app/prompts/page.tsx`
- `frontend/src/app/billing/page.tsx`
- `frontend/src/app/oauth/github/callback/page.tsx`
- `frontend/src/app/admin/settings/page.tsx`
- `frontend/src/app/settings/page.tsx`
- `frontend/src/app/settings/analytics/page.tsx`
- `frontend/src/app/settings/analytics/debug/page.tsx`
- `frontend/src/app/codex/page.tsx`
- `frontend/src/app/codex/get-started/page.tsx`
- `frontend/src/app/codex/onboarding/page.tsx`
- `frontend/src/app/codex/shortcuts/page.tsx`
- `frontend/src/app/codex/tasks/page.tsx`
- `frontend/src/app/codex/tasks/[taskId]/page.tsx`
- `frontend/src/app/codex/tasks/[taskId]/artifacts/page.tsx`
- `frontend/src/app/codex/tasks/[taskId]/diff/page.tsx`
- `frontend/src/app/codex/tasks/[taskId]/logs/page.tsx`
- `frontend/src/app/codex/tasks/[taskId]/pull-request/page.tsx`
- `frontend/src/app/codex/tasks/[taskId]/tests/page.tsx`
- `frontend/src/app/codex/settings/page.tsx`
- `frontend/src/app/codex/settings/analytics/page.tsx`
- `frontend/src/app/codex/settings/apireference/page.tsx`
- `frontend/src/app/codex/settings/code-review/page.tsx`
- `frontend/src/app/codex/settings/connectors/page.tsx`
- `frontend/src/app/codex/settings/environments/page.tsx`
- `frontend/src/app/codex/settings/environments/new/page.tsx`
- `frontend/src/app/codex/settings/environments/[environmentId]/page.tsx`
- `frontend/src/app/codex/settings/managed-configs/page.tsx`
- `frontend/src/app/codex/settings/usage/page.tsx`
- `frontend/src/app/codex/settings/usage/credits/page.tsx`

### 14.2 Runtime server-side, stores e libs

**Server `src/server/codex`**

- `frontend/src/server/codex/auth.ts`
- `frontend/src/server/codex/billing.ts`
- `frontend/src/server/codex/db.ts`
- `frontend/src/server/codex/events.ts`
- `frontend/src/server/codex/http.ts`
- `frontend/src/server/codex/queue.ts`
- `frontend/src/server/codex/redis.ts`
- `frontend/src/server/codex/seed.ts`
- `frontend/src/server/codex/task-runner.ts`
- `frontend/src/server/codex/types.ts`

**Stores**

- `frontend/src/stores/auth-store.ts`
- `frontend/src/stores/chat-store.ts`
- `frontend/src/stores/chat-store.test.ts`
- `frontend/src/stores/theme-store.ts`
- `frontend/src/stores/toast-store.ts`
- `frontend/src/stores/toast-store.test.ts`

**Libs**

- `frontend/src/lib/analytics.ts`
- `frontend/src/lib/analytics.test.ts`
- `frontend/src/lib/api.ts`
- `frontend/src/lib/api.test.ts`
- `frontend/src/lib/cn.ts`
- `frontend/src/lib/codex-api.ts`
- `frontend/src/lib/model-catalog.ts`
- `frontend/src/lib/model-utils.ts`
- `frontend/src/lib/streaming.ts`
- `frontend/src/lib/services/analytics-service.ts`

### 14.3 Leitura arquitetural do frontend apos inventario

| Eixo | Leitura |
|---|---|
| `src/app/api` | produto server-side importante, mas com varios endpoints sinteticos |
| `src/server/codex` | verdadeiro coracao da oferta atual |
| `src/app/codex` | experiencia mais proxima de produto coerente |
| `src/app/chat` + `src/lib/streaming` | ponte real com o backend agregador |
| `src/app/settings` e `src/app/billing` | UX boa, mas parte relevante ainda opera com heuristicas, mocks ou dupla fonte de verdade |
