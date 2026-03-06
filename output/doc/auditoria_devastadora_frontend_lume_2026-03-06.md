# Auditoria Devastadora do Frontend - Lume / IA-AGGREGATOR

Data: 2026-03-06
Autor: Codex (forca-tarefa simulada UX/UI/CX/A11y/DS/FE/BE/Arq/Perf/QA)
Escopo: frontend completo (`frontend/src/app`, `frontend/src/components`, `frontend/src/lib`, `frontend/src/stores`)

## 1. Veredito Executivo

O frontend esta compilavel, testavel e navegavel em desktop, mas ainda nao e um produto maduro. Ele transmite esforco visual, nao sistema.

Os 5 problemas sistemicos mais graves:
- Shell autenticado quebra em mobile e compromete navegacao/legibilidade.
- Design system existe na base, mas nao governa implementacao das telas.
- Linguagem visual e textual fragmentada (PT-BR quebrado + ingles solto + copy interna).
- Analytics esta com semantica de ferramenta interna em rota de produto.
- Performance percebida muito abaixo para um workspace premium.

Risco para negocio:
- Conversao: login/landing com latencia alta e discurso visual inconsistente.
- Confianca: telas parecem "internas" em vez de produto acabado.
- Retencao: uso em mobile comprometido nas rotas autenticadas.
- Manutencao: cada rota continua inventando padrao proprio.

## 2. Scorecard 0-10

| Dominio | Nota | Evidencia resumida |
|---|---:|---|
| Identidade visual | 4 | Paleta coerente, semantica de pagina inconsistente |
| Consistencia de UI | 4 | Shell compartilhado, componentes e densidade variam demais |
| Tipografia | 3 | 46 tokens text arbitrarios, microtamanhos excessivos |
| Navegabilidade | 5 | Desktop usavel, IA de rotas e analytics mal posicionados |
| UX/CX | 4 | Fluxos por pagina, nao por tarefa |
| Acessibilidade | 5 | Lighthouse alto, mas falhas em skip-link/target-size |
| Responsividade | 2 | Quebra grave no shell autenticado mobile |
| Reuso/Design System | 3 | Primitives existem, adocao parcial e sem governanca |
| Engenharia Frontend | 5 | Lint/type/build/test verdes, porem paginas monoliticas |
| Integracao Front/Back | 5 | Funciona, com redundancias e workarounds frageis |
| Arquitetura | 4 | Mistura produto/debug/marketing e estado global excessivo |
| Performance percebida | 2 | LCP/TBT ruins em rotas criticas |

## 3. Evidencias Tecnicas (objetivas)

### 3.1 Checks de qualidade
- `npm run lint`: OK (0 warnings/0 errors)
- `npm run type-check`: OK
- `npm run build`: OK
- `npx jest --runInBand`: 12 suites, 45 testes, tudo passando

### 3.2 Performance (Lighthouse mobile)
Arquivos: `output/lighthouse/*.json`

- `/login`: perf 18, LCP 15.8s, TBT 18.7s, CLS 0.173
- `/chat`: perf 28, LCP 9.2s, TBT 2.7s
- `/settings/analytics`: perf 25, LCP 11.8s, TBT 5.1s
- `/`: perf 35, LCP 8.7s, TBT 2.0s
- `/library`: perf 49, LCP 8.6s, TBT 0.35s

### 3.3 Acessibilidade (Lighthouse)
- Falha recorrente `skip-link` em todas as rotas auditadas.
- Falha recorrente `target-size` em `/login`, `/chat`, `/library`, `/settings/analytics`.

### 3.4 Responsividade (evidencia visual)
Arquivos:
- `output/playwright/chat-mobile.png`
- `output/playwright/analytics-mobile.png`

Resultado: conteudo autenticado comprimido em faixa estreita lateral, com perda real de legibilidade e uso.

### 3.5 Sinais de entropia no codigo
- `46` tokens unicos `text-[...]` no frontend.
- `11` tokens unicos `rounded-[...]`.
- `47` ocorrencias de `<button>` em app/components app, com muitas fora das primitives.
- `10` ocorrencias de `<select>` fora de primitives em paginas criticas.

### 3.6 Evidencia de acoplamento/duplicacao
- `useAuthStore.getState().fetchUser()` no modulo + `fetchUser()` em varias paginas (`/`, `/chat`, `/welcome`).
- `settings/analytics` monta endpoint e faz `replace(...)` para adequar URL antes de chamar `api.get`.

## 4. Problemas por Severidade

### P0
1. Shell autenticado quebra a experiencia mobile.
2. Navegacao e conteudo competem no viewport pequeno (drawer + bottom nav + layout principal).

### P1
3. Performance percebida abaixo do minimo para rotas criticas.
4. Design system nao governa implementacao.
5. Linguagem de marca e interface fragmentada.
6. Analytics com semantica de ferramenta interna em rota de produto.
7. Acessibilidade parcial (skip-link/target-size) com impacto real de uso.
8. Bootstrap de auth redundante (duplicacao de chamadas e estados).
9. Primitives de formulario/acao contornadas em fluxos importantes.

### P2
10. Landing/home com excesso de densidade e hierarquia confusa.
11. Workspace sem gramatica unica de pagina (catalogo/dashboard/form/insights).
12. Integracao de analytics com workaround fragil na propria tela.

### P3
13. Polimento basico faltando (`/favicon.ico` 404 em runtime).

## 5. Analise por Dominio

### Identidade visual
Direcao existe, identidade nao. A interface alterna entre marketing, debug, editorial e operacao sem uma mesma gramatica.

### Tipografia
Escolha de fonte boa, aplicacao ruim. Escala fragmentada, microfontes demais e excesso de labels em uppercase/tracking.

### Layout/grid/espacamento
Estrutura macro do shell e boa em desktop, mas pages internas variam densidade sem contrato visual comum. Mobile autenticado quebra.

### Componentes
Biblioteca de UI existe, adocao parcial. Pags criticas ainda usam controles crus e estilos ad hoc.

### Navegacao/IA
Menu funciona, semantica nao. `/settings/analytics` deveria ser split entre insight de produto e debug interno.

### Formularios/feedback
Auth melhor que restante. Em chat/welcome/analytics faltam contracts consistentes de field/state/focus.

### Responsividade
Publico: aceitavel. Autenticado: falha severa em mobile.

### Acessibilidade WCAG
Base boa, execucao incompleta. Falhas em target-size e skip-link impedem chamar de WCAG 2.2 AA robusto.

### Reuso/design system
Infraestrutura pronta, governanca ausente. Sem regra de adocao, entropia cresce a cada tela.

### Frontend engineering
Build/testes ok, mas arquivos de rota grandes demais (`chat` ~571 linhas, `analytics` ~1126 linhas).

### Integracao backend
Funciona, porem com redundancia de `auth/me` e acoplamento de URL na propria tela de analytics.

### Arquitetura
Mistura de responsabilidades entre rotas (marketing/produto/debug), stores e shell.

### Performance
Nao alinhada com promessa premium: TBT/LCP altos em paginas centrais.

## 6. O que Remover / Refatorar / Padronizar / Reconstruir

### Remover
- Copy com benchmark leakage e linguagem interna.
- Workaround de endpoint dentro da tela de analytics.
- Controles crus em fluxos criticos onde ha primitive.

### Refatorar
- `frontend/src/components/app/app-shell.tsx`
- `frontend/src/app/chat/page.tsx`
- `frontend/src/app/settings/analytics/page.tsx`
- `frontend/src/app/page.tsx`
- `frontend/src/stores/auth-store.ts`

### Padronizar
- Escalas de tipografia/spacing/radius/states.
- Content design e glossario de produto.
- Blueprints de pagina (catalogo, dashboard, formulario, insights).

### Reconstruir
- Shell mobile autenticado.
- Analytics orientado a produto (separado de debug).
- Entry experience de home autenticado.

## 7. Proposta Minima de Design System

Principios:
- Claridade > decoracao
- Densidade controlada
- Uma lingua de interface
- Mobile recomposto (nao encolhido)
- A11y por padrao

Tokens minimos:
- Cores semanticas (`canvas`, `surface-*`, `text-*`, `brand`, `success`, `warning`, `danger`)
- Spacing (`4, 8, 12, 16, 24, 32, 48, 64`)
- Radius (`8, 12, 16, 24, pill`)
- Motion (`fast`, `base`, `slow`)

Biblioteca minima:
- `Button`, `IconButton`, `Field`, `Input`, `Textarea`, `Select`, `Switch`, `Tabs`
- `PageHeader`, `SectionCard`, `MetricCard`, `FilterBar`, `EmptyState`, `ActionStrip`
- `DrawerShell`, `MobileBottomNav`, `DashboardPanel`

Governanca:
- owner de DS
- guideline versionado no repo
- rule/lint para limitar `text-[...]`, `rounded-[...]`, `tracking-[...]`
- PR sem justificativa de novo token nao passa

## 8. Roadmap em 4 Ondas

### Onda 1 (0-7 dias)
Objetivo: estabilizar P0.
- Rebuild shell mobile autenticado.
- Corrigir target size em acoes criticas.
- Centralizar bootstrap de auth.
- Corrigir polimento global (favicon/metadata).

### Onda 2 (2-3 semanas)
Objetivo: fundacao DS + normalizacao critica.
- Fechar escala tipografica/radius/spacing.
- Criar primitives de field/filter/header.
- Migrar auth/chat/settings/analytics.
- Split analytics produto vs debug.

### Onda 3 (4-6 semanas)
Objetivo: consolidacao de reuso + a11y + responsividade ampla.
- Decompor chat em subcomponentes.
- Aplicar blueprints de pagina.
- Auditar WCAG 2.2 AA em fluxos criticos.

### Onda 4 (7-10 semanas)
Objetivo: excelencia operacional.
- Budgets de performance por rota.
- Visual regression + E2E criticos.
- Governanca em CI para prevenir recaida.

## 9. Backlog Priorizado (20 tickets)

1. Rebuild do shell mobile autenticado (P0)
2. Montagem condicional do drawer mobile (P0)
3. Correcao de touch targets (P1)
4. Correcao do skip link (P1)
5. Centralizacao do bootstrap de sessao (P1)
6. Criacao de `Field`, `SelectField`, `TextareaField` (P1)
7. Split de analytics entre produto e debug (P1)
8. Servico tipado para analytics (P2)
9. Escala tipografica oficial (P1)
10. Banimento de valores arbitrarios fora de allowlist (P1)
11. Content pass completo em PT-BR (P1)
12. Remocao de benchmark leakage da UI (P1)
13. Simplificacao da landing acima da dobra (P2)
14. Separacao entre landing publica e product home (P2)
15. Decomposicao do chat em subcomponentes (P1)
16. Lazy-load das dependencias pesadas do chat (P1)
17. Blueprints de pagina (P2)
18. Normalizacao visual de library/prompts/billing/settings (P2)
19. Adicao de favicon e revisao de metadata (P3)
20. Pipeline de visual regression/keyboard smoke/perf budget (P1)

## 10. Definicao de Pronto

O frontend so esta recuperado quando:
- identidade visual e textual unicas entre rotas;
- responsividade aprovada em 360/390/412/768/1024/1440;
- WCAG 2.2 AA em fluxos principais;
- estados completos (`loading`, `empty`, `error`, `success`, `disabled`);
- reuso real de primitives (sem controles crus em fluxo critico);
- performance sob budget em rotas centrais;
- regressao visual e de interacao bloqueada no CI.

## 11. Resumo Final

### Lideranca
O frontend melhorou tecnicamente, mas ainda nao atingiu nivel premium por falhas sistemicas em mobile, consistencia, acessibilidade pratica e performance. A recuperacao exige programa de 4 ondas, nao rodada cosmetica.

### Engenharia + Design
A base existe (tokens/primitives/build/test), mas falta governanca. Sem padrao obrigatorio, cada pagina reabre excecoes. A prioridade e: shell mobile, fundacao DS, split produto/debug em analytics, decomposicao de rotas monoliticas e qualidade operacional em CI.
