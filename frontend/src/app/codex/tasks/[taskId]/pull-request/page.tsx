'use client';

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { CodexShell } from '@/components/codex/codex-shell';
import { PullRequestPanel } from '@/components/codex/pull-request-panel';
import { codexApi } from '@/lib/codex-api';

type TaskPayload = {
  id: string;
  resultBranch?: string | null;
  pullRequest?: {
    id: string;
    status: string;
    title: string;
    body: string;
    url?: string | null;
  } | null;
};

export default function TaskPullRequestPage() {
  const params = useParams<{ taskId: string }>();
  const taskId = params.taskId;
  const [task, setTask] = useState<TaskPayload | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const payload = (await codexApi.getTask(taskId)) as TaskPayload;
      setTask(payload);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao carregar task');
    }
  }, [taskId]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <CodexShell title={`Task ${taskId.slice(0, 8)} • Pull Request`} subtitle="Create/update PR e comandos úteis de branch.">
      <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
        <div className="mb-3 flex items-center gap-2">
          <Link className="rounded-full border border-[var(--border)] px-3 py-1.5 text-xs" href={`/codex/tasks/${taskId}`}>
            Summary
          </Link>
          <Link className="rounded-full border border-[var(--border)] px-3 py-1.5 text-xs" href={`/codex/tasks/${taskId}/diff`}>
            Diff
          </Link>
          <Link className="rounded-full border border-[var(--border)] px-3 py-1.5 text-xs" href={`/codex/tasks/${taskId}/logs`}>
            Logs
          </Link>
        </div>
        {error && <p className="mb-3 text-sm text-[var(--destructive)]">{error}</p>}
        {task && (
          <div className="grid gap-3 lg:grid-cols-[1fr_0.9fr]">
            <PullRequestPanel
              taskId={task.id}
              branch={task.resultBranch}
              existingPr={task.pullRequest}
              onRefresh={load}
            />
            <article className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] p-4">
              <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Git Commands</h2>
              <pre className="mt-3 overflow-auto rounded-[var(--radius-lg)] border border-[var(--border)] bg-black/40 p-3 text-xs text-[var(--muted-foreground)]">
{`git fetch origin ${task.resultBranch || `codex/${task.id}`}\ngit checkout ${task.resultBranch || `codex/${task.id}`}\ngit log --oneline -n 20`}
              </pre>
            </article>
          </div>
        )}
      </section>
    </CodexShell>
  );
}
