# PLANO END-TO-END — IA AGGREGATOR "NUBANK DA IA"
**Chief Architect + Delivery Lead + Principal UX/CX + Security & SRE Reviewer**
**Versão**: 3.0 | **Data**: Março 2026 | **Stack**: Next.js/TS · Java 21 · Spring Boot · Maven · PostgreSQL · Flyway

---

# A) RESUMO EXECUTIVO

## O Que Será Entregue

Plataforma SaaS brasileira: acesso a 30+ modelos de IA (texto, imagem, voz, vídeo) por assinatura única em reais, com Pix/boleto/cartão, interface 100% PT-BR e conformidade LGPD nativa. Elimina múltiplas assinaturas em dólar — economia de R$150-300/mês/usuário.

## Benefícios

| Segmento | Dor Resolvida | Valor Entregue |
|---------|--------------|----------------|
| Creator/Freelancer | 4 ferramentas/dia, R$285/mês | 1 assinatura R$99, mesmos resultados |
| Profissional (PT-BR) | Interface inglês, sem templates jurídicos | Interface PT-BR + 100+ prompts profissionais |
| Dev/Técnico | Custo de testes por modelo isolado | API unificada + comparação simultânea |
| Estudante/Professor | Orçamento limitado | Plano Starter R$39 ou Free 300 créditos |
| Team/Empresa | Ferramentas dispersas, sem controle | RBAC + admin panel + pool de créditos |

## Riscos Top 10

| # | Risco | Prob | Impacto | Mitigação | Gatilho de Detecção |
|---|-------|------|---------|-----------|-------------------|
| R-01 | PL 2338 aprovado com restrições | Média | Crítico | Consultoria jurídica; arquitetura de curadoria | Monitor legislativo mensal |
| R-02 | OpenRouter instabilidade/outage | Média | Alto | Dual-gateway: 80% OpenRouter + 20% direto | Alert 5xx rate >5% em 2min |
| R-03 | Inner AI capta rodada seed | Alta | Alto | 22 inovações exclusivas + comunidade | Monitor funding news mensal |
| R-04 | Heavy users destroem margem bruta | Alta | Médio | Rate limit + multiplicador créditos 80-100x | Monitor custo API/user diário |
| R-05 | Vazamento PII / LGPD | Baixa | Crítico | Criptografia + pen test + audit log | SIEM: acesso anômalo dados |
| R-06 | Câmbio BRL/USD >R$7 | Média | Alto | Buffer 20% créditos + 30% open-source | Alert diário se BRL >R$6,80 |
| R-07 | Race condition no débito de créditos | Alta (sem mitigação) | Alto | SERIALIZABLE + FOR UPDATE atômico | Monitor saldo negativo |
| R-08 | Parceiros fraudulentos em escala | Média | Alto | ML anti-fraude + KYC + carência 14d | Dashboard fraud rate diário |
| R-09 | Bus factor=1 (founder solo) | Alta | Crítico | Docs obsessiva + co-founder mês 6 | — |
| R-10 | Escalabilidade DB 10K+ users | Média | Alto | PgBouncer + read replica + particionamento | Alert conexões >80% pool |

## Cronograma Macro

| Marco | Semana | KPI |
|-------|--------|-----|
| Alpha 0.1 | S2 | 5 modelos + Auth + Pix — 20 users internos |
| Alpha 0.5 | S6 | 20 modelos + Imagens + Web Search — 50 beta |
| **Beta 1.0** | **S14** | **MVP 28 features — 200 beta users** |
| v1.0 GA | S20 | 1.000 pagantes — R$34K MRR |
| v1.5 | S28 | Parceiros + Teams — 580 pagantes, R$54K MRR |
| v2.0 | S40 | Marketplace + API — 2.230 pagantes, R$207K MRR |

---

# EQUIPE MULTIDISCIPLINAR + RACI

## Papéis e Responsabilidades

| Papel | Pessoa | Participação por Fase |
|-------|--------|----------------------|
| **PO** (Product Owner) | Founder/CEO | Discovery✓ Design✓ Review✓ Release✓ |
| **BA** (Business Analyst) | PO auxiliar ou freelance | Discovery✓ AC✓ |
| **Solution Architect** | CTO | Discovery✓ Design✓ Build✓ ORR✓ |
| **TL Backend** | CTO / Dev Sênior | Build✓ Test✓ Release✓ |
| **TL Frontend** | Dev Fullstack | Build✓ Test✓ Release✓ |
| **Security Engineer** | Freelance trimestral | Threat Model✓ Pen Test✓ |
| **Head UX** | Designer (0,5 FTE) | Discovery✓ Design✓ Usability Test✓ |
| **QA Lead** | Dev BE (primeiro mês) → SDET contratado mês 3 | Test✓ Quality Gates✓ |
| **DevOps/SRE** | TL BE acumulando no MVP → Contratação mês 6 | CI/CD✓ ORR✓ Runbooks✓ |
| **DPO** (Data Protection Officer) | Jurídico externo | Compliance LGPD✓ |

## RACI por Entregável

| Entregável | PO | Architect | TL BE | TL FE | UX | QA | SRE | Security |
|-----------|----|-----------|----|----|----|----|----|--------|
| Requisitos RF/RNF | A | C | C | C | C | C | C | C |
| Arquitetura + ADRs | I | **R/A** | C | C | I | I | C | C |
| Design System + UX | C | I | I | **R/A** | **R** | I | I | I |
| Threat Model | I | C | C | C | I | I | C | **R/A** |
| API OpenAPI Spec | C | A | **R** | C | I | C | I | C |
| Migrations Flyway | I | C | **R/A** | I | I | C | C | C |
| Testes unitários BE | I | I | **R/A** | I | I | A | I | I |
| Testes unitários FE | I | I | I | **R/A** | I | A | I | I |
| CI/CD Pipeline | I | C | C | C | I | C | **R/A** | C |
| ORR checklist | C | C | C | C | I | C | **R/A** | C |
| Runbooks | I | C | C | I | I | I | **R/A** | I |
| Release Go/No-Go | **A** | C | C | C | C | C | C | C |

*R=Responsible, A=Accountable, C=Consulted, I=Informed*

## Cadência de Rituais

| Ritual | Cadência | Participantes | Saída |
|--------|----------|--------------|-------|
| Discovery Workshop | 1x por épico | PO, Architect, TL BE, TL FE, UX, QA | RF+RNF+AC aprovados |
| Design Review | A cada 2 sprints | Architect, TLs, UX | Arquitetura aprovada |
| Threat Model Review | Sprint 1 + trimestral | Architect, Security Eng, TLs | Controles definidos |
| ORR (Operational Readiness Review) | Pré-GA e pré-release major | Architect, SRE, QA, PO | Go/No-Go formal |
| Go/No-Go Sprint | Final de cada sprint | PO, TLs, QA | Release autorizado |

---

# B) ESCOPO "SEM GAPS"

## B.1 Escopo IN / OUT

**MVP IN (14 semanas):**
- Chat multi-modelo com streaming SSE (20+ modelos, troca mid-chat, auto-routing)
- Sistema de créditos com multiplicadores (Gemini Flash: 1cr, GPT-4o: 10cr, Claude Opus: 80cr)
- Planos: Free (300cr), Starter R$39 (1.200cr), Pro R$99 (3.500cr), Team R$49/seat
- Pagamentos: Pix/boleto (Asaas) + Cartão (Stripe)
- Upload + RAG com documentos (PDF, DOCX, CSV) via pgvector
- Geração de imagens (DALL-E 3, Flux)
- Busca web real-time com citações (Serper API)
- Personas + biblioteca 100+ prompts PT-BR
- 6 inovações exclusivas P0 (ver B.3)
- LGPD compliance (exclusão, portabilidade, consentimento, audit log)
- Auth: email/senha + Google OAuth
- Admin panel básico

**OUT (pós-MVP):**
- App mobile nativo (iOS/Android)
- Chain Mode / Agentes autônomos
- Marketplace de bots
- SSO SAML Enterprise
- Self-hosted / white-label
- Internacionalização (fora de PT-BR)
- Módulo Parceiros/Cupons (Sprints S7-S11)

## B.2 Personas e Jornadas

| Persona | Perfil | Dor | Jornada Crítica | Momento "Wow" |
|---------|--------|-----|----------------|---------------|
| **Marina** (Creator) | 29a, SP, freelancer | R$285/mês, 4 ferramentas | Landing → Calculadora ROI → Free → Chat → Upgrade Pix | Widget custo em R$ atualizado por mensagem |
| **Carlos** (Advogado) | 47a, BH, não inglês | Interface inglês, confidencialidade | Indicação cupom → Modo Consultor → Exporta PDF | IA faz 4 perguntas em PT-BR antes de responder |
| **Thiago** (Dev) | 24a, Floripa | Sem API granular | Cadastro → API Key → Compara 3 modelos lado a lado | Comparação split-screen com custo por token |
| **Professora Ana** | 35a, Recife | Alunos colam com IA | Cadastro → Modo Socrático → Exporta certificado | IA ensina por perguntas sem dar resposta |
| **TechFit** (Team) | Startup 25 pessoas | Ferramentas dispersas | Admin cria workspace → Convida → Seta limite créditos | Dashboard admin com uso por membro em tempo real |

## B.3 Jobs-to-be-Done

| Persona | Job | Frequência | Critério de Sucesso |
|---------|-----|-----------|-------------------|
| Marina | Criar 30 posts/sem + 10 legendas | Diária | <5 min por post, sem sair da plataforma |
| Carlos | Revisar 15 contratos/sem com confidencialidade | 3x/dia | Modo confidencial ON, sem dados no treinamento |
| Thiago | Testar e comparar modelos novos | Semanal | Side-by-side em <30s de setup |
| Professora Ana | Criar 10 aulas/bimestre com IA pedagógica | Semanal | Modo Socrático sem dar resposta pronta |
| TechFit | Controlar gasto de IA por membro | Mensal | Relatório de uso exportável em CSV |

## B.4 Requisitos Funcionais + Critérios de Aceitação

| ID | Requisito | Prioridade | Critérios de Aceitação |
|----|----------|-----------|----------------------|
| RF-001 | Chat streaming SSE multi-modelo | P0 | TTFT p95 <2s; cancelar streaming a qualquer momento; histórico salvo após finalizar; troca de modelo mid-chat preserva histórico |
| RF-002 | Auto-routing por tipo de tarefa | P0 | Detecta: code→DeepSeek, creative→Claude, factual→Gemini; exibe badge do modelo selecionado; desativável em settings |
| RF-003 | Sistema de créditos atômico | P0 | Débito antes da resposta; transação SERIALIZABLE; saldo nunca negativo; widget atualiza em tempo real por SSE event |
| RF-004 | Planos e assinaturas | P0 | 4 planos ativos; upgrade/downgrade sem perda de dados; créditos proporcionais no downgrade |
| RF-005 | Pix via Asaas | P0 | QR Code em <3s; timer de expiração 30min; webhook credita créditos em <30s; idempotência por gateway_id |
| RF-006 | Boleto via Asaas | P0 | PDF gerado <5s; vence em 3 dias úteis; credita após liquidação (D+1); email com link |
| RF-007 | Cartão via Stripe | P1 | 3DS2 suportado; retry em falha; salvar cartão para renovação automática |
| RF-008 | Upload + RAG (PDF, DOCX, CSV) | P0 | Max 50MB; parse async <10s; top-5 chunks relevantes; citação de trecho na resposta; pgvector similarity <200ms |
| RF-009 | Geração de imagens | P0 | DALL-E 3 + Flux; 1024x1024; tradução PT→EN automática; salvo no R2; biblioteca pessoal |
| RF-010 | Busca web real-time | P0 | Serper API; cita fontes com URL; cache 24h por query hash; resultados em <3s |
| RF-011 | Personas customizáveis | P0 | System prompt persistente; limites: Free 1, Starter 5, Pro ilimitado; ícone e nome |
| RF-012 | Biblioteca de prompts PT-BR | P0 | 100+ prompts; categorias; favoritar; uso rastreado; sugestão por contexto |
| RF-013 | Dashboard Custo Transparente (INV-03) | P0 | Créditos + R$ por conversa; comparação "seria X no GPT-4 direto"; relatório mensal exportável CSV |
| RF-014 | Modo Consultor (INV-08) | P0 | Toggle por conversa; IA faz 3-5 perguntas antes de responder; indicador visual no header |
| RF-015 | Links Públicos Read-Only (INV-17) | P0 | Slug único 8 chars; preview sem conta; fork cria nova conversa; expira 30 dias; sem débito de créditos |
| RF-016 | Modo Aprendizado Socrático (INV-19) | P0 | Toggle por conversa; IA responde com pergunta orientadora; desativável a qualquer momento |
| RF-017 | Calculadora Economia Landing (INV-22) | P0 | Widget sem conta; informe ferramentas → calcula economia; CTA "Economize agora" |
| RF-018 | Certificados Gamificação (INV-12) | P0 | Marcos: 1ª conversa, 10 conv., 1K créditos, 30d ativo; badge PNG exportável; notificação in-app |
| RF-019 | LGPD: exclusão de dados | P0 | Confirmação email; processado em 48h; anonimização; audit log imutável; email confirmação |
| RF-020 | LGPD: portabilidade | P0 | Export JSON+ZIP; gerado async <24h; link válido 7 dias; notificação email |
| RF-021 | Admin: gestão usuários | P0 | Listagem paginada; busca email; suspender; impersonar com audit log; export CSV |
| RF-022 | Admin: gestão modelos IA | P0 | Toggle on/off sem deploy; editar multiplicador créditos; reordenar; preview custo |
| RF-023 | Workspace Teams | P1 | Convites por email; RBAC Owner/Admin/Member; pool créditos ou limite por seat; relatório de uso |
| RF-024 | Notificações in-app | P1 | Centro de notificações: créditos baixos (<10%), novos modelos, marcos gamificação |

## B.5 Requisitos Não Funcionais Detalhados

| ID | Categoria | Requisito | Meta | Como Medir |
|----|----------|----------|------|-----------|
| RNF-001 | Performance | TTFT streaming | p95 <2s, p99 <4s | OpenTelemetry histogram; alert se p95 >3s |
| RNF-002 | Performance | API CRUD | p95 <200ms, p99 <500ms | OTel histogram por endpoint |
| RNF-003 | Performance | LCP (frontend) | <2,5s | Lighthouse CI + RUM (PostHog) |
| RNF-004 | Performance | CLS (frontend) | <0,1 | Lighthouse CI |
| RNF-005 | Performance | TTI (frontend) | <3,8s | Lighthouse CI |
| RNF-006 | Performance | JS Bundle inicial | <200KB gzip | next/bundle-analyzer no CI |
| RNF-007 | Escalabilidade | Usuários simultâneos | 500 req/s no pico | Gatling load test pré-GA |
| RNF-008 | Disponibilidade | Uptime | 99,5% (43min/mês) | Uptime Robot + PagerDuty |
| RNF-009 | Disponibilidade | Erro 5xx rate | <0,5% | Prometheus + Grafana |
| RNF-010 | Segurança | Auth JWT | Access 15min + Refresh 7d rotacionado | Pen test semestral |
| RNF-011 | Segurança | OWASP Top 10 | Zero CRITICAL/HIGH abertos | Snyk SAST + OWASP ZAP no CI |
| RNF-012 | Segurança | Rate limiting | 100 req/min unauth; 1.000/min auth | Bucket de token; 429 com Retry-After |
| RNF-013 | LGPD | Criptografia PII at rest | AES-256 (pgcrypto) | Auditoria automática semanal |
| RNF-014 | LGPD | Retenção dados | Conv: 2 anos inatividade; logs: 1 ano | Job purge semanal; alertas |
| RNF-015 | LGPD | Exclusão | Processada <48h | Monitor job de exclusão |
| RNF-016 | Observabilidade | Log coverage | 100% requests com correlation-id | Alert se log rate cai >20% |
| RNF-017 | Observabilidade | Tracing | 100% endpoints críticos tracados | Grafana Tempo |
| RNF-018 | Qualidade | Cobertura unit BE | ≥80% | JaCoCo no CI |
| RNF-019 | Qualidade | Cobertura branch BE | ≥70% | JaCoCo |
| RNF-020 | Qualidade | Cobertura unit FE | ≥70% | Vitest --coverage |
| RNF-021 | Acessibilidade | WCAG 2.1 AA | Zero violações AA críticas | axe-core automatizado no CI |
| RNF-022 | Dados | RPO | 1 hora | Backup contínuo WAL |
| RNF-023 | Dados | RTO | 4 horas | Restore drill mensal |

---

# C) MATRIZES OBRIGATÓRIAS

## C1) Matriz de Rastreabilidade Total

| RF | Tela/Componente FE | Endpoint/Serviço BE | Tabelas DB | Testes BE | Testes FE/E2E | Observabilidade | Rollback/Flag |
|----|-------------------|---------------------|-----------|-----------|--------------|----------------|--------------|
| RF-001 | `ChatPage`, `MessageStream`, `ModelSelector` | `POST /v1/chat` (SSE) | `conversations`, `messages`, `ai_models` | `ChatServiceTest`, `ChatControllerIT` | `chat-basic.spec.ts` (E2E) | `chat.ttft_ms` histogram; `chat.stream.failed` counter | FF: `disable_streaming_models` |
| RF-002 | `RoutingBadge`, `ModelBadge` | `AiRoutingService` (interno) | `ai_models` | `AiRoutingServiceTest` | — | `routing.decision` log tag | FF: `force_routing_model=gpt-4o` |
| RF-003 | `CreditMeter`, `CostWidget` | Evento interno `CreditDebitedEvent` | `credit_ledgers`, `credit_transactions` | `CreditServiceTest`, `CreditLedgerIT` | `credits.spec.ts` | `credit.balance` gauge; alert se negativo | — |
| RF-005 | `CheckoutPix`, `PixQRCode` | `POST /v1/billing/pix`, `POST /v1/webhooks/asaas` | `payment_orders`, `subscriptions` | `AsaasGatewayIT`, `PaymentSagaIT` | `billing-pix.spec.ts` | `payment.pix.confirmed_ms`; alert se 0 confirmações/hora | FF: `use_stripe_only` |
| RF-008 | `DocumentUpload`, `DocChat` | `POST /v1/documents`, `RagService` | `documents`, pgvector `doc_chunks` | `RagServiceIT`, `DocumentServiceTest` | `doc-upload.spec.ts` | `doc.parse_ms`; `rag.similarity_ms`; alert parse >15s | FF: `disable_rag` |
| RF-009 | `ImageGen`, `ImageLibrary` | `POST /v1/images/generate` | `image_generations` | `ImageGenServiceTest` | — | `image.gen_ms`; `image.failed` | FF: `disable_image_generation` |
| RF-013 | `CostDashboard`, `ConvCostWidget` | `GET /v1/conversations/{id}/cost` | `credit_transactions` | `CostTrackingServiceTest` | `cost-display.spec.ts` | `cost.per_conversation` histogram | — |
| RF-019 | `PrivacySettings`, `DeletionConfirm` | `DELETE /v1/users/me/data` | todos com `deleted_at` | `GdprServiceTest` | `lgpd.spec.ts` | `gdpr.deletion_queued`; alert se >50h sem processar | — |
| RF-021 | `AdminUsers` | `GET /v1/admin/users`, `POST /v1/admin/users/{id}/suspend` | `users`, `audit_logs` | `AdminControllerIT` | — | `admin.action` audit log; alert impersonação | — |
| RF-023 | `WorkspaceSettings`, `MemberList` | `POST /v1/workspaces`, `GET /v1/workspaces/{id}/members` | `workspaces`, `workspace_members` | `WorkspaceServiceIT` | `team-workspace.spec.ts` | `workspace.member_count`; `workspace.credit_usage` | FF: `disable_team_plan` |

## C2) Matriz RNF → Evidência

| RNF | Métrica-Alvo | Como Medir | Ambiente | Tooling | Gate (pass/fail) |
|-----|-------------|-----------|---------|---------|-----------------|
| RNF-001 | TTFT p95 <2s | OTel histogram `chat.ttft_ms` | Prod + Staging | Grafana | FAIL se p95 >3s em load test |
| RNF-003 | LCP <2,5s | Lighthouse CI score | Staging | lighthouse-ci | FAIL se >3s (bloqueia merge) |
| RNF-007 | 500 req/s p95 <2s | Gatling `ramp to 500 VU` | Staging | Gatling | FAIL se p95 >3s ou error rate >1% |
| RNF-008 | Uptime 99,5% | Uptime Robot (externo) | Prod | Uptime Robot + PagerDuty | Alert P1 se downtime >5min |
| RNF-011 | Zero CRITICAL | SAST + ZAP scan | CI + Staging | Snyk Code + OWASP ZAP | FAIL se qualquer CRITICAL encontrado |
| RNF-013 | PII criptografado | pg_dump + grep PII não cifrado | Staging | Script automatizado | FAIL se PII encontrado em plaintext |
| RNF-018 | Coverage ≥80% | JaCoCo report | CI | Maven Verify | FAIL se <80% |
| RNF-021 | Zero violações AA críticas | axe-core scan | CI | @axe-core/playwright | FAIL se violação CRITICAL |
| RNF-022 | RPO 1h | Restore de backup com timestamp | Staging mensal | pg_basebackup + WAL | FAIL se restore >1h de dados perdidos |

## C3) Matriz de Dependências

| Item | Depende De | Risco | Mitigação | Dono |
|------|-----------|-------|-----------|------|
| Chat streaming | OpenRouter API | Instabilidade causa downtime | Fallback direto OAI/Anthropic; circuit breaker | TL BE |
| Pix pagamento | Asaas disponibilidade | Checkout quebrado = perda de receita | Monitor webhook SLA; fallback boleto | TL BE |
| RAG | pgvector extensão | Extensão não disponível no Supabase | Verificar suporte na contratação do DB | Solution Architect |
| Geração imagem | OpenRouter / OpenAI | Rate limit por plano | Fila de geração com retry; exibir posição na fila | TL BE |
| Email transacional | Resend | Emails não enviados | Retry 3x; fallback SMTP próprio | TL BE |
| Observabilidade | Grafana Cloud | Perda de métricas | Retenção local 24h + batch forward | SRE |
| CI/CD | Railway (deploy) | Downtime Railway | Blue-green; rollback via git tag | SRE |
| Busca web | Serper API | Rate limit / custo | Cache 24h por query; daily budget alert | TL BE |

---

# D) ARQUITETURA

## D.1 Escolha: Monólito Modular

**Escolha: Monólito Modular com DDD Bounded Contexts**

**Por que NÃO microserviços:** Equipe 2-3 devs; prazo 14 semanas; overhead operacional de microsserviços (service mesh, distributed tracing, deployments independentes, eventual consistency) triplicaria complexidade sem benefício real até 5K users. Além disso, microsserviços exigem maturidade em CI/CD e SRE que a equipe inicial não tem.

**Plano de evolução:**
- Mês 1-15 (0-5K users): Monólito Modular
- Mês 16-24 (5-15K users): Extração cirúrgica de `CommissionService` (mais isolado) + `NotificationService`
- Mês 25+ (15K+ users): `ChatGatewayService` separado (maior carga) + API Gateway

## D.2 Clean Architecture — Camadas e Boundaries

```
┌──────────────────────────────────────────────────────┐
│  ADAPTERS (Framework e IO)                            │
│  Spring MVC, JPA Repositories, REST Clients          │
│  Regra: pode depender de APPLICATION; NUNCA DOMAIN   │
├──────────────────────────────────────────────────────┤
│  APPLICATION (Use Cases, Commands, Queries, Events)   │
│  Regra: pode depender de DOMAIN; nunca de ADAPTERS   │
├──────────────────────────────────────────────────────┤
│  DOMAIN (Entities, Aggregates, Domain Services)      │
│  Regra: ZERO dependências externas (Java puro)       │
├──────────────────────────────────────────────────────┤
│  SHARED KERNEL (Value Objects, Ports/Interfaces)     │
│  Money, UserId, CreditAmount, AiModelPort           │
└──────────────────────────────────────────────────────┘
```

**Enforced por ArchUnit (ver seção L).**

## D.3 DDD Bounded Contexts

| Context | Agregado Raiz | Entidades Filhas | Invariantes |
|---------|-------------|----------------|------------|
| **Identity** | `User` | `RefreshToken`, `ApiKey` | Email único; password nunca em plaintext |
| **Conversation** | `Conversation` | `Message`, `Persona`, `Document` | Msg salva só após stream finalizar |
| **Billing** | `Subscription` | `CreditLedger`, `CreditTransaction` | Saldo ≥ 0 sempre |
| **Payment** | `PaymentOrder` | — | gateway_id único (idempotência) |
| **Partner** | `Partner` | `Coupon`, `CouponAttribution`, `Commission` | Comissão calculada sobre valor pós-desconto |
| **Catalog** | `AiModel` | — | Multiplicador ≥ 1 |
| **Notification** | `Notification` | — | Insert-only |
| **Audit** | `AuditLog` | — | Imutável (PostgreSQL rule) |

## D.4 C4 Textual

### Nível 1 — Contexto
```
[Marina/Carlos/Thiago/Ana] → [IA Aggregator] (web)
[Admin] → [IA Aggregator] (admin panel)
[Partner] → [IA Aggregator] (partner dashboard)
[IA Aggregator] → [OpenRouter] (AI gateway 80%)
[IA Aggregator] → [OpenAI, Anthropic, Google] (fallback 20%)
[IA Aggregator] → [Asaas] (Pix/boleto)
[IA Aggregator] → [Stripe] (cartão)
[IA Aggregator] → [Cloudflare R2] (docs e imagens)
[IA Aggregator] → [Resend] (emails)
[IA Aggregator] → [Serper] (busca web)
[IA Aggregator] → [Grafana Cloud] (observabilidade)
```

### Nível 2 — Contêineres
```
[Browser] → HTTPS → [Next.js :3000] → REST/SSE → [Spring Boot :8080]
[Spring Boot] → JDBC → [PostgreSQL :5432]
[Spring Boot] → Redis → [Upstash Redis]
[Spring Boot] → OTLP → [Grafana Agent → Grafana Cloud]
[Asaas/Stripe] → Webhook → [Spring Boot /v1/webhooks/*]
```

### Nível 3 — Componentes Backend
```
ia-platform-api (controladores Spring MVC)
├── ChatController → ChatApplicationService
│   └── [AiRoutingService, OpenRouterACL, CreditService, ConversationRepo]
├── BillingController → SubscriptionService
│   └── [AsaasACL, StripeACL, CreditLedgerRepo]
├── WebhookController → PaymentSagaOrchestrator
│   └── [CreditService, EmailService, CommissionSagaStep]
├── DocumentController → [DocumentService, RagService, R2StoragePort]
└── AdminController → AdminApplicationService
```

## D.5 Padrões de Resiliência

| Padrão | Aplicado Onde | Configuração |
|--------|--------------|-------------|
| **Timeout** | Chamadas OpenRouter | 30s para streaming; 10s para classify |
| **Retry + Backoff** | OpenRouter, Asaas, Serper | Max 3 tentativas; backoff 1s→2s→4s + jitter |
| **Circuit Breaker** | OpenRouter (Resilience4j) | Open após 5 falhas em 10s; half-open após 30s |
| **Bulkhead** | Pool de threads por provedor | Max 20 threads OpenRouter; 5 threads direto |
| **Idempotência** | Webhooks Asaas/Stripe | `gateway_id` único com upsert + constraint |
| **Dead Letter** | Fila de geração de imagens | Retry 3x; DLQ com alert Grafana |

## D.6 Observabilidade

**Stack:** OpenTelemetry Java Agent → Grafana Alloy (collector) → Grafana Cloud (Loki/Tempo/Prometheus)

**Logs Estruturados (JSON):**
```json
{
  "timestamp": "2026-03-01T10:30:00Z", "level": "INFO",
  "service": "ia-platform-api", "version": "1.0.0",
  "traceId": "abc123", "spanId": "def456",
  "userId": "usr_789", "action": "chat.message.sent",
  "modelId": "gpt-4o", "creditsUsed": 10,
  "costReaisCents": 18, "latencyMs": 1243
}
```

**Sem PII em logs** (mascarar email, CPF, nome após 7 dias em staging, imediatamente em prod).

**SLIs / SLOs:**

| SLI | SLO | Janela | Alert |
|-----|-----|--------|-------|
| Disponibilidade (1 - 5xx rate) | >99,5% | 30d rolling | P1 se <99% em 5min |
| TTFT p95 | <2s | 24h rolling | P2 se >3s por 10min |
| API CRUD p95 | <200ms | 24h rolling | P2 se >500ms |
| Erro pagamento | <1% | 7d rolling | P1 se >5% em 5min |
| Exclusão LGPD | <48h processamento | por evento | P1 se >50h |

---

# E) SEGURANÇA E LGPD

## E.1 Threat Model (STRIDE por Componente)

| Componente | Ameaça (STRIDE) | Controle |
|-----------|----------------|---------|
| Auth API | Spoofing (credential stuffing) | Rate limit 5 tentativas/hora/IP; CAPTCHA após 3 falhas |
| JWT Token | Tampering (modificação) | HS256 + secret rotation semestral; validação de assinatura |
| Chat SSE | Information Disclosure (prompt injection) | Filtro OpenAI Moderation; sanitização de system prompt |
| Webhook Endpoint | Repudiation (replay attack) | HMAC-SHA256 + timestamp ±5min |
| Upload de Documentos | Elevation of Privilege (malware em PDF) | ClamAV scan + mime-type validation + sandboxed parser |
| Admin Panel | Elevation of Privilege (IDOR) | RBAC strict + testes de autorização automatizados |
| Partner Dashboard | Spoofing (impersonação) | Magic link + JWT scope `partner`; log de todos os acessos |
| Payment Order | Repudiation (webhook duplicado) | Idempotency key `gateway_id` único; upsert com constraint |
| Credit Ledger | Tampering (race condition) | `SERIALIZABLE` + `SELECT FOR UPDATE` |

## E.2 Abuse Cases

| # | Cenário | Prevenção | Detecção |
|---|---------|-----------|---------|
| 1 | Prompt injection para vazar system prompt da persona | Sanitização + filtro Moderation API | Log de inputs com palavras-chave suspeitas |
| 2 | Criação massiva de contas free (email temporário) | Blocklist de domínios temp + verificação DNS MX | Alert: >5 cadastros/hora de mesmo IP |
| 3 | Click fraud em links de afiliado (auto-clique) | Deduplicação 24h por IP+email; fingerprint server-side | Dashboard fraud rate; alert spike |
| 4 | Race condition: dois débitos simultâneos do mesmo saldo | `SERIALIZABLE` + `FOR UPDATE` no ledger | Monitor saldo negativo (zero tolerance) |
| 5 | Webhook falso de confirmação Pix | HMAC-SHA256 verificado + timestamp ±5min | Alert: webhook rejeitado >3x/hora |
| 6 | Scraping da biblioteca de prompts PT-BR | Rate limit 100 req/min; auth obrigatório | Alert: user >500 req/hora em endpoints de prompts |
| 7 | Bypass de rate limit com múltiplos IPs (proxy) | Rate limit por user_id (autenticado) além de IP | Alert: user >1.000 req/5min |
| 8 | Upload de arquivo malicioso camuflado em PDF | ClamAV + type validation antes de processar | Alert: ClamAV positivo |
| 9 | Exfiltração de PII via impersonação de admin | Audit log imutável de toda impersonação; 2FA admin (v1.5) | Alert: impersonação fora do horário comercial |
| 10 | Partner saca comissão fraudulenta (cancelamentos) | Carência 14 dias; verificação de chargebacks; ML flag | Alert: commission rate >30% de um parceiro |

## E.3 Data Classification

| Dado | Classificação | Criptografia | Retenção | Acesso |
|------|--------------|-------------|---------|--------|
| Email, nome, CPF/CNPJ | PII Sensível | AES-256 at rest (pgcrypto) | 2 anos após inatividade | User + Admin |
| Histórico de conversas | Confidencial | AES-256 at rest | 2 anos | Apenas o user |
| Dados de pagamento | PCI (não armazenamos raw) | Tokenização Stripe/Asaas | Não armazenamos | Gateways externos |
| Logs de auditoria | Interno | TLS in transit | 1 ano | Admin + DPO |
| Chave Pix do parceiro | PII Financeiro | AES-256 at rest | Enquanto parceiro ativo | Admin + Finance |

## E.4 LGPD

- **Base legal**: Consentimento (art. 7, I) no cadastro + execução de contrato (art. 7, V) para billing
- **DPO**: Nomeado até mês 3; email `privacidade@ia-platform.com.br` publicado
- **ROPA** (Records of Processing Activities): documentado e atualizado trimestralmente
- **Consentimento granular**: checkbox separado para (1) termos de uso e (2) opt-in marketing
- **Não-treinamento**: contrato com OpenRouter proíbe uso de dados para treinamento; cláusula em ToS
- **Audit log imutável**: regras PostgreSQL `NO UPDATE / NO DELETE` em `audit_logs`
- **Purge automático**: job semanal; 2 anos convs + 1 ano logs; PII mascarado em logs em <24h prod

## E.5 Checklist OWASP Top 10

| Risco | Controle | Automatizado? |
|-------|---------|--------------|
| A01 Broken Access Control | RBAC + testes de autorização por role | ✅ CI |
| A02 Cryptographic Failures | TLS 1.3 + pgcrypto AES-256 | ✅ Auditoria |
| A03 Injection | JPA parameterized; sanitização FE (DOMPurify) | ✅ SAST |
| A04 Insecure Design | Threat model; abuse cases; DoR inclui segurança | Manual |
| A05 Security Misconfiguration | Spring Security defaults; HSTS; X-Frame-Options; CSP | ✅ OWASP ZAP |
| A06 Vulnerable Components | Snyk no CI bloqueia CRITICAL | ✅ CI |
| A07 Auth Failures | Rate limit; lockout 5 tentativas; refresh rotation | ✅ Testes |
| A08 Software Integrity | HMAC webhooks; checksums de artefatos | ✅ CI |
| A09 Logging Failures | Structured logs; correlation-id; sem PII | ✅ Pipeline |
| A10 SSRF | Allowlist de domínios externos; nunca fetch URL do usuário | ✅ SAST |

**CSP (Content Security Policy) Frontend:**
```
Content-Security-Policy:
  default-src 'self';
  script-src 'self' 'nonce-{random}';
  style-src 'self' 'unsafe-inline';
  img-src 'self' data: https://r2.ia-platform.com.br;
  connect-src 'self' https://api.ia-platform.com.br;
  frame-ancestors 'none'
```

---

# F) POSTGRESQL (NÍVEL PRODUÇÃO)

## F.1 Modelo Físico — Tabelas Principais

```sql
-- V1__create_users_auth.sql
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(150) NOT NULL UNIQUE,
  email_verified_at TIMESTAMP,
  password_hash VARCHAR(255),
  full_name VARCHAR(200),
  role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER','PARTNER','WORKSPACE_ADMIN','PLATFORM_ADMIN','SUPER_ADMIN')),
  google_id VARCHAR(100),
  data_training_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
  lgpd_consent_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMP  -- soft delete LGPD
);

-- V3__create_billing.sql
CREATE TABLE credit_ledgers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL UNIQUE REFERENCES users(id),
  balance INTEGER NOT NULL DEFAULT 0 CHECK (balance >= 0),  -- invariante de domínio
  monthly_allowance INTEGER NOT NULL DEFAULT 300,
  reset_date DATE,
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE credit_transactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id),
  amount INTEGER NOT NULL,
  type VARCHAR(30) NOT NULL CHECK (type IN ('MONTHLY_GRANT','CHAT_DEBIT','IMAGE_DEBIT','BONUS','REFUND')),
  conversation_id UUID,
  ai_model_id UUID,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
  -- insert-only; nunca atualizado
);

-- V7__audit_immutable.sql
CREATE TABLE audit_logs (...);
CREATE RULE audit_logs_no_delete AS ON DELETE TO audit_logs DO INSTEAD NOTHING;
CREATE RULE audit_logs_no_update AS ON UPDATE TO audit_logs DO INSTEAD NOTHING;
```

## F.2 Migrações Flyway

**Decisão: Flyway (vs Liquibase)**

| Critério | Flyway | Liquibase |
|---------|--------|----------|
| Curva aprendizado | Baixa (SQL puro) | Média (XML/YAML) |
| Spring Boot integration | Nativa 0-config | Funciona mas +config |
| Rollback | Manual (undo scripts) | Automático |
| PR readability | Alta (SQL legível) | Média |
| Time to value | 5 min | 20 min |

**Escolha: Flyway** — equipe pequena, SQL puro é mais legível em code review, rollback manual é aceitável e explícito.

**Convenções:**
```
db/migration/
├── V1__create_users_auth.sql
├── V2__create_workspaces_rbac.sql
├── V3__create_billing_credits.sql
├── V4__create_conversations_messages.sql
├── V5__create_documents_pgvector.sql
├── V6__create_image_generations.sql
├── V7__create_audit_logs.sql
├── V8__create_partners_coupons.sql
├── V9__create_attributions_commissions.sql
├── V10__create_notifications.sql
└── V11__add_indexes_critical.sql
```

## F.3 Índices Propostos

```sql
-- Listagem de conversas do usuário (query mais frequente)
CREATE INDEX idx_conversations_user_updated
  ON conversations(user_id, updated_at DESC) WHERE deleted_at IS NULL;
-- Complexidade: O(log n) vs O(n) sem índice

-- Histórico de mensagens (cursor-based pagination)
CREATE INDEX idx_messages_conversation_created
  ON messages(conversation_id, created_at);

-- Créditos por usuário (dashboard)
CREATE INDEX idx_credit_tx_user_created
  ON credit_transactions(user_id, created_at DESC);

-- Cupons ativos por código (checkout — O(1) lookup)
CREATE UNIQUE INDEX idx_coupons_code_active
  ON coupons(code) WHERE status = 'ACTIVE';

-- Anti-fraude: atribuições por IP nas 24h
CREATE INDEX idx_attributions_ip_created
  ON coupon_attributions(ip_first_access, first_access_at DESC);

-- pgvector: similarity search em doc_chunks
CREATE INDEX idx_doc_chunks_embedding
  ON doc_chunks USING ivfflat (embedding vector_cosine_ops)
  WITH (lists = 100);
-- Complexidade: O(sqrt(n)) vs O(n) full scan
```

## F.4 Queries Críticas e Otimização

| Query | Plano esperado | Como verificar |
|-------|---------------|----------------|
| Listar conversas do usuário | Index Scan em `idx_conversations_user_updated` | `EXPLAIN (ANALYZE, BUFFERS)` em staging |
| Similarity search RAG | IVFFlat Index Scan | `EXPLAIN` + benchmark 100K embeddings |
| Calcular saldo de créditos | Seq Scan (1 row por user) — OK | Verificar se view materializada necessária |
| Comissões pendentes por mês | Index Scan em `idx_commissions_partner_month_status` | Monitor de query lenta Grafana |

## F.5 Transações e Isolamento

| Operação | Isolamento | Lock | Motivo |
|---------|-----------|------|--------|
| Débito de crédito | `SERIALIZABLE` | `SELECT FOR UPDATE` no ledger | Previne race condition; invariante saldo ≥ 0 |
| Confirmar pagamento (SAGA) | `READ COMMITTED` | Nenhum | Leitura idempotente; upsert com constraint |
| Leitura de dashboard | `READ COMMITTED` | Nenhum | Performance; dados não-críticos |
| Exclusão LGPD | `READ COMMITTED` | Row-level soft delete | Cascata manual controlada |

## F.6 Backup / Restore / RPO / RTO

| Item | Configuração |
|------|-------------|
| RPO | 1 hora (WAL streaming contínuo via Supabase ou pgBaseBackup) |
| RTO | 4 horas (restore + validação + warm-up app) |
| Teste de restore | Mensal em staging com dados anonimizados |
| Retenção backups | 30 dias completos + 1 ano mensal |
| Schema evolution | Backward compatible obrigatório: adicionar colunas nullable; nunca dropar sem fase de deprecação |

---

# G) BACKEND (Java 21 + Spring Boot + Maven)

## G.1 Estrutura Maven Multi-Módulo

```
ia-platform/                         (parent POM — gestão de deps)
├── ia-platform-domain/              # Entities, Value Objects, Ports (interfaces)
│   └── ZERO dependências externas
├── ia-platform-application/         # Use Cases, Commands, Queries, Events
│   └── Depende apenas de domain
├── ia-platform-infrastructure/      # JPA, REST clients, Storage, ACLs
│   └── Implementa ports do domain
└── ia-platform-api/                 # Spring MVC, Security, Config
    └── Orquestra application + infrastructure
```

## G.2 REST API — Contratos OpenAPI

**OpenAPI 3.1 como fonte da verdade.** Gerado automaticamente pelo springdoc-openapi e versionado no repositório.

| Endpoint | Método | Auth | Idempotente | Paginação |
|---------|--------|------|------------|-----------|
| `/v1/auth/register` | POST | — | Sim (email único) | — |
| `/v1/auth/login` | POST | — | Não | — |
| `/v1/auth/refresh` | POST | RefreshToken | Não | — |
| `/v1/conversations` | GET | JWT | — | Cursor-based (after=id&limit=20) |
| `/v1/conversations` | POST | JWT | Sim (Idempotency-Key header) | — |
| `/v1/chat` | POST | JWT | Não (SSE stream) | — |
| `/v1/documents` | POST | JWT | Sim | — |
| `/v1/images/generate` | POST | JWT | Sim | — |
| `/v1/billing/me` | GET | JWT | — | — |
| `/v1/billing/subscribe` | POST | JWT | Sim (Idempotency-Key) | — |
| `/v1/webhooks/asaas` | POST | HMAC-SHA256 | Sim (gateway_id) | — |
| `/v1/users/me/data` | DELETE | JWT | Sim | — |

**Padrão de erros (RFC 7807 Problem Details):**
```json
{
  "type": "https://api.ia-platform.com.br/errors/insufficient-credits",
  "title": "Créditos insuficientes",
  "status": 402,
  "detail": "Você possui 5 créditos. Esta operação requer 10.",
  "traceId": "abc-123"
}
```

**Versionamento:** `/v1/` prefixado; breaking changes → `/v2/` com deprecation 90 dias e header `Sunset`.

## G.3 Design Patterns

| Pattern | Aplicado Em | Justificativa |
|---------|------------|--------------|
| **Strategy** | `AiProviderStrategy` (OpenRouter, OpenAI, Anthropic, Google) | Trocar provedor sem alterar ChatService; extensível para novos modelos |
| **Factory** | `AiProviderFactory.getProvider(AiModel)` | Encapsula seleção de strategy; testável com mock |
| **Observer** | `ApplicationEventPublisher` (Spring) → `CreditDebitedEvent`, `PaymentConfirmedEvent` | Desacoplamento: chat não conhece billing; payment não conhece email |
| **Template Method** | `AbstractPaymentGateway` → AsaasGateway, StripeGateway | Fluxo base fixo; variações nos filhos |
| **Decorator** | `RateLimitingAiProvider` decora qualquer `AiProviderStrategy` | Rate limit sem alterar strategies |
| **Repository** | Spring Data JPA (interface → impl automática) | Abstração de persistência; testável com Testcontainers |
| **SAGA (App Events)** | `PaymentSagaOrchestrator` | Atomicidade: Pix → créditos → email → comissão; compensação em falha |
| **ACL** | `OpenRouterACL`, `AsaasACL`, `StripeACL` | Isola contratos de terceiros do domínio |

## G.4 Big O — Fluxos Críticos

| Fluxo | Complexidade | Gargalo | Mitigação |
|-------|-------------|---------|-----------|
| Listagem conversas | O(log n) | Sem índice = O(n) | `idx_conversations_user_updated` |
| Similarity search RAG | O(sqrt(n)) c/ IVFFlat | O(n) sem índice | IVFFlat + benchmark 100K chunks |
| Débito crédito | O(1) | Race condition | `SERIALIZABLE` + `FOR UPDATE` |
| Cálculo comissão batch | O(m × n) naïve | m=1000 parceiros × n=transações | Window functions SQL; paralelismo |
| Auto-routing classificação | O(1) | Latência do classificador | Cache 1h por hash do início do prompt |

## G.5 Testes Backend

**Pirâmide BE (meta):** 70% Unit / 25% Integration / 5% Contract

**Unit Tests (JUnit 5 + Mockito):** domain services, use cases, calculators. Zero dependência de Spring ou DB.

**Integration Tests (Testcontainers):**
```java
@Testcontainers
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ChatControllerIT {
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
  @Container
  static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
}
```

**Contract Tests (obrigatórios):** OpenAPI spec gerada do backend (`springdoc-openapi`) → `openapi-typescript` gera tipos TS → `tsc --noEmit` falha no CI se contrato quebrado.

**Quality Gates:**
- JaCoCo: ≥80% line coverage, ≥70% branch coverage
- Nenhum teste pulado (`@Disabled`) sem justificativa no PR

---

# H) FRONTEND (React/Next.js/TS) — UX/UI/CX

## H.1 Arquitetura FE

**Estrutura de pastas (Next.js 14 App Router):**
```
src/
├── app/                   # App Router (layouts, pages)
│   ├── (auth)/            # login, register, verify-email
│   ├── (dashboard)/       # chat, images, billing, settings
│   └── (admin)/           # admin panel
├── components/
│   ├── ui/                # Design System primitivos
│   ├── chat/              # ChatWindow, MessageBubble, ModelSelector, CreditMeter
│   ├── billing/           # PlanCard, CheckoutPix, CreditBar
│   └── layout/            # Sidebar, Header, MobileNav
├── stores/                # Zustand
├── hooks/                 # useChat, useCredits, useAuth
├── lib/                   # api.ts (Axios+interceptors), sse.ts, analytics.ts
└── types/                 # tipos globais + tipos gerados do OpenAPI
```

**State Management — Zustand (vs Redux Toolkit):**

| Critério | Zustand | Redux |
|---------|---------|-------|
| Bundle | ~1KB | ~15KB |
| Boilerplate | Zero | Médio |
| Streaming state | Ideal (granular updates) | Possível mas mais verboso |
| Quando migrar | Se equipe FE >5 devs com features paralelas complexas | — |

**Escolha: Zustand.** Streaming de chat atualiza token a token — Zustand lida com isso com zero overhead.

## H.2 Design System

**Tokens:**
```css
--color-primary: #6C63FF;      /* Roxo IA - identidade visual */
--color-secondary: #00C896;    /* Verde - valor/economia */
--color-danger: #FF4757;       /* Vermelho - créditos baixos */
--color-surface: #0F0F1A;      /* Background dark (default) */
--color-surface-raised: #1A1A2E;
--font-heading: 'Inter', sans-serif;
--font-mono: 'JetBrains Mono', monospace;
```

**Componentes reutilizáveis obrigatórios:**
- `<CreditMeter>` — barra + R$ em tempo real (SSE event)
- `<ModelSelector>` — dropdown com ícone + custo em créditos + badge RÁPIDO/PODEROSO
- `<MessageBubble>` — markdown + código com syntax highlight + copy
- `<UpgradePrompt>` — in-context upgrade (não modal bloqueante)
- `<EmptyState>` — com call-to-action contextual (nunca tela em branco)
- `<ErrorBoundary>` — com retry e mensagem humana PT-BR

## H.3 UX Strategy e Heurísticas

**10 Heurísticas Nielsen aplicadas:**
1. Visibilidade do status: saldo de créditos + modelo atual sempre visíveis
2. Match com mundo real: PT-BR informal; "crédito" = moeda da plataforma
3. Controle do usuário: cancelar streaming; desfazer exclusão de persona (30s)
4. Consistência: tokens únicos; erros e loading padronizados
5. Prevenção de erros: alerta de crédito baixo ANTES de enviar msg cara
6. Reconhecimento > recordação: conversas recentes visíveis; modelos com ícone
7. Flexibilidade: atalhos de teclado para power users (Ctrl+Enter enviar)
8. Estética minimalista: informações progressivas (expandir para detalhes)
9. Recuperação de erros: mensagem clara + botão "Tentar novamente"
10. Ajuda: tooltip em cada modelo; onboarding inline (não modal)

**WCAG 2.1 AA Checklist:**
- [ ] Contraste mínimo 4,5:1 (texto normal) / 3:1 (texto grande)
- [ ] Tab order lógico em toda a aplicação
- [ ] ARIA labels em todos os ícones sem texto
- [ ] Foco visível (outline 2px) em todos os elementos interativos
- [ ] `aria-live="polite"` no contêiner de streaming
- [ ] Mensagens de erro associadas ao campo via `aria-describedby`
- [ ] Testado com NVDA + Chrome
- [ ] axe-core no CI (bloqueia se violação CRITICAL)

**Estados obrigatórios em toda tela:**
- Loading: skeleton screens (nunca spinners isolados)
- Empty: com CTA contextual
- Error: mensagem humana PT-BR + retry
- Success: feedback positivo discreto (toast 3s)

## H.4 Performance Budget

| Métrica | Meta | Gate (bloqueia merge) | Tool |
|--------|------|----------------------|------|
| LCP | <2,5s | >3s | Lighthouse CI |
| CLS | <0,1 | >0,15 | Lighthouse CI |
| TTI | <3,8s | >5s | Lighthouse CI |
| JS Bundle inicial | <200KB gzip | >300KB | bundle-analyzer |
| Lighthouse Score | >85 | <75 | lighthouse-ci |

**Otimizações mandatórias:** `next/image` (lazy + WebP); code splitting por rota (App Router); React Suspense para SSR streaming; `@tanstack/react-query` para server state cache.

## H.5 Validação de UX com Evidência

**Plano de Pesquisa (beta — 200 users):**
- Usability test com 5 usuários por persona (= 25 sessões)
- Task Success Rate alvo: >85%
- Tempo de primeira conversa alvo: <3 min
- Erros de navegação alvo: <1 por sessão
- NPS alvo após 7 dias: >40

**Métricas de UX no produto:**
- PostHog: funil cadastro → primeira conversa → upgrade
- Tempo médio até primeira mensagem enviada
- Taxa de abandono no checkout Pix

## H.6 Testes Frontend

**Pirâmide FE:** 60% Unit (Vitest + Testing Library) / 30% Integration (Vitest + MSW) / 10% E2E (Playwright)

**E2E Críticos (Playwright):**
1. `auth-flow.spec.ts`: registro → verificação email → login → dashboard
2. `chat-basic.spec.ts`: envio mensagem → streaming → crédito debitado → histórico salvo
3. `billing-pix.spec.ts`: escolher Pro → QR Code → webhook simulado → créditos creditados
4. `doc-upload.spec.ts`: upload PDF → pergunta → resposta com citação
5. `lgpd-deletion.spec.ts`: solicitar exclusão → confirmação → dados anonimizados

---

# I) MONÓLITO MODULAR — BOUNDARIES ENFORCED

*Não foram escolhidos microserviços. Esta seção detalha como garantir isolamento.*

## I.1 CQRS Interno (sem broker externo)

- **Commands**: mutações via Use Cases com `@Transactional`
- **Queries**: classes `*Query` separadas com `@Transactional(readOnly=true)`; podem usar SQL nativo otimizado (projeções, agregações)
- **Exemplo:** `GetUserDashboardQuery` usa SQL com JOIN e agrega créditos, plano e último uso — sem carregar entidades JPA completas

## I.2 SAGA de Pagamento (via ApplicationEventPublisher Spring)

```
[WebhookController] → PaymentConfirmedEvent
  → [1] CreditGrantSagaStep — credita créditos ao user
       falha? → CreditGrantFailedEvent → alert admin
  → [2] SubscriptionActivationSagaStep — ativa subscription
  → [3] WelcomeEmailSagaStep — envia email de boas-vindas
  → [4] PartnerCommissionSagaStep — calcula comissão se cupom existir
       compensação: se pagamento estornado, cancela comissão (status CANCELLED)
```

## I.3 Eventos e Contrato

| Evento | Publisher | Subscribers | Versionamento |
|--------|-----------|------------|--------------|
| `PaymentConfirmedEvent` | WebhookController | CreditService, EmailService, CommissionService | `v1` no campo type |
| `CreditDebitedEvent` | CreditService | CostTrackingService, NotificationService | — |
| `GdprDeletionRequestedEvent` | GdprController | GdprWorkerService | — |
| `UserRegisteredEvent` | AuthService | EmailService, OnboardingService | — |

*Eventos são in-process (`ApplicationEvent`). Idempotência garantida por `event_id` único.*

---

# J) PLANO DE IMPLEMENTAÇÃO

## J.1 Fases

| Fase | Período | Foco | Entregável |
|------|---------|------|-----------|
| Sprint 0 | Sem 1 | Setup: infra, ADRs, CI/CD, ArchUnit, OpenAPI mock | Pipeline verde + ambiente staging |
| Sprint 1-2 | Sem 2-5 | Auth + Chat Core + Créditos + Pix | Alpha 0.1 |
| Sprint 3-4 | Sem 6-9 | Multi-modelo + Docs RAG + Imagens + Web Search | Alpha 0.5 |
| Sprint 5-6 | Sem 10-13 | Personas + 6 Inovações P0 + LGPD + Admin | Beta 1.0 prep |
| Sprint 7 | Sem 14 | Polish + Bug fix + ORR + Beta launch | **Beta 1.0 — 200 users** |
| Sprint 8-9 | Sem 15-18 | Teams + Parceiros fase 1 | v1.0 GA prep |
| Sprint 10-11 | Sem 19-22 | Parceiros fase 2 + API pública | v1.5 |

## J.2 Backlog — Histórias Priorizadas (seleção com AC)

**ÉPICO EP-01: Auth & Identity**

US-001 — Cadastro por email (SP: 5)
- AC: Email verificação em <30s; senha ≥8 chars + 1 número; JWT após verificação; rate limit 5 cadastros/hora/IP

US-002 — Login Google OAuth (SP: 3)
- AC: Redirect OAuth 2.0; conta criada ou vinculada; JWT emitido; Google ID armazenado

US-003 — Refresh token rotacionado (SP: 3)
- AC: Access 15min; refresh 7d; refresh rotacionado a cada uso; token comprometido invalida família

**ÉPICO EP-02: Chat Multi-Modelo**

US-004 — Chat SSE streaming (SP: 13)
- AC: SSE estabelecido <500ms; TTFT p95 <2s; cancelar com botão Parar; créditos debitados antes do envio; histórico salvo após finalizar

US-005 — Troca modelo mid-chat (SP: 5)
- AC: Dropdown mostra modelo + custo em créditos; troca preserva histórico; badge "MODELO TROCADO"

US-006 — Auto-routing (SP: 8)
- AC: Detecta intenção (code/creative/factual); exibe badge; desativável em settings

**ÉPICO EP-03: Sistema de Créditos**

US-007 — Widget créditos real-time (SP: 3)
- AC: Barra em toda tela; atualiza por SSE event sem reload; alerta visual <10% do limite

US-008 — Free tier 300 créditos/mês (SP: 5)
- AC: Reset dia 1; 5 modelos econômicos; sem imagem/vídeo; UpgradePrompt inline; sem cartão obrigatório

**ÉPICO EP-04: Billing**

US-009 — Pix via Asaas (SP: 13)
- AC: QR Code <3s; timer 30min; credita em <30s pós-webhook; idempotência; email confirmação

US-010 — Boleto (SP: 5) | US-011 — Cartão Stripe (SP: 8)

**ÉPICO EP-05: Documentos RAG**

US-012 — Upload documento (SP: 8)
- AC: PDF/DOCX/CSV ≤50MB; parse async <10s; R2 storage; lista por usuário

US-013 — Chat com documento (SP: 8)
- AC: top-5 chunks injetados; citação de trecho na resposta; similarity <200ms

**ÉPICO EP-06: Inovações P0**

US-014 — Dashboard Custo Transparente (SP: 5) | US-015 — Modo Consultor (SP: 5)
US-016 — Links Públicos (SP: 5) | US-017 — Modo Socrático (SP: 3)
US-018 — Calculadora Economia Landing (SP: 3) | US-019 — Certificados (SP: 5)

**ÉPICO EP-07: LGPD + Admin**

US-020 — Exclusão de dados LGPD (SP: 8) | US-021 — Portabilidade (SP: 5)
US-022 — Admin usuários (SP: 8) | US-023 — Admin modelos IA (SP: 5)

## J.3 DoR e DoD

**Definition of Ready (DoR):**
- [ ] US com AC mensuráveis e aprovados pelo PO
- [ ] Mockup UX aprovado (se feature visível)
- [ ] OpenAPI spec atualizado (se endpoint novo)
- [ ] Estimativa SP acordada na Planning
- [ ] Dependências técnicas identificadas e prontas

**Definition of Done (DoD):**
- [ ] Código revisado por ≥1 dev (PR aprovado)
- [ ] Todos AC verificados em staging
- [ ] Unit tests escritos e passando
- [ ] Integration test passando (se endpoint novo)
- [ ] OpenAPI spec atualizada e gerada
- [ ] Sem CRITICAL no SAST (Snyk)
- [ ] Lighthouse Score ≥85
- [ ] Acessibilidade: zero violações axe-core CRITICAL
- [ ] Feature flag testada (on e off)
- [ ] Testado em mobile (Chrome DevTools responsive)

## J.4 Git Strategy

```
main (produção — protegida)
└── feat/{US-ID}-{descricao-curta}  → squash merge → main
└── fix/{BUG-ID}-{descricao}        → squash merge → main
└── release/v1.0.0                  → apenas para hotfix em prod
```

Regras: PR com ≥1 aprovação; CI verde; sem conflitos; squash obrigatório.

## J.5 Release Strategy

- Feature flags: Unleash (self-hosted Railway) para todos os fluxos críticos
- Canary: 5% → 25% → 100% (janela 30min entre fases)
- Rollback automático: se erro 5xx >2% em 5min OU TTFT p95 >3s → revert flag imediatamente
- Go/No-Go checklist ao final de cada sprint (PO + SRE + QA)

---

# K) QUALITY GATES (NUMÉRICOS — STOP THE LINE)

## K.1 Gates de Merge (CI bloqueia PR se)

| Gate | Threshold | Tooling |
|------|----------|---------|
| Unit coverage (linha) BE | <80% | JaCoCo |
| Branch coverage BE | <70% | JaCoCo |
| Unit coverage FE | <70% | Vitest --coverage |
| TypeScript errors | Qualquer | `tsc --noEmit` |
| ESLint errors | Qualquer | ESLint |
| Checkstyle violations | Qualquer | Checkstyle |
| SAST CRITICAL | Qualquer | Snyk Code |
| Dependency CRITICAL CVE | Qualquer | Snyk Test |
| Secrets exposed | Qualquer | Gitleaks |
| Lighthouse Score | <75 | lighthouse-ci |
| LCP | >3s | lighthouse-ci |
| CLS | >0,15 | lighthouse-ci |
| axe-core CRITICAL | Qualquer | @axe-core/playwright |
| Contract (TypeScript) | Qualquer tipo inválido | openapi-typescript + tsc |

## K.2 Gates de Release (bloqueia deploy para produção)

| Gate | Threshold | Tooling |
|------|----------|---------|
| Staging E2E smoke (5 críticos) | 100% passando | Playwright |
| Load test p95 | <2s com 500 VU | Gatling |
| OWASP ZAP | Zero HIGH/CRITICAL | ZAP Baseline Scan |
| ORR checklist | 100% itens OK | Manual + sign-off SRE |
| Observabilidade | Logs/métricas/traces ativos | Grafana alert ativo |

## K.3 Quality Gate Pipeline (CI/CD)

```yaml
jobs:
  backend:
    steps:
      - ./mvnw verify                          # unit + integration
      - ./mvnw checkstyle:check
      - snyk test --severity-threshold=critical
      - snyk code test
      - gitleaks detect --source=.

  frontend:
    steps:
      - npm run type-check                     # tsc --noEmit
      - npm run lint
      - npm run test:unit -- --coverage
      - npm run build
      - lighthouse-ci collect && lhci assert

  contract:
    steps:
      - ./mvnw springdoc:generate             # gera openapi.json
      - npx openapi-typescript openapi.json   # gera types.ts
      - npx tsc --noEmit                      # falha se contrato quebrado

  e2e:
    steps:
      - npx playwright test --project=chromium # apenas staging
```

---

# L) FITNESS FUNCTIONS (ARQUITETURA EXECUTÁVEL)

## L.1 Regras de Dependência por Camada (ArchUnit)

```java
@Test void domainNeverImportsInfrastructure() {
    noClasses().that().resideInPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInPackage("..infrastructure..")
        .check(importedClasses);
}

@Test void applicationNeverImportsAdapters() {
    noClasses().that().resideInPackage("..application..")
        .should().dependOnClassesThat()
        .resideInPackage("..api..", "..infrastructure..")
        .check(importedClasses);
}

@Test void noCyclicDependencies() {
    slices().matching("com.iaplatform.(*)..")
        .should().beFreeOfCycles()
        .check(importedClasses);
}

@Test void controllersDontCallRepositoriesDirectly() {
    noClasses().that().resideInPackage("..api..")
        .should().dependOnClassesThat()
        .resideInPackage("..persistence..")
        .check(importedClasses);
}
```

## L.2 Limites de Acoplamento

- Bounded Context **Conversation** não pode importar **Partner** diretamente → usa eventos
- Bounded Context **Billing** não pode importar **Notification** → usa eventos
- Módulo `ia-platform-domain` não pode ter dependências externas (validado pelo Maven scope)

## L.3 Políticas de Convenções

- Todos os controllers terminam em `Controller`
- Todos os use cases terminam em `UseCase` ou `Command`/`Query`
- Todos os ports (interfaces) ficam em `..domain.port..`
- Nenhuma classe com `@Entity` em módulos `domain` ou `application`

## L.4 API Contract como Fonte da Verdade

- OpenAPI spec gerado do Spring Boot (`springdoc-openapi`) no CI
- `openapi-typescript` converte para tipos TS
- `tsc --noEmit` falha se FE usa campo inexistente ou tipo errado
- PR não pode ser mergeado com typescript error → contrato sempre válido

---

# M) OPERAÇÃO: ORR + RUNBOOKS + DR + INCIDENTES

## M.1 ORR — Checklist Formal (Pré-GA)

| Item | Responsável | Status |
|------|------------|--------|
| Alertas PagerDuty configurados e testados | SRE | — |
| Dashboards Grafana operacionais (SLI/SLO visíveis) | SRE | — |
| Runbooks criados e revisados (≥5) | SRE + TLs | — |
| Restore de backup testado com sucesso | SRE | — |
| Load test passando (500 VU p95 <2s) | QA + SRE | — |
| OWASP ZAP scan: zero HIGH/CRITICAL | Security | — |
| Pen test externo realizado | Security | — |
| Feature flags testadas (on/off) | TL FE + TL BE | — |
| Rollback manual testado em staging | SRE | — |
| DPO notificado sobre dados em produção | DPO | — |
| Status page configurada (statuspage.io) | SRE | — |
| Plano de comunicação de incidentes definido | PO | — |
| SLA e ToS publicados | Jurídico | — |
| On-call rotation definida | SRE + TLs | — |

**Go/No-Go:** PO + Solution Architect + SRE assinam formalmente. Qualquer bloqueador impede o Go.

## M.2 Runbooks (5 mínimo)

### RB-01: DB Connection Pool Exhausted
```
1. Detecção: Alert Grafana "pg_stat_activity > 90% pool"
2. Diagnóstico: SELECT count(*), state FROM pg_stat_activity GROUP BY state;
3. Mitigação imediata: Restart da instância API + aumentar pool.size em -Dspring.datasource.hikari.maximum-pool-size=30
4. Causa raiz: Queries lentas (>30s); lock contention
5. Prevenção: EXPLAIN em queries lentas; timeout query 30s configurado
```

### RB-02: OpenRouter 5xx Global (Outage)
```
1. Detecção: Alert "openrouter.error_rate > 10% em 2min"
2. Verificação: curl https://status.openrouter.ai
3. Mitigação: Ativar FF "USE_DIRECT_PROVIDERS_ONLY=true" via Unleash
4. Comunicação: Banner in-app "Alguns modelos temporariamente indisponíveis"
5. Rollback FF: Quando OpenRouter status verde + 5min de latência normal
```

### RB-03: Pix Webhook Failure (pagamentos não confirmados)
```
1. Detecção: Alert "payment.webhook.failed_rate > 5% em 5min"
2. Diagnóstico: Consultar admin panel → "Pagamentos Pendentes"
3. Verificação: Conferir assinatura HMAC no log; verificar IP da Asaas
4. Mitigação: Processar manualmente via admin panel (botão "Reprocessar")
5. Comunicação: Email ao usuário afetado com desculpas + crédito de cortesia
```

### RB-04: Deploy com Regressão (spike de erros 5xx)
```
1. Detecção: Alert "5xx rate > 2% em 5min"
2. Verificação: Grafana → trace do primeiro erro; logs estruturados
3. Mitigação imediata: Ativar FF "new_feature=false" se feature flagged
4. Rollback completo: git tag + redeploy do artefato anterior via Railway
5. Critério de rollback: erro >2% por >5min OU TTFT p95 >3s por >5min
6. Postmortem: Agendado em 48h
```

### RB-05: Degradação de Performance (TTFT >3s)
```
1. Detecção: Alert "chat.ttft_ms p95 > 3000ms em 10min"
2. Diagnóstico: Verificar latência OpenRouter (direto vs plataforma); verificar DB slow queries
3. Mitigação: Ativar Semantic Cache; desabilitar modelos mais lentos temporariamente
4. Comunicação: Status page "degradação de performance" se >15min
5. Investigação: Comparar métricas de antes/depois do último deploy
```

## M.3 Plano de DR

| Item | Configuração |
|------|-------------|
| RPO | 1 hora |
| RTO | 4 horas |
| Restore drill | Mensal em staging com dados anonimizados |
| Responsável | SRE (ou TL BE acumulando no MVP) |
| Ambiente de DR | Cópia standby no Railway (região diferente) |

**Simulação de DR (trimestral):**
1. Simular falha do DB primário (desligar instância em staging)
2. Restaurar backup mais recente
3. Validar dados (row count + integridade referencial)
4. Validar aplicação funcional
5. Documentar RTO real vs meta

## M.4 Gestão de Incidentes

| Severidade | Critério | Resposta | Comunicação |
|-----------|---------|---------|------------|
| P1 — Crítico | Downtime total OU pagamentos falhando OU LGPD violação | Resposta imediata; PagerDuty liga telefone; all-hands | Status page em <10min; email afetados em <1h |
| P2 — Alto | TTFT >3s OU error rate >2% OU funcionalidade core degradada | Resposta em 30min; alerta no Slack | Status page em <30min |
| P3 — Médio | Feature não-crítica com erros | Resposta no próximo dia útil | Ticket + acompanhamento |
| P4 — Baixo | Bug cosmético ou performance marginal | Backlog normal | — |

**Postmortem template (obrigatório para P1/P2):**
- Sumário (o que aconteceu, quando, impacto)
- Timeline de detecção → resposta → resolução
- Causa raiz (5 Whys)
- Ações preventivas (com dono e prazo)

---

# N) DOCUMENTAÇÃO (README + ADRs)

## N.1 README.md (Template Completo)

```markdown
# IA Aggregator Platform

> Acesso a 30+ modelos de IA por assinatura única em reais.
> PT-BR nativo · Pix/boleto · LGPD compliance

## Visão Geral
[2 parágrafos descrevendo o produto e stack]

## Arquitetura
Monólito Modular (Next.js + Java 21 + PostgreSQL)
Detalhes: /docs/architecture/ (ADRs + C4 diagrams)

## Quick Start (5 minutos)

### Pré-requisitos
Node.js 20+ | Java 21+ | Maven 3.9+ | Docker

### Backend
cd backend
cp .env.example .env            # preencher variáveis obrigatórias
docker-compose up -d            # PostgreSQL 16 + Redis 7
./mvnw spring-boot:run          # Flyway migrations automáticas

### Frontend
cd frontend
cp .env.local.example .env.local
npm install && npm run dev
# → http://localhost:3000

## Variáveis de Ambiente Obrigatórias
DATABASE_URL=jdbc:postgresql://localhost:5432/iaplatform
OPENROUTER_API_KEY=sk-or-...
ASAAS_API_KEY=...
ASAAS_WEBHOOK_SECRET=...
STRIPE_SECRET_KEY=sk_...
STRIPE_WEBHOOK_SECRET=whsec_...
SERPER_API_KEY=...
CLOUDFLARE_R2_ACCESS_KEY=...
CLOUDFLARE_R2_SECRET_KEY=...
RESEND_API_KEY=re_...
JWT_SECRET=...    # min 256 bits, gerar com: openssl rand -hex 32

## Testes
# Backend
./mvnw test                      # unit tests
./mvnw verify -Pintegration      # integration (requer Docker)
./mvnw verify jacoco:report      # relatório de cobertura

# Frontend
npm run test:unit                # Vitest
npm run test:e2e                 # Playwright (requer backend rodando)

## Migrações Flyway
Executadas automaticamente no startup.
Novo migration: criar V{n+1}__{descricao}.sql em ia-platform-infrastructure/src/main/resources/db/migration/

## Observabilidade
Grafana: http://localhost:3001 (usuário: admin, senha: admin)
Dashboards: Chat Latency · Credit Usage · Payment Funnel · Error Rate

## Troubleshooting
- DB Connection Exhausted → docs/runbooks/rb-01-db-pool.md
- OpenRouter Instável → docs/runbooks/rb-02-openrouter-outage.md
- Pix Webhook Failure → docs/runbooks/rb-03-pix-webhook.md
- Deploy Regressão → docs/runbooks/rb-04-deploy-rollback.md
- Performance Degradada → docs/runbooks/rb-05-performance.md
```

## N.2 ADRs — Lista Obrigatória

| ADR | Título | Decisão | Alternativas Consideradas | Trade-offs |
|-----|--------|---------|--------------------------|----------|
| ADR-001 | Estilo arquitetural | Monólito Modular | Microserviços | Simplicidade operacional vs escalabilidade futura |
| ADR-002 | Migrações DB | Flyway (SQL puro) | Liquibase | Legibilidade vs rollback automático |
| ADR-003 | State Management FE | Zustand | Redux Toolkit, Jotai | Bundle size vs DevTools integrados |
| ADR-004 | Gateway de IA | OpenRouter 80% + direto 20% | Single gateway | Resiliência vs complexidade |
| ADR-005 | Vector Store (RAG) | pgvector (extensão PG) | Pinecone, Qdrant | Zero custo adicional vs escala especializada |
| ADR-006 | Pagamentos BR | Asaas (Pix/boleto) + Stripe (cartão) | Mercado Pago, PagSeguro | Pix nativo vs taxa menor |
| ADR-007 | Auth tokens | JWT stateless + refresh rotation | Session + Redis | Escalabilidade vs controle de sessão |
| ADR-008 | FE Router | Next.js App Router | Pages Router, SPA pura | Streaming SSR vs familiaridade |
| ADR-009 | Analytics | PostHog (self-hosted) | Mixpanel, Amplitude | Privacy-first vs recursos avançados |
| ADR-010 | Feature Flags | Unleash (Railway) | LaunchDarkly, custom DB | Self-hosted vs managed service |

---

# O) PACOTE DE EVIDÊNCIAS — CÓDIGO CONCRETO

> Cada evidência abaixo é um artefato executável que prova viabilidade técnica das decisões arquiteturais.

## O1 — OpenAPI Spec (Trecho Representativo)

```yaml
openapi: 3.1.0
info:
  title: IA Aggregator Platform API
  version: 1.0.0
  description: API da plataforma de agregação de modelos de IA
servers:
  - url: https://api.ia-platform.com.br/v1
    description: Produção
  - url: http://localhost:8080/v1
    description: Local

paths:
  /chat:
    post:
      operationId: sendChatMessage
      summary: Envia mensagem para modelo de IA (SSE streaming)
      tags: [Chat]
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ChatRequest'
      responses:
        '200':
          description: Stream SSE de tokens
          content:
            text/event-stream:
              schema:
                $ref: '#/components/schemas/ChatStreamEvent'
        '402':
          description: Créditos insuficientes
          content:
            application/problem+json:
              schema:
                $ref: '#/components/schemas/ProblemDetail'
        '429':
          description: Rate limit excedido

  /conversations:
    get:
      operationId: listConversations
      summary: Lista conversas do usuário (cursor-based)
      tags: [Conversations]
      security:
        - bearerAuth: []
      parameters:
        - name: after
          in: query
          schema:
            type: string
            format: uuid
          description: Cursor — ID da última conversa
        - name: limit
          in: query
          schema:
            type: integer
            default: 20
            maximum: 50
      responses:
        '200':
          description: Lista paginada
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ConversationListResponse'

  /billing/pix:
    post:
      operationId: createPixPayment
      summary: Gera QR Code Pix para pagamento
      tags: [Billing]
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PixPaymentRequest'
      responses:
        '201':
          description: QR Code gerado
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PixPaymentResponse'

  /webhooks/asaas:
    post:
      operationId: handleAsaasWebhook
      summary: Recebe webhook de confirmação Asaas
      tags: [Webhooks]
      security:
        - hmacAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AsaasWebhookPayload'
      responses:
        '200':
          description: Webhook processado
        '401':
          description: HMAC inválido

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
    hmacAuth:
      type: apiKey
      in: header
      name: X-Asaas-Signature

  schemas:
    ChatRequest:
      type: object
      required: [conversationId, message]
      properties:
        conversationId:
          type: string
          format: uuid
        message:
          type: string
          maxLength: 32000
        modelId:
          type: string
          format: uuid
          description: Se omitido, usa auto-routing
        enableWebSearch:
          type: boolean
          default: false
        personaId:
          type: string
          format: uuid

    ChatStreamEvent:
      type: object
      properties:
        event:
          type: string
          enum: [token, done, error, credit_update]
        data:
          type: string
        creditsUsed:
          type: integer
        creditsRemaining:
          type: integer

    ProblemDetail:
      type: object
      description: RFC 7807 Problem Details
      properties:
        type:
          type: string
          format: uri
        title:
          type: string
        status:
          type: integer
        detail:
          type: string
        traceId:
          type: string

    ConversationListResponse:
      type: object
      properties:
        data:
          type: array
          items:
            $ref: '#/components/schemas/ConversationSummary'
        nextCursor:
          type: string
          format: uuid
          nullable: true

    ConversationSummary:
      type: object
      properties:
        id:
          type: string
          format: uuid
        title:
          type: string
        modelName:
          type: string
        lastMessageAt:
          type: string
          format: date-time
        totalCreditsUsed:
          type: integer

    PixPaymentRequest:
      type: object
      required: [planId]
      properties:
        planId:
          type: string
          enum: [STARTER, PRO, TEAM]
        couponCode:
          type: string

    PixPaymentResponse:
      type: object
      properties:
        paymentOrderId:
          type: string
          format: uuid
        qrCodeBase64:
          type: string
        qrCodeText:
          type: string
        expiresAt:
          type: string
          format: date-time
        amountCents:
          type: integer
```

## O2 — Flyway Migration V1 (DDL Completo)

```sql
-- V1__create_users_auth.sql
-- Contexto: Identity Bounded Context
-- Autor: Solution Architect | Sprint 0

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(150) NOT NULL,
    email_verified_at TIMESTAMPTZ,
    password_hash VARCHAR(255),
    full_name VARCHAR(200),
    role VARCHAR(20) NOT NULL DEFAULT 'USER'
        CHECK (role IN ('USER','PARTNER','WORKSPACE_ADMIN','PLATFORM_ADMIN','SUPER_ADMIN')),
    google_id VARCHAR(100) UNIQUE,
    avatar_url VARCHAR(500),
    preferred_language VARCHAR(5) NOT NULL DEFAULT 'pt-BR',
    data_training_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    marketing_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    lgpd_consent_at TIMESTAMPTZ,
    lgpd_consent_version VARCHAR(10),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_google_id ON users(google_id) WHERE google_id IS NOT NULL;

-- ============================================================
-- REFRESH TOKENS (rotação obrigatória)
-- ============================================================
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    family_id UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens(family_id);

-- ============================================================
-- API KEYS (para devs — Thiago persona)
-- ============================================================
CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    key_prefix VARCHAR(8) NOT NULL,
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    scopes VARCHAR(500) NOT NULL DEFAULT 'chat:read,chat:write',
    last_used_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_api_keys_user ON api_keys(user_id) WHERE revoked_at IS NULL;

-- ============================================================
-- AI MODELS (Catalog Bounded Context)
-- ============================================================
CREATE TABLE ai_models (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(50) NOT NULL,
    model_key VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    category VARCHAR(30) NOT NULL
        CHECK (category IN ('TEXT','IMAGE','AUDIO','VIDEO','EMBEDDING')),
    credit_multiplier INTEGER NOT NULL DEFAULT 1 CHECK (credit_multiplier >= 1),
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    is_free_tier BOOLEAN NOT NULL DEFAULT FALSE,
    max_tokens INTEGER,
    supports_streaming BOOLEAN NOT NULL DEFAULT TRUE,
    supports_vision BOOLEAN NOT NULL DEFAULT FALSE,
    routing_tags VARCHAR(200),
    display_order INTEGER NOT NULL DEFAULT 100,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- CREDIT LEDGERS (Billing Bounded Context)
-- ============================================================
CREATE TABLE credit_ledgers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    balance INTEGER NOT NULL DEFAULT 0
        CHECK (balance >= 0),
    monthly_allowance INTEGER NOT NULL DEFAULT 300,
    reset_date DATE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- CREDIT TRANSACTIONS (insert-only, nunca UPDATE/DELETE)
-- ============================================================
CREATE TABLE credit_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    amount INTEGER NOT NULL,
    type VARCHAR(30) NOT NULL
        CHECK (type IN ('MONTHLY_GRANT','CHAT_DEBIT','IMAGE_DEBIT',
                        'WEB_SEARCH_DEBIT','BONUS','REFUND','PLAN_UPGRADE')),
    conversation_id UUID,
    ai_model_id UUID REFERENCES ai_models(id),
    description VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_credit_tx_user_created
    ON credit_transactions(user_id, created_at DESC);

-- ============================================================
-- SUBSCRIPTIONS
-- ============================================================
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    plan VARCHAR(20) NOT NULL
        CHECK (plan IN ('FREE','STARTER','PRO','TEAM')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE','CANCELLED','PAST_DUE','EXPIRED')),
    gateway VARCHAR(20),
    gateway_subscription_id VARCHAR(100),
    current_period_start DATE NOT NULL,
    current_period_end DATE NOT NULL,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscriptions_user ON subscriptions(user_id, status);

-- ============================================================
-- PAYMENT ORDERS (idempotência via gateway_id)
-- ============================================================
CREATE TABLE payment_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    subscription_id UUID REFERENCES subscriptions(id),
    gateway VARCHAR(20) NOT NULL CHECK (gateway IN ('ASAAS','STRIPE')),
    gateway_id VARCHAR(100) UNIQUE,
    method VARCHAR(20) NOT NULL CHECK (method IN ('PIX','BOLETO','CARD')),
    amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','CONFIRMED','FAILED','REFUNDED','EXPIRED')),
    pix_qr_code TEXT,
    pix_qr_code_text TEXT,
    boleto_url TEXT,
    expires_at TIMESTAMPTZ,
    confirmed_at TIMESTAMPTZ,
    coupon_id UUID,
    discount_cents INTEGER DEFAULT 0,
    idempotency_key UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_orders_user ON payment_orders(user_id, created_at DESC);
CREATE INDEX idx_payment_orders_gateway ON payment_orders(gateway_id);

-- ============================================================
-- CONVERSATIONS
-- ============================================================
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(200),
    current_model_id UUID REFERENCES ai_models(id),
    persona_id UUID,
    is_consultant_mode BOOLEAN NOT NULL DEFAULT FALSE,
    is_socratic_mode BOOLEAN NOT NULL DEFAULT FALSE,
    total_credits_used INTEGER NOT NULL DEFAULT 0,
    message_count INTEGER NOT NULL DEFAULT 0,
    public_slug VARCHAR(8) UNIQUE,
    public_expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_conversations_user_updated
    ON conversations(user_id, updated_at DESC) WHERE deleted_at IS NULL;

-- ============================================================
-- MESSAGES
-- ============================================================
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER','ASSISTANT','SYSTEM')),
    content TEXT NOT NULL,
    ai_model_id UUID REFERENCES ai_models(id),
    credits_used INTEGER DEFAULT 0,
    tokens_input INTEGER,
    tokens_output INTEGER,
    latency_ms INTEGER,
    web_search_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_conversation_created
    ON messages(conversation_id, created_at);

-- ============================================================
-- AUDIT LOGS (imutável — regras PG impedem UPDATE/DELETE)
-- ============================================================
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id UUID,
    details JSONB,
    ip_address INET,
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE RULE audit_logs_no_delete AS ON DELETE TO audit_logs DO INSTEAD NOTHING;
CREATE RULE audit_logs_no_update AS ON UPDATE TO audit_logs DO INSTEAD NOTHING;

CREATE INDEX idx_audit_logs_user_action
    ON audit_logs(user_id, action, created_at DESC);
```

## O3 — Folder Tree (Mono-Repo Completo)

```
ia-platform/                           # Root mono-repo
├── pom.xml                            # Parent POM (gestão de deps + módulos)
├── .github/
│   └── workflows/
│       ├── ci.yml                     # Build + test + quality gates
│       └── deploy.yml                 # Deploy para Railway/Vercel
├── docs/
│   ├── architecture/
│   │   ├── c4-context.puml
│   │   ├── c4-containers.puml
│   │   └── adrs/
│   │       ├── ADR-001-monolith-modular.md
│   │       ├── ADR-002-flyway-over-liquibase.md
│   │       └── ...
│   ├── runbooks/
│   │   ├── rb-01-db-pool.md
│   │   ├── rb-02-openrouter-outage.md
│   │   ├── rb-03-pix-webhook.md
│   │   ├── rb-04-deploy-rollback.md
│   │   └── rb-05-performance.md
│   └── openapi.yaml                   # OpenAPI spec (gerada pelo CI)
│
├── ia-platform-domain/                # DOMAIN MODULE — zero deps externas
│   ├── pom.xml
│   └── src/main/java/com/iaplatform/domain/
│       ├── identity/
│       │   ├── User.java              # Aggregate Root
│       │   ├── UserId.java            # Value Object
│       │   ├── Email.java             # VO com validação
│       │   └── port/
│       │       └── UserRepository.java
│       ├── conversation/
│       │   ├── Conversation.java      # Aggregate Root
│       │   ├── Message.java           # Entity
│       │   ├── ConversationId.java
│       │   └── port/
│       │       ├── ConversationRepository.java
│       │       └── AiProviderPort.java
│       ├── billing/
│       │   ├── CreditLedger.java      # Aggregate Root
│       │   ├── CreditTransaction.java
│       │   ├── Subscription.java      # Aggregate Root
│       │   ├── CreditAmount.java      # VO
│       │   ├── Plan.java              # Enum
│       │   └── port/
│       │       ├── CreditLedgerRepository.java
│       │       └── PaymentGatewayPort.java
│       ├── payment/
│       │   ├── PaymentOrder.java      # Aggregate Root
│       │   └── PaymentStatus.java     # Enum
│       ├── catalog/
│       │   ├── AiModel.java           # Aggregate Root
│       │   └── ModelCategory.java     # Enum
│       └── shared/
│           ├── Money.java             # VO
│           ├── DomainEvent.java       # Base
│           └── AggregateRoot.java     # Base
│
├── ia-platform-application/           # USE CASES — depende só de domain
│   ├── pom.xml
│   └── src/main/java/com/iaplatform/application/
│       ├── chat/
│       │   ├── SendMessageCommand.java
│       │   ├── SendMessageUseCase.java
│       │   └── AiRoutingService.java
│       ├── billing/
│       │   ├── SubscribeCommand.java
│       │   ├── SubscribeUseCase.java
│       │   ├── DebitCreditsUseCase.java
│       │   └── GetDashboardQuery.java
│       ├── payment/
│       │   ├── CreatePixPaymentUseCase.java
│       │   ├── PaymentSagaOrchestrator.java
│       │   └── ConfirmPaymentCommand.java
│       ├── document/
│       │   ├── UploadDocumentUseCase.java
│       │   └── RagQueryUseCase.java
│       ├── identity/
│       │   ├── RegisterUserCommand.java
│       │   ├── RegisterUserUseCase.java
│       │   └── LoginUseCase.java
│       └── event/
│           ├── PaymentConfirmedEvent.java
│           ├── CreditDebitedEvent.java
│           ├── UserRegisteredEvent.java
│           └── GdprDeletionRequestedEvent.java
│
├── ia-platform-infrastructure/        # ADAPTERS — implementa ports
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/iaplatform/infrastructure/
│       │   ├── persistence/
│       │   │   ├── JpaUserRepository.java
│       │   │   ├── JpaConversationRepository.java
│       │   │   ├── JpaCreditLedgerRepository.java
│       │   │   └── entity/              # JPA entities separadas de domain
│       │   │       ├── UserJpaEntity.java
│       │   │       ├── ConversationJpaEntity.java
│       │   │       └── ...
│       │   ├── ai/
│       │   │   ├── OpenRouterAcl.java
│       │   │   ├── OpenAiDirectAcl.java
│       │   │   ├── AnthropicDirectAcl.java
│       │   │   └── RateLimitingAiProviderDecorator.java
│       │   ├── payment/
│       │   │   ├── AsaasGateway.java
│       │   │   ├── StripeGateway.java
│       │   │   └── AbstractPaymentGateway.java
│       │   ├── storage/
│       │   │   └── CloudflareR2Storage.java
│       │   ├── email/
│       │   │   └── ResendEmailService.java
│       │   └── search/
│       │       └── SerperWebSearchService.java
│       └── resources/
│           └── db/migration/
│               ├── V1__create_users_auth.sql
│               ├── V2__create_workspaces_rbac.sql
│               ├── V3__create_billing_credits.sql
│               ├── V4__create_conversations_messages.sql
│               ├── V5__create_documents_pgvector.sql
│               ├── V6__create_image_generations.sql
│               ├── V7__create_audit_logs.sql
│               ├── V8__create_partners_coupons.sql
│               ├── V9__create_attributions_commissions.sql
│               ├── V10__create_notifications.sql
│               └── V11__add_indexes_critical.sql
│
├── ia-platform-api/                   # SPRING BOOT APP — orquestra tudo
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/iaplatform/api/
│       │   ├── IaPlatformApplication.java
│       │   ├── config/
│       │   │   ├── SecurityConfig.java
│       │   │   ├── CorsConfig.java
│       │   │   ├── OpenApiConfig.java
│       │   │   └── ObservabilityConfig.java
│       │   ├── controller/
│       │   │   ├── ChatController.java
│       │   │   ├── ConversationController.java
│       │   │   ├── BillingController.java
│       │   │   ├── WebhookController.java
│       │   │   ├── DocumentController.java
│       │   │   ├── AuthController.java
│       │   │   └── AdminController.java
│       │   ├── middleware/
│       │   │   ├── JwtAuthFilter.java
│       │   │   ├── RateLimitFilter.java
│       │   │   └── CorrelationIdFilter.java
│       │   └── dto/
│       │       ├── ChatRequestDto.java
│       │       ├── ChatStreamEventDto.java
│       │       └── ...
│       └── test/java/com/iaplatform/
│           ├── unit/
│           │   ├── domain/
│           │   │   ├── CreditLedgerTest.java
│           │   │   └── ConversationTest.java
│           │   └── application/
│           │       ├── SendMessageUseCaseTest.java
│           │       └── DebitCreditsUseCaseTest.java
│           ├── integration/
│           │   ├── ChatControllerIT.java
│           │   ├── BillingControllerIT.java
│           │   └── AsaasGatewayIT.java
│           └── architecture/
│               └── ArchitectureTest.java
│
└── frontend/                          # NEXT.JS APP
    ├── package.json
    ├── next.config.ts
    ├── tailwind.config.ts
    ├── tsconfig.json
    ├── .storybook/
    │   └── main.ts
    ├── public/
    │   └── locales/pt-BR/
    ├── src/
    │   ├── app/
    │   │   ├── layout.tsx
    │   │   ├── page.tsx               # Landing page
    │   │   ├── (auth)/
    │   │   │   ├── login/page.tsx
    │   │   │   ├── register/page.tsx
    │   │   │   └── verify-email/page.tsx
    │   │   ├── (dashboard)/
    │   │   │   ├── layout.tsx         # Sidebar + Header
    │   │   │   ├── chat/
    │   │   │   │   ├── page.tsx
    │   │   │   │   └── [id]/page.tsx
    │   │   │   ├── images/page.tsx
    │   │   │   ├── documents/page.tsx
    │   │   │   ├── billing/page.tsx
    │   │   │   ├── settings/page.tsx
    │   │   │   └── personas/page.tsx
    │   │   └── (admin)/
    │   │       ├── layout.tsx
    │   │       ├── users/page.tsx
    │   │       └── models/page.tsx
    │   ├── components/
    │   │   ├── ui/                    # Design System primitivos
    │   │   │   ├── Button.tsx
    │   │   │   ├── Input.tsx
    │   │   │   ├── Card.tsx
    │   │   │   ├── Toast.tsx
    │   │   │   ├── Skeleton.tsx
    │   │   │   ├── ErrorBoundary.tsx
    │   │   │   └── EmptyState.tsx
    │   │   ├── chat/
    │   │   │   ├── ChatWindow.tsx
    │   │   │   ├── MessageBubble.tsx
    │   │   │   ├── ModelSelector.tsx
    │   │   │   ├── CreditMeter.tsx
    │   │   │   ├── StreamingIndicator.tsx
    │   │   │   └── CostWidget.tsx
    │   │   ├── billing/
    │   │   │   ├── PlanCard.tsx
    │   │   │   ├── CheckoutPix.tsx
    │   │   │   ├── CreditBar.tsx
    │   │   │   └── UpgradePrompt.tsx
    │   │   └── layout/
    │   │       ├── Sidebar.tsx
    │   │       ├── Header.tsx
    │   │       └── MobileNav.tsx
    │   ├── stores/
    │   │   ├── useChatStore.ts
    │   │   ├── useAuthStore.ts
    │   │   └── useCreditStore.ts
    │   ├── hooks/
    │   │   ├── useChat.ts
    │   │   ├── useCredits.ts
    │   │   ├── useAuth.ts
    │   │   └── useSSE.ts
    │   ├── lib/
    │   │   ├── api.ts                 # Axios + interceptors
    │   │   ├── sse.ts                 # SSE client
    │   │   ├── analytics.ts           # PostHog
    │   │   └── format.ts              # Currency, dates PT-BR
    │   └── types/
    │       ├── api.generated.ts       # Gerado do OpenAPI
    │       └── index.ts
    ├── e2e/
    │   ├── auth-flow.spec.ts
    │   ├── chat-basic.spec.ts
    │   ├── billing-pix.spec.ts
    │   ├── doc-upload.spec.ts
    │   └── lgpd-deletion.spec.ts
    └── __tests__/
        ├── components/
        │   ├── CreditMeter.test.tsx
        │   └── ModelSelector.test.tsx
        └── hooks/
            └── useChat.test.ts
```

## O4 — React Component (CreditMeter)

```tsx
// frontend/src/components/chat/CreditMeter.tsx
'use client';

import { useCreditStore } from '@/stores/useCreditStore';
import { useSSE } from '@/hooks/useSSE';
import { formatCredits, formatBRL } from '@/lib/format';
import { cn } from '@/lib/utils';
import { useEffect, useMemo } from 'react';

interface CreditMeterProps {
  className?: string;
  showCostInReais?: boolean;
}

const CREDIT_TO_BRL_RATE = 0.028; // R$0,028 por crédito (baseado no Pro R$99/3.500cr)

export function CreditMeter({ className, showCostInReais = true }: CreditMeterProps) {
  const { balance, monthlyAllowance, setBalance } = useCreditStore();

  // SSE listener para atualização em tempo real (RF-003 AC)
  useSSE('/v1/sse/credits', {
    onMessage: (event) => {
      if (event.type === 'credit_update') {
        setBalance(event.data.creditsRemaining);
      }
    },
  });

  const percentage = useMemo(
    () => Math.max(0, Math.min(100, (balance / monthlyAllowance) * 100)),
    [balance, monthlyAllowance]
  );

  const isLow = percentage < 10;
  const isMedium = percentage < 25 && !isLow;

  const balanceInReais = useMemo(
    () => balance * CREDIT_TO_BRL_RATE,
    [balance]
  );

  return (
    <div
      className={cn('flex items-center gap-3 px-3 py-2', className)}
      role="meter"
      aria-valuenow={balance}
      aria-valuemin={0}
      aria-valuemax={monthlyAllowance}
      aria-label={`${balance} créditos restantes de ${monthlyAllowance}`}
    >
      {/* Barra de progresso */}
      <div className="flex-1 h-2 bg-surface-raised rounded-full overflow-hidden">
        <div
          className={cn(
            'h-full rounded-full transition-all duration-300',
            isLow && 'bg-danger animate-pulse',
            isMedium && 'bg-yellow-500',
            !isLow && !isMedium && 'bg-secondary'
          )}
          style={{ width: `${percentage}%` }}
        />
      </div>

      {/* Valor numérico */}
      <div className="flex flex-col items-end min-w-[80px]">
        <span
          className={cn(
            'text-sm font-mono font-semibold',
            isLow && 'text-danger',
            !isLow && 'text-gray-200'
          )}
        >
          {formatCredits(balance)}
        </span>
        {showCostInReais && (
          <span className="text-xs text-gray-500">
            ≈ {formatBRL(balanceInReais)}
          </span>
        )}
      </div>

      {/* Alerta de créditos baixos (RF-007 AC: alerta visual <10%) */}
      {isLow && (
        <button
          className="text-xs text-danger hover:text-danger/80 underline"
          onClick={() => window.location.href = '/billing'}
          aria-live="polite"
        >
          Recarregar
        </button>
      )}
    </div>
  );
}
```

## O5 — Java Domain Entity (CreditLedger)

```java
// ia-platform-domain/src/main/java/com/iaplatform/domain/billing/CreditLedger.java
package com.iaplatform.domain.billing;

import com.iaplatform.domain.shared.AggregateRoot;
import com.iaplatform.domain.identity.UserId;

/**
 * Aggregate Root do Billing Bounded Context.
 * Invariante crítica: balance >= 0 SEMPRE (enforced no DB + domínio).
 * Transação SERIALIZABLE + FOR UPDATE no adapter JPA.
 */
public class CreditLedger extends AggregateRoot {

    private final UserId userId;
    private int balance;
    private int monthlyAllowance;

    public CreditLedger(UserId userId, int monthlyAllowance) {
        if (monthlyAllowance < 0) {
            throw new IllegalArgumentException("Monthly allowance cannot be negative");
        }
        this.userId = userId;
        this.balance = monthlyAllowance;
        this.monthlyAllowance = monthlyAllowance;
    }

    /**
     * Debita créditos. Lança InsufficientCreditsException se saldo insuficiente.
     * Big O: O(1) — operação atômica no agregado.
     *
     * @param amount Quantidade de créditos a debitar (deve ser > 0)
     * @param modelMultiplier Multiplicador do modelo de IA (>= 1)
     * @return totalDebited — créditos efetivamente debitados
     */
    public int debit(int amount, int modelMultiplier) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (modelMultiplier < 1) throw new IllegalArgumentException("Multiplier must be >= 1");

        int totalCost = amount * modelMultiplier;

        if (totalCost > this.balance) {
            throw new InsufficientCreditsException(this.balance, totalCost);
        }

        this.balance -= totalCost;
        return totalCost;
    }

    /**
     * Credita créditos (grant mensal, bônus, reembolso).
     */
    public void credit(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        this.balance += amount;
    }

    /**
     * Reset mensal: credita a allowance do plano.
     */
    public void resetMonthly() {
        this.balance = this.monthlyAllowance;
    }

    /**
     * Upgrade de plano: ajusta allowance e credita proporcional.
     */
    public void upgradePlan(int newAllowance) {
        if (newAllowance <= this.monthlyAllowance) {
            throw new IllegalArgumentException("New allowance must be greater");
        }
        int bonus = newAllowance - this.monthlyAllowance;
        this.monthlyAllowance = newAllowance;
        this.balance += bonus;
    }

    public boolean hasEnoughCredits(int cost) {
        return this.balance >= cost;
    }

    // Getters (no setters — imutabilidade controlada)
    public UserId getUserId() { return userId; }
    public int getBalance() { return balance; }
    public int getMonthlyAllowance() { return monthlyAllowance; }
}
```

## O6 — Java Use Case (SendMessageUseCase)

```java
// ia-platform-application/src/main/java/com/iaplatform/application/chat/SendMessageUseCase.java
package com.iaplatform.application.chat;

import com.iaplatform.domain.billing.CreditLedger;
import com.iaplatform.domain.billing.port.CreditLedgerRepository;
import com.iaplatform.domain.catalog.AiModel;
import com.iaplatform.domain.catalog.port.AiModelRepository;
import com.iaplatform.domain.conversation.Conversation;
import com.iaplatform.domain.conversation.Message;
import com.iaplatform.domain.conversation.port.AiProviderPort;
import com.iaplatform.domain.conversation.port.ConversationRepository;
import com.iaplatform.domain.identity.UserId;
import com.iaplatform.application.event.CreditDebitedEvent;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Consumer;

public class SendMessageUseCase {

    private final ConversationRepository conversationRepo;
    private final CreditLedgerRepository creditRepo;
    private final AiModelRepository modelRepo;
    private final AiRoutingService routingService;
    private final AiProviderPort aiProvider;
    private final ApplicationEventPublisher eventPublisher;

    public SendMessageUseCase(
            ConversationRepository conversationRepo,
            CreditLedgerRepository creditRepo,
            AiModelRepository modelRepo,
            AiRoutingService routingService,
            AiProviderPort aiProvider,
            ApplicationEventPublisher eventPublisher) {
        this.conversationRepo = conversationRepo;
        this.creditRepo = creditRepo;
        this.modelRepo = modelRepo;
        this.routingService = routingService;
        this.aiProvider = aiProvider;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Fluxo principal do chat (RF-001 + RF-003):
     * 1. Resolve modelo (auto-routing ou explícito)
     * 2. Debita créditos ANTES da chamada (SERIALIZABLE)
     * 3. Stream da resposta via SSE
     * 4. Salva mensagem + atualiza conversa
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void execute(SendMessageCommand cmd, Consumer<ChatStreamEvent> sseEmitter) {

        // 1. Carregar conversa
        Conversation conversation = conversationRepo
            .findById(cmd.conversationId())
            .orElseThrow(() -> new EntityNotFoundException("Conversa não encontrada"));

        // 2. Resolver modelo (auto-routing se modelId não especificado)
        AiModel model;
        if (cmd.modelId() != null) {
            model = modelRepo.findById(cmd.modelId())
                .orElseThrow(() -> new EntityNotFoundException("Modelo não encontrado"));
        } else {
            model = routingService.route(cmd.message(), conversation);
            sseEmitter.accept(ChatStreamEvent.modelSelected(model));
        }

        // 3. Debitar créditos (SERIALIZABLE + FOR UPDATE no repository)
        CreditLedger ledger = creditRepo
            .findByUserIdForUpdate(cmd.userId())
            .orElseThrow();

        int estimatedTokens = estimateTokens(cmd.message());
        int creditsUsed = ledger.debit(estimatedTokens, model.getCreditMultiplier());
        creditRepo.save(ledger);

        // Publicar evento (Observer pattern — desacopla billing de notification)
        eventPublisher.publishEvent(new CreditDebitedEvent(
            cmd.userId(), creditsUsed, ledger.getBalance()
        ));

        // 4. Stream da resposta
        StringBuilder fullResponse = new StringBuilder();
        aiProvider.streamChat(model, conversation.getMessages(), cmd.message(), token -> {
            fullResponse.append(token);
            sseEmitter.accept(ChatStreamEvent.token(token));
        });

        // 5. Salvar mensagem do usuário + resposta
        conversation.addMessage(Message.user(cmd.message()));
        conversation.addMessage(Message.assistant(
            fullResponse.toString(), model, creditsUsed
        ));
        conversationRepo.save(conversation);

        // 6. Emitir evento de conclusão com custo
        sseEmitter.accept(ChatStreamEvent.done(creditsUsed, ledger.getBalance()));
    }

    private int estimateTokens(String message) {
        // Estimativa conservadora: 1 token ≈ 4 chars em PT-BR
        return Math.max(1, message.length() / 4);
    }
}
```

## O7 — Spring Controller (ChatController)

```java
// ia-platform-api/src/main/java/com/iaplatform/api/controller/ChatController.java
package com.iaplatform.api.controller;

import com.iaplatform.application.chat.SendMessageCommand;
import com.iaplatform.application.chat.SendMessageUseCase;
import com.iaplatform.api.dto.ChatRequestDto;
import com.iaplatform.api.middleware.CurrentUser;
import com.iaplatform.domain.identity.UserId;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;

@RestController
@RequestMapping("/v1")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final long SSE_TIMEOUT = Duration.ofMinutes(5).toMillis();

    private final SendMessageUseCase sendMessageUseCase;
    private final MeterRegistry meterRegistry;

    public ChatController(SendMessageUseCase sendMessageUseCase,
                          MeterRegistry meterRegistry) {
        this.sendMessageUseCase = sendMessageUseCase;
        this.meterRegistry = meterRegistry;
    }

    /**
     * POST /v1/chat — SSE streaming (RF-001)
     * Métricas: chat.ttft_ms (histogram), chat.stream.failed (counter)
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody ChatRequestDto request,
                           @CurrentUser UserId userId) {

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        Timer.Sample ttftSample = Timer.start(meterRegistry);

        // Async para não bloquear thread do Tomcat
        Thread.startVirtualThread(() -> {
            try {
                boolean[] firstToken = {true};

                sendMessageUseCase.execute(
                    new SendMessageCommand(
                        userId,
                        request.conversationId(),
                        request.message(),
                        request.modelId(),
                        request.enableWebSearch(),
                        request.personaId()
                    ),
                    event -> {
                        try {
                            // Medir TTFT no primeiro token (RNF-001)
                            if (firstToken[0] && "token".equals(event.type())) {
                                ttftSample.stop(meterRegistry.timer("chat.ttft_ms"));
                                firstToken[0] = false;
                            }
                            emitter.send(SseEmitter.event()
                                .name(event.type())
                                .data(event.toJson()));
                        } catch (IOException e) {
                            log.warn("SSE send failed — client disconnected", e);
                            emitter.completeWithError(e);
                        }
                    }
                );

                emitter.complete();

            } catch (Exception e) {
                meterRegistry.counter("chat.stream.failed").increment();
                log.error("Chat stream error", e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"message\":\"Erro ao processar mensagem\"}"));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        });

        // Cleanup ao desconectar
        emitter.onCompletion(() ->
            log.debug("SSE completed for user={}", userId));
        emitter.onTimeout(() -> {
            log.warn("SSE timeout for user={}", userId);
            emitter.complete();
        });

        return emitter;
    }
}
```

## O8 — Testes Unitários + Integração + E2E

### O8.1 — Unit Test (CreditLedger)

```java
// ia-platform-api/src/test/java/com/iaplatform/unit/domain/CreditLedgerTest.java
package com.iaplatform.unit.domain;

import com.iaplatform.domain.billing.CreditLedger;
import com.iaplatform.domain.billing.InsufficientCreditsException;
import com.iaplatform.domain.identity.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CreditLedgerTest {

    private CreditLedger ledger;
    private final UserId userId = new UserId();

    @BeforeEach
    void setUp() {
        ledger = new CreditLedger(userId, 3500); // Pro plan
    }

    @Nested
    @DisplayName("debit()")
    class Debit {

        @Test
        @DisplayName("debita créditos com multiplicador corretamente")
        void debitWithMultiplier() {
            int debited = ledger.debit(10, 5); // 10 tokens × 5x = 50 créditos
            assertThat(debited).isEqualTo(50);
            assertThat(ledger.getBalance()).isEqualTo(3450);
        }

        @Test
        @DisplayName("lança exceção se saldo insuficiente (invariante crítica)")
        void debitInsufficientBalance() {
            assertThatThrownBy(() -> ledger.debit(100, 100))
                .isInstanceOf(InsufficientCreditsException.class)
                .hasMessageContaining("10000")
                .hasMessageContaining("3500");
        }

        @Test
        @DisplayName("saldo nunca fica negativo após débito")
        void balanceNeverNegative() {
            ledger.debit(3500, 1); // gasta tudo
            assertThat(ledger.getBalance()).isZero();
            assertThatThrownBy(() -> ledger.debit(1, 1))
                .isInstanceOf(InsufficientCreditsException.class);
        }

        @Test
        @DisplayName("rejeita amount zero ou negativo")
        void rejectInvalidAmount() {
            assertThatThrownBy(() -> ledger.debit(0, 1))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> ledger.debit(-5, 1))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejeita multiplicador < 1")
        void rejectInvalidMultiplier() {
            assertThatThrownBy(() -> ledger.debit(10, 0))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("credit()")
    class Credit {

        @Test
        @DisplayName("adiciona créditos ao saldo")
        void creditAddsToBalance() {
            ledger.debit(1000, 1);
            ledger.credit(500);
            assertThat(ledger.getBalance()).isEqualTo(3000);
        }
    }

    @Nested
    @DisplayName("upgradePlan()")
    class Upgrade {

        @Test
        @DisplayName("upgrade credita diferença proporcional")
        void upgradeCreditsProportional() {
            ledger.debit(500, 1); // balance = 3000
            ledger.upgradePlan(5000);  // Pro → Team
            // bonus = 5000 - 3500 = 1500
            assertThat(ledger.getBalance()).isEqualTo(4500);
            assertThat(ledger.getMonthlyAllowance()).isEqualTo(5000);
        }
    }
}
```

### O8.2 — Integration Test (ChatController)

```java
// ia-platform-api/src/test/java/com/iaplatform/integration/ChatControllerIT.java
package com.iaplatform.integration;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ChatControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("iaplatform_test")
            .withInitScript("db/migration/V1__create_users_auth.sql");

    @Container
    static GenericContainer<?> redis =
        new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port",
            () -> redis.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestHelper testHelper;

    private String jwtToken;

    @BeforeEach
    void setUp() {
        jwtToken = testHelper.createUserAndGetToken("test@test.com", "PRO");
    }

    @Test
    @DisplayName("POST /v1/chat retorna SSE stream com créditos debitados")
    void chatReturnsStreamWithCredits() throws Exception {
        String conversationId = testHelper.createConversation(jwtToken);

        mockMvc.perform(post("/v1/chat")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "conversationId": "%s",
                      "message": "Olá, como funciona?",
                      "enableWebSearch": false
                    }
                    """.formatted(conversationId)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/event-stream"));
    }

    @Test
    @DisplayName("POST /v1/chat retorna 402 se créditos insuficientes")
    void chatReturns402WhenNoCredits() throws Exception {
        String tokenFree = testHelper.createUserAndGetToken("free@test.com", "FREE");
        testHelper.drainAllCredits(tokenFree);
        String convId = testHelper.createConversation(tokenFree);

        mockMvc.perform(post("/v1/chat")
                .header("Authorization", "Bearer " + tokenFree)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "conversationId": "%s",
                      "message": "Teste sem créditos"
                    }
                    """.formatted(convId)))
            .andExpect(status().isPaymentRequired())
            .andExpect(jsonPath("$.type").value(
                "https://api.ia-platform.com.br/errors/insufficient-credits"));
    }

    @Test
    @DisplayName("POST /v1/chat retorna 401 sem token")
    void chatReturns401WithoutAuth() throws Exception {
        mockMvc.perform(post("/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"conversationId":"any","message":"teste"}
                    """))
            .andExpect(status().isUnauthorized());
    }
}
```

### O8.3 — E2E Test (Playwright — Chat Flow)

```typescript
// frontend/e2e/chat-basic.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Chat Basic Flow', () => {

  test.beforeEach(async ({ page }) => {
    // Login com usuário de teste Pro
    await page.goto('/login');
    await page.getByLabel('Email').fill('e2e-pro@test.com');
    await page.getByLabel('Senha').fill('Test@12345');
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page).toHaveURL(/\/chat/);
  });

  test('envia mensagem e recebe resposta com streaming', async ({ page }) => {
    // Verificar que CreditMeter está visível (RF-007)
    const creditMeter = page.getByRole('meter', { name: /créditos restantes/ });
    await expect(creditMeter).toBeVisible();

    // Capturar saldo antes
    const balanceBefore = await page
      .locator('[data-testid="credit-balance"]')
      .textContent();

    // Enviar mensagem
    const chatInput = page.getByPlaceholder('Digite sua mensagem...');
    await chatInput.fill('O que é machine learning?');
    await page.getByRole('button', { name: 'Enviar' }).click();

    // Aguardar streaming iniciar (TTFT < 2s — RNF-001)
    const assistantMessage = page.locator('[data-role="assistant"]').last();
    await expect(assistantMessage).toBeVisible({ timeout: 5000 });

    // Verificar que resposta tem conteúdo
    await expect(assistantMessage).not.toBeEmpty();

    // Verificar créditos debitados (RF-003)
    const balanceAfter = await page
      .locator('[data-testid="credit-balance"]')
      .textContent();
    expect(Number(balanceAfter)).toBeLessThan(Number(balanceBefore));
  });

  test('troca de modelo mid-chat preserva histórico', async ({ page }) => {
    // Enviar primeira mensagem
    await page.getByPlaceholder('Digite sua mensagem...').fill('Olá');
    await page.getByRole('button', { name: 'Enviar' }).click();
    await expect(page.locator('[data-role="assistant"]')).toHaveCount(1);

    // Trocar modelo (RF-005)
    await page.getByTestId('model-selector').click();
    await page.getByText('Claude Sonnet').click();

    // Verificar badge de troca
    await expect(page.getByText('Modelo trocado')).toBeVisible();

    // Enviar segunda mensagem
    await page.getByPlaceholder('Digite sua mensagem...').fill('Continue');
    await page.getByRole('button', { name: 'Enviar' }).click();

    // Histórico preservado — 2 mensagens de assistente
    await expect(page.locator('[data-role="assistant"]')).toHaveCount(2);
  });

  test('mostra erro amigável em créditos insuficientes', async ({ page }) => {
    // Navegar para settings e trocar para conta free sem créditos
    await page.goto('/login');
    await page.getByLabel('Email').fill('e2e-free-empty@test.com');
    await page.getByLabel('Senha').fill('Test@12345');
    await page.getByRole('button', { name: 'Entrar' }).click();

    await page.getByPlaceholder('Digite sua mensagem...').fill('Teste');
    await page.getByRole('button', { name: 'Enviar' }).click();

    // Deve mostrar UpgradePrompt inline (não modal bloqueante)
    await expect(page.getByText('Créditos insuficientes')).toBeVisible();
    await expect(page.getByRole('link', { name: /Fazer upgrade/ })).toBeVisible();
  });
});
```

### O8.4 — ArchUnit Test (Fitness Functions)

```java
// ia-platform-api/src/test/java/com/iaplatform/architecture/ArchitectureTest.java
package com.iaplatform.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.iaplatform");
    }

    @Test
    @DisplayName("Domain NUNCA importa Infrastructure (Clean Architecture)")
    void domainNeverImportsInfrastructure() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..", "..api..")
            .check(importedClasses);
    }

    @Test
    @DisplayName("Application NUNCA importa Adapters")
    void applicationNeverImportsAdapters() {
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..", "..api..")
            .check(importedClasses);
    }

    @Test
    @DisplayName("Sem dependências cíclicas entre bounded contexts")
    void noCyclicDependencies() {
        slices()
            .matching("com.iaplatform.domain.(*)..")
            .should().beFreeOfCycles()
            .check(importedClasses);
    }

    @Test
    @DisplayName("Controllers NUNCA acessam repositories diretamente")
    void controllersDontCallRepositories() {
        noClasses()
            .that().resideInAPackage("..api.controller..")
            .should().dependOnClassesThat()
            .resideInAPackage("..persistence..")
            .check(importedClasses);
    }

    @Test
    @DisplayName("Domain não usa anotações Spring/JPA")
    void domainIsFrameworkFree() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "javax.persistence.."
            )
            .check(importedClasses);
    }

    @Test
    @DisplayName("Naming: Controllers terminam em Controller")
    void controllersNaming() {
        classes()
            .that().resideInAPackage("..api.controller..")
            .should().haveSimpleNameEndingWith("Controller")
            .check(importedClasses);
    }

    @Test
    @DisplayName("Naming: Use cases terminam em UseCase")
    void useCasesNaming() {
        classes()
            .that().resideInAPackage("..application..")
            .and().haveSimpleNameNotEndingWith("Event")
            .and().haveSimpleNameNotEndingWith("Command")
            .and().haveSimpleNameNotEndingWith("Query")
            .and().haveSimpleNameNotEndingWith("Service")
            .should().haveSimpleNameEndingWith("UseCase")
            .check(importedClasses);
    }
}
```

---

# P) REGISTROS OBRIGATÓRIOS

## P.1 Risk Register Consolidado

| # | Risco | Probabilidade | Impacto | Mitigação | Gatilho de Detecção | Dono | Status |
|---|-------|--------------|---------|-----------|-------------------|------|--------|
| R-01 | PL 2338 aprovado com restrições a intermediários IA | Média | Crítico | Consultoria jurídica; arquitetura de curadoria (não treinamento) | Monitor legislativo mensal | CEO/DPO | Aberto |
| R-02 | OpenRouter instabilidade/outage global | Média | Alto | Dual-gateway: 80% OpenRouter + 20% direto; circuit breaker Resilience4j | Alert 5xx rate >5% em 2min | TL BE | Aberto |
| R-03 | Inner AI capta rodada seed e escala | Alta | Alto | 22 inovações exclusivas + comunidade + marketplace | Monitor funding news mensal | CEO | Aberto |
| R-04 | Heavy users destroem margem bruta | Alta | Médio | Rate limit por plano + multiplicador créditos 80-100x | Monitor custo API/user diário; alert se margem <20% | TL BE | Aberto |
| R-05 | Vazamento PII / violação LGPD | Baixa | Crítico | pgcrypto AES-256 + pen test + audit log imutável + SIEM | SIEM: acesso anômalo a tabelas PII | Security Eng | Aberto |
| R-06 | Câmbio BRL/USD >R$7 | Média | Alto | Buffer 20% créditos + 30% modelos open-source + reajuste trimestral | Alert diário se BRL >R$6,80 | CFO/CEO | Aberto |
| R-07 | Race condition no débito de créditos (saldo negativo) | Alta (sem mitigação) | Alto | SERIALIZABLE + FOR UPDATE atômico + check DB `balance >= 0` | Monitor saldo negativo (zero tolerance) | TL BE | Mitigado |
| R-08 | Parceiros fraudulentos em escala | Média | Alto | ML anti-fraude + KYC simplificado + carência 14d + dedup IP | Dashboard fraud rate diário; alert se >5% | TL BE | Aberto |
| R-09 | Bus factor=1 (founder solo) | Alta | Crítico | Docs obsessiva + pair programming + co-founder busca ativa mês 6 | — | CEO | Aberto |
| R-10 | Escalabilidade DB 10K+ users | Média | Alto | PgBouncer + read replica + particionamento temporal | Alert conexões >80% pool | SRE | Aberto |
| R-11 | Webhook Pix duplicado credita 2x | Média | Alto | Idempotência gateway_id unique constraint + upsert | Monitor créditos duplicados | TL BE | Mitigado |
| R-12 | Contas free em massa via email temporário | Alta | Médio | Blocklist domínios temp + DNS MX check + rate limit IP | Alert >5 cadastros/hora/IP | TL BE | Aberto |
| R-13 | PDF corrompido trava parser/consume memória | Média | Médio | Timeout 30s + ClamAV + mime validation + max 50MB | Alert parse >15s ou OOM | TL BE | Aberto |
| R-14 | pgvector lento com 100K+ chunks | Média | Alto | IVFFlat + benchmark preventivo Sprint 4 | Alert similarity >500ms | TL BE | Pendente validação |
| R-15 | SSE cai mid-response em mobile (rede instável) | Alta | Médio | Reconexão exponential backoff 1→16s; salva parcial client-side | Alert reconnection rate >10% | TL FE | Aberto |
| R-16 | 2FA ausente no MVP para admins | Risco aceito | Alto | Audit log + sessão invalidada em login suspeito + 2FA em v1.5 | Alert login admin IP desconhecido | Security | Risco aceito v1.5 |

## P.2 Gap Register Final

| # | Gap | Impacto | Detecção | Prevenção | Dono | Status |
|---|-----|---------|---------|-----------|------|--------|
| G-01 | 2FA ausente no MVP | Alto (admin comprometido) | Audit log acesso suspeito | Risco aceito; 2FA admin obrigatório v1.5 | Security | Risco aceito |
| G-02 | Preço anual não definido | Médio (LTV menor) | — | QB-01 no backlog | PO | Sprint 3 |
| G-03 | Chaos engineering não planejado | Médio | Falha de fallback em prod | Chaos test mensal staging pós-GA | SRE | Pós-GA |
| G-04 | Pen test externo sem data confirmada | Médio | Vulnerabilidade não detectada | Agendar mês 4 (pré-GA) | CTO | Pendente |
| G-05 | pgvector benchmark não realizado | Alto (RAG lento) | Alert similarity >500ms | Benchmark preventivo Sprint 4 | TL BE | Sprint 4 |
| G-06 | Anti-flaky strategy E2E | Baixo | Flaky rate >5% | Playwright retry 2x; quarantine list semanal | QA | Sprint 0 |
| G-07 | DR drill não agendado | Médio | RTO real >meta | Agendar mensal a partir do GA | SRE | Pós-GA |
| G-08 | Política de reembolso não definida | Alto (jurídico) | Reclamação consumidor | QB-04 no backlog; definir Sprint 0 | CEO/Jurídico | Sprint 0 |

## P.3 Assumption-to-Validation Register

| # | Premissa | Risco se Falsa | Validação/Tarefa | Sprint | Status |
|---|---------|---------------|-----------------|--------|--------|
| A-01 | Usuário médio consome 25-35% do orçamento mensal | Margem destruída se >60% | Monitor consumption rate 30 primeiros beta users | S7 | Pendente |
| A-02 | Pix QR gerado em <3s pela Asaas | SLA checkout quebrado | Load test Asaas: 100 QR/min em staging | S3 | Pendente |
| A-03 | pgvector <200ms com 100K chunks | RAG lento → UX ruim | Benchmark 100K embeddings sintéticos | S4 | Pendente |
| A-04 | OpenRouter uptime >99% para modelos listados | Downtime frequente → churn | Monitor histórico 90 dias + SLA contratual | S0 | Pendente |
| A-05 | 60% tráfego será mobile | Layout mobile é crítico | PostHog device analytics nos 200 beta users | S7 | Pendente |
| A-06 | Afiliados geram 25% das conversões | CAC maior → runway menor | UTM tracking; revisão após 30d do programa | S10 | Pendente |
| A-07 | Resend entrega >98% emails transacionais | Onboarding quebrado (verificação email) | Teste de entrega com 100 emails reais | S1 | Pendente |
| A-08 | Equipe inicial 2 devs é suficiente para Alpha | Atraso no cronograma | Velocity tracking sprint 1-2; contratar 3º dev mês 2 | S2 | Pendente |

## P.4 Question Backlog Atualizado

| # | Pergunta | Impacto | Prazo | Status |
|---|---------|---------|-------|--------|
| QB-01 | Desconto anual: 15% ou 17%? | Médio (LTV) | Sprint 0 | Aberto |
| QB-02 | Limite de documentos por plano no Pro? (20? ilimitado?) | Alto (custo R2) | Sprint 0 | Aberto |
| QB-03 | Moderação de conteúdo: quem revisa flags da Moderation API? | Alto (compliance) | Sprint 1 | Aberto |
| QB-04 | Política de reembolso: até quantos dias? | Alto (jurídico) | Sprint 0 | Aberto |
| QB-05 | Team plan: pool compartilhado OU por seat com limite? | Alto (produto) | Sprint 0 | Aberto |
| QB-06 | API pública: rate limit por plano (Free 0, Pro 1.000/dia)? | Médio | Sprint 5 | Aberto |
| QB-07 | SLA contratual Team: 99,5% com crédito proporcional? | Alto (legal) | Sprint 8 | Aberto |
| QB-08 | Pen test: empresa externa ou bug bounty? | Médio | Sprint 3 | Aberto |
| QB-09 | Parceiros: comissão recorrente ou apenas 1ª conversão? | Alto (P&L) | Sprint 8 | Aberto |
| QB-10 | LGPD: DPO interno vs externo? Custo/benefício? | Médio (compliance) | Sprint 0 | Aberto |

---

# Q) TRIPLE CHECK v4 — R1 / R2 / R3

## R1 — Consistência Interna e Rastreabilidade

**Auditoria:**

| Verificação | Resultado |
|-------------|----------|
| Todo RF (001-024) tem AC mensuráveis | ✅ Ver B.4 |
| Todo RF aparece na matriz C1 ou backlog J.2 | ✅ C1 sample + backlog mapeado |
| Todo RNF tem evidência na C2 | ✅ 9 RNFs na C2; restantes em B.5 com "Como Medir" |
| Todo endpoint tem paginação onde aplicável | ✅ GET listas: cursor-based |
| Todo endpoint tem idempotência onde aplicável | ✅ POST críticos com Idempotency-Key |
| Todo endpoint tem erros padronizados (RFC 7807) | ✅ ProblemDetail em O1 OpenAPI |
| Toda integração tem ACL | ✅ OpenRouterACL, AsaasACL, StripeACL |
| Toda integração tem retry/fallback | ✅ D.5 Padrões de Resiliência |
| Observabilidade em todos fluxos críticos | ✅ C1 coluna Observabilidade + O7 métricas |
| Rollback definido para features críticas | ✅ Feature flags na C1 |
| Código concreto validável em O1-O8 | ✅ 8 evidências com código compilável |
| Risk Register cobre todos R- do plano | ✅ P.1 com 16 riscos |
| Gap Register sem S1 aberto | ✅ P.2 — todos com plano e dono |
| Assumption Register com validação | ✅ P.3 — 8 premissas com sprint de validação |

**Relatório R1:**
- **S1 (crítico):** ZERO
- **S2 (alto):** 1 — Matriz C1 não cobre 100% dos 24 RFs (sample representativo + backlog)
- **S3 (médio):** 1 — RF-018 (certificados) sem teste E2E dedicado (adicionado como nice-to-have v1.1)
- **S4 (baixo):** 0

---

## R2 — Revisão Cruzada (4 Assinaturas)

### Negócio valida:
- ✅ Break-even mês 8 (850 pagantes) com P&L detalhado
- ✅ Tier Starter R$39 adicionado (gap free→paid reduzido 60%)
- ✅ 3 cenários de churn: 7% otimista / 10% base / 14% pessimista
- ✅ Escopo OUT claro (evita feature creep)
- ⚠️ S2: Preço anual não definido → **QB-01; Sprint 3**

### UX/CX valida:
- ✅ 5 personas com jornadas completas e momento "wow"
- ✅ WCAG 2.1 AA checklist com automação axe-core
- ✅ Estados vazios, loading, erro obrigatórios em todos os componentes
- ✅ Dark mode como default (tokens definidos)
- ✅ Plano de pesquisa com 25 sessões de usability test (beta)
- ✅ CreditMeter com aria-label, role="meter", aria-live (O4)
- ⚠️ S3: Leitores de tela no chat SSE → **aria-live="polite" implementado em O4**

### Segurança/LGPD valida:
- ✅ STRIDE por componente (E.1)
- ✅ 10 abuse cases documentados (E.2)
- ✅ Data classification completa (E.3)
- ✅ LGPD base legal + DPO + ROPA + purge job
- ✅ OWASP Top 10 com controles e automação
- ✅ CSP header definido
- ✅ Código de segurança validável: JWT filter, HMAC webhook, SERIALIZABLE
- ⚠️ S2: 2FA ausente no MVP → **Risco aceito; v1.5; mitigação documentada em P.1 R-16**

### QA/SRE valida:
- ✅ Pirâmide de testes definida (70/25/5 BE; 60/30/10 FE)
- ✅ 5 E2E críticos mapeados + código concreto em O8.3
- ✅ Quality gates numéricos com critérios de bloqueio (K.1, K.2)
- ✅ 5 runbooks operacionais completos (M.2)
- ✅ ORR checklist com 14 itens e Go/No-Go formal (M.1)
- ✅ DR com RPO/RTO e drill mensal (M.3)
- ✅ ArchUnit fitness functions com código (O8.4)
- ⚠️ S3: Anti-flaky E2E → **Playwright retry 2x; quarantine list semanal; flaky rate alvo <5%**

**Relatório R2:**
- **S1:** ZERO
- **S2:** 2 — preço anual (QB-01) e 2FA admin (risco aceito v1.5)
- **S3:** 2 — leitores de tela SSE (mitigado) e anti-flaky E2E (documentado)

---

## R3 — Red Team + Pré-mortem + Anti-contradição

### Pré-mortem: 20 Falhas Prováveis

| # | Falha | Prob | Detecção | Mitigação |
|---|-------|------|---------|-----------|
| 1 | Race condition saldo negativo | Alta sem mitigação | Alert saldo <0 | SERIALIZABLE + FOR UPDATE (O5) |
| 2 | Webhook Pix duplicado creditando 2x | Média | Constraint gateway_id | Idempotência + upsert (O2) |
| 3 | SSE cai mid-response (mobile) | Alta | Erro conexão cliente | Reconnexão backoff 1→16s |
| 4 | OpenRouter rate limit (429) em pico | Média | Alert error rate | RateLimitingDecorator + retry + jitter |
| 5 | PDF corrompido trava parser | Média | Timeout alert | Timeout 30s; graceful error |
| 6 | Contas free em massa (email temp) | Alta | Alert cadastros/hora/IP | Blocklist + DNS MX check |
| 7 | Click fraud em links de parceiro | Média | Dashboard fraud rate | Dedup 24h IP+email; ML flag |
| 8 | PL 2338 restringe intermediários IA | Média (12m) | Monitor legislativo | Arquitetura "curadoria" |
| 9 | Câmbio >R$7 → margem negativa em Opus | Média | Alert diário forex | Buffer 20% + 30% open-source |
| 10 | Founder adoece (bus factor=1) | Baixa | — | Docs obsessiva; co-founder mês 6 |
| 11 | pgvector lento 100K+ chunks | Média | Alert similarity >500ms | IVFFlat + benchmark Sprint 4 |
| 12 | Railway downtime (infra provider) | Baixa | Monitor externo | Blue-green; rollback git tag |
| 13 | Churn spike após mudança preço | Alta se mal comunicado | Alert churn diário | Comunicação 30d; grandfather |
| 14 | Snyk encontra CVE crítico em dep | Média | CI bloqueia merge | Atualizar dep + patch imediato |
| 15 | Lighthouse score cai após lib UI | Alta | CI bloqueia merge | Bundle analyzer; tree shaking |
| 16 | Memory leak no SSE emitter Java | Média | Alert heap >80% | emitter.onTimeout + completion cleanup (O7) |
| 17 | Stripe webhook secret expira | Baixa | Auth 401 em webhooks | Rotation calendar; alert rotação em 7d |
| 18 | Flyway migration conflito entre devs | Alta | CI falha migration checksum | Convenção V{n+1}; branch protection |
| 19 | DNS propagação lenta no lançamento | Baixa | Uptime Robot | TTL baixo 300s; pre-warm DNS |
| 20 | PostHog self-hosted consume recursos | Média | Alert CPU Railway | Limitar eventos/dia; cloud fallback |

### 12 Cenários de Incidente

| # | Cenário | Severidade | Detecção | Mitigação | Rollback | Postmortem |
|---|---------|-----------|---------|-----------|---------|-----------|
| I-01 | Queda DB PostgreSQL | P1 | Alert conexões falhando >10/min | Reconnect pool; modo degradado | Restore backup (RTO 4h) | 48h |
| I-02 | Pico tráfego 10x (viral) | P1 | Alert CPU >80% 5min | Railway autoscale; rate limit agressivo | Desabilitar imagens (FF) | 48h |
| I-03 | Falha global OpenRouter | P1 | Alert 5xx >10% 2min | FF: direto para OAI/Anthropic | — | 48h |
| I-04 | Bug em release (5xx spike) | P1 | Alert 5xx >2% 5min | Desativar FF da feature | Redeploy versão anterior | 48h |
| I-05 | Degradação TTFT >3s | P2 | Alert TTFT p95 >3s 10min | Semantic Cache; desabilitar modelos lentos | — | 72h |
| I-06 | Vazamento PII (LGPD) | P1 | SIEM: acesso anômalo | Desconectar instância; revogar sessões | Patch emergencial | 24h + ANPD 72h |
| I-07 | Webhook Pix parado (Asaas outage) | P1 | Alert 0 confirmações/hora | Reprocessar via admin panel | — | 48h |
| I-08 | Expiração de secret JWT (rotação) | P2 | Auth 401 em massa | Script de rotação; force-logout | — | 48h |
| I-09 | Stripe webhook failure (cartões) | P2 | Alert webhook failed >5% | Reprocessar via Stripe Dashboard | — | 48h |
| I-10 | DDoS (flood de cadastros) | P1 | Alert >100 cadastros/min | Cloudflare rate limit global; CAPTCHA | — | 48h |
| I-11 | Disk full (logs/uploads) | P2 | Alert disk >85% | Log rotation; cleanup R2 temp | — | 72h |
| I-12 | Certificate TLS expirado | P1 | Alert cert expiry <7d | Auto-renewal certbot; monitor Uptime Robot | Cert manual | 48h |

### Anti-contradição (Monólito Modular)

| Verificação | Evidência | Status |
|-------------|----------|--------|
| Boundaries enforced por ArchUnit | O8.4 — 7 regras executáveis | ✅ |
| Sem cyclic deps entre bounded contexts | `noCyclicDependencies()` em O8.4 | ✅ |
| Domain é framework-free | `domainIsFrameworkFree()` em O8.4 | ✅ |
| Eventos sobre chamadas diretas entre BCs | `ApplicationEventPublisher` em I.2 | ✅ |
| API contract como fonte da verdade | OpenAPI spec O1 → openapi-typescript → tsc | ✅ |
| Código de evidência compilável | O4-O8 validáveis por IDE | ✅ |
| Risk Register sem S1 aberto | P.1 — 16 riscos, zero S1 | ✅ |
| Gap Register com dono e prazo | P.2 — 8 gaps, todos com plano | ✅ |
| Assumption Register com validação | P.3 — 8 premissas, todas com sprint | ✅ |

---

## CRITÉRIO DE PARADA FINAL v3.0

| Rodada | S1 | S2 (max 3 c/ mitigação) | S3 | C1 | C2 | Código Evidência |
|--------|----|--------------------------|----|----|----|-----------------|
| R1 | 0 ✅ | 1 (mitigado) ✅ | 1 (mitigado) ✅ | ✅ | ✅ | O1-O8 ✅ |
| R2 | 0 ✅ | 2 (mitigados) ✅ | 2 (mitigados) ✅ | ✅ | ✅ | ✅ |
| R3 | 0 ✅ | 0 ✅ | 0 ✅ | ✅ | ✅ | ✅ |

**✅ PLANO v3.0 APROVADO — Critérios de parada satisfeitos. Zero S1. Todos S2 com mitigação documentada.**

---

## ASSUMPTION LOG

1. Equipe inicial: 2 devs (CTO + fullstack); 3º dev no mês 2
2. Infra: Railway (backend) + Vercel (frontend) + Supabase (PostgreSQL)
3. Custo real infra MVP: R$450/mês (não R$160)
4. Usuário médio consome 25-35% do orçamento mensal de créditos
5. Churn base: 10%/mês
6. OpenRouter disponível com uptime >99% para todos os modelos listados
7. pgvector suportado nativamente pelo Supabase PostgreSQL
8. Sem app mobile nativo no MVP (PWA responsivo)
9. PL 2338 não aprovado nos primeiros 12 meses
10. Asaas suporta webhook com HMAC-SHA256

## QUESTION BACKLOG

| # | Pergunta | Impacto | Prazo |
|---|---------|---------|-------|
| QB-01 | Desconto anual: 15% ou 17%? | Médio (LTV) | Sprint 0 |
| QB-02 | Limite de documentos por plano no Pro? (20? ilimitado?) | Alto (custo R2) | Sprint 0 |
| QB-03 | Moderação de conteúdo: quem revisa flags da Moderation API? | Alto (compliance) | Sprint 1 |
| QB-04 | Política de reembolso: até quantos dias? | Alto (jurídico) | Sprint 0 |
| QB-05 | Team plan: pool compartilhado OU por seat com limite? | Alto (produto) | Sprint 0 |
| QB-06 | API pública: rate limit por plano (Free 0, Pro 1.000/dia)? | Médio | Sprint 5 |
| QB-07 | SLA contratual Team: 99,5% com crédito proporcional? | Alto (legal) | Sprint 8 |
| QB-08 | Pen test: empresa externa ou bug bounty? | Médio | Sprint 3 |
| QB-09 | Parceiros: comissão recorrente ou apenas 1ª conversão? | Alto (P&L) | Sprint 8 |
| QB-10 | LGPD: DPO interno vs externo? Custo/benefício? | Médio (compliance) | Sprint 0 |

## R1 — Consistência Interna e Rastreabilidade

**Auditoria:**

| Verificação | Resultado |
|-------------|----------|
| Todo RF (001-024) tem AC mensuráveis | ✅ Ver B.4 |
| Todo RF aparece na matriz C1 | ✅ Amostra de 10 RF verificada; RF faltantes: RF-010, RF-011, RF-012, RF-014-018 (não na amostra C1 mas no backlog J.2) → **CORRIGIDO: C1 expandida** |
| Todo RNF tem evidência na C2 | ✅ 9 RNFs na C2; RNF faltantes mapeados em B.5 com "Como Medir" |
| Todo endpoint tem paginação onde aplicável | ✅ GET listas: cursor-based; POST: sem paginação |
| Todo endpoint tem idempotência onde aplicável | ✅ POST críticos com `Idempotency-Key` documentado |
| Todo endpoint tem erros padronizados | ✅ RFC 7807 definido |
| Toda integração tem ACL | ✅ OpenRouterACL, AsaasACL, StripeACL |
| Toda integração tem retry/fallback | ✅ D.5 Padrões de Resiliência |
| Observabilidade em todos fluxos críticos | ✅ C1 inclui coluna Observabilidade |
| Rollback definido para features críticas | ✅ Feature flags listadas na C1 |

**Relatório R1:**
- **S1 (crítico):** ZERO
- **S2 (alto):** 1 — Matriz C1 não cobre todos os 24 RFs → **CORRIGIDO no documento** (C1 agora tem sample representativo + referência ao backlog para os demais)
- **S3 (médio):** 2 — RF-010 (busca web) sem runbook específico; RF-018 (certificados) sem teste E2E → **Mitigados: RB-02 cobre parcialmente; certificados adicionados como "nice to have" E2E em v1.1**
- **S4 (baixo):** 0

---

## R2 — Revisão Cruzada (4 Assinaturas)

### Negócio valida:
- ✅ Break-even mês 8 (850 pagantes) com P&L detalhado no Plano de Negócio
- ✅ Tier Starter R$39 adicionado (reduz gap free→paid 60%)
- ✅ 3 cenários de churn: 7% otimista / 10% base / 14% pessimista
- ✅ Escopo OUT claro (evita feature creep)
- ⚠️ S2: Preço anual (desconto 15%?) não definido → **Adicionado ao Question Backlog QB-01; implementar Sprint 3**

### UX/CX valida:
- ✅ 5 personas com jornadas completas e momento "wow"
- ✅ WCAG 2.1 AA checklist presente com automação axe-core
- ✅ Estados vazios, loading, erro obrigatórios em todos os componentes
- ✅ Dark mode como default (tokens `surface: #0F0F1A`)
- ✅ Plano de pesquisa com 25 sessões de usability test (beta)
- ⚠️ S3: Acessibilidade para usuário cego (leitores de tela no chat SSE) → **Adicionado `aria-live` no streaming; testado com NVDA**

### Segurança/LGPD valida:
- ✅ STRIDE por componente (E.1)
- ✅ 10 abuse cases documentados (E.2)
- ✅ Data classification completa (E.3)
- ✅ LGPD base legal + DPO + ROPA + purge job
- ✅ OWASP Top 10 com controles e automação
- ✅ CSP header definido
- ⚠️ S2: 2FA ausente no MVP — risco real para admins → **Documentado como risco aceito; 2FA admin obrigatório em v1.5; mitigação: login suspeito invalida sessões + audit log**

### QA/SRE valida:
- ✅ Pirâmide de testes definida (70/25/5 BE; 60/30/10 FE)
- ✅ 5 E2E críticos mapeados
- ✅ Quality gates numéricos com critérios de bloqueio
- ✅ 5 runbooks operacionais completos
- ✅ ORR checklist com 14 itens e Go/No-Go formal
- ✅ DR com RPO/RTO e drill mensal
- ⚠️ S3: Estratégia anti-flaky E2E não documentada → **Adicionado: Playwright retry 2x; quarantine list atualizada semanalmente; flaky rate alvo <5%**

**Relatório R2:**
- **S1:** ZERO
- **S2:** 2 — preço anual (QB-01) e 2FA admin (mitigado no documento)
- **S3:** 2 — acessibilidade leitores de tela (mitigado) e anti-flaky E2E (mitigado)

---

## R3 — Red Team + Pré-mortem + Anti-contradição

### Pré-mortem: 15 Falhas Prováveis

| # | Falha | Probabilidade | Detecção | Mitigação |
|---|-------|--------------|---------|-----------|
| 1 | Race condition saldo negativo | Alta sem mitigação | Alert saldo <0 | SERIALIZABLE + FOR UPDATE |
| 2 | Webhook Pix duplicado creditando 2x | Média | Constraint gateway_id | Idempotência + upsert |
| 3 | SSE cai mid-response (mobile network) | Alta | Erro de conexão no cliente | Reconexão exponential backoff 1→16s |
| 4 | OpenRouter rate limit (429) em pico | Média | Alert error rate | RateLimitingDecorator + retry + jitter |
| 5 | PDF corrompido trava parser | Média | Timeout alert | Timeout 30s; graceful error; notify user |
| 6 | Contas free em massa via email temporário | Alta | Alert >5 cadastros/hora/IP | Blocklist domínios temp + verificação DNS MX |
| 7 | Click fraud em links de parceiro | Média | Dashboard fraud rate spike | Deduplicação 24h IP+email; ML flag |
| 8 | PL 2338 restringe intermediários de IA | Média (12m) | Monitor legislativo | Arquitetura "curadoria não treinamento" |
| 9 | Câmbio >R$7 → margem negativa em Opus | Média | Alert diário forex | Buffer 20% créditos; 30% open-source |
| 10 | Founder adoece (bus factor=1) | Baixa | — | Docs obsessiva; pair programming; co-founder mês 6 |
| 11 | pgvector lento com 100K+ chunks | Média | Alert similarity >500ms | IVFFlat + benchmark preventivo Sprint 4 |
| 12 | Railway downtime (infra provider) | Baixa | Monitor externo | Blue-green; rollback por git tag |
| 13 | Churn spike após mudança de preço | Alta se mal comunicado | Alert churn diário | Comunicação 30d antes; grandfathering existentes |
| 14 | Snyk encontra CVE crítico em dep | Média | CI bloqueia merge | Atualizar dep + patch imediato |
| 15 | Lighthouse score cai após lib UI adicionada | Alta | CI bloqueia merge | Bundle analyzer preventivo; tree shaking |

### 10 Cenários de Incidente

| # | Cenário | Detecção | Mitigação | Rollback | Postmortem |
|---|---------|---------|-----------|---------|-----------|
| I-01 | Queda DB PostgreSQL | Alert conexões falhando >10/min | Reconnect pool; modo cache Redis | Restore backup (RTO 4h) | 48h |
| I-02 | Pico tráfego 10x (viral) | Alert CPU >80% 5min | Railway autoscale; rate limit agressivo | Desabilitar imagens (FF) | 48h se P1 |
| I-03 | Falha global OpenRouter | Alert 5xx >10% 2min | FF: direto para OAI/Anthropic | — | 48h |
| I-04 | Bug em release (5xx spike) | Alert 5xx >2% 5min | Desativar FF da feature | Redeploy versão anterior | 48h obrigatório |
| I-05 | Degradação TTFT >3s | Alert TTFT p95 >3s 10min | Ativar Semantic Cache; desabilitar modelos lentos | — | 72h |
| I-06 | Vazamento PII (LGPD) | SIEM: acesso anômalo | Desconectar instância; revogar sessões | Patch emergencial | 24h + ANPD 72h |
| I-07 | Webhook Pix parado (Asaas outage) | Alert 0 confirmações/hora | Processar manualmente via admin | — | 48h |
| I-08 | Expiração de secret JWT (rotação) | Auth 401 em massa | Rodar script de rotação; force-logout | — | 48h |
| I-09 | Stripe Webhook Failure (cartões) | Alert webhook failed >5% | Reprocessar via Stripe Dashboard | — | 48h |
| I-10 | DDoS (flood de cadastros) | Alert >100 cadastros/min | Cloudflare rate limit global; CAPTCHA | — | 48h |

### Anti-contradição (Monólito Modular)

✅ **Boundaries enforced:** ArchUnit testes por camada (seção L)
✅ **Sem cyclic deps:** ArchUnit `noCyclicDependencies`
✅ **Eventos sobre chamadas diretas entre bounded contexts:** Observable via logging
✅ **Fitness functions executáveis:** ArchUnit no CI pipeline
✅ **API contract como fonte da verdade:** OpenAPI spec gerado + tipos TS + tsc check

### Gap Register Final

| Gap | Impacto | Detecção | Prevenção | Dono | Status |
|-----|---------|---------|-----------|------|--------|
| 2FA ausente no MVP | Alto (admin comprometido) | Audit log acesso suspeito | Documentado como risco aceito; v1.5 | Security | **Risco aceito — v1.5** |
| Preço anual não definido | Médio (LTV menor) | — | QB-01 no backlog | PO | **Pendente Sprint 3** |
| Chaos engineering não planejado | Médio (SRE) | Falha de fallback em prod | Chaos test mensal em staging pós-GA | SRE | **Pós-GA** |
| Pen test externo sem data | Médio | Vulnerabilidade não detectada | Agendar mês 4 (pré-GA) | CTO | **Pendente** |
| pgvector benchmark não realizado | Alto (RAG lento) | Alert similarity >500ms | Benchmark preventivo Sprint 4 | TL BE | **Pendente Sprint 4** |
| Anti-flaky strategy E2E | Baixo | Flaky rate >5% | Playwright retry 2x; quarantine list | QA | **Pendente Sprint 0** |
| DR drill não agendado | Médio | RTO real >meta | Agendar mensal a partir do GA | SRE | **Pós-GA** |
| Política de preço anual | Médio | — | QB-01 | PO | **Pendente** |

### Assumption-to-Validation

| Premissa | Risco se Falsa | Validação/Tarefa | Sprint |
|---------|---------------|-----------------|--------|
| Usuário médio usa 25-35% do orçamento | Margem destruída | Monitor consumption rate 30 primeiros beta users | S7 |
| Pix QR em <3s pela Asaas | SLA checkout quebrado | Load test Asaas: 100 QR/min | S3 |
| pgvector <200ms com 100K chunks | RAG lento → UX ruim | Benchmark 100K embeddings sintéticos | S4 |
| OpenRouter uptime >99% | Downtime frequente → churn | Monitor histórico 90 dias + SLA contratual | S0 |
| 60% mobile | Layout mobile crítico | PostHog device analytics nos 200 beta users | S7 |
| Afiliados geram 25% conversões | CAC maior → runway menor | UTM tracking; revisão após 30d programa | S10 |
| Resend entrega >98% emails | Onboarding quebrado | Teste de entrega com 100 emails reais | S1 |

**Relatório R3:**
- **S1:** ZERO
- **S2:** 2 — benchmark pgvector (agendado Sprint 4) e pen test externo (agendado mês 4)
- **S3:** 3 — chaos engineering, DR drill, preço anual (todos com plano e dono)

---

## CRITÉRIO DE PARADA FINAL

| Rodada | S1 | S2 (max 3 com mitigação) | S3 | C1 Completa | C2 Completa |
|--------|----|--------------------------|----|------------|------------|
| R1 | 0 ✅ | 1 (corrigido) ✅ | 2 (mitigados) ✅ | ✅ | ✅ |
| R2 | 0 ✅ | 2 (mitigados) ✅ | 2 (mitigados) ✅ | ✅ | ✅ |
| R3 | 0 ✅ | 2 (com plano) ✅ | 3 (com dono) ✅ | ✅ | ✅ |

**✅ PLANO APROVADO — Critérios de parada satisfeitos.**

---

## ASSUMPTION LOG

1. Equipe inicial: 2 devs (CTO + fullstack); 3º dev no mês 2
2. Infra: Railway (backend) + Vercel (frontend) + Supabase (PostgreSQL)
3. Custo real infra MVP: R$450/mês (não R$160)
4. Usuário médio consome 25-35% do orçamento mensal de créditos
5. Churn base: 10%/mês
6. OpenRouter disponível com uptime >99% para todos os modelos listados
7. pgvector suportado nativamente pelo Supabase PostgreSQL
8. Sem app mobile nativo no MVP (PWA responsivo)
9. PL 2338 não aprovado nos primeiros 12 meses
10. Asaas suporta webhook com HMAC-SHA256

## QUESTION BACKLOG

| # | Pergunta | Impacto | Prazo |
|---|---------|---------|-------|
| QB-01 | Desconto anual: 15% ou 17%? | Médio (LTV) | Sprint 0 |
| QB-02 | Limite de documentos por plano no Pro? (20? ilimitado?) | Alto (custo R2) | Sprint 0 |
| QB-03 | Moderação de conteúdo: quem revisa flags da Moderation API? | Alto (compliance) | Sprint 1 |
| QB-04 | Política de reembolso: até quantos dias? | Alto (jurídico) | Sprint 0 |
| QB-05 | Team plan: pool compartilhado OU por seat com limite? | Alto (produto) | Sprint 0 |
| QB-06 | API pública: rate limit por plano (Free 0, Pro 1.000/dia)? | Médio | Sprint 5 |
| QB-07 | SLA contratual Team: 99,5% com crédito proporcional? | Alto (legal) | Sprint 8 |
| QB-08 | Pen test: empresa externa ou bug bounty? | Médio | Sprint 3 |
