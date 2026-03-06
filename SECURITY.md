# SECURITY

Current controls implemented:
- JWT session dependency for codex APIs (`access_token` cookie required).
- Workspace scoping checks in route handlers.
- Secret material persisted separately in `EnvironmentSecret` with setup-only intent.
- Audit logs for webhook and admin-relevant actions.
- Separation between setup policy metadata and task runtime evidence.

Pending hardening:
- Full webhook signature verification (GitHub/Slack/Linear secrets).
- Strict RBAC by workspace role for admin endpoints.
- Token encryption-at-rest for OAuth secrets.
- SSRF/network egress enforcement at container/runtime level.
- Advanced secrets masking pipeline in streamed logs.
