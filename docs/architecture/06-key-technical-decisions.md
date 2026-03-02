# Section 6: Key Technical Decisions

---

## 6.1 WebSocket/SSE Handling for Chat Streaming

### Decision: Server-Sent Events (SSE) as Primary, WebSocket as Upgrade Path

**Rationale**: SSE is simpler, works over standard HTTP, is natively supported by browsers, and fits the unidirectional streaming model of AI completions. WebSocket is reserved for real-time bidirectional features (e.g., collaborative editing, presence).

### SSE Implementation

```java
// presentation/chat/controller/ChatController.java
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final SendMessageUseCase sendMessageUseCase;
    private final StreamResponseUseCase streamResponseUseCase;

    /**
     * Send a message and receive streaming response via SSE.
     * Content-Type: text/event-stream
     */
    @PostMapping(value = "/conversations/{id}/messages",
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<StreamChunkResponse>> sendMessage(
            @PathVariable UUID id,
            @RequestBody @Valid SendMessageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var command = new SendMessageCommand(
            new ConversationId(id),
            new UserId(principal.getUserId()),
            new MessageContent(request.content()),
            request.modelId(),
            request.routingStrategy()
        );

        return streamResponseUseCase.execute(command)
            .map(chunk -> ServerSentEvent.<StreamChunkResponse>builder()
                .id(UUID.randomUUID().toString())
                .event(chunk.isComplete() ? "complete" : "chunk")
                .data(new StreamChunkResponse(
                    chunk.content(),
                    chunk.isComplete(),
                    chunk.tokenUsage()
                ))
                .build())
            .concatWith(Flux.just(
                ServerSentEvent.<StreamChunkResponse>builder()
                    .event("done")
                    .data(null)
                    .build()
            ))
            .doOnCancel(() -> {
                // User closed connection, clean up resources
                log.info("SSE stream cancelled for conversation {}", id);
            })
            .timeout(Duration.ofMinutes(5));
    }
}

// application/chat/usecase/StreamResponseUseCaseImpl.java
@Service
public class StreamResponseUseCaseImpl implements StreamResponseUseCase {

    private final ConversationRepository conversationRepo;
    private final AiProviderClientFactory clientFactory;
    private final ModelRoutingService routingService;
    private final ConsumeCreditsUseCase consumeCreditsUseCase;
    private final SemanticCachePort semanticCache;

    @Override
    public Flux<StreamChunk> execute(SendMessageCommand command) {
        return Mono.fromCallable(() -> {
                // 1. Load conversation and prepare context
                var conversation = conversationRepo.findById(command.conversationId())
                    .orElseThrow();

                // 2. Check semantic cache
                var cacheKey = semanticCache.generateKey(command.content(), command.modelId());
                var cached = semanticCache.lookup(cacheKey);
                if (cached.isPresent()) {
                    return Flux.just(new StreamChunk(
                        conversation.getId().toString(),
                        cached.get().content(), true, cached.get().tokenUsage()
                    ));
                }

                // 3. Route to model
                var model = routingService.selectModel(command);
                var client = clientFactory.getClient(model.getProvider().getName());

                // 4. Build prompt with conversation context
                var promptRequest = buildPromptRequest(conversation, command, model);

                // 5. Stream from AI provider
                return ((StreamingAiProviderClient) client)
                    .streamComplete(promptRequest);
            })
            .flatMapMany(Function.identity())
            .doOnComplete(() -> {
                // After streaming completes:
                // - Save assistant message
                // - Consume credits
                // - Cache result
                // - Record analytics
            });
    }
}
```

### WebSocket for Future Real-Time Features

```java
// infrastructure/config/WebSocketConfig.java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            .addHandler(chatWebSocketHandler(), "/ws/chat")
            .setAllowedOriginPatterns("*")
            .addInterceptors(new JwtWebSocketInterceptor(tokenProvider));
    }
}
```

---

## 6.2 Credit System Calculation Engine

### Architecture

The credit system uses a **decorator chain** for cost calculation and **pessimistic locking** for balance updates to prevent race conditions.

```java
// domain/billing/service/CreditCalculationService.java
public class CreditCalculationService {

    // Base cost table (loaded from database, cached in Redis)
    // Model ID -> CreditCost mapping
    private final Map<String, CreditCost> costTable;

    /**
     * Calculate the credit cost for an AI completion request.
     *
     * Cost formula:
     *   baseCost = (inputTokens * inputRate / 1000) + (outputTokens * outputRate / 1000) + fixedRequestCost
     *   planDiscount = baseCost * planTier.discountRate
     *   volumeDiscount = (baseCost - planDiscount) * volumeTierRate
     *   couponDiscount = applied last on remaining amount
     *   finalCost = max(1, baseCost - planDiscount - volumeDiscount - couponDiscount)
     *
     * Minimum cost is always 1 credit to prevent free API usage.
     */
    public CreditCalculationResult calculate(CreditCalculationInput input) {
        var creditCost = costTable.get(input.modelId());
        if (creditCost == null) {
            throw new ModelCostNotConfiguredException(input.modelId());
        }

        // Step 1: Base cost
        long baseCost = creditCost.calculateTotal(
            input.inputTokens(), input.outputTokens()).amount();

        // Step 2: Plan discount
        double planDiscount = input.planTier().getCreditDiscount();
        long afterPlan = Math.round(baseCost * (1.0 - planDiscount));

        // Step 3: Volume discount (based on current month usage)
        double volumeDiscount = calculateVolumeDiscount(input.monthlyUsage());
        long afterVolume = Math.round(afterPlan * (1.0 - volumeDiscount));

        // Step 4: Coupon discount (if applicable)
        long afterCoupon = afterVolume;
        if (input.coupon() != null && input.coupon().isValid()) {
            afterCoupon = input.coupon().applyDiscount(new Credits(afterVolume)).amount();
        }

        // Step 5: Minimum floor
        long finalCost = Math.max(1, afterCoupon);

        return new CreditCalculationResult(
            new Credits(finalCost),
            new Credits(baseCost),
            planDiscount, volumeDiscount,
            baseCost - finalCost  // total savings
        );
    }

    private double calculateVolumeDiscount(Credits monthlyUsage) {
        long usage = monthlyUsage.amount();
        if (usage > 100_000) return 0.15;
        if (usage > 50_000) return 0.10;
        if (usage > 10_000) return 0.05;
        return 0.0;
    }
}

// Atomic credit consumption with pessimistic lock
// infrastructure/billing/persistence/CreditAccountRepositoryImpl.java
@Repository
public class CreditAccountRepositoryImpl implements CreditAccountRepository {

    @Override
    @Transactional
    public CreditAccount consumeCreditsAtomically(UserId userId, Credits amount, String reason) {
        // Pessimistic lock prevents concurrent consumption
        var jpaEntity = jpaRepo.findByUserIdWithLock(userId.value())
            .orElseThrow(() -> new CreditAccountNotFoundException(userId));

        var account = mapper.toDomain(jpaEntity);
        account.consume(amount, reason);

        var saved = jpaRepo.save(mapper.toJpa(account));
        return mapper.toDomain(saved);
    }
}
```

### Credit Cost Table (Database Schema)

```sql
CREATE TABLE model_credit_costs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_id VARCHAR(100) NOT NULL UNIQUE,
    input_token_credits BIGINT NOT NULL,    -- credits per 1K input tokens
    output_token_credits BIGINT NOT NULL,   -- credits per 1K output tokens
    base_request_credits BIGINT NOT NULL DEFAULT 0,  -- fixed cost per request
    effective_from TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    effective_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Example data:
-- GPT-4o:      input=3, output=12, base=0
-- Claude 3.5:  input=3, output=15, base=0
-- GPT-3.5:     input=1, output=2,  base=0
-- Llama 3.1:   input=1, output=1,  base=0
```

---

## 6.3 AI Model Routing Algorithm (Auto-Select)

### Algorithm Design

```java
// domain/aigateway/service/ModelRoutingService.java
public class ModelRoutingService {

    /**
     * Auto-select algorithm:
     *
     * 1. FILTER phase: Remove models that don't meet hard constraints
     *    - Active status
     *    - Required capabilities (e.g., vision, function calling)
     *    - Minimum context window size
     *    - Provider health status
     *
     * 2. SCORE phase: Compute weighted score for remaining models
     *    score = (qualityWeight * qualityScore)
     *          + (speedWeight * latencyScore)
     *          + (costWeight * costEfficiency)
     *
     * 3. SELECT phase: Choose the highest-scoring model
     *    - If top models are within 5% of each other, prefer the one
     *      with best health metrics (lowest error rate)
     *
     * 4. FALLBACK phase: If no model matches, use default fallback chain
     */
    public AiModel autoSelect(List<AiModel> allModels, RoutingCriteria criteria,
                               Map<String, ProviderHealth> healthMap) {

        // Phase 1: Filter
        var candidates = allModels.stream()
            .filter(AiModel::isActive)
            .filter(m -> m.getCapabilities().containsAll(criteria.requiredCapabilities()))
            .filter(m -> criteria.maxContextNeeded() == null
                      || m.getMaxContextTokens() >= criteria.maxContextNeeded())
            .filter(m -> {
                var health = healthMap.get(m.getProvider().getName());
                return health != null && health.isHealthy();
            })
            .toList();

        if (candidates.isEmpty()) {
            throw new NoSuitableModelException(criteria);
        }

        // Phase 2: Score
        var scored = candidates.stream()
            .map(model -> new ScoredModel(model, model.computeRoutingScore(criteria)))
            .sorted(Comparator.comparingDouble(ScoredModel::score).reversed())
            .toList();

        // Phase 3: Select with tiebreaking
        var topScore = scored.getFirst().score();
        var topTier = scored.stream()
            .filter(s -> s.score() >= topScore * 0.95)  // within 5%
            .toList();

        if (topTier.size() > 1) {
            // Tiebreak by health metrics
            return topTier.stream()
                .min(Comparator.comparingDouble(s -> {
                    var health = healthMap.get(s.model().getProvider().getName());
                    return health.getErrorRate();
                }))
                .map(ScoredModel::model)
                .orElse(topTier.getFirst().model());
        }

        return scored.getFirst().model();
    }

    private record ScoredModel(AiModel model, double score) {}
}
```

---

## 6.4 Caching Strategy (Redis)

### Multi-Layer Caching Architecture

```java
// infrastructure/config/RedisConfig.java
@Configuration
public class RedisConfig {

    @Bean
    public RedissonClient redissonClient() {
        var config = new Config();
        config.useSingleServer()
            .setAddress("redis://localhost:6379")
            .setDatabase(0)
            .setConnectionPoolSize(64)
            .setConnectionMinimumIdleSize(24);
        return Redisson.create(config);
    }

    @Bean
    public CacheManager cacheManager(RedissonClient redisson) {
        var configMap = new HashMap<String, CacheConfig>();

        // Layer 1: Hot data - short TTL, high frequency
        configMap.put("models", new CacheConfig(300_000, 180_000));     // 5 min TTL
        configMap.put("plans", new CacheConfig(3600_000, 1800_000));    // 1 hour TTL
        configMap.put("credit-balances", new CacheConfig(60_000, 30_000)); // 1 min TTL

        // Layer 2: Warm data - medium TTL
        configMap.put("templates", new CacheConfig(1800_000, 900_000));  // 30 min TTL
        configMap.put("categories", new CacheConfig(3600_000, 1800_000)); // 1 hour TTL
        configMap.put("user-profiles", new CacheConfig(600_000, 300_000)); // 10 min TTL

        // Layer 3: Semantic cache - long TTL
        configMap.put("semantic-cache", new CacheConfig(86400_000, 43200_000)); // 24 hour TTL

        return new RedissonSpringCacheManager(redisson, configMap);
    }
}

// Semantic Cache Implementation
// infrastructure/aigateway/adapter/SemanticCacheAdapter.java
@Component
public class SemanticCacheAdapter implements SemanticCachePort {

    private final RedissonClient redisson;
    private final RMapCache<String, String> cache;

    public SemanticCacheAdapter(RedissonClient redisson) {
        this.redisson = redisson;
        this.cache = redisson.getMapCache("ai:semantic-cache");
    }

    /**
     * Generate a cache key from the prompt.
     * Uses a hash of: modelId + normalized prompt content + key parameters.
     * Only caches deterministic requests (temperature=0).
     */
    @Override
    public String generateKey(String promptContent, String modelId,
                               double temperature, int maxTokens) {
        if (temperature > 0.0) return null; // Don't cache non-deterministic requests

        var normalized = promptContent.toLowerCase().strip()
            .replaceAll("\\s+", " ");
        var keyInput = modelId + "|" + normalized + "|" + maxTokens;
        return DigestUtils.sha256Hex(keyInput);
    }

    @Override
    public Optional<CachedCompletion> lookup(String cacheKey) {
        if (cacheKey == null) return Optional.empty();
        var cached = cache.get(cacheKey);
        if (cached != null) {
            return Optional.of(JsonUtils.parse(cached, CachedCompletion.class));
        }
        return Optional.empty();
    }

    @Override
    public void store(String cacheKey, CachedCompletion completion, Duration ttl) {
        if (cacheKey == null) return;
        cache.put(cacheKey, JsonUtils.toJson(completion),
                  ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void invalidateByModel(String modelId) {
        // Scan and remove entries for a specific model
        // Used when model pricing or behavior changes
        cache.entrySet().removeIf(entry -> entry.getKey().startsWith(modelId + "|"));
    }
}
```

---

## 6.5 Rate Limiting Implementation

```java
// presentation/shared/filter/RateLimitFilter.java
@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RedissonClient redisson;
    private final RateLimitConfigProperties config;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {

        var userId = extractUserId(request);
        var endpoint = categorizeEndpoint(request);

        // Get rate limit configuration for this user's plan and endpoint
        var limit = config.getLimitFor(userId, endpoint);

        // Use Redis sliding window rate limiter
        var rateLimiter = redisson.getRateLimiter("rate:" + userId + ":" + endpoint);

        // Set rate if not already configured
        rateLimiter.trySetRate(
            RateType.PER_CLIENT,
            limit.permits(),
            limit.period(),
            limit.periodUnit()
        );

        if (rateLimiter.tryAcquire(1)) {
            // Add rate limit headers
            long remaining = rateLimiter.availablePermits();
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit.permits()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            response.setHeader("X-RateLimit-Reset",
                String.valueOf(Instant.now().plusSeconds(limit.period()).getEpochSecond()));

            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(limit.period()));
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit.permits()));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.getWriter().write(JsonUtils.toJson(
                new ApiErrorResponse("RATE_LIMIT_EXCEEDED",
                    "Too many requests. Please retry after " + limit.period() + " seconds.")
            ));
        }
    }

    private RateLimitEndpoint categorizeEndpoint(HttpServletRequest request) {
        var path = request.getRequestURI();
        if (path.contains("/ai/completions")) return RateLimitEndpoint.AI_COMPLETION;
        if (path.contains("/auth/login")) return RateLimitEndpoint.AUTH;
        if (path.contains("/billing")) return RateLimitEndpoint.BILLING;
        return RateLimitEndpoint.GENERAL;
    }
}

// Rate limit tiers by plan:
// FREE:  10 req/min AI, 100 req/min general
// BASIC: 30 req/min AI, 300 req/min general
// PRO:   60 req/min AI, 600 req/min general
// ENTERPRISE: 120 req/min AI, 1200 req/min general
```

---

## 6.6 LGPD Compliance

```java
// application/auth/usecase/DataErasureUseCaseImpl.java
@Service
@Transactional
public class DataErasureUseCaseImpl implements DataErasureUseCase {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepo;
    private final CreditAccountRepository creditRepo;
    private final AuditEntryRepository auditRepo;

    /**
     * LGPD Article 18: Right to deletion of personal data.
     * Anonymizes user data while preserving aggregate analytics.
     */
    @Override
    public DataErasureResult execute(UserId userId) {
        // 1. Anonymize user profile
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        user.anonymize();  // Sets email="deleted_<hash>@anon.com", name="Deleted User"
        userRepository.save(user);

        // 2. Delete conversation content (preserve metadata for analytics)
        conversationRepo.anonymizeUserConversations(userId);

        // 3. Anonymize billing records (keep for tax compliance, 5 years)
        creditRepo.anonymizeTransactionHistory(userId);

        // 4. Record the erasure in audit log
        auditRepo.save(new AuditEntry(
            userId, "DATA_ERASURE", "User", userId.toString(),
            null, null, LocalDateTime.now()
        ));

        // 5. Invalidate all sessions/tokens
        tokenProvider.invalidateAllUserTokens(userId);

        // 6. Clear caches
        cacheManager.evict("user-profiles", userId.toString());

        return new DataErasureResult(userId, LocalDateTime.now(), "COMPLETED");
    }
}

// Consent management
// domain/auth/entity/UserConsent.java
public class UserConsent extends BaseEntity {
    private UUID id;
    private UserId userId;
    private ConsentType type;     // DATA_PROCESSING, MARKETING, ANALYTICS, COOKIES
    private boolean granted;
    private LocalDateTime grantedAt;
    private LocalDateTime revokedAt;
    private String ipAddress;
    private String version;        // consent policy version

    public void revoke() {
        this.granted = false;
        this.revokedAt = LocalDateTime.now();
    }
}

// Multi-tenancy: Row-Level Security filter
// infrastructure/multitenancy/TenantAwareInterceptor.java
@Component
public class TenantAwareInterceptor implements HibernateInterceptor {

    @Override
    public void onPrepareStatement(String sql, PreparedStatement statement) {
        var tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            // Automatically append tenant filter to all queries
            // This is handled via Hibernate @Filter annotation
        }
    }
}
```

### Row-Level Security (Database Level)

```sql
-- Enable RLS on sensitive tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE credit_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE credit_transactions ENABLE ROW LEVEL SECURITY;

-- Policy: users can only see their own data
CREATE POLICY user_isolation ON conversations
    USING (tenant_id = current_setting('app.current_tenant')::uuid);

-- Application sets tenant context before each request
-- SET LOCAL app.current_tenant = '<tenant-uuid>';
```

---

## 6.7 Multi-Tenancy with Row-Level Security

```java
// infrastructure/multitenancy/TenantContext.java
public class TenantContext {
    private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();

    public static void setCurrentTenant(UUID tenantId) {
        currentTenant.set(tenantId);
    }

    public static UUID getCurrentTenant() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}

// infrastructure/multitenancy/TenantFilter.java
@Component
@Order(0)
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        try {
            // Extract tenant from JWT claims or header
            var tenantId = extractTenantId(request);
            if (tenantId != null) {
                TenantContext.setCurrentTenant(tenantId);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}

// Hibernate filter to enforce tenant isolation at JPA level
// Applied to all tenant-aware entities

// infrastructure/config/JpaConfig.java
@Configuration
public class JpaConfig {

    @Bean
    public HibernatePropertiesCustomizer tenantFilterCustomizer() {
        return properties -> {
            properties.put("hibernate.session_factory.interceptor", new TenantInterceptor());
        };
    }
}

// On tenant-aware JPA entities:
@Entity
@Table(name = "conversations")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class ConversationJpaEntity {
    // ...
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
}
```

---

## 6.8 Authentication Flow (JWT + Refresh Tokens + OAuth2)

### Token Architecture

```
Authentication Flow:

  Client                       Server                     Redis
    |                            |                          |
    |-- POST /auth/login ------->|                          |
    |   {email, password}        |                          |
    |                            |-- validate credentials   |
    |                            |-- generate access token  |
    |                            |   (15 min, stateless)    |
    |                            |-- generate refresh token |
    |                            |   (7 days)               |
    |                            |-- store refresh token -->|
    |<--- {accessToken,          |                          |
    |      refreshToken,         |                          |
    |      expiresIn} -----------|                          |
    |                            |                          |
    |-- GET /api/... ----------->|                          |
    |   Authorization: Bearer AT |-- validate JWT locally   |
    |                            |   (no DB/Redis call)     |
    |<--- response --------------|                          |
    |                            |                          |
    |-- POST /auth/refresh ----->|                          |
    |   {refreshToken}           |-- validate RT ---------->|
    |                            |<-- RT valid, get user ---|
    |                            |-- rotate: invalidate     |
    |                            |   old RT, create new ---->|
    |                            |-- generate new AT + RT   |
    |<--- {newAccessToken,       |                          |
    |      newRefreshToken} -----|                          |
```

```java
// infrastructure/auth/security/JwtTokenProvider.java
@Component
public class JwtTokenProvider implements TokenProvider {

    private final SecretKey accessKey;
    private final SecretKey refreshKey;

    @Value("${jwt.access-token-expiration:900000}")  // 15 minutes
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}")  // 7 days
    private long refreshTokenExpiration;

    private final RedissonClient redisson;

    @Override
    public TokenPair generateTokens(User user) {
        var now = Instant.now();

        var accessToken = Jwts.builder()
            .subject(user.getId().value().toString())
            .claim("email", user.getEmail())
            .claim("roles", user.getRoleNames())
            .claim("tenantId", user.getTenantId().toString())
            .claim("planTier", user.getPlanTier().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(accessTokenExpiration)))
            .signWith(accessKey)
            .compact();

        var refreshTokenId = UUID.randomUUID().toString();
        var refreshToken = Jwts.builder()
            .id(refreshTokenId)
            .subject(user.getId().value().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(refreshTokenExpiration)))
            .signWith(refreshKey)
            .compact();

        // Store refresh token in Redis with TTL
        var bucket = redisson.getBucket("refresh:" + refreshTokenId);
        bucket.set(user.getId().value().toString(),
                   refreshTokenExpiration, TimeUnit.MILLISECONDS);

        return new TokenPair(accessToken, refreshToken, accessTokenExpiration / 1000);
    }

    @Override
    public UserId validateAccessToken(String token) {
        var claims = Jwts.parser()
            .verifyWith(accessKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        return new UserId(UUID.fromString(claims.getSubject()));
    }

    @Override
    public TokenPair refreshTokens(String refreshToken) {
        var claims = Jwts.parser()
            .verifyWith(refreshKey)
            .build()
            .parseSignedClaims(refreshToken)
            .getPayload();

        var tokenId = claims.getId();
        var bucket = redisson.getBucket("refresh:" + tokenId);

        if (!bucket.isExists()) {
            throw new InvalidRefreshTokenException("Token has been revoked");
        }

        // Rotate: delete old refresh token
        bucket.delete();

        // Load user and generate new pair
        var userId = new UserId(UUID.fromString(claims.getSubject()));
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        return generateTokens(user);
    }

    @Override
    public void invalidateRefreshToken(String refreshToken) {
        var claims = Jwts.parser()
            .verifyWith(refreshKey)
            .build()
            .parseSignedClaims(refreshToken)
            .getPayload();

        redisson.getBucket("refresh:" + claims.getId()).delete();
    }

    public void invalidateAllUserTokens(UserId userId) {
        // Scan Redis for all refresh tokens belonging to user
        var keys = redisson.getKeys().getKeysByPattern("refresh:*");
        keys.forEach(key -> {
            var bucket = redisson.getBucket(key);
            if (userId.value().toString().equals(bucket.get())) {
                bucket.delete();
            }
        });
    }
}

// OAuth2 Flow
// infrastructure/auth/adapter/GoogleOAuthAdapter.java
@Component
public class GoogleOAuthAdapter implements OAuthClient {

    private final WebClient webClient;

    @Override
    public OAuthUserInfo exchangeCodeForUser(String authorizationCode, String redirectUri) {
        // 1. Exchange code for tokens
        var tokenResponse = webClient.post()
            .uri("https://oauth2.googleapis.com/token")
            .bodyValue(Map.of(
                "code", authorizationCode,
                "client_id", clientId,
                "client_secret", clientSecret,
                "redirect_uri", redirectUri,
                "grant_type", "authorization_code"
            ))
            .retrieve()
            .bodyToMono(GoogleTokenResponse.class)
            .block();

        // 2. Get user info
        var userInfo = webClient.get()
            .uri("https://www.googleapis.com/oauth2/v2/userinfo")
            .header("Authorization", "Bearer " + tokenResponse.accessToken())
            .retrieve()
            .bodyToMono(GoogleUserInfo.class)
            .block();

        return new OAuthUserInfo(
            OAuthProvider.GOOGLE,
            userInfo.id(),
            userInfo.email(),
            userInfo.name(),
            userInfo.picture()
        );
    }
}
```
