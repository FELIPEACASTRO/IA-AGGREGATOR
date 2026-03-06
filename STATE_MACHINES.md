# STATE_MACHINES

## Task Status
draft -> queued -> preparing_environment -> downloading_repository -> cloning_repository -> running_setup -> running_maintenance -> running_agent -> validating -> generating_diff -> pr_ready -> completed

Alternative transitions:
- any running state -> failed
- any running state -> cancelled
- completed -> archived
- archived -> completed
- failed -> queued (retry)

## Environment Cache Status
cold -> preparing -> warm
warm -> invalidated
invalidated -> reset_pending -> preparing
any -> failed

## Pull Request Status
none -> draft -> open -> merged/closed
open -> update_pending -> open
any -> failed

All transitions must emit task event and update `updatedAt`.
