'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { CodexShell } from '@/components/codex/codex-shell';
import { codexApi } from '@/lib/codex-api';

type UsagePayload = {
  entries: Array<{ id: string; metric: string; amount: number; unit: string; period: string; createdAt: string }>;
  taskUsage: Array<{ taskId: string | null; _sum: { amount: number | null } }>;
  repositoryUsage: Array<{ repositoryId: string | null; _sum: { amount: number | null } }>;
  creditBalance?: { balance: number; includedUsageLeft: number } | null;
};

export default function UsageSettingsPage() {
  const [usage, setUsage] = useState<UsagePayload | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    codexApi
      .getUsage()
      .then((payload) => setUsage(payload as UsagePayload))
      .catch((err) => setError(err instanceof Error ? err.message : 'Falha ao carregar usage'));
  }, []);

  return (
    <CodexShell title="Usage Dashboard" subtitle="Consumo por período, task e repositório com saldo de créditos.">
      <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Usage Overview</h2>
          <Link href="/codex/settings/usage/credits" className="rounded-full border border-[var(--border)] px-3 py-1.5 text-xs">
            Add credits
          </Link>
        </div>
        {error && <p className="mt-3 text-sm text-[var(--destructive)]">{error}</p>}
        {!usage && !error && <p className="mt-3 text-sm text-[var(--muted-foreground)]">Carregando dashboard...</p>}
        {usage && (
          <div className="mt-3 grid gap-3 lg:grid-cols-3">
            <article className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] p-3">
              <p className="text-xs text-[var(--subtle-foreground)]">Included usage</p>
              <p className="text-xl font-semibold">{usage.creditBalance?.includedUsageLeft ?? 0}</p>
            </article>
            <article className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] p-3">
              <p className="text-xs text-[var(--subtle-foreground)]">Credits balance</p>
              <p className="text-xl font-semibold">{usage.creditBalance?.balance ?? 0}</p>
            </article>
            <article className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] p-3">
              <p className="text-xs text-[var(--subtle-foreground)]">Recent entries</p>
              <p className="text-xl font-semibold">{usage.entries.length}</p>
            </article>
          </div>
        )}
      </section>
    </CodexShell>
  );
}

