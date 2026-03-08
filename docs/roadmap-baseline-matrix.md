# Roadmap Baseline Matrix

## Objective
- Align the implementation roadmap with the actual repository baseline.
- Separate current facts from benchmark references and proposed architecture.

## Observed Directly
- `frontend` is a `Next.js 15 + React 19 + TypeScript` app with App Router and BFF routes.
- `frontend/prisma/schema.prisma` is the operational data model for workspaces, tasks, environments, connectors, usage and compliance.
- `frontend/src/server/codex/*` owns task orchestration with `BullMQ`, Prisma and SSE-style event streams.
- `backend` is a Spring Boot multi-module service that already owns authentication, AI provider integrations, routing guardrails and analytics.
- Current product surfaces are `/chat`, `/library`, `/prompts`, `/settings`, `/billing` and `/codex/*`.

## Inferred With High Confidence
- The current repo is split between a general Lume surface and a deeper `/codex` task-execution surface.
- Workspace support exists in the schema, but it was previously operating through implicit bootstrap rather than explicit tenant selection.
- Chat remains a partially isolated track because conversations are still client-side and not yet part of the workspace-scoped domain model.

## External Benchmark
- The benchmark is used as a reference for functional coverage only.
- Benchmark-derived modules to track are `workspace shell`, `projects`, `task execution`, `artifacts`, `search`, `connectors`, `provider catalog`, `usage`, `billing`, `compliance`, `skills`, `mail`, `cloud browser`.
- Benchmark-derived routes or labels such as `/users` and `/agents` are not treated as required repo parity.

## Proposed Canonical Ownership
- `Next.js + Prisma + BullMQ`: workspace, membership, projects, tasks, artifacts, connectors, usage BFF and workspace-scoped UI contracts.
- `Spring Boot`: auth, AI gateway, provider integrations, guardrails, telemetry and analytics services.
- New frontend-owned contracts start under `/api/v1`; legacy `/api/*` endpoints remain in compatibility mode until migrated.

## Backlog Classification
- `core`: tenancy, workspace selection, shell normalization, tasks, artifacts, search foundation, connectors foundation, workspace summary, usage foundation.
- `after-core`: skills, personalization, advanced sharing, mail, cloud browser.
- `not-now`: visual cloning of any benchmark, `/users`, `/agents`, media-heavy quick actions.
