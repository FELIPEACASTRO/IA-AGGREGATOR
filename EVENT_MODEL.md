# EVENT_MODEL

Task events currently emitted:
- task.created
- task.queued
- task.started
- provisioning.started
- repository.cloned
- setup.completed
- agent.progress
- validation.completed
- diff.ready
- pr.created
- pr.updated
- task.completed
- task.failed
- task.cancelled
- task.archived
- task.unarchived
- task.retry
- task.followup_created

Transport:
- Persistent event log in `TaskEvent`
- Realtime stream via SSE endpoint `GET /api/tasks/:id/events`

Log phases in `TaskLogChunk`:
- provisioning
- repo_download
- setup
- maintenance
- agent
- validation
- pr_push
