'use client';

import { useEffect, useState } from 'react';
import { CodexShell } from '@/components/codex/codex-shell';

type EndpointDto = { methods: string; path: string };

const FALLBACK_ROWS = [
  'POST /api/v1/auth/login',
  'GET /api/v1/auth/session',
  'GET /api/v1/ai/models',
  'GET /api/v1/billing/plans',
  'GET/POST /api/v1/chat/conversations',
  'GET /api/v1/analytics/dashboard',
  'GET /api/v1/compliance/exports',
];

export default function ApiReferencePage() {
  const [rows, setRows] = useState<string[]>(FALLBACK_ROWS);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const API_BASE = process.env.NEXT_PUBLIC_API_URL || '';
    fetch(`${API_BASE}/api/v1/meta/endpoints`, { cache: 'no-store' })
      .then((res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json() as Promise<{ success: boolean; data?: EndpointDto[] }>;
      })
      .then((payload) => {
        if (payload.success && payload.data) {
          setRows(payload.data.map((ep) => `${ep.methods} ${ep.path}`));
        }
      })
      .catch(() => {
        // Keep fallback rows
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <CodexShell title="API Reference" subtitle="Endpoints registrados no backend, atualizados em tempo real.">
      <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
        <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">
          Endpoints disponíveis {loading && <span className="ml-2 text-xs text-[var(--muted-foreground)]">carregando...</span>}
        </h2>
        <ul className="mt-3 space-y-2">
          {rows.map((row) => (
            <li key={row} className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] px-3 py-2 font-mono text-xs text-[var(--muted-foreground)]">
              {row}
            </li>
          ))}
        </ul>
      </section>
    </CodexShell>
  );
}
