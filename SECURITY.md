# SECURITY — Modelo de Segurança

> Documento de segurança da plataforma IA-AGGREGATOR / Lume Codex Cloud.
> Versão: 2.0.0 | Última atualização: 2026-03-08

---

## Resumo Executivo

A plataforma implementa autenticação JWT stateless com refresh tokens no Redis, validação de entrada via Zod/Jakarta Bean Validation, e separação de secrets em modelos dedicados. Entretanto, **4 vulnerabilidades críticas** foram identificadas durante auditoria e estão documentadas neste arquivo com plano de remediação.

| Classificação | Quantidade | Status |
|--------------|-----------|--------|
| Crítica | 4 | Em remediação |
| Alta | 3 | Planejado |
| Média | 2 | Planejado |
| Controles Implementados | 8 | Ativo |

---

## Threat Model (STRIDE)

### S — Spoofing (Falsificação de Identidade)

| Ameaça | Severidade | Status | Ref |
|--------|-----------|--------|-----|
| JWT forjado aceito pelo frontend (assinatura não verificada) | CRITICA | **VULNERAVEL** | GAP-001 |
| Refresh token reutilizado após revogação | BAIXA | Mitigado (Redis TTL) | — |
| OAuth token previsível (mock determinístico) | CRITICA | **VULNERAVEL** | GAP-004 |
| Brute-force de senha | BAIXA | Mitigado (lock 5 tentativas/30min) | — |

**Detalhes GAP-001 — JWT Sem Verificação**:
- **Arquivo**: `frontend/src/server/codex/auth.ts`
- **Função**: `decodeJwtPayload()` faz apenas `Buffer.from(parts[1], 'base64')` sem `jwtVerify()`
- **CVSS v3.1**: 9.8 (Critical) — Network/Low/None/Unchanged/High/High/High
- **Remediação**: Instalar `jose`, usar `jwtVerify()` com `JWT_SECRET` do backend

**Detalhes GAP-004 — OAuth Mock**:
- **Arquivo**: `frontend/src/app/api/oauth/github/callback/route.ts`
- **Código**: `accessToken = \`token-from-\${code.slice(0, 8)}\``
- **Remediação**: Implementar troca real de code via POST `https://github.com/login/oauth/access_token`

---

### T — Tampering (Adulteração)

| Ameaça | Severidade | Status | Ref |
|--------|-----------|--------|-----|
| Webhook payload adulterado (GitHub) | CRITICA | **VULNERAVEL** | GAP-003 |
| Webhook payload adulterado (Slack) | CRITICA | **VULNERAVEL** | GAP-003 |
| Webhook payload adulterado (Linear) | CRITICA | **VULNERAVEL** | GAP-003 |
| SQL injection via queries | BAIXA | Mitigado (Prisma ORM + JPA) | — |
| Request body malformado | BAIXA | Mitigado (Zod + Jakarta Validation) | — |

**Detalhes GAP-003 — Webhooks Sem HMAC**:
- **Arquivos**:
  - `frontend/src/app/api/webhooks/github/route.ts` — Não verifica `x-hub-signature-256`
  - `frontend/src/app/api/webhooks/slack/route.ts` — Não verifica `x-slack-signature`
  - `frontend/src/app/api/webhooks/linear/route.ts` — Não verifica `linear-signature`
- **CVSS v3.1**: 8.6 (High) — Network/Low/None/Changed/None/High/None
- **Remediação**: Criar `webhook-verify.ts` com `crypto.timingSafeEqual()` + HMAC SHA-256

---

### I — Information Disclosure (Vazamento de Informação)

| Ameaça | Severidade | Status | Ref |
|--------|-----------|--------|-----|
| Cookies legíveis via JavaScript (XSS) | CRITICA | **VULNERAVEL** | GAP-002 |
| Cookies trafegam via HTTP (sem secure) | ALTA | **VULNERAVEL** | GAP-002 |
| API keys em respostas de IA | MEDIA | Mitigado (output guardrails regex) | — |
| Stack traces em respostas de erro | BAIXA | Mitigado (GlobalExceptionHandler mascara) | — |
| Secrets em logs de task | ALTA | **PARCIAL** | SECURITY pendente |

**Detalhes GAP-002 — Cookies Inseguros**:
- **Arquivo**: `frontend/src/app/api/auth/login/route.ts`
- **Configuração atual**: `httpOnly: false, secure: false`
- **CVSS v3.1**: 7.5 (High) — Network/Low/None/Unchanged/High/None/None
- **Remediação**: `httpOnly: true`, `secure: process.env.NODE_ENV === 'production'`

---

### D — Denial of Service (Negação de Serviço)

| Ameaça | Severidade | Status | Ref |
|--------|-----------|--------|-----|
| Abuso de API sem rate limiting | ALTA | **VULNERAVEL** | GAP-008 |
| BullMQ queue flooding | MEDIA | Parcial (concurrency=2) | — |
| Prompt excessivamente longo | BAIXA | Mitigado (max 5000 chars guardrail) | — |
| Response excessivamente longa | BAIXA | Mitigado (max 8000 chars guardrail) | — |

---

### E — Elevation of Privilege (Escalação de Privilégio)

| Ameaça | Severidade | Status | Ref |
|--------|-----------|--------|-----|
| Membro acessa endpoints admin (RBAC incompleto) | MEDIA | **VULNERAVEL** | GAP-013 |
| Workspace cross-access | BAIXA | Mitigado (scoping em requireCodexContext) | — |
| API key scopes não enforced | MEDIA | **PENDENTE** (schema existe) | — |

---

## Controles Implementados

### 1. Autenticação JWT (Backend)
- **Tipo**: Access token JWT com HMAC-SHA256
- **Duração**: 15 minutos (configurável)
- **Claims**: `sub` (userId), `email`, `role`, `iss`, `exp`
- **Geração**: `JwtTokenProvider` com JJWT 0.12.6
- **Secret**: `JWT_SECRET` (Base64-encoded, via env var)

### 2. Refresh Token com Revogação (Backend)
- **Duração**: 7 dias
- **Storage**: Redis com TTL
- **Revogação**: Token ID removido do Redis
- **Rotação**: Novo ID a cada refresh

### 3. Proteção contra Brute-Force (Backend)
- **Limite**: 5 tentativas falhadas
- **Lock**: 30 minutos
- **Tracking**: `failed_login_count` + `locked_until` na tabela `auth.users`

### 4. Validação de Entrada
- **Frontend**: Zod schemas em todos os POST endpoints
- **Backend**: Jakarta Bean Validation (`@Valid`) nos DTOs
- **Proteção SQL**: Queries parametrizadas via Prisma ORM e JPA

### 5. Guardrails de IA
- **Prompt**: Regex para jailbreak, injection (max 5000 chars)
- **Output**: Regex para API keys, tokens privados (max 8000 chars)
- **Ação configurável**: `block` ou `log`
- **Padrões bloqueados**:
  - Prompt: `ignore previous instructions`, `system override`, `jailbreak`, `bypass policy`
  - Output: `api key`, `private key`, `token`

### 6. Circuit Breaker (Backend)
- **Biblioteca**: Resilience4j 2.2.0
- **Config**: Window=20 calls, threshold=50%, wait=20s, half-open=5
- **Por provider**: Instância dedicada para cada um dos 17 AI providers

### 7. CORS Configuration (Backend)
- **Origens permitidas**: `localhost:3000`, `localhost:3001`, `iaggregator.com.br`
- **Métodos**: GET, POST, PUT, PATCH, DELETE, OPTIONS
- **Credentials**: Enabled
- **Max-age**: 3600s

### 8. Separação de Secrets
- **EnvironmentSecret**: Modelo dedicado com `encryptedValue` e flag `setupOnly`
- **EnvironmentVariable**: Valores não-sensíveis separados
- **API Keys**: `key_hash` (não reversível) + `key_prefix` (8 chars para identificação)

---

## Código de Erros de Segurança (Backend)

| Código | HTTP | Descrição |
|--------|------|-----------|
| AUTH_001 | 401 | Credenciais inválidas |
| AUTH_002 | 404 | Usuário não encontrado |
| AUTH_003 | 403 | Conta desativada |
| AUTH_004 | 423 | Conta bloqueada (brute-force) |
| AUTH_005 | 403 | Acesso negado |
| AUTH_006 | 401 | Token inválido ou expirado |
| AUTH_007 | 422 | Email já cadastrado |
| AUTH_008 | 422 | Dados inválidos |
| AI_001 | 400 | Prompt inválido |
| AI_002 | 503 | Provider indisponível |
| AI_003 | 429 | Rate limit excedido |
| AI_004 | 400 | Guardrail bloqueou conteúdo |
| AI_005 | 502 | Erro de parsing na resposta |
| AI_006 | 500 | Nenhum provider disponível |
| AI_007 | 500 | Configuração inválida |

---

## Roadmap de Hardening

| Prioridade | Ação | Sprint | Status |
|-----------|------|--------|--------|
| P0 | Verificação de assinatura JWT no frontend | S1 | Pendente |
| P0 | Flags HttpOnly + Secure nos cookies | S1 | Pendente |
| P0 | Verificação HMAC nos 3 webhooks | S2 | Pendente |
| P0 | OAuth real (substituir mock) | S2 | Pendente |
| P1 | Rate limiting server-side com Redis | S3 | Pendente |
| P1 | RBAC por role nos endpoints admin | S6 | Pendente |
| P1 | Masking de secrets em logs streamados | S7 | Pendente |
| P2 | Encryption at-rest para OAuth tokens | S8 | Pendente |
| P2 | SSRF/network egress enforcement | S9 | Pendente |
| P2 | API key scope enforcement | S10 | Pendente |
| P3 | Penetration testing externo | S12 | Pendente |
| P3 | LGPD compliance automation | S13 | Pendente |

---

## Checklist de Segurança por Release

- [ ] Nenhum secret hardcoded no código (`grep -r "api_key\|secret\|password" src/`)
- [ ] Cookies com HttpOnly=true e Secure=true em produção
- [ ] JWT verificado com assinatura no frontend e backend
- [ ] Webhooks validam HMAC antes de processar
- [ ] Rate limiting ativo em todos os endpoints públicos
- [ ] Guardrails de IA ativos (prompt + output)
- [ ] CORS restrito a domínios de produção
- [ ] Logs não contêm tokens ou credentials
- [ ] Dependências sem CVEs conhecidos (`npm audit`, `mvn dependency-check`)
- [ ] Testes de segurança passando no CI

---

## OWASP Top 10 — Mapeamento

| # | Vulnerabilidade OWASP | Status na Plataforma |
|---|----------------------|---------------------|
| A01 | Broken Access Control | **PARCIAL** — Workspace scoping OK, RBAC incompleto (GAP-013) |
| A02 | Cryptographic Failures | **PARCIAL** — BCrypt OK, mas cookies sem flags (GAP-002) |
| A03 | Injection | **MITIGADO** — Prisma + JPA parametrizados, Zod validation |
| A04 | Insecure Design | **PARCIAL** — Threat model agora documentado, webhook sem HMAC (GAP-003) |
| A05 | Security Misconfiguration | **PARCIAL** — CORS OK, mas rate limiting ausente (GAP-008) |
| A06 | Vulnerable Components | **A VERIFICAR** — npm audit e mvn dependency-check pendentes |
| A07 | Authentication Failures | **VULNERAVEL** — JWT sem verificação (GAP-001), OAuth mock (GAP-004) |
| A08 | Data Integrity Failures | **VULNERAVEL** — Webhooks sem HMAC (GAP-003) |
| A09 | Logging & Monitoring | **PARCIAL** — Logs OK, alerting ausente |
| A10 | SSRF | **PENDENTE** — Network egress não enforced |

---

## Referências

- [ROADMAP.md](./ROADMAP.md) — Lista completa de gaps com planos de correção
- [ARCHITECTURE.md](./ARCHITECTURE.md) — Visão geral da arquitetura
- [API_CONTRACT.md](./API_CONTRACT.md) — Contratos de API e autenticação
