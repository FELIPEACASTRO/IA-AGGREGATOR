# PLANO DE IMPLEMENTAÇÃO COMPLETO

## Plataforma Agregadora de IA Multi-Modelo — Mercado Brasileiro

**Versão:** 1.0
**Data:** Março 2026
**Classificação:** Confidencial
**Stack:** React 19 (Next.js 15) · Java 21 · Spring Boot 3.3 · Maven · PostgreSQL 16

---

## ÍNDICE

1. [Equipe Multidisciplinar](#1-equipe-multidisciplinar)
2. [Visão Arquitetural (C4 Model)](#2-visão-arquitetural-c4-model)
3. [Arquitetura Backend — Java 21 + Spring Boot](#3-arquitetura-backend)
4. [Arquitetura Frontend — React 19 + Next.js 15](#4-arquitetura-frontend)
5. [Banco de Dados — PostgreSQL 16](#5-banco-de-dados)
6. [Design Patterns](#6-design-patterns)
7. [Microservices Patterns](#7-microservices-patterns)
8. [Clean Architecture](#8-clean-architecture)
9. [Clean Code & SOLID](#9-clean-code--solid)
10. [Análise Assintótica (Big O)](#10-análise-assintótica-big-o)
11. [Estratégia de Testes](#11-estratégia-de-testes)
12. [CI/CD e DevOps](#12-cicd-e-devops)
13. [Segurança e LGPD](#13-segurança-e-lgpd)
14. [Plano de Sprints](#14-plano-de-sprints)
15. [Plano de Go-to-Market Técnico](#15-plano-de-go-to-market-técnico)
16. [Métricas e Observabilidade](#16-métricas-e-observabilidade)
17. [Riscos Técnicos e Mitigações](#17-riscos-técnicos-e-mitigações)

---

## 1. EQUIPE MULTIDISCIPLINAR

### 1.1 Organograma por Fase

```
FASE MVP (Meses 1-4) — 8 profissionais
│
├── NEGÓCIO & ESTRATÉGIA
│   ├── Product Owner / CEO (1)
│   │   └── Visão de produto, priorização, stakeholder management
│   └── Business Analyst (1)
│       └── Refinamento de requisitos, critérios de aceitação, validação
│
├── DESIGN & EXPERIÊNCIA
│   ├── UX Designer Senior (1)
│   │   └── Pesquisa, personas, jornadas, wireframes, testes de usabilidade
│   └── UI Designer / Design System Lead (1)
│       └── Design system, componentes visuais, prototipação, acessibilidade
│
├── ENGENHARIA
│   ├── Tech Lead / Arquiteto (1) — CTO
│   │   └── Arquitetura, code review, decisões técnicas, mentoria
│   ├── Backend Developer Senior — Java/Spring (1)
│   │   └── API, domínio, integrações AI, billing, segurança
│   ├── Frontend Developer Senior — React/Next.js (1)
│   │   └── Interface, design system code, streaming, responsividade
│   └── QA Engineer (1)
│       └── Testes automatizados, E2E, performance, cobertura
│
FASE GROWTH (Meses 5-8) — +5 profissionais (total 13)
│
├── + Backend Developer Pleno — Java/Spring (1)
├── + Frontend Developer Pleno — React (1)
├── + DevOps / SRE Engineer (1)
├── + Data Analyst (1)
└── + Customer Success / Suporte PT-BR (1)

FASE SCALE (Meses 9-14) — +5 profissionais (total 18)
│
├── + Backend Developer Pleno (1)
├── + Frontend Developer Pleno (1)
├── + Security Engineer (1)
├── + Product Designer (1)
└── + Marketing Digital (1)

FASE ENTERPRISE (Meses 15+) — +7 profissionais (total 25)
│
├── + Sales B2B (1)
├── + Solutions Architect (1)
├── + Mobile Developer — React Native (1)
├── + ML Engineer — Anti-fraude (1)
├── + Technical Writer (1)
├── + Jurídico / DPO LGPD (1)
└── + People/HR (1)
```

### 1.2 RACI Matrix — Responsabilidades por Módulo

| Módulo | PO | BA | UX | UI | Tech Lead | Backend | Frontend | QA |
|--------|----|----|----|----|-----------|---------|----------|----|
| **auth** | A | C | C | R | A/R | R | R | R |
| **billing** | A | R | C | R | A | R | R | R |
| **chat** | A | C | R | R | A | R | R | R |
| **ai-gateway** | C | C | I | I | A/R | R | I | R |
| **partners** | A | R | C | R | A | R | R | R |
| **content** | A | R | R | R | A | R | R | R |
| **teams** | A | R | C | R | A | R | R | R |
| **analytics** | A | R | C | R | A | R | R | R |

> R = Responsible, A = Accountable, C = Consulted, I = Informed

### 1.3 Rituais Ágeis

| Ritual | Frequência | Duração | Participantes |
|--------|-----------|---------|---------------|
| Daily Standup | Diário | 15min | Engenharia + QA |
| Sprint Planning | Quinzenal (início sprint) | 2h | Todos |
| Sprint Review/Demo | Quinzenal (fim sprint) | 1h | Todos + stakeholders |
| Sprint Retrospective | Quinzenal (fim sprint) | 1h | Todos |
| Backlog Refinement | Semanal (terça) | 1h | PO + BA + Tech Lead + UX |
| Design Review | Semanal (quarta) | 30min | UX + UI + Frontend + PO |
| Architecture Decision | Sob demanda | 1h | Tech Lead + Seniors |
| Security Review | Quinzenal | 30min | Tech Lead + Security (quando houver) |

### 1.4 Ferramentas da Equipe

| Categoria | Ferramenta | Propósito |
|-----------|-----------|-----------|
| Gestão de Projeto | Linear (ou Jira) | Sprints, backlog, epics, user stories |
| Repositório | GitHub | Código, PRs, code review, CI/CD |
| Documentação | Notion | Specs, ADRs, runbooks, onboarding |
| Design | Figma | Protótipos, design system, handoff |
| Comunicação | Discord + Slack | Chat diário + canais por módulo |
| Monitoramento | Grafana + Sentry | Métricas, alertas, error tracking |
| API Docs | Swagger UI | Documentação OpenAPI interativa |

---

## 2. VISÃO ARQUITETURAL (C4 Model)

### 2.1 Nível 1 — Diagrama de Contexto

```
                    ┌──────────────┐
                    │   Usuário    │
                    │  (Browser /  │
                    │   Mobile)    │
                    └──────┬───────┘
                           │ HTTPS
                           ▼
                    ┌──────────────┐
                    │  Plataforma  │
                    │  Agregadora  │◄──── Parceiro (Dashboard)
                    │    de IA     │◄──── Admin (Painel)
                    └──────┬───────┘
                           │
            ┌──────────────┼──────────────┐
            ▼              ▼              ▼
     ┌─────────────┐ ┌──────────┐ ┌──────────────┐
     │ AI Providers │ │ Payment  │ │   External   │
     │  (OpenAI,   │ │ Gateway  │ │  Services    │
     │  Anthropic, │ │ (Stripe, │ │ (Email,      │
     │  Google,    │ │  Asaas)  │ │  Storage,    │
     │  OpenRouter)│ │          │ │  Analytics)  │
     └─────────────┘ └──────────┘ └──────────────┘
```

### 2.2 Nível 2 — Diagrama de Containers

```
┌─────────────────────────────────────────────────────────────────┐
│                        PLATAFORMA                                │
│                                                                  │
│  ┌──────────────────┐    ┌──────────────────────────────────┐   │
│  │   FRONTEND SPA   │    │         BACKEND API              │   │
│  │  Next.js 15 +    │───▶│    Java 21 + Spring Boot 3.3    │   │
│  │  React 19 +      │    │    Maven Multi-Module            │   │
│  │  TypeScript       │◀───│    Clean Architecture            │   │
│  │  (Vercel CDN)    │SSE │    (Railway / AWS ECS)           │   │
│  └──────────────────┘    └──────────┬───────────────────────┘   │
│                                     │                            │
│           ┌─────────────────────────┼─────────────────┐         │
│           ▼                         ▼                 ▼         │
│  ┌─────────────────┐  ┌──────────────────┐  ┌──────────────┐   │
│  │   PostgreSQL 16  │  │   Redis Cache    │  │  Event Bus   │   │
│  │   (Supabase /    │  │   (Upstash)      │  │  (Spring     │   │
│  │    Neon)         │  │                  │  │   Events +   │   │
│  │   + pgvector     │  │  • Session cache │  │   Outbox)    │   │
│  │   + RLS          │  │  • AI response   │  │              │   │
│  │                  │  │    semantic cache │  │              │   │
│  │  8 schemas:      │  │  • Rate limiting │  │              │   │
│  │  auth, billing,  │  │  • Credit balance│  │              │   │
│  │  chat, ai_gw,    │  │                  │  │              │   │
│  │  partners,       │  └──────────────────┘  └──────────────┘   │
│  │  content, teams, │                                            │
│  │  analytics       │                                            │
│  └─────────────────┘                                             │
│                                                                  │
│  ┌──────────────────┐  ┌──────────────────┐                    │
│  │ Object Storage   │  │  Job Scheduler   │                    │
│  │ (Cloudflare R2)  │  │  (Spring Quartz  │                    │
│  │                  │  │   / @Scheduled)  │                    │
│  │ • Documents      │  │                  │                    │
│  │ • Generated imgs │  │ • Commission     │                    │
│  │ • Avatars        │  │   carência 7d    │                    │
│  │ • Exports        │  │ • Credit reset   │                    │
│  └──────────────────┘  │ • Analytics agg  │                    │
│                        │ • Cleanup LGPD   │                    │
│                        └──────────────────┘                    │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 Nível 3 — Diagrama de Componentes (Backend)

```
┌─────────────────────────────────────────────────────────────────┐
│                    SPRING BOOT APPLICATION                       │
│                                                                  │
│  ┌──────────────── PRESENTATION LAYER ────────────────────┐     │
│  │  AuthController  │ ChatController  │ BillingController  │     │
│  │  PartnerController │ ContentController │ TeamController │     │
│  │  AnalyticsController │ AdminController │ WebhookController│   │
│  │  GlobalExceptionHandler │ CorsConfig │ SecurityConfig   │     │
│  └────────────────────────┬───────────────────────────────┘     │
│                           │ calls                                │
│  ┌──────────────── APPLICATION LAYER ─────────────────────┐     │
│  │                    USE CASES                            │     │
│  │  SendMessageUseCase  │ SwitchModelUseCase              │     │
│  │  ProcessPaymentUseCase │ CalculateCommissionUseCase    │     │
│  │  ApplyCouponUseCase  │ RouteToModelUseCase             │     │
│  │  CreatePersonaUseCase │ InviteTeamMemberUseCase        │     │
│  │  TrackAttributionUseCase │ GenerateReportUseCase       │     │
│  │                                                        │     │
│  │  ── PORTS (Interfaces) ──                              │     │
│  │  AIGatewayPort │ PaymentPort │ StoragePort │ EmailPort │     │
│  │  CachePort │ SearchPort │ AnalyticsPort                │     │
│  └────────────────────────┬───────────────────────────────┘     │
│                           │ implements                           │
│  ┌──────────────── DOMAIN LAYER ──────────────────────────┐     │
│  │                 ENTITIES & VALUE OBJECTS                 │     │
│  │  User │ Conversation │ Message │ Plan │ Subscription    │     │
│  │  Credit │ Partner │ Coupon │ Commission │ Attribution   │     │
│  │  Persona │ Prompt │ Template │ Workspace │ TeamMember  │     │
│  │  AIModel │ AIProvider │ RoutingRule │ KnowledgeBase     │     │
│  │                                                        │     │
│  │  ── DOMAIN SERVICES ──                                 │     │
│  │  CreditCalculator │ CommissionCalculator               │     │
│  │  ModelRouter │ FraudDetector │ CouponValidator          │     │
│  │                                                        │     │
│  │  ── DOMAIN EVENTS ──                                   │     │
│  │  MessageSent │ PaymentCompleted │ CouponApplied         │     │
│  │  SubscriptionCreated │ CommissionCalculated             │     │
│  │  PartnerRegistered │ CreditsDepleted                    │     │
│  └────────────────────────┬───────────────────────────────┘     │
│                           │ adapts                               │
│  ┌──────────────── INFRASTRUCTURE LAYER ──────────────────┐     │
│  │  ── ADAPTERS (External) ──                              │     │
│  │  OpenRouterAdapter │ OpenAIAdapter │ AnthropicAdapter   │     │
│  │  GoogleAIAdapter │ StripeAdapter │ AsaasAdapter         │     │
│  │  ResendEmailAdapter │ CloudflareR2Adapter               │     │
│  │  PostHogAdapter │ RedisAdapter                          │     │
│  │                                                        │     │
│  │  ── PERSISTENCE ──                                     │     │
│  │  JPA Repositories (Spring Data JPA)                    │     │
│  │  Custom query implementations (QueryDSL / JOOQ)       │     │
│  │                                                        │     │
│  │  ── CONFIGURATION ──                                   │     │
│  │  SecurityConfig │ CorsConfig │ OpenAPIConfig            │     │
│  │  RedisConfig │ AsyncConfig │ SchedulerConfig            │     │
│  │  WebSocketConfig │ ObjectMapperConfig                   │     │
│  └────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
```

### 2.4 Fluxo de Dados — Chat com IA (Sequência Principal)

```
Usuário          Frontend(React)      Backend(Spring)       Redis        AI Provider
  │                    │                    │                  │              │
  │ 1. Digita msg      │                    │                  │              │
  │───────────────────▶│                    │                  │              │
  │                    │ 2. POST /api/v1/   │                  │              │
  │                    │    chat/messages    │                  │              │
  │                    │───────────────────▶│                  │              │
  │                    │                    │ 3. Validate JWT  │              │
  │                    │                    │ 4. Check credits │              │
  │                    │                    │─────────────────▶│              │
  │                    │                    │◀─────────────────│              │
  │                    │                    │ 5. Semantic cache │              │
  │                    │                    │    lookup         │              │
  │                    │                    │─────────────────▶│              │
  │                    │                    │◀─ miss ──────────│              │
  │                    │                    │                  │              │
  │                    │                    │ 6. Route to model │              │
  │                    │                    │    (Strategy)     │              │
  │                    │                    │──────────────────────────────▶│
  │                    │                    │◀── SSE stream ───────────────│
  │                    │ 7. SSE stream      │                  │              │
  │                    │◀───────────────────│                  │              │
  │ 8. Render stream   │                    │                  │              │
  │◀───────────────────│                    │                  │              │
  │                    │                    │ 9. Save to DB    │              │
  │                    │                    │ 10. Deduct credits│              │
  │                    │                    │ 11. Cache response│              │
  │                    │                    │─────────────────▶│              │
  │                    │                    │ 12. Emit event   │              │
  │                    │                    │    MessageSent   │              │
```

---

## 3. ARQUITETURA BACKEND — Java 21 + Spring Boot 3.3

### 3.1 Estrutura Maven Multi-Module

```
ia-aggregator/
├── pom.xml                              # Parent POM
├── ia-aggregator-domain/
│   ├── pom.xml
│   └── src/main/java/br/com/iaggregator/domain/
│       ├── auth/
│       │   ├── entity/          User, Role, Session, Organization
│       │   ├── valueobject/     Email, Password, UserId, OrgId
│       │   ├── repository/      UserRepository (interface)
│       │   ├── service/         AuthDomainService
│       │   └── event/           UserRegistered, UserDeleted
│       ├── billing/
│       │   ├── entity/          Plan, Subscription, Credit, Payment, Invoice
│       │   ├── valueobject/     Money, CreditAmount, PlanTier
│       │   ├── repository/      SubscriptionRepository, PaymentRepository
│       │   ├── service/         CreditCalculator, PricingService
│       │   └── event/           PaymentCompleted, CreditsExhausted, SubscriptionCreated
│       ├── chat/
│       │   ├── entity/          Conversation, Message, SharedLink, Fork
│       │   ├── valueobject/     MessageContent, ConversationId, ModelResponse
│       │   ├── repository/      ConversationRepository, MessageRepository
│       │   ├── service/         ConversationDomainService
│       │   └── event/           MessageSent, ConversationForked, ConversationShared
│       ├── aigateway/
│       │   ├── entity/          AIModel, AIProvider, RoutingRule, SemanticCacheEntry
│       │   ├── valueobject/     ModelId, PromptTokens, CompletionTokens, CreditCost
│       │   ├── repository/      AIModelRepository, CacheRepository
│       │   ├── service/         ModelRouter, CostEstimator, FallbackHandler
│       │   └── event/           ModelInvoked, CacheHit, FallbackTriggered
│       ├── partners/
│       │   ├── entity/          Partner, Coupon, Attribution, Commission, PaymentBatch
│       │   ├── valueobject/     CouponCode, CommissionRate, ClickId, PartnerStatus
│       │   ├── repository/      PartnerRepository, CouponRepository, CommissionRepository
│       │   ├── service/         CommissionCalculator, CouponValidator, FraudDetector
│       │   └── event/           CouponApplied, CommissionCalculated, PartnerPaid
│       ├── content/
│       │   ├── entity/          Persona, Prompt, Template, KnowledgeBase, Document
│       │   ├── valueobject/     SystemPrompt, TemplateVariable, EmbeddingVector
│       │   ├── repository/      PersonaRepository, PromptRepository, KnowledgeBaseRepo
│       │   ├── service/         RAGService, TemplateEngine
│       │   └── event/           DocumentUploaded, PersonaCreated
│       ├── teams/
│       │   ├── entity/          Workspace, TeamMember, Invite, TeamRole
│       │   ├── valueobject/     WorkspaceId, InviteToken, TeamPermission
│       │   ├── repository/      WorkspaceRepository, TeamMemberRepository
│       │   ├── service/         TeamDomainService, AccessControl
│       │   └── event/           MemberInvited, MemberJoined, WorkspaceCreated
│       ├── analytics/
│       │   ├── entity/          UsageEvent, DailyMetric, Report
│       │   ├── valueobject/     MetricType, DateRange, AggregationType
│       │   ├── repository/      UsageEventRepository, MetricRepository
│       │   └── service/         MetricAggregator, ReportGenerator
│       └── shared/
│           ├── entity/          BaseEntity, AuditableEntity
│           ├── valueobject/     DateRange, PageRequest, SortOrder
│           ├── event/           DomainEvent (base)
│           ├── exception/       DomainException, BusinessRuleViolation
│           └── specification/   Specification<T> (base)
│
├── ia-aggregator-application/
│   ├── pom.xml
│   └── src/main/java/br/com/iaggregator/application/
│       ├── auth/
│       │   ├── usecase/         RegisterUserUseCase, LoginUseCase, RefreshTokenUseCase
│       │   │                    DeleteAccountUseCase, Enable2FAUseCase
│       │   ├── dto/             RegisterRequest, LoginResponse, UserDTO
│       │   ├── port/            out/ AuthProviderPort, TokenPort
│       │   └── mapper/          UserMapper
│       ├── billing/
│       │   ├── usecase/         CreateSubscriptionUseCase, ProcessPaymentUseCase
│       │   │                    DeductCreditsUseCase, TopUpCreditsUseCase
│       │   │                    ChangeSubscriptionUseCase, CancelSubscriptionUseCase
│       │   ├── dto/             PlanDTO, SubscriptionDTO, PaymentDTO, CreditBalanceDTO
│       │   ├── port/            out/ PaymentGatewayPort, InvoicePort
│       │   └── mapper/          BillingMapper
│       ├── chat/
│       │   ├── usecase/         SendMessageUseCase, CreateConversationUseCase
│       │   │                    SwitchModelUseCase, ForkConversationUseCase
│       │   │                    ShareConversationUseCase, SearchHistoryUseCase
│       │   ├── dto/             ConversationDTO, MessageDTO, StreamChunkDTO
│       │   ├── port/            out/ AIGatewayPort, StreamingPort
│       │   └── mapper/          ChatMapper
│       ├── aigateway/
│       │   ├── usecase/         RouteToModelUseCase, EstimateCostUseCase
│       │   │                    CheckSemanticCacheUseCase, RegisterModelUseCase
│       │   ├── dto/             AIRequestDTO, AIResponseDTO, CostEstimateDTO
│       │   ├── port/            out/ AIProviderPort, CachePort
│       │   └── mapper/          AIModelMapper
│       ├── partners/
│       │   ├── usecase/         RegisterPartnerUseCase, CreateCouponUseCase
│       │   │                    ApplyCouponUseCase, TrackAttributionUseCase
│       │   │                    CalculateCommissionUseCase, ProcessBatchPaymentUseCase
│       │   │                    ValidateFraudUseCase
│       │   ├── dto/             PartnerDTO, CouponDTO, CommissionDTO, AttributionDTO
│       │   ├── port/            out/ PixPaymentPort, FraudDetectionPort
│       │   └── mapper/          PartnerMapper
│       ├── content/
│       │   ├── usecase/         CreatePersonaUseCase, SavePromptUseCase
│       │   │                    UploadDocumentUseCase, QueryKnowledgeBaseUseCase
│       │   │                    RenderTemplateUseCase
│       │   ├── dto/             PersonaDTO, PromptDTO, TemplateDTO, DocumentDTO
│       │   ├── port/            out/ StoragePort, EmbeddingPort, OCRPort
│       │   └── mapper/          ContentMapper
│       ├── teams/
│       │   ├── usecase/         CreateWorkspaceUseCase, InviteMemberUseCase
│       │   │                    SetTeamRoleUseCase, GetTeamAnalyticsUseCase
│       │   ├── dto/             WorkspaceDTO, TeamMemberDTO, InviteDTO
│       │   ├── port/            out/ NotificationPort
│       │   └── mapper/          TeamMapper
│       ├── analytics/
│       │   ├── usecase/         TrackEventUseCase, GetDashboardUseCase
│       │   │                    GenerateReportUseCase, ExportDataUseCase
│       │   ├── dto/             DashboardDTO, MetricDTO, ReportDTO
│       │   ├── port/            out/ AnalyticsSinkPort
│       │   └── mapper/          AnalyticsMapper
│       └── shared/
│           ├── usecase/         BaseUseCase<I, O>
│           ├── dto/             PageResponse<T>, ErrorResponse, ApiResponse<T>
│           └── port/            EventPublisherPort, LogPort
│
├── ia-aggregator-infrastructure/
│   ├── pom.xml
│   └── src/main/java/br/com/iaggregator/infrastructure/
│       ├── persistence/
│       │   ├── jpa/
│       │   │   ├── entity/      JPA @Entity classes (JpaUser, JpaConversation, etc.)
│       │   │   ├── repository/  Spring Data JPA repositories
│       │   │   └── mapper/      JPA Entity ↔ Domain Entity mappers
│       │   └── config/          DataSourceConfig, JpaConfig, FlywayConfig
│       ├── external/
│       │   ├── ai/              OpenRouterClient, OpenAIClient, AnthropicClient, GoogleAIClient
│       │   ├── payment/         StripeClient, AsaasClient (Pix, Boleto)
│       │   ├── email/           ResendClient
│       │   ├── storage/         CloudflareR2Client
│       │   └── analytics/       PostHogClient
│       ├── cache/               RedisCacheAdapter, SemanticCacheAdapter
│       ├── security/            JwtTokenProvider, SecurityFilterChain, RateLimitFilter
│       ├── event/               SpringEventPublisher, OutboxEventStore
│       ├── scheduler/           CommissionCarenciaJob, CreditResetJob, LgpdCleanupJob
│       ├── config/
│       │   ├── SecurityConfig.java
│       │   ├── CorsConfig.java
│       │   ├── RedisConfig.java
│       │   ├── AsyncConfig.java
│       │   ├── WebSocketConfig.java
│       │   ├── OpenApiConfig.java
│       │   ├── ObjectMapperConfig.java
│       │   ├── Resilience4jConfig.java
│       │   └── ObservabilityConfig.java
│       └── migration/           Flyway SQL migrations (V1__*, V2__*, ...)
│
├── ia-aggregator-presentation/
│   ├── pom.xml
│   └── src/main/java/br/com/iaggregator/presentation/
│       ├── rest/
│       │   ├── auth/            AuthController, AuthRequestDTO, AuthResponseDTO
│       │   ├── billing/         BillingController, CheckoutController
│       │   ├── chat/            ChatController, StreamController (SSE)
│       │   ├── aigateway/       ModelController, CostController
│       │   ├── partners/        PartnerController, CouponController, CommissionController
│       │   ├── content/         PersonaController, PromptController, DocumentController
│       │   ├── teams/           WorkspaceController, TeamController
│       │   ├── analytics/       AnalyticsController, DashboardController
│       │   ├── admin/           AdminController (gestão geral)
│       │   └── webhook/         StripeWebhookController, AsaasWebhookController
│       ├── sse/                 ChatSSEEmitter, NotificationSSEEmitter
│       ├── websocket/           ChatWebSocketHandler (futuro)
│       ├── filter/              RequestLoggingFilter, TenantContextFilter
│       ├── interceptor/         RateLimitInterceptor, AuditInterceptor
│       ├── exception/           GlobalExceptionHandler, ErrorResponseBuilder
│       └── docs/                OpenAPI annotations, examples
│
└── ia-aggregator-common/
    ├── pom.xml
    └── src/main/java/br/com/iaggregator/common/
        ├── util/                StringUtils, DateUtils, CryptoUtils, SlugGenerator
        ├── validation/          CPFValidator, CNPJValidator, EmailValidator, CouponCodeValidator
        ├── constant/            AppConstants, ErrorCodes, CreditConstants
        └── annotation/          @Auditable, @RateLimited, @RequiresCredits
```

### 3.2 Parent POM — Dependências Principais

```xml
<!-- Java 21 + Spring Boot 3.3 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
</parent>

<properties>
    <java.version>21</java.version>
    <spring-cloud.version>2023.0.3</spring-cloud.version>
</properties>

<!-- Dependências Chave -->
- spring-boot-starter-web          (REST API)
- spring-boot-starter-data-jpa     (Persistence)
- spring-boot-starter-security     (Auth + JWT)
- spring-boot-starter-validation   (Bean Validation)
- spring-boot-starter-cache        (Cache abstraction)
- spring-boot-starter-actuator     (Health + Metrics)
- spring-boot-starter-webflux      (WebClient para AI APIs, SSE)
- spring-data-redis                (Redis cache)
- flyway-core                      (DB migrations)
- postgresql                       (JDBC Driver)
- jjwt (io.jsonwebtoken)           (JWT tokens)
- resilience4j-spring-boot3        (Circuit Breaker, Retry, RateLimiter)
- springdoc-openapi-starter-webmvc-ui (Swagger/OpenAPI 3.1)
- mapstruct                        (Object mapping)
- lombok                           (Boilerplate reduction)
- querydsl-jpa                     (Type-safe queries)
- testcontainers                   (Integration tests)
- archunit                         (Architecture tests)
- jacoco                           (Code coverage)
```

### 3.3 Convenções de API REST

```
Base URL: /api/v1

── AUTH ──
POST   /api/v1/auth/register              Cadastro
POST   /api/v1/auth/login                 Login (email+senha)
POST   /api/v1/auth/login/google          Login OAuth Google
POST   /api/v1/auth/refresh               Refresh token
POST   /api/v1/auth/logout                Logout
POST   /api/v1/auth/forgot-password       Recuperar senha
DELETE /api/v1/auth/account               Excluir conta (LGPD)

── USERS ──
GET    /api/v1/users/me                   Perfil do usuário
PUT    /api/v1/users/me                   Atualizar perfil
GET    /api/v1/users/me/credits           Saldo de créditos
GET    /api/v1/users/me/usage             Uso atual do mês

── BILLING ──
GET    /api/v1/plans                      Listar planos disponíveis
POST   /api/v1/subscriptions              Criar assinatura
GET    /api/v1/subscriptions/current      Assinatura atual
PUT    /api/v1/subscriptions/current      Alterar plano
DELETE /api/v1/subscriptions/current      Cancelar assinatura
POST   /api/v1/payments/checkout          Iniciar checkout (Pix/Cartão)
GET    /api/v1/payments/history           Histórico de pagamentos
POST   /api/v1/credits/top-up            Comprar créditos extras
GET    /api/v1/credits/consumption        Consumo detalhado

── CHAT ──
GET    /api/v1/conversations              Listar conversas (paginado, filtrável)
POST   /api/v1/conversations              Criar conversa
GET    /api/v1/conversations/:id          Detalhes da conversa
DELETE /api/v1/conversations/:id          Excluir conversa
POST   /api/v1/conversations/:id/messages Enviar mensagem (retorna SSE stream)
PUT    /api/v1/conversations/:id/model    Trocar modelo mid-chat
POST   /api/v1/conversations/:id/fork     Forkar conversa
POST   /api/v1/conversations/:id/share    Gerar link público
GET    /api/v1/conversations/shared/:slug Acessar conversa pública
GET    /api/v1/conversations/search       Buscar no histórico

── AI MODELS ──
GET    /api/v1/models                     Listar modelos disponíveis
GET    /api/v1/models/:id                 Detalhes do modelo
POST   /api/v1/models/estimate-cost       Estimar custo de um prompt
GET    /api/v1/models/recommend           Recomendação baseada em perfil

── CONTENT ──
GET    /api/v1/personas                   Listar personas
POST   /api/v1/personas                   Criar persona
PUT    /api/v1/personas/:id               Editar persona
DELETE /api/v1/personas/:id               Excluir persona
GET    /api/v1/prompts                    Listar prompts salvos
POST   /api/v1/prompts                    Salvar prompt
GET    /api/v1/prompts/gallery            Galeria de prompts prontos
GET    /api/v1/templates                  Listar templates por profissão
POST   /api/v1/documents                  Upload de documento
GET    /api/v1/documents/:id              Detalhes do documento
POST   /api/v1/knowledge-bases            Criar base de conhecimento
POST   /api/v1/knowledge-bases/:id/query  Consultar base (RAG)

── PARTNERS (Painel Parceiro) ──
GET    /api/v1/partner/dashboard           Dashboard KPIs
GET    /api/v1/partner/coupons             Meus cupons
GET    /api/v1/partner/commissions         Extrato de comissões
GET    /api/v1/partner/commissions/export  Exportar CSV

── COUPONS (Público) ──
POST   /api/v1/coupons/validate           Validar cupom no checkout
POST   /api/v1/coupons/apply              Aplicar cupom

── TEAMS ──
GET    /api/v1/workspaces                 Listar workspaces
POST   /api/v1/workspaces                 Criar workspace
GET    /api/v1/workspaces/:id/members     Listar membros
POST   /api/v1/workspaces/:id/invites     Convidar membro
PUT    /api/v1/workspaces/:id/members/:uid Alterar role
GET    /api/v1/workspaces/:id/analytics   Analytics do workspace

── ANALYTICS ──
GET    /api/v1/analytics/dashboard         Dashboard pessoal
GET    /api/v1/analytics/cost-breakdown    Breakdown de custos por modelo
GET    /api/v1/analytics/savings           Economia vs. assinaturas individuais

── ADMIN ──
GET    /api/v1/admin/users                 Gestão de usuários
GET    /api/v1/admin/partners              Gestão de parceiros
POST   /api/v1/admin/partners              Cadastrar parceiro
POST   /api/v1/admin/coupons              Criar cupom
GET    /api/v1/admin/commissions           Comissões pendentes
POST   /api/v1/admin/commissions/batch-pay Pagamento em lote
GET    /api/v1/admin/analytics             Dashboard administrativo
GET    /api/v1/admin/audit-logs            Logs de auditoria

── WEBHOOKS (Recebimento) ──
POST   /api/v1/webhooks/stripe            Webhook Stripe
POST   /api/v1/webhooks/asaas             Webhook Asaas (Pix)
```

### 3.4 Formato Padrão de Resposta

```json
// Sucesso
{
  "success": true,
  "data": { ... },
  "meta": {
    "page": 1,
    "pageSize": 20,
    "totalItems": 150,
    "totalPages": 8
  }
}

// Erro
{
  "success": false,
  "error": {
    "code": "CREDITS_EXHAUSTED",
    "message": "Seus créditos acabaram. Faça upgrade do plano ou compre créditos extras.",
    "details": [
      { "field": "credits", "issue": "Saldo: 0 créditos. Necessário: 10 créditos." }
    ],
    "timestamp": "2026-03-15T14:30:00Z",
    "traceId": "abc-123-def"
  }
}
```

---

## 6. DESIGN PATTERNS — Catálogo de Aplicação

### 6.1 Patterns Criacionais

#### Factory Method — Criação de Clientes AI

```java
// Cada provider de IA tem sua implementação, mas todos implementam a mesma interface.
// O Factory decide qual instanciar baseado no provider do modelo escolhido.

public interface AIProviderClient {
    Flux<StreamChunk> streamCompletion(AIRequest request);
    AIResponse complete(AIRequest request);
    CostEstimate estimateCost(String prompt, String model);
}

public class AIProviderClientFactory {
    private final Map<String, AIProviderClient> clients;

    public AIProviderClient create(AIProvider provider) {
        return switch (provider) {
            case OPENAI     -> clients.get("openai");
            case ANTHROPIC  -> clients.get("anthropic");
            case GOOGLE     -> clients.get("google");
            case OPENROUTER -> clients.get("openrouter");
            case DEEPSEEK   -> clients.get("deepseek");
            default -> throw new UnsupportedProviderException(provider);
        };
    }
}
```

**Onde usa:** `ai-gateway` module — instanciação de clientes por provider.

#### Builder — Construção de Prompts Complexos

```java
// Prompts com system message, contexto, personas, modo consultor, etc.
AIRequest request = AIRequest.builder()
    .model(selectedModel)
    .systemPrompt(persona.getSystemPrompt())
    .conversationHistory(conversation.getMessages())
    .userMessage(userInput)
    .temperature(0.7)
    .maxTokens(4096)
    .stream(true)
    .mode(ChatMode.CONSULTANT) // Modo Consultor ativo
    .documents(attachedDocs)   // RAG context
    .build();
```

**Onde usa:** `chat` e `content` modules — construção de requests para IA.

#### Singleton — Configurações e Cache Managers

```java
// Spring Beans são Singleton por padrão. Garantido via @Configuration.
@Configuration
public class CacheConfig {
    @Bean
    public SemanticCacheManager semanticCacheManager(RedisTemplate<String, String> redis) {
        return new SemanticCacheManager(redis, Duration.ofHours(24));
    }
}
```

**Onde usa:** Config classes, cache managers, connection pools.

### 6.2 Patterns Estruturais

#### Adapter — Integração com APIs Externas (ACL)

```java
// Cada serviço externo tem seu Adapter que traduz o formato externo
// para o formato do domínio interno. Anti-Corruption Layer.

// Port (interface no application layer)
public interface PaymentGatewayPort {
    PaymentResult processPixPayment(Money amount, PixKey key);
    PaymentResult processCardPayment(Money amount, CardToken token);
    SubscriptionResult createSubscription(Plan plan, PaymentMethod method);
}

// Adapter (implementação no infrastructure layer)
@Component
public class StripePaymentAdapter implements PaymentGatewayPort {
    private final StripeClient stripeClient;

    @Override
    public PaymentResult processPixPayment(Money amount, PixKey key) {
        // Traduz domínio → Stripe API
        PaymentIntent intent = stripeClient.paymentIntents().create(
            PaymentIntentCreateParams.builder()
                .setAmount(amount.toCentavos())
                .setCurrency("brl")
                .addPaymentMethodType("pix")
                .build()
        );
        // Traduz Stripe API → domínio
        return PaymentResult.from(intent);
    }
}
```

**Onde usa:** `billing` (Stripe/Asaas), `ai-gateway` (OpenRouter/OpenAI/Anthropic), `partners` (Pix batch).

#### Decorator — Cálculo de Créditos com Desconto em Camadas

```java
// O custo de créditos pode ter múltiplas transformações:
// base cost → cupom desconto → bônus plano anual → cache hit (custo zero)

public interface CreditCostCalculator {
    CreditCost calculate(AIModel model, TokenUsage usage);
}

public class BaseCreditCostCalculator implements CreditCostCalculator {
    public CreditCost calculate(AIModel model, TokenUsage usage) {
        return new CreditCost(model.getCreditMultiplier() * usage.totalTokens() / 1000);
    }
}

public class CouponDiscountDecorator implements CreditCostCalculator {
    private final CreditCostCalculator inner;
    private final Coupon activeCoupon;

    public CreditCost calculate(AIModel model, TokenUsage usage) {
        CreditCost base = inner.calculate(model, usage);
        if (activeCoupon != null && activeCoupon.appliesToCredits()) {
            return base.applyDiscount(activeCoupon.getDiscountRate());
        }
        return base;
    }
}

public class CacheHitDecorator implements CreditCostCalculator {
    private final CreditCostCalculator inner;
    private final boolean cacheHit;

    public CreditCost calculate(AIModel model, TokenUsage usage) {
        if (cacheHit) return CreditCost.ZERO; // Cache hit = sem custo
        return inner.calculate(model, usage);
    }
}
```

**Onde usa:** `billing` + `ai-gateway` — cálculo de custo final de créditos.

### 6.3 Patterns Comportamentais

#### Strategy — Roteamento Inteligente de Modelos

```java
// O auto-routing seleciona o melhor modelo baseado em diferentes estratégias.
public interface RoutingStrategy {
    AIModel selectModel(RoutingContext context);
}

public class CostOptimizedStrategy implements RoutingStrategy {
    // Prioriza modelos baratos que atendem à tarefa
    public AIModel selectModel(RoutingContext ctx) {
        return ctx.getAvailableModels().stream()
            .filter(m -> m.supportsTask(ctx.getTaskType()))
            .min(Comparator.comparing(AIModel::getCreditCost))
            .orElseThrow();
    }
}

public class QualityOptimizedStrategy implements RoutingStrategy {
    // Prioriza qualidade para a tarefa detectada
    public AIModel selectModel(RoutingContext ctx) {
        return ctx.getAvailableModels().stream()
            .filter(m -> m.supportsTask(ctx.getTaskType()))
            .max(Comparator.comparing(m -> m.getQualityScore(ctx.getTaskType())))
            .orElseThrow();
    }
}

public class BalancedStrategy implements RoutingStrategy {
    // Score ponderado: qualidade × 0.6 + (1/custo) × 0.4
}

// Context que usa Strategy
public class ModelRouter {
    private final Map<RoutingMode, RoutingStrategy> strategies;

    public AIModel route(RoutingMode mode, RoutingContext context) {
        return strategies.get(mode).selectModel(context);
    }
}
```

**Onde usa:** `ai-gateway` — seleção automática de modelo.

#### Observer / Event-Driven — Eventos de Domínio

```java
// Quando um pagamento é completado, múltiplos handlers reagem:
// 1. Creditar créditos ao usuário
// 2. Calcular comissão do parceiro
// 3. Enviar email de confirmação
// 4. Registrar evento de analytics

// Domain Event
public record PaymentCompleted(
    UUID paymentId, UUID userId, UUID subscriptionId,
    Money amount, PaymentMethod method, Instant timestamp
) implements DomainEvent {}

// Handler 1: Créditos
@EventListener
public class CreditHandler {
    void handle(PaymentCompleted event) {
        creditService.addMonthlyCredits(event.userId(), event.subscriptionId());
    }
}

// Handler 2: Comissão do Parceiro
@EventListener
public class CommissionHandler {
    void handle(PaymentCompleted event) {
        attributionService.findByUserId(event.userId())
            .ifPresent(attr -> commissionCalculator.calculate(attr, event));
    }
}

// Handler 3: Email
@EventListener
@Async
public class PaymentEmailHandler {
    void handle(PaymentCompleted event) {
        emailService.sendPaymentConfirmation(event.userId(), event.amount());
    }
}
```

**Onde usa:** Todos os módulos — eventos `PaymentCompleted`, `MessageSent`, `CouponApplied`, `PartnerRegistered`, `SubscriptionCreated`, `CreditsDepleted`.

#### Chain of Responsibility — Validação Anti-Fraude

```java
// Cada regra de anti-fraude é um elo da chain. Se qualquer regra detecta
// fraude, a chain para e retorna o resultado.

public abstract class FraudRule {
    private FraudRule next;

    public FraudRule setNext(FraudRule next) {
        this.next = next;
        return next;
    }

    public FraudResult check(FraudContext context) {
        FraudResult result = evaluate(context);
        if (result.isBlocked()) return result;
        if (next != null) return next.check(context);
        return FraudResult.approved();
    }

    protected abstract FraudResult evaluate(FraudContext context);
}

// Regras concretas
public class ClickVelocityRule extends FraudRule { /* >10 clicks/min → block */ }
public class SelfReferralRule extends FraudRule { /* parceiro=usuário → block */ }
public class GeoMismatchRule extends FraudRule { /* país diferente em <1h → flag */ }
public class DeviceFingerprintRule extends FraudRule { /* 3+ signups mesmo device → block */ }
public class ClickToConversionTimeRule extends FraudRule { /* <30s → flag */ }
public class DisposableEmailRule extends FraudRule { /* tempmail → block */ }
public class AnomalousConversionRateRule extends FraudRule { /* >40% → flag */ }
public class AnomalousTimingRule extends FraudRule { /* >80% 2-5AM → flag */ }

// Montagem da chain
FraudRule chain = new ClickVelocityRule();
chain.setNext(new SelfReferralRule())
     .setNext(new GeoMismatchRule())
     .setNext(new DeviceFingerprintRule())
     .setNext(new ClickToConversionTimeRule())
     .setNext(new DisposableEmailRule())
     .setNext(new AnomalousConversionRateRule())
     .setNext(new AnomalousTimingRule());
```

**Onde usa:** `partners` module — 8 regras anti-fraude pré-filtro.

#### Template Method — Fluxo Base de Comunicação com IA

```java
// Todos os providers seguem o mesmo fluxo, mas cada um tem suas particularidades.
public abstract class BaseAIProviderClient implements AIProviderClient {

    // Template method — define o algoritmo
    public final Flux<StreamChunk> streamCompletion(AIRequest request) {
        validateRequest(request);                  // Comum
        HttpRequest httpRequest = buildRequest(request); // Específico por provider
        return executeStream(httpRequest)           // Comum (WebClient)
            .map(this::parseChunk)                 // Específico por provider
            .doOnComplete(() -> logCompletion(request)); // Comum
    }

    protected abstract HttpRequest buildRequest(AIRequest request);
    protected abstract StreamChunk parseChunk(String rawChunk);

    // Métodos comuns (não sobrescritos)
    protected void validateRequest(AIRequest request) { /* validação comum */ }
    protected Flux<String> executeStream(HttpRequest req) { /* WebClient SSE */ }
    protected void logCompletion(AIRequest request) { /* métricas */ }
}

public class OpenAIClient extends BaseAIProviderClient {
    protected HttpRequest buildRequest(AIRequest request) { /* formato OpenAI */ }
    protected StreamChunk parseChunk(String raw) { /* parse SSE OpenAI */ }
}

public class AnthropicClient extends BaseAIProviderClient {
    protected HttpRequest buildRequest(AIRequest request) { /* formato Anthropic */ }
    protected StreamChunk parseChunk(String raw) { /* parse SSE Anthropic */ }
}
```

**Onde usa:** `ai-gateway` — comunicação com cada provider de IA.

#### Specification — Queries Dinâmicas

```java
// Busca de conversas com filtros dinâmicos
public class ConversationSpecification {
    public static Specification<JpaConversation> belongsToUser(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<JpaConversation> containsText(String search) {
        return (root, query, cb) -> cb.like(
            cb.lower(root.get("title")), "%" + search.toLowerCase() + "%"
        );
    }

    public static Specification<JpaConversation> usesModel(String modelId) {
        return (root, query, cb) -> cb.equal(root.get("lastModelId"), modelId);
    }

    public static Specification<JpaConversation> inFolder(UUID folderId) {
        return (root, query, cb) -> cb.equal(root.get("folderId"), folderId);
    }

    public static Specification<JpaConversation> createdBetween(Instant from, Instant to) {
        return (root, query, cb) -> cb.between(root.get("createdAt"), from, to);
    }
}

// Uso no repository
conversationRepo.findAll(
    belongsToUser(userId)
        .and(containsText("contrato"))
        .and(usesModel("claude-sonnet"))
        .and(createdBetween(startOfMonth, now)),
    PageRequest.of(0, 20, Sort.by("updatedAt").descending())
);
```

**Onde usa:** `chat`, `partners`, `analytics` — todas as buscas com filtros dinâmicos.

### 6.4 Resumo de Patterns por Módulo

| Módulo | Patterns Aplicados |
|--------|-------------------|
| **auth** | Factory (OAuth providers), Strategy (auth methods), Observer (UserRegistered) |
| **billing** | Observer (PaymentCompleted), Decorator (credit cost), Strategy (pricing rules), Saga (subscription flow) |
| **chat** | Builder (AI request), Observer (MessageSent), Template Method (streaming), Specification (search) |
| **ai-gateway** | Factory (AI clients), Strategy (routing), Template Method (provider comm), Decorator (cache/cost), Chain (fallback) |
| **partners** | Chain of Responsibility (anti-fraud), Observer (CouponApplied/CommissionCalculated), Specification (partner search), Saga (batch payment) |
| **content** | Builder (templates), Strategy (RAG retrieval), Factory (document parsers) |
| **teams** | Observer (MemberInvited), Strategy (permission check), Specification (member search) |
| **analytics** | Observer (all events), Strategy (aggregation), Builder (reports) |

---

## 7. MICROSERVICES PATTERNS

> **Nota:** A aplicação inicia como **monolito modular** (Spring Modulith). Os patterns de microsserviços são aplicados internamente para permitir extração futura sem reescrita.

### 7.1 CQRS — Command Query Responsibility Segregation

```
WRITE (Commands)                    READ (Queries)
────────────────                    ─────────────
POST /messages     ──▶ Domain      GET /analytics  ──▶ Read Model
POST /payments         Model       GET /dashboard       (Views
PUT  /subscriptions    (JPA)       GET /reports          materializadas
                        │                                 em tabelas
                        │ Domain Events                   separadas)
                        ▼
                   Event Handler ──▶ Materializa read models
                                     em tabelas otimizadas
                                     para consulta
```

**Aplicação concreta:**
- `chat`: Messages escritas via JPA. Dashboard de analytics lê de `daily_metrics` (tabela materializada).
- `billing`: Payments processados pelo domínio. `credit_consumption_view` atualizada via event handler para queries de dashboard.
- `analytics`: Todos os eventos de domínio são capturados e agregados em read models otimizados.

### 7.2 SAGA — Orquestração de Workflows Distribuídos

#### Saga: Assinatura com Cupom de Parceiro

```
1. ValidateCoupon        ──▶ Cupom existe, ativo, válido para o plano?
   │ (rollback: noop)
   ▼
2. ProcessPayment        ──▶ Cobrar via Stripe/Asaas (Pix ou Cartão)
   │ (rollback: estornar pagamento)
   ▼
3. CreateSubscription    ──▶ Ativar plano, alocar créditos mensais
   │ (rollback: cancelar subscription)
   ▼
4. TrackAttribution      ──▶ Vincular usuário ao parceiro/cupom
   │ (rollback: remover atribuição)
   ▼
5. CalculateCommission   ──▶ Criar comissão pendente (carência 7 dias)
   │ (rollback: cancelar comissão)
   ▼
6. SendConfirmation      ──▶ Email de boas-vindas + recibo
   (compensação: noop — email já enviado é idempotente)
```

**Implementação:** Orchestration Saga via Spring `@Transactional` para steps 1-5 (mesmo DB), step 6 `@Async` com retry.

#### Saga: Pagamento em Lote de Comissões

```
1. SelectEligiblePartners  ──▶ Parceiros com saldo ≥ R$50
2. CreatePaymentBatch      ──▶ Registro do lote (status: processando)
3. ForEachPartner:
   3a. ProcessPixTransfer  ──▶ API Pix para cada parceiro
   3b. UpdateCommissions   ──▶ Status: pendente → paga
   3c. UpdatePartnerBalance──▶ Zerar saldo_pendente
   3d. SendReceipt         ──▶ Email com comprovante
4. FinalizeBatch           ──▶ Status: concluido | falha_parcial
```

### 7.3 ACL — Anti-Corruption Layer

Cada integração externa tem um **Adapter + Translator** que impede que formatos externos contaminem o domínio:

```
Domínio Interno          ACL (Adapter)              API Externa
───────────────          ─────────────              ───────────
AIRequest         ──▶  OpenRouterAdapter    ──▶   OpenRouter API
PaymentResult     ◀──  StripeAdapter        ◀──   Stripe API
CommissionPayout  ──▶  AsaasAdapter         ──▶   Asaas Pix API
EmbeddingVector   ◀──  OpenAIEmbeddingAdapter◀──  OpenAI Embeddings
EmailMessage      ──▶  ResendAdapter        ──▶   Resend API
StorageObject     ──▶  CloudflareR2Adapter  ──▶   Cloudflare R2
AnalyticsEvent    ──▶  PostHogAdapter       ──▶   PostHog API
```

### 7.4 Circuit Breaker — Resilience4j

```java
@CircuitBreaker(name = "openrouter", fallbackMethod = "fallbackToDirectAPI")
@Retry(name = "openrouter", fallbackMethod = "fallbackToDirectAPI")
@TimeLimiter(name = "openrouter")
public Flux<StreamChunk> streamViaOpenRouter(AIRequest request) {
    return openRouterClient.stream(request);
}

public Flux<StreamChunk> fallbackToDirectAPI(AIRequest request, Throwable t) {
    // Fallback: chamar API direta do provider (OpenAI, Anthropic, etc.)
    AIProviderClient directClient = factory.create(request.getModel().getProvider());
    return directClient.streamCompletion(request);
}

// application.yml
resilience4j:
  circuitbreaker:
    instances:
      openrouter:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
  retry:
    instances:
      openrouter:
        maxAttempts: 3
        waitDuration: 1s
        exponentialBackoffMultiplier: 2
  timelimiter:
    instances:
      openrouter:
        timeoutDuration: 30s
```

### 7.5 Outbox Pattern — Eventos Confiáveis

```sql
-- Tabela outbox para garantir que eventos de domínio são publicados
-- mesmo em caso de falha. Polling periódico ou CDC (Change Data Capture).
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,  -- 'Payment', 'Subscription'
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,      -- 'PaymentCompleted'
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    published_at TIMESTAMPTZ,
    status VARCHAR(20) DEFAULT 'PENDING'   -- PENDING, PUBLISHED, FAILED
);
```

```java
// Scheduler que publica eventos pendentes
@Scheduled(fixedRate = 5000) // A cada 5 segundos
public void publishPendingEvents() {
    List<OutboxEvent> pending = outboxRepo.findByStatus("PENDING");
    for (OutboxEvent event : pending) {
        try {
            eventPublisher.publish(event.toDomainEvent());
            event.markPublished();
        } catch (Exception e) {
            event.markFailed();
        }
        outboxRepo.save(event);
    }
}
```

### 7.6 API Gateway Pattern (preparação para microsserviços)

```
                     ┌────────────────────┐
  Frontend ─────────▶│  Spring Cloud      │
                     │  Gateway (futuro)  │
  Partner App ──────▶│                    │──▶ auth-service
                     │  Rate Limiting     │──▶ chat-service
  Admin Panel ──────▶│  Auth Validation   │──▶ billing-service
                     │  Request Routing   │──▶ partner-service
  Webhooks ─────────▶│  Load Balancing    │──▶ ai-gateway-service
                     └────────────────────┘
```

> **Fase atual (monolito):** O gateway é implementado como filtros Spring MVC internos (rate limiting, auth, tenant context). Quando extrair microsserviços, esses filtros migram para Spring Cloud Gateway.

---

## 8. CLEAN ARCHITECTURE — Implementação

### 8.1 Regra de Dependência

```
     ┌─────────────────────────────┐
     │       PRESENTATION          │  Controllers, DTOs de request/response
     │    (ia-aggregator-presentation)
     └──────────────┬──────────────┘
                    │ depende de
     ┌──────────────▼──────────────┐
     │        APPLICATION          │  Use Cases, DTOs, Ports (interfaces)
     │    (ia-aggregator-application)
     └──────────────┬──────────────┘
                    │ depende de
     ┌──────────────▼──────────────┐
     │          DOMAIN             │  Entities, Value Objects, Domain Services
     │    (ia-aggregator-domain)   │  ** ZERO dependências externas **
     └─────────────────────────────┘
                    ▲
                    │ implementa interfaces de
     ┌──────────────┴──────────────┐
     │       INFRASTRUCTURE        │  JPA, Redis, HTTP Clients, Config
     │  (ia-aggregator-infrastructure)
     └─────────────────────────────┘
```

**Regra fundamental:** As setas de dependência SEMPRE apontam para dentro (para o domínio). O domínio NUNCA depende de infraestrutura.

### 8.2 Enforcement via ArchUnit

```java
@AnalyzeClasses(packages = "br.com.iaggregator")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_presentation =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..presentation..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_spring =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_infrastructure =
        noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule usecases_should_only_be_called_by_controllers =
        classes().that().resideInAPackage("..usecase..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage("..presentation..", "..usecase..");
}
```

---

## 9. CLEAN CODE & SOLID

### 9.1 SOLID — Aplicação Concreta

#### S — Single Responsibility Principle

```java
// ERRADO: Uma classe faz tudo
public class ChatService {
    void sendMessage() { /* lógica de chat */ }
    void deductCredits() { /* lógica de billing */ }
    void trackAttribution() { /* lógica de partners */ }
    void sendEmail() { /* lógica de notificação */ }
}

// CORRETO: Cada classe tem UMA responsabilidade
public class SendMessageUseCase { /* apenas orquestra o envio */ }
public class DeductCreditsUseCase { /* apenas deduz créditos */ }
public class TrackAttributionUseCase { /* apenas rastreia atribuição */ }
// Eventos de domínio conectam as responsabilidades sem acoplamento
```

#### O — Open/Closed Principle

```java
// A adição de novos modelos de IA NÃO requer modificação de código existente.
// Basta criar novo Adapter implementando a interface.

// Novo provider? Crie: DeepSeekClient extends BaseAIProviderClient
// Registre no Factory. Zero mudanças no código existente.
```

#### L — Liskov Substitution Principle

```java
// Todos os AIProviderClient são intercambiáveis.
// O ModelRouter não sabe nem se importa com qual provider está usando.
AIProviderClient client = factory.create(model.getProvider());
Flux<StreamChunk> stream = client.streamCompletion(request);
// Funciona identicamente para OpenAI, Anthropic, Google, DeepSeek...
```

#### I — Interface Segregation Principle

```java
// ERRADO: Interface monolítica
public interface AIService {
    void streamText();
    void generateImage();
    void generateVideo();
    void transcribeAudio();
}

// CORRETO: Interfaces segregadas por capacidade
public interface TextCompletionPort { Flux<StreamChunk> stream(AIRequest req); }
public interface ImageGenerationPort { ImageResult generate(ImageRequest req); }
public interface AudioTranscriptionPort { String transcribe(AudioFile file); }
// Cada provider implementa apenas as interfaces que suporta
```

#### D — Dependency Inversion Principle

```java
// Use Case depende de ABSTRAÇÕES (Ports), não de implementações concretas.

public class SendMessageUseCase {
    private final AIGatewayPort aiGateway;       // Interface, não OpenRouterClient
    private final ConversationRepository convRepo; // Interface, não JpaConversationRepo
    private final CreditService creditService;     // Interface, não StripeBillingService
    private final EventPublisherPort eventPublisher; // Interface, não RabbitMQ/Kafka

    // Injeção via construtor — Spring resolve as implementações
    public SendMessageUseCase(AIGatewayPort aiGateway, ...) { ... }
}
```

### 9.2 Clean Code — Diretrizes

| Regra | Aplicação |
|-------|-----------|
| **Nomes significativos** | `calculatePartnerCommission()` não `calc()` |
| **Funções pequenas** | Máximo ~20 linhas. Se maior, extrair |
| **Um nível de abstração por função** | Controller não faz lógica de negócio |
| **Sem comentários óbvios** | Código auto-documentado. Comentários só para "por quê" |
| **Tratamento de erros** | Exceções de domínio tipadas, não `catch (Exception e)` |
| **Sem números mágicos** | `CreditConstants.MAX_FREE_CREDITS = 300` não `300` |
| **Imutabilidade** | Records do Java 21 para Value Objects e DTOs |
| **Fail Fast** | Validações no início do método, não no meio |
| **DRY** | Lógica repetida extraída para Domain Services |

---

## 10. ANÁLISE ASSINTÓTICA (Big O)

### 10.1 Operações Críticas

| Operação | Complexidade | Justificativa |
|----------|-------------|---------------|
| **Auto-routing de modelo** | O(n·m) | n = modelos disponíveis, m = regras de matching. n ≤ 30, m ≤ 5 → efetivamente O(1) |
| **Cálculo de créditos** | O(1) | Lookup direto: `modelo.creditMultiplier × tokens` |
| **Validação de cupom** | O(1) | Lookup por código (índice único no PostgreSQL) |
| **Comissão de parceiro** | O(1) | Cálculo aritmético direto sobre valor pago |
| **Busca no histórico de chat** | O(log n) | Full-text search via GIN index no PostgreSQL |
| **Semantic cache lookup** | O(log n) | Busca por embedding via pgvector (ANN/IVFFlat index) |
| **Chat history loading** | O(k) | k = mensagens da conversa. Paginado, max 50 por página |
| **Dashboard do parceiro (KPIs)** | O(1) | Leitura de campos pre-agregados na tabela `partners` |
| **Anti-fraude (chain of rules)** | O(r) | r = número de regras (fixo em 8) → O(1) |
| **Pagamento em lote (batch Pix)** | O(p) | p = parceiros elegíveis. Linear, inevitável |
| **Geração de relatório** | O(n log n) | n = registros no período. Sort + aggregate |

### 10.2 Estratégias de Otimização

| Problema | Solução | Complexidade Resultante |
|----------|---------|------------------------|
| Chat history cresce indefinidamente | Paginação cursor-based (keyset pagination) | O(log n) por página |
| Busca textual em milhões de mensagens | GIN index + tsvector no PostgreSQL | O(log n) |
| Semantic cache com milhões de embeddings | pgvector com IVFFlat index (ANN) | O(√n) amortizado |
| Dashboard com agregações complexas | Materialized views atualizadas via CQRS events | O(1) leitura |
| Rate limiting por usuário | Redis sliding window counter | O(1) |
| Listagem de modelos | Cache Redis com TTL 5min | O(1) |

---

## 11. ESTRATÉGIA DE TESTES

### 11.1 Pirâmide de Testes

```
           ╱╲
          ╱  ╲        E2E Tests (Playwright)
         ╱ 5% ╲       → Fluxos críticos: onboarding, chat, checkout, parceiro
        ╱──────╲
       ╱        ╲     Integration Tests (Testcontainers + Spring Boot Test)
      ╱   20%    ╲    → API endpoints, JPA repos, external service mocks
     ╱────────────╲
    ╱              ╲   Unit Tests (JUnit 5 + Mockito)
   ╱     75%        ╲  → Domain services, use cases, validators, calculators
  ╱──────────────────╲
```

### 11.2 Ferramentas

| Tipo | Ferramenta | Propósito |
|------|-----------|-----------|
| Unit Tests | JUnit 5 + Mockito + AssertJ | Testes de domínio e use cases |
| Integration Tests | Spring Boot Test + Testcontainers | Testes de API e persistência |
| Architecture Tests | ArchUnit | Enforce regras de Clean Architecture |
| E2E Tests | Playwright (frontend) | Fluxos completos usuário→sistema |
| Performance Tests | Gatling | Load testing de endpoints críticos |
| Contract Tests | Spring Cloud Contract | Contratos API entre frontend/backend |
| Mutation Tests | PITest | Qualidade dos testes unitários |
| Coverage | JaCoCo | Cobertura de código |

### 11.3 Metas de Cobertura

| Camada | Meta de Cobertura | Justificativa |
|--------|-------------------|---------------|
| **Domain** | ≥ 95% | Core do negócio — zero margem para bugs |
| **Application (Use Cases)** | ≥ 90% | Orquestração — deve estar 100% coberto por fluxos |
| **Infrastructure** | ≥ 70% | Adapters — testados via integration tests |
| **Presentation** | ≥ 80% | Controllers — request/response mapping |
| **Global** | ≥ 85% | Meta geral do projeto |

### 11.4 Exemplos de Testes por Camada

#### Unit Test — Domain Service

```java
@ExtendWith(MockitoExtension.class)
class CommissionCalculatorTest {

    @InjectMocks CommissionCalculator calculator;

    @Test
    @DisplayName("Deve calcular comissão sobre valor pago (não preço cheio)")
    void shouldCalculateCommissionOnPaidAmount() {
        var payment = Payment.of(Money.of(79.20), PaymentMethod.PIX);
        var coupon = Coupon.withCommissionRate(new CommissionRate(20.0));

        Commission result = calculator.calculate(payment, coupon);

        assertThat(result.getAmount()).isEqualTo(Money.of(15.84));
        assertThat(result.getStatus()).isEqualTo(CommissionStatus.PENDING);
    }

    @Test
    @DisplayName("Deve usar override do cupom quando existir")
    void shouldUseOverrideWhenPresent() {
        var partner = Partner.withDefaultCommission(new CommissionRate(20.0));
        var coupon = Coupon.withOverrideCommission(new CommissionRate(30.0));

        CommissionRate rate = calculator.resolveRate(partner, coupon);

        assertThat(rate.getValue()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("Deve bloquear auto-referência como fraude")
    void shouldBlockSelfReferral() {
        var partnerId = UUID.randomUUID();
        var context = FraudContext.builder()
            .partnerId(partnerId)
            .convertedUserId(partnerId) // mesmo ID = auto-referência
            .build();

        assertThatThrownBy(() -> calculator.validateAttribution(context))
            .isInstanceOf(FraudDetectedException.class)
            .hasMessageContaining("auto-referência");
    }
}
```

#### Integration Test — API Endpoint

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class CouponControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired TestRestTemplate restTemplate;
    @Autowired CouponRepository couponRepo;

    @Test
    @DisplayName("POST /api/v1/coupons/validate - cupom válido retorna desconto")
    void shouldValidateCouponAndReturnDiscount() {
        // Given
        couponRepo.save(Coupon.create("MARIA20", 20.0, CouponStatus.ACTIVE));

        // When
        var response = restTemplate.postForEntity(
            "/api/v1/coupons/validate",
            new ValidateCouponRequest("MARIA20", "PLAN_PRO"),
            CouponValidationResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isValid()).isTrue();
        assertThat(response.getBody().getDiscountAmount()).isEqualTo(19.80);
        assertThat(response.getBody().getFinalPrice()).isEqualTo(79.20);
    }

    @Test
    @DisplayName("POST /api/v1/coupons/validate - cupom expirado retorna erro")
    void shouldReturnErrorForExpiredCoupon() {
        couponRepo.save(Coupon.expired("OLD50"));

        var response = restTemplate.postForEntity(
            "/api/v1/coupons/validate",
            new ValidateCouponRequest("OLD50", "PLAN_PRO"),
            ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().getError().getCode()).isEqualTo("COUPON_EXPIRED");
    }
}
```

### 11.5 Testes Frontend (React)

| Tipo | Ferramenta | Cobertura |
|------|-----------|-----------|
| Components | Vitest + Testing Library | ≥ 85% dos componentes |
| Hooks | Vitest | ≥ 90% dos custom hooks |
| E2E | Playwright | 15 fluxos críticos |
| Visual Regression | Chromatic (Storybook) | Todos os componentes do Design System |
| Accessibility | jest-axe + Lighthouse | WCAG 2.1 AA compliance |

---

## 12. CI/CD E DevOps

### 12.1 Pipeline GitHub Actions

```yaml
# .github/workflows/ci.yml
name: CI Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  # ──── BACKEND ────
  backend-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: iaggregator_test
          POSTGRES_PASSWORD: test
        ports: ['5432:5432']
      redis:
        image: redis:7-alpine
        ports: ['6379:6379']
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'
      - name: Run Tests + Coverage
        run: mvn verify -P coverage
      - name: Check Coverage Threshold
        run: |
          COVERAGE=$(cat target/site/jacoco/jacoco.csv | tail -1 | cut -d',' -f4)
          if [ "$COVERAGE" -lt 85 ]; then echo "Coverage below 85%!"; exit 1; fi
      - name: Architecture Tests
        run: mvn test -pl ia-aggregator-infrastructure -Dtest=ArchitectureTest
      - name: Upload Coverage Report
        uses: codecov/codecov-action@v4

  backend-build:
    needs: backend-test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin', cache: 'maven' }
      - run: mvn package -DskipTests
      - name: Build Docker Image
        run: docker build -t iaggregator-api:${{ github.sha }} .

  # ──── FRONTEND ────
  frontend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20', cache: 'pnpm' }
      - run: pnpm install --frozen-lockfile
      - run: pnpm lint
      - run: pnpm type-check
      - run: pnpm test --coverage
      - run: pnpm build

  # ──── E2E ────
  e2e-test:
    needs: [backend-build, frontend-test]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Start Services (docker-compose)
        run: docker-compose -f docker-compose.test.yml up -d
      - name: Run Playwright
        run: pnpm exec playwright test
      - uses: actions/upload-artifact@v4
        if: failure()
        with:
          name: playwright-report
          path: playwright-report/

  # ──── DEPLOY ────
  deploy-staging:
    needs: [backend-build, frontend-test]
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    steps:
      - name: Deploy Backend to Railway (staging)
        run: railway up --service api-staging
      - name: Deploy Frontend to Vercel (staging)
        run: vercel deploy --env preview

  deploy-production:
    needs: [e2e-test]
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment: production
    steps:
      - name: Deploy Backend to Railway (production)
        run: railway up --service api-production
      - name: Deploy Frontend to Vercel (production)
        run: vercel deploy --prod
      - name: Run DB Migrations
        run: railway run mvn flyway:migrate
      - name: Smoke Tests
        run: curl -f https://api.iaggregator.com.br/actuator/health
```

### 12.2 Ambientes

| Ambiente | Backend | Frontend | Banco | Propósito |
|----------|---------|----------|-------|-----------|
| **Local** | `localhost:8080` | `localhost:3000` | Docker Compose | Desenvolvimento |
| **Staging** | Railway (staging) | Vercel Preview | Supabase (staging) | QA + PO review |
| **Production** | Railway (prod) | Vercel (prod) | Supabase (prod) | Usuários reais |

### 12.3 Branching Strategy (GitHub Flow)

```
main ──────────────────────────────────────▶ (production)
  │
  ├── feature/US-001-send-message ──▶ PR ──▶ merge
  ├── feature/US-020-credit-system ──▶ PR ──▶ merge
  ├── fix/coupon-validation-edge-case ──▶ PR ──▶ merge
  └── hotfix/payment-webhook-timeout ──▶ PR ──▶ merge (fast)
```

---

## 13. SEGURANÇA E LGPD

### 13.1 Autenticação e Autorização

```
┌──────────────────────────────────────────┐
│ Spring Security Filter Chain              │
│                                          │
│ 1. CorsFilter                            │
│ 2. RateLimitFilter (Redis sliding window)│
│ 3. JwtAuthenticationFilter               │
│ 4. TenantContextFilter (set org_id)      │
│ 5. AuditFilter (log action)             │
│ 6. Controller execution                 │
└──────────────────────────────────────────┘

Tokens:
- Access Token: JWT, 15 min TTL, contém userId + orgId + roles
- Refresh Token: opaque, 7 days TTL, stored in HttpOnly cookie
- Partner Token: JWT separado, 24h TTL, role=PARTNER
```

### 13.2 Roles e Permissões

| Role | Permissões |
|------|-----------|
| `USER_FREE` | Chat (5 modelos), 300 créditos, upload docs (5MB) |
| `USER_STARTER` | Chat (todos modelos), 1000 créditos, upload docs (50MB), personas |
| `USER_PRO` | Tudo do Starter + 4000 créditos + RAG + templates + export |
| `TEAM_MEMBER` | Tudo do Pro + workspace compartilhado |
| `TEAM_ADMIN` | Tudo do Member + RBAC + analytics do workspace |
| `PARTNER` | Dashboard parceiro + extrato + cupons (somente leitura) |
| `ADMIN` | Tudo + gestão de usuários, parceiros, planos, analytics global |

### 13.3 LGPD Compliance Checklist

| Requisito | Implementação | Sprint |
|-----------|--------------|--------|
| Consentimento granular | Checkbox separados: ToS, Privacy, Analytics opt-in | S1 |
| Right to access (portabilidade) | GET /api/v1/users/me/data-export (JSON) | S2 |
| Right to erasure | DELETE /api/v1/auth/account → soft delete imediato, hard delete 72h (job) | S1 |
| Data minimization | Chat logs: 90 dias em produção, depois anonymize | S3 |
| DPO nomeado | Documentação + contato público dpo@iaggregator.com.br | S1 |
| ROPA (registro de operações) | Tabela audit_logs com todas operações de tratamento | S1 |
| Breach notification | Runbook + template de comunicação em <72h | S4 |
| Cookies consent | Banner first-party apenas, sem third-party tracking | S1 |

### 13.4 Segurança da Aplicação

| Controle | Implementação |
|----------|--------------|
| Encryption at rest | PostgreSQL TDE + Cloudflare R2 server-side encryption |
| Encryption in transit | TLS 1.3 obrigatório em todas as conexões |
| Secrets management | Environment variables via Railway/Vercel (zero secrets em código) |
| SQL Injection | Spring Data JPA (parameterized queries) + input validation |
| XSS | React (auto-escape) + Content-Security-Policy headers |
| CSRF | SameSite cookies + CSRF token em forms |
| Rate Limiting | Redis sliding window: 100 req/min (free), 500 req/min (paid) |
| Audit Trail | `audit_logs` table (imutável) para ações críticas |
| Dependency scanning | Dependabot + Snyk (GitHub Actions) |
| Pen testing | Trimestral a partir de 1K users |

---

## 14. PLANO DE SPRINTS

### 14.1 Parâmetros

- **Duração do Sprint:** 2 semanas
- **Velocity MVP (2 devs + 1 QA):** 24 SP/sprint
- **Velocity Growth (4 devs + 1 QA):** 42 SP/sprint
- **Definition of Ready:** User story COMO/QUERO/PARA + ACs + wireframe + API spec
- **Definition of Done:** Code review + testes (≥85%) + QA manual + deploy staging + PO aceite

### 14.2 Fase MVP — Sprints S1 a S7 (14 semanas)

#### Sprint S1 (Semanas 1-2) — Fundação
| Story | Descrição | SP |
|-------|-----------|-----|
| INFRA-01 | Setup: Maven multi-module, CI/CD, Docker Compose, Flyway, Supabase | 5 |
| INFRA-02 | Setup: Next.js 15, Tailwind, Shadcn/UI, Storybook, Design tokens | 5 |
| AUTH-01 | Registro com email + senha (bcrypt) + JWT + refresh token | 5 |
| AUTH-02 | Login email/senha + Login Google OAuth2 | 5 |
| AUTH-03 | Middleware de auth + roles + tenant context | 3 |
| **Total** | | **23 SP** |
| **Entregável** | Auth funcional + Projeto estruturado + CI/CD rodando |

#### Sprint S2 (Semanas 3-4) — Chat Core
| Story | Descrição | SP |
|-------|-----------|-----|
| CHAT-01 | Criar conversa + enviar mensagem + receber resposta (1 modelo) | 8 |
| CHAT-02 | Streaming SSE (Server-Sent Events) de respostas | 5 |
| CHAT-03 | Seletor de modelo com custo em créditos visível | 3 |
| CHAT-04 | Interface de chat: input, mensagens, indicador de digitação | 5 |
| BILL-01 | Modelo de dados de planos + créditos + tabela de multiplicadores | 3 |
| **Total** | | **24 SP** |
| **Entregável** | Chat funcional com 5 modelos + streaming |

#### Sprint S3 (Semanas 5-6) — Billing + Multi-Modelo
| Story | Descrição | SP |
|-------|-----------|-----|
| BILL-02 | Sistema de créditos: dedução por mensagem, saldo em tempo real | 5 |
| BILL-03 | Checkout com Pix (Asaas) + Cartão (Stripe) | 8 |
| BILL-04 | Plano gratuito (300cr, 5 modelos) + ativação automática no registro | 3 |
| CHAT-05 | Troca de modelo mid-chat mantendo contexto | 5 |
| UX-01 | Dark/light mode + interface 100% PT-BR | 3 |
| **Total** | | **24 SP** |
| **Entregável** | Pagamento funcional + créditos + troca de modelo |

#### Sprint S4 (Semanas 7-8) — Auto-Routing + Imagens + Docs
| Story | Descrição | SP |
|-------|-----------|-----|
| AI-01 | Auto-routing inteligente (Strategy pattern) | 8 |
| IMG-01 | Geração de imagens (Flux + Stable Diffusion via API) | 5 |
| DOC-01 | Upload de documentos (PDF, TXT, CSV) + armazenamento R2 | 5 |
| DOC-02 | Chat com documentos (Q&A básico) | 5 |
| UX-02 | Responsivo mobile | 3 |
| **Total** | | **26 SP** → ajustar com velocity real |
| **Entregável** | Auto-select modelo + Imagens + Upload docs |

#### Sprint S5 (Semanas 9-10) — Personas + Web Search + Inovações
| Story | Descrição | SP |
|-------|-----------|-----|
| PER-01 | Custom personas com system prompt editável | 5 |
| PER-02 | 10 personas pré-construídas PT-BR | 3 |
| WEB-01 | Busca web em tempo real (via modelo com web access) | 5 |
| INV-01 | Dashboard de custo transparente por conversa | 5 |
| INV-02 | Modo Consultor (IA faz perguntas antes de responder) | 3 |
| PRM-01 | 50 prompts prontos por profissão (PT-BR) | 2 |
| **Total** | | **23 SP** |
| **Entregável** | Personas + Web search + 2 inovações exclusivas |

#### Sprint S6 (Semanas 11-12) — Organização + Landing + Calculadora
| Story | Descrição | SP |
|-------|-----------|-----|
| PRM-02 | Histórico pesquisável + pastas/organização de chats | 5 |
| PRM-03 | Biblioteca de prompts salvos pelo usuário | 3 |
| INV-03 | Calculadora de economia na landing page | 3 |
| LAND-01 | Landing page completa: hero, features, pricing, FAQ, CTA | 8 |
| SEG-01 | Criptografia dados + LGPD compliance básico | 3 |
| **Total** | | **22 SP** |
| **Entregável** | Landing page live + Organização de chats |

#### Sprint S7 (Semanas 13-14) — Onboarding + QA + Beta
| Story | Descrição | SP |
|-------|-----------|-----|
| UX-03 | Tour de onboarding guiado (4 fluxos por persona) | 5 |
| SUP-01 | Help center / FAQ em PT-BR | 3 |
| SUP-02 | Suporte por email/ticket | 2 |
| QA-01 | Testes de carga (Gatling) | 3 |
| QA-02 | Bug fixes críticos do QA | 5 |
| QA-03 | Smoke tests + monitoramento Grafana | 3 |
| **Total** | | **21 SP** |
| **Entregável** | **MVP COMPLETO — Beta privado 200 usuários** |

### 14.3 Fase Growth — Sprints S8 a S13 (12 semanas)

| Sprint | Foco | Stories Principais |
|--------|------|--------------------|
| **S8** | Partners + Retenção | Cadastro parceiro + Criar cupom + Auth parceiro + Fork conversa |
| **S9** | Checkout cupom + Tracking | Aplicar cupom checkout + Tracking S2S + Cookie + Click ID |
| **S10** | Comissões + Inovações | Cálculo auto comissão + Carência 7d + Modo Aprendizado + Links públicos |
| **S11** | Dashboard parceiro + Multimodal | Dashboard KPIs parceiro + CSV export + Input multimodal (imagem+áudio) |
| **S12** | Pagamento lote + Anti-fraude | Batch Pix + Admin analytics + Anti-fraude rules + Top-up créditos |
| **S13** | Referral + QA | Link indicação assinante + Partner API + Webhooks + E2E QA completo |

### 14.4 Fase Scale — Sprints S14 a S20

| Sprint | Foco |
|--------|------|
| **S14-S15** | Duel Mode + Split screen + RAG knowledge base |
| **S16-S17** | Team workspace + RBAC + Admin panel |
| **S18-S19** | TTS/STT + Voice input + Prompt coach real-time |
| **S20** | Auto-translate PT→EN→PT + Templates workflow |

### 14.5 Fase Enterprise — Sprints S21 a S30

| Sprint | Foco |
|--------|------|
| **S21-S23** | Chain Mode + Projetos persistentes + PWA mobile |
| **S24-S26** | API pública + Integrações (Slack, Drive, WhatsApp) |
| **S27-S28** | SSO (SAML) + White-label + Self-hosting |
| **S29-S30** | Canvas visual + Agentes autônomos + Deep research |

---

## 15. PLANO DE GO-TO-MARKET TÉCNICO

### 15.1 Checklist de Lançamento

| Item | Responsável | Sprint |
|------|------------|--------|
| SSL + domínio configurado | DevOps | S1 |
| Monitoramento (Grafana + Sentry) | DevOps | S3 |
| Rate limiting configurado | Backend | S3 |
| Backup automático PostgreSQL | DevOps | S4 |
| Status page pública | DevOps | S6 |
| LGPD: termos + política + DPO | Jurídico + Backend | S6 |
| Load test (200 users simultâneos) | QA | S7 |
| Pen test básico | Security | S7 |
| Analytics (PostHog) integrado | Frontend + Backend | S5 |
| Email transacional (Resend) | Backend | S4 |
| Webhook handlers (Stripe/Asaas) | Backend | S3 |
| SEO: sitemap, meta tags, OG tags | Frontend | S6 |
| Error tracking (Sentry) | Both | S3 |

---

## 16. MÉTRICAS E OBSERVABILIDADE

### 16.1 Stack de Observabilidade

```
Logs:       Structured JSON → Grafana Loki
Métricas:   Spring Actuator → Prometheus → Grafana
Traces:     OpenTelemetry → Grafana Tempo
Errors:     Sentry (frontend + backend)
Analytics:  PostHog (product analytics)
Uptime:     BetterUptime / UptimeRobot
```

### 16.2 Dashboards Grafana

| Dashboard | Métricas |
|-----------|----------|
| **API Health** | Latência p50/p95/p99, error rate, throughput, status codes |
| **AI Gateway** | Latência por provider, cache hit rate, fallback rate, custo/minuto |
| **Billing** | Pagamentos/hora, falhas Pix/Cartão, conversão checkout |
| **Credits** | Consumo/minuto, média por user, alertas de esgotamento |
| **Partners** | Conversões/dia, comissões calculadas, fraudes detectadas |
| **Business** | MRR, churn, NPS, DAU/MAU, free→paid conversion |

### 16.3 Alertas Críticos

| Alerta | Condição | Ação |
|--------|---------|------|
| API down | Health check falha 3x consecutivas | PagerDuty → CTO |
| AI provider down | Circuit breaker aberto | Auto-fallback + Slack alert |
| Error rate > 5% | 5xx responses > 5% em 5 min | Slack #alerts + investigar |
| Payment failure spike | >10% falhas em 15 min | Slack #billing + verificar Stripe/Asaas |
| Credit system inconsistency | Saldo negativo detectado | Slack #critical + block user |
| Database CPU > 80% | CPU > 80% por 10 min | Auto-scale + Slack #infra |

---

## 17. RISCOS TÉCNICOS E MITIGAÇÕES

| # | Risco | Prob. | Impacto | Mitigação |
|---|-------|-------|---------|-----------|
| 1 | **API de IA fora do ar** | Alta | Alto | Circuit Breaker + fallback multi-provider + OpenRouter como gateway redundante |
| 2 | **Latência de streaming alta** | Média | Alto | SSE direto (sem proxy), connection pooling, CDN para assets estáticos |
| 3 | **Heavy users destruindo margens** | Média | Alto | Sistema de créditos com teto + throttling + modelos baratos como padrão |
| 4 | **Fraude no programa de parceiros** | Média | Médio | Chain of Responsibility (8 regras) + ML anti-fraude (fase 2) |
| 5 | **Problemas de concorrência (créditos)** | Média | Alto | Pessimistic locking no PostgreSQL + Redis atomic decrement |
| 6 | **LGPD violation** | Baixa | Muito Alto | DPO nomeado + audit logs + data retention policies + right to erasure |
| 7 | **Vendor lock-in (OpenRouter)** | Média | Médio | ACL pattern + APIs diretas como fallback (30% do tráfego) |
| 8 | **Performance degradation com escala** | Média | Alto | CQRS + materialized views + connection pooling + horizontal scaling |
| 9 | **Security breach** | Baixa | Muito Alto | Pen testing trimestral + Dependabot + OWASP top 10 compliance + 2FA |
| 10 | **Technical debt acumulando** | Alta | Médio | Max 15% velocity/sprint para tech debt + ArchUnit enforcement |

---

## APÊNDICE A — Checklist de Qualidade por PR

Todo Pull Request DEVE atender:

- [ ] Testes unitários para nova lógica (≥ 85% coverage do diff)
- [ ] Testes de integração para novos endpoints
- [ ] Sem warnings de compilação
- [ ] Lint passing (backend: Checkstyle, frontend: ESLint)
- [ ] Types corretos (sem `any` no TypeScript, sem raw types no Java)
- [ ] Sem secrets/credentials no código
- [ ] OpenAPI atualizado se novo endpoint
- [ ] Migrations Flyway idempotentes
- [ ] Performance: sem N+1 queries (verificar via Hibernate logging)
- [ ] Segurança: inputs validados, queries parametrizadas
- [ ] LGPD: dados pessoais protegidos, audit log para ações sensíveis
- [ ] Code review por pelo menos 1 senior
- [ ] Storybook atualizado (se componente UI novo)

---

*Seções 4 (Frontend Detalhado) e 5 (Banco de Dados DDL) são documentos complementares anexos.*
