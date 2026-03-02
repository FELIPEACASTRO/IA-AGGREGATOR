# Section 2: Clean Architecture Implementation

## 2.1 Architecture Principles

Each business module follows the Hexagonal/Clean Architecture pattern with strict dependency rules:

- **Domain Layer**: Pure Java, zero framework dependencies. Contains entities, value objects, domain services, domain events, repository interfaces (ports), and specifications.
- **Application Layer**: Orchestrates use cases. Depends only on domain. Defines input/output ports. Contains DTOs and mappers.
- **Infrastructure Layer**: Implements domain ports. Contains JPA entities, Spring Data repositories, external API adapters, configuration.
- **Presentation Layer**: HTTP layer only. Translates HTTP requests to application commands/queries and application responses to HTTP responses.

---

## 2.2 AUTH Module

### Domain Entities

```java
// domain/auth/entity/User.java
public class User extends BaseEntity {
    private UserId id;
    private String email;
    private String name;
    private HashedPassword password;
    private Set<Role> roles;
    private OAuthProvider oauthProvider;
    private String oauthExternalId;
    private boolean emailVerified;
    private boolean active;
    private LocalDateTime lastLoginAt;
    private UUID tenantId;

    // Business rules
    public void changePassword(HashedPassword newPassword) {
        this.password = newPassword;
        registerEvent(new PasswordChangedEvent(this.id));
    }

    public boolean hasPermission(String permissionCode) {
        return roles.stream()
            .flatMap(role -> role.getPermissions().stream())
            .anyMatch(p -> p.getCode().equals(permissionCode));
    }

    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
        registerEvent(new UserLoggedInEvent(this.id));
    }
}
```

### Value Objects

```java
// domain/auth/vo/UserId.java
public record UserId(UUID value) {
    public UserId {
        Objects.requireNonNull(value, "User ID cannot be null");
    }
    public static UserId generate() { return new UserId(UUID.randomUUID()); }
}

// domain/auth/vo/HashedPassword.java
public record HashedPassword(String hash) {
    public HashedPassword {
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be empty");
        }
    }
}

// domain/auth/vo/TokenPair.java
public record TokenPair(String accessToken, String refreshToken, long expiresIn) {}
```

### Repository Interfaces

```java
// domain/auth/repository/UserRepository.java
public interface UserRepository {
    Optional<User> findById(UserId id);
    Optional<User> findByEmail(String email);
    Optional<User> findByOAuthProviderAndExternalId(OAuthProvider provider, String externalId);
    User save(User user);
    boolean existsByEmail(String email);
    void deleteById(UserId id);  // LGPD: data erasure
    List<User> findAll(UserSpecification specification, PageRequest pageRequest);
}
```

### Use Cases

```java
// application/auth/port/in/RegisterUserUseCase.java
public interface RegisterUserUseCase {
    UserResponse execute(RegisterUserCommand command);
}

// application/auth/usecase/RegisterUserUseCaseImpl.java
@Service
@Transactional
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserEventPublisher eventPublisher;
    private final AuthMapper mapper;

    @Override
    public UserResponse execute(RegisterUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new UserAlreadyExistsException(command.email());
        }

        var hashedPassword = passwordEncoder.encode(command.password());
        var user = User.create(command.email(), command.name(), hashedPassword);
        var savedUser = userRepository.save(user);

        eventPublisher.publish(new UserRegisteredEvent(savedUser.getId()));
        return mapper.toResponse(savedUser);
    }
}
```

### REST Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/v1/auth/register` | Register new user | Public |
| POST | `/api/v1/auth/login` | Login with credentials | Public |
| POST | `/api/v1/auth/refresh` | Refresh access token | Public (with refresh token) |
| POST | `/api/v1/auth/oauth/{provider}` | OAuth2 login | Public |
| POST | `/api/v1/auth/logout` | Invalidate tokens | Authenticated |
| GET | `/api/v1/auth/me` | Get current user profile | Authenticated |
| PUT | `/api/v1/auth/me` | Update profile | Authenticated |
| POST | `/api/v1/auth/password/change` | Change password | Authenticated |
| POST | `/api/v1/auth/password/reset` | Request password reset | Public |
| DELETE | `/api/v1/auth/me/data` | LGPD data erasure | Authenticated |

---

## 2.3 BILLING Module

### Domain Entities

```java
// domain/billing/entity/CreditAccount.java
public class CreditAccount extends BaseEntity {
    private UUID id;
    private UserId userId;
    private Credits balance;
    private Credits totalPurchased;
    private Credits totalConsumed;
    private PlanTier currentPlan;
    private UUID tenantId;

    public void consume(Credits amount, String reason) {
        if (this.balance.isLessThan(amount)) {
            throw new InsufficientCreditsException(this.balance, amount);
        }
        this.balance = this.balance.subtract(amount);
        this.totalConsumed = this.totalConsumed.add(amount);
        registerEvent(new CreditsConsumedEvent(this.userId, amount, reason));
    }

    public void addCredits(Credits amount, String source) {
        this.balance = this.balance.add(amount);
        this.totalPurchased = this.totalPurchased.add(amount);
        registerEvent(new CreditsPurchasedEvent(this.userId, amount, source));
    }
}

// domain/billing/entity/Subscription.java
public class Subscription extends BaseEntity {
    private UUID id;
    private UserId userId;
    private Plan plan;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime nextBillingDate;
    private BillingCycle billingCycle;
    private String externalSubscriptionId; // Stripe/Asaas ID

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE
            && LocalDateTime.now().isBefore(endDate);
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELLED;
        registerEvent(new SubscriptionCancelledEvent(this.id, this.userId));
    }
}
```

### Value Objects

```java
// domain/billing/vo/Credits.java
public record Credits(long amount) {
    public Credits {
        if (amount < 0) throw new IllegalArgumentException("Credits cannot be negative");
    }
    public static Credits ZERO = new Credits(0);
    public Credits add(Credits other) { return new Credits(this.amount + other.amount); }
    public Credits subtract(Credits other) { return new Credits(this.amount - other.amount); }
    public boolean isLessThan(Credits other) { return this.amount < other.amount; }
    public Credits multiply(double factor) { return new Credits(Math.round(this.amount * factor)); }
}

// domain/billing/vo/CreditCost.java
public record CreditCost(
    String modelId,
    long inputTokenCredits,   // credits per 1K input tokens
    long outputTokenCredits,  // credits per 1K output tokens
    long baseRequestCredits   // base cost per request
) {
    public Credits calculateTotal(long inputTokens, long outputTokens) {
        long inputCost = (inputTokens * inputTokenCredits) / 1000;
        long outputCost = (outputTokens * outputTokenCredits) / 1000;
        return new Credits(inputCost + outputCost + baseRequestCredits);
    }
}
```

### Use Cases

```java
// application/billing/port/in/PurchaseCreditsUseCase.java
public interface PurchaseCreditsUseCase {
    CreditBalanceResponse execute(PurchaseCreditsCommand command);
}

// application/billing/port/in/ConsumeCreditsUseCase.java
public interface ConsumeCreditsUseCase {
    void execute(ConsumeCreditsCommand command);
}

// application/billing/port/out/PaymentGateway.java
public interface PaymentGateway {
    PaymentResult processPayment(PaymentRequest request);
    SubscriptionResult createSubscription(SubscriptionRequest request);
    void cancelSubscription(String externalSubscriptionId);
    WebhookEvent parseWebhook(String payload, String signature);
}
```

### REST Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/v1/billing/credits` | Get credit balance | User |
| POST | `/api/v1/billing/credits/purchase` | Purchase credits | User |
| GET | `/api/v1/billing/credits/transactions` | List transactions | User |
| GET | `/api/v1/billing/plans` | List available plans | Public |
| POST | `/api/v1/billing/subscriptions` | Create subscription | User |
| GET | `/api/v1/billing/subscriptions/current` | Get current subscription | User |
| DELETE | `/api/v1/billing/subscriptions/current` | Cancel subscription | User |
| GET | `/api/v1/billing/invoices` | List invoices | User |
| GET | `/api/v1/billing/invoices/{id}/pdf` | Download invoice PDF | User |
| POST | `/api/v1/billing/webhooks/stripe` | Stripe webhook | Public (signature verified) |
| POST | `/api/v1/billing/webhooks/asaas` | Asaas webhook | Public (signature verified) |

---

## 2.4 CHAT Module

### Domain Entities

```java
// domain/chat/entity/Conversation.java
public class Conversation extends BaseEntity {
    private ConversationId id;
    private UserId userId;
    private String title;
    private String modelId;
    private List<Message> messages;
    private PromptConfig systemPrompt;
    private boolean archived;
    private UUID tenantId;

    public Message addUserMessage(MessageContent content) {
        var message = Message.userMessage(this.id, content, this.messages.size());
        this.messages.add(message);
        registerEvent(new MessageSentEvent(this.id, message.getId()));
        return message;
    }

    public Message addAssistantMessage(MessageContent content, TokenUsage tokenUsage) {
        var message = Message.assistantMessage(this.id, content, this.messages.size(), tokenUsage);
        this.messages.add(message);
        return message;
    }

    public void archive() {
        this.archived = true;
        registerEvent(new ConversationArchivedEvent(this.id));
    }

    public List<Message> getContextWindow(int maxTokens) {
        // Returns messages that fit within the context window
        var contextMessages = new ArrayList<Message>();
        int totalTokens = 0;
        for (int i = messages.size() - 1; i >= 0; i--) {
            var msg = messages.get(i);
            if (totalTokens + msg.getEstimatedTokens() > maxTokens) break;
            contextMessages.addFirst(msg);
            totalTokens += msg.getEstimatedTokens();
        }
        return contextMessages;
    }
}

// domain/chat/entity/Message.java
public class Message extends BaseEntity {
    private UUID id;
    private ConversationId conversationId;
    private MessageRole role;
    private MessageContent content;
    private TokenUsage tokenUsage;
    private int sequenceNumber;
    private List<Attachment> attachments;
    private LocalDateTime createdAt;
}
```

### Value Objects

```java
// domain/chat/vo/TokenUsage.java
public record TokenUsage(int inputTokens, int outputTokens, int totalTokens) {
    public TokenUsage {
        if (inputTokens < 0 || outputTokens < 0)
            throw new IllegalArgumentException("Token counts cannot be negative");
    }
    public int totalTokens() { return inputTokens + outputTokens; }
}

// domain/chat/vo/StreamChunk.java
public record StreamChunk(
    String conversationId,
    String content,
    boolean isComplete,
    TokenUsage tokenUsage  // only set when isComplete
) {}
```

### REST Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/v1/chat/conversations` | Create conversation | User |
| GET | `/api/v1/chat/conversations` | List conversations | User |
| GET | `/api/v1/chat/conversations/{id}` | Get conversation | User |
| DELETE | `/api/v1/chat/conversations/{id}` | Delete conversation | User |
| POST | `/api/v1/chat/conversations/{id}/messages` | Send message (returns SSE stream) | User |
| GET | `/api/v1/chat/conversations/{id}/messages` | Get message history | User |
| PUT | `/api/v1/chat/conversations/{id}/archive` | Archive conversation | User |
| GET | `/api/v1/chat/conversations/search` | Search conversations | User |

---

## 2.5 AI-GATEWAY Module

### Domain Entities

```java
// domain/aigateway/entity/AiModel.java
public class AiModel extends BaseEntity {
    private ModelId id;
    private String name;
    private String displayName;
    private ModelProvider provider;
    private Set<ModelCapability> capabilities;
    private int maxContextTokens;
    private CreditCost creditCost;
    private boolean active;
    private double qualityScore;     // 0.0 - 1.0
    private double latencyScore;     // 0.0 - 1.0, higher = faster
    private double costEfficiency;   // 0.0 - 1.0, higher = cheaper

    public boolean supportsCapability(ModelCapability capability) {
        return capabilities.contains(capability);
    }

    public double computeRoutingScore(RoutingCriteria criteria) {
        return (criteria.qualityWeight() * qualityScore)
             + (criteria.speedWeight() * latencyScore)
             + (criteria.costWeight() * costEfficiency);
    }
}

// domain/aigateway/entity/ModelProvider.java
public class ModelProvider extends BaseEntity {
    private UUID id;
    private String name;          // "openrouter", "openai-direct", "anthropic-direct"
    private String baseUrl;
    private boolean active;
    private HealthStatus healthStatus;
    private LocalDateTime lastHealthCheck;
}
```

### Value Objects

```java
// domain/aigateway/vo/RoutingCriteria.java
public record RoutingCriteria(
    double qualityWeight,    // 0.0-1.0, sum must be 1.0
    double speedWeight,
    double costWeight,
    Set<ModelCapability> requiredCapabilities,
    Integer maxContextNeeded,
    String preferredProvider
) {
    public RoutingCriteria {
        double sum = qualityWeight + speedWeight + costWeight;
        if (Math.abs(sum - 1.0) > 0.01) {
            throw new IllegalArgumentException("Weights must sum to 1.0");
        }
    }

    public static RoutingCriteria balanced() {
        return new RoutingCriteria(0.4, 0.3, 0.3, Set.of(), null, null);
    }
    public static RoutingCriteria qualityFirst() {
        return new RoutingCriteria(0.7, 0.15, 0.15, Set.of(), null, null);
    }
    public static RoutingCriteria costOptimized() {
        return new RoutingCriteria(0.2, 0.2, 0.6, Set.of(), null, null);
    }
}

// domain/aigateway/vo/ModelCapability.java
public enum ModelCapability {
    TEXT_GENERATION, CODE_GENERATION, IMAGE_ANALYSIS, FUNCTION_CALLING,
    LONG_CONTEXT, REASONING, CREATIVE_WRITING, DATA_ANALYSIS, MULTILINGUAL
}
```

### Domain Service

```java
// domain/aigateway/service/ModelRoutingService.java
public class ModelRoutingService {

    public AiModel selectBestModel(
            List<AiModel> availableModels,
            RoutingCriteria criteria) {

        return availableModels.stream()
            .filter(AiModel::isActive)
            .filter(m -> m.getCapabilities().containsAll(criteria.requiredCapabilities()))
            .filter(m -> criteria.maxContextNeeded() == null
                      || m.getMaxContextTokens() >= criteria.maxContextNeeded())
            .filter(m -> criteria.preferredProvider() == null
                      || m.getProvider().getName().equals(criteria.preferredProvider()))
            .max(Comparator.comparingDouble(m -> m.computeRoutingScore(criteria)))
            .orElseThrow(() -> new NoSuitableModelException(criteria));
    }
}
```

### REST Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/v1/ai/models` | List available models | User |
| GET | `/api/v1/ai/models/{id}` | Get model details | User |
| POST | `/api/v1/ai/completions` | Request completion (streaming SSE) | User |
| POST | `/api/v1/ai/completions/auto` | Auto-routed completion | User |
| GET | `/api/v1/ai/models/{id}/status` | Model health status | Admin |
| GET | `/api/v1/ai/usage` | Get AI usage stats | User |

---

## 2.6 PARTNERS Module

### Domain Entities

```java
// domain/partners/entity/Partner.java
public class Partner extends BaseEntity {
    private PartnerId id;
    private UserId userId;
    private String companyName;
    private String documentNumber;  // CPF/CNPJ
    private PartnerTier tier;
    private CommissionRate baseCommissionRate;
    private AffiliateLink affiliateLink;
    private boolean approved;
    private boolean active;
    private Credits totalEarnings;
    private Credits pendingPayout;

    public Commission calculateCommission(Credits purchaseAmount) {
        var effectiveRate = tier.getCommissionBonus()
            .map(bonus -> baseCommissionRate.add(bonus))
            .orElse(baseCommissionRate);

        var commissionAmount = purchaseAmount.multiply(effectiveRate.asDecimal());
        return new Commission(this.id, commissionAmount, effectiveRate);
    }

    public void requestPayout() {
        if (pendingPayout.isLessThan(new Credits(1000))) { // minimum payout
            throw new MinimumPayoutNotReachedException(pendingPayout);
        }
        registerEvent(new PayoutRequestedEvent(this.id, this.pendingPayout));
    }
}

// domain/partners/entity/Coupon.java
public class Coupon extends BaseEntity {
    private UUID id;
    private PartnerId partnerId;
    private CouponCode code;
    private DiscountType discountType;   // PERCENTAGE, FIXED_CREDITS
    private int discountValue;
    private int maxRedemptions;
    private int currentRedemptions;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private boolean active;

    public boolean isValid() {
        var now = LocalDateTime.now();
        return active
            && now.isAfter(validFrom)
            && now.isBefore(validUntil)
            && currentRedemptions < maxRedemptions;
    }

    public Credits applyDiscount(Credits originalAmount) {
        if (!isValid()) throw new CouponExpiredException(code);
        return switch (discountType) {
            case PERCENTAGE -> originalAmount.multiply(1.0 - discountValue / 100.0);
            case FIXED_CREDITS -> originalAmount.subtract(new Credits(discountValue));
        };
    }

    public void redeem() {
        if (!isValid()) throw new CouponExpiredException(code);
        this.currentRedemptions++;
        registerEvent(new CouponRedeemedEvent(this.id, this.partnerId));
    }
}
```

### REST Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/v1/partners/register` | Register as partner | User |
| GET | `/api/v1/partners/me` | Get partner dashboard | Partner |
| GET | `/api/v1/partners/me/commissions` | List commissions | Partner |
| POST | `/api/v1/partners/me/payouts` | Request payout | Partner |
| GET | `/api/v1/partners/me/payouts` | List payouts | Partner |
| POST | `/api/v1/partners/coupons` | Create coupon | Partner |
| GET | `/api/v1/partners/coupons` | List partner coupons | Partner |
| PUT | `/api/v1/partners/coupons/{id}` | Update coupon | Partner |
| DELETE | `/api/v1/partners/coupons/{id}` | Deactivate coupon | Partner |
| POST | `/api/v1/coupons/redeem` | Redeem coupon | User |
| GET | `/api/v1/coupons/validate/{code}` | Validate coupon | User |
| GET | `/api/v1/admin/partners` | List all partners | Admin |
| PUT | `/api/v1/admin/partners/{id}/approve` | Approve partner | Admin |

---

## 2.7 CONTENT Module

### Domain Entities

```java
// domain/content/entity/Template.java
public class Template extends BaseEntity {
    private TemplateId id;
    private String title;
    private String description;
    private String promptContent;
    private TemplateType type;
    private Category category;
    private Set<Tag> tags;
    private Slug slug;
    private UserId createdBy;
    private boolean published;
    private int usageCount;
    private double averageRating;
    private UUID tenantId;

    public void publish() {
        if (promptContent == null || promptContent.isBlank()) {
            throw new TemplateContentRequiredException(id);
        }
        this.published = true;
        registerEvent(new TemplatePublishedEvent(this.id));
    }

    public void incrementUsage() {
        this.usageCount++;
    }
}
```

### REST Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/v1/content/templates` | List/search templates | User |
| GET | `/api/v1/content/templates/{slug}` | Get template by slug | User |
| POST | `/api/v1/content/templates` | Create template | Admin |
| PUT | `/api/v1/content/templates/{id}` | Update template | Admin |
| DELETE | `/api/v1/content/templates/{id}` | Delete template | Admin |
| GET | `/api/v1/content/categories` | List categories | Public |
| POST | `/api/v1/content/favorites` | Add to favorites | User |
| GET | `/api/v1/content/favorites` | List favorites | User |
| DELETE | `/api/v1/content/favorites/{templateId}` | Remove from favorites | User |

---

## 2.8 TEAMS Module

### Domain Entities

```java
// domain/teams/entity/Team.java
public class Team extends BaseEntity {
    private TeamId id;
    private String name;
    private UserId ownerId;
    private List<TeamMember> members;
    private TeamCreditPool creditPool;
    private int maxMembers;
    private UUID tenantId;

    public void addMember(UserId userId, TeamRole role) {
        if (members.size() >= maxMembers) {
            throw new TeamCapacityExceededException(id, maxMembers);
        }
        if (hasMember(userId)) {
            throw new UserAlreadyTeamMemberException(userId, id);
        }
        var member = new TeamMember(this.id, userId, role);
        members.add(member);
        registerEvent(new MemberAddedEvent(this.id, userId));
    }

    public void removeMember(UserId userId) {
        if (userId.equals(ownerId)) {
            throw new CannotRemoveTeamOwnerException(id);
        }
        members.removeIf(m -> m.getUserId().equals(userId));
        registerEvent(new MemberRemovedEvent(this.id, userId));
    }

    public boolean hasMember(UserId userId) {
        return members.stream().anyMatch(m -> m.getUserId().equals(userId));
    }
}

// domain/teams/entity/TeamCreditPool.java
public class TeamCreditPool extends BaseEntity {
    private UUID id;
    private TeamId teamId;
    private Credits totalCredits;
    private Map<UserId, CreditAllocation> allocations;

    public void allocateCredits(UserId memberId, Credits amount) {
        var totalAllocated = allocations.values().stream()
            .map(CreditAllocation::getAllocated)
            .reduce(Credits.ZERO, Credits::add);

        if (totalAllocated.add(amount).amount() > totalCredits.amount()) {
            throw new InsufficientPoolCreditsException(totalCredits, totalAllocated, amount);
        }
        allocations.put(memberId, new CreditAllocation(memberId, amount, Credits.ZERO));
    }
}
```

### REST Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/api/v1/teams` | Create team | User |
| GET | `/api/v1/teams` | List user's teams | User |
| GET | `/api/v1/teams/{id}` | Get team details | Team Member |
| PUT | `/api/v1/teams/{id}` | Update team | Team Owner/Admin |
| DELETE | `/api/v1/teams/{id}` | Delete team | Team Owner |
| POST | `/api/v1/teams/{id}/members` | Invite member | Team Owner/Admin |
| DELETE | `/api/v1/teams/{id}/members/{userId}` | Remove member | Team Owner/Admin |
| POST | `/api/v1/teams/{id}/invitations/{invId}/accept` | Accept invitation | Invitee |
| POST | `/api/v1/teams/{id}/credits/allocate` | Allocate credits | Team Owner/Admin |
| GET | `/api/v1/teams/{id}/credits` | Get pool status | Team Member |
| GET | `/api/v1/teams/{id}/usage` | Team usage report | Team Owner/Admin |

---

## 2.9 ANALYTICS Module

### Domain Entities

```java
// domain/analytics/entity/UsageMetric.java
public class UsageMetric extends BaseEntity {
    private UUID id;
    private UserId userId;
    private UUID teamId;
    private MetricType type;        // API_CALL, TOKENS_USED, CREDITS_SPENT
    private String modelId;
    private long value;
    private LocalDateTime timestamp;
    private Map<String, String> dimensions;  // additional grouping dimensions
    private UUID tenantId;
}

// domain/analytics/entity/AuditEntry.java
public class AuditEntry extends BaseEntity {
    private UUID id;
    private UserId actorId;
    private String action;
    private String resourceType;
    private String resourceId;
    private String oldValue;       // JSON snapshot
    private String newValue;       // JSON snapshot
    private String ipAddress;
    private String userAgent;
    private LocalDateTime timestamp;
    private UUID tenantId;
}
```

### REST Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | `/api/v1/analytics/dashboard` | Usage dashboard data | User |
| GET | `/api/v1/analytics/usage` | Detailed usage metrics | User |
| GET | `/api/v1/analytics/costs` | Cost analysis report | User |
| GET | `/api/v1/analytics/models` | Model comparison stats | User |
| POST | `/api/v1/analytics/reports/generate` | Generate custom report | User |
| GET | `/api/v1/analytics/reports/{id}` | Get generated report | User |
| GET | `/api/v1/analytics/reports/{id}/download` | Download report (CSV/PDF) | User |
| GET | `/api/v1/admin/analytics/platform` | Platform-wide analytics | Admin |
| GET | `/api/v1/admin/audit` | Query audit log | Admin |
| GET | `/api/v1/admin/audit/export` | Export audit log | Admin |
