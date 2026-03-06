'use client';

import { useCallback, useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { CodexShell } from '@/components/codex/codex-shell';
import { codexApi } from '@/lib/codex-api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';

type EnvironmentDetail = {
  id: string;
  name: string;
  description?: string | null;
  defaultBranch: string;
  baseImage: string;
  setupScript?: string | null;
  maintenanceScript?: string | null;
  internetMode: string;
  domainAllowlist: string[];
  allowedHttpMethods: string[];
  executions: Array<{ id: string; status: string; createdAt: string; errorMessage?: string | null }>;
  caches: Array<{ id: string; status: string; updatedAt: string; invalidationReason?: string | null }>;
};

export default function EnvironmentDetailsPage() {
  const params = useParams<{ environmentId: string }>();
  const environmentId = params.environmentId;
  const router = useRouter();
  const [environment, setEnvironment] = useState<EnvironmentDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    try {
      const payload = (await codexApi.getEnvironment(environmentId)) as EnvironmentDetail;
      setEnvironment(payload);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao carregar environment');
    }
  }, [environmentId]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <CodexShell title="Editar Environment" subtitle="Histórico, cache e validação de saúde do ambiente.">
      {!environment && !error && <p className="text-sm text-[var(--muted-foreground)]">Carregando environment...</p>}
      {error && <p className="text-sm text-[var(--destructive)]">{error}</p>}
      {environment && (
        <form
          className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4"
          onSubmit={async (event) => {
            event.preventDefault();
            setLoading(true);
            setError(null);
            try {
              await codexApi.updateEnvironment(environment.id, {
                name: environment.name,
                description: environment.description,
                defaultBranch: environment.defaultBranch,
                baseImage: environment.baseImage,
                setupScript: environment.setupScript,
                maintenanceScript: environment.maintenanceScript,
                internetMode: environment.internetMode,
                domainAllowlist: environment.domainAllowlist,
                allowedHttpMethods: environment.allowedHttpMethods,
              });
              await load();
            } catch (err) {
              setError(err instanceof Error ? err.message : 'Falha ao salvar');
            } finally {
              setLoading(false);
            }
          }}
        >
          <div className="grid gap-3 md:grid-cols-2">
            <label className="space-y-1">
              <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Nome</span>
              <Input
                value={environment.name}
                onChange={(event) => setEnvironment((prev) => (prev ? { ...prev, name: event.target.value } : prev))}
              />
            </label>
            <label className="space-y-1">
              <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Default branch</span>
              <Input
                value={environment.defaultBranch}
                onChange={(event) =>
                  setEnvironment((prev) => (prev ? { ...prev, defaultBranch: event.target.value } : prev))
                }
              />
            </label>
            <label className="space-y-1 md:col-span-2">
              <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Descrição</span>
              <Textarea
                rows={2}
                value={environment.description || ''}
                onChange={(event) => setEnvironment((prev) => (prev ? { ...prev, description: event.target.value } : prev))}
              />
            </label>
            <label className="space-y-1">
              <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Base image</span>
              <Input
                value={environment.baseImage}
                onChange={(event) => setEnvironment((prev) => (prev ? { ...prev, baseImage: event.target.value } : prev))}
              />
            </label>
            <label className="space-y-1">
              <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Internet mode</span>
              <select
                value={environment.internetMode}
                onChange={(event) => setEnvironment((prev) => (prev ? { ...prev, internetMode: event.target.value } : prev))}
                className="h-11 w-full rounded-[var(--radius-lg)] border border-[var(--border)] bg-transparent px-3 text-sm"
              >
                <option value="OFF">OFF</option>
                <option value="LIMITED">LIMITED</option>
                <option value="UNRESTRICTED">UNRESTRICTED</option>
              </select>
            </label>
            <label className="space-y-1 md:col-span-2">
              <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Setup script</span>
              <Textarea
                rows={4}
                value={environment.setupScript || ''}
                onChange={(event) => setEnvironment((prev) => (prev ? { ...prev, setupScript: event.target.value } : prev))}
              />
            </label>
            <label className="space-y-1 md:col-span-2">
              <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Maintenance script</span>
              <Textarea
                rows={3}
                value={environment.maintenanceScript || ''}
                onChange={(event) =>
                  setEnvironment((prev) => (prev ? { ...prev, maintenanceScript: event.target.value } : prev))
                }
              />
            </label>
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            <Button type="submit" disabled={loading}>
              {loading ? 'Salvando...' : 'Salvar'}
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={async () => {
                await codexApi.validateEnvironment(environment.id);
                await load();
              }}
            >
              Validate setup
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={async () => {
                await codexApi.resetEnvironmentCache(environment.id);
                await load();
              }}
            >
              Reset cache
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={async () => {
                await codexApi.deleteEnvironment(environment.id);
                router.push('/codex/settings/environments');
              }}
            >
              Remover
            </Button>
          </div>
          <div className="mt-4 grid gap-3 lg:grid-cols-2">
            <article className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] p-3">
              <h3 className="text-xs uppercase tracking-[0.12em] text-[var(--subtle-foreground)]">Execution history</h3>
              <ul className="mt-2 space-y-2 text-xs text-[var(--muted-foreground)]">
                {environment.executions.map((item) => (
                  <li key={item.id}>
                    {item.status} • {new Date(item.createdAt).toLocaleString('pt-BR')}
                    {item.errorMessage ? ` • ${item.errorMessage}` : ''}
                  </li>
                ))}
                {environment.executions.length === 0 && <li>Sem execuções registradas.</li>}
              </ul>
            </article>
            <article className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] p-3">
              <h3 className="text-xs uppercase tracking-[0.12em] text-[var(--subtle-foreground)]">Cache history</h3>
              <ul className="mt-2 space-y-2 text-xs text-[var(--muted-foreground)]">
                {environment.caches.map((item) => (
                  <li key={item.id}>
                    {item.status} • {new Date(item.updatedAt).toLocaleString('pt-BR')}
                    {item.invalidationReason ? ` • ${item.invalidationReason}` : ''}
                  </li>
                ))}
                {environment.caches.length === 0 && <li>Sem histórico de cache.</li>}
              </ul>
            </article>
          </div>
          {error && <p className="mt-3 text-sm text-[var(--destructive)]">{error}</p>}
        </form>
      )}
    </CodexShell>
  );
}

