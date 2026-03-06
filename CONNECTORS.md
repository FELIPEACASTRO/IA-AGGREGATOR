# CONNECTORS

## GitHub
- Install endpoint: `POST /api/integrations/github/install`
- OAuth callback flow: `/oauth/github/callback` -> `/api/oauth/github/callback`
- Review policy integration via `CodeReviewPolicy`
- PR surface via task pull-request endpoints

## Slack
- Install endpoint: `POST /api/integrations/slack/install`
- Webhook endpoint: `POST /api/webhooks/slack`
- Mention parsing triggers task creation with environment/repo fallback

## Linear
- Install endpoint: `POST /api/integrations/linear/install`
- Webhook endpoint: `POST /api/webhooks/linear`
- Assignment/comment trigger creates task and persists event logs

Connector status aggregation:
- `GET /api/integrations/status`
