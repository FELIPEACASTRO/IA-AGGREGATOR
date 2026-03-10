/** @jest-environment node */

jest.mock('node:fs/promises', () => ({
  mkdir: jest.fn(),
  readFile: jest.fn(),
  writeFile: jest.fn(),
}));

jest.mock('@/server/codex/events', () => ({
  appendTaskEvent: jest.fn(),
  appendTaskLog: jest.fn(),
  setTaskStatus: jest.fn(),
}));

jest.mock('@/server/codex/routing', () => ({
  buildRoutePlan: jest.fn(),
  buildUsageOutcome: jest.fn(),
}));

jest.mock('@/server/codex/db', () => ({
  codexDb: {
    task: { findUnique: jest.fn() },
    taskRun: { count: jest.fn(), create: jest.fn(), update: jest.fn(), updateMany: jest.fn() },
    routeDecision: { update: jest.fn() },
    modelRun: { update: jest.fn() },
    costLedgerEntry: { create: jest.fn() },
    $transaction: jest.fn(),
  },
}));

import { mkdir } from 'node:fs/promises';
import { codexDb } from '@/server/codex/db';
import { appendTaskEvent, appendTaskLog, setTaskStatus } from '@/server/codex/events';
import { buildRoutePlan } from '@/server/codex/routing';
import { executeTask } from '@/server/codex/task-runner';

describe('executeTask hardening', () => {
  const mkdirMock = mkdir as unknown as jest.Mock;
  const db = codexDb as unknown as {
    task: { findUnique: jest.Mock };
    taskRun: { count: jest.Mock; create: jest.Mock; update: jest.Mock; updateMany: jest.Mock };
    routeDecision: { update: jest.Mock };
    modelRun: { update: jest.Mock };
    costLedgerEntry: { create: jest.Mock };
    $transaction: jest.Mock;
  };
  const setTaskStatusMock = setTaskStatus as unknown as jest.Mock;
  const appendTaskEventMock = appendTaskEvent as unknown as jest.Mock;
  const appendTaskLogMock = appendTaskLog as unknown as jest.Mock;
  const buildRoutePlanMock = buildRoutePlan as unknown as jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();

    db.task.findUnique.mockResolvedValue({
      id: 'task-1',
      workspaceId: 'workspace-1',
      repositoryId: 'repo-1',
      environmentId: null,
      projectContextId: 'project-1',
      createdById: 'user-1',
      title: 'Task',
      prompt: 'Corrigir bug no endpoint',
      mode: 'CODE',
      status: 'queued',
      sourceRef: null,
      baseBranch: 'main',
      resultBranch: 'codex/task-1',
      internetMode: 'OFF',
      bestOfN: 1,
      repository: {
        id: 'repo-1',
        fullName: 'org/repo',
        defaultBranch: 'main',
        cloneUrl: 'https://example.com/repo.git',
      },
      environment: null,
      input: {
        attachments: [],
        imageInputs: [],
        voiceTranscript: null,
      },
      projectContext: { id: 'project-1', name: 'Projeto 1' },
    });

    db.taskRun.count.mockResolvedValue(0);
    db.taskRun.create.mockResolvedValue({ id: 'run-1' });
    db.taskRun.update.mockResolvedValue({});
    db.taskRun.updateMany.mockResolvedValue({ count: 1 });
    db.routeDecision.update.mockResolvedValue({});
    db.modelRun.update.mockResolvedValue({});
    db.costLedgerEntry.create.mockResolvedValue({});

    buildRoutePlanMock.mockReturnValue({
      serviceClass: 'BALANCED',
      taskClass: 'engineering_task',
      policyKey: 'policy',
      policyVersion: '2026-03-09',
      selectedProvider: 'openai',
      selectedModel: 'gpt-4.1',
      fallbackProvider: 'anthropic',
      fallbackModel: 'claude-3-7-sonnet',
      estimatedInputTokens: 120,
      estimatedOutputTokens: 300,
      estimatedCost: 0.22,
      creditsToBurn: 2,
      reason: 'route selected',
    });

    db.$transaction.mockImplementation(async (callback: (tx: unknown) => Promise<unknown>) => {
      const tx = {
        routeDecision: {
          create: jest.fn().mockResolvedValue({ id: 'route-1' }),
        },
        modelRun: {
          create: jest.fn().mockResolvedValue({ id: 'model-1' }),
        },
        costLedgerEntry: {
          create: jest.fn().mockResolvedValue({ id: 'estimate-cost-1' }),
        },
      };
      return callback(tx);
    });

    appendTaskEventMock.mockResolvedValue(undefined);
    appendTaskLogMock.mockResolvedValue(undefined);
    setTaskStatusMock.mockResolvedValue(undefined);
  });

  it('marks taskRun, routeDecision and modelRun as failed when bootstrap phase crashes', async () => {
    mkdirMock.mockRejectedValueOnce(new Error('mkdir failure'));

    await executeTask('task-1');

    expect(db.routeDecision.update).toHaveBeenCalledWith(
      expect.objectContaining({
        where: { id: 'route-1' },
        data: expect.objectContaining({ status: 'FAILED' }),
      })
    );
    expect(db.modelRun.update).toHaveBeenCalledWith(
      expect.objectContaining({
        where: { id: 'model-1' },
        data: expect.objectContaining({ status: 'FAILED' }),
      })
    );
    expect(db.taskRun.update).toHaveBeenCalledWith(
      expect.objectContaining({
        where: { id: 'run-1' },
        data: expect.objectContaining({ status: 'failed' }),
      })
    );
    expect(db.costLedgerEntry.create).toHaveBeenCalledWith(
      expect.objectContaining({
        data: expect.objectContaining({
          category: 'FAILED_EXECUTION',
          taskRunId: 'run-1',
          routeDecisionId: 'route-1',
          modelRunId: 'model-1',
        }),
      })
    );
    expect(setTaskStatusMock).toHaveBeenCalledWith(
      'task-1',
      'failed',
      expect.objectContaining({ errorMessage: 'mkdir failure' })
    );
    expect(db.taskRun.updateMany).toHaveBeenCalledWith({
      where: { id: 'run-1', completedAt: null },
      data: { completedAt: expect.any(Date) },
    });
  });
});
