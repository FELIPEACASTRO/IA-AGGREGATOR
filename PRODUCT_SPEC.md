# PRODUCT_SPEC — Especificacao do Produto

> Visao, personas, objetivos e escopo da plataforma IA-AGGREGATOR / Lume Codex Cloud.
> Versao: 2.0.0 | Ultima atualizacao: 2026-03-08

---

## Visao

Lume Codex Cloud e uma plataforma web para execucao de tasks de codificacao em nuvem com execucao paralela/background, evidencias revisaveis, e delegacao via conectores (GitHub, Slack, Linear). Integra 17 provedores de IA com fallback automatico.

---

## Personas

### Staff Engineer
- Executa mudancas de codigo em paralelo em multiplos repositorios
- Precisa de: tasks paralelas, diff review, PR lifecycle automatizado
- Dor: tempo perdido com tarefas repetitivas de codificacao

### Tech Lead
- Revisa diffs, testes e ciclo de vida de PRs
- Precisa de: evidencias (logs, diff, tests), politicas de review, governanca
- Dor: falta de visibilidade e controle sobre tasks automatizadas

### Engineering Manager
- Acompanha uso/creditos e conformidade de politicas
- Precisa de: dashboard de uso, analytics, configs admin, LGPD compliance
- Dor: governanca e custo de ferramentas de IA

---

## Objetivos do Produto

1. Lancar tasks Ask/Code a partir de um unico composer
2. Manter execucao viva em background com progresso realtime (SSE)
3. Revisar summary/logs/diff/tests/artifacts antes do PR
4. Continuar tasks com follow-up preservando contexto
5. Governar ambientes, politica de internet e review policies por repo/workspace

---

## Escopo

| Area | Funcionalidades | Status |
|------|----------------|--------|
| Auth | Login, registro, OAuth GitHub, JWT | Partial (GAP-001, GAP-002, GAP-004) |
| Onboarding | Get-started, wizard de configuracao | Partial |
| Dashboard | Lista de tasks, filtros, acoes | Partial |
| Task Detail | Summary, logs, diff, tests, artifacts, PR | Partial |
| Environments | CRUD, secrets, internet policy, cache | Partial |
| Connectors | GitHub, Slack, Linear (install + webhooks) | Partial (GAP-003) |
| Code Review | Policies, automatic reviews, findings | Partial |
| Usage/Credits | Dashboard, saldo, compra | Partial (GAP-021) |
| Analytics/Admin | Metricas, configs, audit, compliance | Partial |
| Shortcuts | Templates de prompts rapidos | Partial |
| Chat IA | Multi-provider com fallback | Partial (GAP-007) |
| Billing | Planos, pagamentos, Stripe | Not started (GAP-021) |
| Times/Multi-tenant | Workspaces, memberships, convites | Not started (GAP-024) |
| LGPD | Consentimento, erasure, data export | Behind flag (GAP-025) |

---

## Principios de UX

- **Dark-first**: Interface otimizada para tema escuro
- **Keyboard-first**: Atalhos de teclado, Command Palette (Cmd+K)
- **Alta relacao sinal/ruido**: Foco em informacao relevante
- **Evidence-first workflow**: summary → logs → diff → tests → PR
- **Risk labels explicitos**: Badges para internet policy e secrets setup-only

---

## Roadmap por Trimestre

### Q1 2026 (Atual) — Fundacao + Seguranca
- Corrigir vulnerabilidades criticas (GAP-001 a GAP-004)
- Implementar rate limiting e cache Redis
- Expandir cobertura de testes para 50%

### Q2 2026 — Integracoes Reais
- OAuth GitHub real (substituir mock)
- Webhook HMAC verification
- Streaming real no chat (SSE)
- Billing com Stripe

### Q3 2026 — Governanca + Multi-Tenant
- RBAC completo por role
- Multi-workspace com convites
- LGPD workflows automatizados
- Feature flags runtime

### Q4 2026 — Escala + Polish
- Testes de carga e otimizacao
- Documentacao completa
- Onboarding enterprise
- API publica documentada

---

## Metricas de Sucesso

| Metrica | Atual | Meta Q2 | Meta Q4 |
|---------|-------|---------|---------|
| Vulnerabilidades criticas | 4 | 0 | 0 |
| Cobertura de testes | ~15% | 50% | 80% |
| Capacidades "Complete" | 0/26 | 12/26 | 22/26 |
| Tempo medio task | N/A | <5min | <3min |
| Uptime | N/A | 99% | 99.9% |

---

## Referencias

- [PARITY_MATRIX.md](./PARITY_MATRIX.md) — Status detalhado por capacidade
- [ROADMAP.md](./ROADMAP.md) — Gaps priorizados
- [ARCHITECTURE.md](./ARCHITECTURE.md) — Arquitetura tecnica
