'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { CodexShell } from '@/components/codex/codex-shell';
import { codexApi } from '@/lib/codex-api';

type TestPayload = {
  summary: {
    lint: string;
    typecheck: string;
    tests: string;
  };
  logs: Array<{
    id: string;
    line: string;
    lineNumber: number;
    isError: boolean;
  }>;
};

export default function TaskTestsPage() {
  const params = useParams<{ taskId: string }>();
  const taskId = params.taskId;
  const [tests, setTests] = useState<TestPayload | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    codexApi
      .getTaskTests(taskId)
      .then((payload) => setTests(payload as TestPayload))
      .catch((err) => setError(err instanceof Error ? err.message : 'Falha ao carregar testes'));
  }, [taskId]);

  return (
    <CodexShell title={`Task ${taskId.slice(0, 8)} • Tests`} subtitle="Resultados de lint, typecheck e testes.">
      <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
        <div className="mb-3 flex items-center gap-2">
          <Link className="rounded-full border border-[var(--border)] px-3 py-1.5 text-xs" href={`/codex/tasks/${taskId}`}>
            Summary
          </Link>
          <Link className="rounded-full border border-[var(--border)] px-3 py-1.5 text-xs" href={`/codex/tasks/${taskId}/logs`}>
            Logs
          </Link>
          <Link className="rounded-full border border-[var(--border)] px-3 py-1.5 text-xs" href={`/codex/tasks/${taskId}/diff`}>
            Diff
          </Link>
        </div>
        {error && <p className="mb-3 text-sm text-[var(--destructive)]">{error}</p>}
        {!tests && !error && <p className="text-sm text-[var(--muted-foreground)]">Carregando resultados...</p>}
        {tests && (
          <div className="grid gap-3 lg:grid-cols-[300px_1fr]">
            <article className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] p-3 text-sm">
              <p className="font-semibold">Lint: {tests.summary.lint}</p>
              <p className="mt-1 font-semibold">Typecheck: {tests.summary.typecheck}</p>
              <p className="mt-1 font-semibold">Tests: {tests.summary.tests}</p>
            </article>
            <pre className="max-h-[55vh] overflow-auto rounded-[var(--radius-xl)] border border-[var(--border)] bg-black/40 p-3 text-xs text-[var(--muted-foreground)]">
              {tests.logs.map((line) => `${line.lineNumber.toString().padStart(4, '0')} ${line.line}`).join('\n')}
            </pre>
          </div>
        )}
      </section>
    </CodexShell>
  );
}
