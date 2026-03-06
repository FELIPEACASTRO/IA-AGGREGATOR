'use client';

import { useEffect, useState } from 'react';
import { CodexShell } from '@/components/codex/codex-shell';
import { codexApi } from '@/lib/codex-api';

type AnalyticsPayload = {
  taskCounts: Array<{ status: string; _count: { status: number } }>;
  reviewFindings: Array<{ severity: string; _count: { severity: number } }>;
  usage: Array<{ metric: string; _sum: { amount: number | null } }>;
};

export default function AnalyticsPage() {
  const [analytics, setAnalytics] = useState<AnalyticsPayload | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    codexApi
      .getAnalytics()
      .then((payload) => setAnalytics(payload as AnalyticsPayload))
      .catch((err) => setError(err instanceof Error ? err.message : 'Falha ao carregar analytics'));
  }, []);

  return (
    <CodexShell title="Analytics" subtitle="Daily users, cloud tasks, review activity e findings por severidade.">
      <section className="grid gap-3 lg:grid-cols-3">
        <article className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
          <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Task status</h2>
          <ul className="mt-3 space-y-2 text-sm text-[var(--muted-foreground)]">
            {analytics?.taskCounts.map((item) => (
              <li key={item.status}>
                {item.status}: {item._count.status}
              </li>
            ))}
            {!analytics?.taskCounts?.length && <li>Sem dados.</li>}
          </ul>
        </article>

        <article className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
          <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Findings severity</h2>
          <ul className="mt-3 space-y-2 text-sm text-[var(--muted-foreground)]">
            {analytics?.reviewFindings.map((item) => (
              <li key={item.severity}>
                {item.severity}: {item._count.severity}
              </li>
            ))}
            {!analytics?.reviewFindings?.length && <li>Sem dados.</li>}
          </ul>
        </article>

        <article className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
          <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Usage metrics</h2>
          <ul className="mt-3 space-y-2 text-sm text-[var(--muted-foreground)]">
            {analytics?.usage.map((item) => (
              <li key={item.metric}>
                {item.metric}: {item._sum.amount ?? 0}
              </li>
            ))}
            {!analytics?.usage?.length && <li>Sem dados.</li>}
          </ul>
        </article>
      </section>
      {error && <p className="mt-3 text-sm text-[var(--destructive)]">{error}</p>}
    </CodexShell>
  );
}

