'use client';

import { useEffect, useState } from 'react';
import { CodexShell } from '@/components/codex/codex-shell';
import { codexApi } from '@/lib/codex-api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

type ConnectorPayload = {
  github: Array<{ id: string; accountLogin: string; status: string; installationExternalId: string }>;
  slack: Array<{ id: string; teamName: string; status: string; teamId: string }>;
  linear: Array<{ id: string; organizationName: string; status: string; organizationId: string }>;
};

export default function ConnectorsSettingsPage() {
  const [payload, setPayload] = useState<ConnectorPayload | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [githubLogin, setGithubLogin] = useState('davis');
  const [slackTeam, setSlackTeam] = useState('Lume Workspace');
  const [linearOrg, setLinearOrg] = useState('Lume Org');

  async function load() {
    try {
      const data = (await codexApi.getConnectorsStatus()) as ConnectorPayload;
      setPayload(data);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao carregar status dos conectores');
    }
  }

  useEffect(() => {
    void load();
  }, []);

  return (
    <CodexShell title="Connectors" subtitle="GitHub, Slack e Linear com health check e reconnect.">
      <section className="grid gap-3 lg:grid-cols-3">
        <article className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
          <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">GitHub</h2>
          <Input className="mt-3" value={githubLogin} onChange={(event) => setGithubLogin(event.target.value)} />
          <Button
            className="mt-2"
            onClick={async () => {
              await codexApi.installGitHub({
                accountLogin: githubLogin,
                installationExternalId: `inst-${Date.now()}`,
              });
              await load();
            }}
          >
            Connect / Reinstall
          </Button>
          <ul className="mt-3 space-y-2 text-xs text-[var(--muted-foreground)]">
            {payload?.github?.map((item) => (
              <li key={item.id}>
                {item.accountLogin} • {item.status}
              </li>
            ))}
          </ul>
        </article>

        <article className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
          <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Slack</h2>
          <Input className="mt-3" value={slackTeam} onChange={(event) => setSlackTeam(event.target.value)} />
          <Button
            className="mt-2"
            onClick={async () => {
              await codexApi.installSlack({
                teamId: `team-${Date.now()}`,
                teamName: slackTeam,
                postFinalReply: true,
              });
              await load();
            }}
          >
            Install App
          </Button>
          <ul className="mt-3 space-y-2 text-xs text-[var(--muted-foreground)]">
            {payload?.slack?.map((item) => (
              <li key={item.id}>
                {item.teamName} • {item.status}
              </li>
            ))}
          </ul>
        </article>

        <article className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
          <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Linear</h2>
          <Input className="mt-3" value={linearOrg} onChange={(event) => setLinearOrg(event.target.value)} />
          <Button
            className="mt-2"
            onClick={async () => {
              await codexApi.installLinear({
                organizationId: `org-${Date.now()}`,
                organizationName: linearOrg,
              });
              await load();
            }}
          >
            Link Workspace
          </Button>
          <ul className="mt-3 space-y-2 text-xs text-[var(--muted-foreground)]">
            {payload?.linear?.map((item) => (
              <li key={item.id}>
                {item.organizationName} • {item.status}
              </li>
            ))}
          </ul>
        </article>
      </section>
      {error && <p className="mt-3 text-sm text-[var(--destructive)]">{error}</p>}
    </CodexShell>
  );
}

