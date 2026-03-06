# RUNBOOK

## Bootstrap
1. Start infra (`docker compose up -d`)
2. Ensure frontend deps (`npm --prefix frontend install`)
3. Export DB URL for codex schema:
   - PowerShell: `$env:CODEX_DATABASE_URL='postgresql://ia_aggregator:ia_aggregator@localhost:5432/ia_aggregator?schema=codex'`
4. Run schema sync:
   - `npm --prefix frontend run codex:bootstrap`
5. Start app:
   - `npm --prefix frontend run dev`

## Task worker
- BullMQ worker is initialized lazily on first task enqueue in Next server runtime.
- Redis required at `CODEX_REDIS_URL`.

## Health checks
- API readiness: `/api/auth/session`, `/api/tasks`, `/api/environments`
- Realtime stream: `/api/tasks/{id}/events`

## Incident notes
- If queue stuck: restart Next server and Redis.
- If DB mismatch: rerun `prisma:push` with `CODEX_DATABASE_URL`.
