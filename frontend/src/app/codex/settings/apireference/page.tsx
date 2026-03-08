import { CodexShell } from '@/components/codex/codex-shell';

const apiRows = [
  'POST /api/auth/login',
  'GET /api/auth/session',
  'POST /api/v1/ai/chat',
  'GET /api/v1/providers',
  'GET /api/v1/providers/:id/models',
  'GET /api/v1/providers/:id/health',
  'GET /api/v1/agents',
  'GET /api/v1/credentials/required',
  'GET /api/repositories',
  'GET/POST /api/environments',
  'GET/POST /api/tasks',
  'GET /api/tasks/:id/events',
  'POST /api/tasks/:id/followups',
  'POST /api/tasks/:id/pull-requests',
  'GET/POST /api/code-review/policies',
  'POST /api/integrations/github/install',
  'POST /api/webhooks/github',
  'GET /api/usage',
  'GET /api/credits',
  'GET /api/analytics',
  'GET/POST /api/compliance/exports',
  'GET/PATCH /api/managed-configs/:id',
];

export default function ApiReferencePage() {
  return (
    <CodexShell title="API Reference" subtitle="Surface interna para analytics/compliance/settings e task operations.">
      <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[var(--surface)] p-4">
        <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Endpoints disponíveis</h2>
        <ul className="mt-3 space-y-2">
          {apiRows.map((row) => (
            <li key={row} className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface-hover)] px-3 py-2 font-mono text-xs text-[var(--muted-foreground)]">
              {row}
            </li>
          ))}
        </ul>
      </section>
    </CodexShell>
  );
}

