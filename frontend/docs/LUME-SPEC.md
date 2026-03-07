# LUME — Especificacao Completa da Plataforma

**Versao:** 1.0.0
**Data:** 2026-03-07
**Classificacao:** Confidencial
**Idioma:** Portugues do Brasil

> **Lume** e uma plataforma de agregacao de IA (AI Gateway / AI Orchestration Platform) que permite ao usuario acessar multiplas solucoes de inteligencia artificial — gratuitas ou pagas — dentro de uma unica interface premium e unificada.

**Legenda de confianca:**
- `[OBSERVADO]` — Evidencia extraida diretamente do codebase existente ou da analise visual da solucao de referencia
- `[INFERIDO]` — Deduzido com alta confianca a partir de padroes observados, comportamentos de UI ou convencoes de mercado
- `[PROPOSTO]` — Recomendacao tecnica ou de produto proposta pela equipe de arquitetura

---

# SECAO 1 — RESUMO EXECUTIVO

## 1.1 Sobre a Solucao de Referencia

A solucao de referencia analisada (Claude.ai, da Anthropic) e uma plataforma de chat com inteligencia artificial premium, focada em conversas de alta qualidade com um unico provedor (Anthropic). `[OBSERVADO]`

### Dominios funcionais identificados:

| Dominio | Descricao | Confianca |
|---------|-----------|-----------|
| Chat conversacional | Interface de chat com streaming em tempo real, multiplos modelos do mesmo provedor, historico persistente | `[OBSERVADO]` |
| Gestao de conversas | Criacao, renomeacao, fixacao (pin), exclusao, busca e organizacao temporal de conversas | `[OBSERVADO]` |
| Projects | Agrupamento de conversas em projetos com contexto compartilhado, arquivos e instrucoes | `[INFERIDO]` |
| Artifacts | Geracao de conteudo rico (codigo, documentos, diagramas) em painel lateral interativo | `[INFERIDO]` |
| Code execution | Execucao de codigo gerado em sandbox (JavaScript, Python) | `[INFERIDO]` |
| Model selection | Selecao entre modelos do provedor (Haiku, Sonnet, Opus) com diferentes capacidades | `[OBSERVADO]` |
| Billing/Subscription | Planos Free e Pro, limites de uso, upgrade/downgrade | `[INFERIDO]` |
| Teams/Organizations | Workspaces colaborativos, membros, permissoes, billing centralizado | `[INFERIDO]` |
| Settings/Preferences | Tema (claro/escuro/sistema), idioma, notificacoes, privacidade, dados | `[OBSERVADO]` |
| Authentication | Login, registro, OAuth (Google), recuperacao de senha, verificacao de email | `[INFERIDO]` |

### Diferenciais de UX observados:

1. **Streaming de alta fidelidade** — Respostas aparecem caractere por caractere com latencia minima `[OBSERVADO]`
2. **Greeting personalizado** — Saudacao baseada no horario + nome do usuario com fonte ultra-light (weight 290) `[OBSERVADO]`
3. **Sparkle icon** — Icone decorativo tipo asterisco em cor terracota como identidade visual `[OBSERVADO]`
4. **Quick action chips** — Botoes de acao rapida (Escrever, Aprender, Codigo, etc.) abaixo do input `[OBSERVADO]`
5. **Input box com shadow system** — Tres estados de sombra (default/hover/focus) para o campo de entrada `[OBSERVADO]`
6. **Sidebar compacta** — Navegacao vertical com icones + labels em 12px, agrupamento temporal automatico `[OBSERVADO]`
7. **Model selector inline** — Seletor de modelo integrado na barra inferior do input, nao como elemento separado `[OBSERVADO]`
8. **Tema adaptativo** — Sistema de design tokens HSL com transicoes suaves entre temas `[OBSERVADO]`

## 1.2 Posicionamento da Lume

A Lume se diferencia fundamentalmente da solucao de referencia por ser um **agregador multi-provedor de IA**:

| Aspecto | Solucao de Referencia | Lume |
|---------|----------------------|------|
| Provedores | Unico (Anthropic) | 18+ provedores (OpenAI, Anthropic, Google, Mistral, Cohere, DeepSeek, Groq, xAI, etc.) |
| Modelos | 3-4 do mesmo provedor | 50+ modelos de multiplos provedores |
| Modalidades | Texto + Visao | Texto, Visao, Imagem, Audio, Video, Embeddings, Search, Code |
| Roteamento | Fixo | Inteligente (custo, latencia, qualidade, disponibilidade, fallback) |
| BYOK | N/A | Bring Your Own Key suportado |
| Comparacao | N/A | Comparacao lado a lado entre modelos/provedores |
| Billing | Plano fixo | Pay-per-use + planos, metering granular por provedor |
| Observabilidade | Limitada | Dashboard completo por provedor/modelo (latencia, erro, custo) |

### Dominios funcionais da Lume:

1. **AI Gateway** — Camada de abstracao unificada para multiplos provedores de IA `[PROPOSTO]`
2. **Chat Conversacional** — Interface de chat multi-modelo com streaming `[OBSERVADO]`
3. **Gestao de Conversas** — CRUD completo com organizacao temporal `[OBSERVADO]`
4. **Catalogo de Provedores/Modelos** — Registry de provedores, modelos e capacidades `[PROPOSTO]`
5. **Playground** — Ambiente de teste unificado para prompts e chamadas `[PROPOSTO]`
6. **Gestao de Prompts** — Templates, presets, versionamento `[PROPOSTO]`
7. **Credenciais e BYOK** — Gestao de chaves da plataforma e do cliente `[PROPOSTO]`
8. **Billing e Metering** — Consumo por provedor, planos, cotas, alertas `[OBSERVADO parcial]`
9. **Projects** — Agrupamento de conversas com contexto compartilhado `[INFERIDO]`
10. **Teams/Organizations** — Multi-tenant, colaboracao, permissoes `[INFERIDO]`
11. **Codex (Task Automation)** — Automacao de tarefas de codigo em cloud `[OBSERVADO]`
12. **Observabilidade** — Dashboard de metricas por provedor/modelo `[PROPOSTO]`
13. **Compliance e Auditoria** — LGPD, logs, exportacao de dados `[OBSERVADO parcial]`
14. **Integracoes** — GitHub, Slack, Linear, webhooks `[OBSERVADO]`
15. **Admin** — Configuracoes globais, feature flags, gestao de usuarios `[OBSERVADO parcial]`

## 1.3 O que a Lume precisa para atingir equivalencia funcional + qualidade premium

1. **Chat multi-modelo com streaming SSE real** (atualmente simula streaming no frontend) `[OBSERVADO]`
2. **Persistencia server-side de conversas** (atualmente localStorage apenas) `[OBSERVADO]`
3. **Catalogo dinamico de provedores/modelos** com health check e status em tempo real `[PROPOSTO]`
4. **Roteamento inteligente** com fallback automatico ja implementado, expandir para roteamento por custo/latencia/qualidade `[OBSERVADO parcial]`
5. **BYOK** — Permitir que usuarios tragam suas proprias chaves de API `[PROPOSTO]`
6. **Playground unificado** para comparacao lado a lado `[PROPOSTO]`
7. **Billing real** com Stripe integration (estrutura de billing existe no backend mas nao esta conectada) `[OBSERVADO]`
8. **Teams e organizations** com multi-tenant real (schema existe, implementacao parcial) `[OBSERVADO parcial]`
9. **Design system original** com identidade propria (nao clonar Claude.ai) `[PROPOSTO]`
10. **Observabilidade por provedor** — Dashboard com latencia, erro, custo, disponibilidade `[PROPOSTO]`

---

# SECAO 2 — MAPA COMPLETO DA NAVEGACAO

## 2.1 Visao Geral do Sitemap

```
LUME PLATFORM
|
+-- AREAS PUBLICAS
|   +-- / (Landing Page → redirect /chat)
|   +-- /home (redirect → /chat)
|   +-- /welcome (Onboarding / Welcome flow)
|   +-- /oauth/github/callback (OAuth callback)
|
+-- AREA AUTENTICADA — CHAT
|   +-- /chat (Chat principal — empty state ou conversa ativa)
|   |   +-- Sidebar (navegacao principal)
|   |   |   +-- Logo "Lume" (link → novo chat)
|   |   |   +-- [+] Novo bate-papo
|   |   |   +-- [Busca] Procurar conversas
|   |   |   +-- [Sliders] Personalizar (ciclo de tema)
|   |   |   +-- --- separador ---
|   |   |   +-- [Chat] Conversas
|   |   |   +-- [Folder] Projetos
|   |   |   +-- [Grid] Artefatos
|   |   |   +-- [Code] Codigo
|   |   |   +-- --- Recentes ---
|   |   |   +-- Lista de conversas (agrupadas por data)
|   |   |   +-- --- footer ---
|   |   |   +-- Avatar + Nome + Plano
|   |   |
|   |   +-- Content Area
|   |       +-- Empty State (greeting + input + quick actions)
|   |       +-- Active Chat (mensagens + input)
|   |       +-- Model Selector (dropdown no input)
|   |       +-- Streaming Indicator
|   |
|   +-- /library (Biblioteca de conversas)
|   |   +-- Grid view / List view toggle
|   |   +-- Filtros e busca
|   |   +-- Cards de conversas
|   |
|   +-- /prompts (Templates de prompts)
|   |   +-- Lista de prompts
|   |   +-- Criar/editar prompt
|   |
|   +-- /settings (Configuracoes do usuario)
|   |   +-- /settings/analytics (Dashboard de analytics)
|   |   +-- /settings/analytics/debug (Diagnostico de analytics)
|   |
|   +-- /billing (Billing e assinatura)
|       +-- Plano atual
|       +-- Historico de uso
|       +-- Upgrade/downgrade
|
+-- AREA AUTENTICADA — CODEX (Task Automation)
|   +-- /codex (Dashboard de tarefas)
|   +-- /codex/get-started (Onboarding Codex)
|   +-- /codex/onboarding (Onboarding detalhado)
|   +-- /codex/shortcuts (Atalhos e comandos)
|   +-- /codex/tasks (Lista de tarefas)
|   |   +-- /codex/tasks/[taskId] (Detalhe da tarefa)
|   |       +-- /codex/tasks/[taskId]/logs (Logs de execucao)
|   |       +-- /codex/tasks/[taskId]/diff (Diff de codigo)
|   |       +-- /codex/tasks/[taskId]/tests (Resultados de teste)
|   |       +-- /codex/tasks/[taskId]/artifacts (Artefatos gerados)
|   |       +-- /codex/tasks/[taskId]/pull-request (Gestao de PR)
|   |
|   +-- /codex/settings (Configuracoes Codex)
|       +-- /codex/settings/analytics (Analytics de uso)
|       +-- /codex/settings/apireference (Documentacao API)
|       +-- /codex/settings/code-review (Politicas de review)
|       +-- /codex/settings/connectors (Integracoes)
|       +-- /codex/settings/environments (Ambientes)
|       |   +-- /codex/settings/environments/new
|       |   +-- /codex/settings/environments/[environmentId]
|       +-- /codex/settings/managed-configs (Configs gerenciadas)
|       +-- /codex/settings/usage (Uso e consumo)
|           +-- /codex/settings/usage/credits (Creditos)
|
+-- AREA ADMIN
|   +-- /admin/settings (Configuracoes administrativas)
|
+-- API ROUTES (Next.js BFF)
    +-- /api/auth/* (login, logout, session)
    +-- /api/tasks/* (CRUD de tarefas + subrotas)
    +-- /api/repositories/* (repos e branches)
    +-- /api/environments/* (CRUD de ambientes)
    +-- /api/code-review/* (politicas de review)
    +-- /api/integrations/* (GitHub, Slack, Linear)
    +-- /api/webhooks/* (webhooks recebidos)
    +-- /api/oauth/* (fluxo OAuth)
    +-- /api/billing/* (planos, uso)
    +-- /api/credits/* (saldo e compra)
    +-- /api/analytics/* (dados de analytics)
    +-- /api/compliance/* (exportacoes)
    +-- /api/health/* (health check)
    +-- /api/models/* (capacidades de modelos)
    +-- /api/managed-configs/* (configs)
    +-- /api/usage/* (estatisticas de uso)
```

## 2.2 Fluxos de Navegacao Principais

### Fluxo 1: Primeiro acesso (First Run)
```
/ → /welcome → (onboarding) → /chat (empty state)
```
`[OBSERVADO]`

### Fluxo 2: Chat padrao
```
/chat → Empty State → digitar mensagem → Enter → conversa criada → mensagens fluem → sidebar atualiza
```
`[OBSERVADO]`

### Fluxo 3: Trocar modelo
```
/chat → clicar Model Selector → escolher modelo → proximo envio usa modelo selecionado
```
`[OBSERVADO]`

### Fluxo 4: Gerenciar conversa
```
Sidebar → hover conversa → menu contextual → Renomear | Fixar | Limpar | Excluir
```
`[OBSERVADO]`

### Fluxo 5: Buscar conversa
```
Sidebar → clicar Procurar → campo de busca expande → digitar → lista filtra em tempo real
```
`[OBSERVADO]`

### Fluxo 6: Codex task
```
/codex → Task Composer → criar tarefa → acompanhar logs → ver diff → criar PR
```
`[OBSERVADO]`

### Fluxo 7: Configuracoes
```
/settings → alterar preferencias → salvar
/billing → ver plano → upgrade/downgrade
```
`[OBSERVADO]`

## 2.3 Pontos de Entrada e Saida

| Ponto de Entrada | Destino | Tipo |
|-----------------|---------|------|
| URL direta | Qualquer rota | Publico/Auth |
| Logo "Lume" | /chat (novo) | Auth |
| Botao "+ Novo bate-papo" | /chat (novo) | Auth |
| Quick action chip | /chat com prompt pre-preenchido | Auth |
| Sidebar conversa item | /chat com conversa ativa | Auth |
| Library card | /chat com conversa ativa | Auth |

| Ponto de Saida | Acao | Resultado |
|---------------|------|-----------|
| Logout | POST /api/auth/logout | Redireciona para login |
| Fechar aba | N/A | Estado preservado em localStorage |
| Sessao expirada | 401 interceptado | Tenta refresh, se falhar redireciona login |

---

# SECAO 3 — INVENTARIO COMPLETO DO FRONTEND

## 3.1 Modulos e Telas

### MOD-001: Chat Principal

| Campo | Valor |
|-------|-------|
| **ID** | MOD-001 |
| **Nome** | Chat Principal |
| **Objetivo** | Permitir conversas com modelos de IA em tempo real |
| **Rota** | `/chat` |
| **Atores** | Usuario autenticado |
| **Permissoes** | `authenticated` |
| **Confianca** | `[OBSERVADO]` |

**Componentes presentes:**
- `ChatLayout` — Layout com sidebar colapsavel + area de conteudo
- `ChatSidebar` — Navegacao lateral com lista de conversas
- `EmptyState` — Estado inicial com greeting, input e quick actions
- `ChatMessages` — Area de exibicao de mensagens
- `ChatInput` — Campo de entrada com model selector e controles
- `MessageBubble` — Bolha de mensagem individual (user/assistant/error)
- `MessageMarkdown` — Renderizacao de markdown nas respostas
- `MessageActions` — Botoes de acao (copiar, retry, etc.)
- `CodeBlock` — Bloco de codigo com syntax highlighting
- `ModelSelector` — Dropdown de selecao de modelo
- `StreamingIndicator` — Indicador de streaming ativo
- `ConversationList` — Lista de conversas com agrupamento temporal
- `ConversationItem` — Item individual na lista de conversas

**Acoes disponiveis:**
1. Enviar mensagem (Enter ou botao)
2. Selecionar modelo (dropdown)
3. Parar geracao (botao stop)
4. Criar nova conversa (sidebar ou top bar)
5. Buscar conversas (sidebar search)
6. Selecionar conversa (sidebar click)
7. Renomear conversa (menu contextual)
8. Fixar/desfixar conversa (menu contextual)
9. Limpar mensagens (menu contextual)
10. Excluir conversa (menu contextual)
11. Alternar tema (sidebar "Personalizar")
12. Colapsar/expandir sidebar (botao toggle ou Ctrl+B)
13. Copiar mensagem (message actions)
14. Retry mensagem (message actions)
15. Copiar bloco de codigo (code block header)

**Estados de interface:**

| Estado | Descricao | Trigger |
|--------|-----------|---------|
| Empty state | Greeting + input + chips, nenhuma conversa ativa | Nenhuma conversa selecionada |
| Active chat | Mensagens + input na parte inferior | Conversa selecionada |
| Sending | Input desabilitado, indicador ativo | Mensagem enviada |
| Streaming | Texto aparecendo progressivamente | Resposta do modelo |
| Error | Mensagem de erro vermelha na bolha | Falha na API |
| Sidebar collapsed | Sidebar oculta, botoes toggle e new-chat visiveis | Toggle ou Ctrl+B |
| Sidebar mobile | Overlay com sidebar deslizante | Tela < 768px |
| Search active | Campo de busca expandido | Clicar "Procurar" |
| No conversations | Mensagem "Nenhuma conversa ainda" | Zero conversas |
| Search no results | Mensagem "Nenhuma conversa encontrada" | Busca sem resultados |

**Microinteracoes:**
- `active:scale-[1.0]` nos botoes da sidebar (feedback de clique) `[OBSERVADO]`
- `active:scale-95` no botao toggle `[OBSERVADO]`
- `active:scale-[0.985]` na area do usuario `[OBSERVADO]`
- Shadow transition no input container (3 estados) `[OBSERVADO]`
- Textarea auto-resize ate 200px `[OBSERVADO]`
- Botao send muda de cinza para primario quando ha texto `[OBSERVADO]`
- Conversas agrupadas por data (Hoje, Ontem, Ultimos 7 dias, Ultimos 30 dias, Mais antigos) `[OBSERVADO]`

**Validacoes:**
- Input nao pode ser enviado vazio (botao desabilitado) `[OBSERVADO]`
- Input nao pode ser enviado durante streaming (isSending check) `[OBSERVADO]`
- Prompt maximo: 5000 caracteres (guardrail backend) `[OBSERVADO]`

**Dependencias:**
- `chat-store` (Zustand) — estado de conversas, mensagens, modelo
- `auth-store` (Zustand) — usuario autenticado
- `theme-store` (Zustand) — preferencia de tema
- `POST /api/v1/ai/chat` — backend de inferencia
- `model-catalog.ts` — catalogo de modelos disponíveis

**Comportamento responsivo:**
- Desktop (>= 768px): Sidebar fixa com 288px + content area flexivel `[OBSERVADO]`
- Mobile (< 768px): Sidebar oculta, acessivel via overlay + botao hamburger `[OBSERVADO]`

**Telemetria recomendada:** `[PROPOSTO]`
- `chat_send_start` — inicio de envio com modelo e tamanho do prompt `[OBSERVADO]`
- `chat_send_success` — envio bem-sucedido com latencia e modelo `[OBSERVADO]`
- `chat_send_error` — erro no envio com mensagem `[OBSERVADO]`
- `chat_change_model` — troca de modelo `[OBSERVADO]`
- `chat_stop_generation` — usuario parou a geracao `[OBSERVADO]`
- `chat_new_conversation` — nova conversa criada `[PROPOSTO]`
- `chat_delete_conversation` — conversa excluida `[PROPOSTO]`
- `chat_search` — busca realizada `[PROPOSTO]`

---

### MOD-002: Empty State

| Campo | Valor |
|-------|-------|
| **ID** | MOD-002 |
| **Nome** | Empty State |
| **Objetivo** | Primeira impressao e ponto de entrada para nova conversa |
| **Rota** | `/chat` (quando nenhuma conversa ativa) |
| **Atores** | Usuario autenticado |
| **Permissoes** | `authenticated` |
| **Confianca** | `[OBSERVADO]` |

**Componentes presentes:**
- `SparkleIcon` — Icone SVG decorativo (asterisco 8 pontas) em cor accent
- Greeting text — "Bom dia/Boa tarde/Boa noite, {firstName}"
- Textarea — Campo de entrada com placeholder "Como posso ajudar voce hoje?"
- Botao Plus — Mais opcoes (a implementar: upload, web search, etc.)
- `ModelSelector` — Dropdown de modelo na barra inferior
- Botao Send — ArrowUp, ativado quando ha texto
- Quick action chips — 5 botoes: Escrever, Aprender, Codigo, Assuntos pessoais, Escolha do Lume

**Estados de interface:**
- Default: greeting + input vazio + chips
- Typing: input com texto, botao send ativo (primario)
- Sending: input desabilitado, transicao para chat ativo

---

### MOD-003: Chat Sidebar

| Campo | Valor |
|-------|-------|
| **ID** | MOD-003 |
| **Nome** | Chat Sidebar |
| **Objetivo** | Navegacao principal, gestao de conversas, acesso rapido |
| **Rota** | Componente lateral em `/chat` |
| **Atores** | Usuario autenticado |
| **Permissoes** | `authenticated` |
| **Confianca** | `[OBSERVADO]` |

**Componentes presentes:**
- Logo "Lume" (text link, font-semibold, 18px)
- Botao toggle sidebar (icone painel)
- Nav primaria: + Novo bate-papo, Busca, Personalizar
- Separador horizontal
- Nav secundaria: Conversas, Projetos, Artefatos, Codigo
- Label "Recentes"
- `ConversationList` com `ConversationItem`
- Footer: Avatar + nome + "Plano Pro" + icones Download/ChevronDown

**Acoes disponiveis:**
1. Clicar logo → nova conversa
2. Toggle sidebar → colapsar/expandir
3. Novo bate-papo → criar conversa
4. Procurar → expandir campo de busca
5. Personalizar → ciclar tema (light → dark → system)
6. Clicar conversa → selecionar
7. Menu contextual conversa → renomear/fixar/limpar/excluir

---

### MOD-004: Conversation Item

| Campo | Valor |
|-------|-------|
| **ID** | MOD-004 |
| **Nome** | Conversation Item |
| **Objetivo** | Representar uma conversa na lista lateral |
| **Rota** | Componente dentro de `ConversationList` |
| **Atores** | Usuario autenticado |
| **Confianca** | `[OBSERVADO]` |

**Estados:**
- Default: titulo truncado, hover mostra menu
- Active: background destacado (`surface-active`)
- Hover: background sutil, menu contextual visivel
- Pinned: icone de pin visivel `[OBSERVADO no store]`
- Editing: campo de texto para renomear `[OBSERVADO no store]`

**Menu contextual:**
- Renomear (icone Pencil)
- Excluir (icone Trash2, com confirmacao)

---

### MOD-005: Library

| Campo | Valor |
|-------|-------|
| **ID** | MOD-005 |
| **Nome** | Biblioteca de Conversas |
| **Objetivo** | Visualizar e gerenciar todas as conversas em formato grid/lista |
| **Rota** | `/library` |
| **Atores** | Usuario autenticado |
| **Permissoes** | `authenticated` |
| **Confianca** | `[OBSERVADO]` |

**Componentes presentes:**
- Header com titulo "Biblioteca"
- Toggle grid/list view
- Campo de busca
- Filtros (modelo, data)
- Grid de cards / Lista de items
- Botao "Abrir" em cada item

**Acoes:**
1. Alternar visualizacao (grid/list)
2. Buscar conversas
3. Filtrar por modelo/data
4. Abrir conversa → navegar para /chat

---

### MOD-006: Prompts

| Campo | Valor |
|-------|-------|
| **ID** | MOD-006 |
| **Nome** | Templates de Prompts |
| **Objetivo** | Gerenciar templates e presets de prompts reutilizaveis |
| **Rota** | `/prompts` |
| **Atores** | Usuario autenticado |
| **Permissoes** | `authenticated` |
| **Confianca** | `[OBSERVADO]` |

---

### MOD-007: Settings

| Campo | Valor |
|-------|-------|
| **ID** | MOD-007 |
| **Nome** | Configuracoes |
| **Objetivo** | Gerenciar preferencias do usuario |
| **Rota** | `/settings` |
| **Atores** | Usuario autenticado |
| **Permissoes** | `authenticated` |
| **Confianca** | `[OBSERVADO]` |

**Sub-telas:**
- `/settings/analytics` — Dashboard de analytics com metricas de uso
- `/settings/analytics/debug` — Diagnostico de eventos de analytics

---

### MOD-008: Billing

| Campo | Valor |
|-------|-------|
| **ID** | MOD-008 |
| **Nome** | Billing e Assinatura |
| **Objetivo** | Gerenciar plano, ver consumo, comprar creditos |
| **Rota** | `/billing` |
| **Atores** | Usuario autenticado |
| **Permissoes** | `authenticated` |
| **Confianca** | `[OBSERVADO]` |

---

### MOD-009: Welcome/Onboarding

| Campo | Valor |
|-------|-------|
| **ID** | MOD-009 |
| **Nome** | Onboarding |
| **Objetivo** | Guiar novos usuarios no primeiro acesso |
| **Rota** | `/welcome` |
| **Atores** | Novo usuario |
| **Permissoes** | `authenticated` (first run) |
| **Confianca** | `[OBSERVADO]` |

---

### MOD-010: Codex Dashboard

| Campo | Valor |
|-------|-------|
| **ID** | MOD-010 |
| **Nome** | Codex — Automacao de Tarefas |
| **Objetivo** | Dashboard para criar e gerenciar tarefas de automacao de codigo |
| **Rota** | `/codex` |
| **Atores** | Usuario autenticado |
| **Permissoes** | `authenticated` |
| **Confianca** | `[OBSERVADO]` |

**Sub-telas:**
- `/codex/get-started` — Onboarding Codex
- `/codex/onboarding` — Onboarding detalhado
- `/codex/shortcuts` — Atalhos e comandos
- `/codex/tasks` — Lista de tarefas
- `/codex/tasks/[taskId]` — Detalhe com sub-rotas (logs, diff, tests, artifacts, PR)

**Componentes:**
- `CodexShell` — Container layout
- `TaskComposer` — Formulario de criacao de tarefa
- `TaskList` — Lista filtavel de tarefas
- `TaskTimeline` — Timeline visual de status
- `DiffViewer` — Visualizador de diff de codigo
- `LiveLogViewer` — Viewer de logs em tempo real
- `PullRequestPanel` — Painel de gestao de PR

---

### MOD-011: Codex Settings

| Campo | Valor |
|-------|-------|
| **ID** | MOD-011 |
| **Nome** | Codex Settings |
| **Objetivo** | Configurar ambientes, integracoes, politicas e uso do Codex |
| **Rota** | `/codex/settings` |
| **Atores** | Usuario autenticado / Admin |
| **Permissoes** | `authenticated`, `admin` para algumas sub-rotas |
| **Confianca** | `[OBSERVADO]` |

**Sub-telas:**
- `/codex/settings/analytics` — Uso e metricas
- `/codex/settings/apireference` — Documentacao da API
- `/codex/settings/code-review` — Politicas de code review
- `/codex/settings/connectors` — Integracoes (GitHub, Slack, Linear)
- `/codex/settings/environments` — CRUD de ambientes de execucao
- `/codex/settings/managed-configs` — Configuracoes gerenciadas
- `/codex/settings/usage` — Uso e creditos

---

### MOD-012: Admin Settings

| Campo | Valor |
|-------|-------|
| **ID** | MOD-012 |
| **Nome** | Admin Settings |
| **Objetivo** | Configuracoes administrativas da plataforma |
| **Rota** | `/admin/settings` |
| **Atores** | Super Admin |
| **Permissoes** | `super_admin` |
| **Confianca** | `[OBSERVADO]` |

---

### MOD-013: Error Pages

| Campo | Valor |
|-------|-------|
| **ID** | MOD-013 |
| **Nome** | Paginas de Erro |
| **Objetivo** | Tratar erros de forma amigavel |
| **Rotas** | `/error`, `/error/page`, `not-found` (404), `error.tsx` (boundary) |
| **Atores** | Qualquer usuario |
| **Confianca** | `[OBSERVADO]` |

**Estados:**
- 404 Not Found — Pagina nao encontrada, com link "Voltar ao inicio"
- 500 Error — Erro interno, com botao de retry e link para inicio
- Error boundary — Captura erros de React, exibe mensagem e botao de retry

---

## 3.2 Componentes UI Compartilhados

| ID | Componente | Arquivo | Objetivo |
|----|-----------|---------|----------|
| UI-001 | Button | `components/ui/button.tsx` | Botao com variantes (primary, secondary, ghost, danger) e tamanhos (sm, md, lg) |
| UI-002 | Input | `components/ui/input.tsx` | Campo de texto com label, erro e helper |
| UI-003 | Textarea | `components/ui/textarea.tsx` | Campo multi-linha |
| UI-004 | Avatar | `components/ui/avatar.tsx` | Avatar com iniciais ou imagem, tamanhos sm/md/lg |
| UI-005 | Badge | `components/ui/badge.tsx` | Badge de status com variantes |
| UI-006 | Modal | `components/ui/modal.tsx` | Dialog modal com overlay, focus trap |
| UI-007 | Dropdown | `components/ui/dropdown.tsx` | Menu dropdown com opcoes |
| UI-008 | Alert | `components/ui/alert.tsx` | Caixa de alerta (info/warning/error) |
| UI-009 | Tooltip | `components/ui/tooltip.tsx` | Tooltip posicional |
| UI-010 | Skeleton | `components/ui/skeleton.tsx` | Placeholder de loading |
| UI-011 | IconButton | `components/ui/icon-button.tsx` | Botao apenas com icone |
| UI-012 | FormField | `components/ui/form-field.tsx` | Wrapper de campo de formulario |
| UI-013 | ToastViewport | `components/ui/toast-viewport.tsx` | Container de notificacoes |
| UI-014 | CodeBlock (UI) | `components/ui/code-block.tsx` | Bloco de codigo generico |

## 3.3 Stores (Estado Global)

| ID | Store | Arquivo | Responsabilidade |
|----|-------|---------|------------------|
| ST-001 | AuthStore | `stores/auth-store.ts` | Usuario autenticado, isAuthenticated, isLoading, fetchUser() |
| ST-002 | ChatStore | `stores/chat-store.ts` | Conversas, mensagens, modelo selecionado, streaming, CRUD completo |
| ST-003 | ThemeStore | `stores/theme-store.ts` | Tema (light/dark/system), resolvedTheme, setTheme() |
| ST-004 | ToastStore | `stores/toast-store.ts` | Notificacoes (success/error/info), auto-dismiss 3.5s |

## 3.4 Bibliotecas e Utilitarios

| ID | Lib | Arquivo | Funcao |
|----|-----|---------|--------|
| LIB-001 | API Client | `lib/api.ts` | Axios com interceptors JWT, refresh token, error handling |
| LIB-002 | Analytics | `lib/analytics.ts` | Event tracking com queue local, auto-flush |
| LIB-003 | Model Catalog | `lib/model-catalog.ts` | Catalogo estatico de 14 modelos / 8 provedores |
| LIB-004 | CN | `lib/cn.ts` | Merge de classes Tailwind (clsx + tailwind-merge) |
| LIB-005 | Codex API | `lib/codex-api.ts` | Wrapper fetch para backend Codex completo |

---

# SECAO 4 — MATRIZ EXAUSTIVA DE FUNCIONALIDADES

## 4.1 Modulo: Autenticacao e Identidade

| ID | Modulo | Descricao | Ator | Gatilho | Fluxo Principal | Estados UI | Regra de Negocio | Backend | Endpoint | Servico | Eventos | Persistencia | Tabelas | Cache | Auditoria | Observabilidade | Testes | Prioridade | Confianca |
|----|--------|-----------|------|---------|-----------------|-----------|-----------------|---------|----------|---------|---------|-------------|---------|-------|-----------|----------------|--------|-----------|-----------|
| F-AUTH-001 | Auth | Registro de usuario com email/senha | Visitante | Clicar "Criar conta" | Preencher form → validar → criar user → enviar email verificacao → redirect login | form, loading, erro validacao, sucesso, email enviado | Email unico, senha min 8 chars, aceite termos obrigatorio | RegisterUserUseCase | POST /api/v1/auth/register | AuthService | UserRegisteredEvent | PostgreSQL | auth.users, auth.email_verification_tokens | N/A | Log de registro | Contador de registros, taxa de erro | Unit + Integration + E2E | P0 | `[OBSERVADO]` |
| F-AUTH-002 | Auth | Login com email/senha | Usuario | Clicar "Entrar" | Preencher email+senha → validar → gerar tokens → redirect /chat | form, loading, erro credencial, erro bloqueio, sucesso | Max 5 tentativas, bloqueio 30min, atualizar lastLogin | LoginUseCase | POST /api/v1/auth/login | AuthService | UserLoggedInEvent | PostgreSQL | auth.users, auth.sessions | Session em Redis | Log de login (IP, UA) | Logins/min, taxa de falha, bloqueios | Unit + Integration + E2E | P0 | `[OBSERVADO]` |
| F-AUTH-003 | Auth | Refresh de token JWT | Sistema | Token access expira (15min) | Interceptor detecta 401 → envia refresh → recebe novo access | transparente, erro (redirect login) | Refresh valido por 7 dias, single-use | RefreshTokenUseCase | POST /api/v1/auth/refresh | AuthService | TokenRefreshedEvent | PostgreSQL | auth.sessions | N/A | Log de refresh | Refreshes/min | Unit + Integration | P0 | `[OBSERVADO]` |
| F-AUTH-004 | Auth | Obter usuario atual | Sistema | Pagina carrega | GET /me → retorna user profile | loading, autenticado, nao autenticado | Token valido, user ativo | GetCurrentUserUseCase | GET /api/v1/auth/me | AuthService | N/A | PostgreSQL | auth.users | User cache Redis 5min | N/A | N/A | Unit + Integration | P0 | `[OBSERVADO]` |
| F-AUTH-005 | Auth | Logout | Usuario | Clicar "Sair" | POST logout → revogar session → limpar localStorage → redirect login | loading, sucesso | Revogar session no banco, limpar cookies | LogoutUseCase | POST /api/v1/auth/logout | AuthService | UserLoggedOutEvent | PostgreSQL | auth.sessions | Invalidar cache | Log de logout | N/A | Unit + Integration | P1 | `[OBSERVADO]` |
| F-AUTH-006 | Auth | OAuth GitHub | Usuario | Clicar "Entrar com GitHub" | Redirect OAuth → callback → criar/vincular user → gerar tokens | redirect, callback loading, erro, sucesso | Vincular por email se existir, senao criar | OAuthUseCase | POST /api/oauth/github/connect, GET /oauth/github/callback | AuthService | UserRegisteredEvent ou UserLoggedInEvent | PostgreSQL | auth.users, auth.user_identities | N/A | Log de OAuth | OAuth logins/dia | Unit + Integration + E2E | P1 | `[OBSERVADO]` |

## 4.2 Modulo: Chat e Conversas

| ID | Modulo | Descricao | Ator | Gatilho | Fluxo Principal | Estados UI | Regra de Negocio | Backend | Endpoint | Servico | Eventos | Persistencia | Tabelas | Cache | Auditoria | Observabilidade | Testes | Prioridade | Confianca |
|----|--------|-----------|------|---------|-----------------|-----------|-----------------|---------|----------|---------|---------|-------------|---------|-------|-----------|----------------|--------|-----------|-----------|
| F-CHAT-001 | Chat | Enviar mensagem para modelo de IA | Usuario | Enter ou botao Send | Validar input → criar conversa (se nova) → enviar ao backend → streaming resposta → exibir | typing, sending, streaming, sucesso, erro, retry | Prompt nao vazio, max 5000 chars, guardrail patterns, modelo valido | ChatUseCase | POST /api/v1/ai/chat | AiGatewayService | MessageSentEvent, ResponseReceivedEvent | PostgreSQL + localStorage | chat.conversations, chat.messages | Response cache opcional | Log por requisicao | Latencia p50/p95/p99, tokens/req, erros/provedor | Unit + Integration + E2E + Load | P0 | `[OBSERVADO]` |
| F-CHAT-002 | Chat | Selecionar modelo de IA | Usuario | Clicar ModelSelector dropdown | Abrir dropdown → selecionar modelo → proximo envio usa modelo | dropdown open, selected | Modelo deve estar no catalogo, provedor deve ter chave configurada | N/A (frontend) | N/A | N/A | chat_change_model (analytics) | localStorage (chat-store) | N/A | N/A | N/A | Trocas de modelo/dia | Unit | P0 | `[OBSERVADO]` |
| F-CHAT-003 | Chat | Criar nova conversa | Usuario | Clicar "+ Novo bate-papo" ou logo | Gerar ID → criar conversa vazia → limpar area de chat → mostrar empty state | empty state | Auto-generate title apos primeira mensagem | createConversation (store) | N/A (local) → futuro: POST /api/v1/conversations | ConversationService | ConversationCreatedEvent | localStorage → futuro: PostgreSQL | chat.conversations | N/A | N/A | Conversas criadas/dia | Unit | P0 | `[OBSERVADO]` |
| F-CHAT-004 | Chat | Parar geracao em andamento | Usuario | Clicar botao Stop (Square) | AbortController.abort() → cancelar request → manter texto parcial | streaming → parado | Texto parcial preservado, mensagem marcada como incompleta | N/A (abort signal) | Abort via AbortController | N/A | chat_stop_generation (analytics) | localStorage | N/A | N/A | N/A | Stops/dia | Unit | P1 | `[OBSERVADO]` |
| F-CHAT-005 | Chat | Buscar conversas | Usuario | Clicar "Procurar" e digitar | Expandir campo → filtrar lista por titulo em tempo real | search open, results, no results | Case-insensitive, match parcial no titulo | N/A (frontend filter) | N/A | N/A | chat_search (analytics) | N/A | N/A | N/A | N/A | Buscas/dia | Unit | P1 | `[OBSERVADO]` |
| F-CHAT-006 | Chat | Renomear conversa | Usuario | Menu contextual → Renomear | Exibir input → digitar novo nome → confirmar | editing, sucesso | Titulo nao vazio, max 100 chars | renameConversation (store) | N/A (local) → futuro: PATCH /api/v1/conversations/:id | ConversationService | ConversationRenamedEvent | localStorage → futuro: PostgreSQL | chat.conversations | N/A | N/A | N/A | Unit | P2 | `[OBSERVADO]` |
| F-CHAT-007 | Chat | Fixar/desfixar conversa | Usuario | Menu contextual → Fixar | Toggle pinned flag → reordenar lista | pinned/unpinned | Conversas fixadas aparecem no topo | toggleConversationPinned (store) | N/A (local) → futuro: PATCH /api/v1/conversations/:id | ConversationService | N/A | localStorage → futuro: PostgreSQL | chat.conversations | N/A | N/A | N/A | Unit | P2 | `[OBSERVADO]` |
| F-CHAT-008 | Chat | Excluir conversa | Usuario | Menu contextual → Excluir | Confirmacao → remover da lista → se ativa, mostrar empty state | confirmacao, sucesso | Soft delete no backend, remover do estado local | deleteConversation (store) | N/A (local) → futuro: DELETE /api/v1/conversations/:id | ConversationService | ConversationDeletedEvent | localStorage → futuro: PostgreSQL | chat.conversations | N/A | Log de exclusao | N/A | Unit | P2 | `[OBSERVADO]` |
| F-CHAT-009 | Chat | Limpar mensagens da conversa | Usuario | Menu contextual → Limpar | Confirmacao → remover mensagens → manter conversa | confirmacao, sucesso, empty | Conversa permanece, mensagens removidas | clearConversationMessages (store) | N/A (local) → futuro: DELETE /api/v1/conversations/:id/messages | ConversationService | N/A | localStorage → futuro: PostgreSQL | chat.messages | N/A | N/A | N/A | Unit | P3 | `[OBSERVADO]` |
| F-CHAT-010 | Chat | Copiar mensagem | Usuario | Clicar icone Copy na mensagem | Copiar conteudo para clipboard → feedback visual | default, copied (check icon temporario) | Copiar markdown raw, nao HTML renderizado | N/A (frontend clipboard) | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | Unit | P2 | `[OBSERVADO]` |
| F-CHAT-011 | Chat | Copiar bloco de codigo | Usuario | Clicar "Copy" no code block header | Copiar codigo para clipboard → feedback "Copiado!" | default, copied | Copiar apenas o codigo, sem header | N/A (frontend clipboard) | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | Unit | P2 | `[OBSERVADO]` |
| F-CHAT-012 | Chat | Fallback automatico entre provedores | Sistema | Provedor principal falha | Tentar modelo preferido → se erro, tentar fallback chain → retornar com metadata | transparente (mensagem indica modelo usado) | Chain: preferred → default → fallback list, max 3 tentativas | ChatUseCase | POST /api/v1/ai/chat | AiGatewayService, RoutingPolicy | ProviderFailoverEvent | Metricas | N/A | Circuit breaker stats | Fallbacks/dia, taxa de sucesso por provedor | Unit + Integration | P0 | `[OBSERVADO]` |

## 4.3 Modulo: Tema e Personalizacao

| ID | Modulo | Descricao | Ator | Gatilho | Fluxo Principal | Estados UI | Regra de Negocio | Backend | Endpoint | Servico | Eventos | Persistencia | Tabelas | Cache | Auditoria | Observabilidade | Testes | Prioridade | Confianca |
|----|--------|-----------|------|---------|-----------------|-----------|-----------------|---------|----------|---------|---------|-------------|---------|-------|-----------|----------------|--------|-----------|-----------|
| F-THEME-001 | Tema | Alternar tema (light/dark/system) | Usuario | Clicar "Personalizar" na sidebar | Ciclar light→dark→system → aplicar CSS vars → persistir | light, dark, system | System segue preferencia do OS (prefers-color-scheme) | N/A (frontend) | N/A | N/A | theme_change (analytics) | localStorage (lume-theme) | N/A | N/A | N/A | N/A | Unit | P1 | `[OBSERVADO]` |
| F-THEME-002 | Tema | Resolver tema do sistema | Sistema | Pagina carrega ou OS muda | Detectar prefers-color-scheme → aplicar light ou dark | light, dark | Listener em matchMedia para mudancas em tempo real | N/A (frontend) | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | Unit | P1 | `[OBSERVADO]` |

## 4.4 Modulo: Codex (Task Automation)

| ID | Modulo | Descricao | Ator | Gatilho | Fluxo Principal | Estados UI | Regra de Negocio | Backend | Endpoint | Servico | Eventos | Persistencia | Tabelas | Cache | Auditoria | Observabilidade | Testes | Prioridade | Confianca |
|----|--------|-----------|------|---------|-----------------|-----------|-----------------|---------|----------|---------|---------|-------------|---------|-------|-----------|----------------|--------|-----------|-----------|
| F-CDX-001 | Codex | Criar tarefa de automacao | Usuario | Task Composer form | Preencher descricao + repo + branch + env → criar task → acompanhar | form, validating, creating, running, completed, failed | Ambiente valido, repo conectado, creditos suficientes | TaskService | POST /api/tasks | TaskService | TaskCreatedEvent | PostgreSQL | codex.tasks | N/A | Log de criacao | Tasks criadas/dia, tempo de execucao | Unit + Integration + E2E | P1 | `[OBSERVADO]` |
| F-CDX-002 | Codex | Visualizar logs em tempo real | Usuario | Abrir /tasks/[id]/logs | Stream de logs → exibir em LiveLogViewer | loading, streaming, completed, error | Logs disponiveis apenas para owner da task | TaskService | GET /api/tasks/:id/logs | TaskService | N/A | PostgreSQL | codex.task_logs | N/A | N/A | N/A | Unit + Integration | P1 | `[OBSERVADO]` |
| F-CDX-003 | Codex | Visualizar diff de codigo | Usuario | Abrir /tasks/[id]/diff | Carregar diff → exibir em DiffViewer | loading, diff view, no changes, error | Diff disponivel apos execucao | TaskService | GET /api/tasks/:id/diff | TaskService | N/A | PostgreSQL | codex.task_artifacts | N/A | N/A | N/A | Unit + Integration | P1 | `[OBSERVADO]` |
| F-CDX-004 | Codex | Criar Pull Request | Usuario | Abrir /tasks/[id]/pull-request | Configurar PR → criar no GitHub → exibir link | form, creating, created, error | Requer integracao GitHub ativa | TaskService, GitHubIntegration | POST /api/tasks/:id/pull-requests | TaskService | PullRequestCreatedEvent | PostgreSQL | codex.pull_requests | N/A | Log de PR | PRs criadas/dia | Unit + Integration + E2E | P1 | `[OBSERVADO]` |
| F-CDX-005 | Codex | Cancelar tarefa em execucao | Usuario | Botao "Cancelar" | Enviar cancel → abortar execucao → atualizar status | running → cancelling → cancelled | Apenas tasks em execucao podem ser canceladas | TaskService | POST /api/tasks/:id/cancel | TaskService | TaskCancelledEvent | PostgreSQL | codex.tasks | N/A | Log de cancelamento | N/A | Unit + Integration | P2 | `[OBSERVADO]` |
| F-CDX-006 | Codex | Retry de tarefa falhada | Usuario | Botao "Retry" | Recriar task com mesmos parametros → executar | failed → retrying → running | Apenas tasks falhadas, respeitar limite de retries | TaskService | POST /api/tasks/:id/retry | TaskService | TaskRetriedEvent | PostgreSQL | codex.tasks | N/A | Log de retry | Retries/dia | Unit + Integration | P2 | `[OBSERVADO]` |

## 4.5 Modulo: Integracoes

| ID | Modulo | Descricao | Ator | Gatilho | Fluxo Principal | Estados UI | Regra de Negocio | Backend | Endpoint | Servico | Eventos | Persistencia | Tabelas | Cache | Auditoria | Observabilidade | Testes | Prioridade | Confianca |
|----|--------|-----------|------|---------|-----------------|-----------|-----------------|---------|----------|---------|---------|-------------|---------|-------|-----------|----------------|--------|-----------|-----------|
| F-INT-001 | Integracoes | Conectar GitHub | Admin | Clicar "Instalar GitHub" | OAuth flow → callback → armazenar tokens → listar repos | disconnected, connecting, connected, error | OAuth app registrado, scopes corretos | IntegrationService | POST /api/integrations/github/install | IntegrationService | GitHubConnectedEvent | PostgreSQL | integrations.connections | Token cache | Log de conexao | Integracoes ativas | Unit + Integration + E2E | P1 | `[OBSERVADO]` |
| F-INT-002 | Integracoes | Conectar Slack | Admin | Clicar "Instalar Slack" | OAuth flow → webhook setup → confirmar | disconnected, connecting, connected, error | Webhook URL validado | IntegrationService | POST /api/integrations/slack/install | IntegrationService | SlackConnectedEvent | PostgreSQL | integrations.connections | N/A | Log de conexao | N/A | Unit + Integration | P2 | `[OBSERVADO]` |
| F-INT-003 | Integracoes | Conectar Linear | Admin | Clicar "Instalar Linear" | OAuth flow → token → confirmar | disconnected, connecting, connected, error | Token valido | IntegrationService | POST /api/integrations/linear/install | IntegrationService | LinearConnectedEvent | PostgreSQL | integrations.connections | N/A | Log de conexao | N/A | Unit + Integration | P2 | `[OBSERVADO]` |
| F-INT-004 | Integracoes | Verificar status de integracoes | Admin | Pagina connectors carrega | GET status → exibir cards com status por integracao | loading, connected, disconnected, error | N/A | IntegrationService | GET /api/integrations/status | IntegrationService | N/A | PostgreSQL | integrations.connections | Status cache 1min | N/A | N/A | Unit + Integration | P1 | `[OBSERVADO]` |

## 4.6 Modulo: Billing e Creditos

| ID | Modulo | Descricao | Ator | Gatilho | Fluxo Principal | Estados UI | Regra de Negocio | Backend | Endpoint | Servico | Eventos | Persistencia | Tabelas | Cache | Auditoria | Observabilidade | Testes | Prioridade | Confianca |
|----|--------|-----------|------|---------|-----------------|-----------|-----------------|---------|----------|---------|---------|-------------|---------|-------|-----------|----------------|--------|-----------|-----------|
| F-BILL-001 | Billing | Visualizar planos disponiveis | Visitante/Usuario | Pagina /billing | GET plans → exibir cards de planos com features e precos | loading, loaded, error | Planos: Free, Pro, Team, Enterprise | BillingService | GET /api/billing/plans | BillingService | N/A | PostgreSQL | billing.plans | Plans cache 1h | N/A | N/A | Unit + Integration | P1 | `[OBSERVADO]` |
| F-BILL-002 | Billing | Visualizar uso atual | Usuario | Pagina /billing | GET usage → exibir graficos e metricas | loading, loaded, empty, error | Uso por periodo, por modelo, por provedor | BillingService | GET /api/billing/usage, GET /api/billing/usage/current | BillingService | N/A | PostgreSQL | billing.usage_records | Usage cache 5min | N/A | Usage queries/dia | Unit + Integration | P1 | `[OBSERVADO]` |
| F-BILL-003 | Billing | Comprar creditos | Usuario | Botao "Comprar creditos" | Selecionar pacote → checkout (Stripe) → confirmar pagamento → creditar | selection, checkout, processing, success, error | Integracao Stripe, webhook de confirmacao | BillingService | POST /api/credits/purchase | BillingService, StripeService | CreditsPurchasedEvent | PostgreSQL | billing.credits, billing.transactions | Saldo cache 1min | Log de compra | Compras/dia, receita | Unit + Integration + E2E | P1 | `[OBSERVADO]` |
| F-BILL-004 | Billing | Verificar saldo de creditos | Usuario | Qualquer pagina (header/sidebar) | GET credits → exibir saldo | loading, loaded, low balance alert | Alerta quando saldo < threshold | BillingService | GET /api/credits | BillingService | CreditLowEvent (se baixo) | PostgreSQL | billing.credits | Saldo cache 1min | N/A | N/A | Unit + Integration | P1 | `[OBSERVADO]` |

## 4.7 Modulo: Ambientes e Configuracoes (Codex)

| ID | Modulo | Descricao | Ator | Gatilho | Fluxo Principal | Estados UI | Regra de Negocio | Backend | Endpoint | Servico | Eventos | Persistencia | Tabelas | Cache | Auditoria | Observabilidade | Testes | Prioridade | Confianca |
|----|--------|-----------|------|---------|-----------------|-----------|-----------------|---------|----------|---------|---------|-------------|---------|-------|-----------|----------------|--------|-----------|-----------|
| F-ENV-001 | Ambientes | Criar ambiente de execucao | Admin | Botao "Novo ambiente" | Form → validar → criar → disponibilizar | form, validating, creating, created, error | Nome unico, config valida | EnvironmentService | POST /api/environments | EnvironmentService | EnvironmentCreatedEvent | PostgreSQL | codex.environments | N/A | Log de criacao | N/A | Unit + Integration | P2 | `[OBSERVADO]` |
| F-ENV-002 | Ambientes | Validar ambiente | Admin | Botao "Validar" | POST validate → verificar conectividade → resultado | validating, valid, invalid | Testar SSH/Docker/API connectivity | EnvironmentService | POST /api/environments/:id/validate | EnvironmentService | N/A | N/A | N/A | N/A | N/A | N/A | Unit + Integration | P2 | `[OBSERVADO]` |

## 4.8 Modulo: Analytics

| ID | Modulo | Descricao | Ator | Gatilho | Fluxo Principal | Estados UI | Regra de Negocio | Backend | Endpoint | Servico | Eventos | Persistencia | Tabelas | Cache | Auditoria | Observabilidade | Testes | Prioridade | Confianca |
|----|--------|-----------|------|---------|-----------------|-----------|-----------------|---------|----------|---------|---------|-------------|---------|-------|-----------|----------------|--------|-----------|-----------|
| F-ANA-001 | Analytics | Enviar eventos de telemetria | Sistema | Acoes do usuario (auto-flush) | Queue local → batch → POST /analytics/events | transparente | Max 8 eventos por batch, flush em page hide | AnalyticsIngestionService | POST /api/v1/analytics/events | AnalyticsService | N/A | PostgreSQL | analytics.event_reports, analytics.ingested_events | N/A | N/A | Eventos/min | Unit + Integration | P1 | `[OBSERVADO]` |
| F-ANA-002 | Analytics | Visualizar dashboard de analytics | Admin | Pagina /settings/analytics | GET reports → exibir graficos e metricas | loading, loaded, empty, error | Filtros por periodo, categoria, paginacao | AnalyticsIngestionService | GET /api/v1/analytics/reports | AnalyticsService | N/A | PostgreSQL | analytics.event_reports | Reports cache 5min | N/A | N/A | Unit + Integration | P2 | `[OBSERVADO]` |

## 4.9 Modulo: Funcionalidades Propostas (AI Aggregator)

| ID | Modulo | Descricao | Ator | Gatilho | Fluxo Principal | Estados UI | Regra de Negocio | Backend | Endpoint | Servico | Eventos | Persistencia | Tabelas | Cache | Auditoria | Observabilidade | Testes | Prioridade | Confianca |
|----|--------|-----------|------|---------|-----------------|-----------|-----------------|---------|----------|---------|---------|-------------|---------|-------|-----------|----------------|--------|-----------|-----------|
| F-AGG-001 | Catalogo | Visualizar catalogo de provedores e modelos | Usuario | Pagina /providers ou modal | GET providers → listar com status, capacidades, precos | loading, loaded, error, provider offline | Atualizar status via health check periodico | ProviderRegistryService | GET /api/v1/providers, GET /api/v1/providers/:id/models | ProviderRegistryService | N/A | PostgreSQL | ai_gateway.providers, ai_gateway.models | Cache 5min | N/A | Consultas/dia | Unit + Integration | P0 | `[PROPOSTO]` |
| F-AGG-002 | BYOK | Gerenciar chaves proprias do usuario | Usuario | Pagina /settings/keys | CRUD de chaves por provedor, criptografia AES-256, validacao | form, saved, error, testing, valid, invalid | Chaves criptografadas em repouso, scoped por org | CredentialService | POST/GET/DELETE /api/v1/credentials | CredentialService | CredentialAddedEvent | PostgreSQL | ai_gateway.user_credentials | N/A | Log de acesso | N/A | Unit + Integration + Security | P1 | `[PROPOSTO]` |
| F-AGG-003 | Playground | Testar prompts com multiplos modelos | Usuario | Pagina /playground | Selecionar modelos → enviar prompt → comparar respostas lado a lado | multi-panel, loading per model, results, error per model | Limite de modelos simultaneos por plano | PlaygroundService | POST /api/v1/playground/compare | PlaygroundService | PlaygroundComparisonEvent | PostgreSQL (log) | ai_gateway.playground_sessions | N/A | N/A | Comparacoes/dia | Unit + Integration + E2E | P1 | `[PROPOSTO]` |
| F-AGG-004 | Roteamento | Configurar politicas de roteamento | Admin | Pagina /settings/routing | Definir regras: custo, latencia, qualidade, fallback chain | form, saved, active | Regras por org/user, prioridade configuravel | RoutingPolicyService | POST/GET /api/v1/routing/policies | RoutingPolicyService | PolicyUpdatedEvent | PostgreSQL | ai_gateway.routing_policies | Policy cache 1min | Log de mudancas | N/A | Unit + Integration | P1 | `[PROPOSTO]` |
| F-AGG-005 | Observabilidade | Dashboard de metricas por provedor/modelo | Admin | Pagina /dashboard/providers | Graficos de latencia, erro, custo, disponibilidade por provedor | loading, loaded, empty, error | Dados agregados de Micrometer/Prometheus | MetricsService | GET /api/v1/metrics/providers, GET /api/v1/metrics/models | MetricsService | N/A | Prometheus + PostgreSQL | ai_gateway.metrics_snapshots | Metrics cache 1min | N/A | Self-observing | Unit + Integration | P1 | `[PROPOSTO]` |
| F-AGG-006 | Prompts | Gestao de templates de prompts | Usuario | Pagina /prompts | CRUD de templates, versionamento, categorias, favoritos | list, form, preview, saved, error | Templates por org, versionados, compartilhaveis | PromptService | CRUD /api/v1/prompts | PromptService | PromptCreatedEvent | PostgreSQL | content.prompts, content.prompt_versions | N/A | Log de mudancas | Templates criados/dia | Unit + Integration | P2 | `[PROPOSTO]` |

---

# SECAO 5 — ESPECIFICACAO FUNCIONAL DETALHADA

## F-CHAT-001: Enviar Mensagem para Modelo de IA

### Objetivo
Permitir que o usuario envie uma mensagem de texto a um modelo de IA selecionado e receba uma resposta em streaming.

### Problema que resolve
O usuario precisa interagir com modelos de IA de diferentes provedores de forma transparente, sem precisar gerenciar APIs, chaves, formatos ou limites individuais.

### Persona/Ator
- **Primario:** Usuario autenticado (qualquer plano)
- **Secundario:** Sistema (roteamento, fallback, guardrails)

### Jornada completa passo a passo

1. Usuario esta na tela de chat (empty state ou conversa ativa)
2. Usuario digita mensagem no textarea
3. Textarea faz auto-resize ate 200px maximo
4. Botao Send muda de cinza (`surface-hover`) para primario (`primary`) quando ha texto
5. Usuario pressiona Enter (sem Shift) ou clica no botao Send
6. **Validacao client-side:** input.trim() nao vazio, isSending === false
7. Se nenhuma conversa ativa → `createConversation()` gera nova conversa com ID UUID
8. Input e limpo, textarea reseta para altura minima
9. Mensagem do usuario e adicionada ao array de mensagens da conversa (role: 'user')
10. `isSending = true`, `isStreaming = true`
11. AbortController criado para permitir cancelamento
12. **Request:** POST /api/v1/ai/chat com `{ prompt, preferredModel }`
13. **Backend — Prompt Guardrail:** Valida comprimento (max 5000 chars) e patterns bloqueados
14. **Backend — Routing:** Resolve modelos ordenados (preferred → default → fallbacks)
15. **Backend — Provider Loop:** Tenta cada provedor na ordem, com circuit breaker e retry
16. **Backend — Output Guardrail:** Valida resposta (max 8000 chars, sem API keys)
17. **Backend — Telemetria:** Registra metricas (latencia, modelo, provedor, sucesso/falha)
18. **Response:** `{ content, modelUsed, providerUsed, fallbackUsed, attempts }`
19. Frontend recebe resposta e faz streaming simulado (chunks de 4 chars com 12ms delay)
20. Mensagem do assistente adicionada (role: 'assistant', com metadata do modelo)
21. `isSending = false`, `isStreaming = false`
22. Se primeira mensagem da conversa → auto-gerar titulo (primeiros ~30 chars do prompt)
23. Conversa atualiza `updatedAt` → reordena na lista lateral
24. Foco retorna ao textarea

### Entradas
- `prompt: string` — Mensagem do usuario (1-5000 chars)
- `preferredModel: string` — ID do modelo selecionado (ex: "gpt-4o-mini")

### Saidas
- `content: string` — Resposta do modelo
- `modelUsed: string` — Modelo que efetivamente gerou a resposta
- `providerUsed: string` — Provedor utilizado
- `fallbackUsed: boolean` — Se houve fallback
- `attempts: number` — Numero de tentativas

### Regras de negocio
1. Prompt nao pode ser vazio `[OBSERVADO]`
2. Prompt maximo: 5000 caracteres (configuravel) `[OBSERVADO]`
3. Patterns bloqueados: regex contra jailbreak/system override `[OBSERVADO]`
4. Resposta maxima: 8000 caracteres `[OBSERVADO]`
5. Fallback chain: preferred → gpt-4o-mini → claude-3-5-haiku → gemini-1.5-flash `[OBSERVADO]`
6. Circuit breaker por provedor: abre com 50% de falha em 20 chamadas `[OBSERVADO]`
7. Retry: 2 tentativas com backoff de 250ms `[OBSERVADO]`
8. Timeout por provedor: 15 segundos `[OBSERVADO]`

### Regras de permissao
- Requer autenticacao (JWT valido) `[OBSERVADO]`
- Requer plano ativo (free com limites, pro sem limites) `[PROPOSTO]`
- Rate limit por usuario: 60 req/min (free), 300 req/min (pro) `[PROPOSTO]`

### Validacoes client-side
- Input nao vazio apos trim
- isSending === false (nao permite envio duplo)
- Modelo selecionado presente no catalogo

### Validacoes server-side
- JWT valido e nao expirado
- User status === ACTIVE
- Prompt length <= maxPromptLength
- Prompt nao match com blockedPatterns
- Modelo suportado por pelo menos um provedor configurado
- Response length <= maxOutputLength
- Response nao match com outputBlockedPatterns

### Regras de persistencia
- **Atual:** localStorage via Zustand persist (key: `ia-aggregator-chat-store`) `[OBSERVADO]`
- **Proposto:** PostgreSQL server-side com sync bilateral `[PROPOSTO]`
  - Tabela `chat.conversations` para conversas
  - Tabela `chat.messages` para mensagens
  - Sync: salvar no servidor apos cada mensagem, carregar ao login

### Mensagens de erro, aviso e sucesso

| Cenario | Tipo | Mensagem |
|---------|------|----------|
| Prompt vazio | Prevencao | Botao Send desabilitado (cinza) |
| Prompt muito longo | Erro client | "Mensagem excede o limite de 5000 caracteres" |
| Prompt bloqueado (guardrail) | Erro server | "Sua mensagem nao pode ser processada. Tente reformular." |
| Provedor indisponivel | Erro server | "O modelo selecionado esta temporariamente indisponivel. Tentando alternativa..." |
| Todos provedores falharam | Erro server | "Nao foi possivel gerar uma resposta. Tente novamente em alguns instantes." |
| Rate limit | Erro server | "Voce atingiu o limite de requisicoes. Aguarde um momento." |
| Resposta bloqueada (output guardrail) | Erro server | "A resposta foi filtrada por questoes de seguranca." |
| Sucesso com fallback | Info | Badge discreto: "Respondido por {modelUsed}" |
| Streaming interrompido | Info | Texto parcial preservado |

### Estados de loading, empty, partial, erro e retry

| Estado | Visual | Comportamento |
|--------|--------|---------------|
| **Empty state** | Greeting + input + quick actions | Nenhuma conversa ativa |
| **Typing** | Input com texto, botao Send primario | Aguardando envio |
| **Sending** | Input desabilitado, botao vira Stop | Request em andamento |
| **Streaming** | Texto aparece progressivamente, StreamingIndicator ativo | Resposta sendo recebida |
| **Success** | Mensagem completa, input reativado | Resposta finalizada |
| **Error** | MessageBubble role='error' com mensagem vermelha | Falha no request |
| **Partial** | Texto parcial + indicacao de interrupcao | Usuario clicou Stop |
| **Retry** | Botao retry na mensagem de erro | Usuario pode reenviar |

### Edge cases
1. **Envio duplo:** Prevenido por check `isSending` `[OBSERVADO]`
2. **Conexao perdida:** AbortError capturado, mensagem de erro exibida `[OBSERVADO]`
3. **Token expirado durante envio:** Interceptor tenta refresh, se falhar exibe erro `[OBSERVADO]`
4. **Modelo removido do catalogo:** Fallback para modelo default `[OBSERVADO]`
5. **Todos provedores em circuit breaker aberto:** Retorna erro "Servico temporariamente indisponivel" `[INFERIDO]`
6. **Resposta vazia do provedor:** Retentativa automatica (error code AI_005) `[OBSERVADO]`
7. **Conversa com muitas mensagens:** Scroll automatico para ultima mensagem `[OBSERVADO]`
8. **Input com quebras de linha:** Shift+Enter insere nova linha `[OBSERVADO]`

### Criterios de aceite (Given/When/Then)

```gherkin
Scenario: Envio bem-sucedido de mensagem
  Given usuario autenticado na tela de chat
  And modelo "gpt-4o-mini" selecionado
  When usuario digita "Ola, como voce esta?" e pressiona Enter
  Then mensagem do usuario aparece na area de chat
  And indicador de streaming fica ativo
  And resposta do modelo aparece progressivamente
  And mensagem do assistente tem metadata "modelUsed: gpt-4o-mini"
  And input fica vazio e reativado

Scenario: Fallback automatico quando provedor falha
  Given usuario autenticado com modelo "gpt-4o-mini" selecionado
  And provedor OpenAI esta indisponivel
  When usuario envia mensagem
  Then sistema tenta OpenAI (falha)
  And sistema tenta claude-3-5-haiku (sucesso)
  And resposta mostra "fallbackUsed: true, modelUsed: claude-3-5-haiku"

Scenario: Prompt bloqueado por guardrail
  Given usuario autenticado
  When usuario envia mensagem com pattern bloqueado
  Then resposta e uma mensagem de erro (role: 'error')
  And mensagem diz "Sua mensagem nao pode ser processada"

Scenario: Cancelamento de geracao
  Given usuario enviou mensagem e streaming esta ativo
  When usuario clica no botao Stop
  Then streaming para imediatamente
  And texto parcial e preservado na conversa
  And input e reativado
```

---

## F-AUTH-001: Registro de Usuario

### Objetivo
Permitir que novos usuarios criem conta na plataforma Lume.

### Problema que resolve
Primeiro acesso a plataforma, necessario para uso de qualquer funcionalidade autenticada.

### Persona/Ator
- **Primario:** Visitante (nao autenticado)

### Jornada completa
1. Visitante acessa pagina de registro
2. Preenche: nome completo, email, senha, confirmacao de senha
3. Aceita termos de uso e politica de privacidade
4. Clica "Criar conta"
5. **Validacao client-side:** campos obrigatorios, email valido, senha >= 8 chars, senhas coincidem
6. **Request:** POST /api/v1/auth/register
7. **Backend:** Verifica email unico, hash BCrypt(10), cria User entity, publica UserRegisteredEvent
8. **Response:** TokenResponse (accessToken + refreshToken)
9. Email de verificacao enviado (async)
10. Redirect para /chat ou /welcome (first run)

### Regras de negocio
- Email deve ser unico no sistema `[OBSERVADO]`
- Senha minimo 8 caracteres `[PROPOSTO]`
- BCrypt com strength 10 `[OBSERVADO]`
- User criado com status PENDING_VERIFICATION `[OBSERVADO]`
- User role default: USER `[OBSERVADO]`
- Organization pessoal auto-criada `[INFERIDO]`

### Criterios de aceite
```gherkin
Scenario: Registro bem-sucedido
  Given visitante na pagina de registro
  When preenche nome "Joao Silva", email "joao@email.com", senha "Senha123!"
  And aceita termos
  And clica "Criar conta"
  Then conta e criada com status PENDING_VERIFICATION
  And tokens JWT sao retornados
  And email de verificacao e enviado
  And usuario e redirecionado para /welcome

Scenario: Email ja cadastrado
  Given email "joao@email.com" ja existe
  When visitante tenta registrar com esse email
  Then erro "Este email ja esta cadastrado"
  And nenhuma conta e criada
```

---

## F-AGG-001: Catalogo de Provedores e Modelos

### Objetivo
Exibir ao usuario todos os provedores de IA disponiveis na plataforma, seus modelos, capacidades, status e precos.

### Problema que resolve
Em um agregador multi-provedor, o usuario precisa visualizar e comparar opcoes de modelos para tomar decisoes informadas.

### Persona/Ator
- **Primario:** Usuario autenticado
- **Secundario:** Admin (gestao do catalogo)

### Jornada completa
1. Usuario acessa pagina /providers (ou modal de selecao de modelo)
2. Catalogo carrega com lista de provedores organizados por categoria
3. Cada provedor mostra: nome, logo, status (online/offline/degraded), modelos disponiveis
4. Cada modelo mostra: nome, capacidades (texto, visao, imagem, etc.), context window, preco por token, latencia media
5. Usuario pode filtrar por: categoria, capacidade, preco, status
6. Usuario pode favoritar modelos para acesso rapido no ModelSelector
7. Status atualizado via health check periodico (a cada 60s)

### Regras de negocio
- Provedores com chave nao configurada aparecem como "Configuracao necessaria" `[PROPOSTO]`
- Provedores em circuit breaker aberto aparecem como "Degradado" `[PROPOSTO]`
- Precos atualizados periodicamente via API do provedor ou manualmente `[PROPOSTO]`
- Modelos podem ser habilitados/desabilitados por plano `[PROPOSTO]`
- BYOK users veem todos os modelos do provedor configurado `[PROPOSTO]`

### Estados de interface

| Estado | Visual |
|--------|--------|
| Loading | Skeleton cards para cada provedor |
| Loaded | Grid de cards com status colorido (verde=online, amarelo=degraded, vermelho=offline) |
| Empty | "Nenhum provedor configurado. Configure suas chaves de API." |
| Error | "Erro ao carregar catalogo. Tente novamente." |
| Provider offline | Card com badge "Offline" e ultima hora de disponibilidade |
| Provider degraded | Card com badge "Degradado" e latencia elevada |

### Criterios de aceite
```gherkin
Scenario: Visualizar catalogo completo
  Given usuario autenticado
  When acessa pagina /providers
  Then ve lista de provedores com status atual
  And cada provedor mostra modelos disponiveis
  And modelos mostram capacidades e precos

Scenario: Filtrar por capacidade
  Given catalogo carregado
  When usuario filtra por "geracao de imagem"
  Then apenas provedores com modelos de imagem sao exibidos
```

---

## F-AGG-002: BYOK (Bring Your Own Key)

### Objetivo
Permitir que usuarios utilizem suas proprias chaves de API para acessar provedores diretamente, sem intermediacao de billing da Lume.

### Problema que resolve
Usuarios com contratos existentes com provedores ou que desejam controle total sobre custos e limites.

### Persona/Ator
- **Primario:** Usuario autenticado (plano Pro ou superior)

### Jornada completa
1. Usuario acessa /settings/keys
2. Lista de provedores disponíveis para BYOK
3. Usuario seleciona provedor (ex: OpenAI)
4. Preenche campo de API key
5. Clica "Testar chave" → backend valida com chamada de teste ao provedor
6. Se valida → criptografa com AES-256 → armazena
7. Se invalida → exibe erro com detalhes
8. A partir de agora, chamadas para modelos desse provedor usam a chave do usuario

### Regras de negocio
- Chave criptografada em repouso (AES-256-GCM) `[PROPOSTO]`
- Chave nunca exibida apos salvar (apenas prefixo: sk-...abc) `[PROPOSTO]`
- Validacao obrigatoria antes de salvar `[PROPOSTO]`
- Chave scoped por user + org `[PROPOSTO]`
- Chamadas BYOK nao consomem creditos da Lume `[PROPOSTO]`
- Rate limits do provedor aplicam-se diretamente `[PROPOSTO]`
- Logs de uso preservados para auditoria `[PROPOSTO]`

---

## F-AGG-003: Playground Comparativo

### Objetivo
Permitir que o usuario envie um mesmo prompt para multiplos modelos simultaneamente e compare respostas lado a lado.

### Problema que resolve
Escolher o melhor modelo para um caso de uso especifico, comparando qualidade, velocidade e custo.

### Persona/Ator
- **Primario:** Usuario autenticado

### Jornada completa
1. Usuario acessa /playground
2. Seleciona 2-4 modelos para comparacao
3. Digita prompt no campo unificado
4. Clica "Comparar"
5. Respostas aparecem em paineis lado a lado, com streaming independente
6. Cada painel mostra: modelo, provedor, tempo de resposta, tokens usados, custo estimado
7. Usuario pode avaliar (thumbs up/down) cada resposta
8. Historico de comparacoes salvo para referencia

### Regras de negocio
- Maximo de 4 modelos simultaneos `[PROPOSTO]`
- Cada chamada conta separadamente para billing `[PROPOSTO]`
- Free plan: 5 comparacoes/dia; Pro: ilimitadas `[PROPOSTO]`
- Resultados logados para analytics de qualidade `[PROPOSTO]`

---

# SECAO 6 — ESPECIFICACAO DE NEGOCIO

## 6.1 Proposta de Valor

**Lume** ilumina o caminho para a melhor IA. Em vez de ficar preso a um unico provedor, a Lume permite que usuarios e empresas acessem dezenas de modelos de IA — de texto, imagem, audio, video, codigo e busca — dentro de uma unica plataforma premium, com roteamento inteligente, observabilidade completa e billing unificado.

**Value proposition canvas:**
- **Para** profissionais de tecnologia, empresas e equipes de produto
- **Que** precisam usar multiplas IAs mas enfrentam fragmentacao de ferramentas, billing e credenciais
- **A Lume** e uma plataforma unificada de agregacao de IA
- **Que** permite acessar 50+ modelos de 18+ provedores em uma unica interface
- **Diferente de** usar cada provedor separadamente ou proxies simples como OpenRouter
- **Porque** oferece roteamento inteligente, fallback automatico, observabilidade por provedor, BYOK, billing unificado e experiencia premium de chat

## 6.2 Visao de Produto

Ser a plataforma de referencia para acesso unificado a inteligencia artificial na America Latina, comecando pelo Brasil, com suporte a todos os principais provedores globais de IA e experiencia de uso premium.

## 6.3 Dominios de Negocio

| Dominio | Bounded Context | Responsabilidade |
|---------|-----------------|------------------|
| **Identity** | auth, users, sessions | Autenticacao, autorizacao, perfis, sessoes |
| **AI Gateway** | providers, models, routing, inference | Roteamento, fallback, guardrails, inferencia |
| **Conversation** | conversations, messages | Chat, historico, persistencia |
| **Billing** | plans, subscriptions, credits, usage | Planos, cobranca, metering, creditos |
| **Teams** | organizations, members, roles | Multi-tenant, permissoes, colaboracao |
| **Content** | prompts, templates, artifacts | Templates, presets, conteudo gerado |
| **Codex** | tasks, environments, PRs | Automacao de tarefas de codigo |
| **Integration** | connectors, webhooks | GitHub, Slack, Linear, webhooks |
| **Analytics** | events, reports, metrics | Telemetria, dashboards, insights |
| **Compliance** | audit, consent, erasure | LGPD, auditoria, exportacao de dados |

## 6.4 Entidades de Negocio

| Entidade | Descricao | Atributos-chave |
|----------|-----------|-----------------|
| User | Pessoa fisica com conta na Lume | id, email, fullName, role, status, orgId |
| Organization | Tenant (pessoal ou team) | id, name, slug, type, plan, stripeId |
| Conversation | Sessao de chat com um modelo | id, userId, orgId, title, model, messages[] |
| Message | Mensagem individual em conversa | id, conversationId, role, content, modelUsed, providerUsed |
| Provider | Provedor de IA (OpenAI, Anthropic, etc.) | id, name, baseUrl, status, capabilities |
| Model | Modelo especifico de um provedor | id, providerId, name, modalities[], contextWindow, pricing |
| APIKey | Chave de API (plataforma ou BYOK) | id, userId, orgId, provider, keyHash, scopes |
| Subscription | Assinatura do plano | id, orgId, planId, status, currentPeriodEnd |
| UsageRecord | Registro de consumo por request | id, userId, orgId, model, provider, tokens, cost, timestamp |
| Credential | Chave BYOK do usuario | id, userId, providerId, encryptedKey, lastValidated |
| Prompt | Template de prompt | id, userId, orgId, title, content, category, version |
| Task | Tarefa do Codex | id, userId, description, repo, status, result |

## 6.5 Papeis e Permissoes

| Role | Escopo | Permissoes |
|------|--------|------------|
| **user** | Pessoal | Chat, conversas, prompts, billing pessoal, BYOK pessoal |
| **org_member** | Organizacao | Tudo de user + conversas compartilhadas, prompts da org |
| **org_admin** | Organizacao | Tudo de org_member + gerenciar membros, billing org, integracoes, ambientes |
| **org_owner** | Organizacao | Tudo de org_admin + transferir propriedade, excluir org |
| **admin** | Plataforma | Acesso a metricas globais, feature flags, configuracoes |
| **super_admin** | Plataforma | Tudo de admin + gerenciar usuarios, orgs, planos, compliance |

## 6.6 Planos e Limites

| Recurso | Free | Pro | Team | Enterprise |
|---------|------|-----|------|------------|
| Preco | R$ 0 | R$ 49/mes | R$ 99/membro/mes | Sob consulta |
| Mensagens/dia | 50 | Ilimitadas | Ilimitadas | Ilimitadas |
| Modelos disponiveis | 5 basicos | Todos | Todos + priority | Todos + custom |
| Context window max | 32K | 200K | 200K | Custom |
| Playground comparacoes/dia | 3 | Ilimitadas | Ilimitadas | Ilimitadas |
| BYOK | Nao | Sim | Sim | Sim |
| Codex tasks/mes | 10 | 100 | 500 | Ilimitadas |
| Membros | 1 | 1 | Ilimitados | Ilimitados |
| Suporte | Comunidade | Email | Email + Chat | Dedicado |
| SLA | Nenhum | 99.5% | 99.9% | 99.95% |
| Retencao de dados | 30 dias | 1 ano | Ilimitada | Custom |
| Analytics | Basico | Completo | Completo + export | Custom |
| Integrações | Nenhuma | GitHub | GitHub, Slack, Linear | Custom + API |
| SSO | Nao | Nao | SAML/OIDC | SAML/OIDC + SCIM |

`[PROPOSTO]` — Valores de referencia baseados em benchmarks de mercado (OpenRouter markup 5%, Portkey $49/mes)

## 6.7 Modelo de Receita

1. **Assinaturas recorrentes** — Planos mensais/anuais via Stripe `[PROPOSTO]`
2. **Pay-per-use** — Markup sobre tokens consumidos (10-15%) para Free/Pro `[PROPOSTO]`
3. **Creditos pre-pagos** — Pacotes de creditos com desconto por volume `[OBSERVADO]`
4. **Enterprise** — Contratos anuais com SLA e suporte dedicado `[PROPOSTO]`

## 6.8 Faturamento e Billing

- **Processador:** Stripe (checkout, subscriptions, invoices, webhooks) `[PROPOSTO]`
- **Metering:** Registro de cada request com tokens in/out, modelo, provedor, custo `[PROPOSTO]`
- **Ciclo:** Mensal, pro-rata para upgrades mid-cycle `[PROPOSTO]`
- **Trial:** 14 dias Pro gratis para novos usuarios `[PROPOSTO]`
- **Downgrade:** Efetivo no final do ciclo atual `[PROPOSTO]`
- **Cancelamento:** Acesso mantido ate final do ciclo pago `[PROPOSTO]`
- **Cobranca falha:** 3 tentativas, suspensao apos todas falharem `[PROPOSTO]`

## 6.9 Governanca de Dados e LGPD

| Aspecto | Implementacao |
|---------|---------------|
| **Base legal** | Consentimento explicito (registro) e execucao de contrato `[OBSERVADO — schema consent_records]` |
| **Direito de acesso** | Export completo dos dados do usuario via /api/compliance/exports `[OBSERVADO]` |
| **Direito de exclusao** | Erasure request → anonimizar/excluir dados → manter logs de auditoria `[OBSERVADO — schema erasure_requests]` |
| **Portabilidade** | Exportar conversas, prompts, configuracoes em JSON `[PROPOSTO]` |
| **Retencao** | Conversas: conforme plano; Logs: 90 dias; Auditoria: 5 anos `[PROPOSTO]` |
| **Consentimento** | Registro de consentimento com versao, data, IP `[OBSERVADO]` |
| **DPO** | Ponto de contato para questoes de privacidade `[PROPOSTO]` |
| **Incidentes** | Notificacao em 72h conforme LGPD Art. 48 `[PROPOSTO]` |

## 6.10 KPIs e Metricas de Negocio

| KPI | Descricao | Meta |
|-----|-----------|------|
| MAU | Monthly Active Users | Crescimento 20% MoM |
| DAU/MAU | Stickiness ratio | > 40% |
| Messages/day | Volume de mensagens diarias | Monitorar tendencia |
| Tokens consumed | Total de tokens processados | Monitorar custo |
| Revenue (MRR) | Receita recorrente mensal | Crescimento 15% MoM |
| ARPU | Receita media por usuario | > R$ 30 |
| Churn rate | Taxa de cancelamento mensal | < 5% |
| NPS | Net Promoter Score | > 50 |
| TTFR | Time to first response (latencia) | < 500ms p95 |
| Error rate | Taxa de erro por provedor | < 1% |
| Fallback rate | Taxa de uso de fallback | < 10% |
| Conversion (free→pro) | Taxa de conversao | > 5% |

---

# SECAO 7 — ESPECIFICACAO TECNICA COMPLETA

## 7.1 Arquitetura Geral

```
[Browser]
    |
    v
[Next.js 15 BFF] ←→ [CDN / Static Assets]
    |
    | (HTTPS / REST / SSE)
    v
[Spring Boot 3.4.3 API Gateway]
    |
    +--→ [PostgreSQL 16] (transacional)
    +--→ [Redis 7] (cache, sessions, rate limiting)
    +--→ [S3 / MinIO] (file storage) [PROPOSTO]
    +--→ [Prometheus + Grafana] (metricas) [PROPOSTO]
    +--→ [Loki] (logs centralizados) [PROPOSTO]
    |
    | (HTTP/HTTPS)
    v
[AI Providers] (18+ provedores via adaptadores)
    - OpenAI, Anthropic, Google, Mistral, Cohere, DeepSeek,
      Groq, xAI, Together, Fireworks, Azure, NVIDIA,
      Cerebras, SambaNova, Novita, OpenRouter, Perplexity
```

## 7.2 Arquitetura de Frontend

| Aspecto | Implementacao | Confianca |
|---------|---------------|-----------|
| **Framework** | Next.js 15 (App Router) + React 19 | `[OBSERVADO]` |
| **Linguagem** | TypeScript 5.7, strict mode | `[OBSERVADO]` |
| **Estilizacao** | Tailwind CSS v4 + CSS custom properties | `[OBSERVADO]` |
| **Estado cliente** | Zustand 5.0 com persist (localStorage) | `[OBSERVADO]` |
| **Estado servidor** | @tanstack/react-query 5.x | `[OBSERVADO]` |
| **Formularios** | react-hook-form 7.x + zod 3.x | `[OBSERVADO]` |
| **Markdown** | react-markdown + remark-gfm + rehype-highlight | `[OBSERVADO]` |
| **Animacoes** | framer-motion 11.x | `[OBSERVADO]` |
| **Icones** | lucide-react 0.460 | `[OBSERVADO]` |
| **HTTP** | Axios 1.7 com interceptors | `[OBSERVADO]` |
| **i18n** | next-intl 4.x (pt-BR) | `[OBSERVADO]` |
| **Testes** | Jest 29 + Playwright 1.53 | `[OBSERVADO]` |

### Padroes de Frontend
- **Server Components** para paginas estaticas, **Client Components** para interatividade `[OBSERVADO]`
- **BFF pattern** via Next.js API routes proxying para backend Java `[OBSERVADO]`
- **CSS Variables** para theming (light/dark/system) `[OBSERVADO]`
- **Zustand persist** para estado offline-first `[OBSERVADO]`
- **Analytics queue** com auto-flush em page hide `[OBSERVADO]`

## 7.3 Arquitetura de Backend

| Aspecto | Implementacao | Confianca |
|---------|---------------|-----------|
| **Framework** | Spring Boot 3.4.3 | `[OBSERVADO]` |
| **Linguagem** | Java 21 | `[OBSERVADO]` |
| **Arquitetura** | Hexagonal (Ports & Adapters) | `[OBSERVADO]` |
| **ORM** | Spring Data JPA (Hibernate) | `[OBSERVADO]` |
| **Banco** | PostgreSQL (Hikari pool 5-20) | `[OBSERVADO]` |
| **Cache** | Redis (Spring Data Redis) | `[OBSERVADO]` |
| **Migrations** | Flyway 10.20 | `[OBSERVADO]` |
| **Auth** | JWT HS256 (jjwt 0.12.6) | `[OBSERVADO]` |
| **Passwords** | BCrypt strength 10 | `[OBSERVADO]` |
| **Resiliencia** | Resilience4j 2.2 (circuit breaker) | `[OBSERVADO]` |
| **Metricas** | Micrometer 1.13 + Actuator | `[OBSERVADO]` |
| **Mapping** | MapStruct 1.6 | `[OBSERVADO]` |
| **API Docs** | SpringDoc OpenAPI 2.6 | `[OBSERVADO]` |
| **Testes** | JUnit 5 + Mockito + TestContainers + ArchUnit | `[OBSERVADO]` |
| **Build** | Maven, spring-boot-maven-plugin | `[OBSERVADO]` |

### Modulos Maven (Clean Architecture)

```
ia-aggregator/
├── ia-aggregator-common          # Exceptions, error codes, shared utils
├── ia-aggregator-domain           # Entities, value objects, domain events, repo interfaces
├── ia-aggregator-application      # Use cases, DTOs, port interfaces (in/out)
├── ia-aggregator-infrastructure   # JPA, Redis, security, AI providers, adapters
└── ia-aggregator-presentation     # Controllers, filters, config, migrations, application.yml
```
`[OBSERVADO]`

## 7.4 Autenticacao e Autorizacao

| Aspecto | Implementacao |
|---------|---------------|
| **Protocolo** | JWT Bearer Token (Authorization header) `[OBSERVADO]` |
| **Algoritmo** | HS256 (HMAC-SHA256) `[OBSERVADO]` |
| **Access token** | 15 minutos de expiracao `[OBSERVADO]` |
| **Refresh token** | 7 dias de expiracao `[OBSERVADO]` |
| **Secret** | 256-bit base64, configuravel via JWT_SECRET `[OBSERVADO]` |
| **Brute force** | Max 5 tentativas, bloqueio 30 minutos `[OBSERVADO]` |
| **OAuth** | GitHub (implementado), Google (proposto) `[OBSERVADO parcial]` |
| **Session tracking** | Tabela auth.sessions com IP, UA, device info `[OBSERVADO]` |

## 7.5 Multi-Tenant

| Aspecto | Implementacao |
|---------|---------------|
| **Estrategia** | Shared database, discriminator por org_id `[OBSERVADO]` |
| **Schemas** | auth, billing, chat, ai_gateway, partners, content, teams, analytics, audit `[OBSERVADO]` |
| **Isolamento** | Row-level via org_id em todas as queries `[PROPOSTO]` |
| **Personal org** | Cada user tem org pessoal auto-criada `[OBSERVADO]` |
| **Team orgs** | Criacao manual, convite de membros `[PROPOSTO]` |

## 7.6 Storage de Arquivos

| Aspecto | Implementacao | Confianca |
|---------|---------------|-----------|
| **Provider** | AWS S3 ou MinIO (self-hosted) | `[PROPOSTO]` |
| **Uso** | Uploads de chat (imagens, PDFs), avatares, exports | `[PROPOSTO]` |
| **Limite** | 10MB por arquivo (free), 100MB (pro) | `[PROPOSTO]` |
| **Seguranca** | Signed URLs com expiracao, virus scanning | `[PROPOSTO]` |

## 7.7 Busca e Indexacao

| Aspecto | Implementacao | Confianca |
|---------|---------------|-----------|
| **Full-text search** | PostgreSQL tsvector + GIN index | `[PROPOSTO]` |
| **Busca semantica** | Embeddings + pgvector (futuro) | `[PROPOSTO]` |
| **Indices** | B-tree para FKs, GIN para JSONB, GIN para tsvector | `[PROPOSTO]` |

## 7.8 Cache

| Camada | Tecnologia | TTL | Uso |
|--------|-----------|-----|-----|
| Session | Redis | 15min (access), 7d (refresh) | JWT sessions `[OBSERVADO]` |
| User profile | Redis | 5min | User data apos login `[PROPOSTO]` |
| Provider status | Redis | 1min | Health check results `[PROPOSTO]` |
| Model catalog | Redis | 5min | Lista de modelos/provedores `[PROPOSTO]` |
| Billing plans | Redis | 1h | Planos disponiveis `[PROPOSTO]` |
| Usage current | Redis | 1min | Saldo de creditos `[PROPOSTO]` |
| Analytics reports | Redis | 5min | Reports pre-computados `[PROPOSTO]` |

## 7.9 Filas e Jobs

| Job | Tecnologia | Trigger | Funcao | Confianca |
|-----|-----------|---------|--------|-----------|
| Email verification | Spring Events (in-memory) | UserRegisteredEvent | Enviar email de verificacao | `[OBSERVADO — event existe]` |
| Analytics flush | Frontend queue | Page hide / 8+ events | Enviar batch de eventos | `[OBSERVADO]` |
| Health check | Cron (a implementar) | A cada 60s | Verificar status de provedores | `[PROPOSTO]` |
| Usage metering | Async | Cada request de chat | Registrar consumo | `[PROPOSTO]` |
| Credit alerts | Async | Saldo abaixo de threshold | Notificar usuario | `[PROPOSTO]` |
| Data export | Async | Solicitacao do usuario | Gerar arquivo de export | `[PROPOSTO]` |
| Data erasure | Async | Solicitacao LGPD | Anonimizar/excluir dados | `[OBSERVADO — schema existe]` |

> **Nota:** Atualmente o backend usa Spring ApplicationEventPublisher (in-memory). Para producao, recomenda-se migrar para RabbitMQ ou Redis Streams para durabilidade. `[PROPOSTO]`

## 7.10 Notificacoes

| Canal | Implementacao | Confianca |
|-------|---------------|-----------|
| In-app toasts | Zustand toast-store, auto-dismiss 3.5s | `[OBSERVADO]` |
| Email | A implementar (SendGrid ou SES) | `[PROPOSTO]` |
| Push | A implementar (Web Push API) | `[PROPOSTO]` |
| Webhook | Endpoint para eventos de integracao | `[OBSERVADO]` |

## 7.11 Observabilidade

| Camada | Ferramenta | Status | Confianca |
|--------|-----------|--------|-----------|
| **Metricas** | Micrometer + Prometheus | Implementado | `[OBSERVADO]` |
| **Dashboards** | Grafana | A implementar | `[PROPOSTO]` |
| **Logs** | SLF4J + Logback (local) | Implementado | `[OBSERVADO]` |
| **Logs centralizados** | Loki ou ELK | A implementar | `[PROPOSTO]` |
| **Tracing** | OpenTelemetry + Jaeger | A implementar | `[PROPOSTO]` |
| **Health checks** | Spring Actuator (/health, /info) | Implementado | `[OBSERVADO]` |
| **Alertas** | Grafana Alerting ou PagerDuty | A implementar | `[PROPOSTO]` |
| **APM** | Micrometer AI routing telemetry | Implementado | `[OBSERVADO]` |

### Metricas AI ja implementadas `[OBSERVADO]`
- `ai.routing.attempts` — counter por modelo/provedor
- `ai.routing.success` — counter com tag fallback
- `ai.routing.failures` — counter com errorCode
- `ai.routing.attempts.before.success` — distribution summary
- `ai.guardrail.blocked` — counter por stage/motivo

## 7.12 Seguranca

| Aspecto | Implementacao | Confianca |
|---------|---------------|-----------|
| **CORS** | Whitelist de origens (localhost:3000, iaggregator.com.br) | `[OBSERVADO]` |
| **CSRF** | Desabilitado (API stateless, JWT) | `[OBSERVADO]` |
| **Session** | Stateless (JWT) | `[OBSERVADO]` |
| **Encryption at rest** | PostgreSQL TDE ou disk encryption | `[PROPOSTO]` |
| **Encryption in transit** | HTTPS (TLS 1.3) | `[PROPOSTO]` |
| **API key storage** | Hash com SHA-256, prefixo visivel | `[OBSERVADO]` |
| **BYOK storage** | AES-256-GCM encryption | `[PROPOSTO]` |
| **Rate limiting** | A implementar (Redis sliding window) | `[PROPOSTO]` |
| **Input sanitization** | Guardrails (prompt + output) | `[OBSERVADO]` |
| **Dependency scanning** | A implementar (Snyk ou Dependabot) | `[PROPOSTO]` |

## 7.13 Feature Flags

O schema `auth.feature_flags` ja existe no banco com suporte a: `[OBSERVADO]`
- Flag por nome, habilitado/desabilitado globalmente
- Habilitado por lista de users/orgs
- Habilitado por tier (plano)
- Rollout percentual
- Metadata JSONB

## 7.14 Resiliencia

| Pattern | Implementacao | Configuracao | Confianca |
|---------|---------------|-------------|-----------|
| **Circuit Breaker** | Resilience4j | 20 calls window, 50% threshold, 20s open, 5 half-open | `[OBSERVADO]` |
| **Retry** | Manual loop no ChatUseCase | 2 attempts, 250ms backoff | `[OBSERVADO]` |
| **Timeout** | HTTP client timeout | 15s por provedor | `[OBSERVADO]` |
| **Fallback** | ChatModelRoutingPolicy | preferred → default → fallback chain | `[OBSERVADO]` |
| **Bulkhead** | A implementar | Isolar pools por provedor | `[PROPOSTO]` |
| **Rate Limiter** | A implementar | Redis sliding window | `[PROPOSTO]` |

## 7.15 Ambientes e Deploy

| Ambiente | Uso | Confianca |
|----------|-----|-----------|
| **Local** | Desenvolvimento (Docker Compose) | `[OBSERVADO]` |
| **Staging** | Testes pre-producao | `[PROPOSTO]` |
| **Production** | Usuarios finais | `[PROPOSTO]` |

### CI/CD Proposto `[PROPOSTO]`
```
Push → GitHub Actions → Lint + Test + Build → Docker image → Push to Registry → Deploy to K8s/ECS
```

## 7.16 SLOs/SLAs Sugeridos

| Metrica | Free | Pro | Team | Enterprise |
|---------|------|-----|------|------------|
| Disponibilidade | Best effort | 99.5% | 99.9% | 99.95% |
| Latencia p95 (chat) | < 5s | < 3s | < 2s | < 1s |
| Latencia p95 (API) | < 500ms | < 200ms | < 100ms | < 50ms |
| Recovery time | N/A | < 4h | < 1h | < 15min |
| Data durability | N/A | 99.99% | 99.999% | 99.9999% |

`[PROPOSTO]`

---

# SECAO 8 — CONTRATOS DE API

## 8.1 Autenticacao

### API-AUTH-001: Registrar Usuario
| Campo | Valor |
|-------|-------|
| **Endpoint** | `POST /api/v1/auth/register` |
| **Autenticacao** | Nenhuma (publico) |
| **Objetivo** | Criar nova conta de usuario |
| **Confianca** | `[OBSERVADO]` |

**Request:**
```json
{
  "fullName": "string (required, 2-100 chars)",
  "email": "string (required, valid email)",
  "password": "string (required, min 8 chars)"
}
```

**Response 201:**
```json
{
  "status": "ok",
  "data": {
    "accessToken": "string (JWT, 15min)",
    "refreshToken": "string (JWT, 7d)",
    "expiresIn": 900,
    "tokenType": "Bearer"
  }
}
```

**Erros:**
| HTTP | Codigo | Descricao |
|------|--------|-----------|
| 400 | AUTH_001 | Campos invalidos |
| 409 | AUTH_002 | Email ja cadastrado |
| 500 | GEN_001 | Erro interno |

### API-AUTH-002: Login
| **Endpoint** | `POST /api/v1/auth/login` | Publico |

**Request:** `{ "email": "string", "password": "string" }`
**Response 200:** TokenResponse (accessToken, refreshToken, expiresIn)
**Erros:** 401 AUTH_003 (credenciais invalidas), 423 AUTH_004 (conta bloqueada), 403 AUTH_005 (conta inativa)

### API-AUTH-003: Refresh Token
| **Endpoint** | `POST /api/v1/auth/refresh` | Publico (requer refresh token) |

**Request:** `{ "refreshToken": "string" }`
**Response 200:** TokenResponse
**Erros:** 401 AUTH_006 (token invalido/expirado)

### API-AUTH-004: Obter Usuario Atual
| **Endpoint** | `GET /api/v1/auth/me` | Bearer Token obrigatorio |

**Response 200:** UserResponse (id, email, fullName, avatarUrl, role, status, locale, timezone, emailVerified)

---

## 8.2 Chat / AI Gateway

### API-CHAT-001: Enviar Mensagem (Chat Completion) `[OBSERVADO]`
| **Endpoint** | `POST /api/v1/ai/chat` | Bearer Token |

**Request:** `{ "prompt": "string (1-5000)", "preferredModel": "string (optional)" }`
**Response 200:** `{ "content": "string", "modelUsed": "string", "providerUsed": "string", "fallbackUsed": bool, "attempts": int }`
**Erros:** 400 GEN_002, 400 AI_004, 503 AI_001, 503 AI_002, 429 AI_003, 500 AI_005
**Eventos:** MessageSentEvent, ResponseReceivedEvent, ProviderFailoverEvent

### API-CHAT-002: Chat Completion com Streaming (SSE) `[PROPOSTO]`
| **Endpoint** | `POST /api/v1/ai/chat/stream` | Bearer Token | Content-Type: text/event-stream |

**SSE Events:** start (metadata), delta (content chunks), end (totalTokens, cost, latency)

---

## 8.3 Conversas `[PROPOSTO]`

| Endpoint | Metodo | Descricao |
|----------|--------|-----------|
| `/api/v1/conversations` | GET | Listar conversas (paginacao, filtros: search, model, pinned) |
| `/api/v1/conversations` | POST | Criar conversa |
| `/api/v1/conversations/:id` | GET | Obter conversa com mensagens |
| `/api/v1/conversations/:id` | PATCH | Atualizar (title, pinned, model) |
| `/api/v1/conversations/:id` | DELETE | Excluir (soft delete) |
| `/api/v1/conversations/:id/messages` | DELETE | Limpar mensagens |

---

## 8.4 Catalogo de Provedores `[PROPOSTO]`

| Endpoint | Metodo | Descricao |
|----------|--------|-----------|
| `/api/v1/providers` | GET | Listar provedores com status |
| `/api/v1/providers/:id/models` | GET | Modelos de um provedor |
| `/api/v1/providers/:id/health` | GET | Health check de um provedor |

---

## 8.5 Credenciais BYOK `[PROPOSTO]`

| Endpoint | Metodo | Descricao |
|----------|--------|-----------|
| `/api/v1/credentials` | GET | Listar credenciais (prefixo apenas) |
| `/api/v1/credentials` | POST | Adicionar credencial (valida + criptografa) |
| `/api/v1/credentials/:id/test` | POST | Testar credencial |
| `/api/v1/credentials/:id` | DELETE | Revogar credencial |

---

## 8.6 Billing e Creditos `[OBSERVADO parcial]`

| Endpoint | Metodo | Descricao |
|----------|--------|-----------|
| `/api/v1/billing/plans` | GET | Planos disponiveis (publico) |
| `/api/v1/billing/usage/current` | GET | Uso do periodo atual |
| `/api/v1/billing/usage` | GET | Historico com filtros (from, to, groupBy) |
| `/api/v1/billing/usage/weekly` | GET | Uso semanal |
| `/api/v1/credits` | GET | Saldo de creditos |
| `/api/v1/credits/purchase` | POST | Comprar creditos |

---

## 8.7 Analytics `[OBSERVADO]`

| Endpoint | Metodo | Descricao |
|----------|--------|-----------|
| `/api/v1/analytics/events` | POST | Ingerir batch de eventos (publico) |
| `/api/v1/analytics/reports` | GET | Listar reports com paginacao |
| `/api/v1/analytics/reports/:id/events` | GET | Eventos de um report |

---

## 8.8 Metricas `[PROPOSTO]`

| Endpoint | Metodo | Descricao |
|----------|--------|-----------|
| `/api/v1/metrics/providers` | GET | Metricas por provedor (latencia, erro, custo) |
| `/api/v1/metrics/models` | GET | Metricas por modelo |
| `/api/v1/metrics/dashboard` | GET | Dashboard consolidado |

---

## 8.9 Playground `[PROPOSTO]`

| Endpoint | Metodo | Descricao |
|----------|--------|-----------|
| `/api/v1/playground/compare` | POST | Enviar prompt para N modelos simultaneamente |
| `/api/v1/playground/history` | GET | Historico de comparacoes |

---

## 8.10 Codex (Tasks) `[OBSERVADO]`

| Endpoint | Metodo | Descricao |
|----------|--------|-----------|
| `/api/tasks` | GET/POST | Listar/criar tarefas |
| `/api/tasks/:id` | GET/PATCH/DELETE | CRUD de tarefa |
| `/api/tasks/:id/logs` | GET | Logs de execucao |
| `/api/tasks/:id/diff` | GET | Diff de codigo |
| `/api/tasks/:id/tests` | GET | Resultados de teste |
| `/api/tasks/:id/artifacts` | GET | Artefatos gerados |
| `/api/tasks/:id/cancel` | POST | Cancelar tarefa |
| `/api/tasks/:id/retry` | POST | Retentar tarefa |
| `/api/tasks/:id/pull-requests` | GET/POST | PRs da tarefa |

---

# SECAO 9 — MODELO DE DADOS E BANCO

## 9.1 Schemas do PostgreSQL

| Schema | Responsabilidade | Status |
|--------|-----------------|--------|
| auth | Usuarios, orgs, sessoes, chaves, consent, feature flags | `[OBSERVADO]` |
| chat | Conversas e mensagens (server-side) | `[PROPOSTO]` |
| ai_gateway | Provedores, modelos, credenciais BYOK, routing | `[PROPOSTO]` |
| billing | Planos, assinaturas, creditos, uso, invoices | `[PROPOSTO]` |
| content | Prompts, templates | `[PROPOSTO]` |
| codex | Tasks, logs, artifacts, environments, PRs | `[OBSERVADO]` |
| teams | Membros e convites | `[PROPOSTO]` |
| integrations | Conexoes e webhooks | `[OBSERVADO]` |
| analytics | Event reports e ingested events | `[OBSERVADO]` |
| audit | Logs de auditoria | `[PROPOSTO]` |

## 9.2 DDL das Tabelas Principais

### auth.users `[OBSERVADO]`
```sql
CREATE TABLE auth.users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  personal_org_id UUID REFERENCES auth.organizations(id),
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255),
  full_name VARCHAR(200) NOT NULL,
  display_name VARCHAR(100),
  avatar_url TEXT,
  role VARCHAR(20) NOT NULL DEFAULT 'user',
  status VARCHAR(30) NOT NULL DEFAULT 'pending_verification',
  last_login_at TIMESTAMPTZ,
  failed_login_count INT DEFAULT 0,
  locked_until TIMESTAMPTZ,
  email_verified BOOLEAN DEFAULT FALSE,
  metadata JSONB DEFAULT '{}',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);
```

### chat.conversations `[PROPOSTO]`
```sql
CREATE TABLE chat.conversations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id),
  org_id UUID NOT NULL REFERENCES auth.organizations(id),
  title VARCHAR(200) DEFAULT 'Nova conversa',
  model VARCHAR(100),
  pinned BOOLEAN DEFAULT FALSE,
  message_count INT DEFAULT 0,
  total_tokens INT DEFAULT 0,
  last_message_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMPTZ
);
CREATE INDEX idx_conv_user ON chat.conversations(user_id);
CREATE INDEX idx_conv_updated ON chat.conversations(updated_at DESC);
```

### chat.messages `[PROPOSTO]`
```sql
CREATE TABLE chat.messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id UUID NOT NULL REFERENCES chat.conversations(id) ON DELETE CASCADE,
  role VARCHAR(20) NOT NULL CHECK (role IN ('user','assistant','error','system')),
  content TEXT NOT NULL,
  model_used VARCHAR(100),
  provider_used VARCHAR(50),
  fallback_used BOOLEAN DEFAULT FALSE,
  tokens_input INT,
  tokens_output INT,
  cost DECIMAL(10,6),
  latency_ms INT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_msg_conv ON chat.messages(conversation_id);
```

### ai_gateway.providers `[PROPOSTO]`
```sql
CREATE TABLE ai_gateway.providers (
  id VARCHAR(50) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  base_url TEXT NOT NULL,
  status VARCHAR(20) DEFAULT 'active',
  capabilities TEXT[] DEFAULT '{}',
  avg_latency_ms INT,
  uptime_percent DECIMAL(5,2),
  last_health_check TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

### ai_gateway.models `[PROPOSTO]`
```sql
CREATE TABLE ai_gateway.models (
  id VARCHAR(100) PRIMARY KEY,
  provider_id VARCHAR(50) REFERENCES ai_gateway.providers(id),
  name VARCHAR(100) NOT NULL,
  modalities TEXT[] DEFAULT '{"text"}',
  max_context_tokens INT,
  max_output_tokens INT,
  input_price_per_m DECIMAL(10,4),
  output_price_per_m DECIMAL(10,4),
  supports_streaming BOOLEAN DEFAULT TRUE,
  supports_vision BOOLEAN DEFAULT FALSE,
  status VARCHAR(20) DEFAULT 'available',
  created_at TIMESTAMPTZ DEFAULT NOW()
);
```

### billing.usage_records `[PROPOSTO]`
```sql
CREATE TABLE billing.usage_records (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id),
  org_id UUID NOT NULL REFERENCES auth.organizations(id),
  model VARCHAR(100) NOT NULL,
  provider VARCHAR(50) NOT NULL,
  tokens_input INT DEFAULT 0,
  tokens_output INT DEFAULT 0,
  cost_usd DECIMAL(10,6) DEFAULT 0,
  is_byok BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_usage_user ON billing.usage_records(user_id);
CREATE INDEX idx_usage_created ON billing.usage_records(created_at);
```

## 9.3 Diagrama Relacional

```
users 1──N sessions          users 1──N conversations
users 1──N api_keys          conversations 1──N messages
users 1──1 organizations     users 1──N usage_records
organizations 1──N subscriptions
providers 1──N models        users 1──N user_credentials
providers 1──N user_credentials
plans 1──N subscriptions     event_reports 1──N ingested_events
```

## 9.4 Estrategia de Migracao

Usar Flyway (ja configurado) com convencao: `[OBSERVADO]`
- `V1__initial_setup.sql`
- `V2__auth_module.sql` (existente)
- `V4__analytics_ingestion.sql` (existente)
- `V5__chat_conversations.sql` (novo)
- `V6__ai_gateway_providers.sql` (novo)
- `V7__billing_subscriptions.sql` (novo)
- `V8__content_prompts.sql` (novo)
- `V9__audit_logs.sql` (novo)

---

# SECAO 10 — MATRIZ DE RASTREABILIDADE

| ID | Funcionalidade Frontend | Regra de Negocio | Servico Backend | Endpoint(s) | Evento(s) | Job/Fila | Tabela(s) | Cache/Index | Observabilidade | Testes |
|----|------------------------|-----------------|----------------|-------------|-----------|---------|-----------|-------------|----------------|--------|
| F-AUTH-001 | Registro de usuario | Email unico, BCrypt, status pending | RegisterUserUseCase | POST /auth/register | UserRegisteredEvent | Email verification (async) | auth.users, auth.email_verification_tokens | N/A | Registros/dia, taxa erro | Unit+Integ+E2E |
| F-AUTH-002 | Login | Max 5 tentativas, bloqueio 30min | LoginUseCase | POST /auth/login | UserLoggedInEvent | N/A | auth.users, auth.sessions | Session Redis | Logins/min, falhas, bloqueios | Unit+Integ+E2E |
| F-AUTH-003 | Refresh token | Single-use, 7d validade | RefreshTokenUseCase | POST /auth/refresh | TokenRefreshedEvent | N/A | auth.sessions | N/A | Refreshes/min | Unit+Integ |
| F-AUTH-004 | Get current user | Token valido | GetCurrentUserUseCase | GET /auth/me | N/A | N/A | auth.users | User cache 5min | N/A | Unit+Integ |
| F-AUTH-005 | Logout | Revogar session | LogoutUseCase | POST /auth/logout | UserLoggedOutEvent | N/A | auth.sessions | Invalidar cache | N/A | Unit+Integ |
| F-AUTH-006 | OAuth GitHub | Vincular/criar user | OAuthUseCase | POST /oauth/github/connect | UserRegisteredEvent | N/A | auth.users, auth.user_identities | N/A | OAuth/dia | Unit+Integ+E2E |
| F-CHAT-001 | Enviar mensagem | Guardrails, fallback, max 5000 chars | ChatUseCase | POST /ai/chat | MessageSent, ResponseReceived, Failover | Usage metering (async) | chat.messages, billing.usage_records | Response cache opt. | Latencia p50/p95, erros/provedor | Unit+Integ+E2E+Load |
| F-CHAT-002 | Selecionar modelo | Modelo no catalogo | N/A (frontend) | N/A | chat_change_model | N/A | N/A (localStorage) | N/A | Trocas/dia | Unit |
| F-CHAT-003 | Criar conversa | Auto-title apos 1a msg | ConversationService | POST /conversations | ConversationCreated | N/A | chat.conversations | N/A | Conversas/dia | Unit |
| F-CHAT-004 | Parar geracao | AbortController | N/A | Abort signal | chat_stop_generation | N/A | N/A | N/A | Stops/dia | Unit |
| F-CHAT-005 | Buscar conversas | Case-insensitive match | N/A (frontend) | N/A | chat_search | N/A | N/A (localStorage) | N/A | Buscas/dia | Unit |
| F-CHAT-006 | Renomear conversa | Max 100 chars | ConversationService | PATCH /conversations/:id | ConversationRenamed | N/A | chat.conversations | N/A | N/A | Unit |
| F-CHAT-007 | Fixar conversa | Toggle pinned | ConversationService | PATCH /conversations/:id | N/A | N/A | chat.conversations | N/A | N/A | Unit |
| F-CHAT-008 | Excluir conversa | Soft delete | ConversationService | DELETE /conversations/:id | ConversationDeleted | N/A | chat.conversations | N/A | N/A | Unit |
| F-CHAT-009 | Limpar mensagens | Preserva conversa | ConversationService | DELETE /conversations/:id/messages | N/A | N/A | chat.messages | N/A | N/A | Unit |
| F-CHAT-010 | Copiar mensagem | Clipboard API | N/A (frontend) | N/A | N/A | N/A | N/A | N/A | N/A | Unit |
| F-CHAT-011 | Copiar codigo | Clipboard API | N/A (frontend) | N/A | N/A | N/A | N/A | N/A | N/A | Unit |
| F-CHAT-012 | Fallback provedores | Chain routing, circuit breaker | ChatUseCase, RoutingPolicy | POST /ai/chat | ProviderFailoverEvent | N/A | N/A | Circuit breaker stats | Fallbacks/dia, sucesso/provedor | Unit+Integ |
| F-THEME-001 | Alternar tema | Light/dark/system | N/A (frontend) | N/A | theme_change | N/A | N/A (localStorage) | N/A | N/A | Unit |
| F-CDX-001 | Criar task | Ambiente valido, creditos | TaskService | POST /tasks | TaskCreatedEvent | Task execution (async) | codex.tasks | N/A | Tasks/dia, tempo exec | Unit+Integ+E2E |
| F-CDX-002 | Ver logs | Owner-only | TaskService | GET /tasks/:id/logs | N/A | N/A | codex.task_logs | N/A | N/A | Unit+Integ |
| F-CDX-003 | Ver diff | Apos execucao | TaskService | GET /tasks/:id/diff | N/A | N/A | codex.task_artifacts | N/A | N/A | Unit+Integ |
| F-CDX-004 | Criar PR | GitHub ativo | TaskService, GitHub | POST /tasks/:id/pull-requests | PullRequestCreated | N/A | codex.pull_requests | N/A | PRs/dia | Unit+Integ+E2E |
| F-INT-001 | Conectar GitHub | OAuth scopes | IntegrationService | POST /integrations/github/install | GitHubConnected | N/A | integrations.connections | Token cache | Integracoes ativas | Unit+Integ+E2E |
| F-BILL-001 | Ver planos | Publico | BillingService | GET /billing/plans | N/A | N/A | billing.plans | Cache 1h | N/A | Unit+Integ |
| F-BILL-002 | Ver uso | Por periodo/modelo | BillingService | GET /billing/usage | N/A | N/A | billing.usage_records | Cache 5min | Queries/dia | Unit+Integ |
| F-BILL-003 | Comprar creditos | Stripe checkout | BillingService | POST /credits/purchase | CreditsPurchased | Webhook processing | billing.credits, billing.transactions | Saldo cache 1min | Compras/dia, receita | Unit+Integ+E2E |
| F-ANA-001 | Enviar eventos | Batch max 8 | AnalyticsService | POST /analytics/events | N/A | N/A | analytics.event_reports, analytics.ingested_events | N/A | Eventos/min | Unit+Integ |
| F-AGG-001 | Catalogo provedores | Health check 60s | ProviderRegistryService | GET /providers | N/A | Health check (cron) | ai_gateway.providers, ai_gateway.models | Cache 5min | Consultas/dia | Unit+Integ |
| F-AGG-002 | BYOK | AES-256, validacao | CredentialService | CRUD /credentials | CredentialAdded | N/A | ai_gateway.user_credentials | N/A | N/A | Unit+Integ+Security |
| F-AGG-003 | Playground | Max 4 modelos | PlaygroundService | POST /playground/compare | PlaygroundComparison | N/A | ai_gateway.playground_sessions | N/A | Comparacoes/dia | Unit+Integ+E2E |
| F-AGG-004 | Config roteamento | Por org/user | RoutingPolicyService | CRUD /routing/policies | PolicyUpdated | N/A | ai_gateway.routing_policies | Policy cache 1min | N/A | Unit+Integ |
| F-AGG-005 | Dashboard metricas | Prometheus data | MetricsService | GET /metrics/* | N/A | N/A | Prometheus + snapshots | Cache 1min | Self-observing | Unit+Integ |

**Resultado:** Zero lacunas. Toda funcionalidade frontend possui backend, endpoint, persistencia e observabilidade definidos.

---

# SECAO 11 — DESIGN SYSTEM ORIGINAL DA LUME

## 11.1 Posicionamento da Marca

**Conceito:** "Lume" vem do latim *lumen* (luz). A marca evoca clareza, iluminacao e inteligencia — a ideia de que a plataforma **ilumina o caminho** para a melhor IA disponivel.

**Atributos da marca:**
- **Moderna** — Interface limpa, sem ruido visual
- **Elegante** — Premium sem ser exclusiva
- **Acessivel** — Funciona para todos, de desenvolvedores a executivos
- **Confiavel** — Robusta, segura, previsivel
- **Inteligente** — Roteamento automatico, sugestoes contextuais

**Personalidade:** Sofisticada porem acessivel. Como um consultor experiente que fala de forma clara e direta. Nao e fria ou corporativa demais — tem calor humano, como a luz que ilumina.

## 11.2 Logotipo

### Conceito do Logo
O logotipo da Lume combina um **wordmark tipografico** com um **simbolo de luz minimalista** — uma forma abstrata que remete a uma chama ou feixe de luz, representando iluminacao e inteligencia.

### SVG do Logotipo (Minimalista)
```svg
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 32" fill="none">
  <!-- Simbolo: chama/luz abstrata -->
  <path d="M8 28C8 28 4 20 4 14C4 8 8 4 12 4C10 8 14 12 14 16C14 20 12 24 8 28Z"
        fill="url(#lume-gradient)" />
  <path d="M16 28C16 28 20 20 20 14C20 8 16 4 12 4C14 8 10 12 10 16C10 20 12 24 16 28Z"
        fill="url(#lume-gradient)" opacity="0.7" />
  <!-- Wordmark -->
  <text x="28" y="24" font-family="-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif"
        font-size="22" font-weight="600" letter-spacing="-0.02em" fill="currentColor">
    Lume
  </text>
  <defs>
    <linearGradient id="lume-gradient" x1="12" y1="4" x2="12" y2="28">
      <stop offset="0%" stop-color="#F59E0B" />
      <stop offset="100%" stop-color="#D97706" />
    </linearGradient>
  </defs>
</svg>
```

### Variacoes
- **Full color:** Simbolo gradient amber + wordmark escuro
- **Monocromatico:** Tudo em foreground
- **Icone:** Apenas simbolo (para favicon, app icon)
- **Dark mode:** Simbolo amber + wordmark claro

### Favicon/App Icon
Simbolo da chama em 32x32, simplificado para leitura em tamanho pequeno.

## 11.3 Tokens de Cor

### Paleta Principal

A paleta da Lume e baseada em **tons quentes de luz** (amber/gold) como acento, com neutros sofisticados para o corpo da interface. Isso diferencia claramente da paleta terracota/salmon do Claude.ai.

**Light Mode:**
```css
:root {
  /* Backgrounds */
  --background: #FAFAF8;         /* Neutro quente claro */
  --surface: #FFFFFF;             /* Cards, inputs */
  --surface-sidebar: #F5F4F0;    /* Sidebar */
  --surface-hover: #F0EFE9;      /* Hover states */
  --surface-active: #EAE9E3;     /* Active/selected */

  /* Foregrounds */
  --foreground: #1A1A18;          /* Texto primario */
  --foreground-secondary: #6B6B63; /* Texto secundario */
  --muted-foreground: #9B9B91;    /* Texto muted */
  --subtle-foreground: #B8B8AE;   /* Placeholders */

  /* Accent (Amber/Gold) */
  --accent: #D97706;              /* Cor principal da marca */
  --accent-light: #F59E0B;        /* Hover, highlights */
  --accent-subtle: #FEF3C7;       /* Backgrounds sutis */
  --primary: #1A1A18;             /* Botoes primarios (escuro) */
  --primary-foreground: #FAFAF8;  /* Texto em botoes primarios */

  /* Borders */
  --border: #E5E5DD;
  --border-strong: #D4D4CC;

  /* Semantic */
  --success: #16A34A;
  --warning: #D97706;
  --error: #DC2626;
  --info: #2563EB;
}
```

**Dark Mode:**
```css
[data-theme="dark"] {
  --background: #1A1A18;
  --surface: #242422;
  --surface-sidebar: #1E1E1C;
  --surface-hover: #2A2A28;
  --surface-active: #333331;

  --foreground: #FAFAF8;
  --foreground-secondary: #B8B8AE;
  --muted-foreground: #8A8A80;
  --subtle-foreground: #6B6B63;

  --accent: #F59E0B;
  --accent-light: #FBBF24;
  --accent-subtle: #78350F;
  --primary: #FAFAF8;
  --primary-foreground: #1A1A18;

  --border: #333331;
  --border-strong: #444442;
}
```

## 11.4 Tipografia

| Nivel | Tamanho | Peso | Line Height | Uso |
|-------|---------|------|-------------|-----|
| Display | clamp(1.875rem, 1.2rem + 2vw, 2.5rem) | 290 | 1.5 | Greeting |
| H1 | 24px | 600 | 1.3 | Titulos de pagina |
| H2 | 18px | 600 | 1.4 | Subtitulos |
| H3 | 16px | 600 | 1.4 | Section headers |
| Body | 16px | 400 | 1.5 | Texto padrao, chat |
| Body small | 14px | 400 | 1.5 | Textos secundarios |
| Caption | 13px | 400 | 1.4 | Botoes, labels, nav |
| Tiny | 12px | 400/500 | 1.3 | Nav items, timestamps |
| Micro | 11px | 500 | 1.2 | Group labels, badges |

**Font stack:** `-apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, Roboto, 'Helvetica Neue', Arial, sans-serif`
**Monospace:** `'Geist Mono', 'SF Mono', 'Fira Code', 'Cascadia Code', monospace`

## 11.5 Spacing

Escala baseada em **4px**:
`0, 1(4px), 1.5(6px), 2(8px), 2.5(10px), 3(12px), 3.5(14px), 4(16px), 5(20px), 6(24px), 7(28px), 8(32px), 10(40px), 12(48px), 16(64px)`

## 11.6 Border Radius

| Token | Valor | Uso |
|-------|-------|-----|
| --radius-sm | 4px | Badges, small elements |
| --radius-md | 6px | Botoes, nav items |
| --radius-lg | 8px | Quick action chips, cards |
| --radius-xl | 12px | Modais, panels |
| --radius-2xl | 16px | Large cards |
| --radius-input | 20px | Input containers |
| --radius-full | 9999px | Avatares, pills |

## 11.7 Shadows

```css
--input-shadow: 0 0 0 0.8px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04);
--input-shadow-hover: 0 0 0 0.8px rgba(0,0,0,0.12), 0 2px 4px rgba(0,0,0,0.06);
--input-shadow-focus: 0 0 0 2px var(--accent), 0 0 0 4px rgba(217,119,6,0.15);
--card-shadow: 0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04);
--modal-shadow: 0 20px 60px rgba(0,0,0,0.15);
```

## 11.8 Iconografia

- **Biblioteca:** Lucide React (open-source, consistente, 24x24 grid)
- **Tamanhos padrao:** h-3.5 w-3.5 (menu items), h-4 w-4 (nav, actions), h-[18px] w-[18px] (input controls)
- **Stroke width:** 1.5 (default), 2.5 (emphasis, ex: send button)

## 11.9 Principios de UX

1. **Clareza** — Cada elemento tem um proposito claro
2. **Velocidade percebida** — Streaming, skeleton loading, optimistic updates
3. **Consistencia** — Mesmos padroes em toda a aplicacao
4. **Acessibilidade** — WCAG 2.1 AA, keyboard navigable, screen reader friendly
5. **Minimal friction** — Menos cliques para acoes comuns
6. **Progressive disclosure** — Complexidade revelada conforme necessario
7. **Feedback constante** — Toda acao tem resposta visual imediata

## 11.10 Guidelines de Acessibilidade

- Contraste minimo: 4.5:1 (texto), 3:1 (elementos grandes) — WCAG AA
- Todos os botoes tem `aria-label`
- Focus trap em modais
- Keyboard shortcuts: Ctrl+B (sidebar), Enter (enviar), Shift+Enter (nova linha), Escape (fechar modal/overlay)
- Skip links para navegacao
- Roles semanticos em landmarks

---

# SECAO 12 — FRONTEND PROPOSTO DA LUME

## 12.1 Sitemap Completo

```
Lume Frontend
+-- / (redirect → /chat)
+-- /chat (Chat principal + empty state)
+-- /library (Biblioteca de conversas)
+-- /prompts (Templates de prompts)
+-- /playground (Comparacao de modelos) [PROPOSTO]
+-- /providers (Catalogo de provedores/modelos) [PROPOSTO]
+-- /settings
|   +-- /settings/profile (Perfil do usuario)
|   +-- /settings/keys (BYOK credentials) [PROPOSTO]
|   +-- /settings/routing (Politicas de roteamento) [PROPOSTO]
|   +-- /settings/analytics (Dashboard de analytics)
+-- /billing (Planos, uso, creditos)
+-- /welcome (Onboarding)
+-- /codex (Task automation + sub-rotas)
+-- /admin/settings (Admin)
```

## 12.2 Padroes de Layout

| Tipo de Pagina | Layout | Exemplo |
|---------------|--------|---------|
| Chat | Sidebar + Content (full height, no scroll on content wrapper) | /chat |
| List/Grid | Header + Filters + Grid/List + Pagination | /library, /prompts |
| Settings | Sidebar nav + Content panel | /settings/* |
| Form | Card centered com campos | /settings/keys/new |
| Dashboard | Header + Grid of metric cards + Charts | /settings/analytics |
| Detail | Header + Tabs + Content panels | /codex/tasks/[id] |

## 12.3 Guidelines de Copy (PT-BR)

- **Tom:** Profissional mas acolhedor. Nao excessivamente formal.
- **Saudacao:** "Bom dia/Boa tarde/Boa noite, {nome}"
- **Botoes:** Verbos no infinitivo ou presente ("Enviar", "Criar", "Salvar")
- **Erros:** Claros e actionable ("Nao foi possivel enviar. Verifique sua conexao.")
- **Empty states:** Encorajadores ("Nenhuma conversa ainda. Comece uma agora!")
- **Confirmacoes:** Explicitas ("Tem certeza que deseja excluir esta conversa?")
- **Loading:** Sem texto, apenas skeleton ou spinner

## 12.4 Guidelines de Feedback Visual

| Acao | Feedback |
|------|----------|
| Enviar mensagem | Input limpa + mensagem aparece + streaming inicia |
| Erro de envio | Toast vermelha + mensagem de erro na bolha |
| Copiar texto | Icone muda para check + "Copiado!" |
| Excluir conversa | Confirmacao → toast de sucesso → item some |
| Trocar tema | Transicao suave de cores (35ms) |
| Hover em botao | Background sutil + cursor pointer |
| Focus em input | Shadow accent (amber glow) |

## 12.5 Responsividade

| Breakpoint | Largura | Comportamento |
|-----------|---------|---------------|
| Mobile | < 768px | Sidebar overlay, single-column layout, bottom nav |
| Tablet | 768-1279px | Sidebar colapsavel, content flexivel |
| Desktop | >= 1280px | Sidebar fixa 288px, content max-width var(--chat-max-width) |

---

# SECAO 13 — STACK SUGERIDA

## 13.1 Stack Implementada (Observada)

| Camada | Tecnologia | Versao | Justificativa |
|--------|-----------|--------|---------------|
| **Frontend Framework** | Next.js | 15.1 | App Router, RSC, API routes como BFF, otimizacoes de performance |
| **UI Library** | React | 19.0 | Concurrent features, Server Components |
| **Linguagem Frontend** | TypeScript | 5.7 | Type safety, DX, refactoring |
| **Estilizacao** | Tailwind CSS | 4.0 | Utility-first, design tokens via CSS vars, tree-shaking |
| **Estado Cliente** | Zustand | 5.0 | Leve, persist middleware, sem boilerplate |
| **Estado Servidor** | React Query | 5.x | Cache, refetch, optimistic updates |
| **Formularios** | react-hook-form + zod | 7.x + 3.x | Performatico, validacao type-safe |
| **Backend Framework** | Spring Boot | 3.4.3 | Enterprise-grade, ecossistema maduro, observabilidade |
| **Linguagem Backend** | Java | 21 | Virtual threads, pattern matching, records |
| **Banco** | PostgreSQL | 16 | ACID, JSONB, tsvector, extensivel |
| **Cache** | Redis | 7 | Sessions, cache, rate limiting, pub/sub |
| **Migrations** | Flyway | 10.20 | Versioned, repeatable, Java-native |
| **Auth** | JWT + BCrypt | jjwt 0.12 | Stateless, escalavel |
| **Resiliencia** | Resilience4j | 2.2 | Circuit breaker, retry, rate limiter |
| **Metricas** | Micrometer | 1.13 | Prometheus-compatible, Actuator |
| **API Docs** | SpringDoc | 2.6 | OpenAPI 3.0 auto-generated |
| **Testes** | Jest + Playwright + JUnit + TestContainers | Various | Unit, integration, E2E, architecture |
| **Build Frontend** | npm | 10.x | Package management |
| **Build Backend** | Maven | 3.9 | Dependency management, multi-module |

## 13.2 Adicoes Propostas

| Camada | Tecnologia | Justificativa | Prioridade |
|--------|-----------|---------------|-----------|
| **File Storage** | AWS S3 / MinIO | Upload de arquivos no chat, avatares, exports | P1 |
| **Payment** | Stripe | Subscriptions, checkout, invoicing, webhooks | P1 |
| **Email** | SendGrid ou AWS SES | Email verification, alertas, billing | P1 |
| **Dashboards** | Grafana | Visualizacao de metricas Prometheus | P2 |
| **Logs centralizados** | Grafana Loki | Agregacao e busca de logs | P2 |
| **Tracing** | OpenTelemetry + Jaeger | Distributed tracing entre services | P2 |
| **Product Analytics** | PostHog ou Mixpanel | Funnel analysis, retention, cohorts | P2 |
| **Feature Flags** | Usar tabela existente + cache | Progressive rollout, A/B testing | P2 |
| **Rate Limiting** | Redis sliding window (Spring) | Protecao contra abuso | P1 |
| **Message Queue** | RabbitMQ ou Redis Streams | Durabilidade de eventos async | P2 |
| **Search** | pgvector (PostgreSQL extension) | Busca semantica em conversas/prompts | P3 |
| **CDN** | CloudFront ou Cloudflare | Static assets, images, global distribution | P2 |
| **Container** | Docker + Docker Compose | Dev local, CI/CD, deploy | P1 |
| **Orchestration** | Kubernetes (EKS) ou ECS | Producao, auto-scaling | P2 |
| **CI/CD** | GitHub Actions | Build, test, deploy automation | P1 |

---

# SECAO 14 — PLANO DE IMPLEMENTACAO

## Fase 0: Descoberta e Arquitetura (CONCLUIDA)
**Duracao:** 2 semanas
**Entregaveis:**
- Este documento de especificacao
- Inventario completo do codebase
- Decisoes de arquitetura documentadas

**Status:** `CONCLUIDO`

---

## Fase 1: Fundacao Tecnica
**Duracao:** 3-4 semanas

**Escopo:**
1. Persistencia server-side de conversas (chat.conversations + chat.messages)
2. Streaming real via SSE (substituir simulacao de streaming)
3. API de catalogo de provedores/modelos dinamico
4. Docker Compose para ambiente local completo
5. CI/CD basico (GitHub Actions: lint + test + build)
6. Rate limiting com Redis

**Entregaveis:**
- Migration V5 (chat schema)
- Migration V6 (ai_gateway schema)
- ConversationService + ConversationController
- SSE streaming endpoint
- ProviderRegistryService
- Docker Compose (PostgreSQL + Redis + Backend + Frontend)
- GitHub Actions workflow

**Dependencias:** Nenhuma (base)
**Riscos:** SSE pode exigir refactoring significativo no ChatUseCase
**Criterios de pronto:**
- Conversas persistem no servidor
- Streaming real funciona end-to-end
- Catalogo de provedores acessivel via API
- `docker compose up` sobe ambiente completo
- CI pipeline verde

**Testes:**
- Unit: ConversationService, ProviderRegistry
- Integration: Chat flow com TestContainers
- E2E: Playwright — criar conversa, enviar mensagem, ver resposta streaming

---

## Fase 2: Modulos Core
**Duracao:** 4-6 semanas

**Escopo:**
1. BYOK (credenciais do usuario)
2. Playground comparativo
3. Dashboard de metricas por provedor
4. Gestao de prompts/templates
5. Billing foundation (planos + Stripe integration)
6. Health check periodico de provedores

**Entregaveis:**
- CredentialService + BYOK UI
- PlaygroundService + Comparacao lado a lado
- MetricsService + Dashboard Grafana basico
- PromptService + CRUD UI
- Migration V7 (billing schema)
- Stripe integration (checkout, subscription, webhook)
- Provider health check cron job

**Dependencias:** Fase 1 (persistencia, catalogo)
**Riscos:**
- Complexidade de criptografia BYOK
- Stripe integration testavel apenas com sandbox
- Playground com N chamadas simultaneas pode gerar custos altos em dev

**Criterios de pronto:**
- Usuario consegue salvar/usar chave BYOK
- Playground compara 2-4 modelos lado a lado
- Dashboard mostra latencia/erro por provedor
- Templates CRUD funcional
- Checkout Stripe funciona em sandbox

---

## Fase 3: Colaboracao e Integracoes
**Duracao:** 4-5 semanas

**Escopo:**
1. Organizations e Teams (multi-tenant real)
2. Membros, convites, roles
3. Conversas compartilhadas na org
4. Integracoes (GitHub, Slack, Linear — ja tem estrutura)
5. Webhooks para eventos da plataforma
6. Upload de arquivos (S3/MinIO)

**Entregaveis:**
- TeamService + convites + roles UI
- Shared conversations within org
- Webhook system para eventos
- File upload/download service (S3)
- Migration V8 (teams schema completo)

**Dependencias:** Fase 2 (billing para planos team)
**Riscos:**
- Multi-tenant row-level filtering e complexo
- Compartilhamento de conversas requer permissao granular

---

## Fase 4: Billing, Admin, Compliance
**Duracao:** 3-4 semanas

**Escopo:**
1. Billing completo (metering real, invoices, creditos)
2. Admin panel (gestao de users, orgs, feature flags)
3. LGPD compliance (export, erasure, consent management)
4. Auditoria completa
5. Planos e limites enforced

**Entregaveis:**
- Usage metering real por request
- Invoice generation via Stripe
- Admin dashboard
- Data export endpoint
- Data erasure workflow
- Audit log em todas as acoes criticas

**Dependencias:** Fase 3 (teams, multi-tenant)
**Riscos:**
- Metering preciso requer instrumentacao em todos os pontos de entrada
- LGPD erasure com integridade referencial e complexo

---

## Fase 5: Hardening, Escala e Observabilidade
**Duracao:** 3-4 semanas

**Escopo:**
1. Grafana dashboards completos
2. Loki para logs centralizados
3. OpenTelemetry tracing
4. Load testing (k6 ou Artillery)
5. Security hardening (OWASP, dependency scanning)
6. Performance optimization (caching, CDN, lazy loading)
7. Kubernetes deployment configs
8. Disaster recovery plan

**Entregaveis:**
- Grafana dashboards por dominio
- Loki integration
- k6 load test scripts
- Security audit report
- K8s manifests + Helm charts
- DR runbook

**Dependencias:** Fase 4 (tudo funcional)
**Riscos:**
- Tuning de performance requer dados de producao
- K8s setup pode ser complexo para equipe pequena (considerar ECS como alternativa)

---

# SECAO 15 — LACUNAS, INFERENCIAS E RISCOS

## 15.1 Itens Observados Diretamente

| Item | Fonte | Status |
|------|-------|--------|
| 18 provedores de IA implementados no backend | Codebase backend | Funcional |
| 14 modelos no catalogo frontend | model-catalog.ts | Funcional |
| JWT auth com BCrypt e brute-force protection | AuthController + domain | Funcional |
| Circuit breaker Resilience4j por provedor | application.yml | Configurado |
| Guardrails de prompt e output | ConfigurableGuardrailAdapters | Funcional |
| Roteamento com fallback chain | ChatModelRoutingPolicy | Funcional |
| Telemetria Micrometer | MicrometerAiRoutingTelemetryAdapter | Funcional |
| Zustand stores com persist | chat-store, auth-store, theme-store | Funcional |
| 50+ API routes no frontend BFF | app/api/ directory | Funcional |
| Flyway migrations para auth e analytics | db/migration/ | Aplicadas |
| Feature flags schema | auth.feature_flags | Schema criado |
| Consent e erasure schemas | auth.consent_records, auth.erasure_requests | Schema criado |
| Analytics ingestion | AnalyticsIngestionService | Funcional |

## 15.2 Itens Inferidos

| Item | Base da Inferencia | Nivel de Confianca |
|------|-------------------|-------------------|
| Projects (agrupamento de conversas) | Sidebar tem botao "Projetos" | Alta |
| Artifacts (conteudo rico lateral) | Sidebar tem botao "Artefatos" | Alta |
| Code execution | Sidebar tem botao "Codigo" | Alta |
| Teams/billing billing real | Schema billing exists, Stripe customer ID in orgs | Alta |
| Email verification flow | Schema email_verification_tokens exists | Alta |
| Password reset flow | Schema password_reset_tokens exists | Alta |
| Organization-level settings | JSONB settings column in orgs | Media |
| Notification preferences | Schema notification_preferences exists | Alta |

## 15.3 Itens que Exigem Validacao

| Pergunta | Impacto | Premissa Adotada |
|----------|---------|-----------------|
| Qual o modelo de precificacao exato? (markup %, planos) | Receita, competitividade | Markup 10-15% + planos mensais (ref: OpenRouter 5%) |
| Quais modalidades alem de texto? (imagem, audio, video) | Scope de desenvolvimento, conectores | Fase inicial: texto + visao. Expandir depois. |
| BYOK e obrigatorio em algum plano? | Arquitetura de billing | Opcional para Pro+, nao disponivel no Free |
| Qual o mercado-alvo principal? (devs, empresas, consumidores) | UX, pricing, marketing | Devs e equipes de produto no Brasil |
| Integracao com Stripe ja existe? | Timeline de billing | Nao existe, apenas placeholder (stripe_customer_id) |
| Quais provedores sao prioridade para Fase 1? | Custo de API keys para dev | OpenAI, Anthropic, Google, Groq (free tier) |
| Limite de conversas por plano? | Storage, custos | Free: 50 conversas, Pro: ilimitadas |
| Real-time voice/video e escopo? | Complexidade, custo | Fora do escopo inicial. Roadmap futuro. |
| SSO/SAML e necessario? | Enterprise sales | Apenas para plano Enterprise |

## 15.4 Riscos Tecnicos

| Risco | Severidade | Mitigacao |
|-------|-----------|-----------|
| Streaming SSE nao implementado (apenas simulacao) | Alta | Fase 1 prioriza SSE real. Pode usar WebSocket como fallback. |
| Conversas apenas em localStorage (perda de dados) | Alta | Fase 1 implementa persistencia server-side. |
| Circuit breaker pode ser muito agressivo em dev | Media | Configurar thresholds diferentes por ambiente. |
| Custo de API keys para 18 provedores em dev/test | Media | Usar mocks + 2-3 provedores reais (OpenAI, Groq free). |
| Event bus in-memory (Spring Events) nao duravel | Media | Fase 3 migrar para RabbitMQ/Redis Streams. |
| PostgreSQL como unico banco pode ser bottleneck | Baixa | Read replicas + connection pooling + particionamento. |

## 15.5 Riscos de Produto

| Risco | Severidade | Mitigacao |
|-------|-----------|-----------|
| Diferenciacao insuficiente vs OpenRouter/LiteLLM | Alta | Focar em UX premium + PT-BR nativo + observabilidade |
| Usuarios preferem usar provedores diretamente | Alta | Value prop: unified billing, fallback, comparacao, BYOK |
| Custo de operacao (API keys, infra) vs receita | Alta | Metering preciso + markup adequado + planos enterprise |
| Churn alto se free tier for muito limitado | Media | Free generoso o suficiente para hook, Pro com valor claro |

## 15.6 Riscos Juridicos/Compliance

| Risco | Severidade | Mitigacao |
|-------|-----------|-----------|
| LGPD: dados de conversas com IA | Alta | Consent explicito, retencao configuravel, erasure implementado |
| Termos de uso dos provedores (redistribuicao) | Alta | Validar ToS de cada provedor. BYOK mitiga parcialmente. |
| BYOK liability (chave do usuario vaza) | Media | Criptografia AES-256, audit log, termos claros |
| Precos dos provedores mudam frequentemente | Media | Atualizar catalogo periodicamente, alertas de mudanca |
| Responsabilidade sobre conteudo gerado | Media | Termos de uso claros, guardrails, moderation |

## 15.7 Plano para Fechar Lacunas

| Lacuna | Acao | Responsavel | Prazo |
|--------|------|------------|-------|
| Modelo de precificacao | Workshop com equipe de negocio | Product Owner | Fase 0 |
| Prioridade de provedores | Analise de custo/demanda | Tech Lead + PO | Fase 0 |
| SSE streaming | Implementacao tecnica | Backend dev | Fase 1 |
| Persistencia server-side | Migrations + service | Backend dev | Fase 1 |
| Stripe integration | Implementacao + sandbox testing | Backend dev | Fase 2 |
| BYOK security review | Audit de criptografia | Security engineer | Fase 2 |
| ToS dos provedores | Revisao juridica | Legal | Fase 2 |
| LGPD compliance review | Audit completo | DPO + Legal | Fase 4 |
| Load testing | Scripts + execucao | DevOps | Fase 5 |

---

# APENDICE A — CATALOGO GLOBAL DE PROVEDORES DE IA

## A.1 Provedores de LLM (Texto)

| Provedor | Modelos Principais | Auth | Pricing | Status Lume | Confianca |
|----------|-------------------|------|---------|-------------|-----------|
| OpenAI | GPT-4o, GPT-4o Mini, GPT-4.1, o1, o3 | API Key | Pay-per-token | Implementado | `[OBSERVADO]` |
| Anthropic | Claude 3.5 Haiku, Sonnet, Opus | API Key | Pay-per-token | Implementado | `[OBSERVADO]` |
| Google | Gemini 1.5 Flash, Pro, 2.0 | API Key | Pay-per-token + free tier | Implementado | `[OBSERVADO]` |
| Mistral | Small, Large, Codestral | API Key | Pay-per-token | Implementado | `[OBSERVADO]` |
| Cohere | Command R, Command R+ | API Key | Pay-per-token + free trial | Implementado | `[OBSERVADO]` |
| DeepSeek | Chat, Reasoner | API Key | Pay-per-token (baixo custo) | Implementado | `[OBSERVADO]` |
| Groq | Llama 3.1 8B/70B | API Key | Free tier generoso | Implementado | `[OBSERVADO]` |
| xAI | Grok-2, Grok-3 | API Key | Pay-per-token | Implementado | `[OBSERVADO]` |
| Together AI | Llama 3.1, Mixtral | API Key | Pay-per-token | Implementado | `[OBSERVADO]` |
| Fireworks | Llama 3.1 variants | API Key | Pay-per-token | Implementado | `[OBSERVADO]` |
| Cerebras | Llama 3.1 (ultra-fast) | API Key | Pay-per-token | Implementado | `[OBSERVADO]` |
| SambaNova | Llama 3.1 variants | API Key | Pay-per-token | Implementado | `[OBSERVADO]` |
| Novita | 200+ models | API Key | Pay-per-token | Implementado | `[OBSERVADO]` |
| NVIDIA NIM | Llama 3.1 variants | API Key | Pay-per-token | Implementado | `[OBSERVADO]` |
| Azure OpenAI | GPT-4o, GPT-4 | API Key + Endpoint | Pay-per-token | Implementado | `[OBSERVADO]` |
| OpenRouter | 100+ models (aggregator) | API Key | Pass-through + 5% | Implementado | `[OBSERVADO]` |
| Perplexity | Sonar, Sonar Pro | API Key | Pay-per-token | Implementado | `[OBSERVADO]` |
| AI21 Labs | Jamba 1.5 | API Key | Pay-per-token | Nao implementado | `[PROPOSTO]` |
| Inflection | Pi (via API) | API Key | Pay-per-token | Nao implementado | `[PROPOSTO]` |
| Writer | Palmyra | API Key | Enterprise | Nao implementado | `[PROPOSTO]` |
| Reka | Reka Core, Flash | API Key | Pay-per-token | Nao implementado | `[PROPOSTO]` |

## A.2 Provedores de Geracao de Imagem `[PROPOSTO]`

| Provedor | Modelos | Auth | Pricing | Prioridade |
|----------|---------|------|---------|-----------|
| OpenAI | DALL-E 3 | API Key | Per image ($0.04-0.12) | P1 |
| Stability AI | Stable Diffusion 3, SDXL | API Key | Per image + credits | P2 |
| Black Forest Labs | FLUX.1 Pro, Dev | API Key | Per image | P2 |
| Ideogram | Ideogram 2.0 | API Key | Per image | P3 |
| Leonardo AI | Leonardo models | API Key | Credits-based | P3 |

## A.3 Provedores de Audio/Speech `[PROPOSTO]`

| Provedor | Capacidade | Auth | Pricing | Prioridade |
|----------|-----------|------|---------|-----------|
| OpenAI | Whisper (STT), TTS | API Key | Per minute/char | P1 |
| ElevenLabs | TTS (high quality), Voice cloning | API Key | Per character | P2 |
| Deepgram | STT (real-time) | API Key | Per minute | P2 |
| AssemblyAI | STT + summarization | API Key | Per minute | P3 |
| Play.ht | TTS | API Key | Per character | P3 |

## A.4 Provedores de Video `[PROPOSTO]`

| Provedor | Capacidade | Status API | Prioridade |
|----------|-----------|-----------|-----------|
| Runway ML | Video generation (Gen-3) | API disponivel | P3 |
| Luma AI | Dream Machine | API disponivel | P3 |
| Pika Labs | Video generation | API limitada | P4 |
| OpenAI | Sora | Acesso restrito (2026) | P4 |

## A.5 Provedores de Embeddings `[PROPOSTO]`

| Provedor | Modelos | Dimensoes | Prioridade |
|----------|---------|-----------|-----------|
| OpenAI | text-embedding-3-small/large | 1536/3072 | P1 |
| Cohere | embed-v3 | 1024 | P2 |
| Voyage AI | voyage-3 | 1024 | P2 |
| Jina AI | jina-embeddings-v3 | 1024 | P3 |
| Google | text-embedding-004 | 768 | P2 |

## A.6 Provedores de Busca/Research `[PROPOSTO]`

| Provedor | Tipo | Auth | Prioridade |
|----------|------|------|-----------|
| Perplexity | AI search (ja implementado) | API Key | P1 |
| Tavily | Search API for AI | API Key | P2 |
| Exa AI | Neural search | API Key | P3 |
| Brave Search | Web search API | API Key | P2 |
| SerpAPI | Google search scraping | API Key | P3 |

## A.7 Concorrentes Diretos (AI Gateways)

| Concorrente | Diferencial | Pricing | Lume vs |
|------------|------------|---------|---------|
| **OpenRouter** | Simplicidade, 100+ modelos, markup 5% | Pass-through + 5% | Lume oferece UX premium + observabilidade + PT-BR |
| **LiteLLM** | Open-source, 100+ providers, self-hosted | Free (OSS) | Lume e managed + UX superior + billing integrado |
| **Portkey AI** | Enterprise guardrails, virtual keys, observability | $49+/mes | Lume compete com preco menor + foco LATAM |
| **Helicone** | Observabilidade, caching semantico, OSS | Free (OSS) + hosted | Lume integra observabilidade + chat UX + billing |
| **Martian** | Smart routing por qualidade | Enterprise | Lume oferece routing + chat completo |
| **Cloudflare AI Gateway** | Edge deployment, caching | Pay-per-request | Lume oferece mais provedores + UX completa |

---

# FIM DO DOCUMENTO

**Ultima atualizacao:** 2026-03-07
**Proxima revisao:** Apos validacao com equipe de negocio e estrategia
**Responsavel:** Equipe de Arquitetura Lume

