# PARITY_MATRIX

| Capability | UI Surface | API | Data Model | Test Surface | Status |
|---|---|---|---|---|---|
| Cloud tasks in parallel/background | /codex, /codex/tasks/:id | POST /api/tasks + BullMQ worker | Task, TaskRun, TaskEvent | integration pending | Partial |
| Ask/Code modes | Composer + task details | POST /api/tasks | Task.mode, TaskInput | unit pending | Partial |
| Realtime events | Task details/logs | GET /api/tasks/:id/events (SSE) | TaskEvent | e2e pending | Partial |
| Logs with phases | /codex/tasks/:id/logs | GET /api/tasks/:id/logs | TaskLogChunk | integration pending | Partial |
| Diff review | /codex/tasks/:id/diff | GET /api/tasks/:id/diff | DiffSnapshot, DiffFile, DiffHunk | integration pending | Partial |
| Tests evidence | /codex/tasks/:id/tests | GET /api/tasks/:id/tests | TaskLogChunk (validation) | integration pending | Partial |
| Artifacts | /codex/tasks/:id/artifacts | GET /api/tasks/:id/artifacts | TaskArtifact | integration pending | Partial |
| PR create/update | /codex/tasks/:id/pull-request | POST/PATCH pull-requests endpoints | PullRequest | integration pending | Partial |
| Follow-up | Task summary page | POST /api/tasks/:id/followups | TaskFollowUp | integration pending | Partial |
| Retry/Cancel/Archive | task list/actions | /retry /cancel /archive /unarchive | TaskArchiveRecord | integration pending | Partial |
| Environments full CRUD | /codex/settings/environments* | /api/environments* | Environment + related models | integration pending | Partial |
| Internet policy | environment forms + composer badges | env create/update | InternetPolicy, Environment | unit pending | Partial |
| AGENTS.md support | N/A (planned panel) | N/A | ReviewGuidelineSource placeholder | not started | Behind flag |
| GitHub review policy | /codex/settings/code-review | /api/code-review/policies* | CodeReviewPolicy, GitHubReviewRun, ReviewFinding | integration pending | Partial |
| Slack delegation | connectors + webhook | POST /api/webhooks/slack | Slack* models + Task | integration pending | Partial |
| Linear delegation | connectors + webhook | POST /api/webhooks/linear | Linear* models + Task | integration pending | Partial |
| Usage dashboard | /codex/settings/usage | GET /api/usage | UsageEntry, CreditBalance | integration pending | Partial |
| Credits flow | /codex/settings/usage/credits | GET/POST /api/credits* | CreditBalance, CreditLedgerEntry, BillingIntent | integration pending | Partial |
| Analytics/admin | /codex/settings/analytics, /admin/settings | /api/analytics, /api/managed-configs* | AnalyticsAggregate, ManagedConfig, AuditLog | integration pending | Partial |
| Shortcuts + deep links | /codex/shortcuts + query params | N/A | N/A | e2e pending | Partial |
| Voice + image inputs | composer | POST /api/tasks | TaskInput.imageInputs/voiceTranscript | e2e pending | Partial |

Legend: Complete = production-ready; Partial = implemented core path, needs hardening/tests; Behind flag = model/scaffold only.
