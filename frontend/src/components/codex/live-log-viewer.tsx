'use client';

import { useMemo, useState } from 'react';

type TaskLog = {
  id: string;
  phase: string;
  line: string;
  lineNumber: number;
  isError: boolean;
  createdAt: string;
};

type LiveLogViewerProps = {
  logs: TaskLog[];
};

const phases = ['all', 'provisioning', 'repo_download', 'setup', 'maintenance', 'agent', 'validation', 'pr_push'];

export function LiveLogViewer({ logs }: LiveLogViewerProps) {
  const [phaseFilter, setPhaseFilter] = useState('all');
  const [search, setSearch] = useState('');

  const filtered = useMemo(() => {
    return logs.filter((log) => {
      const byPhase = phaseFilter === 'all' || log.phase === phaseFilter;
      const bySearch = search.trim().length === 0 || log.line.toLowerCase().includes(search.toLowerCase());
      return byPhase && bySearch;
    });
  }, [logs, phaseFilter, search]);

  return (
    <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-black/40 p-4">
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <h2 className="mr-auto text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Live Logs</h2>
        <input
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Buscar texto..."
          className="h-9 rounded-[var(--radius-pill)] border border-[var(--border)] bg-transparent px-3 text-xs"
        />
        <select
          value={phaseFilter}
          onChange={(event) => setPhaseFilter(event.target.value)}
          className="h-9 rounded-[var(--radius-pill)] border border-[var(--border)] bg-transparent px-3 text-xs"
        >
          {phases.map((phase) => (
            <option key={phase} value={phase}>
              {phase}
            </option>
          ))}
        </select>
      </div>
      <div className="max-h-[54vh] overflow-auto rounded-[var(--radius-lg)] border border-[var(--border)] bg-black/40 p-3 font-mono text-xs">
        {filtered.length === 0 && <p className="text-[var(--muted-foreground)]">Sem linhas de log para o filtro atual.</p>}
        {filtered.map((line) => (
          <div key={line.id} className={`grid grid-cols-3 gap-2 py-0.5 ${line.isError ? 'text-[var(--destructive)]' : 'text-[var(--muted-foreground)]'}`}>
            <span>{line.lineNumber.toString().padStart(4, '0')}</span>
            <span>{line.phase}</span>
            <span className="whitespace-pre-wrap break-words">{line.line}</span>
          </div>
        ))}
      </div>
    </section>
  );
}
