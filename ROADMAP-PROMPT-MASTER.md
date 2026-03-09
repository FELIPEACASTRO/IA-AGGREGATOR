# ROADMAP DE IMPLEMENTAÇÃO — PROMPT MESTRE CLAUDE OPUS 4.6
> Análise rigorosa linha a linha do `prompt-mestre-claude-opus-4.6.txt`
> Data: 2026-03-08 | Versão: 1.0.0

---

## 1. SÍNTESE DO PROMPT — O QUE ELE EXIGE

### Missão
Plataforma Java 21 + Maven multi-módulo + Spring Boot 3, integrando **45 providers de IA** com
**13 capabilities**, production-ready: observável, resiliente, testada e documentada por provider.

### 13 Capabilities Obrigatórias
```
CHAT · RESPONSES · EMBEDDINGS · RERANK · IMAGE_GENERATION · IMAGE_EDITING
VIDEO_GENERATION · SPEECH_TO_TEXT · TEXT_TO_SPEECH · OCR
WEB_SEARCH · WEB_GROUNDED_CHAT · THREAT_INTEL_SEARCH
```

### 45 Providers Obrigatórios por Grupo

| Grupo | Qtd | Providers |
|-------|-----|-----------|
| OpenAI-Compatible | 13 | OpenAI, Groq, Cerebras, OpenRouter, DeepSeek, xAI/Grok, Together AI, NVIDIA NIM, Fireworks AI, SambaNova, SiliconFlow, GitHub Models, DeepInfra |
| Native LLM & Gateways | 10 | Gemini, Anthropic, Cohere, Mistral, Cloudflare, HuggingFace, AI21, AWS Bedrock, Perplexity Sonar, Azure OpenAI |
| Media / Voice / Image | 14 | Stability AI, fal.ai, Replicate, Deepgram, AssemblyAI, ElevenLabs, GCloud Vision, GCloud Speech, GCloud NLP, GCloud Translation, GCloud TTS, BFL/FLUX, Runway, Ideogram |
| Search & Threat Intel | 8 | Exa, NewsCatcher, Onion Search, DarkOwl, Twingly, FullHunt, DarknetSearch/Kaduu, Flare |
| **Out of scope** | 1 | Midjourney (sem API pública oficial) |

### Arquitetura Alvo do Prompt (14 módulos Maven)
```
core-domain · core-spi · infra-http · infra-auth · infra-json
infra-observability · infra-resilience
providers-openai-compatible · providers-native-llm
providers-media-voice · providers-search-intel · providers-enterprise-gateways
app-api · app-cli · docs
```

### Stack Tecnológica Exigida
- Java 21, Maven, Spring Boot 3.x
- **Spring WebFlux + WebClient** (reativo — exigência do prompt)
- Resilience4j: CB + Retry + **Bulkhead** + **RateLimiter**
- Micrometer + SLF4J/Logback (structured JSON logs)
- JUnit 5 + **WireMock** + Testcontainers
- AWS SDK v2 para Bedrock; SDKs oficiais quando úteis

---

## 2. GAP ANALYSIS — Estado Atual vs. Prompt

### 2.1 O Que JÁ EXISTE no Projeto

| Item | Qtd/Status | Observação |
|------|-----------|-----------|
| Módulos Maven | 5 de 14 | common, domain, application, infrastructure, presentation |
| Providers implementados | **50** | Todos os 45 do prompt + 5 extras: Novita, JinaAI, Tavily, Gladia, Leonardo.AI |
| Capabilities (enum) | 13/13 OK | Completo e correto |
| Auth strategies | 6 | Faltam: CompositeAuthStrategy, AzureApiKeyAuthStrategy, OptionalAzureEntraAuthStrategy |
| Abstract base classes | 6 OK | OpenAiCompat, NativeLlm, Media, Audio, GoogleCloud, Search |
| Controllers REST | 12 OK | 1 por capability |
| Compliance gate | OK | dark-web-enabled flag |
| CircuitBreaker + Retry | OK | Resilience4j por provider |
| Testes WireMock para AI | **ZERO** | CRITICO — Definition of Done bloqueado |
| app-cli | **ZERO** | Módulo não existe |
| ProviderHttpClientFactory | **ZERO** | Cada provider cria seu próprio HttpClient (50 instâncias) |
| Spring WebFlux | **ZERO** | Stack atual: Spring MVC sincrono |
| Bulkhead + RateLimiter | Parcial | CircuitBreaker existe; Bulkhead/RL nao configurados |

### 2.2 Gaps por Prioridade

| Gap | Criticidade | Impacto |
|-----|-------------|---------|
| Zero testes WireMock para providers | CRITICO | Definition of Done bloqueado |
| Spring WebFlux vs. MVC atual | CRITICO | Prompt exige; alternativa: Virtual Threads Java 21 |
| 14 modulos vs. 5 atuais | ALTO | Refatoracao arquitetural significativa |
| ProviderHttpClientFactory ausente | ALTO | 50 HttpClients em memoria; gargalo a 500 TPS |
| Enums ProviderType, AuthType, SyncMode, ProviderStatus | MEDIO | Tipagem de dominio |
| DTOs UsageMetadata, BillingMetadata, GroundingMetadata, CitationMetadata | MEDIO | Normalizacao de response |
| SPI interfaces ProviderErrorMapper, ResponseNormalizer, RateLimitPolicy, AsyncJobHandle | MEDIO | Clean Architecture |
| app-cli | MEDIO | Deliverable obrigatorio do prompt |
| Documentacao per-provider (45 arquivos) | MEDIO | Definition of Done |
| BedrockModelResolver, AzureDeploymentResolver formalizados | BAIXO | Logica ja existe embutida nos providers |

---

## 3. DECISAO ARQUITETURAL

### Opcao A: Migracao Completa (14 modulos + WebFlux) — NAO RECOMENDADA
- Reescrever 50 providers (HttpClient sincrono WebClient reativo)
- 4-6 sprints adicionais, alto risco de regressao
- Descarta ~80% do trabalho ja feito

### Opcao B: Evolucao Pragmatica (5 modulos + Java 21 Virtual Threads) — RECOMENDADA

**Justificativa tecnica**:
Java 21 Virtual Threads (Project Loom) + `spring.threads.virtual.enabled=true`:
- Spring Boot 3.2+ usa VTs automaticamente para todos os servlets
- 500 TPS x 200ms latencia = 100 VTs simultaneos em flight
- Custo: 100 VTs x ~10kB = ~1MB vs. 100 OS threads x ~1MB = ~100MB
- Throughput I/O-bound: equivalente ao WebFlux sem rewrite completo

**Beneficios**:
- Mantem os 50 providers funcionais sem reescrever nada
- Menor risco; 60% menos tempo de entrega
- Reorganizacao logica dos pacotes reflete os grupos do prompt

---

## 4. ROADMAP POR FASES

---

### FASE 0 — Analise, Reconciliacao e Setup
**Duracao**: 2 dias

**Tarefas**:
- Localizar documentos referenciados no workspace (precedencia 1 a 7)
- Ativar Virtual Threads: `spring.threads.virtual.enabled: true`
- Build baseline: `mvn clean verify -q`
- Criar matriz reconciliada: 45 providers x auth x capability x sync/async

**Entregavel**: Build verde confirmado + matriz de providers preenchida

---

### FASE 1 — Enriquecimento do Core Domain
**Duracao**: 3 dias

#### Enums de Dominio (4 novos)
Localizacao: `ia-aggregator-domain/.../domain/ai/`

```java
// ProviderType.java
enum ProviderType {
    OPENAI_COMPATIBLE, NATIVE_LLM, ENTERPRISE_GATEWAY,
    MEDIA, AUDIO, SEARCH, THREAT_INTEL
}

// AuthType.java
enum AuthType {
    BEARER_TOKEN, API_KEY_HEADER, QUERY_PARAM,
    AWS_SIGV4, OAUTH2_CLIENT_CREDENTIALS, COMPOSITE
}

// SyncMode.java
enum SyncMode { SYNCHRONOUS, ASYNC_POLLING, STREAMING }

// ProviderStatus.java
enum ProviderStatus {
    IMPLEMENTED, IMPLEMENTED_WITH_RESTRICTIONS, BLOCKED, OUT_OF_SCOPE
}
```

#### SPI Interfaces (6 novas)
Localizacao: `ia-aggregator-application/.../application/ai/port/out/`

```
ProviderDescriptor       — name, type, authType, syncMode, capabilities, status, docsUrl, apiKeyUrl
ModelCatalogService      — listAll(), findByCapability(Capability), findByProvider(String)
RateLimitPolicy          — isAllowed(String provider, Capability cap): boolean
AsyncJobHandle           — submit(), poll(jobId), cancel(jobId), getResult(jobId)
ProviderErrorMapper      — toTechnicalException(int httpStatus, String body, String provider)
ProviderResponseNormalizer — normalize(JsonNode raw, Class<T> target): T
```

#### DTOs de Enriquecimento (5 novos)
```
UsageMetadata           — inputTokens, outputTokens, totalTokens, estimatedCostUsd
BillingMetadata         — provider, model, capability, estimatedCostUsd, timestamp
GroundingMetadata       — List<CitationMetadata> citations, groundingSource
CitationMetadata        — title, url, snippet, index
ProviderConfigSnapshot  — providerName, model, type, authType, syncMode, baseUrl
```

**Entregavel**: `mvn compile -q` verde com novos tipos; sem breaking changes

---

### FASE 2 — Infraestrutura Compartilhada
**Duracao**: 4 dias

#### 2.1 ProviderHttpClientFactory — CRITICO para FinOps
Localizacao: `ia-aggregator-infrastructure/.../ai/http/ProviderHttpClientFactory.java`

**Problema**: 50 providers x 1 HttpClient = 50 instancias com seus proprios thread pools.

**Solucao** — Factory Singleton com Virtual Threads:
```java
@Component
public class ProviderHttpClientFactory {
    // O(K) instancias onde K = distinct timeouts (tipicamente 2-3)
    private final ConcurrentHashMap<Long, HttpClient> cache = new ConcurrentHashMap<>();

    public HttpClient getOrCreate(long timeoutMs) {
        return cache.computeIfAbsent(timeoutMs, t ->
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(t))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build());
    }
}
```

Refatorar as 6 abstract bases para injetar `ProviderHttpClientFactory`.

#### 2.2 Auth Strategies Faltantes

```java
// CompositeAuthStrategy.java
// Aplica lista de estrategias em sequencia — util para providers que precisam
// de API key + query param simultaneamente

// AzureApiKeyAuthStrategy.java
// Header "api-key: {key}" + query "api-version={version}"
// Distinto do BearerTokenAuth — Azure usa header diferente

// OptionalAzureEntraAuthStrategy.java
// OAuth2 via Azure AD: POST https://login.microsoftonline.com/{tenantId}/oauth2/v2.0/token
// Scope: https://cognitiveservices.azure.com/.default
// Alternativa enterprise ao api-key para Azure OpenAI
```

#### 2.3 Resolvers Especializados

```java
// BedrockModelResolver.java
// Mapeia: "claude-3-5-sonnet" -> "anthropic.claude-3-5-sonnet-20241022-v2:0"
// Mapeia: "llama3-8b" -> "meta.llama3-8b-instruct-v1:0"

// AzureDeploymentResolver.java
// Mapeia model friendlyName -> deployment name configurado externamente
// Ex: "gpt-4o" -> "my-gpt4o-deployment" (definido no Azure Portal)
```

#### 2.4 DefaultProviderErrorMapper
Classifica falhas conforme exigido pelo prompt:
`auth | validation | rate_limit | upstream_4xx | upstream_5xx | timeout | parsing | compliance_block`

**Entregavel**: `mvn compile -q` verde; 50 providers usando ProviderHttpClientFactory

---

### FASE 3 — Resilience Avancada e Observabilidade
**Duracao**: 2 dias

#### Bulkhead + RateLimiter por Provider
```yaml
resilience4j:
  bulkhead:
    instances:
      aiProviderOpenAi: { maxConcurrentCalls: 50, maxWaitDuration: 500ms }
      aiProviderAnthropic: { maxConcurrentCalls: 20, maxWaitDuration: 500ms }
      # ... todos os 45 providers com valores calibrados
  rate-limiter:
    instances:
      aiProviderOpenAi: { limitForPeriod: 100, limitRefreshPeriod: 1m, timeoutDuration: 500ms }
      # ... todos os 45 providers
```

**REGRA** (do prompt): Nao aplicar retry em jobs assincronos caros (image-gen, video-gen).
Retry somente em operacoes idempotentes (chat, embed, search).

#### Correlation ID + Request ID
```java
// CorrelationIdFilter.java (OncePerRequestFilter)
// Le X-Correlation-Id do header de entrada (ou gera UUID)
// Propaga ao MDC -> aparece em todos os logs JSON do request
```

#### Log Estruturado JSON com Classificacao de Falha
```json
{
  "timestamp": "2026-03-08T10:00:00Z",
  "level": "ERROR",
  "provider": "openai",
  "capability": "CHAT",
  "failureCategory": "RATE_LIMIT",
  "correlationId": "uuid",
  "requestId": "uuid",
  "durationMs": 150
}
```

#### Metricas Adicionais (Micrometer)
```
ai.provider.polling.attempts  — histogram (async providers)
ai.provider.token.input       — counter
ai.provider.token.output      — counter
ai.provider.cost.estimate.usd — counter (FinOps)
ai.provider.bulkhead.rejected — counter
ai.provider.rate_limit.rejected — counter
```

---

### FASE 4 — Verificacao e Correcao dos 45 Providers
**Duracao**: 5 dias

#### Checklist por Provider
Para cada um dos 45 providers:
- [ ] `@ConditionalOnProperty(name = "app.ai.providers.{name}.api-key")` presente
- [ ] `capabilities()` retorna EnumSet correto
- [ ] Modelo default valido (verificado na documentacao oficial)
- [ ] Nenhuma credencial hardcoded
- [ ] Auth strategy correta

#### Providers com Atencao Especial

**Azure OpenAI** — gateway enterprise:
- URL: `https://{resource}.openai.azure.com/openai/deployments/{deployment}/chat/completions?api-version={version}`
- Auth: `AzureApiKeyAuthStrategy` OU `OptionalAzureEntraAuthStrategy`
- `AzureDeploymentResolver` obrigatorio
- Campos separados: `resource-endpoint`, `deployment-name`, `api-version`

**AWS Bedrock**:
- `BedrockModelResolver` para model friendlyName -> ARN
- `AwsSigV4Auth` com `access-key`, `secret-key`, `region`

**Cloudflare**:
- `accountId` como campo separado (nao embutido na base-url)
- URL: `https://api.cloudflare.com/client/v4/accounts/{accountId}/ai/run/{model}`

**OpenRouter**:
- Se `freeTierOnly=true`, aceitar somente modelos com sufixo `:free` (regra inviolavel)

**Perplexity Sonar**:
- `WEB_GROUNDED_CHAT`: parsear `citations` do response
- Citations: array de strings (URLs) OU objetos {title, url, snippet}

**Google Cloud NLP**:
- API nao mapeia diretamente as 13 capabilities
- Decisao: mapear como `WEB_SEARCH` com `WebSearchResult` normalizado
- Documentar como `IMPLEMENTED_WITH_RESTRICTIONS` com limitacao explicita

**Thread Safety** — confirmar:
- `StabilityAiProvider` e `FalAiProvider`: `ThreadLocal<String> currentModel`
- Verificar `currentModel.remove()` no bloco `finally` — obrigatorio para evitar memory leak

#### Providers Extras (fora do escopo mandatorio)
JinaAI, Tavily, Gladia, Leonardo.AI, Novita:
- Classificar como `IMPLEMENTED (extra — fora do escopo mandatorio)`

---

### FASE 5 — Test Suite Completa
**Duracao**: 10 dias

#### Piramide de Testes

```
         [E2E Smoke: catalog + health endpoints] (~5 testes)
        [Integracao @WebMvcTest — 12 controllers] (~60 testes)
       [WireMock — 45 providers, 3-8 testes cada] (~250 testes)
      [Unit — 12 Use Cases Mockito] (~48 testes)
     [Unit — Routing + Registry] (~15 testes)
    [Unit — Auth Strategies] (~18 testes)
   [Unit — Compliance Gate] (~6 testes)
  [Unit — Error Mapper + Factory] (~10 testes)
 [ArchUnit — regras de dependencia] (~5 regras)
[Performance — baseline 500 TPS] (~1 benchmark)
```

**Total estimado**: ~420 testes

#### Padrao WireMock por Provider
```java
class OpenAiModelProviderTest {
    MockWebServer mockServer;
    OpenAiModelProvider provider;

    // Testes obrigatorios por provider:
    @Test void chat_success_parsesContentAndUsageTokens() { ... }
    @Test void chat_rateLimited_throwsAI003() { /* enqueue 429 */ }
    @Test void chat_serverError_throwsAI002() { /* enqueue 500 */ }
    @Test void chat_circuitBreakerOpen_throwsAI002() { ... }
    // + testes por capability especifica (embed, image, etc.)
}
```

#### Testes Async Polling (media providers)
Para `StabilityAi`, `FalAi`, `Replicate`, `BflFlux`, `Runway`:
```java
@Test void generateImage_submitsAndPollsToCompletion() { /* submit ok, poll 2x, succeed */ }
@Test void generateImage_pollTimeout_throwsAI015() { /* maxPollAttempts reached */ }
@Test void generateImage_jobFailed_throwsAI010() { /* poll returns failed status */ }
@Test void generateImage_transientError_retriesAndSucceeds() { /* 500 + ok */ }
```

#### ArchUnit — Regras de Dependencia
```java
// domain nao depende de application ou infrastructure
// application nao depende de infrastructure
// Todos os providers estendem uma abstract base
// Todos os providers tem @ConditionalOnProperty
// Nenhum provider tem credencial literal no codigo-fonte
```

#### Coverage (JaCoCo)
```xml
<minimum>0.90</minimum> <!-- 90% LINE coverage em providers e use cases -->
<excludes>
  <exclude>**/dto/**</exclude>
  <exclude>**/*Config.class</exclude>
  <exclude>**/*Application.class</exclude>
</excludes>
```

**Entregavel**: `mvn clean verify` verde; coverage >= 90%

---

### FASE 6 — app-cli
**Duracao**: 3 dias

```
ia-aggregator-cli/
  src/main/java/com/ia/aggregator/cli/
    IaAggregatorCli.java          # @SpringBootApplication
    command/
      ChatCommand.java            # --provider openai --model gpt-4o --prompt "..."
      EmbedCommand.java           # --provider cohere --text "..."
      ImageCommand.java           # --provider stability --prompt "..."
      TranscribeCommand.java      # --provider deepgram --file audio.mp3
      SearchCommand.java          # --provider exa --query "..."
      ThreatIntelCommand.java     # --provider darkowl --query "..." (requer compliance flag)
      ProvidersCommand.java       # --list | --health openai | --capabilities anthropic
```

Tecnologia: `picocli` ou `Spring Shell`
Reusa os mesmos Use Cases do modulo `application`.

---

### FASE 7 — Documentacao Completa
**Duracao**: 4 dias

#### README.md Principal
- Quick start (5 minutos para rodar localmente)
- Matriz: 45 providers x auth x capability x sync/async
- Matriz: provider x docs URL x API key URL
- Exemplos curl para cada uma das 13 capabilities

#### docs/providers/ — 45 arquivos .md
Padrao por arquivo:
```markdown
# Provider: {Nome}
| Auth | Docs | API Key URL | Capabilities | Sync/Async | Status |
...
## Endpoints Mapeados
## Exemplo curl
## Exemplo Java
## Health check
## Limitacoes e Blockers
```

#### .env.example
Todas as variaveis de ambiente para os 45 providers + compliance flags.

---

### FASE 8 — FinOps e Otimizacao 500 TPS
**Duracao**: 2 dias

#### Big O — Componentes Criticos

| Componente | Big O Atual | Big O Alvo | Acao |
|-----------|-------------|-----------|------|
| ProviderRegistry.getProviders(cap) | O(1) | O(1) OK | — |
| Model lookup: supports(model) | O(M) stream | O(1) HashSet | Substituir em todos os 50 providers |
| HttpClient instantiation | O(N=50) | O(K=2-3) | ProviderHttpClientFactory (Fase 2) |
| Auth strategy | O(1) OK | O(1) OK | — |
| CircuitBreaker lookup | O(1) OK | O(1) OK | — |

#### Model Lookup O(N) -> O(1)
```java
// Substituir em todos os providers:
// DE: supportedModels.stream().anyMatch(model::equals) — O(M)
// PARA:
private final Set<String> supportedModelsSet;
// construtor: this.supportedModelsSet = new HashSet<>(supportedModels.stream().map(String::trim).toList());
// supports(): return apiKey != null && !apiKey.isBlank() && supportedModelsSet.contains(model);
```

#### Response Cache L1 (Caffeine) — Apenas Idempotentes
```
COM cache: EMBEDDINGS, RERANK, SEARCH, OCR (mesma entrada = mesmo output)
SEM cache: CHAT, RESPONSES, IMAGE_GEN, VIDEO_GEN (geração creativa/contextual)
```

#### Estimativa 500 TPS com Virtual Threads
```
500 TPS x 200ms latencia = 100 VTs em flight simultaneamente
Memoria: 100 VTs x ~10kB = ~1MB (vs. 100 OS threads x ~1MB = ~100MB)
HTTP connections: configurar maxConnections por provider (Bulkhead)
```

---

### FASE 9 — Classificacao Final dos Providers
**Duracao**: 1 dia

#### Matriz de Status Final

| Provider | Capabilities | Auth | Status | Blocker |
|----------|-------------|------|--------|---------|
| OpenAI | CHAT, RESPONSES, EMBED, IMAGE_GEN, STT, TTS | Bearer | IMPLEMENTED | Sem creditos gratuitos |
| Anthropic | CHAT, RESPONSES | x-api-key | IMPLEMENTED | — |
| Gemini | CHAT, EMBED, WEB_GROUNDED_CHAT | API key query param | IMPLEMENTED | — |
| Cohere | CHAT, EMBED, RERANK | Bearer | IMPLEMENTED | Trial nao-comercial |
| Mistral | CHAT, EMBED | Bearer | IMPLEMENTED | — |
| Groq | CHAT, STT | Bearer | IMPLEMENTED | — |
| Perplexity Sonar | CHAT, WEB_GROUNDED_CHAT | Bearer | IMPLEMENTED | — |
| Azure OpenAI | CHAT, EMBED, IMAGE_GEN | api-key / Entra OAuth2 | IMPLEMENTED_WITH_RESTRICTIONS | Requer subscription Azure |
| AWS Bedrock | CHAT, EMBED, IMAGE_GEN | AWS SigV4 | IMPLEMENTED_WITH_RESTRICTIONS | Requer conta AWS + Bedrock access |
| Cloudflare | CHAT, IMAGE_GEN | Bearer + account-id | IMPLEMENTED | — |
| HuggingFace | CHAT, EMBED, IMAGE_GEN, STT | Bearer | IMPLEMENTED | Rate limit livre baixo |
| AI21 | CHAT, EMBED | x-api-key | IMPLEMENTED | — |
| OpenRouter | CHAT | Bearer | IMPLEMENTED | Somente :free se freeTierOnly |
| DeepSeek | CHAT | Bearer | IMPLEMENTED | Creditos expiram |
| xAI/Grok | CHAT | Bearer | IMPLEMENTED | Creditos variam por conta |
| Together AI | CHAT, IMAGE_GEN | Bearer | IMPLEMENTED | — |
| NVIDIA NIM | CHAT, EMBED | Bearer | IMPLEMENTED | — |
| Fireworks AI | CHAT, EMBED, IMAGE_GEN | Bearer | IMPLEMENTED | — |
| Cerebras | CHAT | Bearer | IMPLEMENTED | — |
| SambaNova | CHAT | Bearer | IMPLEMENTED | — |
| SiliconFlow | CHAT, EMBED, IMAGE_GEN | Bearer | IMPLEMENTED | — |
| GitHub Models | CHAT, EMBED | Bearer | IMPLEMENTED_WITH_RESTRICTIONS | Requer GitHub Copilot |
| DeepInfra | CHAT, EMBED | Bearer | IMPLEMENTED | — |
| Stability AI | IMAGE_GEN, IMAGE_EDITING | Bearer | IMPLEMENTED | Sem tier gratuito |
| fal.ai | IMAGE_GEN, VIDEO_GEN | Bearer | IMPLEMENTED | — |
| Replicate | IMAGE_GEN, VIDEO_GEN | Bearer | IMPLEMENTED | Pay-per-prediction |
| BFL/FLUX | IMAGE_GEN | Bearer | IMPLEMENTED | — |
| Runway | VIDEO_GEN | Bearer | IMPLEMENTED | Plano pago obrigatorio |
| Ideogram | IMAGE_GEN | Bearer | IMPLEMENTED | — |
| Deepgram | STT, TTS | Token | IMPLEMENTED | — |
| AssemblyAI | STT | Bearer | IMPLEMENTED | — |
| ElevenLabs | TTS | xi-api-key | IMPLEMENTED | — |
| GCloud Vision | OCR | OAuth2 | IMPLEMENTED | Requer GCP project |
| GCloud Speech | STT | OAuth2 | IMPLEMENTED | Requer GCP project |
| GCloud NLP | WEB_SEARCH* | OAuth2 | IMPLEMENTED_WITH_RESTRICTIONS | Capability mapping aproximado |
| GCloud Translation | WEB_SEARCH* | OAuth2 | IMPLEMENTED_WITH_RESTRICTIONS | Capability mapping aproximado |
| GCloud TTS | TEXT_TO_SPEECH | OAuth2 | IMPLEMENTED | Requer GCP project |
| Exa | WEB_SEARCH | x-api-key | IMPLEMENTED | — |
| NewsCatcher | WEB_SEARCH | x-api-key | IMPLEMENTED | — |
| Onion Search | THREAT_INTEL_SEARCH | Custom | IMPLEMENTED | dark-web-enabled=true obrigatorio |
| DarkOwl | THREAT_INTEL_SEARCH | Bearer | IMPLEMENTED | Enterprise plan; dark-web-enabled=true |
| Twingly | THREAT_INTEL_SEARCH | Bearer | IMPLEMENTED | dark-web-enabled=true obrigatorio |
| FullHunt | THREAT_INTEL_SEARCH | Bearer | IMPLEMENTED | dark-web-enabled=true obrigatorio |
| DarknetSearch/Kaduu | THREAT_INTEL_SEARCH | Bearer | BLOCKED | Documentacao limitada; acesso enterprise |
| Flare | THREAT_INTEL_SEARCH | Bearer | IMPLEMENTED_WITH_RESTRICTIONS | Trial restrito |
| **Midjourney** | — | — | **OUT_OF_SCOPE** | Sem API publica oficial |
| JinaAI (extra) | EMBED, RERANK | Bearer | IMPLEMENTED (extra) | Fora do escopo mandatorio |
| Tavily (extra) | WEB_SEARCH | Bearer | IMPLEMENTED (extra) | Fora do escopo mandatorio |
| Gladia (extra) | STT | x-gladia-key | IMPLEMENTED (extra) | Fora do escopo mandatorio |
| Leonardo.AI (extra) | IMAGE_GEN | Bearer | IMPLEMENTED (extra) | Fora do escopo mandatorio |
| Novita (extra) | CHAT | Bearer | IMPLEMENTED (extra) | Fora do escopo mandatorio |

---

### FASE 10 — Definition of Done e Evidencias Finais
**Duracao**: 2 dias

#### Comandos de Verificacao Obrigatorios

```bash
# 1. Build limpo
mvn clean compile -q
# Esperado: BUILD SUCCESS, zero erros

# 2. Testes completos
mvn clean verify
# Esperado: BUILD SUCCESS, 100% testes passando

# 3. Coverage
mvn verify -Pjacoco
# Esperado: LINE coverage >= 90% em providers/use-cases

# 4. ArchUnit
mvn test -pl ia-aggregator-infrastructure -Dtest="ArchitectureTest"
# Esperado: 0 violations

# 5. Aplicacao sobe sem erros
mvn spring-boot:run -pl ia-aggregator-presentation &
# Providers sem credencial = simplesmente nao registrados (ConditionalOnProperty)

# 6. Catalog com 45+ providers
curl http://localhost:8080/api/v1/ai/providers/catalog | jq '. | length'

# 7. Health de todos os providers
curl http://localhost:8080/api/v1/ai/providers/health

# 8. Compliance gate bloqueando threat intel
curl -X POST http://localhost:8080/api/v1/ai/threat-intel
# Esperado: 403 Forbidden (dark-web-enabled=false por padrao)

# 9. Capabilities disponiveis
curl http://localhost:8080/api/v1/ai/providers/capabilities
# Esperado: 13 capabilities no JSON
```

#### Deliverables Finais Obrigatorios

- [ ] Codigo-fonte completo (50 providers, todos compilando)
- [ ] `pom.xml` raiz e por modulo
- [ ] `application.yml` com todos os providers + compliance
- [ ] `.env.example` (45+ providers)
- [ ] `README.md` com matrizes completas + quick start
- [ ] `docs/providers/` — 45+ arquivos `.md`
- [ ] Testes automatizados passando (evidencia: output do `mvn verify`)
- [ ] Coverage report (evidencia: JaCoCo HTML)
- [ ] Matriz IMPLEMENTED / IMPLEMENTED_WITH_RESTRICTIONS / BLOCKED / OUT_OF_SCOPE
- [ ] `BLOCKERS.md` — blockers reais por provider, sem esconder pendencias

---

## 5. CRONOGRAMA RESUMIDO

| Sprint | Fase | Duracao | Entregavel Chave |
|--------|------|---------|-----------------|
| Sprint 0 | Fase 0 | 2 dias | Baseline verde, VT habilitado, matriz de providers |
| Sprint 1 | Fase 1 + Fase 2 inicio | 5 dias | Domain enriquecido, ProviderHttpClientFactory |
| Sprint 2 | Fase 2 fim + Fase 3 + Fase 4 | 7 dias | Auth completo, resilience, 45 providers verificados |
| Sprint 3 | Fase 5 inicio + Fase 6 | 7 dias | 50% testes WireMock + app-cli |
| Sprint 4 | Fase 5 meio | 5 dias | 80% testes WireMock |
| Sprint 5 | Fase 5 fim | 5 dias | 100% testes + coverage >= 90% |
| Sprint 6 | Fase 7 + Fase 8 | 6 dias | Docs completa + FinOps + 500 TPS baseline |
| Sprint 7 | Fase 9 + Fase 10 | 3 dias | Classificacao final + Definition of Done |
| **TOTAL** | | **~40 dias uteis** | **Plataforma production-ready** |

---

## 6. REGRAS INVIOLAVEIS (Do Prompt — Literalmente)

1. Nunca hardcodar: precos, quotas, rate limits, catalogos de modelos, creditos promocionais
2. OpenRouter: `freeTierOnly=true` obriga somente modelos com sufixo `:free`
3. Cloudflare: `accountId` + `apiToken` separados — nao embutidos na base-url
4. Azure OpenAI: gateway enterprise distinto do OpenAI publico — `deployment-name`, `resource-endpoint`, `api-version` obrigatorios e separados
5. Perplexity: capturar `citations` e `search_metadata` quando presentes no response
6. Threat Intel: `security.compliance.dark-web-enabled=true` obrigatorio; auditoria com ator tecnico + request ID + justificativa; negar com flag OFF
7. Nunca logar API keys ou segredos em claro
8. Nunca afirmar 100% de sucesso sem evidencia executada de build e testes reais
9. WireMock/mocks: apoiam testes locais, nao contam como prova final de integracao online
10. Providers sem validacao online: marcar como BLOCKED com causa exata

---

## 7. BLOCKERS PREVISTOS

| Provider | Blocker | Tipo |
|----------|---------|------|
| DarkOwl | Enterprise plan obrigatorio; pricing nao publico | Credencial/Plano |
| DarknetSearch/Kaduu | Documentacao limitada; acesso enterprise | Credencial/Docs |
| Flare | Trial restrito; comercial somente | Credencial/Plano |
| AWS Bedrock | Conta AWS + request de acesso por modelo no console | Credencial/Regiao |
| Azure OpenAI | Subscription Azure + deployment configurado no Azure Portal | Credencial/Enterprise |
| Runway | API paga; sem tier gratuito | Credencial |
| Stability AI | Sem tier gratuito real | Credencial |
| Replicate | Pay-per-prediction; sem creditos automaticos | Credencial |

---

*Roadmap gerado por analise rigorosa e linha a linha do `prompt-mestre-claude-opus-4.6.txt`.*
*Toda implementacao deve seguir a ordem das fases, executar os comandos de verificacao,*
*e documentar blockers honestamente antes de declarar conclusao.*
*Regra de ouro: nunca escrever "100% de sucesso" sem evidencia de build, execucao e testes reais.*
