# API_CONTRACT

## Auth
- POST /api/auth/login
- POST /api/auth/logout
- GET /api/auth/session
- POST /api/oauth/github/connect
- GET /api/oauth/github/callback

## Repositories
- GET /api/repositories
- GET /api/repositories/:repoId/branches

## Environments
- GET /api/environments
- POST /api/environments
- GET /api/environments/:id
- PATCH /api/environments/:id
- DELETE /api/environments/:id
- POST /api/environments/:id/validate
- POST /api/environments/:id/reset-cache

## Tasks
- GET /api/tasks
- POST /api/tasks
- GET /api/tasks/:id
- GET /api/tasks/:id/events (SSE)
- GET /api/tasks/:id/logs
- GET /api/tasks/:id/diff
- GET /api/tasks/:id/tests
- GET /api/tasks/:id/artifacts
- POST /api/tasks/:id/followups
- POST /api/tasks/:id/cancel
- POST /api/tasks/:id/retry
- POST /api/tasks/:id/archive
- POST /api/tasks/:id/unarchive

## Pull Requests
- GET /api/tasks/:id/pull-requests
- POST /api/tasks/:id/pull-requests
- PATCH /api/tasks/:id/pull-requests/:prId

## Code Review
- GET /api/code-review/policies
- POST /api/code-review/policies
- PATCH /api/code-review/policies/:id

## Integrations + Webhooks
- POST /api/integrations/github/install
- POST /api/integrations/slack/install
- POST /api/integrations/linear/install
- GET /api/integrations/status
- POST /api/webhooks/github
- POST /api/webhooks/slack
- POST /api/webhooks/linear

## Usage/Credits
- GET /api/usage
- GET /api/credits
- POST /api/credits
- POST /api/credits/purchase

## Analytics/Admin
- GET /api/analytics
- GET /api/compliance/exports
- POST /api/compliance/exports
- GET /api/managed-configs
- POST /api/managed-configs
- PATCH /api/managed-configs/:id
