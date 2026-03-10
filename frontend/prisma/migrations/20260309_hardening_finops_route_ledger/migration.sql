-- Enums
DO $$
BEGIN
  CREATE TYPE "ProjectContextStatus" AS ENUM ('ACTIVE', 'ARCHIVED');
EXCEPTION
  WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
  CREATE TYPE "RouteServiceClass" AS ENUM ('FAST', 'BALANCED', 'PREMIUM', 'RESEARCH', 'MULTIMODAL');
EXCEPTION
  WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
  CREATE TYPE "RouteExecutionStatus" AS ENUM ('PLANNED', 'STARTED', 'COMPLETED', 'FAILED');
EXCEPTION
  WHEN duplicate_object THEN NULL;
END $$;

-- ProjectContext
CREATE TABLE IF NOT EXISTS "ProjectContext" (
  "id" TEXT NOT NULL,
  "workspaceId" TEXT NOT NULL,
  "repositoryId" TEXT,
  "ownerUserId" TEXT,
  "slug" TEXT NOT NULL,
  "name" TEXT NOT NULL,
  "description" TEXT,
  "sourceRef" TEXT,
  "status" "ProjectContextStatus" NOT NULL DEFAULT 'ACTIVE',
  "metadata" JSONB,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL,
  CONSTRAINT "ProjectContext_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX IF NOT EXISTS "ProjectContext_workspaceId_slug_key"
  ON "ProjectContext"("workspaceId", "slug");

-- Task.projectContextId
ALTER TABLE "Task" ADD COLUMN IF NOT EXISTS "projectContextId" TEXT;

-- RouteDecision
CREATE TABLE IF NOT EXISTS "RouteDecision" (
  "id" TEXT NOT NULL,
  "workspaceId" TEXT NOT NULL,
  "taskId" TEXT NOT NULL,
  "taskRunId" TEXT,
  "projectContextId" TEXT,
  "serviceClass" "RouteServiceClass" NOT NULL DEFAULT 'BALANCED',
  "policyKey" TEXT NOT NULL DEFAULT 'default',
  "policyVersion" TEXT NOT NULL DEFAULT '2026-03-09',
  "requestedMode" "TaskMode",
  "selectedProvider" TEXT NOT NULL,
  "selectedModel" TEXT NOT NULL,
  "fallbackProvider" TEXT,
  "fallbackModel" TEXT,
  "fallbackUsed" BOOLEAN NOT NULL DEFAULT false,
  "reason" TEXT,
  "estimatedInputTokens" INTEGER NOT NULL DEFAULT 0,
  "estimatedOutputTokens" INTEGER NOT NULL DEFAULT 0,
  "estimatedCost" DOUBLE PRECISION NOT NULL DEFAULT 0,
  "status" "RouteExecutionStatus" NOT NULL DEFAULT 'PLANNED',
  "metadata" JSONB,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL,
  CONSTRAINT "RouteDecision_pkey" PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX IF NOT EXISTS "RouteDecision_taskRunId_key" ON "RouteDecision"("taskRunId");
CREATE INDEX IF NOT EXISTS "RouteDecision_workspaceId_createdAt_idx" ON "RouteDecision"("workspaceId", "createdAt");
CREATE INDEX IF NOT EXISTS "RouteDecision_taskId_createdAt_idx" ON "RouteDecision"("taskId", "createdAt");
CREATE INDEX IF NOT EXISTS "RouteDecision_serviceClass_selectedProvider_selectedModel_idx"
  ON "RouteDecision"("serviceClass", "selectedProvider", "selectedModel");

-- ModelRun
CREATE TABLE IF NOT EXISTS "ModelRun" (
  "id" TEXT NOT NULL,
  "workspaceId" TEXT NOT NULL,
  "taskId" TEXT NOT NULL,
  "taskRunId" TEXT,
  "routeDecisionId" TEXT,
  "projectContextId" TEXT,
  "provider" TEXT NOT NULL,
  "model" TEXT NOT NULL,
  "taskClass" TEXT NOT NULL,
  "inputTokens" INTEGER NOT NULL DEFAULT 0,
  "outputTokens" INTEGER NOT NULL DEFAULT 0,
  "latencyMs" INTEGER,
  "estimatedCost" DOUBLE PRECISION NOT NULL DEFAULT 0,
  "actualCost" DOUBLE PRECISION NOT NULL DEFAULT 0,
  "currency" TEXT NOT NULL DEFAULT 'USD',
  "fallbackUsed" BOOLEAN NOT NULL DEFAULT false,
  "routePolicyVersion" TEXT,
  "status" "RouteExecutionStatus" NOT NULL DEFAULT 'STARTED',
  "metadata" JSONB,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updatedAt" TIMESTAMP(3) NOT NULL,
  CONSTRAINT "ModelRun_pkey" PRIMARY KEY ("id")
);

CREATE INDEX IF NOT EXISTS "ModelRun_workspaceId_createdAt_idx" ON "ModelRun"("workspaceId", "createdAt");
CREATE INDEX IF NOT EXISTS "ModelRun_provider_model_createdAt_idx" ON "ModelRun"("provider", "model", "createdAt");
CREATE INDEX IF NOT EXISTS "ModelRun_taskId_taskRunId_idx" ON "ModelRun"("taskId", "taskRunId");

-- CostLedgerEntry
CREATE TABLE IF NOT EXISTS "CostLedgerEntry" (
  "id" TEXT NOT NULL,
  "workspaceId" TEXT NOT NULL,
  "taskId" TEXT,
  "taskRunId" TEXT,
  "projectContextId" TEXT,
  "routeDecisionId" TEXT,
  "modelRunId" TEXT,
  "category" TEXT NOT NULL,
  "amount" DOUBLE PRECISION NOT NULL,
  "currency" TEXT NOT NULL DEFAULT 'USD',
  "quantity" DOUBLE PRECISION NOT NULL DEFAULT 0,
  "unit" TEXT,
  "description" TEXT NOT NULL,
  "metadata" JSONB,
  "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT "CostLedgerEntry_pkey" PRIMARY KEY ("id")
);

CREATE INDEX IF NOT EXISTS "CostLedgerEntry_workspaceId_createdAt_idx" ON "CostLedgerEntry"("workspaceId", "createdAt");
CREATE INDEX IF NOT EXISTS "CostLedgerEntry_category_currency_createdAt_idx" ON "CostLedgerEntry"("category", "currency", "createdAt");
CREATE INDEX IF NOT EXISTS "CostLedgerEntry_taskId_taskRunId_idx" ON "CostLedgerEntry"("taskId", "taskRunId");

-- Foreign keys
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'Task_projectContextId_fkey') THEN
    ALTER TABLE "Task"
      ADD CONSTRAINT "Task_projectContextId_fkey"
      FOREIGN KEY ("projectContextId") REFERENCES "ProjectContext"("id")
      ON DELETE SET NULL ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ProjectContext_workspaceId_fkey') THEN
    ALTER TABLE "ProjectContext"
      ADD CONSTRAINT "ProjectContext_workspaceId_fkey"
      FOREIGN KEY ("workspaceId") REFERENCES "Workspace"("id")
      ON DELETE CASCADE ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ProjectContext_repositoryId_fkey') THEN
    ALTER TABLE "ProjectContext"
      ADD CONSTRAINT "ProjectContext_repositoryId_fkey"
      FOREIGN KEY ("repositoryId") REFERENCES "GitRepository"("id")
      ON DELETE SET NULL ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ProjectContext_ownerUserId_fkey') THEN
    ALTER TABLE "ProjectContext"
      ADD CONSTRAINT "ProjectContext_ownerUserId_fkey"
      FOREIGN KEY ("ownerUserId") REFERENCES "User"("id")
      ON DELETE SET NULL ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'RouteDecision_workspaceId_fkey') THEN
    ALTER TABLE "RouteDecision"
      ADD CONSTRAINT "RouteDecision_workspaceId_fkey"
      FOREIGN KEY ("workspaceId") REFERENCES "Workspace"("id")
      ON DELETE CASCADE ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'RouteDecision_taskId_fkey') THEN
    ALTER TABLE "RouteDecision"
      ADD CONSTRAINT "RouteDecision_taskId_fkey"
      FOREIGN KEY ("taskId") REFERENCES "Task"("id")
      ON DELETE CASCADE ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'RouteDecision_taskRunId_fkey') THEN
    ALTER TABLE "RouteDecision"
      ADD CONSTRAINT "RouteDecision_taskRunId_fkey"
      FOREIGN KEY ("taskRunId") REFERENCES "TaskRun"("id")
      ON DELETE SET NULL ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'RouteDecision_projectContextId_fkey') THEN
    ALTER TABLE "RouteDecision"
      ADD CONSTRAINT "RouteDecision_projectContextId_fkey"
      FOREIGN KEY ("projectContextId") REFERENCES "ProjectContext"("id")
      ON DELETE SET NULL ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ModelRun_workspaceId_fkey') THEN
    ALTER TABLE "ModelRun"
      ADD CONSTRAINT "ModelRun_workspaceId_fkey"
      FOREIGN KEY ("workspaceId") REFERENCES "Workspace"("id")
      ON DELETE CASCADE ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ModelRun_taskId_fkey') THEN
    ALTER TABLE "ModelRun"
      ADD CONSTRAINT "ModelRun_taskId_fkey"
      FOREIGN KEY ("taskId") REFERENCES "Task"("id")
      ON DELETE CASCADE ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ModelRun_taskRunId_fkey') THEN
    ALTER TABLE "ModelRun"
      ADD CONSTRAINT "ModelRun_taskRunId_fkey"
      FOREIGN KEY ("taskRunId") REFERENCES "TaskRun"("id")
      ON DELETE SET NULL ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ModelRun_routeDecisionId_fkey') THEN
    ALTER TABLE "ModelRun"
      ADD CONSTRAINT "ModelRun_routeDecisionId_fkey"
      FOREIGN KEY ("routeDecisionId") REFERENCES "RouteDecision"("id")
      ON DELETE SET NULL ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ModelRun_projectContextId_fkey') THEN
    ALTER TABLE "ModelRun"
      ADD CONSTRAINT "ModelRun_projectContextId_fkey"
      FOREIGN KEY ("projectContextId") REFERENCES "ProjectContext"("id")
      ON DELETE SET NULL ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'CostLedgerEntry_workspaceId_fkey') THEN
    ALTER TABLE "CostLedgerEntry"
      ADD CONSTRAINT "CostLedgerEntry_workspaceId_fkey"
      FOREIGN KEY ("workspaceId") REFERENCES "Workspace"("id")
      ON DELETE CASCADE ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'CostLedgerEntry_taskId_fkey') THEN
    ALTER TABLE "CostLedgerEntry"
      ADD CONSTRAINT "CostLedgerEntry_taskId_fkey"
      FOREIGN KEY ("taskId") REFERENCES "Task"("id")
      ON DELETE SET NULL ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'CostLedgerEntry_taskRunId_fkey') THEN
    ALTER TABLE "CostLedgerEntry"
      ADD CONSTRAINT "CostLedgerEntry_taskRunId_fkey"
      FOREIGN KEY ("taskRunId") REFERENCES "TaskRun"("id")
      ON DELETE SET NULL ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'CostLedgerEntry_projectContextId_fkey') THEN
    ALTER TABLE "CostLedgerEntry"
      ADD CONSTRAINT "CostLedgerEntry_projectContextId_fkey"
      FOREIGN KEY ("projectContextId") REFERENCES "ProjectContext"("id")
      ON DELETE SET NULL ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'CostLedgerEntry_routeDecisionId_fkey') THEN
    ALTER TABLE "CostLedgerEntry"
      ADD CONSTRAINT "CostLedgerEntry_routeDecisionId_fkey"
      FOREIGN KEY ("routeDecisionId") REFERENCES "RouteDecision"("id")
      ON DELETE SET NULL ON UPDATE CASCADE;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'CostLedgerEntry_modelRunId_fkey') THEN
    ALTER TABLE "CostLedgerEntry"
      ADD CONSTRAINT "CostLedgerEntry_modelRunId_fkey"
      FOREIGN KEY ("modelRunId") REFERENCES "ModelRun"("id")
      ON DELETE SET NULL ON UPDATE CASCADE;
  END IF;
END $$;
