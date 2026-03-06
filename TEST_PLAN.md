# TEST_PLAN

## Unit
- Task payload validation (zod schemas)
- State transition helper coverage
- Diff parser (`parseDiffFiles`)
- Usage/credits math

## Integration
- Auth session guard for codex APIs
- Environment CRUD + cache invalidation
- Task lifecycle endpoints + SSE event emission
- PR create/update contracts
- Webhook ingestion for GitHub/Slack/Linear

## E2E critical flows
1. Onboarding to first task
2. Ask task with summary/logs
3. Code task with diff/tests/artifacts
4. Follow-up on existing task
5. Retry/cancel/archive/unarchive
6. Environment validate/reset-cache
7. Connector install + webhook-triggered task
8. Usage + credits purchase
9. Admin managed configs

## Quality gates
- `npm --prefix frontend run lint`
- `npm --prefix frontend run type-check`
- `npm --prefix frontend run build`
- Playwright smoke on /codex, task detail, environments, connectors, usage.
