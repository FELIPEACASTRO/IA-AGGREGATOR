# CHANGELOG_IMPLEMENTATION

## Added
- New codex cloud module under `frontend/src/app/codex/*` with required product routes.
- Full codex API surface under `frontend/src/app/api/*` for tasks, environments, connectors, reviews, usage, credits, analytics, compliance and managed configs.
- Prisma data model (`frontend/prisma/schema.prisma`) covering identity, tasks, environments, connectors, review, billing and admin/compliance entities.
- BullMQ + Redis queue runtime (`src/server/codex/queue.ts`) and executable task pipeline (`src/server/codex/task-runner.ts`).
- SSE realtime endpoint for task events.
- New codex UI components (shell, composer, task list, timeline, logs, diff, PR panel).
- Route-level auth/session helpers and workspace seeding.
- Documentation package: product spec, parity matrix, assumptions, routes, API contract, state/event model, security, connectors, runbook and test plan.

## Changed
- Frontend nav now includes `Codex Cloud` entry.
- `.env.local.example` extended with codex DB/Redis/OAuth env vars.
- Frontend package scripts extended with Prisma bootstrap commands.

## Notes
- Current delivery implements the core cloud execution backbone and full route/API surface.
- Some parity items remain partial and are explicitly tracked in `PARITY_MATRIX.md`.
