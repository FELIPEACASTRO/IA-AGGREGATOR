'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { CodexShell } from '@/components/codex/codex-shell';
import { LiveLogViewer } from '@/components/codex/live-log-viewer';
import { codexApi } from '@/lib/codex-api';

export default function TaskLogsPage() {
  const params = useParams<{ taskId: string }>();
  const taskId = params.taskId;
  const [logs, setLogs] = useState<
    Array<{ id: string; phase: string; line: string; lineNumber: number; isError: boolean; createdAt: string }>
  >([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    codexApi
      .getTaskLogs(taskId)
      .then((payload) => setLogs(payload as typeof logs))
      .catch((err) => setError(err instanceof Error ? err.message : 'Falha ao carregar logs'));
  }, [taskId]);

  return (
    <CodexShell title={`Task ${taskId.slice(0, 8)} • Logs`} subtitle="Stream e inspeção por fase.">
      <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
        <div className="mb-3 flex items-center gap-2">
          <Link className="rounded-full border border-[var(--border)] px-3 py-1.5 text-xs" href={`/codex/tasks/${taskId}`}>
            Summary
          </Link>
          <Link className="rounded-full border border-[var(--border)] px-3 py-1.5 text-xs" href={`/codex/tasks/${taskId}/diff`}>
            Diff
          </Link>
          <Link className="rounded-full border border-[var(--border)] px-3 py-1.5 text-xs" href={`/codex/tasks/${taskId}/tests`}>
            Tests
          </Link>
        </div>
        {error && <p className="mb-3 text-sm text-[var(--destructive)]">{error}</p>}
        <LiveLogViewer logs={logs} />
      </section>
    </CodexShell>
  );
}

