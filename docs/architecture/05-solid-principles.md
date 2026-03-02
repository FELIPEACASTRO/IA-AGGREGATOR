# Section 5: SOLID Principles - Specific Code Examples

---

## 5.1 Single Responsibility Principle (SRP)

**Definition**: A class should have one, and only one, reason to change.

### Example: Separating Credit Concerns

**WRONG** -- A single class that handles credit calculation, persistence, and notification:

```java
// BAD: Multiple reasons to change
public class CreditService {
    public void purchaseCredits(UserId userId, int amount, String paymentMethod) {
        // Responsibility 1: Payment processing
        var paymentResult = callStripeApi(paymentMethod, amount);

        // Responsibility 2: Credit calculation
        double discount = calculatePlanDiscount(userId);
        int finalCredits = (int)(amount * (1 + discount));

        // Responsibility 3: Persistence
        jdbcTemplate.update("UPDATE credit_accounts SET balance = balance + ? WHERE user_id = ?",
            finalCredits, userId);

        // Responsibility 4: Notification
        emailService.send(userId, "Credits purchased: " + finalCredits);
    }
}
```

**CORRECT** -- Each class has a single responsibility:

```java
// domain/billing/service/CreditCalculationService.java
// Responsibility: ONLY credit math
public class CreditCalculationService {
    public Credits calculateWithDiscount(Credits baseAmount, PlanTier tier, Coupon coupon) {
        var afterPlanDiscount = baseAmount.multiply(1.0 - tier.getDiscount());
        if (coupon != null) {
            return coupon.applyDiscount(afterPlanDiscount);
        }
        return afterPlanDiscount;
    }
}

// application/billing/usecase/PurchaseCreditsUseCaseImpl.java
// Responsibility: ONLY orchestrating the purchase flow
@Service
public class PurchaseCreditsUseCaseImpl implements PurchaseCreditsUseCase {
    private final PaymentGateway paymentGateway;              // payment
    private final CreditAccountRepository creditAccountRepo;   // persistence
    private final CreditCalculationService calculationService; // calculation
    private final BillingEventPublisher eventPublisher;        // event publishing

    @Override
    @Transactional
    public CreditBalanceResponse execute(PurchaseCreditsCommand command) {
        var paymentResult = paymentGateway.processPayment(command.toPaymentRequest());
        var finalCredits = calculationService.calculateWithDiscount(
            command.creditAmount(), command.planTier(), command.coupon());
        var account = creditAccountRepo.findByUserIdWithLock(command.userId()).orElseThrow();
        account.addCredits(finalCredits, "purchase");
        creditAccountRepo.save(account);
        eventPublisher.publish(new CreditsPurchasedEvent(command.userId(), finalCredits));
        return new CreditBalanceResponse(account.getBalance());
    }
}

// Notification handled by a separate event listener (SRP)
@Component
public class BillingNotificationListener {
    @EventListener
    public void onCreditsPurchased(CreditsPurchasedEvent event) {
        // ONLY notification logic
    }
}
```

---

## 5.2 Open/Closed Principle (OCP)

**Definition**: Software entities should be open for extension, but closed for modification.

### Example: Adding New AI Providers Without Modifying Existing Code

The system must support adding new AI providers (e.g., Google Gemini, Mistral) without changing the routing logic or existing adapters.

```java
// application/aigateway/port/out/AiProviderClient.java
// This interface is CLOSED for modification
public interface AiProviderClient {
    CompletionResponse complete(CompletionRequest request);
    Flux<StreamChunk> streamComplete(CompletionRequest request);
    ModelHealthStatus checkHealth();
    String getProviderName();
}

// Existing implementations are not modified when adding a new provider:
// infrastructure/aigateway/adapter/OpenRouterAdapter.java   -- untouched
// infrastructure/aigateway/adapter/OpenAiDirectAdapter.java  -- untouched
// infrastructure/aigateway/adapter/AnthropicDirectAdapter.java -- untouched

// EXTENDING: Add Google Gemini support by creating a NEW class
// infrastructure/aigateway/adapter/GeminiDirectAdapter.java
@Component
public class GeminiDirectAdapter implements AiProviderClient {
    private final WebClient webClient;
    private final GeminiACL acl;

    @Override
    public CompletionResponse complete(CompletionRequest request) {
        var geminiRequest = acl.toProviderRequest(request);
        var response = webClient.post()
            .uri("/v1/models/{model}:generateContent", request.modelId())
            .bodyValue(geminiRequest)
            .retrieve()
            .bodyToMono(GeminiResponse.class)
            .block();
        return acl.toDomainResponse(response);
    }

    @Override
    public String getProviderName() { return "gemini"; }
}

// The factory auto-discovers all AiProviderClient implementations
// via Spring's dependency injection -- no modification needed:
@Component
public class AiProviderClientFactory {
    private final Map<String, AiProviderClient> clients;

    // Spring injects ALL beans implementing AiProviderClient
    public AiProviderClientFactory(List<AiProviderClient> clientList) {
        this.clients = clientList.stream()
            .collect(Collectors.toMap(AiProviderClient::getProviderName, Function.identity()));
    }
    // Adding GeminiDirectAdapter is automatically picked up. Zero modifications.
}
```

### Example: Adding New Payment Methods

```java
// To add PIX payments via Asaas, simply create a new PaymentGateway implementation.
// The PurchaseCreditsUseCase does not need to change.

// NEW file -- no existing files modified
@Component
public class AsaasPixPaymentAdapter implements PaymentGateway {
    @Override
    public String getProvider() { return "asaas-pix"; }

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        // Asaas PIX-specific implementation
    }
}
```

---

## 5.3 Liskov Substitution Principle (LSP)

**Definition**: Objects of a superclass should be replaceable with objects of its subclasses without affecting program correctness.

### Example: Payment Gateway Implementations

Any `PaymentGateway` implementation must honor the contract defined by the interface. Callers should not need to know which implementation is used.

```java
// application/billing/port/out/PaymentGateway.java
public interface PaymentGateway {
    /**
     * Contract:
     * - MUST return PaymentResult with non-null transactionId on success
     * - MUST throw PaymentProcessingException on failure (not return null)
     * - MUST be idempotent for the same idempotencyKey
     * - MUST NOT modify the PaymentRequest object
     */
    PaymentResult processPayment(PaymentRequest request);
    void cancelSubscription(String externalSubscriptionId);
    String getProvider();
}

// Both implementations satisfy the contract identically:

// StripePaymentAdapter
@Component
public class StripePaymentAdapter implements PaymentGateway {
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        try {
            var intent = PaymentIntent.create(buildParams(request));
            // Always returns a result with transactionId
            return new PaymentResult(intent.getId(), mapStatus(intent.getStatus()),
                                     request.amountInCents());
        } catch (StripeException e) {
            // Throws domain exception, never returns null
            throw new PaymentProcessingException("stripe", e.getMessage(), e);
        }
    }
}

// AsaasPaymentAdapter
@Component
public class AsaasPaymentAdapter implements PaymentGateway {
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        try {
            var response = asaasClient.createPayment(buildRequest(request));
            // Same contract: non-null transactionId
            return new PaymentResult(response.getId(), mapStatus(response.getStatus()),
                                     request.amountInCents());
        } catch (AsaasApiException e) {
            // Same exception type as Stripe adapter
            throw new PaymentProcessingException("asaas", e.getMessage(), e);
        }
    }
}

// The use case works identically with either implementation:
@Service
public class PurchaseCreditsUseCaseImpl implements PurchaseCreditsUseCase {
    public CreditBalanceResponse execute(PurchaseCreditsCommand command) {
        // This code works correctly regardless of which PaymentGateway is injected
        PaymentGateway gateway = gateways.get(command.paymentProvider());
        PaymentResult result = gateway.processPayment(command.toPaymentRequest());
        // result.transactionId() is guaranteed non-null by LSP
    }
}
```

### Counter-example (LSP Violation to Avoid)

```java
// BAD: Violates LSP -- subclass changes expected behavior
public class FreeTrialPaymentAdapter implements PaymentGateway {
    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        // VIOLATION: Returns null transactionId, breaking the contract
        return new PaymentResult(null, PaymentStatus.COMPLETED, 0);
    }

    @Override
    public void cancelSubscription(String externalId) {
        // VIOLATION: Throws UnsupportedOperationException
        throw new UnsupportedOperationException("Free trial has no subscription");
    }
}

// CORRECT approach: Free trial should not implement PaymentGateway
// because it fundamentally does not process payments.
// Use a separate FreeTrialService instead.
```

---

## 5.4 Interface Segregation Principle (ISP)

**Definition**: No client should be forced to depend on interfaces it does not use.

### Example: Splitting User Repository Concerns

**WRONG** -- One fat interface forces all implementations to handle everything:

```java
// BAD: Too many methods in one interface
public interface UserRepository {
    User findById(UUID id);
    User save(User user);
    void delete(UUID id);
    List<User> findAll(Specification spec, Pageable page);
    List<User> findByTeamId(UUID teamId);
    long countByPlan(PlanTier tier);
    List<User> findInactiveUsersOlderThan(Duration duration);
    void bulkUpdatePlan(List<UUID> userIds, PlanTier newPlan);
    Map<String, Long> getUserStatsByCountry();
    void anonymizeUser(UUID id);  // LGPD
}
```

**CORRECT** -- Split into focused interfaces:

```java
// domain/auth/repository/UserRepository.java
// Core CRUD operations -- used by auth use cases
public interface UserRepository {
    Optional<User> findById(UserId id);
    Optional<User> findByEmail(String email);
    User save(User user);
    boolean existsByEmail(String email);
    List<User> findAll(UserSpecification spec, PageRequest pageRequest);
}

// domain/auth/repository/UserDataManagementRepository.java
// LGPD compliance operations -- used only by data erasure use case
public interface UserDataManagementRepository {
    void anonymizeUser(UserId id);
    void deleteUserData(UserId id);
    UserDataExport exportUserData(UserId id);
}

// domain/teams/repository/TeamMemberQueryRepository.java
// Team-specific queries -- used only by team use cases
public interface TeamMemberQueryRepository {
    List<User> findByTeamId(TeamId teamId);
}

// domain/analytics/repository/UserAnalyticsRepository.java
// Analytics queries -- used only by analytics/reporting use cases
public interface UserAnalyticsRepository {
    long countByPlan(PlanTier tier);
    Map<String, Long> getUserCountByCountry();
    List<User> findInactiveUsersOlderThan(Duration duration);
}

// Each use case depends only on the interface it needs:
@Service
public class DataErasureUseCaseImpl implements DataErasureUseCase {
    // Only depends on the LGPD-specific interface
    private final UserDataManagementRepository dataManagementRepo;

    @Override
    public void execute(UserId userId) {
        dataManagementRepo.anonymizeUser(userId);
    }
}
```

### Example: AI Provider Client Capabilities

```java
// Not all providers support all features. ISP prevents forcing
// unsupported operations on providers.

// Base interface -- all providers support this
public interface AiProviderClient {
    CompletionResponse complete(CompletionRequest request);
    String getProviderName();
}

// Streaming capability -- only some providers support it
public interface StreamingAiProviderClient extends AiProviderClient {
    Flux<StreamChunk> streamComplete(CompletionRequest request);
}

// Function calling capability
public interface FunctionCallingAiProviderClient extends AiProviderClient {
    CompletionResponse completeWithFunctions(CompletionRequest request,
                                              List<FunctionDefinition> functions);
}

// Image analysis capability
public interface VisionAiProviderClient extends AiProviderClient {
    CompletionResponse completeWithImage(CompletionRequest request, byte[] image);
}

// OpenRouter supports everything
@Component
public class OpenRouterAdapter implements StreamingAiProviderClient,
                                           FunctionCallingAiProviderClient,
                                           VisionAiProviderClient {
    // Implements all capabilities
}

// A simpler provider might only support basic completion
@Component
public class SimpleModelAdapter implements AiProviderClient {
    // Only implements basic completion -- not forced to implement
    // streaming, function calling, or vision
}
```

---

## 5.5 Dependency Inversion Principle (DIP)

**Definition**: High-level modules should not depend on low-level modules. Both should depend on abstractions.

### Example: The Entire Clean Architecture Structure

This is the foundational principle of the architecture. The domain (high-level policy) never depends on infrastructure (low-level detail).

```java
// HIGH-LEVEL: Domain defines the abstraction
// domain/billing/repository/CreditAccountRepository.java
public interface CreditAccountRepository {
    Optional<CreditAccount> findByUserId(UserId userId);
    CreditAccount save(CreditAccount account);
}

// HIGH-LEVEL: Application use case depends on abstraction, NOT on JPA
// application/billing/usecase/ConsumeCreditsUseCaseImpl.java
@Service
public class ConsumeCreditsUseCaseImpl implements ConsumeCreditsUseCase {

    // Depends on domain interface, NOT on CreditAccountRepositoryImpl
    // NOT on JpaRepository, NOT on JdbcTemplate
    private final CreditAccountRepository creditAccountRepo;

    @Override
    @Transactional
    public void execute(ConsumeCreditsCommand command) {
        var account = creditAccountRepo.findByUserIdWithLock(command.userId())
            .orElseThrow(() -> new CreditAccountNotFoundException(command.userId()));

        account.consume(command.amount(), command.reason());
        creditAccountRepo.save(account);
    }
}

// LOW-LEVEL: Infrastructure implements the abstraction
// infrastructure/billing/persistence/CreditAccountRepositoryImpl.java
@Repository
public class CreditAccountRepositoryImpl implements CreditAccountRepository {

    // Depends on Spring Data JPA (low-level detail)
    private final CreditAccountJpaRepository jpaRepo;
    private final BillingPersistenceMapper mapper;

    @Override
    public Optional<CreditAccount> findByUserId(UserId userId) {
        return jpaRepo.findByUserId(userId.value())
            .map(mapper::toDomain);  // JPA entity -> Domain entity
    }

    @Override
    public CreditAccount save(CreditAccount account) {
        var jpaEntity = mapper.toJpa(account);  // Domain entity -> JPA entity
        return mapper.toDomain(jpaRepo.save(jpaEntity));
    }
}
```

### Diagram: Dependency Direction

```
    +---------------------------------------------------+
    |                DOMAIN LAYER                        |
    |  CreditAccountRepository (interface)              |
    |  CreditAccount (entity)                           |
    |  Credits (value object)                           |
    +-------------------^-------------------------------+
                        |
                        | depends on (abstraction)
                        |
    +-------------------+-------------------------------+
    |            APPLICATION LAYER                       |
    |  ConsumeCreditsUseCaseImpl                        |
    |  (uses CreditAccountRepository interface)         |
    +---------------------------------------------------+
                        ^
                        | implements (abstraction)
                        |
    +-------------------+-------------------------------+
    |          INFRASTRUCTURE LAYER                      |
    |  CreditAccountRepositoryImpl                      |
    |  (implements CreditAccountRepository)             |
    |  CreditAccountJpaRepository (Spring Data)         |
    |  CreditAccountJpaEntity (JPA entity)              |
    +---------------------------------------------------+
```

**Key insight**: Both Application and Infrastructure depend on the Domain's abstraction. Infrastructure points inward. If we switch from PostgreSQL to MongoDB, only the Infrastructure layer changes. The domain and application layers are completely untouched.

### Another DIP Example: Token Provider

```java
// Domain defines what it needs (abstraction)
// application/auth/port/out/TokenProvider.java
public interface TokenProvider {
    TokenPair generateTokens(User user);
    UserId validateAccessToken(String token);
    UserId validateRefreshToken(String token);
    void invalidateRefreshToken(String token);
}

// Infrastructure provides the implementation (detail)
// infrastructure/auth/security/JwtTokenProvider.java
@Component
public class JwtTokenProvider implements TokenProvider {
    private final SecretKey signingKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    @Override
    public TokenPair generateTokens(User user) {
        var accessToken = Jwts.builder()
            .subject(user.getId().value().toString())
            .claim("roles", user.getRoleNames())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
            .signWith(signingKey)
            .compact();
        // ... refresh token generation
        return new TokenPair(accessToken, refreshToken, accessTokenExpiration);
    }
}

// If we later switch from JWT to opaque tokens or PASETO,
// we only create a new TokenProvider implementation.
// The auth use cases never change.
```
