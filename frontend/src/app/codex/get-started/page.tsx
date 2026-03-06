'use client';

import Link from 'next/link';
import { CodexShell } from '@/components/codex/codex-shell';

const steps = [
  'Conecte GitHub e confirme repositorios autorizados.',
  'Configure environment com setup, maintenance e policy de internet.',
  'Valide o environment e inicie a primeira task em modo Ask.',
  'Execute task em modo Code com evidencias, diff e testes.',
  'Crie ou atualize PR e acompanhe fluxo de review.',
];

export default function CodexGetStartedPage() {
  return (
    <CodexShell title="Get Started" subtitle="Fluxo de onboarding completo do workspace cloud.">
      <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-6">
        <h2 className="text-lg font-semibold tracking-[-0.03em]">Onboarding Checklist</h2>
        <ol className="mt-4 space-y-3">
          {steps.map((step, idx) => (
            <li key={step} className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] px-4 py-3 text-sm">
              <span className="mr-2 inline-flex h-6 w-6 items-center justify-center rounded-full border border-[var(--border)] text-xs">
                {idx + 1}
              </span>
              {step}
            </li>
          ))}
        </ol>
        <div className="mt-5 flex flex-wrap gap-2">
          <Link href="/codex/onboarding" className="rounded-full border border-[var(--border)] px-4 py-2 text-sm">
            Abrir Wizard
          </Link>
          <Link href="/codex/settings/connectors" className="rounded-full border border-[var(--border)] px-4 py-2 text-sm">
            Connectors
          </Link>
          <Link href="/codex/settings/environments/new" className="rounded-full border border-[var(--border)] px-4 py-2 text-sm">
            Novo Environment
          </Link>
          <Link href="/codex" className="rounded-full border border-[var(--border)] px-4 py-2 text-sm">
            Ir para Tasks
          </Link>
        </div>
      </section>
    </CodexShell>
  );
}

