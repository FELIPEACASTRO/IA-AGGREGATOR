# CONTRIBUTING — Guia de Contribuicao

> Standards de codigo, processo de PR, commits, review e boas praticas.
> Versao: 2.0.0 | Ultima atualizacao: 2026-03-08

---

## Setup do Ambiente

### Pre-Requisitos

| Software | Versao | Instalacao |
|----------|--------|-----------|
| Node.js | 22.x | [nodejs.org](https://nodejs.org) |
| Java (Temurin) | 21 | [adoptium.net](https://adoptium.net) |
| Maven | 3.9+ | [maven.apache.org](https://maven.apache.org) |
| Docker Desktop | 4.x | [docker.com](https://docker.com) |
| Git | 2.40+ | [git-scm.com](https://git-scm.com) |

### Primeiro Setup

```bash
# 1. Clonar repositorio
git clone <repo-url>
cd IA-AGGREGATOR

# 2. Subir infraestrutura
docker compose up -d

# 3. Backend
cd backend
mvn clean verify

# 4. Frontend
cd ../frontend
npm install
export CODEX_DATABASE_URL='postgresql://ia_aggregator:ia_aggregator@localhost:5432/ia_aggregator?schema=codex'
npm run codex:bootstrap
npm run dev
```

Ver [RUNBOOK.md](./RUNBOOK.md) para detalhes completos.

---

## Estrutura do Projeto

```
IA-AGGREGATOR/
├── backend/                          # Spring Boot 3.4 (Java 21)
│   ├── ia-aggregator-common/         # DTOs, utils
│   ├── ia-aggregator-domain/         # Entidades, VOs, eventos
│   ├── ia-aggregator-application/    # Use cases, portas
│   ├── ia-aggregator-infrastructure/ # Adapters (DB, AI, Redis)
│   └── ia-aggregator-presentation/   # Controllers, config
├── frontend/                         # Next.js 15 (React 19)
│   ├── src/app/                      # App Router pages
│   ├── src/components/               # Componentes React
│   ├── src/lib/                      # Utilitarios
│   ├── src/stores/                   # Zustand stores
│   ├── src/server/codex/             # Server-side (task runner, auth)
│   ├── prisma/                       # Schema Prisma
│   └── e2e/                          # Playwright tests
├── scripts/                          # Scripts de automacao
├── docker-compose.yml                # PostgreSQL + Redis
└── *.md                              # Documentacao
```

---

## Convencoes de Codigo

### Frontend (TypeScript/React)

| Regra | Convencao |
|-------|-----------|
| Linguagem | TypeScript strict (no `any`) |
| Framework CSS | Tailwind v4 (CSS nativo, sem config JS) |
| State management | Zustand (stores em `src/stores/`) |
| Data fetching | React Query (`@tanstack/react-query`) |
| Validacao | Zod schemas |
| Componentes | Function components + hooks |
| Imports | Absolutos (`@/components/...`) |
| Nomes de arquivo | kebab-case (`task-list.tsx`) |
| Nomes de componente | PascalCase (`TaskList`) |
| Nomes de funcao | camelCase (`handleSubmit`) |
| i18n | `next-intl` (chaves em pt-BR) |
| Testes | Colocados ao lado (`page.test.tsx`) |

### Backend (Java)

| Regra | Convencao |
|-------|-----------|
| Linguagem | Java 21 |
| Arquitetura | Clean/Hexagonal (ports + adapters) |
| Nomes de classe | PascalCase (`ChatUseCaseImpl`) |
| Nomes de metodo | camelCase (`executeChatRequest`) |
| Pacotes | `com.ia.aggregator.{modulo}.{camada}` |
| DTOs | Records Java |
| Mapeamento | MapStruct |
| Validacao | Bean Validation (`@Valid`) |
| Testes | Sufixo `Test` (unit) ou `IntegrationTest` |

### SQL/Banco

| Regra | Convencao |
|-------|-----------|
| Prisma schema | `camelCase` para campos |
| Flyway migrations | `V{n}__{descricao}.sql` |
| Schemas | `codex` (Prisma), `auth`/`billing`/etc. (Flyway) |
| Indices | `idx_{tabela}_{campo}` |

---

## Processo de Branches

```
main (protegida)
  └── develop
       ├── feature/XXX-descricao
       ├── fix/XXX-descricao
       ├── chore/descricao
       └── refactor/descricao
```

| Prefixo | Uso |
|---------|-----|
| `feature/` | Nova funcionalidade |
| `fix/` | Correcao de bug |
| `chore/` | Manutencao, deps, config |
| `refactor/` | Refatoracao sem mudanca funcional |
| `docs/` | Apenas documentacao |
| `test/` | Apenas testes |

---

## Convencao de Commits

Formato: `tipo(escopo): descricao`

```
feat(tasks): adicionar retry automatico com backoff
fix(auth): corrigir validacao de JWT expirado
chore(deps): atualizar prisma para 6.17.0
refactor(chat): extrair logica de streaming para hook
docs(security): documentar threat model STRIDE
test(webhooks): adicionar testes de integracao para GitHub
```

| Tipo | Uso |
|------|-----|
| `feat` | Nova funcionalidade |
| `fix` | Correcao de bug |
| `chore` | Manutencao |
| `refactor` | Refatoracao |
| `docs` | Documentacao |
| `test` | Testes |
| `perf` | Performance |
| `ci` | CI/CD |
| `style` | Formatacao (sem mudanca logica) |

### Escopos Comuns

| Escopo | Area |
|--------|------|
| `tasks` | Task lifecycle |
| `auth` | Autenticacao/autorizacao |
| `chat` | Chat com IA |
| `envs` | Environments |
| `webhooks` | Conectores webhook |
| `billing` | Credits/billing |
| `ui` | Componentes visuais |
| `api` | Endpoints API |
| `deps` | Dependencias |
| `frontend` | Frontend geral |
| `backend` | Backend geral |

---

## Processo de Pull Request

### Antes de Abrir PR

```bash
# Frontend
npm --prefix frontend run quality:ci

# Backend
cd backend && mvn clean verify
```

### Template de PR

```markdown
## O que muda?
Descricao breve das alteracoes.

## Por que?
Motivacao e contexto.

## Como testar?
1. Passo 1
2. Passo 2
3. Resultado esperado

## Checklist
- [ ] Testes adicionados/atualizados
- [ ] Lint passa sem warnings
- [ ] Type-check passa
- [ ] Build funciona
- [ ] Documentacao atualizada (se aplicavel)
- [ ] Sem secrets no codigo
- [ ] Sem console.log em producao
```

### Criterios de Review

| Criterio | Obrigatorio | Descricao |
|----------|------------|-----------|
| CI verde | Sim | Todos os checks passando |
| Testes | Sim (para features/fixes) | Cobertura adequada |
| Type safety | Sim | Sem `any`, sem `@ts-ignore` |
| Seguranca | Sim | Sem XSS, injection, secrets expostos |
| Performance | Recomendado | Sem regressao de bundle/queries |
| Documentacao | Se aplicavel | Atualizar .md relevantes |
| 1 aprovacao | Sim | Minimo 1 reviewer |

---

## Escrevendo Testes

### Frontend — Unit (Jest)

```typescript
// src/stores/example-store.test.ts
import { useExampleStore } from './example-store'

describe('ExampleStore', () => {
  beforeEach(() => {
    useExampleStore.setState({ /* reset */ })
  })

  it('should do something', () => {
    const { doSomething } = useExampleStore.getState()
    doSomething()
    expect(useExampleStore.getState().result).toBe('expected')
  })
})
```

### Frontend — E2E (Playwright)

```typescript
// e2e/example.spec.ts
import { test, expect } from '@playwright/test'
import { loginUserViaUi } from './support/auth'

test('deve realizar acao esperada', async ({ page }) => {
  await loginUserViaUi(page)
  await page.goto('/codex')
  await expect(page.getByText('Tasks')).toBeVisible()
})
```

### Backend — Unit (JUnit 5)

```java
@ExtendWith(MockitoExtension.class)
class ExampleUseCaseImplTest {
    @Mock private ExamplePort port;
    @InjectMocks private ExampleUseCaseImpl useCase;

    @Test
    void execute_shouldReturnExpectedResult() {
        when(port.find(any())).thenReturn(Optional.of(entity));
        var result = useCase.execute(input);
        assertThat(result).isNotNull();
    }
}
```

---

## Documentacao

### Quando Atualizar

| Mudanca | Docs Afetados |
|---------|--------------|
| Novo endpoint API | API_CONTRACT.md, ROUTES.md |
| Novo modelo de dados | DATA_MODEL.md |
| Nova rota de pagina | ROUTES.md |
| Novo estado/transicao | STATE_MACHINES.md, EVENT_MODEL.md |
| Novo conector | CONNECTORS.md |
| Mudanca de seguranca | SECURITY.md |
| Novo teste | TEST_PLAN.md |
| Mudanca de config | RUNBOOK.md, DEPLOYMENT.md |

### Idioma

- Documentacao: **Portugues do Brasil** (pt-BR)
- Termos tecnicos em ingles aceitos: JWT, SSE, CRUD, API, etc.
- Codigo e comentarios: **Ingles**

---

## Seguranca

### Regras Absolutas

1. **NUNCA** commitar secrets, API keys ou senhas
2. **NUNCA** usar `httpOnly: false` em cookies de autenticacao
3. **NUNCA** aceitar input do usuario sem validacao (Zod/Bean Validation)
4. **NUNCA** usar `dangerouslySetInnerHTML` ou `rehype-raw` sem sanitizacao
5. **SEMPRE** verificar assinatura HMAC em webhooks
6. **SEMPRE** validar JWT com verificacao de assinatura
7. **SEMPRE** usar parameterized queries (Prisma/JPA fazem por padrao)

### Reportando Vulnerabilidades

Vulnerabilidades de seguranca devem ser reportadas de forma **privada** ao time de seguranca, nao em issues publicas.

---

## Referencias

- [ARCHITECTURE.md](./ARCHITECTURE.md) — Visao tecnica
- [SECURITY.md](./SECURITY.md) — Requisitos de seguranca
- [TEST_PLAN.md](./TEST_PLAN.md) — Estrategia de testes
- [PERFORMANCE.md](./PERFORMANCE.md) — Requisitos de performance
- [ROADMAP.md](./ROADMAP.md) — Gaps e prioridades
