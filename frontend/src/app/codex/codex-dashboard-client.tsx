'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { TaskMode } from '@prisma/client';
import { CodexShell } from '@/components/codex/codex-shell';
import { TaskComposer } from '@/components/codex/task-composer';
import { TaskList } from '@/components/codex/task-list';
import { CodexTask, codexApi } from '@/lib/codex-api';

type CodexDashboardClientProps = {
  tab: string;
  prompt?: string;
  environment?: string;
  branch?: string;
  mode?: TaskMode;
};

export function CodexDashboardClient({
  tab,
  prompt,
  environment,
  branch,
  mode,
}: CodexDashboardClientProps) {
  const [tasks, setTasks] = useState<CodexTask[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const query = useMemo(() => {
    const query = new URLSearchParams();
    if (tab === 'archived') query.set('tab', 'archived');
    if (tab === 'failed') query.set('status', 'failed');
    if (tab === 'completed') query.set('status', 'completed');
    if (tab === 'queued') query.set('status', 'queued');
    return query.toString();
  }, [tab]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await codexApi.listTasks(query);
      setTasks(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao carregar tasks');
    } finally {
      setLoading(false);
    }
  }, [query]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <CodexShell
      title="Cloud Tasks"
      subtitle="Paralelismo, background execution, diffs revisaveis e follow-up contextual."
    >
      <TaskComposer
        onCreated={load}
        defaultPrompt={prompt}
        defaultEnvironmentId={environment}
        defaultBranch={branch}
        defaultMode={mode}
      />
      <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">
            Tasks - {tab}
          </h2>
        </div>
        {loading && <p className="text-sm text-[var(--muted-foreground)]">Carregando tasks...</p>}
        {error && <p className="text-sm text-[var(--destructive)]">{error}</p>}
        {!loading && !error && <TaskList tasks={tasks} onRefresh={load} />}
      </section>
    </CodexShell>
  );
}

