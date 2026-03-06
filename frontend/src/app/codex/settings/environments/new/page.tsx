'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { CodexShell } from '@/components/codex/codex-shell';
import { codexApi } from '@/lib/codex-api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';

type Repo = {
  id: string;
  fullName: string;
  defaultBranch: string;
};

export default function NewEnvironmentPage() {
  const router = useRouter();
  const [repos, setRepos] = useState<Repo[]>([]);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [baseImage, setBaseImage] = useState('node:20-bullseye');
  const [defaultBranch, setDefaultBranch] = useState('main');
  const [setupScript, setSetupScript] = useState('npm ci');
  const [maintenanceScript, setMaintenanceScript] = useState('npm cache verify');
  const [internetMode, setInternetMode] = useState('OFF');
  const [domainAllowlist, setDomainAllowlist] = useState('');
  const [allowedMethods, setAllowedMethods] = useState('GET,HEAD');
  const [repositoryId, setRepositoryId] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    codexApi
      .listRepositories()
      .then((data) => {
        const casted = data as Repo[];
        setRepos(casted);
        if (casted[0]) {
          setRepositoryId(casted[0].id);
          setDefaultBranch(casted[0].defaultBranch);
        }
      })
      .catch(() => setRepos([]));
  }, []);

  return (
    <CodexShell title="Novo Environment" subtitle="Configuração completa de setup, cache e internet policy.">
      <form
        className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4"
        onSubmit={async (event) => {
          event.preventDefault();
          setLoading(true);
          setError(null);
          try {
            const created = (await codexApi.createEnvironment({
              name,
              description,
              baseImage,
              defaultBranch,
              setupScript,
              maintenanceScript,
              repositoryId,
              internetMode,
              domainAllowlist: domainAllowlist
                .split(',')
                .map((item) => item.trim())
                .filter(Boolean),
              allowedHttpMethods: allowedMethods
                .split(',')
                .map((item) => item.trim().toUpperCase())
                .filter(Boolean),
            })) as { id: string };
            router.push(`/codex/settings/environments/${created.id}`);
          } catch (err) {
            setError(err instanceof Error ? err.message : 'Falha ao criar environment');
          } finally {
            setLoading(false);
          }
        }}
      >
        <div className="grid gap-3 md:grid-cols-2">
          <label className="space-y-1">
            <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Nome</span>
            <Input value={name} onChange={(event) => setName(event.target.value)} required />
          </label>
          <label className="space-y-1">
            <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Repository</span>
            <select
              value={repositoryId}
              onChange={(event) => setRepositoryId(event.target.value)}
              className="h-11 w-full rounded-[var(--radius-lg)] border border-[var(--border)] bg-transparent px-3 text-sm"
            >
              {repos.map((repo) => (
                <option value={repo.id} key={repo.id}>
                  {repo.fullName}
                </option>
              ))}
            </select>
          </label>
          <label className="space-y-1 md:col-span-2">
            <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Descrição</span>
            <Textarea rows={3} value={description} onChange={(event) => setDescription(event.target.value)} />
          </label>
          <label className="space-y-1">
            <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Base image</span>
            <Input value={baseImage} onChange={(event) => setBaseImage(event.target.value)} />
          </label>
          <label className="space-y-1">
            <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Default branch</span>
            <Input value={defaultBranch} onChange={(event) => setDefaultBranch(event.target.value)} />
          </label>
          <label className="space-y-1 md:col-span-2">
            <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Setup script</span>
            <Textarea rows={4} value={setupScript} onChange={(event) => setSetupScript(event.target.value)} />
          </label>
          <label className="space-y-1 md:col-span-2">
            <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Maintenance script</span>
            <Textarea rows={3} value={maintenanceScript} onChange={(event) => setMaintenanceScript(event.target.value)} />
          </label>
          <label className="space-y-1">
            <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Internet mode</span>
            <select
              value={internetMode}
              onChange={(event) => setInternetMode(event.target.value)}
              className="h-11 w-full rounded-[var(--radius-lg)] border border-[var(--border)] bg-transparent px-3 text-sm"
            >
              <option value="OFF">OFF</option>
              <option value="LIMITED">LIMITED</option>
              <option value="UNRESTRICTED">UNRESTRICTED</option>
            </select>
          </label>
          <label className="space-y-1">
            <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Allowed methods</span>
            <Input value={allowedMethods} onChange={(event) => setAllowedMethods(event.target.value)} />
          </label>
          <label className="space-y-1 md:col-span-2">
            <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Domain allowlist (comma-separated)</span>
            <Input value={domainAllowlist} onChange={(event) => setDomainAllowlist(event.target.value)} />
          </label>
        </div>

        <div className="mt-4 flex gap-2">
          <Button type="submit" disabled={loading}>
            {loading ? 'Criando...' : 'Criar environment'}
          </Button>
          <Button type="button" variant="outline" onClick={() => router.push('/codex/settings/environments')}>
            Cancelar
          </Button>
        </div>
        {error && <p className="mt-3 text-sm text-[var(--destructive)]">{error}</p>}
      </form>
    </CodexShell>
  );
}

