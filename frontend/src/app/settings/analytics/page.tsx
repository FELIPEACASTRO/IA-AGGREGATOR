'use client';

import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import { Activity, ArrowUpRight, BarChart3, Database, Gauge, RefreshCcw } from 'lucide-react';
import { AppShell } from '@/components/app/app-shell';
import { PageSection, PageStack } from '@/components/app/page-blueprint';
import { Button } from '@/components/ui/button';
import { analyticsService, type AnalyticsPersistedReport } from '@/lib/services/analytics-service';
import { toast } from '@/stores/toast-store';

function StatCard({
  label,
  value,
  helper,
  icon: Icon,
}: {
  label: string;
  value: string;
  helper: string;
  icon: React.ElementType;
}) {
  return (
    <div className="lume-panel-soft rounded-[var(--radius-xl)] p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">{label}</p>
          <p className="mt-2 text-[var(--text-2xl)] font-semibold text-[var(--foreground)]">{value}</p>
          <p className="mt-1 text-[var(--text-xs)] text-[var(--muted-foreground)]">{helper}</p>
        </div>
        <span className="inline-flex h-11 w-11 items-center justify-center rounded-[18px] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] text-[var(--brand-primary)]">
          <Icon className="h-5 w-5" />
        </span>
      </div>
    </div>
  );
}

const formatDateTime = (value?: string | null) => {
  if (!value) return 'sem data';
  try {
    return new Date(value).toLocaleString('pt-BR');
  } catch {
    return value;
  }
};

export default function AnalyticsPage() {
  const [reports, setReports] = useState<AnalyticsPersistedReport[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [lastUpdatedAt, setLastUpdatedAt] = useState<string | null>(null);

  const refresh = async () => {
    setIsLoading(true);
    try {
      const next = await analyticsService.listReports({
        page: 0,
        limit: 50,
        sortBy: 'receivedAt',
        sortDir: 'desc',
      });

      setReports(next);
      setLastUpdatedAt(new Date().toISOString());
      toast.success('Painel atualizado', `${next.length} relatório(s) disponíveis no período recente.`);
    } catch {
      toast.error('Falha no analytics', 'Não foi possível carregar os relatórios de uso.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    void refresh();
  }, []);

  const totalEvents = useMemo(
    () => reports.reduce((acc, report) => acc + (report.totalEvents || 0), 0),
    [reports],
  );

  const avgEvents = useMemo(
    () => (reports.length > 0 ? Math.round(totalEvents / reports.length) : 0),
    [reports, totalEvents],
  );

  const sourceBreakdown = useMemo(() => {
    const map = new Map<string, { reports: number; events: number }>();

    reports.forEach((report) => {
      const source = report.source || 'unknown';
      const current = map.get(source) || { reports: 0, events: 0 };
      current.reports += 1;
      current.events += report.totalEvents || 0;
      map.set(source, current);
    });

    const rows = Array.from(map.entries())
      .map(([source, data]) => ({ source, ...data }))
      .sort((a, b) => b.events - a.events);

    const maxEvents = Math.max(...rows.map((row) => row.events), 1);

    return rows.map((row) => ({
      ...row,
      eventSharePct: Math.round((row.events / Math.max(totalEvents, 1)) * 100),
      widthPct: Math.max(8, Math.round((row.events / maxEvents) * 100)),
    }));
  }, [reports, totalEvents]);

  const mostRecentReport = reports[0];

  const summaryCards = [
    {
      label: 'Relatórios',
      value: reports.length.toLocaleString('pt-BR'),
      helper: 'coleta recente disponível no workspace',
      icon: Database,
    },
    {
      label: 'Eventos',
      value: totalEvents.toLocaleString('pt-BR'),
      helper: 'volume agregado dos relatórios carregados',
      icon: Activity,
    },
    {
      label: 'Média por relatório',
      value: avgEvents.toLocaleString('pt-BR'),
      helper: 'densidade média para leitura de saúde do produto',
      icon: Gauge,
    },
    {
      label: 'Fontes ativas',
      value: sourceBreakdown.length.toLocaleString('pt-BR'),
      helper: 'origens distintas enviando telemetria',
      icon: BarChart3,
    },
  ];

  return (
    <AppShell
      title="Analytics"
      subtitle="Dashboard executivo de uso e engajamento. Diagnóstico detalhado permanece isolado na área técnica."
      headerActions={
        <div className="flex flex-wrap items-center gap-2">
          <Button variant="secondary" onClick={() => void refresh()} disabled={isLoading}>
            <RefreshCcw className="h-4 w-4" />
            {isLoading ? 'Atualizando...' : 'Atualizar métricas'}
          </Button>
          <Link
            href="/settings/analytics/debug"
            className="inline-flex min-h-11 items-center justify-center gap-2 rounded-[var(--radius-pill)] border border-[var(--border)] px-5 text-[0.86rem] font-semibold text-[var(--foreground)] transition-colors hover:border-[var(--brand-primary)]/40 hover:bg-[rgba(96,115,255,0.08)]"
          >
            Abrir diagnóstico técnico
            <ArrowUpRight className="h-4 w-4" />
          </Link>
        </div>
      }
    >
      <PageStack>
        <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {summaryCards.map((card) => (
            <StatCard
              key={card.label}
              label={card.label}
              value={card.value}
              helper={card.helper}
              icon={card.icon}
            />
          ))}
        </section>

        <div className="grid gap-4 xl:grid-cols-[minmax(0,1.2fr)_minmax(0,0.8fr)]">
          <PageSection className="p-5 md:p-6">
            <div className="flex items-center justify-between gap-3 border-b border-[var(--border)] pb-4">
              <div>
                <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Distribuição por fonte</p>
                <h2 className="mt-2 text-[var(--text-xl)] font-semibold text-[var(--foreground)]">Participação relativa do volume de eventos</h2>
              </div>
              <span className="rounded-full border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-3 py-1 text-[var(--text-xs)] text-[var(--muted-foreground)]">
                Atualizado: {formatDateTime(lastUpdatedAt)}
              </span>
            </div>

            <div className="mt-4 space-y-3">
              {sourceBreakdown.length === 0 ? (
                <div className="rounded-[var(--radius-xl)] border border-dashed border-[var(--border)] bg-[rgba(255,255,255,0.02)] p-6 text-center">
                  <p className="text-[var(--text-sm)] text-[var(--muted-foreground)]">Nenhum relatório persistido foi encontrado.</p>
                </div>
              ) : (
                sourceBreakdown.map((row) => (
                  <div key={row.source} className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-3">
                    <div className="flex items-center justify-between gap-3">
                      <p className="truncate text-[var(--text-sm)] font-semibold text-[var(--foreground)]">{row.source}</p>
                      <p className="text-[var(--text-xs)] text-[var(--muted-foreground)]">
                        {row.events.toLocaleString('pt-BR')} eventos ({row.eventSharePct}%)
                      </p>
                    </div>
                    <div className="mt-2 h-2 rounded-full bg-[rgba(255,255,255,0.06)]">
                      <div
                        className="h-2 rounded-full bg-[var(--brand-primary)]"
                        style={{ width: `${row.widthPct}%` }}
                      />
                    </div>
                    <p className="mt-1 text-[0.72rem] text-[var(--subtle-foreground)]">{row.reports} relatório(s) nesta fonte</p>
                  </div>
                ))
              )}
            </div>
          </PageSection>

          <PageSection variant="soft" className="p-5 md:p-6">
            <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Leitura rápida</p>
            <h2 className="mt-2 text-[var(--text-xl)] font-semibold text-[var(--foreground)]">Saúde operacional</h2>
            <div className="mt-4 space-y-3 text-[var(--text-sm)] text-[var(--muted-foreground)]">
              <p>
                O painel executivo mostra sinal de adoção, densidade de uso e cobertura de origem. Use este resumo para decisões de produto e priorização de onboarding.
              </p>
              <p>
                O detalhamento de eventos, funis técnicos, coortes e exportações continua disponível em <strong className="text-[var(--foreground)]">/settings/analytics/debug</strong>.
              </p>
            </div>

            <div className="mt-5 rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-4">
              <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Último relatório</p>
              {mostRecentReport ? (
                <>
                  <p className="mt-2 text-[var(--text-base)] font-semibold text-[var(--foreground)]">{mostRecentReport.source}</p>
                  <p className="mt-1 text-[var(--text-xs)] text-[var(--muted-foreground)]">
                    {mostRecentReport.totalEvents.toLocaleString('pt-BR')} eventos · recebido em {formatDateTime(mostRecentReport.receivedAt)}
                  </p>
                </>
              ) : (
                <p className="mt-2 text-[var(--text-xs)] text-[var(--muted-foreground)]">Sem histórico recente para exibir.</p>
              )}
            </div>

            <Link
              href="/settings/analytics/debug"
              className="mt-4 inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-[var(--radius-pill)] border border-[var(--border)] px-4 text-[0.82rem] font-semibold text-[var(--foreground)] transition-colors hover:border-[var(--brand-primary)]/40 hover:bg-[rgba(96,115,255,0.08)]"
            >
              Abrir modo técnico
              <ArrowUpRight className="h-4 w-4" />
            </Link>
          </PageSection>
        </div>
      </PageStack>
    </AppShell>
  );
}

