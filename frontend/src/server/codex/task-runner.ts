import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { spawn } from 'node:child_process';
import { TaskMode, TaskStatus } from '@prisma/client';
import { codexDb } from '@/server/codex/db';
import { appendTaskEvent, appendTaskLog, setTaskStatus } from '@/server/codex/events';
import { invokeChatGateway } from '@/server/ai/gateway';

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

async function runCommand(input: {
  cwd: string;
  phase: Phase;
  taskId: string;
  lineOffset: number;
  command: string;
  args: string[];
  env?: Record<string, string | undefined>;
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
      env: {
        ...process.env,
        ...input.env,
      },
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

export async function executeTask(taskId: string) {
  const task = await codexDb.task.findUnique({
    where: { id: taskId },
    include: {
      environment: true,
      repository: true,
    },
  });

  if (!task) return;

  const runCount = await codexDb.taskRun.count({ where: { taskId: task.id } });
  await codexDb.taskRun.create({
    data: {
      taskId: task.id,
      runNumber: runCount + 1,
      status: 'queued',
      startedAt: new Date(),
    },
  });

  const runtimeRoot = path.join(process.cwd(), '.codex-runtime', 'tasks', task.id, `run-${runCount + 1}`);
  const repoDir = path.join(runtimeRoot, 'repo');
  const artifactsDir = path.join(runtimeRoot, 'artifacts');
  const resultBranch = task.resultBranch || `codex/${task.id}`;
  let lineOffset = 0;

  try {
    await setTaskStatus(task.id, 'queued', { startedAt: new Date(), errorMessage: null });
    await appendTaskEvent({
      taskId: task.id,
      eventType: 'task.queued',
      status: 'queued',
      message: 'Task enfileirada para execucao cloud',
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

    if (task.mode === TaskMode.ASK) {
      await updatePhase(task.id, 'agent', 'Executando modo Ask');
    }
    if (task.mode === TaskMode.ASK) {
      const result = await invokeChatGateway({
        prompt: task.prompt,
      });
      const answer = result.content.trim();
      const artifactPath = path.join(artifactsDir, 'ask-summary.md');
      await writeFile(artifactPath, answer, 'utf-8');
      await codexDb.taskArtifact.create({
        data: {
          taskId: task.id,
          artifactType: 'summary',
          title: 'Ask Summary',
          contentType: 'text/markdown',
          url: artifactPath,
          metadata: {
            providerUsed: result.providerUsed,
            modelUsed: result.modelUsed,
            agentUsed: result.agentUsed,
            agentVersion: result.agentVersion,
            attempts: result.attempts,
            fallbackUsed: result.fallbackUsed,
          },
        },
      });

      await appendTaskEvent({
        taskId: task.id,
        eventType: 'agent.answer.generated',
        status: 'running_agent',
        message: 'Resposta Ask persistida com provider real',
        metadata: {
          providerUsed: result.providerUsed,
          modelUsed: result.modelUsed,
          agentUsed: result.agentUsed,
        },
      });
    } else {
      if (!task.repository?.cloneUrl) {
        throw new Error('Task CODE requer repositorio com cloneUrl real configurado');
      }
      if (!task.environment) {
        throw new Error('Task CODE requer environment real configurado');
      }

      await updatePhase(task.id, 'repo_download', 'Clonando repositorio real');
      const clone = await runCommand({
        cwd: runtimeRoot,
        phase: 'repo_download',
        taskId: task.id,
        lineOffset,
        command: 'git',
        args: ['clone', '--depth', '1', '--branch', task.baseBranch || task.repository.defaultBranch || 'main', task.repository.cloneUrl, 'repo'],
      });
      lineOffset = clone.line;
      if (clone.code !== 0) {
        throw new Error(`Falha ao clonar repositorio com exit code ${clone.code}`);
      }

      await runCommand({
        cwd: repoDir,
        phase: 'repo_download',
        taskId: task.id,
        lineOffset,
        command: 'git',
        args: ['checkout', '-B', resultBranch],
      });

      await appendTaskEvent({
        taskId: task.id,
        eventType: 'repository.cloned',
        status: 'cloning_repository',
        message: 'Repositorio real clonado com sucesso',
        metadata: {
          repository: task.repository.fullName,
          branch: task.baseBranch || task.repository.defaultBranch || 'main',
          resultBranch,
        },
      });
      await setTaskStatus(task.id, 'cloning_repository');

      await updatePhase(task.id, 'setup', 'Executando setup do environment');
      if (task.environment.setupScript) {
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
          line: 'Sem setup script configurado para este environment.',
          lineNumber: ++lineOffset,
        });
      }

      await updatePhase(task.id, 'maintenance', 'Executando manutencao de cache');
      if (task.environment.maintenanceScript) {
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
        if (maintenance.code !== 0) {
          throw new Error(`Maintenance falhou com exit code ${maintenance.code}`);
        }
      } else {
        await appendTaskLog({
          taskId: task.id,
          phase: 'maintenance',
          line: 'Sem maintenance script configurado.',
          lineNumber: ++lineOffset,
        });
      }

      await updatePhase(task.id, 'agent', 'Executando modo Code');
      const codeExecutor = process.env.CODEX_CODE_EXECUTOR;
      if (!codeExecutor) {
        throw new Error('Nenhum executor CODE configurado para este ambiente');
      }

      const agentRun = await runCommand({
        cwd: repoDir,
        phase: 'agent',
        taskId: task.id,
        lineOffset,
        command: process.platform === 'win32' ? 'powershell' : 'bash',
        args:
          process.platform === 'win32'
            ? ['-NoProfile', '-Command', codeExecutor]
            : ['-lc', codeExecutor],
        env: {
          CODEX_TASK_ID: task.id,
          CODEX_TASK_TITLE: task.title,
          CODEX_TASK_PROMPT: task.prompt,
          CODEX_TASK_MODE: task.mode,
          CODEX_RESULT_BRANCH: resultBranch,
          CODEX_REPOSITORY_FULL_NAME: task.repository.fullName,
          CODEX_REPO_DIR: repoDir,
        },
      });
      lineOffset = agentRun.line;
      if (agentRun.code !== 0) {
        throw new Error(`Executor CODE falhou com exit code ${agentRun.code}`);
      }
    }

    await appendTaskEvent({
      taskId: task.id,
      eventType: 'agent.progress',
      status: 'running_agent',
      message: 'Execucao de agente concluida',
    });
    await sleep(200);

    if (task.mode === TaskMode.CODE) {
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
    } else {
      await appendTaskEvent({
        taskId: task.id,
        eventType: 'validation.skipped',
        status: 'running_agent',
        message: 'Validacoes Git nao se aplicam ao modo Ask',
      });
    }

    let files: Array<ReturnType<typeof parseDiffFiles>[number]> = [];
    if (task.mode === TaskMode.CODE) {
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
        args: ['diff', '--no-color', 'HEAD'],
      });
      lineOffset = diff.line;

      const patch = diff.stdout || '';
      files = parseDiffFiles(patch);
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
    }

    const summaryPath = path.join(artifactsDir, 'summary.md');
    await writeFile(
      summaryPath,
      [
        '# Task Summary',
        '',
        `- Mode: ${task.mode}`,
        `- Status: completed`,
        `- Files changed: ${files.length}`,
        `- Branch: ${resultBranch}`,
      ].join('\n'),
      'utf-8'
    );

    await codexDb.taskArtifact.create({
      data: {
        taskId: task.id,
        artifactType: 'summary',
        title: 'Task Summary',
        contentType: 'text/markdown',
        url: summaryPath,
      },
    });

    if (task.mode === TaskMode.CODE) {
      await appendTaskEvent({
        taskId: task.id,
        eventType: 'pr.skipped',
        status: 'completed',
        message: 'Nenhum conector de pull request configurado para automacao real',
        metadata: {
          branch: resultBranch,
        },
      });
    }

    await setTaskStatus(task.id, 'completed', { completedAt: new Date() });
    await appendTaskEvent({
      taskId: task.id,
      eventType: 'task.completed',
      status: 'completed',
      message: 'Task concluida com sucesso',
      metadata: {
        changedFiles: files.length,
      },
    });

    await codexDb.usageEntry.create({
      data: {
        workspaceId: task.workspaceId,
        repositoryId: task.repositoryId,
        taskId: task.id,
        metric: 'task_run',
        amount: 1,
        unit: 'run',
        period: new Date().toISOString().slice(0, 7),
      },
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Falha desconhecida no pipeline';
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
    });
  } finally {
    await codexDb.taskRun.updateMany({
      where: { taskId: task.id, completedAt: null },
      data: {
        completedAt: new Date(),
      },
    });
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
      evidences: true,
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

