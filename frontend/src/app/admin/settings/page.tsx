'use client';

import { useEffect, useState } from 'react';
import { CodexShell } from '@/components/codex/codex-shell';
import { codexApi } from '@/lib/codex-api';
import { Button } from '@/components/ui/button';

type ManagedConfig = {
  id: string;
  configKey: string;
  configValue: Record<string, unknown>;
  isLocked: boolean;
};

export default function AdminSettingsPage() {
  const [configs, setConfigs] = useState<ManagedConfig[]>([]);

  async function load() {
    const payload = (await codexApi.getManagedConfigs()) as ManagedConfig[];
    setConfigs(payload);
  }

  useEffect(() => {
    void load();
  }, []);

  return (
    <CodexShell title="Admin Settings" subtitle="Toggles de workspace: cloud tasks, internet policy, connectors e retenção.">
      <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
        <div className="grid gap-2 md:grid-cols-2">
          {[
            { key: 'workspace.cloudTasksEnabled', value: true },
            { key: 'workspace.allowMemberAdmin', value: false },
            { key: 'workspace.internet.defaultPolicy', value: 'OFF' },
            { key: 'workspace.analytics.retentionDays', value: 90 },
            { key: 'workspace.allowedConnectors', value: ['github', 'slack', 'linear'] },
          ].map((item) => (
            <Button
              key={item.key}
              variant="outline"
              onClick={async () => {
                await codexApi.createManagedConfig({
                  configKey: item.key,
                  configValue: {
                    value: item.value,
                  },
                  isLocked: false,
                });
                await load();
              }}
            >
              Set {item.key}
            </Button>
          ))}
        </div>
      </section>

      <section className="mt-3 rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
        <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Current workspace configs</h2>
        <ul className="mt-3 space-y-2 text-xs text-[var(--muted-foreground)]">
          {configs.map((config) => (
            <li key={config.id}>
              {config.configKey} • lock={String(config.isLocked)} • {JSON.stringify(config.configValue)}
            </li>
          ))}
          {configs.length === 0 && <li>Sem configurações aplicadas.</li>}
        </ul>
      </section>
    </CodexShell>
  );
}

