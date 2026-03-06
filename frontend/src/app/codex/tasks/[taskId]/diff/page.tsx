'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { CodexShell } from '@/components/codex/codex-shell';
import { DiffViewer } from '@/components/codex/diff-viewer';
import { codexApi } from '@/lib/codex-api';

type DiffSnapshot = {
  id: string;
  summary?: string | null;
  patch?: string | null;
  files: Array<{
    id: string;
    path: string;
    changeType: string;
    additions: number;
    deletions: number;
    hunks: Array<{ id: string; header: string; content: string }>;
  }>;
};

export default function TaskDiffPage() {
  const params = useParams<{ taskId: string }>();
  const taskId = params.taskId;
  const [snapshot, setSnapshot] = useState<DiffSnapshot | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    codexApi
      .getTaskDiff(taskId)
      .then((payload) => setSnapshot((payload as DiffSnapshot | null) ?? null))
      .catch((err) => setError(err instanceof Error ? err.message : 'Falha ao carregar diff'));
  }, [taskId]);

  return (
    <CodexShell title={`Task ${taskId.slice(0, 8)} • Diff`} subtitle="Revisão unificada por arquivo e hunk.">
      <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
        <div className="mb-3 flex items-center gap-2">
          <Link className="rounded-full border border-[var(--border)] px-3 py-1.5 text-xs" href={`/codex/tasks/${taskId}`}>
            Summary
          </Link>
          <Link className="rounded-full border border-[var(--border)] px-3 py-1.5 text-xs" href={`/codex/tasks/${taskId}/logs`}>
            Logs
          </Link>
          <Link className="rounded-full border border-[var(--border)] px-3 py-1.5 text-xs" href={`/codex/tasks/${taskId}/pull-request`}>
            Pull Request
          </Link>
        </div>
        {error && <p className="mb-3 text-sm text-[var(--destructive)]">{error}</p>}
        <DiffViewer snapshot={snapshot} />
      </section>
    </CodexShell>
  );
}

