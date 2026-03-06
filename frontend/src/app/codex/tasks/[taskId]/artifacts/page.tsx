'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { CodexShell } from '@/components/codex/codex-shell';
import { codexApi } from '@/lib/codex-api';

type Artifact = {
  id: string;
  artifactType: string;
  title: string;
  contentType: string;
  url: string;
  createdAt: string;
};

export default function TaskArtifactsPage() {
  const params = useParams<{ taskId: string }>();
  const taskId = params.taskId;
  const [artifacts, setArtifacts] = useState<Artifact[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    codexApi
      .getTaskArtifacts(taskId)
      .then((payload) => setArtifacts(payload as Artifact[]))
      .catch((err) => setError(err instanceof Error ? err.message : 'Falha ao carregar artifacts'));
  }, [taskId]);

  return (
    <CodexShell title={`Task ${taskId.slice(0, 8)} • Artifacts`} subtitle="Outputs visuais e anexos de execução.">
      <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
        <div className="mb-3 flex items-center gap-2">
          <Link className="rounded-full border border-[var(--border)] px-3 py-1.5 text-xs" href={`/codex/tasks/${taskId}`}>
            Summary
          </Link>
          <Link className="rounded-full border border-[var(--border)] px-3 py-1.5 text-xs" href={`/codex/tasks/${taskId}/diff`}>
            Diff
          </Link>
          <Link className="rounded-full border border-[var(--border)] px-3 py-1.5 text-xs" href={`/codex/tasks/${taskId}/pull-request`}>
            Pull Request
          </Link>
        </div>
        {error && <p className="mb-3 text-sm text-[var(--destructive)]">{error}</p>}
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {artifacts.map((artifact) => (
            <article
              key={artifact.id}
              className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] p-3"
            >
              <p className="text-xs uppercase tracking-[0.12em] text-[var(--subtle-foreground)]">{artifact.artifactType}</p>
              <h3 className="mt-1 text-sm font-semibold">{artifact.title}</h3>
              <p className="mt-1 text-xs text-[var(--muted-foreground)]">{artifact.contentType}</p>
              <a
                className="mt-2 inline-flex text-xs text-[var(--brand-primary)] underline underline-offset-4"
                href={`/api/tasks/${taskId}/artifacts?artifactId=${artifact.id}&download=1`}
                target="_blank"
                rel="noreferrer"
              >
                Download
              </a>
            </article>
          ))}
          {artifacts.length === 0 && (
            <p className="text-sm text-[var(--muted-foreground)]">Nenhum artifact encontrado para esta task.</p>
          )}
        </div>
      </section>
    </CodexShell>
  );
}

