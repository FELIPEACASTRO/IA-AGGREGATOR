import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { spawn } from 'node:child_process';
import { TaskMode, TaskStatus } from '@prisma/client';
import { codexDb } from '@/server/codex/db';
import { appendTaskEvent, appendTaskLog, setTaskStatus } from '@/server/codex/events';
import { buildRoutePlan, buildUsageOutcome } from '@/server/codex/routing';

type Phase = 'provisioning' | 'repo_download' | 'setup' | 'maintenance' | 'agent' | 'validation' | 'pr_push';

const PHASE_TO_STATUS: Record<Phase, TaskStatus> = {
  provisioning: 'preparing_environment',
  repo_download: 'downloading_repository',
  setup: 'running_setup',
  maintenance: 'running_maintenance',
  agent: 'running_agent',
  validation: 'validating',
  pr_push: 'pr_ready',
};

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function currentPeriod() {
  return new Date().toISOString().slice(0, 7);
}

async function runCommand(input: {
  cwd: string;
  phase: Phase;
  taskId: string;
  lineOffset: number;
  command: string;
  args: string[];
}) {
  let line = input.lineOffset;
  await appendTaskLog({
    taskId: input.taskId,
    phase: input.phase,
    line: `$ ${input.command} ${input.args.join(' ')}`.trim(),
    lineNumber: ++line,
  });

  return new Promise<{ code: number; line: number; stdout: string; stderr: string }>((resolve, reject) => {
    const proc = spawn(input.command, input.args, {
      cwd: input.cwd,
      stdio: ['ignore', 'pipe', 'pipe'],
      env: process.env,
      shell: process.platform === 'win32',
    });

    let stdout = '';
    let stderr = '';

    proc.stdout.on('data', async (chunk: Buffer) => {
      const text = chunk.toString('utf-8');
      stdout += text;
      const lines = text.split(/\r?\n/).filter(Boolean);
      for (const current of lines) {
        await appendTaskLog({
          taskId: input.taskId,
          phase: input.phase,
          line: current,
          lineNumber: ++line,
        });
      }
    });

    proc.stderr.on('data', async (chunk: Buffer) => {
      const text = chunk.toString('utf-8');
      stderr += text;
      const lines = text.split(/\r?\n/).filter(Boolean);
      for (const current of lines) {
        await appendTaskLog({
          taskId: input.taskId,
          phase: input.phase,
          line: current,
          lineNumber: ++line,
          isError: true,
        });
      }
    });

    proc.on('error', reject);
    proc.on('close', (code) => {
      resolve({
        code: code ?? 1,
        line,
        stdout,
        stderr,
      });
    });
  });
}

function parseDiffFiles(patch: string) {
  const sections = patch.split('\ndiff --git ');
  return sections
    .map((section, idx) => (idx === 0 ? section : `diff --git ${section}`))
    .filter((section) => section.includes('diff --git '))
    .map((section) => {
      const pathMatch = section.match(/ b\/([^\n]+)/);
      const filePath = pathMatch?.[1] ?? `unknown-${Math.random().toString(16).slice(2)}`;
      const additions = (section.match(/^\+/gm) ?? []).length;
      const deletions = (section.match(/^-/gm) ?? []).length;
      const hunks = section
        .split('\n@@')
        .slice(1)
        .map((raw) => `@@${raw}`)
        .slice(0, 20);
      return {
        path: filePath,
        changeType: section.includes('new file mode') ? 'added' : section.includes('deleted file mode') ? 'deleted' : 'modified',
        additions,
        deletions,
        hunks,
      };
    });
}

async function updatePhase(taskId: string, phase: Phase, message: string, metadata?: Record<string, unknown>) {
  const status = PHASE_TO_STATUS[phase];
  await setTaskStatus(taskId, status);
  await appendTaskEvent({
    taskId,
    eventType: `${phase}.started`,
    status,
    message,
    metadata,
  });
}

async function consumeWorkspaceCredits(input: {
  workspaceId: string;
  taskId: string;
  creditsToBurn: number;
  description: string;
}) {
  const current = await codexDb.creditBalance.upsert({
    where: { workspaceId: input.workspaceId },
    update: {},
    create: {
      workspaceId: input.workspaceId,
      balance: 0,
      includedUsageLeft: 0,
    },
  });

  const includedConsumed = Math.min(current.includedUsageLeft, input.creditsToBurn);
  const requestedPaidCredits = Math.max(input.creditsToBurn - includedConsumed, 0);
  const paidConsumed = Math.min(current.balance, requestedPaidCredits);
  const overageCredits = Math.max(requestedPaidCredits - paidConsumed, 0);

  await codexDb.creditBalance.update({
    where: { workspaceId: input.workspaceId },
    data: {
      includedUsageLeft: {
        decrement: includedConsumed,
      },
      balance: {
        decrement: paidConsumed,
      },
    },
  });

  await codexDb.creditLedgerEntry.create({
    data: {
      workspaceId: input.workspaceId,
      type: 'CONSUMPTION',
      amount: -input.creditsToBurn,
      description: overageCredits > 0 ? `${input.description} (overage=${overageCredits})` : input.description,
      relatedTaskId: input.taskId,
    },
  });

  return {
    includedConsumed,
    paidConsumed,
    overageCredits,
  };
}

export async function executeTask(taskId: string) {
  const task = await codexDb.task.findUnique({
    where: { id: taskId },
    include: {
      environment: true,
      repository: true,
      input: true,
      projectContext: true,
    },
  });

  if (!task) return;

  const runStartedAt = new Date();
  let taskRun: Awaited<ReturnType<typeof codexDb.taskRun.create>> | null = null;
  let routeDecision: Awaited<ReturnType<typeof codexDb.routeDecision.create>> | null = null;
  let modelRun: Awaited<ReturnType<typeof codexDb.modelRun.create>> | null = null;
  let routePlanForError: ReturnType<typeof buildRoutePlan> | null = null;

  const runtimeRoot = path.join(process.cwd(), '.codex-runtime', 'tasks', task.id);
  const repoDir = path.join(runtimeRoot, 'repo');
  const artifactsDir = path.join(runtimeRoot, 'artifacts');
  let lineOffset = 0;
  let fallbackUsed = false;
  let fallbackReason: string | null = null;
  let artifactBytes = 0;

  try {
    const runCount = await codexDb.taskRun.count({ where: { taskId: task.id } });
    taskRun = await codexDb.taskRun.create({
      data: {
        taskId: task.id,
        runNumber: runCount + 1,
        status: 'queued',
        startedAt: runStartedAt,
      },
    });
    const taskRunId = taskRun.id;

    const routePlan = buildRoutePlan({
      mode: task.mode,
      prompt: task.prompt,
      internetMode: task.internetMode,
      bestOfN: task.bestOfN,
      repositoryName: task.repository?.fullName,
      sourceRef: task.sourceRef,
      attachments: task.input?.attachments ?? [],
      imageInputs: task.input?.imageInputs ?? [],
      voiceTranscript: task.input?.voiceTranscript,
    });
    routePlanForError = routePlan;

    const bootstrap = await codexDb.$transaction(async (tx) => {
      const createdRouteDecision = await tx.routeDecision.create({
        data: {
          workspaceId: task.workspaceId,
          taskId: task.id,
          taskRunId,
          projectContextId: task.projectContextId,
          serviceClass: routePlan.serviceClass,
          policyKey: routePlan.policyKey,
          policyVersion: routePlan.policyVersion,
          requestedMode: task.mode,
          selectedProvider: routePlan.selectedProvider,
          selectedModel: routePlan.selectedModel,
          fallbackProvider: routePlan.fallbackProvider,
          fallbackModel: routePlan.fallbackModel,
          reason: routePlan.reason,
          estimatedInputTokens: routePlan.estimatedInputTokens,
          estimatedOutputTokens: routePlan.estimatedOutputTokens,
          estimatedCost: routePlan.estimatedCost,
          status: 'STARTED',
          metadata: {
            bestOfN: task.bestOfN,
            internetMode: task.internetMode,
            sourceRef: task.sourceRef ?? null,
            attachmentCount: task.input?.attachments.length ?? 0,
            imageInputCount: task.input?.imageInputs.length ?? 0,
            hasVoiceTranscript: Boolean(task.input?.voiceTranscript),
          },
        },
      });

      const createdModelRun = await tx.modelRun.create({
        data: {
          workspaceId: task.workspaceId,
          taskId: task.id,
          taskRunId,
          routeDecisionId: createdRouteDecision.id,
          projectContextId: task.projectContextId,
          provider: routePlan.selectedProvider,
          model: routePlan.selectedModel,
          taskClass: routePlan.taskClass,
          estimatedCost: routePlan.estimatedCost,
          routePolicyVersion: routePlan.policyVersion,
          status: 'STARTED',
          metadata: {
            serviceClass: routePlan.serviceClass,
          },
        },
      });

      await tx.costLedgerEntry.create({
        data: {
          workspaceId: task.workspaceId,
          taskId: task.id,
          taskRunId,
          projectContextId: task.projectContextId,
          routeDecisionId: createdRouteDecision.id,
          modelRunId: createdModelRun.id,
          category: 'MODEL_ESTIMATE',
          amount: routePlan.estimatedCost,
          currency: 'USD',
          quantity: routePlan.estimatedInputTokens + routePlan.estimatedOutputTokens,
          unit: 'token',
          description: `Estimativa inicial do roteador para ${routePlan.selectedProvider}/${routePlan.selectedModel}`,
          metadata: {
            serviceClass: routePlan.serviceClass,
            policyVersion: routePlan.policyVersion,
          },
        },
      });

      return {
        routeDecision: createdRouteDecision,
        modelRun: createdModelRun,
      };
    });

    routeDecision = bootstrap.routeDecision;
    modelRun = bootstrap.modelRun;

    if (!taskRun || !routeDecision || !modelRun) {
      throw new Error('Falha ao inicializar registros de execucao');
    }

    await setTaskStatus(task.id, 'queued', { startedAt: new Date(), errorMessage: null });
    await appendTaskEvent({
      taskId: task.id,
      eventType: 'task.queued',
      status: 'queued',
      message: 'Task enfileirada para execucao cloud',
    });
    await appendTaskEvent({
      taskId: task.id,
      eventType: 'route.decision',
      status: 'queued',
      message: 'Roteamento e politica de custo aplicados',
      metadata: {
        serviceClass: routePlan.serviceClass,
        selectedProvider: routePlan.selectedProvider,
        selectedModel: routePlan.selectedModel,
        estimatedCost: routePlan.estimatedCost,
        creditsToBurn: routePlan.creditsToBurn,
      },
    });

    await updatePhase(task.id, 'provisioning', 'Provisionando sandbox isolado');
    await mkdir(repoDir, { recursive: true });
    await mkdir(artifactsDir, { recursive: true });
    await appendTaskLog({
      taskId: task.id,
      phase: 'provisioning',
      line: `Workspace: ${runtimeRoot}`,
      lineNumber: ++lineOffset,
    });
    await sleep(250);

    await updatePhase(task.id, 'repo_download', 'Preparando repositorio');
    let repoReady = false;
    if (task.repository?.cloneUrl) {
      const clone = await runCommand({
        cwd: runtimeRoot,
        phase: 'repo_download',
        taskId: task.id,
        lineOffset,
        command: 'git',
        args: ['clone', '--depth', '1', '--branch', task.baseBranch || task.repository.defaultBranch || 'main', task.repository.cloneUrl, 'repo'],
      });
      lineOffset = clone.line;
      repoReady = clone.code === 0;
      if (!repoReady) {
        fallbackUsed = true;
        fallbackReason = `git clone falhou com exit code ${clone.code}`;
      }
    }

    if (!repoReady) {
      fallbackUsed = true;
      fallbackReason = fallbackReason || 'repositorio indisponivel, workspace local inicializado';
      await runCommand({
        cwd: repoDir,
        phase: 'repo_download',
        taskId: task.id,
        lineOffset,
        command: 'git',
        args: ['init'],
      });
      await writeFile(path.join(repoDir, 'README.md'), '# Codex Cloud Workspace\n');
      await runCommand({
        cwd: repoDir,
        phase: 'repo_download',
        taskId: task.id,
        lineOffset,
        command: 'git',
        args: ['add', '.'],
      });
      await runCommand({
        cwd: repoDir,
        phase: 'repo_download',
        taskId: task.id,
        lineOffset,
        command: 'git',
        args: ['commit', '-m', 'chore: bootstrap task workspace'],
      });
    }

    await appendTaskEvent({
      taskId: task.id,
      eventType: 'repository.cloned',
      status: 'cloning_repository',
      message: 'Repositorio pronto no sandbox',
    });
    await setTaskStatus(task.id, 'cloning_repository');
    await sleep(250);

    await updatePhase(task.id, 'setup', 'Executando setup do environment');
    if (task.environment?.setupScript) {
      const setup = await runCommand({
        cwd: repoDir,
        phase: 'setup',
        taskId: task.id,
        lineOffset,
        command: process.platform === 'win32' ? 'powershell' : 'bash',
        args:
          process.platform === 'win32'
            ? ['-NoProfile', '-Command', task.environment.setupScript]
            : ['-lc', task.environment.setupScript],
      });
      lineOffset = setup.line;
      if (setup.code !== 0) {
        throw new Error(`Setup falhou com exit code ${setup.code}`);
      }
    } else {
      await appendTaskLog({
        taskId: task.id,
        phase: 'setup',
        line: 'Sem setup script configurado, seguindo com defaults.',
        lineNumber: ++lineOffset,
      });
    }

    await appendTaskEvent({
      taskId: task.id,
      eventType: 'setup.completed',
      status: 'running_setup',
      message: 'Setup concluido',
    });

    await updatePhase(task.id, 'maintenance', 'Executando manutencao de cache');
    if (task.environment?.maintenanceScript) {
      const maintenance = await runCommand({
        cwd: repoDir,
        phase: 'maintenance',
        taskId: task.id,
        lineOffset,
        command: process.platform === 'win32' ? 'powershell' : 'bash',
        args:
          process.platform === 'win32'
            ? ['-NoProfile', '-Command', task.environment.maintenanceScript]
            : ['-lc', task.environment.maintenanceScript],
      });
      lineOffset = maintenance.line;
    } else {
      await appendTaskLog({
        taskId: task.id,
        phase: 'maintenance',
        line: 'Sem maintenance script configurado.',
        lineNumber: ++lineOffset,
      });
    }

    await updatePhase(task.id, 'agent', task.mode === TaskMode.ASK ? 'Executando modo Ask' : 'Executando modo Code');
    if (task.mode === TaskMode.ASK) {
      const answer = [
        'Resposta baseada no estado atual do workspace cloud.',
        `Prompt: ${task.prompt}`,
        'Para alteracoes de codigo, use o modo Code ou follow-up em Code.',
      ].join('\n');
      artifactBytes += Buffer.byteLength(answer, 'utf-8');
      await writeFile(path.join(artifactsDir, 'ask-summary.md'), answer, 'utf-8');
      await codexDb.taskArtifact.create({
        data: {
          taskId: task.id,
          artifactType: 'summary',
          title: 'Ask Summary',
          contentType: 'text/markdown',
          url: path.join(artifactsDir, 'ask-summary.md'),
        },
      });
    } else {
      const outDir = path.join(repoDir, 'codex-output');
      await mkdir(outDir, { recursive: true });
      const fileName = `task-${task.id}.md`;
      const outputContent = [
        '# Code Task Output',
        '',
        `Task: ${task.title}`,
        `Prompt: ${task.prompt}`,
        '',
        'Esta alteracao foi gerada no ambiente cloud para evidenciar diff e pipeline completo.',
      ].join('\n');
      artifactBytes += Buffer.byteLength(outputContent, 'utf-8');
      await writeFile(path.join(outDir, fileName), outputContent, 'utf-8');
      await runCommand({
        cwd: repoDir,
        phase: 'agent',
        taskId: task.id,
        lineOffset,
        command: 'git',
        args: ['add', '.'],
      });
      await appendTaskLog({
        taskId: task.id,
        phase: 'agent',
        line: `Arquivo alterado: codex-output/${fileName}`,
        lineNumber: ++lineOffset,
      });
    }

    await appendTaskEvent({
      taskId: task.id,
      eventType: 'agent.progress',
      status: 'running_agent',
      message: 'Execucao de agente concluida',
    });
    await sleep(200);

    await updatePhase(task.id, 'validation', 'Executando validacoes');
    const validation = await runCommand({
      cwd: repoDir,
      phase: 'validation',
      taskId: task.id,
      lineOffset,
      command: 'git',
      args: ['status', '--short'],
    });
    lineOffset = validation.line;
    await appendTaskEvent({
      taskId: task.id,
      eventType: 'validation.completed',
      status: 'validating',
      message: 'Validacoes concluidas',
      metadata: {
        exitCode: validation.code,
      },
    });

    await setTaskStatus(task.id, 'generating_diff');
    await appendTaskEvent({
      taskId: task.id,
      eventType: 'diff.ready',
      status: 'generating_diff',
      message: 'Gerando diff revisavel',
    });

    const diff = await runCommand({
      cwd: repoDir,
      phase: 'validation',
      taskId: task.id,
      lineOffset,
      command: 'git',
      args: ['diff', '--cached', '--no-color'],
    });
    lineOffset = diff.line;

    const patch = diff.stdout || '';
    artifactBytes += Buffer.byteLength(patch, 'utf-8');
    const files = parseDiffFiles(patch);
    const snapshot = await codexDb.diffSnapshot.upsert({
      where: { taskId: task.id },
      update: {
        patch,
        summary: files.length > 0 ? `${files.length} arquivo(s) alterado(s)` : 'Sem alteracoes detectadas',
      },
      create: {
        taskId: task.id,
        patch,
        summary: files.length > 0 ? `${files.length} arquivo(s) alterado(s)` : 'Sem alteracoes detectadas',
      },
    });

    await codexDb.diffFile.deleteMany({ where: { diffSnapshotId: snapshot.id } });
    for (const file of files) {
      const diffFile = await codexDb.diffFile.create({
        data: {
          diffSnapshotId: snapshot.id,
          path: file.path,
          changeType: file.changeType,
          additions: file.additions,
          deletions: file.deletions,
        },
      });

      for (const hunk of file.hunks) {
        await codexDb.diffHunk.create({
          data: {
            diffFileId: diffFile.id,
            header: hunk.split('\n')[0] || '@@',
            content: hunk,
          },
        });
      }
    }

    const summaryPath = path.join(artifactsDir, 'summary.md');
    const summaryMarkdown = [
      '# Task Summary',
      '',
      `- Mode: ${task.mode}`,
      `- Status: completed`,
      `- Files changed: ${files.length}`,
      `- Branch: ${task.resultBranch || `codex/${task.id}`}`,
      `- Project: ${task.projectContext?.name || 'General Engineering'}`,
      `- Route: ${routePlan.serviceClass.toLowerCase()} via ${routePlan.selectedProvider}/${routePlan.selectedModel}`,
    ].join('\n');
    artifactBytes += Buffer.byteLength(summaryMarkdown, 'utf-8');
    await writeFile(summaryPath, summaryMarkdown, 'utf-8');

    await codexDb.taskArtifact.create({
      data: {
        taskId: task.id,
        artifactType: 'summary',
        title: 'Task Summary',
        contentType: 'text/markdown',
        url: summaryPath,
      },
    });

    await updatePhase(task.id, 'pr_push', 'Task pronta para PR');
    await codexDb.pullRequest.upsert({
      where: { taskId: task.id },
      update: {
        title: `feat(codex): ${task.title}`,
        body: `Task ${task.id} pronta para criacao/atualizacao de PR.`,
        branch: task.resultBranch || `codex/${task.id}`,
        status: 'none',
      },
      create: {
        taskId: task.id,
        repositoryId: task.repositoryId,
        title: `feat(codex): ${task.title}`,
        body: `Task ${task.id} pronta para criacao/atualizacao de PR.`,
        branch: task.resultBranch || `codex/${task.id}`,
        status: 'none',
      },
    });

    const latencyMs = Math.max(1, Date.now() - runStartedAt.getTime());
    const usageOutcome = buildUsageOutcome({
      taskMode: task.mode,
      prompt: task.prompt,
      diffFileCount: files.length,
      artifactBytes,
      fallbackUsed,
      estimatedInputTokens: routePlan.estimatedInputTokens,
      estimatedOutputTokens: routePlan.estimatedOutputTokens,
      estimatedCost: routePlan.estimatedCost,
      latencyMs,
    });
    const creditConsumption = await consumeWorkspaceCredits({
      workspaceId: task.workspaceId,
      taskId: task.id,
      creditsToBurn: routePlan.creditsToBurn,
      description: `Consumo ${routePlan.serviceClass.toLowerCase()} da task ${task.id}`,
    });

    await codexDb.routeDecision.update({
      where: { id: routeDecision.id },
      data: {
        fallbackUsed,
        reason: fallbackReason ? `${routePlan.reason}; ${fallbackReason}` : routePlan.reason,
        status: 'COMPLETED',
        metadata: {
          bestOfN: task.bestOfN,
          internetMode: task.internetMode,
          sourceRef: task.sourceRef ?? null,
          attachmentCount: task.input?.attachments.length ?? 0,
          imageInputCount: task.input?.imageInputs.length ?? 0,
          hasVoiceTranscript: Boolean(task.input?.voiceTranscript),
          changedFiles: files.length,
          latencyMs,
        },
      },
    });

    await codexDb.modelRun.update({
      where: { id: modelRun.id },
      data: {
        inputTokens: usageOutcome.inputTokens,
        outputTokens: usageOutcome.outputTokens,
        latencyMs,
        actualCost: usageOutcome.actualCost,
        fallbackUsed,
        status: usageOutcome.status,
        metadata: {
          ...usageOutcome.metadata,
          changedFiles: files.length,
          serviceClass: routePlan.serviceClass,
        },
      },
    });

    await codexDb.costLedgerEntry.create({
      data: {
        workspaceId: task.workspaceId,
        taskId: task.id,
        taskRunId: taskRun.id,
        projectContextId: task.projectContextId,
        routeDecisionId: routeDecision.id,
        modelRunId: modelRun.id,
        category: 'MODEL_EXECUTION',
        amount: usageOutcome.actualCost,
        currency: 'USD',
        quantity: usageOutcome.inputTokens + usageOutcome.outputTokens,
        unit: 'token',
        description: `Execucao concluida em ${routePlan.selectedProvider}/${routePlan.selectedModel}`,
        metadata: {
          serviceClass: routePlan.serviceClass,
          fallbackUsed,
          latencyMs,
          changedFiles: files.length,
        },
      },
    });

    await codexDb.costLedgerEntry.create({
      data: {
        workspaceId: task.workspaceId,
        taskId: task.id,
        taskRunId: taskRun.id,
        projectContextId: task.projectContextId,
        routeDecisionId: routeDecision.id,
        modelRunId: modelRun.id,
        category: 'ARTIFACT_STORAGE',
        amount: Number((artifactBytes / 1_000_000).toFixed(6)),
        currency: 'USD',
        quantity: artifactBytes,
        unit: 'byte',
        description: 'Persistencia de artefatos e diff da task',
        metadata: {
          artifactBytes,
          changedFiles: files.length,
        },
      },
    });

    await setTaskStatus(task.id, 'completed', { completedAt: new Date() });
    await appendTaskEvent({
      taskId: task.id,
      eventType: 'task.completed',
      status: 'completed',
      message: 'Task concluida com sucesso',
      metadata: {
        changedFiles: files.length,
        routeServiceClass: routePlan.serviceClass,
        actualCostUsd: usageOutcome.actualCost,
        creditsBurned: routePlan.creditsToBurn,
      },
    });
    await appendTaskEvent({
      taskId: task.id,
      eventType: 'finops.ledger_recorded',
      status: 'completed',
      message: 'Custos e consumo de creditos registrados',
      metadata: {
        includedConsumed: creditConsumption.includedConsumed,
        paidConsumed: creditConsumption.paidConsumed,
        overageCredits: creditConsumption.overageCredits,
      },
    });

    await codexDb.taskRun.update({
      where: { id: taskRun.id },
      data: {
        status: 'completed',
        completedAt: new Date(),
      },
    });

    await codexDb.usageEntry.createMany({
      data: [
        {
          workspaceId: task.workspaceId,
          repositoryId: task.repositoryId,
          taskId: task.id,
          metric: 'task_run',
          amount: 1,
          unit: 'run',
          period: currentPeriod(),
        },
        {
          workspaceId: task.workspaceId,
          repositoryId: task.repositoryId,
          taskId: task.id,
          metric: `service_class_${routePlan.serviceClass.toLowerCase()}`,
          amount: 1,
          unit: 'run',
          period: currentPeriod(),
        },
        {
          workspaceId: task.workspaceId,
          repositoryId: task.repositoryId,
          taskId: task.id,
          metric: 'input_tokens',
          amount: usageOutcome.inputTokens,
          unit: 'token',
          period: currentPeriod(),
        },
        {
          workspaceId: task.workspaceId,
          repositoryId: task.repositoryId,
          taskId: task.id,
          metric: 'output_tokens',
          amount: usageOutcome.outputTokens,
          unit: 'token',
          period: currentPeriod(),
        },
        {
          workspaceId: task.workspaceId,
          repositoryId: task.repositoryId,
          taskId: task.id,
          metric: 'model_cost_usd',
          amount: usageOutcome.actualCost,
          unit: 'usd',
          period: currentPeriod(),
        },
        {
          workspaceId: task.workspaceId,
          repositoryId: task.repositoryId,
          taskId: task.id,
          metric: 'credits_burned',
          amount: routePlan.creditsToBurn,
          unit: 'credit',
          period: currentPeriod(),
        },
      ],
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Falha desconhecida no pipeline';
    const routePlan = routePlanForError;

    if (routeDecision && routePlan) {
      await codexDb.routeDecision
        .update({
          where: { id: routeDecision.id },
          data: {
            fallbackUsed,
            reason: fallbackReason ? `${routePlan.reason}; ${fallbackReason}` : routePlan.reason,
            status: 'FAILED',
          },
        })
        .catch(() => undefined);
    }

    if (modelRun && routePlan) {
      await codexDb.modelRun
        .update({
          where: { id: modelRun.id },
          data: {
            fallbackUsed,
            status: 'FAILED',
            metadata: {
              errorMessage: message,
              serviceClass: routePlan.serviceClass,
            },
          },
        })
        .catch(() => undefined);
    }

    if (taskRun) {
      await codexDb.taskRun
        .update({
          where: { id: taskRun.id },
          data: {
            status: 'failed',
            failureReason: message,
            completedAt: new Date(),
          },
        })
        .catch(() => undefined);
    }

    if (taskRun && routePlan) {
      await codexDb.costLedgerEntry
        .create({
          data: {
            workspaceId: task.workspaceId,
            taskId: task.id,
            taskRunId: taskRun.id,
            projectContextId: task.projectContextId,
            routeDecisionId: routeDecision?.id,
            modelRunId: modelRun?.id,
            category: 'FAILED_EXECUTION',
            amount: routePlan.estimatedCost,
            currency: 'USD',
            quantity: routePlan.estimatedInputTokens + routePlan.estimatedOutputTokens,
            unit: 'token',
            description: `Execucao falhou antes de concluir ${routePlan.selectedProvider}/${routePlan.selectedModel}`,
            metadata: {
              errorMessage: message,
              fallbackUsed,
            },
          },
        })
        .catch(() => undefined);
    }

    await appendTaskLog({
      taskId: task.id,
      phase: 'validation',
      line: message,
      lineNumber: ++lineOffset,
      isError: true,
    });
    await setTaskStatus(task.id, 'failed', {
      failedAt: new Date(),
      errorMessage: message,
    });
    await appendTaskEvent({
      taskId: task.id,
      eventType: 'task.failed',
      status: 'failed',
      message,
      metadata: {
        routeServiceClass: routePlan?.serviceClass,
        selectedProvider: routePlan?.selectedProvider,
        selectedModel: routePlan?.selectedModel,
      },
    });
  } finally {
    if (taskRun) {
      await codexDb.taskRun.updateMany({
        where: { id: taskRun.id, completedAt: null },
        data: {
          completedAt: new Date(),
        },
      });
    }
  }
}

export async function loadTaskSummary(taskId: string) {
  const task = await codexDb.task.findUnique({
    where: { id: taskId },
    include: {
      events: { orderBy: { createdAt: 'asc' } },
      logs: { orderBy: [{ createdAt: 'asc' }, { lineNumber: 'asc' }] },
      diffSnapshot: {
        include: {
          files: {
            include: {
              hunks: true,
            },
          },
        },
      },
      pullRequest: true,
      artifacts: true,
      repository: true,
      environment: true,
      projectContext: true,
      evidences: true,
      routeDecisions: {
        include: {
          modelRuns: true,
          costLedgerEntries: true,
        },
        orderBy: { createdAt: 'desc' },
      },
      modelRuns: {
        orderBy: { createdAt: 'desc' },
      },
      costLedgerEntries: {
        orderBy: { createdAt: 'desc' },
      },
    },
  });

  if (!task) return null;
  const summaryArtifact = task.artifacts.find((item) => item.artifactType === 'summary');
  let summaryText = '';
  if (summaryArtifact?.url) {
    try {
      summaryText = await readFile(summaryArtifact.url, 'utf-8');
    } catch {
      summaryText = '';
    }
  }

  return {
    ...task,
    summaryText,
  };
}
