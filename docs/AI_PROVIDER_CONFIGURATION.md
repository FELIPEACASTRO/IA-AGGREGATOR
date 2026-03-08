# AI Provider Configuration Matrix

Este documento consolida os providers suportados no backend e as variáveis de ambiente necessárias para habilitação.

## Trilhas oficiais endurecidas

Os providers oficialmente suportados e cobertos por contrato, testes e documentação reforçada são:

- OpenAI
- Gemini
- DeepSeek
- Anthropic
- xAI
- Perplexity

## Regras Gerais

- Um provider só é considerado elegível quando sua chave está configurada e o modelo/deployment solicitado está em sua lista de suporte.
- Todos os providers usam timeout/retry/backoff configuráveis via `application.yml`.
- Circuit breakers são configurados em `resilience4j.circuitbreaker.instances`.

## Providers Disponíveis

### OpenAI
- Prefixo de variáveis: `OPENAI_`
- Chave principal: `OPENAI_API_KEY`
- Model list: `OPENAI_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderOpenai`

### Anthropic
- Prefixo: `ANTHROPIC_`
- Chave: `ANTHROPIC_API_KEY`
- Model list: `ANTHROPIC_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderAnthropic`

### Gemini
- Prefixo: `GEMINI_`
- Chave: `GEMINI_API_KEY`
- Model list: `GEMINI_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderGemini`

### OpenRouter
- Prefixo: `OPENROUTER_`
- Chave: `OPENROUTER_API_KEY`
- Model list: `OPENROUTER_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderOpenrouter`

### Cohere
- Prefixo: `COHERE_`
- Chave: `COHERE_API_KEY`
- Model list: `COHERE_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderCohere`

### DeepSeek
- Prefixo: `DEEPSEEK_`
- Chave: `DEEPSEEK_API_KEY`
- Model list: `DEEPSEEK_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderDeepseek`

### Groq
- Prefixo: `GROQ_`
- Chave: `GROQ_API_KEY`
- Model list: `GROQ_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderGroq`

### Mistral
- Prefixo: `MISTRAL_`
- Chave: `MISTRAL_API_KEY`
- Model list: `MISTRAL_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderMistral`

### Perplexity
- Prefixo: `PERPLEXITY_`
- Chave: `PERPLEXITY_API_KEY`
- Model list: `PERPLEXITY_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderPerplexity`

### Together
- Prefixo: `TOGETHER_`
- Chave: `TOGETHER_API_KEY`
- Model list: `TOGETHER_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderTogether`

### Fireworks
- Prefixo: `FIREWORKS_`
- Chave: `FIREWORKS_API_KEY`
- Model list: `FIREWORKS_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderFireworks`

### xAI
- Prefixo: `XAI_`
- Chave: `XAI_API_KEY`
- Model list: `XAI_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderXai`

### Azure OpenAI
- Prefixo: `AZURE_OPENAI_`
- Chave: `AZURE_OPENAI_API_KEY`
- Endpoint base: `AZURE_OPENAI_BASE_URL`
- API version: `AZURE_OPENAI_API_VERSION`
- Deployment list: `AZURE_OPENAI_SUPPORTED_DEPLOYMENTS`
- Circuit breaker: `aiProviderAzureOpenai`

### NVIDIA NIM
- Prefixo: `NVIDIA_`
- Chave: `NVIDIA_API_KEY`
- Model list: `NVIDIA_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderNvidia`

### Cerebras
- Prefixo: `CEREBRAS_`
- Chave: `CEREBRAS_API_KEY`
- Model list: `CEREBRAS_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderCerebras`

### SambaNova
- Prefixo: `SAMBANOVA_`
- Chave: `SAMBANOVA_API_KEY`
- Model list: `SAMBANOVA_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderSambanova`

### Novita
- Prefixo: `NOVITA_`
- Chave: `NOVITA_API_KEY`
- Model list: `NOVITA_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderNovita`

## Guardrails

- Prompt guardrail:
  - `AI_MAX_PROMPT_LENGTH`
  - `AI_BLOCKED_PATTERNS`
  - `AI_PROMPT_ACTION` (`block` ou `log-only`)
- Output guardrail:
  - `AI_MAX_OUTPUT_LENGTH`
  - `AI_OUTPUT_BLOCKED_PATTERNS`
  - `AI_OUTPUT_ACTION` (`block` ou `log-only`)

## Testes reais opcionais

- Flag canônica: `RUN_REAL_AI_TESTS=true`
- Backend: smoke tests reais rodam apenas quando a flag estiver habilitada e a env var do provider existir
- Frontend: a suíte real usa `RUN_REAL_AI_TESTS=true` + `E2E_RELEASE_EMAIL` + `E2E_RELEASE_PASSWORD`

## Checklist de Ativação

1. Definir a API key do provider desejado por variável de ambiente.
2. Garantir que o modelo/deployment está presente na variável `*_SUPPORTED_MODELS` (ou `AZURE_OPENAI_SUPPORTED_DEPLOYMENTS`).
3. Verificar conectividade HTTP ao endpoint configurado.
4. Subir aplicação e validar `GET /actuator/health`.
5. Validar `GET /api/v1/ai/providers` e `GET /api/v1/ai/providers/health`.
6. Validar execução funcional do endpoint `POST /api/v1/ai/chat`.
7. Nunca armazenar chaves em arquivos versionados, testes, scripts ou snapshots.
