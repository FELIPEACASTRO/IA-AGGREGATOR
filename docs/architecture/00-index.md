# IA-AGGREGATOR Backend Architecture Document

## Platform Overview

Multi-model AI aggregator platform built with **Java 21 + Spring Boot 3.3+ + Maven**.
Architecture: **Modular Monolith** using **Clean Architecture** (Hexagonal) principles.

**Business Modules**: auth, billing, chat, ai-gateway, partners, content, teams, analytics

---

## Document Index

| # | Section | File | Description |
|---|---------|------|-------------|
| 1 | [Maven Multi-Module Structure](./01-maven-multi-module-structure.md) | `01-maven-multi-module-structure.md` | Parent POM, module hierarchy, dependency management, complete package structure |
| 2 | [Clean Architecture Implementation](./02-clean-architecture-implementation.md) | `02-clean-architecture-implementation.md` | Domain entities, value objects, use cases, ports, repositories, and REST endpoints for all 8 modules |
| 3 | [Design Patterns Catalog](./03-design-patterns-catalog.md) | `03-design-patterns-catalog.md` | 11 patterns with specific module usage: Strategy, Factory, Singleton, Observer, Builder, Adapter, Decorator, Chain of Responsibility, Template Method, Repository, Specification |
| 4 | [Microservices Patterns](./04-microservices-patterns.md) | `04-microservices-patterns.md` | CQRS, SAGA, ACL, Circuit Breaker, Event Bus, Outbox Pattern, API Gateway |
| 5 | [SOLID Principles](./05-solid-principles.md) | `05-solid-principles.md` | Concrete code examples for SRP, OCP, LSP, ISP, DIP applied across the codebase |
| 6 | [Key Technical Decisions](./06-key-technical-decisions.md) | `06-key-technical-decisions.md` | SSE streaming, credit engine, model routing algorithm, Redis caching, rate limiting, LGPD, multi-tenancy, JWT auth |
| 7 | [API Design](./07-api-design.md) | `07-api-design.md` | REST conventions, OpenAPI 3.1, Spring Security config, rate limit headers, CORS |
| 8 | [Big O Analysis](./08-big-o-analysis.md) | `08-big-o-analysis.md` | Algorithmic complexity analysis for model routing, credit calculation, commission, chat search, semantic cache |
| 9 | [Error Handling Strategy](./09-error-handling-strategy.md) | `09-error-handling-strategy.md` | Exception hierarchy, global handler, error codes, error response format, retry policies |

---

## Technology Stack Summary

| Category | Technology | Version |
|----------|-----------|---------|
| Language | Java | 21 (with preview features) |
| Framework | Spring Boot | 3.3.5 |
| Build | Maven | Multi-module |
| Database | PostgreSQL | 16+ (with RLS) |
| Migrations | Flyway | 10.x |
| Cache | Redis (Redisson) | 7+ |
| Security | Spring Security + JWT (jjwt) | 0.12.x |
| Resilience | Resilience4j | 2.2.x |
| API Docs | SpringDoc OpenAPI | 2.6.x |
| Mapping | MapStruct + Lombok | 1.6.x / 1.18.x |
| Observability | Micrometer + Prometheus | 1.13.x |
| Testing | JUnit 5 + TestContainers + ArchUnit | - |
| Reactive | WebFlux (for SSE streaming) | - |

---

## Architecture Diagram

```
                          +-----------------------------+
                          |      PRESENTATION LAYER     |
                          |  REST Controllers           |
                          |  SSE Handlers               |
                          |  WebSocket Handlers         |
                          |  Global Exception Handler   |
                          |  JWT Filter                 |
                          |  Rate Limit Filter          |
                          +-------------+---------------+
                                        |
                                        v
                          +-----------------------------+
                          |     APPLICATION LAYER       |
                          |  Use Cases (Interactors)    |
                          |  DTOs / Commands / Queries  |
                          |  Input Ports (interfaces)   |
                          |  Output Ports (interfaces)  |
                          |  Mappers (MapStruct)        |
                          |  Event Handlers             |
                          +-------------+---------------+
                                        |
                                        v
                          +-----------------------------+
                          |       DOMAIN LAYER          |
                          |  Entities                   |
                          |  Value Objects              |
                          |  Domain Services            |
                          |  Domain Events              |
                          |  Repository Interfaces      |
                          |  Specifications             |
                          |  (ZERO framework deps)      |
                          +-----------------------------+
                                        ^
                                        |
                          +-----------------------------+
                          |    INFRASTRUCTURE LAYER     |
                          |  JPA Repository Impls       |
                          |  External API Adapters      |
                          |    - OpenRouter ACL         |
                          |    - Stripe/Asaas ACL       |
                          |    - OAuth Adapters         |
                          |  Redis Cache Adapters       |
                          |  Security Config            |
                          |  Spring Event Publisher     |
                          |  Outbox Event Store         |
                          |  Multi-tenancy Filter       |
                          +-----------------------------+

     +-----------------------------------------------------------+
     |                     COMMON MODULE                          |
     |  Shared VOs, Annotations, Constants, Utils, Exceptions    |
     +-----------------------------------------------------------+
```

---

## Module Communication Rules

1. Modules NEVER import directly from each other's domain entities
2. Cross-module communication happens ONLY through Domain Events (Observer pattern)
3. The Application layer publishes events; other modules' event listeners react
4. ArchUnit tests enforce these boundaries at compile time
5. This architecture allows any module to be extracted to a microservice by:
   - Replacing Spring Application Events with a message broker (Kafka/RabbitMQ)
   - Replacing in-process calls with HTTP/gRPC calls
   - No domain or application layer changes needed
