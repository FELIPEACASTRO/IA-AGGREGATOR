'use client';

import { useEffect, useState } from 'react';
import { CodexShell } from '@/components/codex/codex-shell';
import { codexApi } from '@/lib/codex-api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

type CreditsPayload = {
  balance?: { balance: number; includedUsageLeft: number } | null;
  ledger: Array<{ id: string; type: string; amount: number; description: string; createdAt: string }>;
};

export default function CreditsPage() {
  const [credits, setCredits] = useState<CreditsPayload | null>(null);
  const [amount, setAmount] = useState('20');
  const [error, setError] = useState<string | null>(null);

  async function load() {
    try {
      const payload = (await codexApi.getCredits()) as CreditsPayload;
      setCredits(payload);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao carregar créditos');
    }
  }

  useEffect(() => {
    void load();
  }, []);

  return (
    <CodexShell title="Credits" subtitle="Saldo, ledger e compra de créditos extras.">
      <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
        <div className="grid gap-3 md:grid-cols-3">
          <article className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] p-3">
            <p className="text-xs text-[var(--subtle-foreground)]">Saldo atual</p>
            <p className="text-xl font-semibold">{credits?.balance?.balance ?? 0}</p>
            <p className="mt-1 text-xs text-[var(--muted-foreground)]">Included usage: {credits?.balance?.includedUsageLeft ?? 0}</p>
          </article>
          <Input value={amount} onChange={(event) => setAmount(event.target.value)} />
          <Button
            onClick={async () => {
              await codexApi.purchaseCredits({ amount: Number(amount) || 0, currency: 'USD' });
              await load();
            }}
          >
            Add credits
          </Button>
        </div>

        <div className="mt-4 rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] p-3">
          <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Ledger</h2>
          <ul className="mt-2 space-y-2 text-xs text-[var(--muted-foreground)]">
            {credits?.ledger.map((entry) => (
              <li key={entry.id}>
                {entry.type} • {entry.amount} • {entry.description} • {new Date(entry.createdAt).toLocaleString('pt-BR')}
              </li>
            ))}
            {!credits?.ledger?.length && <li>Nenhuma transação registrada.</li>}
          </ul>
        </div>
      </section>
      {error && <p className="mt-3 text-sm text-[var(--destructive)]">{error}</p>}
    </CodexShell>
  );
}
