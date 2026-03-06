'use client';

import { useCallback, useEffect, useState } from 'react';
import { CodexShell } from '@/components/codex/codex-shell';
import { codexApi } from '@/lib/codex-api';
import { Button } from '@/components/ui/button';

type ReviewPolicy = {
  id: string;
  repositoryId: string;
  enabled: boolean;
  automaticReviews: boolean;
  minSeverity: 'P0' | 'P1' | 'P2' | 'P3';
  agentsMdPrecedence: boolean;
  oneOffFocusEnabled: boolean;
  repository?: { id: string; fullName: string };
};

type Repo = { id: string; fullName: string };

export default function CodeReviewSettingsPage() {
  const [policies, setPolicies] = useState<ReviewPolicy[]>([]);
  const [repos, setRepos] = useState<Repo[]>([]);
  const [repoId, setRepoId] = useState('');
  const [minSeverity, setMinSeverity] = useState<'P0' | 'P1' | 'P2' | 'P3'>('P2');
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const [policyPayload, repoPayload] = await Promise.all([
        codexApi.getReviewPolicies(),
        codexApi.listRepositories(),
      ]);
      setPolicies(policyPayload as ReviewPolicy[]);
      const castedRepos = repoPayload as Repo[];
      setRepos(castedRepos);
      if (!repoId && castedRepos[0]) setRepoId(castedRepos[0].id);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao carregar políticas');
    }
  }, [repoId]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <CodexShell title="Code Review Policies" subtitle="Configuração de review automático/manual por repositório com prioridade e guidelines.">
      <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
        <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Nova política</h2>
        <div className="mt-3 grid gap-3 md:grid-cols-3">
          <label className="space-y-1">
            <span className="text-xs uppercase tracking-[0.12em] text-[var(--subtle-foreground)]">Repository</span>
            <select
              value={repoId}
              onChange={(event) => setRepoId(event.target.value)}
              className="h-11 w-full rounded-[var(--radius-lg)] border border-[var(--border)] bg-transparent px-3 text-sm"
            >
              {repos.map((repo) => (
                <option key={repo.id} value={repo.id}>
                  {repo.fullName}
                </option>
              ))}
            </select>
          </label>
          <label className="space-y-1">
            <span className="text-xs uppercase tracking-[0.12em] text-[var(--subtle-foreground)]">Min severity</span>
            <select
              value={minSeverity}
              onChange={(event) => setMinSeverity(event.target.value as typeof minSeverity)}
              className="h-11 w-full rounded-[var(--radius-lg)] border border-[var(--border)] bg-transparent px-3 text-sm"
            >
              <option value="P0">P0</option>
              <option value="P1">P1</option>
              <option value="P2">P2</option>
              <option value="P3">P3</option>
            </select>
          </label>
          <div className="flex items-end">
            <Button
              onClick={async () => {
                await codexApi.upsertReviewPolicy({
                  repositoryId: repoId,
                  enabled: true,
                  automaticReviews: true,
                  minSeverity,
                  agentsMdPrecedence: true,
                  oneOffFocusEnabled: true,
                });
                await load();
              }}
            >
              Salvar política
            </Button>
          </div>
        </div>
      </section>

      <section className="mt-3 rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
        <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Policies ativas</h2>
        <div className="mt-3 space-y-2">
          {policies.map((policy) => (
            <article key={policy.id} className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] p-3">
              <p className="text-sm font-semibold">{policy.repository?.fullName || policy.repositoryId}</p>
              <p className="mt-1 text-xs text-[var(--muted-foreground)]">
                Enabled: {String(policy.enabled)} • Auto: {String(policy.automaticReviews)} • Min severity: {policy.minSeverity}
              </p>
            </article>
          ))}
          {policies.length === 0 && <p className="text-sm text-[var(--muted-foreground)]">Sem políticas configuradas.</p>}
        </div>
      </section>
      {error && <p className="mt-3 text-sm text-[var(--destructive)]">{error}</p>}
    </CodexShell>
  );
}
