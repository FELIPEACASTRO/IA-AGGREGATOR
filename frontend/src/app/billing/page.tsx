'use client';

import { AppShell } from '@/components/app/app-shell';
import { Button } from '@/components/ui/button';

export default function BillingPage() {
  return (
    <AppShell title="Plano" subtitle="Acompanhe uso de tokens e capacidade da conta">
      <div className="grid gap-4 p-6 md:grid-cols-3">
        <article className="rounded-xl border border-[var(--border)] p-4">
          <p className="text-sm text-[var(--muted-foreground)]">Plano Atual</p>
          <p className="mt-2 text-2xl font-bold">Starter</p>
          <p className="mt-1 text-sm text-[var(--muted-foreground)]">Ideal para validação inicial</p>
        </article>
        <article className="rounded-xl border border-[var(--border)] p-4">
          <p className="text-sm text-[var(--muted-foreground)]">Uso Mensal</p>
          <p className="mt-2 text-2xl font-bold">12.4k</p>
          <p className="mt-1 text-sm text-[var(--muted-foreground)]">tokens consumidos</p>
        </article>
        <article className="rounded-xl border border-[var(--border)] p-4">
          <p className="text-sm text-[var(--muted-foreground)]">Crédito Restante</p>
          <p className="mt-2 text-2xl font-bold">87%</p>
          <p className="mt-1 text-sm text-[var(--muted-foreground)]">até o reset mensal</p>
        </article>
      </div>
      <div className="px-6 pb-6">
        <Button>Solicitar upgrade</Button>
      </div>
    </AppShell>
  );
}
