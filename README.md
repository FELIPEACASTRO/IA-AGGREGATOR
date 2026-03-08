# IA-AGGREGATOR / Lume Codex Cloud

Monorepo com backend Spring Boot para `auth`, `organizations`, `AI gateway`, `analytics` e `billing metadata`, mais frontend Next.js que atua como BFF e UI do produto.

## Stack
- Backend: Spring Boot 3.4, Java 21, Resilience4j, Micrometer
- Frontend: Next.js 15, React 19, TypeScript, Tailwind v4
- Dados operacionais: Prisma, PostgreSQL, Redis, BullMQ
- IA canonica: backend Spring Boot com adapters oficiais para OpenAI, Gemini, DeepSeek, Anthropic, xAI e Perplexity

## Subir a solucao completa
Na raiz do projeto:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-solution.ps1
```

Sem rebuild:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-solution.ps1 -SkipBuild
```

Parar:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-solution.ps1
```

## Configuracao de ambiente
Copie `.env.example`, injete os valores no ambiente local, CI ou secret manager e nunca versione `.env`.

Variaveis obrigatorias para os providers oficiais:

| Variavel | Uso |
| --- | --- |
| `OPENAI_API_KEY` | OpenAI |
| `GEMINI_API_KEY` | Gemini |
| `DEEPSEEK_API_KEY` | DeepSeek |
| `ANTHROPIC_API_KEY` | Anthropic |
| `XAI_API_KEY` | xAI |
| `PERPLEXITY_API_KEY` | Perplexity |
| `RUN_REAL_AI_TESTS` | Habilita smoke tests reais opcionais |

Variaveis opcionais de teste local:

| Variavel | Uso |
| --- | --- |
| `NEXT_PUBLIC_API_URL` | URL do backend para o BFF do frontend |
| `E2E_RELEASE_EMAIL` | Credencial real da suite release |
| `E2E_RELEASE_PASSWORD` | Credencial real da suite release |
| `E2E_AI_MODEL` | Modelo preferido no E2E real |

## Arquitetura da integracao de IA
- O backend Spring Boot e o owner canonico da integracao multi-provider.
- O frontend nao chama OpenAI, Gemini, Anthropic, DeepSeek, xAI ou Perplexity diretamente.
- O browser fala apenas com o BFF do Next.js.
- O BFF encaminha para o backend canonico de IA.
- Providers ausentes falham com erro explicito de configuracao; nao existe fallback mock em runtime.

## Endpoints canonicos
- `POST /api/v1/ai/chat`
- `POST /api/v1/ai/chat/stream`
- `GET /api/v1/ai/providers`
- `GET /api/v1/ai/providers/health`
- `GET /api/v1/ai/providers/{providerId}/health`

## Como rodar localmente
Backend:

```powershell
mvn -q -f backend/pom.xml test
```

Frontend:

```powershell
npm --prefix frontend install
npm --prefix frontend run lint
npm --prefix frontend run type-check
npm --prefix frontend run test -- --runInBand
npm --prefix frontend run build
```

## Cobertura
Backend com JaCoCo:

```powershell
mvn -q -f backend/pom.xml verify
```

Frontend com coverage:

```powershell
npm --prefix frontend run test:coverage
```

## Testes mockados
Backend:

```powershell
mvn -q -f backend/pom.xml test
```

Frontend:

```powershell
npm --prefix frontend run test -- --runInBand
```

## Smoke tests reais opcionais
Backend:
- Classe: `OfficialProvidersRealSmokeTest`
- So executa com `RUN_REAL_AI_TESTS=true`
- Cada teste faz `skip` automatico quando a env var especifica do provider estiver ausente

Frontend release:

```powershell
$env:RUN_REAL_AI_TESTS='true'
$env:E2E_RELEASE_EMAIL='seu-usuario'
$env:E2E_RELEASE_PASSWORD='sua-senha'
npm --prefix frontend run test:release
```

## Como adicionar um novo provider
1. Criar um adapter em `backend/ia-aggregator-infrastructure/.../provider`.
2. Implementar o contrato `AiProviderPort`.
3. Expor `providerId`, modelos suportados, `healthCheck`, `estimateCost` e estrategia de erro.
4. Configurar env vars e defaults no `application.yml`.
5. Adicionar testes unitarios, testes com `MockWebServer` e smoke real opcional.
6. Atualizar o BFF/frontend apenas no catalogo visual, nunca com chamadas diretas ao provider.

## Exemplos de uso
Chat simples via backend:

```json
POST /api/v1/ai/chat
{
  "prompt": "Explique o que e fallback de modelos em uma frase.",
  "preferredModel": "gpt-4o-mini",
  "provider": "openai",
  "fallbackProviders": ["anthropic", "gemini"]
}
```

Streaming via backend:

```json
POST /api/v1/ai/chat/stream
{
  "prompt": "Responda em uma frase curta.",
  "preferredModel": "claude-3-5-haiku-latest",
  "provider": "anthropic",
  "stream": true
}
```

## Checklist de seguranca
- Nenhum secret hardcoded no codigo
- `.env` ignorado no git
- API keys lidas por variaveis de ambiente
- Logs sem `Authorization`, tokens ou API keys
- Payloads sensiveis sanitizados antes de aparecer em mensagens de erro
- Providers configuraveis por env e health/status no backend
- Smoke real somente via `RUN_REAL_AI_TESTS=true`

## Documentacao adicional
- [AI provider matrix](docs/AI_PROVIDER_CONFIGURATION.md)
- [API contract](API_CONTRACT.md)
- [Runbook](RUNBOOK.md)
- [Test plan](TEST_PLAN.md)
