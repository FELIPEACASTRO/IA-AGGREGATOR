'use client';

import { useMemo, useState } from 'react';

type DiffHunk = {
  id: string;
  header: string;
  content: string;
};

type DiffFile = {
  id: string;
  path: string;
  changeType: string;
  additions: number;
  deletions: number;
  hunks: DiffHunk[];
};

type DiffSnapshot = {
  id: string;
  summary?: string | null;
  patch?: string | null;
  files: DiffFile[];
};

export function DiffViewer({ snapshot }: { snapshot: DiffSnapshot | null }) {
  const [selectedFileId, setSelectedFileId] = useState<string>('');

  const files = useMemo(() => snapshot?.files ?? [], [snapshot]);
  const selected = useMemo(() => {
    if (!files.length) return null;
    return files.find((item) => item.id === selectedFileId) ?? files[0];
  }, [files, selectedFileId]);

  return (
    <section className="grid gap-3 lg:grid-cols-[280px_1fr]">
      <aside className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-3">
        <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Changed Files</h2>
        <p className="mt-1 text-xs text-[var(--muted-foreground)]">{snapshot?.summary || 'Sem alteracoes no snapshot'}</p>
        <div className="mt-3 space-y-2">
          {files.length === 0 && <p className="text-sm text-[var(--muted-foreground)]">Nenhum arquivo alterado.</p>}
          {files.map((file) => (
            <button
              type="button"
              key={file.id}
              onClick={() => setSelectedFileId(file.id)}
              className={`w-full rounded-[var(--radius-lg)] border px-3 py-2 text-left text-xs ${selected?.id === file.id ? 'border-[rgba(96,115,255,0.4)] bg-[rgba(96,115,255,0.16)]' : 'border-[var(--border)]'}`}
            >
              <p className="truncate font-semibold text-[var(--foreground)]">{file.path}</p>
              <p className="text-[var(--muted-foreground)]">
                +{file.additions} / -{file.deletions} ({file.changeType})
              </p>
            </button>
          ))}
        </div>
      </aside>
      <div className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-black/40 p-3">
        <h3 className="mb-2 text-sm font-semibold text-[var(--foreground)]">{selected?.path || 'Diff Preview'}</h3>
        {!selected && <p className="text-sm text-[var(--muted-foreground)]">Selecione um arquivo para inspecionar os hunks.</p>}
        {selected && (
          <div className="max-h-[60vh] overflow-auto space-y-3">
            {selected.hunks.length === 0 && (
              <pre className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(0,0,0,0.4)] p-3 text-xs text-[var(--muted-foreground)]">
                {snapshot?.patch || 'Sem patch disponivel'}
              </pre>
            )}
            {selected.hunks.map((hunk) => (
              <pre key={hunk.id} className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(0,0,0,0.4)] p-3 text-xs leading-6 text-[var(--muted-foreground)] whitespace-pre-wrap">
                {hunk.content}
              </pre>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
