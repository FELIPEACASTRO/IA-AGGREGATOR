'use client';

import Link from 'next/link';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams } from 'next/navigation';
import { TaskMode } from '@prisma/client';
import { CodexShell } from '@/components/codex/codex-shell';
import { TaskTimeline } from '@/components/codex/task-timeline';
import { codexApi } from '@/lib/codex-api';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';

type TaskDetails = {
  id: string;
  title: string;
  prompt: string;
  mode: TaskMode;
  status: string;
  baseBranch?: string | null;
  resultBranch?: string | null;
  repository?: { fullName: string } | null;
  environment?: { name: string; internetMode: string } | null;
  summaryText?: string;
  events: Array<{
    id: string;
    eventType: string;
    status?: string | null;
    message?: string | null;
    createdAt: string;
  }>;
  pullRequest?: {
    id: string;
    status: string;
    url?: string | null;
  } | null;
};

export default function CodexTaskDetailsPage() {
  const params = useParams<{ taskId: string }>();
  const taskId = params.taskId;
  const [task, setTask] = useState<TaskDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [followupPrompt, setFollowupPrompt] = useState('');
  const [followupMode, setFollowupMode] = useState<TaskMode>('ASK');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const payload = (await codexApi.getTask(taskId)) as TaskDetails;
      setTask(payload);
      setFollowupMode(payload.mode);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao carregar task');
    } finally {
      setLoading(false);
    }
  }, [taskId]);

  useEffect(() => {
    void load();
  }, [load]);

  const tabs = useMemo(
    () => [
      { href: `/codex/tasks/${taskId}`, label: 'Summary' },
      { href: `/codex/tasks/${taskId}/logs`, label: 'Logs' },
      { href: `/codex/tasks/${taskId}/diff`, label: 'Diff' },
      { href: `/codex/tasks/${taskId}/tests`, label: 'Tests' },
      { href: `/codex/tasks/${taskId}/artifacts`, label: 'Artifacts' },
      { href: `/codex/tasks/${taskId}/pull-request`, label: 'Pull Request' },
    ],
    [taskId]
  );

  return (
    <CodexShell title={task?.title || `Task ${taskId.slice(0, 8)}`} subtitle="Resumo, evidencias e follow-up contextual.">
      <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
        <div className="flex flex-wrap items-center gap-2">
          {tabs.map((tab) => (
            <Link
              key={tab.href}
              href={tab.href}
              className="rounded-full border border-[var(--border)] px-3 py-1.5 text-xs text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
            >
              {tab.label}
            </Link>
          ))}
        </div>
        {loading && <p className="mt-3 text-sm text-[var(--muted-foreground)]">Carregando...</p>}
        {error && <p className="mt-3 text-sm text-[var(--destructive)]">{error}</p>}
        {task && (
          <div className="mt-4 grid gap-3 lg:grid-cols-[1.1fr_0.9fr]">
            <div className="space-y-3">
              <article className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] p-4">
                <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Summary</h2>
                <p className="mt-2 text-sm text-[var(--muted-foreground)]">{task.summaryText || 'Resumo ainda nao disponivel.'}</p>
                <div className="mt-3 grid gap-2 text-xs text-[var(--muted-foreground)] sm:grid-cols-2">
                  <p>Mode: {task.mode}</p>
                  <p>Status: {task.status}</p>
                  <p>Repository: {task.repository?.fullName || 'N/A'}</p>
                  <p>Environment: {task.environment?.name || 'N/A'}</p>
                  <p>Base branch: {task.baseBranch || 'main'}</p>
                  <p>Result branch: {task.resultBranch || `codex/${task.id}`}</p>
                  <p>Internet policy: {task.environment?.internetMode || 'OFF'}</p>
                </div>
              </article>

              <article className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] p-4">
                <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Follow-up</h2>
                <Textarea
                  className="mt-2"
                  rows={4}
                  value={followupPrompt}
                  onChange={(event) => setFollowupPrompt(event.target.value)}
                  placeholder="Continue a task com contexto da execucao mais recente..."
                />
                <div className="mt-2 flex flex-wrap items-center gap-2">
                  <select
                    value={followupMode}
                    onChange={(event) => setFollowupMode(event.target.value as TaskMode)}
                    className="h-10 rounded-full border border-[var(--border)] bg-transparent px-3 text-xs"
                  >
                    <option value="ASK">ASK</option>
                    <option value="CODE">CODE</option>
                  </select>
                  <Button
                    onClick={async () => {
                      if (!followupPrompt.trim()) return;
                      await codexApi.postFollowup(task.id, {
                        prompt: followupPrompt.trim(),
                        mode: followupMode,
                      });
                      setFollowupPrompt('');
                      await load();
                    }}
                  >
                    Enviar follow-up
                  </Button>
                  <Button
                    variant="outline"
                    onClick={async () => {
                      await codexApi.retryTask(task.id);
                      await load();
                    }}
                  >
                    Retry
                  </Button>
                  <Button
                    variant="outline"
                    onClick={async () => {
                      await codexApi.cancelTask(task.id);
                      await load();
                    }}
                  >
                    Cancel
                  </Button>
                </div>
              </article>
            </div>
            <TaskTimeline events={task.events || []} />
          </div>
        )}
      </section>
    </CodexShell>
  );
}

