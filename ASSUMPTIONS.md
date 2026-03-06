# ASSUMPTIONS

1. Cloud task execution runs inside server-side worker process (BullMQ + Redis) and per-task sandbox folder (`frontend/.codex-runtime/tasks/{taskId}`) in this delivery.
2. GitHub/Slack/Linear integrations are implemented as real install/webhook surfaces with persisted state; production credential exchange/signature hardening is pending env-specific secrets.
3. Default workspace/repository/environment are auto-seeded from authenticated user session to reduce onboarding friction.
4. If no repository clone URL is configured, worker initializes a local git workspace and still produces real logs/diff artifacts.
5. Billing adapter is abstracted through `BillingIntent` + ledger; purchase endpoint currently records manual completed intents.
6. AGENTS.md precedence engine is tracked but still behind feature flag for next increment.
7. OAuth callback currently supports deterministic local flow and can be upgraded to full token exchange via `GITHUB_CLIENT_ID/SECRET`.
8. Internet policy is enforced at config level and surfaced in UX; low-level network sandbox enforcement is deferred to container runtime hardening phase.
9. Existing backend auth remains source of truth for login; codex APIs rely on JWT in cookies.
