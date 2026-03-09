# ROADMAP DE IMPLEMENTACAO — IA AGGREGATOR / AI Operating Layer

**Data:** 2026-03-08
**Versao:** 1.0
**Base:** Analise cruzada de 4 especificacoes (tecnica, funcional, negocio, completa) vs. estado atual do codebase

---

## RESUMO EXECUTIVO

O codebase atual possui uma arquitetura hexagonal solida (Java 21 + Spring Boot 3.4) com 105 provedores de IA implementados cobrindo 17 capabilities, alem de um frontend Next.js 15 com rotas e estado basicos. Porem, existem **lacunas criticas** entre o que esta implementado e o que as especificacoes exigem para uma plataforma comercializavel.

Este roadmap extrai **somente o que agrega valor** — imediato ou futuro — e organiza em 7 fases priorizadas por impacto de negocio e dependencias tecnicas.

---

## INVENTARIO: O QUE JA EXISTE

| Area | Status | Cobertura |
|------|--------|-----------|
| Arquitetura Hexagonal (DDD/Ports&Adapters) | Solido | Backend 5 modulos Maven |
| Provedores de IA | Excelente | 105 providers, 17 capabilities |
| Controllers REST | Parcial | 16 AI controllers + Auth + Analytics |
| Frontend (Next.js 15 + React 19) | Parcial | Rotas existem, funcionalidade basica |
| Autenticacao JWT | Vulneravel | JWT sem verificacao no frontend |
| Banco de Dados | Parcial | PostgreSQL 16 + pgvector + Flyway (9 schemas) |
| Circuit Breaker | Implementado | Resilience4j por provider |
| SSE/Streaming | Parcial | Tasks sim, Chat nao (simulado) |
| RBAC | Incompleto | 5 roles no enum, enforcement ausente |
| Billing | Nao iniciado | Modelos Prisma existem, Stripe nao integrado |
| RAG/Knowledge Base | Nao iniciado | pgvector habilitado mas nao usado |
| Workflow/Orchestration | Ausente | Sem Temporal, sem runtime de agentes |
| Rate Limiting | Ausente | Zero throttling server-side |
| Testes de Integracao | Ausente | 0 testes no backend |

---

## FASE 0 — SEGURANCA E ESTABILIDADE CRITICA

**Prioridade:** BLOQUEANTE — Nada mais deve ir para producao sem isto
**Duracao estimada:** 2-3 semanas
**Justificativa:** 4 vulnerabilidades criticas identificadas (GAP-001 a GAP-004) + zero testes de integracao

### 0.1 Correcoes de Seguranca (GAP-001 a GAP-004)

| # | Item | Spec Ref | Descricao |
|---|------|----------|-----------|
| 0.1.1 | JWT Verification no Frontend | Sec.13, Manus 6.4 | Verificar assinatura JWT no frontend (nao apenas base64-decode). Usar `jose` ou `jsonwebtoken` com chave publica |
| 0.1.2 | Cookies httpOnly + Secure + SameSite | Sec.13 | Configurar cookies com flags `httpOnly`, `Secure`, `SameSite=Lax` |
| 0.1.3 | Webhook HMAC Verification | Sec.13.4 | Implementar `crypto.timingSafeEqual()` para webhooks GitHub, Slack, Linear com secrets dedicados |
| 0.1.4 | OAuth Real Implementation | Sec.12.2, Mod.H | Substituir token mock por troca real de codigo OAuth com GitHub (client_credentials flow) |

### 0.2 Rate Limiting (GAP-008)

| # | Item | Spec Ref | Descricao |
|---|------|----------|-----------|
| 0.2.1 | Token Bucket no Gateway | Tec.8.6, Manus 6.2 | Implementar rate limiting via Redis (Token Bucket) com limites por usuario, workspace e IP |
| 0.2.2 | HTTP 429 com Retry-After | Tec.8.6 | Retornar HTTP 429 com header `Retry-After` quando limite atingido |
| 0.2.3 | Rate Limit Configuravel | Mod.I-G | Limites por minuto/hora/dia configuraveis no admin |

### 0.3 Testes de Integracao (GAP-006)

| # | Item | Spec Ref | Descricao |
|---|------|----------|-----------|
| 0.3.1 | TestContainers Setup | Tec.18 | PostgreSQL, Redis, MockWebServer com TestContainers |
| 0.3.2 | Provider Integration Tests | Tec.18 | 1 teste por provider (request → response → error mapping) |
| 0.3.3 | Auth Flow Tests | Sec.13 | Login → JWT → Refresh → Expire → Lock |
| 0.3.4 | Routing Tests | Tec.8 | Capability routing + fallback + circuit breaker |

### 0.4 Streaming Real para Chat (GAP-007)

| # | Item | Spec Ref | Descricao |
|---|------|----------|-----------|
| 0.4.1 | SSE Real Chat Streaming | Tec.9.7, Manus 6.2-7 | Substituir simulacao (4 chars/12ms) por SSE real do provider, relay token-by-token |
| 0.4.2 | Token Counting em Tempo Real | Manus 6.2-7 | Contar tokens durante stream (tiktoken para OpenAI, estimativa char-based para outros) |

**Arquivos afetados:**
- Frontend: auth middleware, cookie config, webhook handlers, OAuth flow, chat streaming
- Backend: `SecurityConfig`, rate limit filter, test infrastructure
- Novos: `RateLimitFilter.java`, `WebhookVerifier.java`, `*IntegrationTest.java` (~60 testes)

---

## FASE 1 — GATEWAY INTELIGENTE E ROUTING POR REGRAS

**Prioridade:** ALTA — Core differentiator da plataforma
**Duracao estimada:** 3-4 semanas
**Dependencia:** Fase 0 (rate limiting)
**Spec Refs:** Tec.Sec.8 (Routing Engine), Manus 6.2-6.3 (YAML Rules), Func.Mod.B (Hub de Interacao)

### 1.1 Motor de Regras YAML

| # | Item | Descricao |
|---|------|-----------|
| 1.1.1 | `routing_rules.yml` Schema | Definir schema YAML com: `global_settings`, `providers`, `models`, `routing_rules`, `fallback_chains`, `circuit_breaker`, `load_balancing` |
| 1.1.2 | Rule Engine | Avaliar regras sequencialmente: regex no prompt, tamanho do prompt (tokens), metadata em headers (`X-Route-Hint`), horario do dia. Primeira regra match vence |
| 1.1.3 | Hot-Reload | Recarregar regras YAML a cada 30s sem restart |
| 1.1.4 | 5 Estrategias de Routing | `cost_optimized`, `latency_optimized`, `quality_optimized`, `policy_strict`, `balanced_default` |
| 1.1.5 | Fallback Chains YAML | Definir cadeias de fallback por modelo (ex: GPT-5 → Claude → DeepSeek). Trigger em 429, 500, 502, 503, 504, timeout |
| 1.1.6 | Load Balancing Multi-Key | Round-robin entre multiplas API keys do mesmo provider |

### 1.2 Semantic Cache

| # | Item | Descricao |
|---|------|-----------|
| 1.2.1 | Cache Layer (Redis) | Hash SHA-256 do prompt. Exact match retorna response cacheada. TTL configuravel por modelo (default 24h) |
| 1.2.2 | Cache Hit Metrics | Registrar taxa de cache hit no ClickHouse/Micrometer |
| 1.2.3 | Cache Toggle por Workspace | Admin pode habilitar/desabilitar cache por workspace |

### 1.3 Policy Service

| # | Item | Descricao |
|---|------|-----------|
| 1.3.1 | Policy Entity e Repository | Entidades: AccessPolicy, CostPolicy, RetentionPolicy, ToolPolicy, RoutingPolicy |
| 1.3.2 | Policy Evaluation Engine | Avaliar politicas em cadeia: tenant → workspace → project. Override controlado |
| 1.3.3 | Model Allowlist/Blocklist | Admin restringe modelos por workspace/team/role. Modelos bloqueados nao aparecem no dropdown |
| 1.3.4 | Budget Soft/Hard Limits | Limites de gasto por usuario/team/workspace. Soft → alerta. Hard → bloqueio |

### 1.4 Canary e Traffic Splitting

| # | Item | Descricao |
|---|------|-----------|
| 1.4.1 | Weight-Based Routing | Direcionar X% do trafego para um novo modelo via campo `weight` na config YAML |
| 1.4.2 | Shadow Traffic | Enviar copia da request para modelo alternativo sem afetar resposta do usuario |

**Arquivos novos:**
- `infrastructure/ai/routing/YamlRuleEngine.java`
- `infrastructure/ai/routing/RoutingRule.java`
- `infrastructure/ai/routing/FallbackChain.java`
- `infrastructure/ai/routing/SemanticCache.java`
- `infrastructure/ai/routing/LoadBalancer.java`
- `domain/policy/Policy.java`, `AccessPolicy.java`, `CostPolicy.java`, `RoutingPolicy.java`
- `application/policy/PolicyEvaluationService.java`
- `routing_rules.yml` (config)

**Arquivos modificados:**
- `CapabilityRouter.java` — integrar com YamlRuleEngine
- `ProviderRegistry.java` — suportar multi-key load balancing
- `application.yml` — config de cache, policies

---

## FASE 2 — RBAC COMPLETO, SSO E GOVERNANCA

**Prioridade:** ALTA — Requisito para qualquer venda B2B
**Duracao estimada:** 3-4 semanas
**Dependencia:** Fase 0 (auth fixes)
**Spec Refs:** Func.Mod.H (Console Admin), Manus 2.1 (RBAC), Tec.Sec.5.1 (Identity)

### 2.1 RBAC Granular

| # | Item | Descricao |
|---|------|-----------|
| 2.1.1 | 4 Roles Enforced | Owner, Admin, Manager, User — com permissoes granulares enforced em cada endpoint |
| 2.1.2 | Permission Matrix | Matriz role × recurso × acao (CRUD + special) |
| 2.1.3 | Resource-Level Permissions | Permissoes por projeto, workspace, knowledge base, agent |
| 2.1.4 | API Key Scoping | Virtual keys com scope por capability, modelo, budget |
| 2.1.5 | `@PreAuthorize` Guards | Anotacoes Spring Security em todos os controllers |

### 2.2 SSO e Provisioning

| # | Item | Descricao |
|---|------|-----------|
| 2.2.1 | SAML 2.0 | Integracao com Okta, Azure AD, Google Workspace |
| 2.2.2 | OpenID Connect (OIDC) | Login federado via OIDC |
| 2.2.3 | SCIM 2.0 | Provisioning/deprovisioning automatico de usuarios |

### 2.3 Audit Trail

| # | Item | Descricao |
|---|------|-----------|
| 2.3.1 | Audit Event Entity | Login, config change, model usage, tool call, browser action, approval, cost event |
| 2.3.2 | Immutable Audit Log | Append-only com retencao minima de 12 meses |
| 2.3.3 | SIEM Export | JSON exportavel para Splunk, Datadog, Elastic |
| 2.3.4 | Audit Dashboard | Visualizacao de eventos no admin console |

### 2.4 Guardrails (PII + Content)

| # | Item | Descricao |
|---|------|-----------|
| 2.4.1 | PII Detection (Input) | Regex pipeline para CPF, cartao de credito, email, telefone. Mascara para `[REDACTED]` |
| 2.4.2 | Custom Regex Patterns | Admin adiciona patterns personalizados |
| 2.4.3 | Content Filter (Output) | Filtro de respostas por keywords/regex (concorrentes, info confidencial, conteudo toxico) |
| 2.4.4 | Zero Data Retention | Modo ZDR por workspace — prompts/respostas nao sao persistidos. Apenas metadata |

**Arquivos novos:**
- `domain/auth/Permission.java`, `PermissionMatrix.java`
- `infrastructure/auth/SamlAuthProvider.java`, `OidcAuthProvider.java`, `ScimProvider.java`
- `infrastructure/security/PiiDetectionFilter.java`, `ContentOutputFilter.java`
- `infrastructure/audit/AuditEventRepository.java`, `AuditEventService.java`
- `presentation/admin/AuditController.java`, `PolicyController.java`, `GuardrailController.java`

---

## FASE 3 — CONTEXT, KNOWLEDGE BASE E RAG

**Prioridade:** ALTA — Diferencial competitivo central
**Duracao estimada:** 4-5 semanas
**Dependencia:** Fase 0 (streaming real)
**Spec Refs:** Tec.Sec.11 (Knowledge Plane), Func.Mod.C (Contexto/Memoria/Conhecimento), Manus 1.3 (RAG)

### 3.1 Knowledge Service

| # | Item | Descricao |
|---|------|-----------|
| 3.1.1 | Document Ingestion Pipeline | Upload → Parsing (PDF/DOCX/CSV/Excel/TXT) → Normalization → Chunking → Metadata Extraction → Embeddings → Indexing → ACL |
| 3.1.2 | Chunking Strategies | Fixed-size, semantic, recursive character splitting. Configuravel por knowledge base |
| 3.1.3 | Embedding Storage (Qdrant) | Vetores armazenados no Qdrant com metadata (source, page, chunk_id) |
| 3.1.4 | pgvector Fallback | Usar pgvector como fallback quando Qdrant indisponivel |
| 3.1.5 | Retrieval Pipeline | Query → Embedding → Vector Search → Reranking → Context Injection no prompt |
| 3.1.6 | Citations e Provenance | Cada trecho recuperado inclui source, page, confidence score |
| 3.1.7 | Semantic + Lexical Search | Busca hibrida: vector similarity + BM25 full-text |
| 3.1.8 | Document ACL | Permissoes herdadas de workspace + source. Override por documento |
| 3.1.9 | Incremental Reindexing | Reindexar apenas documentos alterados desde o ultimo sync |

### 3.2 Memory System

| # | Item | Descricao |
|---|------|-----------|
| 3.2.1 | User Memory | Preferencias persistentes do usuario (idioma, tom, formato) |
| 3.2.2 | Project Memory | Contexto acumulado por projeto (fatos, decisoes, instrucoes) |
| 3.2.3 | Team Memory | Conhecimento compartilhado da equipe |
| 3.2.4 | Agent Operational Memory | Estado acumulado de execucoes anteriores do agente |
| 3.2.5 | Memory Expiration e Review | TTL configuravel, interface de revisao/delecao |

### 3.3 Context Service

| # | Item | Descricao |
|---|------|-----------|
| 3.3.1 | Context Resolution | Resolver memoria + documentos + instrucoes de projeto antes de cada request |
| 3.3.2 | Context Injection Modes | Manual, automatico, hibrido. Configuravel por projeto |
| 3.3.3 | Context Window Management | Priorizar contexto por relevancia quando excede window do modelo |
| 3.3.4 | Web Grounding Integration | Toggle para busca web em tempo real antes de responder |

**Arquivos novos:**
- `domain/knowledge/` — KnowledgeSource, Document, Chunk, MemoryItem, Citation
- `application/knowledge/` — KnowledgeUseCase, IngestionUseCase, RetrievalUseCase, MemoryUseCase
- `infrastructure/knowledge/` — QdrantVectorStore, PgVectorStore, ChunkingService, EmbeddingPipeline
- `infrastructure/knowledge/ingestion/` — PdfParser, DocxParser, CsvParser, MarkdownParser
- `presentation/knowledge/` — KnowledgeBaseController, DocumentController, MemoryController

---

## FASE 4 — BILLING, FINOPS E METERING

**Prioridade:** ALTA — Sem billing, sem receita
**Duracao estimada:** 3-4 semanas
**Dependencia:** Fase 1 (routing com custo), Fase 2 (RBAC)
**Spec Refs:** Tec.Sec.5.9 (Billing & FinOps), Func.Mod.H (Budget/FinOps), Manus 2.2 (Budget), Neg.Sec.6-8

### 4.1 Metering e Cost Tracking

| # | Item | Descricao |
|---|------|-----------|
| 4.1.1 | Cost Event per Request | Registrar: user_id, workspace_id, model_requested, model_used, tokens_in, tokens_out, cost_usd, latency_ms, cache_hit |
| 4.1.2 | Async Cost Pipeline | Publicar CostEvent no Kafka → Worker debita balance no PostgreSQL → Log no ClickHouse |
| 4.1.3 | Cost per Model Catalog | Custo por 1M tokens (input/output) para cada modelo. Atualizado via catalog |
| 4.1.4 | Token Arbitrage Tracking | Medir economia por routing inteligente (modelo barato vs. premium) |
| 4.1.5 | Cache Savings Tracking | Medir economia por cache hits |

### 4.2 Budget Management

| # | Item | Descricao |
|---|------|-----------|
| 4.2.1 | Budget Limits (Hard/Soft) | USD ou tokens por usuario/team/workspace/mês. Soft → alerta. Hard → bloqueia modelo |
| 4.2.2 | Alertas Automaticos | Email e/ou Slack em thresholds configuraveis (50%, 80%, 100%) |
| 4.2.3 | Cost Projection | Projecao fim de mes baseada em tendencia dos ultimos 7 dias |
| 4.2.4 | Chargeback | Custo por centro de custo (team/workspace) para enterprise |

### 4.3 Stripe Integration

| # | Item | Descricao |
|---|------|-----------|
| 4.3.1 | Subscription Management | Planos Free, Pro ($19.99), Team ($29/user), Enterprise (custom) |
| 4.3.2 | Usage-Based Billing (API) | Pay-as-you-go com markup 10-15% sobre custo de inferencia |
| 4.3.3 | Credit System | Compra de creditos, debito por request, saldo visivel |
| 4.3.4 | Webhook de Pagamento | Stripe webhook para confirmar pagamentos, falhas, renovacoes |
| 4.3.5 | BYOK Pricing | Quando BYOK ativo, cobrar apenas licenca de software (margem 90%+) |

### 4.4 FinOps Dashboard

| # | Item | Descricao |
|---|------|-----------|
| 4.4.1 | Cost Analytics | Graficos de consumo por modelo, usuario, team, projeto |
| 4.4.2 | Spend Projection | Estimativa de gasto ate fim do mes |
| 4.4.3 | ROI Metrics | Horas economizadas, tarefas automatizadas, custo por tarefa |
| 4.4.4 | CSV/Excel Export | Exportacao de relatorios de custo |

**Arquivos novos:**
- `domain/billing/` — Budget, CostEvent, Subscription, CreditBalance
- `application/billing/` — BillingUseCase, MeteringUseCase, BudgetUseCase
- `infrastructure/billing/` — StripeAdapter, CostEventPublisher, ClickHouseCostRepository
- `infrastructure/billing/metering/` — TokenCounter, CostCalculator, BudgetEnforcer
- `presentation/billing/` — BillingController, BudgetController, FinOpsController

---

## FASE 5 — HUB DE INTERACAO E ARTIFACT STUDIO

**Prioridade:** MEDIA-ALTA — Experiencia core do usuario
**Duracao estimada:** 4-5 semanas
**Dependencia:** Fase 0 (streaming), Fase 1 (routing), Fase 3 (RAG)
**Spec Refs:** Func.Mod.B (Hub de Interacao), Func.Mod.D (Artifact Studio), Manus 1.1-1.5

### 5.1 Hub de Interacao (5 Modos)

| # | Item | Descricao |
|---|------|-----------|
| 5.1.1 | Auto Mode | Plataforma seleciona modelo e ferramentas baseado em objetivo, tipo de input, contexto e politica do workspace |
| 5.1.2 | Manual Mode | Usuario escolhe modelo explicitamente |
| 5.1.3 | Compare Mode | Mesmo prompt enviado para ate 4 modelos simultaneamente. Respostas side-by-side com nome, custo, latencia |
| 5.1.4 | Research Mode | Sintese de web, documentos e knowledge bases com citacoes |
| 5.1.5 | Action Mode | Plataforma executa passos em ferramentas conectadas ou browser |
| 5.1.6 | Conversation Forks | Editar mensagem ou regenerar cria fork sem perder historico original |
| 5.1.7 | Multimodal Upload | Drag-and-drop para PDF, DOCX, CSV, imagem, audio, video |
| 5.1.8 | Advanced Parameters | Temperature, Top P, Max Tokens, Penalties, System Instruction, Stream toggle, Cache toggle |
| 5.1.9 | Model Transparency | Indicacao do modelo efetivamente usado quando routing/fallback ocorre |

### 5.2 Artifact Studio

| # | Item | Descricao |
|---|------|-----------|
| 5.2.1 | Artifact Panel | Painel lateral para renderizar artefatos |
| 5.2.2 | Format Support | Rich text, Markdown, HTML, React components, Mermaid, tabelas, FAQs, scripts, codigo |
| 5.2.3 | Quick Actions | Resumir, expandir, reescrever, traduzir, mudar tom, converter para tabela/checklist |
| 5.2.4 | Export | TXT, Markdown, HTML, PDF |
| 5.2.5 | Sharing | Links internos e publicos com controle de permissao |
| 5.2.6 | Versioning | Versionamento de artefatos com diff entre versoes |
| 5.2.7 | Publish as Template | Transformar artefato em template reutilizavel |

### 5.3 Biblioteca de Ativos (Mod.E)

| # | Item | Descricao |
|---|------|-----------|
| 5.3.1 | Asset Types | Prompts, templates, system instructions, agents, playbooks, model presets, workflows publicados |
| 5.3.2 | 3 Niveis de Biblioteca | Pessoal, equipe, corporativa |
| 5.3.3 | Dynamic Variables | Templates com variaveis dinamicas (`{{tone}}`, `{{audience}}`) |
| 5.3.4 | Agent Creation | Identidade, instrucoes, modelo default, knowledge base, tools permitidas, budget, regras de aprovacao |
| 5.3.5 | Pre-built Agents | Minimo 60 agentes prontos (vendas, suporte, juridico, marketing, engenharia, financas, RH, research, ops) |
| 5.3.6 | Asset Metrics | Uso, adocao, custo, satisfacao, taxa de sucesso por ativo |

**Arquivos novos (Frontend):**
- `app/chat/components/CompareMode.tsx`
- `app/chat/components/ResearchMode.tsx`
- `app/chat/components/ActionMode.tsx`
- `app/chat/components/ArtifactPanel.tsx`
- `app/chat/components/ArtifactEditor.tsx`
- `app/library/components/AssetLibrary.tsx`
- `app/library/components/AgentBuilder.tsx`
- `app/library/components/TemplateEditor.tsx`

**Arquivos novos (Backend):**
- `domain/artifact/` — Artifact, ArtifactVersion
- `domain/asset/` — Asset, Agent, Template, Prompt, Playbook
- `application/artifact/` — ArtifactUseCase
- `application/asset/` — AssetUseCase, AgentUseCase

---

## FASE 6 — ORCHESTRACAO, AGENTES E AUTONOMIA

**Prioridade:** MEDIA — Diferencial de longo prazo
**Duracao estimada:** 6-8 semanas
**Dependencia:** Fase 1 (routing), Fase 3 (RAG), Fase 4 (billing)
**Spec Refs:** Tec.Sec.10 (Agent Runtime), Func.Mod.F (Orchestracao), Manus 4.1-4.2

### 6.1 Workflow Runtime (Temporal)

| # | Item | Descricao |
|---|------|-----------|
| 6.1.1 | Temporal Integration | Configurar Temporal como runtime de execucao duravel |
| 6.1.2 | Workflow Definition Model | Steps: AI inference, logic, conditions, loops, delays, HTTP calls, integrations |
| 6.1.3 | Triggers | Webhook, cron schedule, email, file upload, internal event, manual |
| 6.1.4 | Actions | Slack, Teams, Google Sheets, email, Jira, APIs externas |
| 6.1.5 | Execution History | Log completo com replay |
| 6.1.6 | Retry e Resume | Retry com backoff, resume apos falha, queueing |

### 6.2 Agent Runtime

| # | Item | Descricao |
|---|------|-----------|
| 6.2.1 | Agentic Loop | Objetivo → Planner → Tool Selection → Execution → Evaluation → Next Step |
| 6.2.2 | 5 Niveis de Autonomia | L0 (assistivo), L1 (sugere), L2 (executa com confirmacao), L3 (executa dentro de politica), L4 (opera continuamente) |
| 6.2.3 | Tool Registry | Registro de ferramentas com schema, scope, risco, custo estimado |
| 6.2.4 | Human-in-the-Loop | Pausa para aprovacao humana em acoes sensiveis |
| 6.2.5 | Budget Guard | Limite maximo de gasto por execucao de agente |
| 6.2.6 | Checkpoint/Replay | Salvar estado de execucao para resume/replay |
| 6.2.7 | Multi-step Delegation | Agente pode delegar sub-tarefas para outros agentes |

### 6.3 Visual Workflow Builder (Frontend)

| # | Item | Descricao |
|---|------|-----------|
| 6.3.1 | Drag-and-Drop Canvas | Interface visual (React Flow ou similar) |
| 6.3.2 | Step Types | AI prompt, HTTP call, data transform, conditional, loop, delay |
| 6.3.3 | Trigger Config | Configuracao visual de triggers |
| 6.3.4 | Test/Debug Mode | Executar workflow passo-a-passo com inspecao de estado |

### 6.4 MCP e Tool Use

| # | Item | Descricao |
|---|------|-----------|
| 6.4.1 | MCP Client | Conectar a MCP Servers externos (databases, repos, SaaS) |
| 6.4.2 | MCP Server | Expor capabilities da plataforma como MCP Server |
| 6.4.3 | Function Calling | Developer define funcoes; AI decide quando chamar |
| 6.4.4 | Code Sandbox | Container Docker isolado para executar Python/Node.js |
| 6.4.5 | A2A Protocol | Agent-to-Agent collaboration (futuro) |

**Arquivos novos:**
- `domain/execution/` — Workflow, Run, Step, Trigger, ToolInvocation, Approval
- `application/execution/` — WorkflowUseCase, AgentRuntimeUseCase, ToolUseCase
- `infrastructure/execution/` — TemporalAdapter, ToolRegistry, McpClient, McpServer, CodeSandbox
- `presentation/execution/` — WorkflowController, AgentController, ToolController

---

## FASE 7 — PLATFORM, MARKETPLACE E ESCALA

**Prioridade:** MEDIA-BAIXA — Crescimento e monetizacao de ecossistema
**Duracao estimada:** 6-8 semanas
**Dependencia:** Fases 2-6
**Spec Refs:** Func.Mod.I (Plataforma Dev), Func.Mod.J (Catalogo/Marketplace), Neg.Sec.10

### 7.1 Developer Platform (Mod.I)

| # | Item | Descricao |
|---|------|-----------|
| 7.1.1 | Universal API OpenAI-Compatible | `/v1/chat/completions`, `/v1/embeddings`, `/v1/images/generations`, `/v1/audio/transcriptions`, `/v1/audio/speech` |
| 7.1.2 | Virtual Keys | Geracao de API keys por app/team/partner com rate limits, budget, modelo scope |
| 7.1.3 | Webhooks para Developers | Notificacoes: budget atingido, model downtime, latencia alta, auth error |
| 7.1.4 | Batch API | Submissao em lote com ate 50% desconto em tokens. Fila + webhook on completion |
| 7.1.5 | Request Tracing | Log detalhado por request: timestamp, latencias, tokens, custo, modelo solicitado vs. usado |
| 7.1.6 | SDKs e Docs | TypeScript, Python, Go SDKs. OpenAPI spec completa |
| 7.1.7 | Sandbox/Staging | Ambiente de teste isolado |
| 7.1.8 | Per-App Console | Dashboard de uso e custo por aplicacao |

### 7.2 Catalogo e Marketplace (Mod.J)

| # | Item | Descricao |
|---|------|-----------|
| 7.2.1 | Public Catalog | Catalogo publico indexavel de modelos, agentes, templates, conectores |
| 7.2.2 | Private Marketplace | Marketplace interno por tenant para ativos enterprise |
| 7.2.3 | Revenue Share | Comissao para parceiros que publicam ativos |
| 7.2.4 | Rating e Curation | Avaliacao, curadoria e recomendacao de ativos |
| 7.2.5 | Category Filters | Filtros por capability, idioma, custo, modalidade, compliance, segmento |

### 7.3 Voice e Canais (Mod.G)

| # | Item | Descricao |
|---|------|-----------|
| 7.3.1 | Voice Input/TTS | Entrada de voz multi-idioma + leitura por ElevenLabs/Web Speech API |
| 7.3.2 | Real-Time Voice Agents | Agentes de voz em chamada ao vivo |
| 7.3.3 | Channels Integration | Web chat, API, email, Slack, Teams, telefonia |
| 7.3.4 | Browser Runtime | Navegacao, preenchimento de forms, operacao de sistemas em ambiente isolado |

### 7.4 Observability & Evals (Tec.Sec.14)

| # | Item | Descricao |
|---|------|-----------|
| 7.4.1 | ClickHouse Analytics | Telemetria de requests, custos, qualidade em alta escala |
| 7.4.2 | Evaluation Datasets | Datasets versionados por capability |
| 7.4.3 | Scorecards | Scorecards por modelo/provider com regressao antes de rollout |
| 7.4.4 | A/B Testing | Canary e shadow traffic por porcentagem |
| 7.4.5 | Prometheus + Grafana | Dashboards operacionais: disponibilidade, p50/p95/p99, overhead, error rate |

### 7.5 LGPD/GDPR Compliance

| # | Item | Descricao |
|---|------|-----------|
| 7.5.1 | Consent Management | Fluxo de consentimento com registro |
| 7.5.2 | Right to Erasure | Workflow de delecao de dados por solicitacao |
| 7.5.3 | Data Portability | Exportacao completa de dados do usuario |
| 7.5.4 | DPO e Evidencias | Trilha de auditoria para ambientes regulados |

---

## MATRIZ DE DEPENDENCIAS

```
Fase 0 (Seguranca) ──┬──→ Fase 1 (Gateway/Routing)
                      ├──→ Fase 2 (RBAC/SSO)
                      ├──→ Fase 3 (Knowledge/RAG)
                      └──→ Fase 5 (Hub/Artifacts)

Fase 1 (Gateway) ────┬──→ Fase 4 (Billing/FinOps)
                      └──→ Fase 6 (Orchestracao)

Fase 2 (RBAC) ───────┬──→ Fase 4 (Billing/FinOps)
                      └──→ Fase 7 (Platform/Marketplace)

Fase 3 (RAG) ────────┬──→ Fase 5 (Hub/Artifacts)
                      └──→ Fase 6 (Orchestracao)

Fase 4 (Billing) ────→ Fase 7 (Platform/Marketplace)

Fase 6 (Orchestracao) → Fase 7 (Platform/Marketplace)
```

---

## TIMELINE CONSOLIDADA

| Fase | Nome | Semanas | Mes Inicio | Mes Fim |
|------|------|---------|------------|---------|
| 0 | Seguranca e Estabilidade | 2-3 | M1 | M1 |
| 1 | Gateway Inteligente | 3-4 | M1 | M2 |
| 2 | RBAC e Governanca | 3-4 | M2 | M3 |
| 3 | Knowledge Base e RAG | 4-5 | M2 | M3 |
| 4 | Billing e FinOps | 3-4 | M3 | M4 |
| 5 | Hub de Interacao + Artifacts | 4-5 | M3 | M5 |
| 6 | Orchestracao e Agentes | 6-8 | M5 | M7 |
| 7 | Platform e Marketplace | 6-8 | M7 | M9 |

**Paralelismo possivel:**
- Fase 0 e 1 parcialmente paralelas (rate limiting e routing compartilham gateway)
- Fase 2 e 3 paralelas (RBAC e RAG sao independentes)
- Fase 4 e 5 parcialmente paralelas (billing e hub sao parcialmente independentes)

**Total estimado:** 9-12 meses para feature-completeness comercializavel

---

## CRITERIOS DE SUCESSO POR FASE

| Fase | Criterio |
|------|----------|
| 0 | Zero vulnerabilidades criticas. >80% test coverage em auth e routing. Rate limit funcional |
| 1 | Routing YAML com 5 estrategias. Cache hit >20% em ambiente de teste. Fallback <50ms |
| 2 | SSO funcional com pelo menos 1 provider. 4 roles enforced. Audit trail completo |
| 3 | RAG end-to-end: upload → chunk → embed → retrieve → cite. Latencia de retrieval <500ms |
| 4 | Stripe checkout funcional. Budget hard limit bloqueando requests. Dashboard de custos |
| 5 | 5 modos de chat funcionais. Compare mode com 4 modelos. Artifact export para PDF |
| 6 | Workflow 3-step funcional via Temporal. Agent L2 com human-in-the-loop |
| 7 | API OpenAI-compatible passando conformance test. Marketplace com 10+ ativos publicados |

---

## DECISOES TECNICAS RECOMENDADAS

| Decisao | Escolha | Justificativa |
|---------|---------|---------------|
| Workflow Runtime | **Temporal** | Execucao duravel, retry nativo, checkpoint/replay, SDK Java |
| Vector Store | **Qdrant** (primary) + **pgvector** (fallback) | Qdrant para escala, pgvector para simplicidade inicial |
| Analytics DB | **ClickHouse** | Alta ingestao, queries analiticas rapidas, custo-beneficio |
| Message Queue | **Kafka** | Event backbone, async billing, telemetry pipeline |
| Cache | **Redis** | Semantic cache, rate limiting, sessions, distributed locks |
| Search | **Hybrid** (vector + BM25) | Melhor recall para RAG |
| Gateway Lang | **Java (Spring Boot)** | Manter stack unificada. Go pode ser considerado futuro |
| Auth | **Spring Security + JJWT** (existente) + **SAML/OIDC** | Aproveitar o que existe, adicionar federacao |
| Billing | **Stripe** | Standard de mercado, suporta subscription + usage-based + credits |

---

## ITENS DESCARTADOS (NAO AGREGAM VALOR AGORA)

| Item | Razao |
|------|-------|
| Go gateway separado | Complexidade operacional. Java com Virtual Threads atende <20ms overhead |
| NestJS control plane | Duplica backend existente. Spring Boot cobre todas as necessidades |
| Python workers | Desnecessario agora. Inferencia via API de providers. Considerar quando houver fine-tuning |
| gRPC interno | HTTP/REST com Virtual Threads suficiente para P1-P7. gRPC pode ser adicionado depois |
| Mobile nativo | PWA cobre mobile adequadamente na fase inicial |
| Multi-regiao | Prematura. Single-region com multi-AZ para inicio |
| Fine-tuning via plataforma | Feature de Year 2+. Nao agrega valor no MVP-Enterprise |
| White-label completo | Feature de Year 2+. Requer estabilidade da plataforma core |

---

## NOTA SOBRE SCHEMA DUAL (Prisma vs Flyway)

**Problema identificado (GAP-005):** Frontend usa Prisma (schema `codex`) e Backend usa Flyway (schemas `auth`, `billing`, etc.) no mesmo PostgreSQL.

**Recomendacao:**
1. **Curto prazo (Fase 0):** Criar Views PostgreSQL para expor dados do Flyway no formato esperado pelo Prisma
2. **Medio prazo (Fase 2):** Migrar auth e billing para schema unico gerenciado por Flyway, com Prisma lendo via views
3. **Longo prazo (Fase 7):** Avaliar se frontend deve migrar para consumo puro via API (eliminar Prisma direto no DB)

---

*Documento gerado a partir da analise cruzada de: especificacao_tecnica_consolidada, especificacao_funcional_consolidada, especificacao_negocio_consolidada, ESPECIFICACAO_COMPLETA_AGREGADOR_IA, e estado atual do codebase (105 providers, 17 capabilities, arquitetura hexagonal).*
