# AI Provider Configuration Matrix

Este documento consolida os providers suportados no backend e as variaveis de ambiente necessarias para habilitacao.

## Trilhas oficiais endurecidas

Os providers oficialmente suportados e cobertos por contrato, testes e documentacao reforcada sao:

- OpenAI
- Gemini
- DeepSeek
- Anthropic
- xAI
- Perplexity

## Regras gerais

- Um provider so e elegivel quando sua chave esta configurada e o modelo solicitado esta em sua lista de suporte.
- Todos os providers usam timeout, retry, backoff e circuit breaker configuraveis.
- Falta de credencial nao derruba a aplicacao inteira: o provider sobe como `not configured`.
- O frontend consome apenas os endpoints do backend canonico.

## Variaveis dos providers oficiais

### OpenAI
- Prefixo: `OPENAI_`
- Chave: `OPENAI_API_KEY`
- Base URL: `OPENAI_BASE_URL`
- Modelos: `OPENAI_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderOpenai`

### Gemini
- Prefixo: `GEMINI_`
- Chave: `GEMINI_API_KEY`
- Base URL: `GEMINI_BASE_URL`
- Modelos: `GEMINI_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderGemini`

### DeepSeek
- Prefixo: `DEEPSEEK_`
- Chave: `DEEPSEEK_API_KEY`
- Base URL: `DEEPSEEK_BASE_URL`
- Modelos: `DEEPSEEK_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderDeepseek`

### Anthropic
- Prefixo: `ANTHROPIC_`
- Chave: `ANTHROPIC_API_KEY`
- Base URL: `ANTHROPIC_BASE_URL`
- Modelos: `ANTHROPIC_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderAnthropic`

### xAI
- Prefixo: `XAI_`
- Chave: `XAI_API_KEY`
- Base URL: `XAI_BASE_URL`
- Modelos: `XAI_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderXai`

### Perplexity
- Prefixo: `PERPLEXITY_`
- Chave: `PERPLEXITY_API_KEY`
- Base URL: `PERPLEXITY_BASE_URL`
- Modelos: `PERPLEXITY_SUPPORTED_MODELS`
- Circuit breaker: `aiProviderPerplexity`

## Outros providers preservados no catalogo

O repositorio ainda preserva adapters extras ja existentes, como OpenRouter, Groq, Cohere, Mistral, Together, Fireworks, Azure OpenAI, NVIDIA NIM, Cerebras, SambaNova e Novita. Eles continuam disponiveis no backend, mas os seis providers acima sao a trilha oficial endurecida nesta rodada.

## Guardrails

Prompt guardrail:
- `AI_MAX_PROMPT_LENGTH`
- `AI_BLOCKED_PATTERNS`
- `AI_PROMPT_ACTION`

Output guardrail:
- `AI_MAX_OUTPUT_LENGTH`
- `AI_OUTPUT_BLOCKED_PATTERNS`
- `AI_OUTPUT_ACTION`

## Testes reais opcionais

- Flag canonica: `RUN_REAL_AI_TESTS=true`
- Backend: `OfficialProvidersRealSmokeTest`
- Frontend: specs em `frontend/e2e/release`
- Se a env var especifica do provider estiver ausente, o teste faz `skip` automatico

## Checklist de ativacao

1. Definir a API key do provider por variavel de ambiente.
2. Garantir que o modelo esteja presente em `*_SUPPORTED_MODELS`.
3. Validar conectividade ao endpoint configurado.
4. Subir a aplicacao e validar `GET /api/v1/ai/providers`.
5. Validar `GET /api/v1/ai/providers/health`.
6. Executar `POST /api/v1/ai/chat`.
7. Executar `POST /api/v1/ai/chat/stream` com `stream=true`.
8. Nunca armazenar chaves em arquivos versionados, scripts, testes ou snapshots.
