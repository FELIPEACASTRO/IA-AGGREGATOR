'use client';

import { useEffect, useState } from 'react';
import { CodexShell } from '@/components/codex/codex-shell';
import { codexApi } from '@/lib/codex-api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';

type ManagedConfig = {
  id: string;
  configKey: string;
  configValue: Record<string, unknown>;
  isLocked: boolean;
};

export default function ManagedConfigsPage() {
  const [configs, setConfigs] = useState<ManagedConfig[]>([]);
  const [configKey, setConfigKey] = useState('internet.defaultPolicy');
  const [configValue, setConfigValue] = useState('{"mode":"OFF"}');
  const [isLocked, setIsLocked] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    try {
      const payload = (await codexApi.getManagedConfigs()) as ManagedConfig[];
      setConfigs(payload);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao carregar managed configs');
    }
  }

  useEffect(() => {
    void load();
  }, []);

  return (
    <CodexShell title="Managed Configs" subtitle="Políticas globais por workspace com lock para governança.">
      <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
        <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Nova configuração</h2>
        <div className="mt-3 grid gap-3 md:grid-cols-2">
          <label className="space-y-1">
            <span className="text-xs uppercase tracking-[0.12em] text-[var(--subtle-foreground)]">Key</span>
            <Input value={configKey} onChange={(event) => setConfigKey(event.target.value)} />
          </label>
          <label className="space-y-1">
            <span className="text-xs uppercase tracking-[0.12em] text-[var(--subtle-foreground)]">Lock</span>
            <select
              className="h-11 w-full rounded-[var(--radius-lg)] border border-[var(--border)] bg-transparent px-3 text-sm"
              value={isLocked ? 'yes' : 'no'}
              onChange={(event) => setIsLocked(event.target.value === 'yes')}
            >
              <option value="no">Unlocked</option>
              <option value="yes">Locked</option>
            </select>
          </label>
          <label className="space-y-1 md:col-span-2">
            <span className="text-xs uppercase tracking-[0.12em] text-[var(--subtle-foreground)]">Value JSON</span>
            <Textarea rows={6} value={configValue} onChange={(event) => setConfigValue(event.target.value)} />
          </label>
        </div>
        <div className="mt-3">
          <Button
            onClick={async () => {
              await codexApi.createManagedConfig({
                configKey,
                configValue: JSON.parse(configValue),
                isLocked,
              });
              await load();
            }}
          >
            Salvar configuração
          </Button>
        </div>
      </section>

      <section className="mt-3 rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
        <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Configurações atuais</h2>
        <div className="mt-3 space-y-2">
          {configs.map((config) => (
            <article key={config.id} className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] p-3">
              <p className="text-sm font-semibold">{config.configKey}</p>
              <p className="text-xs text-[var(--muted-foreground)]">Locked: {String(config.isLocked)}</p>
              <pre className="mt-2 overflow-auto rounded-[var(--radius-md)] border border-[var(--border)] bg-black/40 p-2 text-xs text-[var(--muted-foreground)]">
                {JSON.stringify(config.configValue, null, 2)}
              </pre>
            </article>
          ))}
          {configs.length === 0 && <p className="text-sm text-[var(--muted-foreground)]">Nenhuma configuração registrada.</p>}
        </div>
      </section>
      {error && <p className="mt-3 text-sm text-[var(--destructive)]">{error}</p>}
    </CodexShell>
  );
}
