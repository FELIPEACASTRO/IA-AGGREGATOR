'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { CodexShell } from '@/components/codex/codex-shell';

const shortcuts = [
  { key: 'g c', action: 'Abrir dashboard /codex' },
  { key: 'g e', action: 'Abrir environments' },
  { key: 'g u', action: 'Abrir usage' },
  { key: 'g a', action: 'Abrir archived' },
  { key: 'c', action: 'Focar composer' },
  { key: 'Cmd/Ctrl + Enter', action: 'Submit composer' },
  { key: 'm', action: 'Alternar Ask/Code' },
  { key: 'l', action: 'Abrir logs da task ativa' },
  { key: 'd', action: 'Abrir diff da task ativa' },
  { key: 'r', action: 'Retry task selecionada' },
  { key: 'x', action: 'Cancel task selecionada' },
];

export default function ShortcutsPage() {
  const router = useRouter();

  useEffect(() => {
    const handler = (event: KeyboardEvent) => {
      if (event.key.toLowerCase() === 'g' && event.shiftKey) {
        router.push('/codex');
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [router]);

  return (
    <CodexShell title="Keyboard Shortcuts" subtitle="Fluxo keyboard-first para navegação e execução de tasks cloud.">
      <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
        <ul className="space-y-2">
          {shortcuts.map((item) => (
            <li key={item.key} className="grid grid-cols-[180px_1fr] items-center gap-3 rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] px-3 py-2 text-sm">
              <kbd className="inline-flex w-fit rounded-full border border-[var(--border)] px-3 py-1 text-xs">{item.key}</kbd>
              <span className="text-[var(--muted-foreground)]">{item.action}</span>
            </li>
          ))}
        </ul>
      </section>
    </CodexShell>
  );
}

