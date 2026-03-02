# Section 4: Microservices Patterns (Applied to Modular Monolith)

These patterns are applied within the modular monolith to ensure clean boundaries between modules, allow future extraction to microservices, and provide enterprise-grade reliability.

---

## 4.1 CQRS (Command Query Responsibility Segregation)

### Applied to: Analytics and Reporting

Write operations (recording metrics) and read operations (dashboards, reports) have fundamentally different access patterns and performance needs. CQRS separates these into distinct models.

```java
// === WRITE SIDE (Command) ===

// application/analytics/port/in/RecordMetricCommand.java
public record RecordMetricCommand(
    UserId userId,
    MetricType type,
    String modelId,
    long value,
    Map<String, String> dimensions
) {}

// application/analytics/usecase/RecordMetricUseCaseImpl.java
@Service
public class RecordMetricUseCaseImpl implements RecordMetricUseCase {
    private final UsageMetricRepository writeRepository;

    @Override
    @Transactional
    public void execute(RecordMetricCommand command) {
        var metric = new UsageMetric(
            command.userId(), command.type(),
            command.modelId(), command.value(),
            LocalDateTime.now(), command.dimensions()
        );
        writeRepository.save(metric);
    }
}

// === READ SIDE (Query) ===

// application/analytics/port/in/GetUsageDashboardQuery.java
public record GetUsageDashboardQuery(
    UserId userId,
    ReportPeriod period,
    TimeGranularity granularity
) {}

// application/analytics/dto/UsageDashboardResponse.java
public record UsageDashboardResponse(
    long totalApiCalls,
    long totalTokensUsed,
    long totalCreditsSpent,
    List<DailyUsagePoint> dailyUsage,
    List<ModelUsageBreakdown> modelBreakdown,
    TrendComparison trendVsPreviousPeriod
) {}

// infrastructure/analytics/persistence/AnalyticsReadRepository.java
@Repository
public class AnalyticsReadRepository {
    private final JdbcTemplate jdbcTemplate;

    /**
     * Optimized read-only query against a materialized view
     * for dashboard data. No JPA overhead for aggregation queries.
     */
    public UsageDashboardData getDashboardData(UUID userId, LocalDate from, LocalDate to,
                                                TimeGranularity granularity) {
        // Uses native SQL against pre-aggregated materialized views
        String sql = """
            SELECT
                date_trunc(:granularity, timestamp) as period,
                metric_type,
                SUM(value) as total_value,
                COUNT(*) as entry_count
            FROM usage_metrics_daily_mv
            WHERE user_id = :userId
              AND timestamp BETWEEN :from AND :to
            GROUP BY period, metric_type
            ORDER BY period
            """;

        return jdbcTemplate.query(sql, /* params, mapper */);
    }
}

// Materialized view refresh (scheduled)
@Component
public class MaterializedViewRefresher {
    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedRate = 300_000) // every 5 minutes
    public void refreshDailyMetrics() {
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY usage_metrics_daily_mv");
    }

    @Scheduled(cron = "0 0 2 * * *") // 2 AM daily
    public void refreshMonthlyMetrics() {
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY usage_metrics_monthly_mv");
    }
}
```

### Database: Materialized Views for Read Model

```sql
-- Write table (normalized)
CREATE TABLE usage_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    team_id UUID REFERENCES teams(id),
    metric_type VARCHAR(50) NOT NULL,
    model_id VARCHAR(100),
    value BIGINT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    dimensions JSONB,
    tenant_id UUID NOT NULL
);

CREATE INDEX idx_usage_metrics_user_ts ON usage_metrics(user_id, timestamp);
CREATE INDEX idx_usage_metrics_tenant_ts ON usage_metrics(tenant_id, timestamp);

-- Read model (materialized view for daily aggregation)
CREATE MATERIALIZED VIEW usage_metrics_daily_mv AS
SELECT
    user_id,
    team_id,
    tenant_id,
    metric_type,
    model_id,
    date_trunc('day', timestamp) as day,
    SUM(value) as total_value,
    COUNT(*) as entry_count,
    MIN(timestamp) as first_entry,
    MAX(timestamp) as last_entry
FROM usage_metrics
GROUP BY user_id, team_id, tenant_id, metric_type, model_id, date_trunc('day', timestamp);

CREATE UNIQUE INDEX idx_daily_mv_unique
    ON usage_metrics_daily_mv(user_id, metric_type, model_id, day);
```

---

## 4.2 SAGA Pattern

### Applied to: Billing + Partner Commission Workflow

The credit purchase flow involves multiple steps that must be coordinated. An orchestrator SAGA manages the sequence and compensating actions on failure.

```java
// application/billing/saga/CreditPurchaseSagaOrchestrator.java
@Service
@Slf4j
public class CreditPurchaseSagaOrchestrator {

    private final PaymentGateway paymentGateway;
    private final CreditAccountRepository creditAccountRepo;
    private final CouponRepository couponRepository;
    private final CommissionRepository commissionRepository;
    private final PartnerRepository partnerRepository;
    private final InvoiceGenerator invoiceGenerator;
    private final BillingEventPublisher eventPublisher;

    @Transactional
    public CreditPurchaseResult execute(PurchaseCreditsCommand command) {
        var saga = new SagaExecution();

        try {
            // Step 1: Validate and redeem coupon (if any)
            Coupon coupon = null;
            Credits finalAmount = command.creditAmount();
            if (command.couponCode() != null) {
                coupon = redeemCoupon(saga, command.couponCode(), command.creditAmount());
                finalAmount = coupon.applyDiscount(command.creditAmount());
            }

            // Step 2: Process payment
            var paymentResult = processPayment(saga, command, finalAmount);

            // Step 3: Add credits to account
            var account = addCredits(saga, command.userId(), command.creditAmount());

            // Step 4: Calculate partner commission (if coupon from partner)
            if (coupon != null && coupon.getPartnerId() != null) {
                calculateCommission(saga, coupon.getPartnerId(), command.creditAmount());
            }

            // Step 5: Generate invoice
            var invoice = generateInvoice(saga, command, paymentResult);

            // Step 6: Publish events
            eventPublisher.publish(new CreditsPurchasedEvent(
                command.userId(), command.creditAmount(),
                paymentResult.transactionId(),
                command.couponCode()
            ));

            return new CreditPurchaseResult(account.getBalance(), invoice.getId());

        } catch (Exception e) {
            log.error("SAGA failed, executing compensations", e);
            saga.compensate();
            throw new CreditPurchaseFailedException(e);
        }
    }

    private Coupon redeemCoupon(SagaExecution saga, String code, Credits amount) {
        var coupon = couponRepository.findByCode(new CouponCode(code))
            .orElseThrow(() -> new CouponNotFoundException(code));

        coupon.redeem();
        couponRepository.save(coupon);

        // Register compensation: undo the coupon redemption
        saga.addCompensation(() -> {
            coupon.undoRedeem();
            couponRepository.save(coupon);
        });

        return coupon;
    }

    private PaymentResult processPayment(SagaExecution saga, PurchaseCreditsCommand cmd,
                                          Credits amount) {
        var gateway = paymentGateways.get(cmd.paymentProvider());
        var result = gateway.processPayment(cmd.toPaymentRequest(amount));

        saga.addCompensation(() -> {
            gateway.refundPayment(result.transactionId());
        });

        return result;
    }

    private CreditAccount addCredits(SagaExecution saga, UserId userId, Credits amount) {
        var account = creditAccountRepo.findByUserIdWithLock(userId)
            .orElseThrow(() -> new CreditAccountNotFoundException(userId));

        account.addCredits(amount, "purchase");
        creditAccountRepo.save(account);

        saga.addCompensation(() -> {
            account.consume(amount, "saga-compensation");
            creditAccountRepo.save(account);
        });

        return account;
    }

    private void calculateCommission(SagaExecution saga, PartnerId partnerId, Credits amount) {
        var partner = partnerRepository.findById(partnerId)
            .orElseThrow(() -> new PartnerNotFoundException(partnerId));

        var commission = partner.calculateCommission(amount);
        commissionRepository.save(commission);

        saga.addCompensation(() -> {
            commissionRepository.delete(commission);
        });
    }
}

// application/billing/saga/SagaExecution.java
public class SagaExecution {
    private final Deque<Runnable> compensations = new ArrayDeque<>();

    public void addCompensation(Runnable compensation) {
        compensations.push(compensation);
    }

    public void compensate() {
        while (!compensations.isEmpty()) {
            try {
                compensations.pop().run();
            } catch (Exception e) {
                // Log and continue with remaining compensations
                // Alert for manual intervention
                log.error("Compensation step failed - manual intervention needed", e);
            }
        }
    }
}
```

---

## 4.3 Anti-Corruption Layer (ACL)

### Applied to: OpenRouter, Stripe, OpenAI, Anthropic API Integrations

The ACL prevents external API concepts from leaking into the domain. Each provider has its own ACL that translates between external DTOs and domain objects.

```java
// infrastructure/aigateway/acl/OpenRouterACL.java
@Component
public class OpenRouterACL {

    /**
     * Translates domain CompletionRequest -> OpenRouter API format.
     * OpenRouter uses a slightly modified OpenAI-compatible format.
     */
    public OpenRouterChatRequest toProviderRequest(CompletionRequest domainRequest) {
        var messages = domainRequest.messages().stream()
            .map(msg -> new OpenRouterMessage(
                mapRole(msg.role()),
                msg.content()
            ))
            .toList();

        return new OpenRouterChatRequest(
            domainRequest.modelId(),  // OpenRouter uses "provider/model" format
            messages,
            domainRequest.temperature(),
            domainRequest.maxTokens(),
            domainRequest.topP(),
            domainRequest.frequencyPenalty(),
            domainRequest.presencePenalty(),
            domainRequest.stream(),
            domainRequest.functions() != null
                ? mapFunctions(domainRequest.functions()) : null
        );
    }

    /**
     * Translates OpenRouter API response -> domain CompletionResponse.
     */
    public CompletionResponse toDomainResponse(OpenRouterChatResponse providerResponse) {
        var choice = providerResponse.choices().getFirst();
        return new CompletionResponse(
            choice.message().content(),
            new TokenUsage(
                providerResponse.usage().promptTokens(),
                providerResponse.usage().completionTokens(),
                providerResponse.usage().totalTokens()
            ),
            providerResponse.model(),
            mapFinishReason(choice.finishReason())
        );
    }

    public StreamChunk toStreamChunk(String sseData) {
        var parsed = JsonUtils.parse(sseData, OpenRouterStreamDelta.class);
        var delta = parsed.choices().getFirst().delta();
        return new StreamChunk(
            parsed.id(),
            delta.content() != null ? delta.content() : "",
            "stop".equals(parsed.choices().getFirst().finishReason()),
            null  // token usage only on final chunk
        );
    }

    private String mapRole(MessageRole domainRole) {
        return switch (domainRole) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            case FUNCTION -> "function";
        };
    }
}

// infrastructure/aigateway/acl/AnthropicACL.java
@Component
public class AnthropicACL {

    /**
     * Anthropic has a different API structure:
     * - system message is a separate field, not in the messages array
     * - uses "max_tokens" differently
     * - model names don't have provider prefix
     */
    public AnthropicMessageRequest toProviderRequest(CompletionRequest domainRequest) {
        // Extract system message (Anthropic wants it separate)
        String systemMessage = domainRequest.messages().stream()
            .filter(m -> m.role() == MessageRole.SYSTEM)
            .map(ChatMessage::content)
            .findFirst()
            .orElse(null);

        // Non-system messages only
        var messages = domainRequest.messages().stream()
            .filter(m -> m.role() != MessageRole.SYSTEM)
            .map(msg -> new AnthropicMessage(
                mapRole(msg.role()),
                List.of(new AnthropicContent("text", msg.content()))
            ))
            .toList();

        // Strip "anthropic/" prefix if present
        String modelId = domainRequest.modelId().replace("anthropic/", "");

        return new AnthropicMessageRequest(
            modelId,
            messages,
            systemMessage,
            domainRequest.maxTokens(),
            domainRequest.temperature(),
            domainRequest.stream()
        );
    }

    public CompletionResponse toDomainResponse(AnthropicMessageResponse response) {
        String content = response.content().stream()
            .filter(c -> "text".equals(c.type()))
            .map(AnthropicContent::text)
            .collect(Collectors.joining());

        return new CompletionResponse(
            content,
            new TokenUsage(
                response.usage().inputTokens(),
                response.usage().outputTokens(),
                response.usage().inputTokens() + response.usage().outputTokens()
            ),
            response.model(),
            mapStopReason(response.stopReason())
        );
    }
}

// infrastructure/billing/acl/StripeACL.java
@Component
public class StripeACL {

    public PaymentResult toDomainResult(PaymentIntent stripeIntent) {
        return new PaymentResult(
            stripeIntent.getId(),
            mapStatus(stripeIntent.getStatus()),
            stripeIntent.getAmount(),
            stripeIntent.getCurrency().toUpperCase()
        );
    }

    public WebhookEvent toDomainEvent(Event stripeEvent) {
        return switch (stripeEvent.getType()) {
            case "payment_intent.succeeded" -> {
                var intent = (PaymentIntent) stripeEvent.getData().getObject();
                yield new WebhookEvent(
                    WebhookEventType.PAYMENT_SUCCEEDED,
                    intent.getId(),
                    Map.of("amount", String.valueOf(intent.getAmount()))
                );
            }
            case "payment_intent.payment_failed" -> {
                var intent = (PaymentIntent) stripeEvent.getData().getObject();
                yield new WebhookEvent(
                    WebhookEventType.PAYMENT_FAILED,
                    intent.getId(),
                    Map.of("error", intent.getLastPaymentError().getMessage())
                );
            }
            case "customer.subscription.updated" -> {
                var sub = (Subscription) stripeEvent.getData().getObject();
                yield new WebhookEvent(
                    WebhookEventType.SUBSCRIPTION_UPDATED,
                    sub.getId(),
                    Map.of("status", sub.getStatus())
                );
            }
            default -> throw new UnhandledWebhookEventException(stripeEvent.getType());
        };
    }

    private PaymentStatus mapStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "succeeded" -> PaymentStatus.COMPLETED;
            case "processing" -> PaymentStatus.PROCESSING;
            case "requires_action" -> PaymentStatus.REQUIRES_ACTION;
            case "canceled" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.FAILED;
        };
    }
}
```

---

## 4.4 Circuit Breaker

### Applied to: All External API Calls (OpenRouter, Stripe, Asaas)

Uses Resilience4j to protect against cascading failures when external services are down.

```java
// infrastructure/config/Resilience4jConfig.java
@Configuration
public class Resilience4jConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        var defaultConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slowCallRateThreshold(80)
            .slowCallDurationThreshold(Duration.ofSeconds(10))
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(3)
            .slidingWindowType(SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .build();

        var openRouterConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(40)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slowCallDurationThreshold(Duration.ofSeconds(30)) // AI calls are slow
            .slidingWindowSize(20)
            .build();

        var stripeConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(30)
            .waitDurationInOpenState(Duration.ofSeconds(45))
            .slidingWindowSize(10)
            .build();

        return CircuitBreakerRegistry.of(
            Map.of(
                "default", defaultConfig,
                "openrouter", openRouterConfig,
                "stripe", stripeConfig,
                "asaas", defaultConfig
            )
        );
    }

    @Bean
    public RetryRegistry retryRegistry() {
        var defaultRetryConfig = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .retryExceptions(IOException.class, TimeoutException.class)
            .ignoreExceptions(BusinessException.class)
            .build();

        return RetryRegistry.of(defaultRetryConfig);
    }
}

// infrastructure/aigateway/adapter/OpenRouterAdapter.java
@Component
public class OpenRouterAdapter extends BaseAiProviderAdapter {

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public OpenRouterAdapter(
            CircuitBreakerRegistry cbRegistry,
            RetryRegistry retryRegistry,
            WebClient.Builder webClientBuilder,
            OpenRouterACL acl) {
        this.circuitBreaker = cbRegistry.circuitBreaker("openrouter");
        this.retry = retryRegistry.retry("openrouter");
        this.acl = acl;
        this.webClient = webClientBuilder
            .baseUrl("https://openrouter.ai")
            .build();
    }

    @Override
    public CompletionResponse complete(CompletionRequest request) {
        // Compose resilience decorators
        Supplier<CompletionResponse> decoratedSupplier = Decorators
            .ofSupplier(() -> doComplete(request))
            .withCircuitBreaker(circuitBreaker)
            .withRetry(retry)
            .withFallback(List.of(
                CallNotPermittedException.class::isInstance
            ), e -> handleFallback(request, e))
            .decorate();

        return decoratedSupplier.get();
    }

    private CompletionResponse handleFallback(CompletionRequest request, Throwable e) {
        // Try alternative provider as fallback
        log.warn("OpenRouter circuit open, falling back to direct provider", e);
        throw new ProviderUnavailableException("openrouter", e);
    }
}
```

---

## 4.5 Event Bus (Spring Application Events)

### Applied to: Domain Event Propagation Across Modules

Spring's `ApplicationEventPublisher` serves as the in-process event bus, keeping modules decoupled.

```java
// infrastructure/event/SpringDomainEventPublisher.java
@Component
public class SpringDomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    // AOP aspect that publishes domain events after repository save
    @Aspect
    @Component
    public static class DomainEventPublishingAspect {
        private final ApplicationEventPublisher publisher;

        @AfterReturning(
            pointcut = "execution(* com.ia.aggregator.domain..repository.*Repository.save(..))",
            returning = "entity"
        )
        public void publishEvents(Object entity) {
            if (entity instanceof BaseEntity baseEntity) {
                baseEntity.getDomainEvents().forEach(publisher::publishEvent);
                baseEntity.clearDomainEvents();
            }
        }
    }
}

// Event flow example: User purchases credits -> commission calculated -> metrics recorded

// Step 1: Billing module publishes CreditsPurchasedEvent
// (published automatically by DomainEventPublishingAspect after save)

// Step 2: Partners module listens
@Component
public class PartnerCommissionEventListener {
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreditsPurchased(CreditsPurchasedEvent event) {
        // Calculate and save commission
    }
}

// Step 3: Analytics module listens
@Component
public class AnalyticsEventListener {
    @Async("analyticsExecutor")
    @EventListener
    public void onCreditsPurchased(CreditsPurchasedEvent event) {
        // Record usage metric (async, non-blocking)
    }

    @Async("analyticsExecutor")
    @EventListener
    public void onModelInvoked(ModelInvokedEvent event) {
        // Record API call metric
    }
}

// Step 4: Notification module listens
@Component
public class NotificationEventListener {
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentFailed(PaymentFailedEvent event) {
        // Send email notification
    }
}
```

---

## 4.6 Outbox Pattern

### Applied to: Reliable Event Publishing for Cross-Module Communication

Ensures domain events are never lost, even if the application crashes between saving data and publishing events.

```java
// infrastructure/event/OutboxEventStore.java
@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {
    @Id
    private UUID id;
    private String eventType;

    @Column(columnDefinition = "jsonb")
    private String payload;

    private String aggregateType;
    private String aggregateId;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private boolean processed;
    private int retryCount;
}

// infrastructure/event/OutboxEventRepository.java
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {
    @Query("SELECT e FROM OutboxEventEntity e WHERE e.processed = false ORDER BY e.createdAt ASC")
    List<OutboxEventEntity> findUnprocessedEvents(Pageable pageable);

    @Modifying
    @Query("UPDATE OutboxEventEntity e SET e.processed = true, e.processedAt = :now WHERE e.id = :id")
    void markAsProcessed(@Param("id") UUID id, @Param("now") LocalDateTime now);
}

// infrastructure/event/TransactionalOutboxPublisher.java
@Component
public class TransactionalOutboxPublisher {
    private final OutboxEventJpaRepository outboxRepo;
    private final ObjectMapper objectMapper;

    /**
     * Saves the event to the outbox table within the same transaction
     * as the business operation. This guarantees at-least-once delivery.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveToOutbox(DomainEvent event) {
        var outboxEvent = new OutboxEventEntity();
        outboxEvent.setId(event.getEventId());
        outboxEvent.setEventType(event.getEventType());
        outboxEvent.setPayload(objectMapper.writeValueAsString(event));
        outboxEvent.setAggregateType(event.getAggregateType());
        outboxEvent.setAggregateId(event.getAggregateId());
        outboxEvent.setCreatedAt(LocalDateTime.now());
        outboxEvent.setProcessed(false);
        outboxEvent.setRetryCount(0);
        outboxRepo.save(outboxEvent);
    }
}

// infrastructure/event/OutboxEventProcessor.java
@Component
@Slf4j
public class OutboxEventProcessor {
    private final OutboxEventJpaRepository outboxRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Polls unprocessed events and publishes them.
     * Runs every 5 seconds. In production, could be replaced
     * with CDC (Change Data Capture) using Debezium.
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxEvents() {
        var events = outboxRepo.findUnprocessedEvents(PageRequest.of(0, 100));

        for (var outboxEvent : events) {
            try {
                var domainEvent = deserializeEvent(outboxEvent);
                eventPublisher.publishEvent(domainEvent);
                outboxRepo.markAsProcessed(outboxEvent.getId(), LocalDateTime.now());
            } catch (Exception e) {
                log.error("Failed to process outbox event: {}", outboxEvent.getId(), e);
                outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);
                // Dead-letter after 5 retries
                if (outboxEvent.getRetryCount() >= 5) {
                    outboxEvent.setProcessed(true); // mark as failed
                    log.error("Outbox event dead-lettered: {}", outboxEvent.getId());
                }
            }
        }
    }
}
```

---

## 4.7 API Gateway Pattern (Internal, for Future Extraction)

### Applied to: Cross-Cutting Concerns as a Gateway Layer

Even as a modular monolith, the presentation layer acts as an internal API gateway, applying cross-cutting concerns uniformly.

```java
// presentation/shared/filter/ApiGatewayFilter.java
@Component
@Order(1)
public class ApiGatewayFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final MeterRegistry meterRegistry;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        var startTime = System.nanoTime();
        var requestId = UUID.randomUUID().toString();
        var tenantId = extractTenantId(request);

        // Set MDC for logging
        MDC.put("requestId", requestId);
        MDC.put("tenantId", tenantId);

        // Add correlation headers
        response.setHeader("X-Request-Id", requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            var duration = (System.nanoTime() - startTime) / 1_000_000.0;
            meterRegistry.timer("http.server.requests",
                "method", request.getMethod(),
                "uri", request.getRequestURI(),
                "status", String.valueOf(response.getStatus())
            ).record(Duration.ofMillis((long) duration));

            MDC.clear();
        }
    }
}

// Module boundary markers for future extraction
// Each module's package acts as a bounded context

// infrastructure/config/ModuleBoundaryConfig.java
@Configuration
public class ModuleBoundaryConfig {

    /**
     * ArchUnit test to enforce module boundaries.
     * This ensures modules only communicate through
     * defined interfaces, making future extraction straightforward.
     */
    // See ArchUnit test in section below
}

// Test: ArchUnit enforcement
// test/architecture/ModuleBoundaryTest.java
@AnalyzeClasses(packages = "com.ia.aggregator")
public class ModuleBoundaryTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_presentation =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..presentation..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_spring =
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule billing_should_not_directly_depend_on_partners =
        noClasses()
            .that().resideInAPackage("..billing..")
            .should().dependOnClassesThat().resideInAPackage("..partners..");

    @ArchTest
    static final ArchRule modules_communicate_through_events =
        noClasses()
            .that().resideInAPackage("..partners..usecase..")
            .should().accessClassesThat().resideInAPackage("..billing..entity..");
}
```
