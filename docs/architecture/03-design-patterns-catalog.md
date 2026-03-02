# Section 3: Design Patterns Catalog

Each pattern is described with its exact module usage, rationale, and code example.

---

## 3.1 Strategy Pattern

### Usage: AI Model Routing (ai-gateway)

Different routing strategies allow users to optimize for quality, speed, or cost when selecting an AI model.

```java
// domain/aigateway/service/strategy/RoutingStrategy.java
public interface RoutingStrategy {
    AiModel select(List<AiModel> candidates, RoutingContext context);
    String getName();
}

// domain/aigateway/service/strategy/QualityFirstRoutingStrategy.java
public class QualityFirstRoutingStrategy implements RoutingStrategy {
    @Override
    public AiModel select(List<AiModel> candidates, RoutingContext context) {
        return candidates.stream()
            .filter(m -> m.supportsAllCapabilities(context.requiredCapabilities()))
            .max(Comparator.comparingDouble(AiModel::getQualityScore))
            .orElseThrow(() -> new NoSuitableModelException("quality-first"));
    }

    @Override
    public String getName() { return "quality-first"; }
}

// domain/aigateway/service/strategy/CostOptimizedRoutingStrategy.java
public class CostOptimizedRoutingStrategy implements RoutingStrategy {
    @Override
    public AiModel select(List<AiModel> candidates, RoutingContext context) {
        return candidates.stream()
            .filter(m -> m.supportsAllCapabilities(context.requiredCapabilities()))
            .max(Comparator.comparingDouble(AiModel::getCostEfficiency))
            .orElseThrow(() -> new NoSuitableModelException("cost-optimized"));
    }

    @Override
    public String getName() { return "cost-optimized"; }
}

// domain/aigateway/service/strategy/BalancedRoutingStrategy.java
public class BalancedRoutingStrategy implements RoutingStrategy {
    @Override
    public AiModel select(List<AiModel> candidates, RoutingContext context) {
        var criteria = RoutingCriteria.balanced();
        return candidates.stream()
            .filter(m -> m.supportsAllCapabilities(context.requiredCapabilities()))
            .max(Comparator.comparingDouble(m -> m.computeRoutingScore(criteria)))
            .orElseThrow(() -> new NoSuitableModelException("balanced"));
    }

    @Override
    public String getName() { return "balanced"; }
}

// application/aigateway/usecase/RouteToModelUseCaseImpl.java
@Service
public class RouteToModelUseCaseImpl implements RouteToModelUseCase {
    private final Map<String, RoutingStrategy> strategies;

    public RouteToModelUseCaseImpl(List<RoutingStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(RoutingStrategy::getName, Function.identity()));
    }

    @Override
    public AiModel route(RouteRequestCommand command) {
        var strategy = strategies.getOrDefault(
            command.routingStrategy(), strategies.get("balanced"));
        var candidates = modelRepository.findAllActive();
        return strategy.select(candidates, command.toContext());
    }
}
```

### Usage: Payment Processing (billing)

Different payment gateways (Stripe, Asaas) implement the same strategy interface.

```java
// application/billing/port/out/PaymentGateway.java
public interface PaymentGateway {
    PaymentResult processPayment(PaymentRequest request);
    SubscriptionResult createSubscription(SubscriptionRequest request);
    void cancelSubscription(String externalId);
    WebhookEvent parseWebhook(String payload, String signature);
    String getProvider();  // "stripe" or "asaas"
}

// infrastructure/billing/adapter/StripePaymentAdapter.java
@Component
public class StripePaymentAdapter implements PaymentGateway {
    @Override
    public String getProvider() { return "stripe"; }

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        // Stripe-specific API calls
    }
}

// infrastructure/billing/adapter/AsaasPaymentAdapter.java
@Component
public class AsaasPaymentAdapter implements PaymentGateway {
    @Override
    public String getProvider() { return "asaas"; }

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        // Asaas-specific API calls (for PIX, Boleto)
    }
}

// application/billing/usecase/PurchaseCreditsUseCaseImpl.java
@Service
public class PurchaseCreditsUseCaseImpl implements PurchaseCreditsUseCase {
    private final Map<String, PaymentGateway> gateways;

    public PurchaseCreditsUseCaseImpl(List<PaymentGateway> gatewayList) {
        this.gateways = gatewayList.stream()
            .collect(Collectors.toMap(PaymentGateway::getProvider, Function.identity()));
    }

    @Override
    public CreditBalanceResponse execute(PurchaseCreditsCommand command) {
        var gateway = gateways.get(command.paymentProvider());
        var result = gateway.processPayment(command.toPaymentRequest());
        // ... credit account, publish events
    }
}
```

---

## 3.2 Factory Pattern

### Usage: AI Model Client Creation (ai-gateway)

Creates the correct provider-specific HTTP client based on the selected model's provider.

```java
// infrastructure/aigateway/adapter/AiProviderClientFactory.java
@Component
public class AiProviderClientFactory {

    private final Map<String, AiProviderClient> clients;

    public AiProviderClientFactory(
            OpenRouterAdapter openRouter,
            OpenAiDirectAdapter openAiDirect,
            AnthropicDirectAdapter anthropicDirect) {
        this.clients = Map.of(
            "openrouter", openRouter,
            "openai", openAiDirect,
            "anthropic", anthropicDirect
        );
    }

    public AiProviderClient getClient(String providerName) {
        var client = clients.get(providerName.toLowerCase());
        if (client == null) {
            throw new UnsupportedProviderException(providerName);
        }
        return client;
    }
}
```

### Usage: Notification Creation (shared)

Creates different notification types (email, push, in-app) based on event type.

```java
// infrastructure/event/NotificationFactory.java
@Component
public class NotificationFactory {

    public Notification create(DomainEvent event) {
        return switch (event) {
            case PaymentFailedEvent e -> Notification.email(
                e.getUserId(), "Payment Failed",
                "payment-failed-template", Map.of("amount", e.getAmount()));

            case CreditPoolDepletedEvent e -> Notification.inApp(
                e.getTeamOwnerId(), "Team credit pool depleted",
                NotificationPriority.HIGH);

            case PartnerTierUpgradedEvent e -> Notification.email(
                e.getPartnerId(), "Tier Upgrade",
                "partner-tier-upgrade-template", Map.of("newTier", e.getNewTier()));

            case TeamInvitationEvent e -> Notification.email(
                e.getInviteeId(), "Team Invitation",
                "team-invitation-template", Map.of("teamName", e.getTeamName()));

            default -> throw new UnsupportedNotificationEventException(event.getClass());
        };
    }
}
```

---

## 3.3 Singleton Pattern

### Usage: Configuration Manager

Spring-managed beans are singletons by default. Critical shared state uses explicit singleton management.

```java
// infrastructure/config/AiModelRegistryConfig.java
@Configuration
public class AiModelRegistryConfig {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AiModelRegistry aiModelRegistry(AiModelRepository repository) {
        return new AiModelRegistry(repository.findAllActive());
    }
}

// domain/aigateway/service/AiModelRegistry.java
public class AiModelRegistry {
    private volatile List<AiModel> models;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public AiModelRegistry(List<AiModel> initialModels) {
        this.models = List.copyOf(initialModels);
    }

    public List<AiModel> getAvailableModels() {
        lock.readLock().lock();
        try {
            return models;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void refresh(List<AiModel> updatedModels) {
        lock.writeLock().lock();
        try {
            this.models = List.copyOf(updatedModels);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
```

### Usage: Semantic Cache Manager

```java
// infrastructure/aigateway/adapter/SemanticCacheManager.java
@Component
public class SemanticCacheManager {
    private final RedissonClient redisson;
    private final RMapCache<String, CachedCompletion> semanticCache;

    public SemanticCacheManager(RedissonClient redisson) {
        this.redisson = redisson;
        this.semanticCache = redisson.getMapCache("ai:semantic-cache");
    }

    public Optional<CachedCompletion> lookup(String promptHash) {
        return Optional.ofNullable(semanticCache.get(promptHash));
    }

    public void store(String promptHash, CachedCompletion completion, long ttlMinutes) {
        semanticCache.put(promptHash, completion, ttlMinutes, TimeUnit.MINUTES);
    }
}
```

---

## 3.4 Observer / Event Pattern

### Usage: Domain Event Propagation

Domain events decouple modules. When a payment completes, the billing module publishes an event that the partners module observes to calculate commissions.

```java
// domain/shared/event/DomainEvent.java
public abstract class DomainEvent {
    private final UUID eventId = UUID.randomUUID();
    private final LocalDateTime occurredAt = LocalDateTime.now();
    private final String eventType = this.getClass().getSimpleName();

    public UUID getEventId() { return eventId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getEventType() { return eventType; }
}

// domain/billing/event/CreditsPurchasedEvent.java
public class CreditsPurchasedEvent extends DomainEvent {
    private final UserId userId;
    private final Credits amount;
    private final String source;
    private final String couponCode;  // nullable

    // constructor, getters
}

// infrastructure/event/SpringEventPublisher.java
@Component
public class SpringEventPublisher implements
        UserEventPublisher, BillingEventPublisher, PartnerEventPublisher,
        ChatEventPublisher, TeamEventPublisher {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(DomainEvent event) {
        publisher.publishEvent(event);
    }
}

// Event Handlers (Observers)

// application/partners/eventhandler/CommissionOnPurchaseHandler.java
@Component
@Transactional
public class CommissionOnPurchaseHandler {

    private final PartnerRepository partnerRepository;
    private final CouponRepository couponRepository;
    private final CommissionRepository commissionRepository;

    @EventListener
    public void handle(CreditsPurchasedEvent event) {
        if (event.getCouponCode() == null) return;

        couponRepository.findByCode(new CouponCode(event.getCouponCode()))
            .flatMap(coupon -> partnerRepository.findById(coupon.getPartnerId()))
            .ifPresent(partner -> {
                var commission = partner.calculateCommission(event.getAmount());
                commissionRepository.save(commission);
                partner.addEarnings(commission.getAmount());
                partnerRepository.save(partner);
            });
    }
}

// application/analytics/eventhandler/UsageTrackingHandler.java
@Component
public class UsageTrackingHandler {

    private final UsageMetricRepository metricRepository;

    @EventListener
    public void handle(CreditsConsumedEvent event) {
        var metric = new UsageMetric(
            event.getUserId(), MetricType.CREDITS_SPENT,
            event.getAmount().amount(), event.getOccurredAt()
        );
        metricRepository.save(metric);
    }

    @EventListener
    public void handle(ModelInvokedEvent event) {
        var metric = new UsageMetric(
            event.getUserId(), MetricType.API_CALL,
            1L, event.getOccurredAt(),
            Map.of("model", event.getModelId())
        );
        metricRepository.save(metric);
    }
}
```

---

## 3.5 Builder Pattern

### Usage: Complex Prompt Construction (ai-gateway)

```java
// domain/aigateway/service/PromptBuilder.java
public class PromptBuilder {
    private String systemMessage;
    private final List<ChatMessage> messages = new ArrayList<>();
    private String modelId;
    private double temperature = 0.7;
    private int maxTokens = 4096;
    private double topP = 1.0;
    private double frequencyPenalty = 0.0;
    private double presencePenalty = 0.0;
    private List<FunctionDefinition> functions;
    private boolean stream = false;

    public PromptBuilder systemMessage(String message) {
        this.systemMessage = message;
        return this;
    }

    public PromptBuilder addUserMessage(String content) {
        messages.add(new ChatMessage(MessageRole.USER, content));
        return this;
    }

    public PromptBuilder addAssistantMessage(String content) {
        messages.add(new ChatMessage(MessageRole.ASSISTANT, content));
        return this;
    }

    public PromptBuilder model(String modelId) {
        this.modelId = modelId;
        return this;
    }

    public PromptBuilder temperature(double temperature) {
        if (temperature < 0.0 || temperature > 2.0)
            throw new IllegalArgumentException("Temperature must be 0.0-2.0");
        this.temperature = temperature;
        return this;
    }

    public PromptBuilder maxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public PromptBuilder stream(boolean stream) {
        this.stream = stream;
        return this;
    }

    public PromptBuilder withFunctions(List<FunctionDefinition> functions) {
        this.functions = functions;
        return this;
    }

    public CompletionRequest build() {
        Objects.requireNonNull(modelId, "Model ID is required");
        if (messages.isEmpty()) throw new IllegalStateException("At least one message required");

        var allMessages = new ArrayList<ChatMessage>();
        if (systemMessage != null) {
            allMessages.add(new ChatMessage(MessageRole.SYSTEM, systemMessage));
        }
        allMessages.addAll(messages);

        return new CompletionRequest(
            modelId, allMessages, temperature, maxTokens,
            topP, frequencyPenalty, presencePenalty, functions, stream
        );
    }
}

// Usage in use case
var request = new PromptBuilder()
    .systemMessage("You are a helpful assistant.")
    .addUserMessage("Explain quantum computing")
    .model("anthropic/claude-3.5-sonnet")
    .temperature(0.7)
    .maxTokens(2048)
    .stream(true)
    .build();
```

### Usage: Report Generation (analytics)

```java
// domain/analytics/service/ReportBuilder.java
public class ReportBuilder {
    private String title;
    private UserId userId;
    private ReportPeriod period;
    private TimeGranularity granularity = TimeGranularity.DAILY;
    private final Set<MetricType> metrics = new LinkedHashSet<>();
    private final Set<String> modelFilters = new LinkedHashSet<>();
    private ReportFormat format = ReportFormat.JSON;
    private boolean includeChartData = false;
    private boolean comparePreviousPeriod = false;

    public ReportBuilder title(String title) { this.title = title; return this; }
    public ReportBuilder forUser(UserId userId) { this.userId = userId; return this; }
    public ReportBuilder period(ReportPeriod period) { this.period = period; return this; }
    public ReportBuilder granularity(TimeGranularity g) { this.granularity = g; return this; }
    public ReportBuilder addMetric(MetricType metric) { this.metrics.add(metric); return this; }
    public ReportBuilder filterByModel(String modelId) { this.modelFilters.add(modelId); return this; }
    public ReportBuilder format(ReportFormat format) { this.format = format; return this; }
    public ReportBuilder withChartData() { this.includeChartData = true; return this; }
    public ReportBuilder comparePrevious() { this.comparePreviousPeriod = true; return this; }

    public GenerateReportCommand build() {
        Objects.requireNonNull(userId, "User ID is required");
        Objects.requireNonNull(period, "Report period is required");
        if (metrics.isEmpty()) throw new IllegalStateException("At least one metric required");

        return new GenerateReportCommand(
            title, userId, period, granularity,
            Set.copyOf(metrics), Set.copyOf(modelFilters),
            format, includeChartData, comparePreviousPeriod
        );
    }
}
```

---

## 3.6 Adapter Pattern

### Usage: External API Integrations (ai-gateway)

Each external AI provider has its own API format. Adapters translate between our domain model and the external API.

```java
// application/aigateway/port/out/AiProviderClient.java
public interface AiProviderClient {
    CompletionResponse complete(CompletionRequest request);
    Flux<StreamChunk> streamComplete(CompletionRequest request);
    ModelHealthStatus checkHealth();
    String getProviderName();
}

// infrastructure/aigateway/adapter/OpenRouterAdapter.java
@Component
public class OpenRouterAdapter implements AiProviderClient {
    private final WebClient webClient;
    private final OpenRouterACL acl;

    @Override
    public CompletionResponse complete(CompletionRequest request) {
        // 1. Translate domain request -> OpenRouter API format
        var openRouterRequest = acl.toProviderRequest(request);

        // 2. Call OpenRouter API
        var openRouterResponse = webClient.post()
            .uri("/api/v1/chat/completions")
            .bodyValue(openRouterRequest)
            .retrieve()
            .bodyToMono(OpenRouterCompletionResponse.class)
            .block();

        // 3. Translate OpenRouter response -> domain response
        return acl.toDomainResponse(openRouterResponse);
    }

    @Override
    public Flux<StreamChunk> streamComplete(CompletionRequest request) {
        var openRouterRequest = acl.toProviderRequest(request);
        openRouterRequest.setStream(true);

        return webClient.post()
            .uri("/api/v1/chat/completions")
            .bodyValue(openRouterRequest)
            .retrieve()
            .bodyToFlux(String.class)
            .filter(line -> !line.equals("[DONE]"))
            .map(acl::toStreamChunk);
    }

    @Override
    public String getProviderName() { return "openrouter"; }
}

// infrastructure/aigateway/adapter/AnthropicDirectAdapter.java
@Component
public class AnthropicDirectAdapter implements AiProviderClient {
    private final WebClient webClient;
    private final AnthropicACL acl;

    @Override
    public CompletionResponse complete(CompletionRequest request) {
        var anthropicRequest = acl.toProviderRequest(request);  // Different format
        var response = webClient.post()
            .uri("/v1/messages")
            .header("anthropic-version", "2023-06-01")
            .bodyValue(anthropicRequest)
            .retrieve()
            .bodyToMono(AnthropicMessageResponse.class)
            .block();
        return acl.toDomainResponse(response);
    }

    @Override
    public String getProviderName() { return "anthropic"; }
}
```

### Usage: Payment Provider Adapters (billing)

```java
// infrastructure/billing/adapter/StripePaymentAdapter.java
@Component
public class StripePaymentAdapter implements PaymentGateway {
    private final StripeClient stripe;

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        // Translate domain -> Stripe API
        var params = PaymentIntentCreateParams.builder()
            .setAmount(request.amountInCents())
            .setCurrency(request.currency().getCode())
            .setCustomer(request.externalCustomerId())
            .setPaymentMethod(request.paymentMethodId())
            .setConfirm(true)
            .build();

        var paymentIntent = PaymentIntent.create(params);

        // Translate Stripe response -> domain
        return new PaymentResult(
            paymentIntent.getId(),
            mapStatus(paymentIntent.getStatus()),
            request.amountInCents()
        );
    }

    @Override
    public String getProvider() { return "stripe"; }
}
```

---

## 3.7 Decorator Pattern

### Usage: Credit Cost Calculation with Discounts/Coupons (billing)

```java
// domain/billing/service/CreditCostCalculator.java
public interface CreditCostCalculator {
    Credits calculate(CompletionRequest request, AiModel model);
}

// domain/billing/service/BaseCreditCostCalculator.java
public class BaseCreditCostCalculator implements CreditCostCalculator {
    @Override
    public Credits calculate(CompletionRequest request, AiModel model) {
        var creditCost = model.getCreditCost();
        int estimatedInputTokens = estimateTokens(request.messages());
        return creditCost.calculateTotal(estimatedInputTokens, request.maxTokens());
    }
}

// domain/billing/service/PlanDiscountDecorator.java
public class PlanDiscountDecorator implements CreditCostCalculator {
    private final CreditCostCalculator delegate;
    private final PlanTier planTier;

    public PlanDiscountDecorator(CreditCostCalculator delegate, PlanTier planTier) {
        this.delegate = delegate;
        this.planTier = planTier;
    }

    @Override
    public Credits calculate(CompletionRequest request, AiModel model) {
        var baseCost = delegate.calculate(request, model);
        double discount = planTier.getCreditDiscount(); // e.g., PRO = 0.10 (10% off)
        return baseCost.multiply(1.0 - discount);
    }
}

// domain/billing/service/CouponDiscountDecorator.java
public class CouponDiscountDecorator implements CreditCostCalculator {
    private final CreditCostCalculator delegate;
    private final Coupon coupon;

    public CouponDiscountDecorator(CreditCostCalculator delegate, Coupon coupon) {
        this.delegate = delegate;
        this.coupon = coupon;
    }

    @Override
    public Credits calculate(CompletionRequest request, AiModel model) {
        var baseCost = delegate.calculate(request, model);
        return coupon.applyDiscount(baseCost);
    }
}

// domain/billing/service/VolumeDiscountDecorator.java
public class VolumeDiscountDecorator implements CreditCostCalculator {
    private final CreditCostCalculator delegate;
    private final Credits monthlyUsage;

    @Override
    public Credits calculate(CompletionRequest request, AiModel model) {
        var baseCost = delegate.calculate(request, model);
        // Tiered volume discount
        double discount = 0.0;
        if (monthlyUsage.amount() > 100_000) discount = 0.15;
        else if (monthlyUsage.amount() > 50_000) discount = 0.10;
        else if (monthlyUsage.amount() > 10_000) discount = 0.05;
        return baseCost.multiply(1.0 - discount);
    }
}

// Composition in use case:
CreditCostCalculator calculator = new BaseCreditCostCalculator();
calculator = new PlanDiscountDecorator(calculator, user.getPlanTier());
if (activeCoupon != null) {
    calculator = new CouponDiscountDecorator(calculator, activeCoupon);
}
calculator = new VolumeDiscountDecorator(calculator, monthlyUsage);
Credits finalCost = calculator.calculate(request, selectedModel);
```

---

## 3.8 Chain of Responsibility Pattern

### Usage: Request Validation Pipeline (shared)

```java
// application/shared/validation/ValidationHandler.java
public abstract class ValidationHandler<T> {
    private ValidationHandler<T> next;

    public ValidationHandler<T> setNext(ValidationHandler<T> next) {
        this.next = next;
        return next;
    }

    public final void handle(T request) {
        validate(request);
        if (next != null) {
            next.handle(request);
        }
    }

    protected abstract void validate(T request);
}

// Usage: Chat message validation chain

// application/chat/validation/ContentModerationHandler.java
public class ContentModerationHandler extends ValidationHandler<SendMessageCommand> {
    @Override
    protected void validate(SendMessageCommand command) {
        if (containsProhibitedContent(command.content())) {
            throw new ContentPolicyViolationException("Message violates content policy");
        }
    }
}

// application/chat/validation/CreditSufficiencyHandler.java
public class CreditSufficiencyHandler extends ValidationHandler<SendMessageCommand> {
    private final CreditAccountRepository creditRepo;

    @Override
    protected void validate(SendMessageCommand command) {
        var account = creditRepo.findByUserId(command.userId())
            .orElseThrow(() -> new CreditAccountNotFoundException(command.userId()));
        var estimatedCost = estimateCost(command);
        if (account.getBalance().isLessThan(estimatedCost)) {
            throw new InsufficientCreditsException(account.getBalance(), estimatedCost);
        }
    }
}

// application/chat/validation/RateLimitHandler.java
public class RateLimitHandler extends ValidationHandler<SendMessageCommand> {
    @Override
    protected void validate(SendMessageCommand command) {
        if (isRateLimited(command.userId())) {
            throw new RateLimitExceededException(command.userId());
        }
    }
}

// application/chat/validation/MessageValidationChainFactory.java
@Component
public class MessageValidationChainFactory {
    public ValidationHandler<SendMessageCommand> createChain(/*dependencies*/) {
        var rateLimitHandler = new RateLimitHandler();
        var creditHandler = new CreditSufficiencyHandler(creditRepo);
        var contentHandler = new ContentModerationHandler();

        rateLimitHandler.setNext(creditHandler).setNext(contentHandler);
        return rateLimitHandler;
    }
}
```

### Usage: Anti-Fraud Rule Engine (billing)

```java
// domain/billing/service/fraud/FraudRule.java
public abstract class FraudRule {
    private FraudRule next;

    public FraudRule setNext(FraudRule next) {
        this.next = next;
        return next;
    }

    public final FraudCheckResult check(PaymentRequest request) {
        var result = evaluate(request);
        if (result.isBlocked()) return result;
        if (next != null) return next.check(request);
        return FraudCheckResult.approved();
    }

    protected abstract FraudCheckResult evaluate(PaymentRequest request);
}

// Concrete rules:
public class VelocityCheckRule extends FraudRule {
    @Override
    protected FraudCheckResult evaluate(PaymentRequest request) {
        // Block if > 5 purchases in last hour
        int recentPurchases = countRecentPurchases(request.userId(), Duration.ofHours(1));
        if (recentPurchases > 5) {
            return FraudCheckResult.blocked("Velocity limit exceeded");
        }
        return FraudCheckResult.approved();
    }
}

public class AmountThresholdRule extends FraudRule {
    @Override
    protected FraudCheckResult evaluate(PaymentRequest request) {
        if (request.amountInCents() > 500_000) { // > R$5.000
            return FraudCheckResult.flaggedForReview("High amount transaction");
        }
        return FraudCheckResult.approved();
    }
}

public class GeoLocationRule extends FraudRule {
    @Override
    protected FraudCheckResult evaluate(PaymentRequest request) {
        if (isUnusualLocation(request.userId(), request.ipAddress())) {
            return FraudCheckResult.flaggedForReview("Unusual location");
        }
        return FraudCheckResult.approved();
    }
}
```

---

## 3.9 Template Method Pattern

### Usage: Base AI Model Communication Flow (ai-gateway)

```java
// infrastructure/aigateway/adapter/BaseAiProviderAdapter.java
public abstract class BaseAiProviderAdapter implements AiProviderClient {

    // Template method - defines the algorithm skeleton
    @Override
    public final CompletionResponse complete(CompletionRequest request) {
        // Step 1: Validate request
        validateRequest(request);

        // Step 2: Transform to provider format (abstract)
        var providerRequest = transformRequest(request);

        // Step 3: Execute API call (abstract)
        var providerResponse = executeCall(providerRequest);

        // Step 4: Transform response back (abstract)
        var response = transformResponse(providerResponse);

        // Step 5: Record metrics (concrete)
        recordMetrics(request, response);

        return response;
    }

    // Steps that subclasses must implement
    protected abstract Object transformRequest(CompletionRequest request);
    protected abstract Object executeCall(Object providerRequest);
    protected abstract CompletionResponse transformResponse(Object providerResponse);

    // Steps with default implementation that can be overridden
    protected void validateRequest(CompletionRequest request) {
        if (request.messages().isEmpty()) {
            throw new InvalidPromptException("Messages cannot be empty");
        }
    }

    // Concrete step
    private void recordMetrics(CompletionRequest request, CompletionResponse response) {
        // Micrometer metrics recording
    }
}

// Subclass: OpenRouter
public class OpenRouterAdapter extends BaseAiProviderAdapter {
    @Override
    protected Object transformRequest(CompletionRequest request) {
        return acl.toProviderRequest(request);
    }

    @Override
    protected Object executeCall(Object providerRequest) {
        return webClient.post()
            .uri("/api/v1/chat/completions")
            .bodyValue(providerRequest)
            .retrieve()
            .bodyToMono(OpenRouterCompletionResponse.class)
            .block();
    }

    @Override
    protected CompletionResponse transformResponse(Object providerResponse) {
        return acl.toDomainResponse((OpenRouterCompletionResponse) providerResponse);
    }
}
```

---

## 3.10 Repository Pattern

### Usage: Data Access Abstraction (all modules)

The domain defines repository interfaces. Infrastructure implements them using Spring Data JPA with entity mapping.

```java
// domain/billing/repository/CreditAccountRepository.java
public interface CreditAccountRepository {
    Optional<CreditAccount> findByUserId(UserId userId);
    CreditAccount save(CreditAccount account);
    Optional<CreditAccount> findByUserIdWithLock(UserId userId);  // pessimistic lock
}

// infrastructure/billing/persistence/CreditAccountRepositoryImpl.java
@Repository
public class CreditAccountRepositoryImpl implements CreditAccountRepository {

    private final CreditAccountJpaRepository jpaRepository;
    private final BillingPersistenceMapper mapper;

    @Override
    public Optional<CreditAccount> findByUserId(UserId userId) {
        return jpaRepository.findByUserId(userId.value())
            .map(mapper::toDomain);
    }

    @Override
    public CreditAccount save(CreditAccount account) {
        var jpaEntity = mapper.toJpa(account);
        var saved = jpaRepository.save(jpaEntity);
        return mapper.toDomain(saved);
    }

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public Optional<CreditAccount> findByUserIdWithLock(UserId userId) {
        return jpaRepository.findByUserIdWithLock(userId.value())
            .map(mapper::toDomain);
    }
}

// infrastructure/billing/persistence/repository/CreditAccountJpaRepository.java
public interface CreditAccountJpaRepository extends JpaRepository<CreditAccountJpaEntity, UUID> {
    Optional<CreditAccountJpaEntity> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CreditAccountJpaEntity c WHERE c.userId = :userId")
    Optional<CreditAccountJpaEntity> findByUserIdWithLock(@Param("userId") UUID userId);
}
```

---

## 3.11 Specification Pattern

### Usage: Dynamic Query Building (partners, billing, analytics)

```java
// domain/shared/specification/BaseSpecification.java
public interface BaseSpecification<T> {
    boolean isSatisfiedBy(T entity);
    Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);

    default BaseSpecification<T> and(BaseSpecification<T> other) {
        return new AndSpecification<>(this, other);
    }

    default BaseSpecification<T> or(BaseSpecification<T> other) {
        return new OrSpecification<>(this, other);
    }

    default BaseSpecification<T> not() {
        return new NotSpecification<>(this);
    }
}

// domain/partners/specification/CommissionSpecification.java
public class CommissionSpecification {

    public static BaseSpecification<Commission> byPartner(PartnerId partnerId) {
        return (root, query, cb) -> cb.equal(root.get("partnerId"), partnerId.value());
    }

    public static BaseSpecification<Commission> byDateRange(DateRange range) {
        return (root, query, cb) -> cb.between(
            root.get("createdAt"), range.start(), range.end());
    }

    public static BaseSpecification<Commission> byStatus(CommissionStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static BaseSpecification<Commission> minimumAmount(Credits min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(
            root.get("amount"), min.amount());
    }
}

// Usage in use case:
var spec = CommissionSpecification.byPartner(partnerId)
    .and(CommissionSpecification.byDateRange(dateRange))
    .and(CommissionSpecification.byStatus(CommissionStatus.CONFIRMED));

var commissions = commissionRepository.findAll(spec, pageRequest);
```
