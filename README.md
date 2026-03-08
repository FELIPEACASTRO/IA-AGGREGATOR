# IA-AGGREGATOR / Lume Codex Cloud

Monorepo com backend Java (auth/ai/analytics) e frontend Next.js com módulo de cloud coding agents (`/codex`).

## Stack
- Frontend: Next.js 15, React 19, TypeScript, Tailwind v4
- Cloud task runtime: Prisma + PostgreSQL, Redis + BullMQ, SSE
- Backend existente: Spring Boot (auth e serviços de IA)

## Subir solução completa
No PowerShell (raiz do projeto):

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

## Bootstrap do módulo Codex Cloud
Na pasta `frontend`:

```powershell
$env:CODEX_DATABASE_URL='postgresql://ia_aggregator:ia_aggregator@localhost:5432/ia_aggregator?schema=codex'
npm install
npm run codex:bootstrap
npm run dev
```

## Principais rotas
- `/codex`
- `/codex/tasks/[taskId]`
- `/codex/settings/connectors`
- `/codex/settings/environments`
- `/codex/settings/code-review`
- `/codex/settings/usage`
- `/codex/settings/analytics`
- `/admin/settings`

## Qualidade (frontend)
```powershell
npm --prefix frontend run lint
npm --prefix frontend run type-check
npm --prefix frontend run build
npm --prefix frontend run test -- --runInBand
```

## Integracao canônica de IA
- Owner canônico: backend Spring Boot
- Providers oficialmente endurecidos: OpenAI, Gemini, DeepSeek, Anthropic, xAI e Perplexity
- O frontend Next.js atua como BFF/proxy e nao fala mais diretamente com providers externos

### Variáveis de ambiente obrigatórias
Copie `.env.example` e injete as credenciais no ambiente do sistema, CI ou secret manager. Nunca versione `.env`.

| Variável | Uso |
| --- | --- |
| `OPENAI_API_KEY` | OpenAI |
| `GEMINI_API_KEY` | Gemini |
| `DEEPSEEK_API_KEY` | DeepSeek |
| `ANTHROPIC_API_KEY` | Anthropic |
| `XAI_API_KEY` | xAI |
| `PERPLEXITY_API_KEY` | Perplexity |
| `RUN_REAL_AI_TESTS` | Habilita smoke tests reais opcionais |

### Testes
Mockados:
```powershell
mvn -q -f backend/pom.xml test
npm --prefix frontend run test -- --runInBand
```

Reais opcionais:
```powershell
$env:RUN_REAL_AI_TESTS='true'
npm --prefix frontend run test:release
```

### Checklist de segurança
- Nenhuma API key em código-fonte
- `.env` ignorado no git
- Logs sem headers `Authorization` e sem valores de segredo
- Providers ausentes retornam `NOT_CONFIGURED`/erro claro sem expor valores
- Smoke tests reais só rodam quando explicitamente habilitados

## Documentação técnica
Arquivos gerados na raiz:
- `PRODUCT_SPEC.md`
- `PARITY_MATRIX.md`
- `ASSUMPTIONS.md`
- `ROUTES.md`
- `DATA_MODEL.md`
- `API_CONTRACT.md`
- `EVENT_MODEL.md`
- `STATE_MACHINES.md`
- `SECURITY.md`
- `CONNECTORS.md`
- `RUNBOOK.md`
- `TEST_PLAN.md`
- `CHANGELOG_IMPLEMENTATION.md`
- `docs/roadmap-baseline-matrix.md`

