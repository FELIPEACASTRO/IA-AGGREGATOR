# DATA_MODEL

Primary models implemented in Prisma schema `frontend/prisma/schema.prisma`:

- Identity: User, Workspace, Membership, Session, OAuthConnection
- GitHub: GitHubInstallation, GitRepository, RepositoryPermission, RepositoryBranchCache
- Environment: Environment, EnvironmentRepoMap, EnvironmentRuntimePin, EnvironmentVariable, EnvironmentSecret, EnvironmentCache, InternetPolicy, EnvironmentExecutionHistory
- Task execution: Task, TaskInput, TaskRun, TaskFollowUp, TaskEvent, TaskLogChunk, TaskArtifact, TaskEvidenceCitation, DiffSnapshot, DiffFile, DiffHunk, PullRequest, TaskArchiveRecord
- Review: CodeReviewPolicy, GitHubReviewRun, ReviewFinding, ReviewGuidelineSource
- Connectors: SlackInstall, SlackWorkspaceBinding, SlackEventLog, LinearInstall, LinearWorkspaceBinding, LinearEventLog, TriageRule
- Usage/Billing: UsageEntry, CreditBalance, CreditLedgerEntry, BillingIntent
- Admin/Compliance: AuditLog, AnalyticsAggregate, ComplianceExportJob, ManagedConfig, AssumptionLog

Database target: PostgreSQL schema `codex` via `CODEX_DATABASE_URL`.
