'use client';

import Link from 'next/link';
import { useMemo, useState } from 'react';
import { CodexShell } from '@/components/codex/codex-shell';

const flow = [
  { key: 'welcome', title: 'Boas-vindas', description: 'Entenda como tasks cloud, diff e PR funcionam juntos.' },
  { key: 'github', title: 'Conectar GitHub', description: 'Instale a integração e autorize repositórios.' },
  { key: 'workspace', title: 'Selecionar Workspace', description: 'Defina contexto e permissões básicas.' },
  { key: 'environment', title: 'Criar Environment', description: 'Configure setup, maintenance, runtime pins e internet policy.' },
  { key: 'validate', title: 'Validar Setup', description: 'Execute validação para garantir reproducibilidade.' },
  { key: 'task', title: 'Primeira Task', description: 'Rode uma task Ask e depois uma task Code.' },
  { key: 'review', title: 'Revisão e PR', description: 'Inspecione logs/diff/tests e publique PR.' },
];

export default function CodexOnboardingPage() {
  const [index, setIndex] = useState(0);
  const current = flow[index];
  const progress = useMemo(() => Math.round(((index + 1) / flow.length) * 100), [index]);

  return (
    <CodexShell title="Onboarding Wizard" subtitle="Do zero até primeira entrega com evidência revisável.">
      <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-6">
        <p className="text-xs uppercase tracking-[0.16em] text-[var(--subtle-foreground)]">
          Step {index + 1} / {flow.length}
        </p>
        <h2 className="mt-2 text-xl font-semibold tracking-[-0.03em]">{current.title}</h2>
        <p className="mt-2 text-sm text-[var(--muted-foreground)]">{current.description}</p>
        <div className="mt-4 h-2 w-full rounded-full bg-[rgba(255,255,255,0.08)]">
          <div className="h-2 rounded-full bg-[var(--brand-gradient)]" style={{ width: `${progress}%` }} />
        </div>
        <div className="mt-5 grid gap-2 text-sm">
          <Link href="/codex/settings/connectors" className="rounded-[var(--radius-lg)] border border-[var(--border)] px-3 py-2">
            Conectar integrações
          </Link>
          <Link href="/codex/settings/environments/new" className="rounded-[var(--radius-lg)] border border-[var(--border)] px-3 py-2">
            Criar environment
          </Link>
          <Link href="/codex" className="rounded-[var(--radius-lg)] border border-[var(--border)] px-3 py-2">
            Rodar task
          </Link>
        </div>
        <div className="mt-6 flex gap-2">
          <button
            className="rounded-full border border-[var(--border)] px-4 py-2 text-sm disabled:opacity-50"
            disabled={index === 0}
            onClick={() => setIndex((value) => Math.max(0, value - 1))}
          >
            Voltar
          </button>
          <button
            className="rounded-full border border-[var(--border)] px-4 py-2 text-sm"
            onClick={() => setIndex((value) => Math.min(flow.length - 1, value + 1))}
          >
            Avançar
          </button>
        </div>
      </section>
    </CodexShell>
  );
}

