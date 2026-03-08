'use client';

import { useEffect, useState } from 'react';
import { CodexShell } from '@/components/codex/codex-shell';
import type { AgentDto, ProviderCredentialRequirementDto, ProviderDto } from '@/lib/contracts/platform';

type ProvidersResponse = {
  providers: Array<
    ProviderDto & {
      envKeys?: string[];
      missingKeys?: string[];
      resolvedBaseUrl?: string;
      supportsLiveChat?: boolean;
      models?: Array<{
        id: string;
        label: string;
        availability?: string;
      }>;
    }
  >;
};

type AgentsResponse = {
  agents: AgentDto[];
};

type CredentialRequirementsResponse = {
  providers: ProviderCredentialRequirementDto[];
};

export default function ProvidersSettingsPage() {
  const [providers, setProviders] = useState<ProvidersResponse['providers']>([]);
  const [agents, setAgents] = useState<AgentDto[]>([]);
  const [requirements, setRequirements] = useState<ProviderCredentialRequirementDto[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      try {
        const [providersResponse, agentsResponse, credentialsResponse] = await Promise.all([
          fetch('/api/v1/providers', { cache: 'no-store' }),
          fetch('/api/v1/agents', { cache: 'no-store' }),
          fetch('/api/v1/credentials/required', { cache: 'no-store' }),
        ]);

        const providersPayload = (await providersResponse.json()) as { data: ProvidersResponse };
        const agentsPayload = (await agentsResponse.json()) as { data: AgentsResponse };
        const credentialsPayload = (await credentialsResponse.json()) as { data: CredentialRequirementsResponse };

        setProviders(providersPayload.data.providers);
        setAgents(agentsPayload.data.agents);
        setRequirements(credentialsPayload.data.providers);
        setError(null);
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : 'Falha ao carregar providers');
      }
    }

    void load();
  }, []);

  return (
    <CodexShell
      title="Providers"
      subtitle="Catalogo operacional de APIs, status de configuracao e versoes de agentes suportadas."
    >
      <section className="grid gap-3 xl:grid-cols-[1.5fr_1fr]">
        <article className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[var(--surface)] p-4">
          <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">
            Providers catalogados
          </h2>
          <div className="mt-3 space-y-3">
            {providers.map((provider) => (
              <div
                key={provider.id}
                className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface-hover)] p-3"
              >
                <div className="flex flex-wrap items-center gap-2">
                  <p className="text-sm font-semibold text-[var(--foreground)]">{provider.name}</p>
                  <span className="rounded-full border border-[var(--border)] px-2 py-0.5 text-[11px] text-[var(--muted-foreground)]">
                    {provider.category}
                  </span>
                  <span className="rounded-full border border-[var(--border)] px-2 py-0.5 text-[11px] text-[var(--muted-foreground)]">
                    {provider.protocol}
                  </span>
                  <span
                    className={`rounded-full px-2 py-0.5 text-[11px] ${
                      provider.configured
                        ? 'bg-[var(--success-light)] text-[var(--success)]'
                        : 'bg-[var(--warning-light)] text-[var(--warning)]'
                    }`}
                  >
                    {provider.configured ? 'configured' : provider.status}
                  </span>
                </div>
                <p className="mt-2 text-xs text-[var(--muted-foreground)]">{provider.summary}</p>
                {provider.missingKeys?.length ? (
                  <p className="mt-2 text-xs text-[var(--warning)]">
                    Missing: {provider.missingKeys.join(', ')}
                  </p>
                ) : null}
                {provider.resolvedBaseUrl ? (
                  <p className="mt-1 break-all text-[11px] text-[var(--subtle-foreground)]">
                    {provider.resolvedBaseUrl}
                  </p>
                ) : null}
                {provider.models?.length ? (
                  <div className="mt-2 flex flex-wrap gap-1.5">
                    {provider.models.slice(0, 4).map((model) => (
                      <span
                        key={model.id}
                        className="rounded-full bg-[var(--surface-active)] px-2 py-0.5 text-[11px] text-[var(--muted-foreground)]"
                      >
                        {model.label}
                      </span>
                    ))}
                  </div>
                ) : null}
              </div>
            ))}
          </div>
        </article>

        <div className="space-y-3">
          <article className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[var(--surface)] p-4">
            <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">
              Agent versions
            </h2>
            <div className="mt-3 space-y-2">
              {agents.map((agent) => (
                <div
                  key={agent.id}
                  className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface-hover)] p-3"
                >
                  <p className="text-sm font-semibold text-[var(--foreground)]">
                    {agent.label} <span className="text-[var(--muted-foreground)]">{agent.version}</span>
                  </p>
                  <p className="mt-1 text-xs text-[var(--muted-foreground)]">{agent.summary}</p>
                </div>
              ))}
            </div>
          </article>

          <article className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[var(--surface)] p-4">
            <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">
              API keys necessarias
            </h2>
            <div className="mt-3 space-y-3">
              {requirements.map((provider) => (
                <div
                  key={provider.providerId}
                  className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface-hover)] p-3"
                >
                  <p className="text-sm font-semibold text-[var(--foreground)]">{provider.providerName}</p>
                  <ul className="mt-2 space-y-1 text-[11px] text-[var(--muted-foreground)]">
                    {provider.env.map((item) => (
                      <li key={item.key}>
                        {item.key}
                        {item.required ? ' *' : ' (opcional)'}
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
          </article>
        </div>
      </section>

      {error ? <p className="mt-3 text-sm text-[var(--destructive)]">{error}</p> : null}
    </CodexShell>
  );
}
