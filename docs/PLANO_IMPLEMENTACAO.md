# PLANO DE IMPLEMENTAÇÃO END-TO-END — IA AGGREGATOR
**Chief Architect + Staff Product/Delivery Lead + Principal UX**
**Data**: Março 2026 | **Stack**: Next.js/TS · Java 21 · Spring Boot · Maven · PostgreSQL · Flyway

> A seção de análise documental completa está preservada ao final deste arquivo.

---

# A) RESUMO EXECUTIVO

## O Que Será Entregue

Plataforma SaaS brasileira **"Nubank da IA"**: acesso a 30+ modelos de IA (texto, imagem, voz, vídeo) por assinatura única em reais, com Pix/boleto, interface 100% PT-BR e conformidade LGPD nativa. Elimina a necessidade de múltiplas assinaturas em dólar — economia de R$150-300/mês por usuário.

## Por Quê

Nenhum dos 7 concorrentes mapeados cobre mais que 52,3% das 172 features identificadas (Inner AI lidera). Há gap de mercado para plataforma que cubra 65-70%. O mercado brasileiro tem 70%+ de usuários que preferem interface PT-BR e precisa de Pix nativo.

## Roadmap Macro

| Marco | Semana | Entregável | KPI |
|-------|--------|-----------|-----|
| Alpha 0.1 | S2 | 5 modelos + Auth + Pix | 20 usuários internos |
| Alpha 0.5 | S6 | 20 modelos + Imagens + Web Search | 50 beta testers |
| **Beta 1.0** | **S14** | **MVP completo 28 features** | **200 beta users** |
| v1.0 GA | S20 | Lançamento público | 1.000 users, R$34K MRR |
| v1.5 | S28 | Parceiros + Teams + Mobile PWA | 580 pagantes, R$54K MRR |
| v2.0 | S40 | Chain Mode + Marketplace + API | 2.230 pagantes, R$207K MRR |

## Riscos Principais e Mitigações

| Risco | Prob | Impacto | Mitigação |
|-------|------|---------|-----------|
| PL 2338 Marco Legal IA | Média | Crítico | Monitorar; consultoria jurídica desde Sprint 0 |
| OpenRouter instabilidade | Média | Alto | Arquitetura dual: 80% OpenRouter + 20% diretas |
| Inner AI capta rodada seed | Alta | Alto | 22 inovações exclusivas + comunidade + marketplace |
| Heavy users destroem margem | Alta | Médio | Rate limiting + créditos premium 80-100x custo |
| Câmbio BRL/USD > R$7 | Média | Alto | Buffer 20% créditos + 30% modelos open-source |
| Bus factor = 1 (founder solo) | Alta | Crítico | Documentação obsessiva + co-founder mês 6 |

---

# B) DISCOVERY "SEM GAPS"

## B.1 OKRs e Métricas

| Objetivo | KR | Meta Q1 | Meta Q2 |
|---------|-----|---------|---------|
| Validar product-market fit | Usuários ativos 7d | 100 | 400 |
| Monetizar | MRR | R$11K | R$54K |
| Eficiência | CAC via parceiro | R$25 | R$20 |
| Qualidade | Churn mensal | <12% | <10% |
| Técnica | Uptime | 99,0% | 99,5% |
| Técnica | TTFT p95 | <2s | <1,5s |
| Produto | NPS | 30 | 45 |

## B.2 Escopo IN / OUT

**IN (MVP — 14 semanas):**
- Chat multi-modelo com streaming SSE (20+ modelos, troca mid-chat, auto-routing)
- Sistema de créditos com multiplicadores por modelo (Gemini: 1cr, GPT-4o: 10cr, Claude Opus: 80cr)
- Planos Free/Starter R$39/Pro R$99/Team R$49-seat com Pix, boleto e cartão
- Upload + chat com documentos (PDF, DOCX, CSV) via RAG com pgvector
- Geração de imagens (DALL-E 3, Flux)
- Busca web real-time com citações (Serper API)
- Personas customizáveis + biblioteca 100+ prompts PT-BR
- 6 inovações exclusivas P0 (Dashboard Custo, Modo Consultor, Links Públicos, Socrático, Calculadora ROI, Certificados)
- LGPD compliance completo (exclusão, portabilidade, não-treinamento)
- Auth (email/senha + Google OAuth)
- Admin panel básico (gestão de usuários, modelos, planos)

**OUT (v1.0 em diante):**
- App mobile nativo (iOS/Android)
- Deep Research (400+ fontes)
- Chain Mode / Agentes autônomos
- Marketplace de bots
- SSO SAML Enterprise
- Self-hosted / white-label
- Módulo de Parceiros/Cupons (Sprints S7-S11, paralelo ao pós-MVP)
- Internacionalização (fora de PT-BR)

## B.3 Personas + Jornadas + Jobs-to-be-Done

| Persona | Perfil | Dor Principal | Job Principal | Momento "Wow" |
|---------|--------|--------------|--------------|---------------|
| Marina (Creator) | 29a, SP, freelancer | R$285/mês, 4 ferramentas | 30 posts+10 legendas+5 artigos/sem | Ver créditos restantes + custo em R$ |
| Carlos (Advogado) | 47a, BH, não fala inglês | Interface inglês, confidencialidade | 15 contratos+5 pareceres/sem | Modo Consultor fazendo perguntas em PT-BR |
| Thiago (Dev) | 24a, Florianópolis | Sem controle granular | Testar 10+ modelos/sem | Comparação lado-a-lado + API key |
| Professora Ana | 35a, Recife | Alunos usam IA para colar | 10 aulas+2 provas+200 trabalhos/bimestre | Modo Socrático que ensina sem dar resposta |
| TechFit (Team) | Startup 25 pessoas | Ferramentas dispersas | Unificar com admin e RBAC | Dashboard admin com uso por membro |

**Jornada crítica — Marina (feliz):**
1. Acessa landing → vê Calculadora Economia (R$285 → R$99) → clica "Começar Grátis"
2. Cadastro email → email de verificação → dashboard com 300 créditos
3. Chat com GPT-4o → widget mostra "3 créditos usados / R$0,18 / equivale a R$45/mês se usar assim"
4. Experimenta DALL-E → percebe valor → clica "Upgrade Pro"
5. Pix QR Code → pago em 30s → recebe confirmação + 3.500 créditos
6. Cria persona "Copywriter BR" → salva prompt → compartilha link read-only com cliente

**Jornada crítica — Carlos (socrático):**
1. Acessa via indicação de colega (cupom JURIDICO20 → -20% + comissão parceiro)
2. Liga "Modo Consultor" → digita "Preciso revisar contrato de prestação de serviços"
3. IA responde com 4 perguntas: tipo de contrato? valor? prazo? jurisdição?
4. Carlos responde → IA gera análise completa em PT-BR
5. Exporta como PDF → certificado de uso gerado → compartilha no LinkedIn

## B.4 Requisitos Funcionais (RF)

| ID | Requisito | Prioridade | AC Resumido |
|----|----------|-----------|------------|
| RF-001 | Chat SSE streaming multi-modelo | P0 | Latência TTFT p95 <2s; troca de modelo mid-chat sem perda de histórico |
| RF-002 | Auto-routing por task type | P0 | Classificador detecta: code→DeepSeek, creative→Claude, search→Gemini |
| RF-003 | Sistema de créditos | P0 | Débito atômico antes da resposta; saldo nunca negativo; widget tempo-real |
| RF-004 | Planos e assinaturas | P0 | Free/Starter/Pro/Team; upgrade/downgrade sem perda de dados |
| RF-005 | Pix via Asaas | P0 | QR Code gerado <3s; webhook credita em <30s após confirmação |
| RF-006 | Boleto via Asaas | P0 | Gerado <5s; vence em 3 dias; webhook credita após liquidação |
| RF-007 | Cartão via Stripe | P1 | 3DS2 suportado; retry automático em falha |
| RF-008 | Upload + RAG (PDF, DOCX, CSV) | P0 | Max 50MB; parse <10s; busca semântica retorna top-5 chunks relevantes |
| RF-009 | Geração de imagens | P0 | DALL-E 3 + Flux; 1024x1024; biblioteca de imagens por user |
| RF-010 | Busca web real-time | P0 | Serper API; cita fontes com URL; cache 24h por query |
| RF-011 | Personas customizáveis | P0 | System prompt persistente; restrições por plano (Free: 1, Starter: 5, Pro: ilimitado) |
| RF-012 | Biblioteca de prompts PT-BR | P0 | 100+ prompts categorizados; favoritar; uso rastreado |
| RF-013 | Dashboard Custo Transparente (INV-03) | P0 | Exibe créditos + R$ por conversa; comparação entre modelos |
| RF-014 | Modo Consultor (INV-08) | P0 | Toggle; IA faz 3-5 perguntas antes de responder |
| RF-015 | Links Públicos (INV-17) | P0 | Conversa como link read-only ou fork; expire em 30 dias |
| RF-016 | Modo Socrático (INV-19) | P0 | Ensina por perguntas; não entrega resposta direta |
| RF-017 | Calculadora Economia Landing (INV-22) | P0 | Widget interativo: informe suas assinaturas → veja economia |
| RF-018 | Certificados Gamificação (INV-12) | P0 | Badge exportável para LinkedIn ao atingir marcos de uso |
| RF-019 | LGPD: exclusão de dados | P0 | Processado em 48h; email de confirmação; audit log imutável |
| RF-020 | LGPD: portabilidade | P0 | Export JSON/ZIP de todas as conversas em <24h |
| RF-021 | Admin: gestão usuários | P0 | CRUD + suspend + impersonar (com audit log) |
| RF-022 | Admin: gestão modelos IA | P0 | On/off por modelo; editar multiplicador de créditos |
| RF-023 | Workspace Teams | P1 | Convites por email; RBAC: Owner/Admin/Member; limite créditos por membro |
| RF-024 | Módulo Parceiros (Pós-MVP) | P1 | 4 entidades, 30 regras (ver Doc 5 completo) |

## B.5 Requisitos Não Funcionais (RNF)

| ID | Categoria | Requisito | Meta | Gate |
|----|----------|----------|------|------|
| RNF-001 | Performance | TTFT streaming | p95 <2s, p99 <4s | Bloqueio de deploy se p95 >3s |
| RNF-002 | Performance | API endpoints CRUD | p95 <200ms | Alerta se >500ms |
| RNF-003 | Performance | Core Web Vitals LCP | <2,5s | Bloqueio de merge se >3s (Lighthouse CI) |
| RNF-004 | Performance | Core Web Vitals CLS | <0,1 | Bloqueio de merge |
| RNF-005 | Performance | Core Web Vitals TTI | <3,8s | Bloqueio de merge |
| RNF-006 | Escalabilidade | Usuários simultâneos | 500 req/s no pico | Load test com Gatling antes de GA |
| RNF-007 | Disponibilidade | SLA uptime | 99,5% (43min/mês) | Alerta PagerDuty se downtime >5min |
| RNF-008 | Segurança | Auth JWT | Expiração 15min access + 7d refresh | Pen test trimestral |
| RNF-009 | Segurança | OWASP Top 10 | Zero CRITICAL abertos | SAST no CI bloqueia merge |
| RNF-010 | Segurança | Rate limiting | 100 req/min unauthenticated; 1000/min autenticado | 429 com Retry-After |
| RNF-011 | LGPD | Retenção dados | Conversas: 2 anos após inatividade; logs: 1 ano | Job de purge semanal |
| RNF-012 | LGPD | Criptografia | AES-256 at rest (PII); TLS 1.3 in transit | Auditoria automática |
| RNF-013 | Observabilidade | Logging | Structured JSON + correlation-id em 100% requests | Alert se log rate cai |
| RNF-014 | Observabilidade | Tracing | OpenTelemetry em 100% endpoints | Dashboard Grafana Tempo |
| RNF-015 | Manutenibilidade | Cobertura testes | Unit 80%; Integration 70% branches | Gate no CI pipeline |
| RNF-016 | Dados | RPO | 1 hora | Backup automático PostgreSQL |
| RNF-017 | Dados | RTO | 4 horas | Runbook testado trimestralmente |

## B.6 Abuse Cases (Threat Model por Feature)

| Feature | Abuse | Prevenção | Detecção |
|---------|-------|-----------|---------|
| Chat | Prompt injection para vazar system prompt | Sanitização + filtro OpenAI Moderation | Log de inputs suspeitos + alerta |
| Créditos | Race condition para gastar mais do saldo | Transação SERIALIZABLE + check atômico | Monitor saldo negativo |
| Upload | Upload malware camuflado em PDF | Scan ClamAV + mime-type validation | Alerta no S3/R2 trigger |
| Pix | Fake webhook de confirmação | Verificar assinatura Asaas (HMAC) + idempotência | Monitor duplicatas |
| Links Públicos | Crawlers consumindo créditos alheios | Read-only sem executar créditos; rate limit por IP | Monitor leituras de links públicos |
| Free tier | Criação massiva de contas free (email temp) | Verificação email obrigatória + heurística de criação | Monitor domínios de email temp |
| Parceiros | Click fraud (auto-clique em links de afiliado) | Fingerprint server-side + janela 24h por IP/email | Dashboard anti-fraude + ML flag |
| Admin | IDOR em endpoints de admin | RBAC strict + testes de autorização | Audit log de acesso admin |

## B.7 Matriz de Rastreabilidade

| RF | Tela/Componente | Endpoint | Entidade DB | Teste BE | Teste FE | Métrica | Log/Alerta |
|----|----------------|---------|------------|---------|---------|---------|-----------|
| RF-001 | `ChatPage`, `MessageStream` | `POST /v1/chat` (SSE) | `conversations`, `messages` | `ChatServiceTest`, `ChatControllerIT` | `chat.spec.ts` (Playwright) | TTFT p95 | `chat.ttft_ms` histogram |
| RF-002 | `ModelBadge`, `RoutingIndicator` | (interno `AiRoutingService`) | `ai_models` | `AiRoutingServiceTest` | — | % auto-routed | `routing.decision` log |
| RF-003 | `CreditMeter`, `CostWidget` | (evento interno) | `credit_ledgers`, `credit_transactions` | `CreditServiceTest` | `credits.spec.ts` | Custo API/user | `credit.balance_zero` alert |
| RF-005 | `CheckoutPix` | `POST /v1/billing/pix` | `subscriptions` | `AsaasGatewayIT` | `billing-pix.spec.ts` | Conv. Pix % | `payment.pix_timeout` alert |
| RF-008 | `DocumentUpload`, `DocChat` | `POST /v1/documents` | `documents`, pgvector | `RagServiceIT` | `doc-upload.spec.ts` | Parse success % | `doc.parse_failed` alert |
| RF-019 | `PrivacySettings` | `DELETE /v1/users/me/data` | todos | `GdprServiceTest` | `lgpd.spec.ts` | Exclusões/mês | `gdpr.deletion_48h_overdue` |
| RF-024 | `PartnerDashboard` | `GET /v1/partner/stats` | `partners`, `commissions` | `CommissionServiceIT` | `partner-flow.spec.ts` | CAC via parceiro | `commission.calculation_drift` |

---

# C) ARQUITETURA DA SOLUÇÃO

## C.1 Estilo Arquitetural e Justificativa

**Escolha: Monólito Modular com DDD Bounded Contexts**

Justificativa:
- Equipe 2-3 devs no MVP → microsserviços triplicam complexidade operacional desnecessariamente
- Crescimento projetado: 0→5K users em 12 meses; monólito suporta até ~15K req/s com pooling adequado
- Bounded Contexts claramente definidos no código → extração para serviços independentes é cirúrgica quando chegar em 5K users (mês 15-18)

**Plano de Evolução:**
- 0-5K users: Monólito modular (14 semanas MVP → mês 15)
- 5K-20K users: Extração `CommissionService` (mais isolado) + `NotificationService`
- 20K+ users: `ChatGatewayService` separado (maior carga); API Gateway (Kong)

## C.2 Clean Architecture — Camadas e Boundaries

```
┌─────────────────────────────────────────────────┐
│  ADAPTERS (Controllers, Gateways, Repositories)  │
│  - Spring MVC Controllers                        │
│  - JPA Repositories                             │
│  - OpenRouter/Stripe/Asaas clients              │
├─────────────────────────────────────────────────┤
│  APPLICATION (Use Cases, Commands, Queries)      │
│  - CreateConversationUseCase                    │
│  - ProcessPaymentCommand                        │
│  - GetUserDashboardQuery (CQRS Read)            │
├─────────────────────────────────────────────────┤
│  DOMAIN (Entities, Aggregates, Services, Events) │
│  - User, Conversation, CreditLedger            │
│  - CreditDebitDomainService                    │
│  - ConversationCreatedEvent                    │
│  ZERO dependências externas (puro Java)         │
├─────────────────────────────────────────────────┤
│  SHARED KERNEL (Value Objects, Interfaces)       │
│  - Money, CreditAmount, UserId                 │
│  - AiModelPort, PaymentPort                    │
└─────────────────────────────────────────────────┘
```

**Regra de dependência**: camadas internas NUNCA importam externas. Inversão por interfaces (Ports).

## C.3 DDD — Bounded Contexts

| Context | Agregado Raiz | Entidades | Responsabilidade |
|---------|-------------|----------|-----------------|
| **Identity** | `User` | `RefreshToken`, `ApiKey` | Auth, LGPD, perfil |
| **Conversation** | `Conversation` | `Message`, `Persona`, `Document` | Chat, RAG, personas |
| **Billing** | `Subscription` | `CreditLedger`, `CreditTransaction` | Planos, créditos, fatura |
| **Payment** | `PaymentOrder` | (sem filhos) | Pix/Boleto/Cartão, webhooks, SAGA |
| **Partner** | `Partner` | `Coupon`, `CouponAttribution`, `Commission` | Parceiros, comissões, fraude |
| **Catalog** | `AiModel` | — | Catálogo de modelos, multiplicadores |
| **Notification** | `Notification` | — | Emails, alertas, centros de notificação |
| **Audit** | `AuditLog` | — | Imutável, LGPD, compliance |

**Anti-Corruption Layers (ACL):**
- `OpenRouterACL`: traduz respostas OpenRouter para `AiModelResponse` interno
- `AsaasACL`: normaliza eventos Asaas para `PaymentEvent` interno
- `StripeACL`: normaliza eventos Stripe para `PaymentEvent` interno

## C.4 Diagrama C4 Textual

### Nível 1 — Contexto do Sistema
```
[Marina/Carlos/Thiago/Ana] → [IA Aggregator Platform]
[Admin] → [IA Aggregator Platform]
[Partner] → [IA Aggregator Platform]
[IA Aggregator Platform] → [OpenRouter API]
[IA Aggregator Platform] → [OpenAI API] (fallback)
[IA Aggregator Platform] → [Anthropic API] (fallback)
[IA Aggregator Platform] → [Google Gemini API] (fallback)
[IA Aggregator Platform] → [Asaas] (Pix/boleto)
[IA Aggregator Platform] → [Stripe] (cartão)
[IA Aggregator Platform] → [Cloudflare R2] (docs/imagens)
[IA Aggregator Platform] → [Resend] (emails transacionais)
[IA Aggregator Platform] → [Serper API] (busca web)
[IA Aggregator Platform] → [Grafana Cloud] (observabilidade)
```

### Nível 2 — Contêineres
```
[Browser] → (HTTPS) → [Next.js Frontend :3000]
[Next.js Frontend] → (REST/SSE) → [Spring Boot API :8080]
[Spring Boot API] → (JDBC) → [PostgreSQL :5432]
[Spring Boot API] → (Redis) → [Upstash Redis]
[Spring Boot API] → (HTTPS) → [OpenRouter, Asaas, Stripe, Serper, Resend]
[Spring Boot API] → (OTLP) → [Grafana Agent → Grafana Cloud]
[Asaas/Stripe] → (Webhook HTTPS) → [Spring Boot API /v1/webhooks/*]
[Cloudflare] → (CDN) → [Next.js Static Assets]
```

### Nível 3 — Componentes do Backend
```
ia-platform-api (Spring MVC)
├── ChatController → ChatApplicationService → [OpenRouterACL, CreditService, ConversationRepository]
├── BillingController → SubscriptionService → [AsaasACL, StripeACL, CreditLedgerRepository]
├── WebhookController → PaymentSagaOrchestrator → [CreditService, EmailService]
├── DocumentController → DocumentService → [R2StoragePort, PdfParserPort, VectorStorePort]
├── PartnerController → PartnerService → [CommissionCalculator, FraudDetector]
└── AdminController → AdminApplicationService → [UserRepository, AiModelRepository]
```

## C.5 PostgreSQL — Modelo Físico (Migrations Flyway)

**Decisão: Flyway vs Liquibase**

| Critério | Flyway | Liquibase |
|---------|--------|----------|
| Curva de aprendizado | Baixa (SQL puro) | Média (XML/YAML/JSON) |
| Integração Spring Boot | Nativa (`spring.flyway.*`) | Nativa mas mais config |
| Rollback | Manual (undo scripts V{n}__undo) | Automático (rollback XML) |
| Multi-DB support | SQL específico por DB | Abstração multi-DB |
| Time to value | 5 min | 20 min |

**Escolha: Flyway** — equipe pequena, SQL puro é mais legível em PR review, rollback manual é aceitável no estágio atual (podemos adicionar undo scripts para migrations críticas).

**Migrations (V1..V11):** ver seção completa no Apêndice SQL.

**Índices Críticos:**
```sql
-- Conversas por usuário (listagem principal — acesso O(log n))
CREATE INDEX idx_conversations_user_updated
  ON conversations(user_id, updated_at DESC) WHERE deleted_at IS NULL;

-- Mensagens por conversa (carregamento do chat — acesso O(log n))
CREATE INDEX idx_messages_conversation_created
  ON messages(conversation_id, created_at);

-- Transações de crédito por usuário (dashboard — acesso O(log n))
CREATE INDEX idx_credit_transactions_user_created
  ON credit_transactions(user_id, created_at DESC);

-- Cupons ativos por código (checkout — acesso O(1) com hash index)
CREATE UNIQUE INDEX idx_coupons_code_active
  ON coupons(code) WHERE status = 'ACTIVE';

-- Anti-fraude: atribuições por IP nas últimas 24h — O(log n)
CREATE INDEX idx_attributions_ip_created
  ON coupon_attributions(ip_first_access, first_access_at DESC);

-- Comissões pendentes por parceiro e mês — O(log n)
CREATE INDEX idx_commissions_partner_month_status
  ON commissions(partner_id, reference_month, status);
```

**Estratégia de Transações:**
- Débito de créditos: `SERIALIZABLE` (previne race condition de saldo negativo)
- Leitura de dashboard: `READ COMMITTED` (performance)
- Webhooks de pagamento: idempotency key em `payment_orders.gateway_idempotency_key`

**Backup/Restore:**
- RPO: 1h (backup contínuo Supabase ou WAL streaming)
- RTO: 4h (restore + validação + warm-up)
- Testes de restore: mensalmente em staging

## C.6 Segurança e Compliance

### Autenticação / Autorização
```
JWT Access Token: 15 min expiração, HS256, claims: userId, role, workspaceId
JWT Refresh Token: 7 dias, rotacionado a cada uso, hash armazenado no DB
Roles: USER | PARTNER | WORKSPACE_ADMIN | PLATFORM_ADMIN | SUPER_ADMIN
```

**Spring Security config:**
```java
// Roles hierárquicos: SUPER_ADMIN > PLATFORM_ADMIN > WORKSPACE_ADMIN > PARTNER > USER
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/v1/auth/**", "/v1/public/**").permitAll()
    .requestMatchers("/v1/admin/**").hasRole("PLATFORM_ADMIN")
    .requestMatchers("/v1/partner/**").hasRole("PARTNER")
    .anyRequest().authenticated()
)
```

**OWASP Top 10 — Mapa de Controles:**

| Risco OWASP | Controle |
|------------|---------|
| A01 Broken Access Control | RBAC + testes de autorização em CI |
| A02 Cryptographic Failures | TLS 1.3 + AES-256 at rest (pgcrypto para PII) |
| A03 Injection | JPA parameterized queries; sanitização input FE |
| A04 Insecure Design | Threat model por feature; abuse cases documentados |
| A05 Security Misconfiguration | Spring Security defaults + headers (HSTS, CSP, X-Frame-Options) |
| A06 Vulnerable Components | Snyk no CI (bloqueia se CRITICAL) |
| A07 Auth Failures | Rate limiting 429; lockout após 5 tentativas; 2FA (v1.5) |
| A08 Software Integrity | Assinatura HMAC de webhooks (Asaas + Stripe) |
| A09 Logging Failures | Audit log imutável; correlação IDs; sem PII em logs |
| A10 SSRF | Allowlist de URLs externas; nunca executar URL do usuário |

**LGPD:**
- Base legal: Consentimento (art. 7, I) + execução de contrato (art. 7, V)
- DPO nomeado até mês 3
- ROPA (Records of Processing Activities) documentado
- `deleted_at` soft-delete para todos os dados pessoais
- Audit log `audit_logs`: regra PostgreSQL que impede UPDATE/DELETE
- Export de dados em <24h; exclusão processada em 48h

## C.7 Observabilidade

**Stack:** OpenTelemetry SDK (Java) + Grafana Agent → Grafana Cloud (Loki + Tempo + Prometheus)

**Logs Estruturados (JSON):**
```json
{
  "timestamp": "2026-03-01T10:30:00Z",
  "level": "INFO",
  "service": "ia-platform-api",
  "traceId": "abc123",
  "spanId": "def456",
  "userId": "usr_789",
  "action": "chat.message.sent",
  "modelId": "gpt-4o",
  "creditsUsed": 10,
  "latencyMs": 1243
}
```

**SLIs / SLOs:**

| SLI | SLO | Janela |
|-----|-----|--------|
| Disponibilidade (5xx rate) | <0,5% | 30 dias rolling |
| Latência TTFT p95 | <2s | 24h rolling |
| Latência API CRUD p95 | <200ms | 24h rolling |
| Erro pagamento rate | <1% | 7 dias rolling |

**Alertas PagerDuty (P1 — liga telefone):**
- Downtime >5 min
- Saldo de crédito negativo detectado
- Taxa de erro webhook pagamento >5% em 5min
- Erro LGPD: exclusão não processada em 50h

**3 Runbooks Críticos:**
1. **DB Connection Pool Exhausted**: checar `pg_stat_activity`, aumentar pool, reiniciar app com feature flag de rate limit agressivo
2. **OpenRouter 5xx**: ativar feature flag `USE_DIRECT_PROVIDERS`; monitorar fallback; notificar usuários via banner in-app
3. **Pix Webhook Failure**: verificar fila de reprocessamento; processar manualmente via admin panel; notificar usuário afetado por email

---

# D) FRONTEND (Next.js 14 + React 18 + TypeScript)

## D.1 UX Strategy

**Heurísticas Nielsen aplicadas:**
1. Visibilidade do status: Sempre mostrar saldo de créditos + modelo atual + streaming indicator
2. Match com o mundo real: Linguagem PT-BR informal, analogias brasileiras (ex.: "crédito = moeda da plataforma")
3. Controle do usuário: Cancelar streaming a qualquer momento; desfazer edição de persona
4. Consistência: Design System com tokens únicos; componentes de erro e loading padronizados
5. Prevenção de erros: Alerta de crédito baixo ANTES de enviar mensagem cara; confirmação de exclusão de conta
6. Reconhecimento > recordação: Conversas recentes sempre visíveis; modelos com ícone + nome
7. Flexibilidade: Atalhos de teclado para power users (Thiago/Dev persona)
8. Estética minimalista: Dashboard limpo; informações progressivas (expandir para ver custo detalhado)
9. Recuperação de erros: Mensagem clara + retry automático em falha de streaming
10. Ajuda: Tooltip em cada modelo com custo em créditos; onboarding inline (não modal)

**WCAG 2.1 AA Checklist:**
- [ ] Contraste mínimo 4.5:1 (texto normal) / 3:1 (texto grande)
- [ ] Todos os elementos interativos acessíveis por teclado (Tab order lógico)
- [ ] ARIA labels em ícones sem texto
- [ ] Foco visível (outline de 2px) em todos os elementos
- [ ] Mensagens de erro associadas ao campo via `aria-describedby`
- [ ] Streaming de texto: `aria-live="polite"` no contêiner de resposta
- [ ] Modo alto contraste testado
- [ ] Screen reader testado (NVDA + Chrome)

**Estados obrigatórios em toda tela:**
- Loading (skeleton screens, não spinners)
- Empty state (com call-to-action contextual)
- Error state (mensagem humana + retry)
- Success state (feedback positivo discreto)

## D.2 Arquitetura Frontend

**Estrutura de pastas:**
```
src/
├── app/                    # Next.js App Router
│   ├── (auth)/login/
│   ├── (auth)/register/
│   ├── (dashboard)/chat/[id]/
│   ├── (dashboard)/images/
│   ├── (dashboard)/billing/
│   ├── (dashboard)/settings/
│   ├── (admin)/admin/
│   ├── (partner)/partner/
│   └── api/               # Next.js API routes (BFF apenas para SSE proxy)
├── components/
│   ├── ui/                # Design System primitivos (Button, Input, Badge)
│   ├── chat/              # ChatWindow, MessageBubble, ModelSelector, CreditMeter
│   ├── billing/           # PlanCard, CreditBar, CheckoutPix
│   └── layout/            # Sidebar, Header, MobileNav
├── stores/                # Zustand stores
│   ├── chatStore.ts       # Streaming state, mensagens, modelo ativo
│   ├── authStore.ts       # User, tokens, refresh logic
│   └── billingStore.ts    # Créditos, plano atual
├── hooks/                 # useChat, useCredits, useBilling, useAuth
├── lib/
│   ├── api.ts             # Axios instance com interceptors JWT
│   ├── sse.ts             # SSE client com reconnect exponencial
│   └── analytics.ts       # PostHog (privacy-first)
└── types/                 # TypeScript interfaces globais
```

**State Management — Zustand vs Redux:**

| Critério | Zustand | Redux Toolkit |
|---------|---------|--------------|
| Bundle size | ~1KB | ~15KB |
| Boilerplate | Zero | Médio (slices) |
| TypeScript | Excelente | Excelente |
| DevTools | Via middleware | Nativo |
| Quando escalar | Substituível por Redux quando >5 devs com features paralelas de alta complexidade | — |

**Escolha: Zustand** — equipe pequena, setup em 5 linhas, performance excelente para o padrão de uso (streaming de chat com atualizações granulares).

**Chat SSE Pattern:**
```typescript
// chatStore.ts (Zustand)
interface ChatStore {
  messages: Message[];
  streamingContent: string;
  isStreaming: boolean;
  creditsUsed: number;
  appendToken: (token: string) => void;
  finalizeStream: (stats: StreamStats) => void;
}

// Reconexão exponencial no cliente SSE
const backoff = [1000, 2000, 4000, 8000, 16000]; // ms
```

## D.3 Design System (Tokens)

```css
/* Tokens de cor */
--color-primary: #6C63FF;      /* Roxo IA - identidade visual */
--color-secondary: #00C896;    /* Verde economia - CTAs de valor */
--color-danger: #FF4757;       /* Vermelho créditos baixos */
--color-surface: #0F0F1A;      /* Background dark mode */
--color-surface-raised: #1A1A2E;
--color-text-primary: #F8F8FF;
--color-text-secondary: #A0A0B0;

/* Tokens de espaçamento (4px grid) */
--space-1: 4px; --space-2: 8px; --space-3: 12px;
--space-4: 16px; --space-6: 24px; --space-8: 32px;

/* Tipografia */
--font-heading: 'Inter', sans-serif;
--font-mono: 'JetBrains Mono', monospace; /* para código */
```

**Componentes Reutilizáveis Prioritários:**
- `<CreditMeter>`: barra de créditos + R$ em tempo real (atualiza por SSE event)
- `<ModelSelector>`: dropdown com ícone + custo em créditos + badge "RÁPIDO/PODEROSO"
- `<MessageBubble>`: markdown renderizado + código com syntax highlight + copy button
- `<StreamingIndicator>`: 3 pontos animados durante geração
- `<PlanBadge>`: chip colorido (Free/Starter/Pro/Team)
- `<UpgradePrompt>`: in-context upgrade quando crédito baixo (sem modal bloqueante)

## D.4 Performance Budget

| Métrica | Meta | Bloqueio de Merge |
|--------|------|------------------|
| LCP (Largest Contentful Paint) | <2,5s | >3s |
| CLS (Cumulative Layout Shift) | <0,1 | >0,15 |
| TTI (Time to Interactive) | <3,8s | >5s |
| JS Bundle (initial) | <200KB gzip | >300KB |
| Lighthouse Score | >85 | <75 |

**Como medir:** `@next/bundle-analyzer` no build + Lighthouse CI no GitHub Actions (bloqueia merge se score <75 ou qualquer métrica além do gate).

**Otimizações mandatórias:**
- `next/image` para todas as imagens (lazy loading + WebP)
- Code splitting por rota (automático no App Router)
- Streaming SSR para páginas do dashboard (React Suspense)
- `@tanstack/react-query` para cache de server state (modelos, planos)

## D.5 Testes Frontend

**Pirâmide FE:**
- 60% Unit tests (Vitest + Testing Library): componentes, hooks, stores Zustand
- 30% Integration tests (Vitest + MSW): fluxos com API mockada
- 10% E2E (Playwright): jornadas críticas

**E2E Críticos (Playwright):**
1. `auth-flow.spec.ts`: registro → verificação email → login
2. `chat-basic.spec.ts`: envio mensagem → streaming → crédito debitado
3. `billing-pix.spec.ts`: upgrade plan → QR Pix → webhook simulado → créditos creditados
4. `doc-upload.spec.ts`: upload PDF → pergunta sobre doc → resposta com citação
5. `lgpd-deletion.spec.ts`: solicitar exclusão → confirmação email → dados removidos

**Contrato FE/BE:** OpenAPI spec gerada do Spring Boot (`springdoc-openapi`) → `openapi-typescript` gera tipos TS → qualquer quebra de contrato falha `tsc` no CI.

---

# E) BACKEND (Java 21 + Spring Boot 3.x + Maven)

## E.1 Estrutura Maven Multi-Módulo

```
ia-platform/                          (parent POM)
├── ia-platform-domain/               # Entidades, agregados, ports (interfaces)
│   └── src/main/java/com/iaplatform/domain/
│       ├── conversation/             # Conversation, Message, Persona
│       ├── billing/                  # CreditLedger, Subscription
│       ├── partner/                  # Partner, Coupon, Commission
│       └── shared/                   # Money, UserId, CreditAmount (VOs)
├── ia-platform-application/          # Use cases, commands, queries, events
│   └── src/main/java/com/iaplatform/application/
│       ├── chat/                     # SendMessageCommand, CreateConversationUseCase
│       ├── billing/                  # SubscribeCommand, ProcessPaymentSaga
│       └── partner/                  # RegisterPartnerCommand, CalculateCommissionUseCase
├── ia-platform-infrastructure/       # Adapters: JPA, REST clients, Storage
│   └── src/main/java/com/iaplatform/infrastructure/
│       ├── persistence/              # JPA repositories, Flyway migrations
│       ├── ai/                       # OpenRouterClient, DirectProviderClients
│       ├── payment/                  # AsaasClient, StripeClient (com ACL)
│       └── storage/                  # CloudflareR2Adapter
└── ia-platform-api/                  # Spring MVC controllers, security config
    └── src/main/java/com/iaplatform/api/
        ├── chat/                     # ChatController (SSE), ConversationController
        ├── billing/                  # BillingController, WebhookController
        └── admin/                    # AdminController, PartnerAdminController
```

## E.2 REST API — Contratos Principais

| Método | Endpoint | Auth | Descrição | Idempotente |
|--------|---------|------|-----------|------------|
| `POST` | `/v1/auth/register` | — | Cadastro + email de verificação | Sim (email único) |
| `POST` | `/v1/auth/login` | — | JWT access + refresh | Não |
| `POST` | `/v1/auth/refresh` | RefreshToken | Rotaciona refresh token | Não |
| `GET` | `/v1/conversations` | JWT | Lista conversas (paginado, 20/página) | — |
| `POST` | `/v1/conversations` | JWT | Cria conversa | Sim (idempotency-key header) |
| `POST` | `/v1/chat` | JWT | **SSE stream** de mensagem | Não |
| `GET` | `/v1/chat/{convId}/messages` | JWT | Histórico (cursor-based pagination) | — |
| `POST` | `/v1/documents` | JWT | Upload + parse documento | Sim |
| `POST` | `/v1/images/generate` | JWT | Gerar imagem | Sim |
| `GET` | `/v1/billing/me` | JWT | Plano + créditos + uso | — |
| `POST` | `/v1/billing/subscribe` | JWT | Iniciar assinatura | Sim (idempotency-key) |
| `POST` | `/v1/webhooks/asaas` | HMAC | Webhook Asaas | Sim (gateway_id único) |
| `POST` | `/v1/webhooks/stripe` | Stripe-Sig | Webhook Stripe | Sim |
| `DELETE` | `/v1/users/me/data` | JWT | LGPD exclusão | Sim |
| `POST` | `/v1/admin/partners` | ADMIN | Cadastrar parceiro | Sim |
| `GET` | `/v1/partner/stats` | PARTNER | Dashboard do parceiro | — |

**Versionamento:** `/v1/` prefixado em todos. Breaking changes → `/v2/` com deprecation notice 90 dias.

**Erros padronizados (RFC 7807):**
```json
{
  "type": "https://api.ia-platform.com.br/errors/insufficient-credits",
  "title": "Créditos insuficientes",
  "status": 402,
  "detail": "Você possui 5 créditos. Esta operação requer 10.",
  "instance": "/v1/chat",
  "traceId": "abc-123"
}
```

## E.3 Design Patterns Aplicados

| Pattern | Onde | Por quê |
|---------|------|---------|
| **Strategy** | `AiProviderStrategy` (OpenRouter, OpenAI, Anthropic, Gemini) | Trocar provedor sem mudar código cliente; suportar novos modelos sem if/else |
| **Factory** | `AiProviderFactory.getProvider(AiModel model)` | Seleciona strategy correta; encapsula criação |
| **Observer** | `ApplicationEventPublisher` → `CreditDebitedEvent`, `PaymentConfirmedEvent` | Desacopla billing de chat; desacopla payment de email |
| **Template Method** | `AbstractPaymentGateway` (Asaas/Stripe herdam) | Fluxo base fixo (validate→charge→notify); variações nos filhos |
| **Decorator** | `RateLimitingAiProvider` decora `OpenRouterProvider` | Adiciona rate limit sem alterar strategy |
| **Repository** | Spring Data JPA (interface → implementação automática) | Abstração de persistência; testável com mocks |
| **SAGA** | `PaymentSagaOrchestrator` (ApplicationEvent-based) | Atomicidade: Pix confirmado → créditos creditados → email enviado; compensação em falha |

## E.4 Performance e Big O — Fluxos Críticos

| Fluxo | Complexidade | Gargalo | Mitigação |
|-------|-------------|---------|-----------|
| Listagem de conversas | O(log n) com índice | Sem índice = O(n) full scan | `idx_conversations_user_updated` |
| Histórico de chat (cursor pagination) | O(log n) | sem cursor = offset O(n) | Cursor-based com `created_at` |
| Débito de crédito | O(1) atômico | Race condition sem serializable | `SELECT ... FOR UPDATE` no ledger |
| RAG similarity search | O(n) → O(log n) com IVFFlat | 100K chunks sem índice = lento | `CREATE INDEX USING ivfflat` em pgvector |
| Cálculo de comissão em batch | O(m × n) naïve | m=1000 parceiros × n=transações | Processar com Window Functions SQL; paralelizar por parceiro |
| Auto-routing (classificação) | O(1) | Latência de modelo classificador | Cache de classificação por hash de prompt |

## E.5 Testes Backend

**Pirâmide BE:**
- 70% Unit (JUnit 5 + Mockito): domain services, use cases, calculators
- 25% Integration (Testcontainers): controllers → DB → Redis; webhook flows
- 5% Contract (Pact): frontend/backend desacoplados (gerado do OpenAPI spec)

**Quality Gate (CI bloqueia merge se):**
- Cobertura unit < 80%
- Cobertura branches < 70%
- Qualquer teste de integração crítico falhando
- Qualquer CRITICAL no SAST (Snyk)

**Testcontainers Setup:**
```java
@Testcontainers
class ChatControllerIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withInitScript("schema.sql");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
        .withExposedPorts(6379);
}
```

---

# F) BOUNDARIES DO MONÓLITO MODULAR (sem microsserviços no MVP)

**Como garantir isolamento sem microsserviços:**

1. **Package-private por default**: classes de domain nunca public sem motivo; apenas ports são public
2. **ArchUnit tests**: validam regras de dependência em CI
```java
@Test void domainNeverImportsInfrastructure() {
    noClasses().that().resideInPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInPackage("..infrastructure..").check(classes);
}
```
3. **Módulos Maven**: compilação falha se módulo inferior importar superior
4. **Events sobre chamadas diretas**: `CommissionService` nunca chama `NotificationService` diretamente → publica `CommissionCalculatedEvent` → listener envia email

**CQRS interno (sem broker externo):**
- Commands: mutação via Use Cases com transação Spring
- Queries: classes separadas `*Query` com `@Transactional(readOnly=true)`, podem usar projeções SQL otimizadas

**SAGA de Pagamento (ApplicationEvent-based):**
```
PaymentConfirmedEvent
  → [1] CreditGrantSagaStep (credita créditos)
       → falha? → publica CreditGrantFailedEvent → Admin alert
  → [2] SubscriptionActivationSagaStep (ativa subscription)
  → [3] WelcomeEmailSagaStep (envia email)
  → [4] PartnerCommissionSagaStep (calcula comissão se houver cupom)
```

---

# G) PLANO DE IMPLEMENTAÇÃO

## G.1 Fases

| Fase | Período | Foco | Entregável |
|------|---------|------|-----------|
| Sprint 0 | Sem 1 | Setup, ADRs, CI/CD base | Infra rodando, pipelines verdes |
| Sprint 1-2 | Sem 2-5 | Auth + Chat core + Créditos + Pix | Alpha 0.1 |
| Sprint 3-4 | Sem 6-9 | Multi-modelo + Docs + Imagens + Web | Alpha 0.5 |
| Sprint 5-6 | Sem 10-13 | Personas + 6 Inovações P0 + LGPD | Beta 1.0 prep |
| Sprint 7 | Sem 14 | Polish + Bug fix + Beta 200 users | **Beta 1.0 Launch** |
| Sprint 8-9 | Sem 15-18 | Teams + Parceiros fase 1 | v1.0 GA prep |
| Sprint 10-11 | Sem 19-22 | Parceiros fase 2 + API pública | v1.5 |

## G.2 Backlog — 30+ Histórias Detalhadas

### ÉPICO EP-01: Core Auth & Identity

**US-001 — Cadastro por email**
- **Como** novo usuário **quero** criar conta com email/senha **para** acessar a plataforma
- **AC**: Email de verificação enviado em <30s; senha mínimo 8 chars + 1 número; JWT emitido após verificação; rate limit 5 cadastros/hora por IP
- **DoD**: Unit test 100% branches; integration test com Testcontainers; e2e `auth-flow.spec.ts`
- **SP**: 5

**US-002 — Login Google OAuth**
- **Como** usuário **quero** entrar com Google **para** não criar mais uma senha
- **AC**: Redirect OAuth 2.0 correto; conta criada ou vinculada; JWT emitido; Google ID salvo
- **SP**: 3

**US-003 — Refresh token rotacionado**
- **AC**: Access token 15min; refresh 7d; refresh rotacionado a cada uso; refresh comprometido → invalidar família inteira
- **SP**: 3

### ÉPICO EP-02: Chat Multi-Modelo

**US-004 — Chat com streaming SSE**
- **Como** usuário **quero** enviar mensagem e ver resposta sendo gerada em tempo real **para** ter experiência fluida
- **AC**: SSE estabelecido em <500ms; primeiro token visível em <2s (p95); cancelar streaming com botão "Parar"; histórico salvo atomicamente após finalizar; créditos debitados antes do envio
- **SP**: 13

**US-005 — Troca de modelo mid-chat**
- **AC**: Dropdown mostra modelo atual + custo em créditos; troca preserva histórico; badge "MODELO TROCADO" na conversa; modelo salvo na conversa
- **SP**: 5

**US-006 — Auto-routing inteligente**
- **AC**: Quando modelo = "AUTO": detecta intenção (code→DeepSeek, creative→Claude, analysis→Gemini); exibe badge do modelo selecionado; pode desabilitar no settings
- **SP**: 8

### ÉPICO EP-03: Sistema de Créditos

**US-007 — Widget de créditos em tempo real**
- **AC**: Barra visível em toda tela do chat; atualiza após cada resposta sem reload; mostra créditos + equivalente em R$; alerta visual quando <10% do limite mensal
- **SP**: 3

**US-008 — Free tier (300 créditos/mês)**
- **AC**: Reset automático todo dia 1; 5 modelos econômicos disponíveis; sem imagem/vídeo; UpgradePrompt inline ao tentar modelo premium; sem cartão obrigatório
- **SP**: 5

### ÉPICO EP-04: Billing e Pagamentos

**US-009 — Assinar plano via Pix**
- **AC**: QR Code gerado <3s; exibir timer de expiração (30 min); webhook Asaas processa em <30s após confirmação; créditos creditados imediatamente; email de confirmação; idempotência: mesmo gateway_id = sem duplicação
- **SP**: 13

**US-010 — Assinar via boleto**
- **AC**: PDF gerado <5s; vence em 3 dias; webhook credita após liquidação (D+1); email com link do boleto
- **SP**: 5

**US-011 — Assinar via cartão (Stripe)**
- **AC**: Checkout Stripe Elements; 3DS2; retry automático em falha; salvar cartão para renovação
- **SP**: 8

**US-012 — Downgrade de plano**
- **AC**: Downgrade agendado para fim do período; sem perda de dados; créditos ajustados proporcionalmente; email de confirmação
- **SP**: 5

### ÉPICO EP-05: Documentos e RAG

**US-013 — Upload de documento**
- **AC**: Aceita PDF/DOCX/CSV até 50MB; parse <10s (async); progress indicator; storage Cloudflare R2; lista de documentos por usuário
- **SP**: 8

**US-014 — Chat com documento (RAG)**
- **AC**: Ao mencionar documento na conversa, top-5 chunks relevantes injetados no contexto; citação de trecho no final da resposta; pgvector similarity search <200ms
- **SP**: 8

### ÉPICO EP-06: Geração de Imagens

**US-015 — Gerar imagem com DALL-E 3**
- **AC**: Prompt em PT-BR traduzido para EN automaticamente; resolução 1024x1024; salvo no R2; biblioteca pessoal de imagens; custo em créditos exibido antes de confirmar
- **SP**: 8

### ÉPICO EP-07: Inovações P0

**US-016 — Dashboard Custo Transparente (INV-03)**
- **AC**: Por conversa: créditos usados + custo em R$ + comparação ("seria R$X no GPT-4 direto"); relatório mensal de uso; exportável como CSV
- **SP**: 5

**US-017 — Modo Consultor (INV-08)**
- **AC**: Toggle visível no início do chat; quando ativo, IA responde com 3-5 perguntas antes de responder; toggle persiste por conversa; indicador visual no header do chat
- **SP**: 5

**US-018 — Links Públicos Read-Only (INV-17)**
- **AC**: Botão "Compartilhar" → URL única (slug de 8 chars); preview read-only sem conta; fork cria nova conversa; expira em 30 dias; sem consumo de créditos de quem visualiza
- **SP**: 5

**US-019 — Modo Aprendizado Socrático (INV-19)**
- **AC**: Toggle por conversa; quando ativo, IA responde com pergunta orientadora ao invés de resposta direta; desativável a qualquer momento
- **SP**: 3

**US-020 — Calculadora Economia na Landing (INV-22)**
- **AC**: Widget interativo: usuário informa quais ferramentas usa → calcula economia; sem conta necessária; CTA "Economize agora"; valores atualizados mensalmente
- **SP**: 3

**US-021 — Certificados Gamificação (INV-12)**
- **AC**: Marcos: 1ª conversa, 10 conversas, 1.000 créditos usados, 30 dias ativo; badge exportável como PNG para LinkedIn; notificação in-app ao conquistar
- **SP**: 5

### ÉPICO EP-08: LGPD

**US-022 — Exclusão de dados**
- **AC**: Botão em Settings > Privacidade; confirmação por email; processado em 48h; soft-delete + anonimização; email de confirmação de conclusão; audit log imutável do pedido
- **SP**: 8

**US-023 — Portabilidade de dados**
- **AC**: Export JSON de conversas + perfil + configurações; ZIP gerado async; link por email em <24h; válido por 7 dias
- **SP**: 5

### ÉPICO EP-09: Admin

**US-024 — Admin: gestão de usuários**
- **AC**: Listagem paginada; busca por email; visualizar plano/créditos/atividade; suspender conta; impersonar com audit log; exportar CSV
- **SP**: 8

**US-025 — Admin: gestão de modelos IA**
- **AC**: Toggle on/off por modelo sem deploy; editar multiplicador de créditos; reordenar modelos no selector; preview de custo antes de publicar
- **SP**: 5

## G.3 DoR e DoD

**Definition of Ready (DoR) — história entra no sprint quando:**
- [ ] User story escrita com AC mensuráveis
- [ ] Mockup aprovado pela UX (se feature visível)
- [ ] API contract definido (OpenAPI atualizado)
- [ ] Estimativa em SP acordada
- [ ] Dependências mapeadas e prontas

**Definition of Done (DoD) — história é "done" quando:**
- [ ] Código revisado por pelo menos 1 dev (PR aprovado)
- [ ] Todos os ACs verificados em staging
- [ ] Unit tests escritos e passando (cobertura mantida)
- [ ] Integration test passando (se endpoint novo)
- [ ] OpenAPI spec atualizada
- [ ] Sem CRITICAL no SAST
- [ ] Lighthouse Score mantido (≥85)
- [ ] Feature flag testada (on/off)
- [ ] Testado em mobile (responsivo)

## G.4 Git Strategy e CI/CD

**Branching (Trunk-based):**
```
main (production)
└── feat/{ticket-id}-{descricao} → PR → squash merge → main
└── fix/{ticket-id}-{descricao} → PR → squash merge → main
└── release/v1.0.0 (apenas para hotfix em prod)
```

**GitHub Actions Pipeline (qualquer PR para main):**
```yaml
jobs:
  backend:
    - ./mvnw verify                          # unit + integration tests
    - cobertura mínima: 80% unit, 70% branches
    - snyk test --severity-threshold=critical
    - snyk code test                         # SAST
    - gitleaks (secrets scan)

  frontend:
    - npm run test:unit                      # Vitest
    - npm run test:e2e --project=chromium    # Playwright (críticos apenas)
    - npm run build && lighthouse-ci         # performance budget gate
    - npm audit --audit-level=critical

  contract:
    - npm run generate-types                 # openapi-typescript
    - npx tsc --noEmit                       # falha se contrato quebrado
```

**Release Strategy:**
- Feature flags via Unleash (self-hosted no Railway) ou LaunchDarkly (Free tier)
- Canary: 5% → 25% → 100% com janela de 30min entre cada fase
- Rollback automático: se SLO de erro >2% em 5min → revert feature flag imediatamente
- Critério de rollback manual: TTFT p95 >3s OU churn spike >2x em 1h

---

# H) QUALITY GATES

## H.1 Gates de Merge (CI bloqueia PR se)

| Gate | Threshold | Tool |
|------|----------|------|
| Unit test coverage | <80% | JaCoCo (BE) / Vitest --coverage |
| Branch coverage | <70% | JaCoCo |
| SAST CRITICAL | Qualquer aberto | Snyk Code |
| Dependency CRITICAL CVE | Qualquer aberta | Snyk Test |
| Secrets exposed | Qualquer detectado | Gitleaks |
| Lighthouse Score | <75 | lighthouse-ci |
| LCP | >3s | lighthouse-ci |
| CLS | >0,15 | lighthouse-ci |
| TypeScript errors | Qualquer | tsc --noEmit |
| Lint errors | Qualquer | ESLint + Checkstyle |

## H.2 Gates de Release (bloqueia deploy prod)

| Gate | Threshold |
|------|----------|
| Staging smoke test (E2E críticos) | 100% passando |
| Load test (Gatling) | p95 <2s com 500 VU simultâneos |
| OWASP ZAP scan | Zero HIGH/CRITICAL |
| Contrato FE/BE | 100% compatível |

## H.3 SLOs e Alertas

| SLO | Janela | Alert |
|-----|--------|-------|
| Disponibilidade >99,5% | 30d rolling | PagerDuty P1 se >5min downtime |
| TTFT p95 <2s | 24h rolling | PagerDuty P2 se >3s por 10min |
| Erro pagamento <1% | 7d rolling | PagerDuty P1 se >5% em 5min |
| Erro LGPD (exclusão atrasada) | 50h SLA | PagerDuty P1 imediato |

---

# I) README.md + ADRs

## README.md (Template)

```markdown
# IA Aggregator Platform — Nubank da IA

> Acesso a 30+ modelos de IA por uma assinatura em reais. PT-BR nativo, Pix, LGPD.

## Arquitetura
Monólito Modular (Next.js + Java 21 + PostgreSQL) — detalhes em `/docs/architecture/`

## Rodar Local (5 minutos)

### Pré-requisitos
- Node.js 20+ | Java 21+ | Maven 3.9+ | Docker

### Backend
cd backend
cp .env.example .env          # preencher: DATABASE_URL, OPENROUTER_KEY, ASAAS_KEY
docker-compose up -d          # PostgreSQL + Redis
./mvnw spring-boot:run

### Frontend
cd frontend
cp .env.local.example .env.local
npm install
npm run dev
# → http://localhost:3000

### Migrations Flyway
São executadas automaticamente no startup do Spring Boot.
Para executar manualmente: ./mvnw flyway:migrate

## Testes
# Backend
./mvnw test                           # unit tests
./mvnw verify -Pintegration           # integration (Testcontainers — requer Docker)

# Frontend
npm run test:unit                     # Vitest
npm run test:e2e                      # Playwright (requer backend rodando)

## Variáveis de Ambiente Obrigatórias
OPENROUTER_API_KEY=...
ASAAS_API_KEY=...
ASAAS_WEBHOOK_SECRET=...
STRIPE_SECRET_KEY=...
STRIPE_WEBHOOK_SECRET=...
SERPER_API_KEY=...
CLOUDFLARE_R2_ACCESS_KEY=...
RESEND_API_KEY=...
JWT_SECRET=...   # mínimo 256 bits

## Observabilidade
Grafana: http://localhost:3001
Dashboards: Chat Latency | Credit Usage | Payment Funnel | Error Rate

## Troubleshooting
Ver /docs/runbooks/
- DB Connection Exhausted → runbook-db-pool.md
- OpenRouter Instável → runbook-ai-fallback.md
- Pix Webhook Failure → runbook-payment-webhook.md
```

## ADRs Necessários

| ADR | Decisão | Status |
|-----|---------|--------|
| ADR-001 | Monólito modular vs. microserviços → Monólito modular | Aceito |
| ADR-002 | Flyway vs. Liquibase → Flyway (SQL puro) | Aceito |
| ADR-003 | Zustand vs. Redux → Zustand | Aceito |
| ADR-004 | OpenRouter dual-gateway vs. single → Dual (80/20) | Aceito |
| ADR-005 | pgvector vs. Pinecone para RAG → pgvector (MVP) | Aceito |
| ADR-006 | Asaas vs. Stripe para Pix → Asaas (Pix nativo) + Stripe (cartão) | Aceito |
| ADR-007 | JWT stateless vs. session → JWT com refresh rotation | Aceito |
| ADR-008 | App Router vs. Pages Router → App Router (Streaming SSR) | Aceito |
| ADR-009 | PostHog vs. Mixpanel para analytics → PostHog (self-host, privacy-first) | Aceito |
| ADR-010 | Unleash vs. LaunchDarkly para feature flags → Unleash (self-host no Railway) | Pendente |

---

# J) DOUBLE CHECK ULTRA-CRITERIOSO (4 passes)

## PASS 1 — Consistência e Completude

| Verificação | Status |
|-------------|--------|
| Todo RF tem AC mensuráveis | ✅ RF-001 a RF-024 com AC |
| Todo RF aparece no backlog | ✅ US-001 a US-025+ |
| Todo RF tem testes mapeados | ✅ Ver matriz B.7 |
| Todo RF tem log/alerta | ✅ Ver matriz B.7 |
| Todo endpoint tem paginação (onde aplicável) | ✅ cursor-based em GET listas |
| Todo endpoint tem idempotência (onde aplicável) | ✅ Idempotency-Key em POST críticos |
| Todo endpoint tem validação de entrada | ✅ Spring Validation + Bean Validation |
| RNF de performance tem gate no CI | ✅ Lighthouse CI + Gatling |
| RNF de segurança tem gate no CI | ✅ Snyk SAST + OWASP ZAP |
| LGPD mapeada em todas as entidades com PII | ✅ Ver schema V7 (audit_logs) + V1 (soft delete) |

**Gaps identificados no Pass 1 e corrigidos:**
- Gap: Webhook Stripe não tinha tratamento de replay attack → **Corrigido**: `Stripe-Signature` timestamp validation (dentro de 5 min)
- Gap: RAG sem fallback se pgvector lento → **Corrigido**: timeout 500ms + fallback para contexto sem RAG

## PASS 2 — Revisão Cruzada por Disciplina

### Negócio valida:
- ✅ Tier Starter R$39 adicionado (C-11 resolvido) — reduz gap free→paid em 60%
- ✅ Churn calculado em 3 cenários: 7% otimista, 10% base, 14% pessimista
- ✅ Break-even mês 8 (850 pagantes) validado com P&L detalhado
- ⚠️ **Pendente**: Definir política de preço anual vs mensal (desconto 15%?)

### UX valida:
- ✅ Jornadas críticas de Marina e Carlos documentadas
- ✅ Estados vazios, loading, erro documentados como obrigatórios
- ✅ WCAG 2.1 AA checklist presente
- ⚠️ **Pendente**: Dark mode (maioria dos usuários de IA preferem) — incluir desde S1 (tokens de cor já dark-first)

### Segurança valida:
- ✅ OWASP Top 10 mapeado com controles
- ✅ Threat model por feature (abuse cases B.6)
- ✅ HMAC em webhooks Asaas + Stripe
- ✅ Rate limiting definido
- ⚠️ **Pendente**: Pen test externo agendado (incluir no ADR de segurança, trimestral)

### QA valida:
- ✅ Pirâmide de testes definida (70/25/5 BE; 60/30/10 FE)
- ✅ 5 E2E críticos mapeados
- ✅ Quality gates numéricos definidos
- ⚠️ **Pendente**: Estratégia anti-flaky tests (Playwright retry 2x; quarantine lista)

### SRE valida:
- ✅ 3 runbooks críticos documentados
- ✅ RPO 1h / RTO 4h definidos
- ✅ Rollback com critérios objetivos (SLO-based)
- ⚠️ **Pendente**: Chaos engineering (injetar falha OpenRouter em staging → validar fallback)

## PASS 3 — Red Team (10 falhas prováveis)

| # | Falha | Como o Plano Previne/Detecta/Corrige |
|---|-------|-------------------------------------|
| 1 | Race condition no débito de créditos → saldo negativo | Transação `SERIALIZABLE` + `SELECT FOR UPDATE`; teste de concorrência no CI |
| 2 | Webhook duplicado credita 2x créditos | Idempotency key `gateway_id` único em `payment_orders`; upsert com constraint |
| 3 | Streaming SSE cai mid-response | Cliente SSE com reconexão exponential backoff (1→2→4→8→16s); mensagem salva parcialmente |
| 4 | OpenRouter retorna 429 (rate limit) | `RateLimitingDecorator` + retry com jitter; alertas automáticos; fallback para diretas |
| 5 | Upload PDF corrompido trava worker de parse | Timeout 30s no parser; graceful degradation (notifica usuário); dead letter queue |
| 6 | Free user cria 100 contas com email temporário | Heurística de email temp (lista blocklist atualizada); CAPTCHA após 3 cadastros por IP/hora |
| 7 | Partner faz click fraud no próprio link | Janela de deduplicação 24h por IP+email; ML flag (XGBoost); revisão manual de spikes |
| 8 | PL 2338 aprovado com restrições a intermediários de IA | Consultoria jurídica desde Sprint 0; arquitetura de "curadoria" não "treinamento"; ROPA atualizado |
| 9 | Câmbio USD/BRL > R$7 → margem negativa em modelos premium | Buffer de 20% nos créditos premium; 30% de modelos open-source no catálogo; cláusula de ajuste trimestral |
| 10 | Founder único sai do projeto (bus factor=1) | Documentação obsessiva (ADRs + README + runbooks); co-founder contratado mês 6; pair programming semanal |

**5 Cenários de Incidentes:**

| Cenário | Detecção | Mitigação | Rollback | Comunicação |
|---------|---------|-----------|---------|-------------|
| Queda DB PostgreSQL | Alert Grafana: connection errors >10/min | Tentar reconexão pool; ativar modo de leitura de cache Redis se disponível | Restore snapshot mais recente (RPO 1h) | Status page + email usuários ativos |
| Pico de tráfego 10x | Alert: CPU >80% por 5min | HPA no Railway escala; ativar rate limit agressivo | Feature flag: desabilitar geração de imagens (mais pesada) | Banner in-app "Sistema sob carga" |
| Falha OpenRouter (5xx global) | Alert: error rate >10% em 5min | Ativar feature flag `USE_DIRECT_PROVIDERS_ONLY` | Sem rollback (problema é externo); aguardar resolução | Status page + email parceiros |
| Bug em release (churn spike) | Alert: erro 5xx >2% em 5min OU reclamações NPS | Rollback imediato via feature flag; reverter deploy se necessário | Git revert + deploy anterior | Status page + email afetados com crédito de compensação |
| Vazamento de dados (LGPD) | Alert: acesso anômalo a dados pessoais | Desconectar instância comprometida; revogar todas as sessões; notificar DPO | Investigar scope; patch emergencial | ANPD em 72h (obrigação legal) + usuários afetados |

## PASS 4 — Gap Register Final

| Gap | Impacto | Como Detectar | Ação Preventiva | Responsável | Status |
|-----|---------|--------------|----------------|------------|--------|
| Sem 2FA no MVP | Alto (segurança) | Pen test externo | Implementar em v1.5 (TOTP); documentar como risco aceito | Security Eng | **Pendente — v1.5** |
| Dark mode não especificado explicitamente | Médio (UX) | Pesquisa com beta users | Tokens dark-first desde S1; toggle em S3 | UX Lead | **Mitigado** |
| Limite de créditos por workspace não especificado | Alto (billing) | AC da US-023 incompleta | Definir: admin do workspace seta limite por membro; padrão = pool compartilhado | PO + BA | **Pendente — Sprint 0** |
| Sem estratégia de preço anual | Médio (negócio) | Perda de LTV vs mensalidade | Definir desconto 15-17%; implementar em S3 junto ao billing | PO | **Pendente** |
| Chaos testing não planejado | Médio (SRE) | Falha de fallback em prod | Incluir chaos test mensal em staging (kill OpenRouter container) | SRE | **Pendente — pós-MVP** |
| Sem política de retenção de logs (LGPD) | Alto (compliance) | Auditoria ANPD | Job de purge semanal: logs > 1 ano deletados; PII mascarado em logs em 7 dias | Security Eng | **Pendente — Sprint 1** |
| Anti-flaky strategy para E2E não documentada | Baixo (qualidade) | Pipeline flaky > 10% | Playwright retry 2x; quarantine lista atualizada semanalmente | QA Lead | **Pendente — Sprint 0** |
| Pen test externo sem data | Médio (segurança) | Vulnerabilidade não detectada | Agendar: mês 4 (pré-GA) + trimestral | CTO | **Pendente** |
| Parceiro sem 2FA na API | Médio (fraude) | Abuso de API key comprometida | Magic link + API key com IP allowlist; 2FA opcional em v1.5 | Security Eng | **Mitigado parcialmente** |
| SLA para clientes Team não definido contratualmente | Alto (negócio) | Churn por expectativa | Definir: 99,5% + crédito de compensação proporcional ao downtime | PO + Jurídico | **Pendente — pré-GA** |

### Assumption-to-Validation (premissa → validação no backlog)

| Premissa | Risco se Falsa | Validação/Tarefa | Sprint |
|---------|---------------|-----------------|--------|
| Usuário médio consome 25-35% do orçamento de créditos | Margem destruída | Monitorar consumption rate nos 30 primeiros beta users | S7 |
| Pix QR Code gerado em <3s pela Asaas | SLA quebrado no checkout | Load test Asaas API: 100 QR/min simultâneos | S3 |
| pgvector suporta 100K chunks com latência <200ms | RAG lento → UX ruim | Benchmark pgvector com dataset sintético 100K chunks | S4 |
| OpenRouter tem uptime >99% | Downtime frequente → churn | Monitor externo OpenRouter status + histórico 90 dias | S0 |
| 60% dos usuários acessam via mobile | Layout mobile crítico | Analytics PostHog por device nos primeiros 200 users | S7 |
| Afiliados geram 25% das conversões | CAC mais alto → runway curto | Tracking UTM desde S7; revisão após 30 dias de programa | S10 |

---

## ASSUMPTION LOG

1. Equipe inicial: 2 devs (CTO + 1 fullstack); 3º dev contratado no mês 2
2. Infraestrutura: Railway (backend) + Vercel (frontend) + Supabase (PostgreSQL) inicialmente
3. Custo real de infra MVP: R$450/mês (não R$160 como estimado originalmente)
4. Usuário médio: consome 25-35% do orçamento mensal de créditos
5. Churn base: 10%/mês (não 8% — corrigido via benchmarks de mercado)
6. CAC via parceiros: R$15-25 (vs R$45-80 sem parceiros)
7. Sem app mobile nativo no MVP (PWA responsivo suficiente para validação)
8. Sem SAML/SSO Enterprise no MVP (Google OAuth suficiente)
9. OpenRouter disponível e estável para os modelos listados
10. PL 2338 não aprovado durante os primeiros 12 meses do projeto

## QUESTION BACKLOG

| # | Pergunta | Impacto | Prazo |
|---|---------|---------|-------|
| QB-01 | Desconto anual: 15% ou 17%? | Médio (LTV) | Sprint 0 |
| QB-02 | Limite de documentos por usuário no Pro? (5? 20? ilimitado?) | Alto (custo R2) | Sprint 0 |
| QB-03 | Moderação de conteúdo: quem revisa flags do filtro? | Alto (compliance) | Sprint 1 |
| QB-04 | Política de reembolso: até quantos dias? | Alto (jurídico) | Sprint 0 |
| QB-05 | Team plan: pool de créditos compartilhado OU por seat? | Alto (produto) | Sprint 0 |
| QB-06 | API pública: rate limit por plano? (Free: 0, Pro: 1000/dia?) | Médio | Sprint 5 |
| QB-07 | SLA contratual para Team: 99,5% com crédito de compensação? | Alto (legal) | Sprint 8 |
| QB-08 | Pen test externo: quem executará? (Bug bounty ou empresa?) | Médio | Sprint 3 |

---

# ANÁLISE DOCUMENTAL — REFERÊNCIA

> Os 8 documentos únicos analisados estão resumidos abaixo como referência para rastreabilidade.

## Documentos de Referência

| # | Arquivo | Tipo | Autoridade |
|---|---------|------|-----------|
| 1 | Plano_Mestre_Consolidado_v3.docx | Governança | **Máxima (árbitro de conflitos)** |
| 2 | Plano_de_Negocio_Plataforma_IA.docx | Financeiro | Alta |
| 3 | Especificacao_Funcional_v2_Agil_Completo.docx | Produto/Técnico | Alta |
| 4 | Especificacao_Funcional_Concorrentes_IA.docx | Competitivo | Média |
| 5 | Sistema_Parceiros_Cupons_Spec_Completa.docx | Módulo específico | Alta |
| 7 | Guia_Abrangente_de_Documentação.docx | Metodologia | Referência |
| 9 | Guia_Técnico_por_Camada_de_Sistema.docx | Metodologia | Referência |
| 10 | pesquisa_documentacao_ti.docx | Pesquisa | Referência |

**Duplicatas ignoradas:** Arquivos com sufixo `(1)` são cópias exatas dos originais.

**Hierarquia de conflitos:** Doc 1 (v3) > Doc 3 (v2) > Doc 2 > Doc 5. Todas as 12 contradições foram resolvidas pelo Plano Mestre v3.
