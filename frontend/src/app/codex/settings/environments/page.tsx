'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { CodexShell } from '@/components/codex/codex-shell';
import { codexApi } from '@/lib/codex-api';
import { Button } from '@/components/ui/button';

type EnvironmentPayload = Array<{
  id: string;
  name: string;
  description?: string;
  defaultBranch: string;
  baseImage: string;
  internetMode: string;
  automaticSetup: boolean;
  caches: Array<{ status: string; updatedAt: string }>;
  repoMap: Array<{ repository: { fullName: string } }>;
}>;

export default function EnvironmentsListPage() {
  const [envs, setEnvs] = useState<EnvironmentPayload>([]);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    try {
      const payload = (await codexApi.listEnvironments()) as EnvironmentPayload;
      setEnvs(payload);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao carregar environments');
    }
  }

  useEffect(() => {
    void load();
  }, []);

  return (
    <CodexShell title="Environments" subtitle="Setup/maintenance, runtime pins, cache e policy de internet por repositório.">
      <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Environment Registry</h2>
          <Link href="/codex/settings/environments/new">
            <Button>Novo environment</Button>
          </Link>
        </div>

        <div className="space-y-3">
          {envs.map((env) => (
            <article key={env.id} className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] p-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <h3 className="text-sm font-semibold">{env.name}</h3>
                  <p className="text-xs text-[var(--muted-foreground)]">{env.description || 'Sem descrição'}</p>
                </div>
                <div className="flex gap-2">
                  <Link href={`/codex/settings/environments/${env.id}`}>
                    <Button variant="outline" size="sm">
                      Editar
                    </Button>
                  </Link>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={async () => {
                      await codexApi.resetEnvironmentCache(env.id);
                      await load();
                    }}
                  >
                    Reset cache
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={async () => {
                      await codexApi.validateEnvironment(env.id);
                      await load();
                    }}
                  >
                    Validar
                  </Button>
                </div>
              </div>
              <p className="mt-2 text-xs text-[var(--muted-foreground)]">
                Repo: {env.repoMap[0]?.repository.fullName || 'N/A'} • Branch: {env.defaultBranch} • Base image: {env.baseImage} • Internet: {env.internetMode}
              </p>
              <p className="mt-1 text-xs text-[var(--subtle-foreground)]">
                Cache: {env.caches[0]?.status || 'cold'} • Automatic setup: {env.automaticSetup ? 'on' : 'off'}
              </p>
            </article>
          ))}
          {envs.length === 0 && <p className="text-sm text-[var(--muted-foreground)]">Nenhum environment cadastrado.</p>}
        </div>
      </section>
      {error && <p className="mt-3 text-sm text-[var(--destructive)]">{error}</p>}
    </CodexShell>
  );
}

