# Section 7: API Design

---

## 7.1 REST API Conventions

### URL Naming

```
Base URL: https://api.ia-aggregator.com/api/v1

Rules:
- Use lowercase with hyphens for multi-word resources: /credit-transactions
- Use plural nouns for collections: /conversations, /teams
- Use path params for resource identity: /conversations/{id}
- Use query params for filtering, sorting, pagination
- Nest sub-resources max 2 levels deep: /teams/{id}/members
- Use verbs ONLY for actions that don't map to CRUD: /coupons/redeem
```

### HTTP Methods

| Method | Usage | Idempotent | Example |
|--------|-------|------------|---------|
| GET | Retrieve resource(s) | Yes | `GET /api/v1/conversations` |
| POST | Create resource or action | No | `POST /api/v1/conversations` |
| PUT | Full resource update | Yes | `PUT /api/v1/teams/{id}` |
| PATCH | Partial resource update | Yes | `PATCH /api/v1/auth/me` |
| DELETE | Remove resource | Yes | `DELETE /api/v1/conversations/{id}` |

### Versioning Strategy

API versioning is done via URL path (`/api/v1/`). When breaking changes are needed:

```
/api/v1/conversations  -- current stable
/api/v2/conversations  -- new version (coexists during migration)

# Version sunset header:
Sunset: Sat, 01 Mar 2027 00:00:00 GMT
Deprecation: true
Link: </api/v2/conversations>; rel="successor-version"
```

### Pagination

```json
// Request:
// GET /api/v1/conversations?page=0&size=20&sort=createdAt,desc

// Response envelope:
{
  "success": true,
  "data": {
    "content": [ /* array of items */ ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 156,
      "totalPages": 8
    },
    "sort": {
      "field": "createdAt",
      "direction": "DESC"
    }
  },
  "timestamp": "2026-03-01T10:30:00Z"
}
```

### Filtering

```
GET /api/v1/billing/credits/transactions
    ?type=CONSUMPTION
    &dateFrom=2026-01-01
    &dateTo=2026-03-01
    &modelId=anthropic/claude-3.5-sonnet
    &minAmount=100
    &sort=createdAt,desc
    &page=0
    &size=50
```

### Standard Response Format

```java
// presentation/shared/response/ApiResponse.java
public record ApiResponse<T>(
    boolean success,
    T data,
    ApiMeta meta,
    String timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now().toString());
    }

    public static <T> ApiResponse<T> ok(T data, ApiMeta meta) {
        return new ApiResponse<>(true, data, meta, Instant.now().toString());
    }
}

// presentation/shared/response/ApiErrorResponse.java
public record ApiErrorResponse(
    boolean success,
    ApiError error,
    String timestamp,
    String requestId
) {
    public record ApiError(
        String code,
        String message,
        String detail,
        List<FieldError> fieldErrors
    ) {}

    public record FieldError(
        String field,
        String message,
        Object rejectedValue
    ) {}
}
```

### Successful Response Examples

```json
// Single resource:
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "name": "John Doe",
    "planTier": "PRO",
    "createdAt": "2026-01-15T10:30:00Z"
  },
  "timestamp": "2026-03-01T10:30:00Z"
}

// Collection with pagination:
{
  "success": true,
  "data": {
    "content": [
      { "id": "...", "title": "Conversation 1", "modelId": "gpt-4o" },
      { "id": "...", "title": "Conversation 2", "modelId": "claude-3.5-sonnet" }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 42,
      "totalPages": 3
    }
  },
  "timestamp": "2026-03-01T10:30:00Z"
}
```

### Error Response Examples

```json
// Validation error (400):
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Request validation failed",
    "detail": "One or more fields have invalid values",
    "fieldErrors": [
      {
        "field": "email",
        "message": "must be a valid email address",
        "rejectedValue": "not-an-email"
      },
      {
        "field": "password",
        "message": "must be at least 8 characters",
        "rejectedValue": null
      }
    ]
  },
  "timestamp": "2026-03-01T10:30:00Z",
  "requestId": "req-abc-123"
}

// Business error (409):
{
  "success": false,
  "error": {
    "code": "INSUFFICIENT_CREDITS",
    "message": "Insufficient credits to complete this request",
    "detail": "Required: 150 credits, Available: 42 credits",
    "fieldErrors": null
  },
  "timestamp": "2026-03-01T10:30:00Z",
  "requestId": "req-abc-456"
}

// Authentication error (401):
{
  "success": false,
  "error": {
    "code": "AUTHENTICATION_REQUIRED",
    "message": "Access token is expired or invalid",
    "detail": null,
    "fieldErrors": null
  },
  "timestamp": "2026-03-01T10:30:00Z",
  "requestId": "req-abc-789"
}

// Rate limit error (429):
{
  "success": false,
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Too many requests",
    "detail": "Limit: 60 requests per minute. Retry after 23 seconds.",
    "fieldErrors": null
  },
  "timestamp": "2026-03-01T10:30:00Z",
  "requestId": "req-abc-321"
}
```

---

## 7.2 OpenAPI 3.1 Structure

```java
// infrastructure/config/OpenApiConfig.java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("IA Aggregator API")
                .description("Multi-model AI aggregator platform API")
                .version("1.0.0")
                .contact(new Contact()
                    .name("IA Aggregator Team")
                    .email("api@ia-aggregator.com"))
                .license(new License()
                    .name("Proprietary")))
            .externalDocs(new ExternalDocumentation()
                .description("Full Documentation")
                .url("https://docs.ia-aggregator.com"))
            .addSecurityItem(new SecurityRequirement()
                .addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT access token"))
                .addSchemas("ApiResponse", apiResponseSchema())
                .addSchemas("ApiError", apiErrorSchema())
                .addSchemas("PageInfo", pageInfoSchema()))
            .addTagsItem(new Tag().name("Auth").description("Authentication and authorization"))
            .addTagsItem(new Tag().name("Chat").description("Conversations and messages"))
            .addTagsItem(new Tag().name("AI Models").description("AI model management and completions"))
            .addTagsItem(new Tag().name("Billing").description("Credits, subscriptions, and payments"))
            .addTagsItem(new Tag().name("Partners").description("Partner management and commissions"))
            .addTagsItem(new Tag().name("Content").description("Templates and prompt library"))
            .addTagsItem(new Tag().name("Teams").description("Team management and credit pools"))
            .addTagsItem(new Tag().name("Analytics").description("Usage analytics and reporting"));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public")
            .pathsToMatch("/api/v1/**")
            .pathsToExclude("/api/v1/admin/**")
            .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("admin")
            .pathsToMatch("/api/v1/admin/**")
            .build();
    }
}

// Controller annotations for OpenAPI documentation
@RestController
@RequestMapping("/api/v1/billing/credits")
@Tag(name = "Billing")
public class CreditController {

    @Operation(
        summary = "Get credit balance",
        description = "Returns the current credit balance for the authenticated user"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            description = "Credit balance retrieved",
            content = @Content(schema = @Schema(implementation = CreditBalanceResponse.class))),
        @ApiResponse(responseCode = "401",
            description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping
    public ApiResponse<CreditBalanceResponse> getBalance(
            @AuthenticationPrincipal UserPrincipal principal) {
        // ...
    }
}
```

---

## 7.3 Authentication/Authorization (Spring Security + JWT)

```java
// infrastructure/config/SecurityConfig.java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final JwtAuthenticationEntryPoint jwtEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/oauth/**",
                    "/api/v1/auth/password/reset"
                ).permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/billing/plans",
                    "/api/v1/content/categories",
                    "/api/v1/coupons/validate/**"
                ).permitAll()

                // Webhook endpoints (verified by signature)
                .requestMatchers(
                    "/api/v1/billing/webhooks/**"
                ).permitAll()

                // OpenAPI docs
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()

                // Actuator (restricted)
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // Admin endpoints
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // Partner endpoints
                .requestMatchers("/api/v1/partners/me/**").hasRole("PARTNER")

                // Team admin endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/teams/*/members").hasAnyRole("TEAM_OWNER", "TEAM_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/teams/*/members/*").hasAnyRole("TEAM_OWNER", "TEAM_ADMIN")

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "https://*.ia-aggregator.com",
            "http://localhost:3000"  // dev only
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(
            "X-Request-Id",
            "X-RateLimit-Limit",
            "X-RateLimit-Remaining",
            "X-RateLimit-Reset"
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

// presentation/shared/filter/JwtAuthenticationFilter.java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        var token = extractToken(request);

        if (token != null) {
            try {
                var userId = tokenProvider.validateAccessToken(token);
                var userDetails = userDetailsService.loadUserByUsername(userId.value().toString());
                var authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException e) {
                response.setHeader("X-Token-Expired", "true");
                // Let the request continue - the security chain will return 401
            } catch (JwtException e) {
                // Invalid token - do not set authentication
                log.debug("Invalid JWT token: {}", e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        var header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/login")
            || path.startsWith("/api/v1/auth/register")
            || path.startsWith("/api/v1/auth/refresh")
            || path.startsWith("/api/v1/auth/oauth")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/api/v1/billing/webhooks");
    }
}
```

---

## 7.4 Rate Limiting Headers

Every API response includes rate limit information:

```
HTTP/1.1 200 OK
X-Request-Id: req-abc-123
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 47
X-RateLimit-Reset: 1709290860

-- On rate limit exceeded:
HTTP/1.1 429 Too Many Requests
Retry-After: 23
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1709290860
```

Rate limits by plan tier:

| Endpoint Category | Free | Basic | Pro | Enterprise |
|---|---|---|---|---|
| AI Completions | 10/min | 30/min | 60/min | 120/min |
| General API | 100/min | 300/min | 600/min | 1200/min |
| Auth (login) | 5/min | 5/min | 5/min | 5/min |
| Webhooks | unlimited | unlimited | unlimited | unlimited |

---

## 7.5 CORS Configuration

Production CORS allows only the official frontend domains:

```yaml
# application-prod.yml
cors:
  allowed-origins:
    - "https://app.ia-aggregator.com"
    - "https://admin.ia-aggregator.com"
  allowed-methods:
    - GET
    - POST
    - PUT
    - PATCH
    - DELETE
    - OPTIONS
  allowed-headers:
    - Authorization
    - Content-Type
    - X-Request-Id
    - Accept
  exposed-headers:
    - X-Request-Id
    - X-RateLimit-Limit
    - X-RateLimit-Remaining
    - X-RateLimit-Reset
    - X-Token-Expired
  allow-credentials: true
  max-age: 3600
```
