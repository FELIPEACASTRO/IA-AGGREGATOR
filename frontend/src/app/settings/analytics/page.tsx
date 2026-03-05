'use client';

import { useMemo, useState } from 'react';
import { AppShell } from '@/components/app/app-shell';
import { Button } from '@/components/ui/button';
import { clearTrackedEvents, getTrackedEvents, type AnalyticsEvent } from '@/lib/analytics';
import { toast } from '@/stores/toast-store';

const formatDate = (value: string) => {
  try {
    return new Date(value).toLocaleString('pt-BR');
  } catch {
    return value;
  }
};

const RETRYABLE_STATUS = new Set([408, 425, 429, 500, 502, 503, 504]);
const REPORT_MAX_ATTEMPTS = 3;
const REPORT_BASE_DELAY_MS = 150;
const REPORT_HISTORY_PAGE_SIZE = 20;
const REPORT_EVENT_PAGE_SIZE = 50;

const EVENT_CATEGORIES = [
  { value: 'all', label: 'Todas categorias' },
  { value: 'auth', label: 'Auth' },
  { value: 'chat', label: 'Chat' },
  { value: 'billing', label: 'Billing' },
  { value: 'content', label: 'Content' },
  { value: 'system', label: 'System' },
];

const REPORT_SORT_OPTIONS = [
  { value: 'receivedAt', label: 'Recebimento' },
  { value: 'generatedAt', label: 'Geração' },
  { value: 'totalEvents', label: 'Total de eventos' },
  { value: 'source', label: 'Fonte' },
];

const sleep = (ms: number) => new Promise<void>((resolve) => {
  setTimeout(resolve, ms);
});

const shouldRetry = (status: number | null) => {
  if (status === null) return true;
  return RETRYABLE_STATUS.has(status);
};

const csvValue = (value: unknown) => {
  if (value === null || value === undefined) return '""';
  const escaped = String(value).replace(/"/g, '""');
  return `"${escaped}"`;
};

type PersistedReport = {
  id: string;
  source: string;
  generatedAt?: string | null;
  receivedAt?: string | null;
  totalEvents: number;
  counters?: Record<string, number>;
};

type PersistedReportEvent = {
  id: string;
  reportId: string;
  eventName: string;
  eventCategory: string;
  eventTimestamp?: string | null;
  createdAt?: string | null;
  metadata?: Record<string, unknown>;
};

const resolveAnalyticsEndpoint = (suffix: string) => {
  const base = process.env.NEXT_PUBLIC_ANALYTICS_ENDPOINT
    || (process.env.NEXT_PUBLIC_API_URL
      ? `${process.env.NEXT_PUBLIC_API_URL}/api/v1/analytics/events`
      : '/api/v1/analytics/events');
  if (suffix === 'events') return base;
  return base.replace(/\/events$/, `/${suffix}`);
};

export default function AnalyticsDiagnosticsPage() {
  const [events, setEvents] = useState<AnalyticsEvent[]>(() => getTrackedEvents());
  const [isSendingReport, setIsSendingReport] = useState(false);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [isLoadingReportDetails, setIsLoadingReportDetails] = useState(false);
  const [persistedReports, setPersistedReports] = useState<PersistedReport[]>([]);
  const [historyFromDate, setHistoryFromDate] = useState('');
  const [historyToDate, setHistoryToDate] = useState('');
  const [historySortBy, setHistorySortBy] = useState('receivedAt');
  const [historySortDir, setHistorySortDir] = useState<'asc' | 'desc'>('desc');
  const [historyNextPage, setHistoryNextPage] = useState(0);
  const [hasMorePersistedReports, setHasMorePersistedReports] = useState(false);
  const [selectedReportId, setSelectedReportId] = useState<string | null>(null);
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [selectedReportEvents, setSelectedReportEvents] = useState<PersistedReportEvent[]>([]);
  const [selectedReportOffset, setSelectedReportOffset] = useState(0);
  const [hasMoreReportEvents, setHasMoreReportEvents] = useState(false);

  const counters = useMemo(() => {
    const counts: Record<string, number> = {};
    events.forEach((event) => {
      counts[event.event] = (counts[event.event] || 0) + 1;
    });
    return counts;
  }, [events]);

  const refresh = () => {
    setEvents(getTrackedEvents());
    toast.info('Dados atualizados', 'A listagem de eventos foi recarregada.');
  };

  const clearAll = () => {
    clearTrackedEvents();
    setEvents([]);
    toast.success('Analytics limpo', 'Todos os eventos locais foram removidos.');
  };

  const exportJson = () => {
    const blob = new Blob([JSON.stringify(events, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.href = url;
    link.download = `ia-analytics-${Date.now()}.json`;
    link.click();

    URL.revokeObjectURL(url);
    toast.success('Exportação concluída', 'Arquivo JSON gerado com os eventos capturados.');
  };

  const exportPersistedReportsCsv = () => {
    if (persistedReports.length === 0) return;

    const header = ['id', 'source', 'generatedAt', 'receivedAt', 'totalEvents', 'counters'];
    const rows = persistedReports.map((report) => [
      csvValue(report.id),
      csvValue(report.source),
      csvValue(report.generatedAt ?? ''),
      csvValue(report.receivedAt ?? ''),
      csvValue(report.totalEvents),
      csvValue(JSON.stringify(report.counters ?? {})),
    ].join(','));

    const csv = [header.join(','), ...rows].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.href = url;
    link.download = `ia-analytics-history-${Date.now()}.csv`;
    link.click();

    URL.revokeObjectURL(url);
    toast.success('CSV exportado', `${persistedReports.length} relatório(s) exportado(s).`);
  };

  const sendReport = async () => {
    if (events.length === 0 || isSendingReport) return;

    setIsSendingReport(true);
    const endpoint = resolveAnalyticsEndpoint('events');
    const payload = {
      source: 'frontend',
      generatedAt: new Date().toISOString(),
      totalEvents: events.length,
      counters,
      events,
    };

    try {
      let delivered = false;

      for (let attempt = 1; attempt <= REPORT_MAX_ATTEMPTS; attempt += 1) {
        let status: number | null = null;
        try {
          const response = await fetch(endpoint, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify(payload),
          });

          status = response.status;
          if (response.ok) {
            delivered = true;
            break;
          }
        } catch {
          status = null;
        }

        const retry = attempt < REPORT_MAX_ATTEMPTS && shouldRetry(status);
        if (!retry) {
          break;
        }

        const backoffMs = REPORT_BASE_DELAY_MS * 2 ** (attempt - 1);
        await sleep(backoffMs);
      }

      if (!delivered) {
        throw new Error('Failed to send analytics report');
      }

      toast.success('Relatório enviado', 'Eventos enviados para o endpoint de analytics.');
    } catch {
      toast.error('Falha ao enviar relatório', 'Configure o endpoint e tente novamente.');
    } finally {
      setIsSendingReport(false);
    }
  };

  const loadPersistedReports = async (options?: { append?: boolean; page?: number }) => {
    if (isLoadingHistory) return;
    const append = options?.append ?? false;
    const page = options?.page ?? 0;

    setIsLoadingHistory(true);
    try {
      const from = historyFromDate ? `${historyFromDate}T00:00:00Z` : null;
      const to = historyToDate ? `${historyToDate}T23:59:59Z` : null;
      const params = new URLSearchParams({
        limit: String(REPORT_HISTORY_PAGE_SIZE),
        page: String(page),
        sortBy: historySortBy,
        sortDir: historySortDir,
      });
      if (from) params.set('from', from);
      if (to) params.set('to', to);

      const endpoint = `${resolveAnalyticsEndpoint('reports')}?${params.toString()}`;
      const response = await fetch(endpoint, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch persisted reports: ${response.status}`);
      }

      const payload = await response.json() as { data?: PersistedReport[] };
      const reports = Array.isArray(payload?.data) ? payload.data : [];
      setHistoryNextPage(page + 1);
      setHasMorePersistedReports(reports.length === REPORT_HISTORY_PAGE_SIZE);
      setPersistedReports((current) => (append ? [...current, ...reports] : reports));
      toast.success(
        append ? 'Mais histórico carregado' : 'Histórico carregado',
        `${reports.length} relatório(s) persistido(s).`
      );
    } catch {
      toast.error('Falha ao carregar histórico', 'Não foi possível buscar relatórios persistidos.');
    } finally {
      setIsLoadingHistory(false);
    }
  };

  const loadReportEvents = async (
    reportId: string,
    options?: { append?: boolean; offset?: number; category?: string }
  ) => {
    if (isLoadingReportDetails) return;

    const append = options?.append ?? false;
    const offset = options?.offset ?? 0;
    const category = options?.category ?? selectedCategory;

    setIsLoadingReportDetails(true);
    setSelectedReportId(reportId);
    try {
      const params = new URLSearchParams({
        limit: String(REPORT_EVENT_PAGE_SIZE),
        offset: String(offset),
      });
      if (category !== 'all') {
        params.set('category', category);
      }

      const endpoint = `${resolveAnalyticsEndpoint(`reports/${reportId}/events`)}?${params.toString()}`;
      const response = await fetch(endpoint, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch report events: ${response.status}`);
      }

      const payload = await response.json() as { data?: PersistedReportEvent[] };
      const reportEvents = Array.isArray(payload?.data) ? payload.data : [];
      setSelectedCategory(category);
      setSelectedReportOffset(offset + reportEvents.length);
      setHasMoreReportEvents(reportEvents.length === REPORT_EVENT_PAGE_SIZE);
      setSelectedReportEvents((current) => (append ? [...current, ...reportEvents] : reportEvents));
      toast.success(
        append ? 'Mais eventos carregados' : 'Detalhes carregados',
        `${reportEvents.length} evento(s) do relatório.`
      );
    } catch {
      toast.error('Falha ao carregar detalhes', 'Não foi possível buscar eventos do relatório.');
    } finally {
      setIsLoadingReportDetails(false);
    }
  };

  return (
    <AppShell
      title="Analytics"
      subtitle="Diagnóstico local de eventos UX para Product/Data"
      headerActions={
        <div className="flex items-center gap-2">
          <Button variant="secondary" onClick={refresh}>
            Atualizar
          </Button>
          <Button variant="primary" onClick={sendReport} disabled={events.length === 0 || isSendingReport}>
            {isSendingReport ? 'Enviando...' : 'Enviar relatório'}
          </Button>
          <Button
            variant="ghost"
            onClick={() => {
              setHistoryNextPage(0);
              setHasMorePersistedReports(false);
              setPersistedReports([]);
              loadPersistedReports({ append: false, page: 0 });
            }}
            disabled={isLoadingHistory}
          >
            {isLoadingHistory ? 'Carregando...' : 'Carregar histórico'}
          </Button>
          <Button variant="ghost" onClick={exportJson} disabled={events.length === 0}>
            Exportar JSON
          </Button>
          <Button variant="destructive" onClick={clearAll} disabled={events.length === 0}>
            Limpar eventos
          </Button>
        </div>
      }
    >
      <div className="grid gap-4 p-6 lg:grid-cols-[320px_1fr]">
        <aside className="rounded-xl border border-[var(--border)] p-4">
          <p className="text-sm font-semibold">Resumo</p>
          <p className="mt-1 text-xs text-[var(--muted-foreground)]">
            Total de eventos: {events.length}
          </p>
          <div className="mt-3 space-y-2">
            {Object.keys(counters).length === 0 ? (
              <p className="text-sm text-[var(--muted-foreground)]">Sem eventos capturados.</p>
            ) : (
              Object.entries(counters).map(([name, count]) => (
                <div key={name} className="rounded-lg border border-[var(--border)] px-3 py-2">
                  <p className="truncate text-xs font-medium">{name}</p>
                  <p className="text-xs text-[var(--muted-foreground)]">{count} ocorrência(s)</p>
                </div>
              ))
            )}
          </div>

          <div className="mt-5">
            <p className="text-sm font-semibold">Relatórios persistidos</p>
            <div className="mt-2 space-y-2">
              <div className="grid grid-cols-2 gap-2">
                <input
                  type="date"
                  aria-label="Data inicial do histórico"
                  value={historyFromDate}
                  onChange={(event) => setHistoryFromDate(event.target.value)}
                  className="rounded-lg border border-[var(--border)] bg-[var(--background)] px-2 py-1 text-xs"
                />
                <input
                  type="date"
                  aria-label="Data final do histórico"
                  value={historyToDate}
                  onChange={(event) => setHistoryToDate(event.target.value)}
                  className="rounded-lg border border-[var(--border)] bg-[var(--background)] px-2 py-1 text-xs"
                />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <select
                  aria-label="Ordenar histórico por"
                  value={historySortBy}
                  onChange={(event) => setHistorySortBy(event.target.value)}
                  className="rounded-lg border border-[var(--border)] bg-[var(--background)] px-2 py-1 text-xs"
                >
                  {REPORT_SORT_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
                <select
                  aria-label="Direção da ordenação do histórico"
                  value={historySortDir}
                  onChange={(event) => setHistorySortDir(event.target.value as 'asc' | 'desc')}
                  className="rounded-lg border border-[var(--border)] bg-[var(--background)] px-2 py-1 text-xs"
                >
                  <option value="desc">Descendente</option>
                  <option value="asc">Ascendente</option>
                </select>
              </div>
              <Button
                variant="ghost"
                className="w-full text-xs"
                onClick={exportPersistedReportsCsv}
                disabled={persistedReports.length === 0}
              >
                Exportar histórico CSV
              </Button>
            </div>
            <div className="mt-2 space-y-2">
              {persistedReports.length === 0 ? (
                <p className="text-xs text-[var(--muted-foreground)]">Nenhum histórico carregado.</p>
              ) : (
                <>
                  {persistedReports.map((report) => (
                    <div key={report.id} className="rounded-lg border border-[var(--border)] px-3 py-2">
                      <p className="truncate text-xs font-medium">{report.source} · {report.totalEvents} evento(s)</p>
                      <p className="text-xs text-[var(--muted-foreground)]">
                        {report.receivedAt ? formatDate(report.receivedAt) : 'Sem data de recebimento'}
                      </p>
                      <Button
                        variant="ghost"
                        className="mt-2 h-7 px-2 text-xs"
                        onClick={() => {
                          setSelectedCategory('all');
                          setSelectedReportOffset(0);
                          setHasMoreReportEvents(false);
                          setSelectedReportEvents([]);
                          loadReportEvents(report.id, { append: false, offset: 0, category: 'all' });
                        }}
                        disabled={isLoadingReportDetails}
                      >
                        {isLoadingReportDetails && selectedReportId === report.id ? 'Carregando...' : 'Ver detalhes'}
                      </Button>
                    </div>
                  ))}
                  <Button
                    variant="ghost"
                    className="w-full text-xs"
                    onClick={() => loadPersistedReports({ append: true, page: historyNextPage })}
                    disabled={isLoadingHistory || !hasMorePersistedReports}
                  >
                    {isLoadingHistory ? 'Carregando...' : 'Carregar mais histórico'}
                  </Button>
                </>
              )}
            </div>
          </div>
        </aside>

        <section className="rounded-xl border border-[var(--border)] p-4">
          <p className="text-sm font-semibold">Eventos</p>
          <div className="mt-3 max-h-[520px] space-y-2 overflow-y-auto pr-1">
            {events.length === 0 ? (
              <p className="text-sm text-[var(--muted-foreground)]">
                Nenhum evento encontrado. Interaja com o sistema e clique em Atualizar.
              </p>
            ) : (
              [...events].reverse().map((event, index) => (
                <article key={`${event.timestamp}-${index}`} className="rounded-lg border border-[var(--border)] p-3">
                  <p className="text-sm font-medium">{event.event}</p>
                  <p className="mt-1 text-xs text-[var(--muted-foreground)]">{formatDate(event.timestamp)}</p>
                  {event.metadata ? (
                    <pre className="mt-2 overflow-x-auto rounded bg-[var(--secondary)] p-2 text-xs">
{JSON.stringify(event.metadata, null, 2)}
                    </pre>
                  ) : null}
                </article>
              ))
            )}
          </div>

          <div className="mt-5 border-t border-[var(--border)] pt-4">
            <p className="text-sm font-semibold">Detalhes do relatório persistido</p>
            {selectedReportId ? (
              <div className="mt-2 flex items-center gap-2">
                <select
                  aria-label="Filtrar categoria de eventos persistidos"
                  value={selectedCategory}
                  onChange={(event) => {
                    const nextCategory = event.target.value;
                    setSelectedCategory(nextCategory);
                    if (selectedReportId) {
                      setSelectedReportOffset(0);
                      setHasMoreReportEvents(false);
                      setSelectedReportEvents([]);
                      loadReportEvents(selectedReportId, { append: false, offset: 0, category: nextCategory });
                    }
                  }}
                  className="rounded-lg border border-[var(--border)] bg-[var(--background)] px-2 py-1 text-xs"
                >
                  {EVENT_CATEGORIES.map((item) => (
                    <option key={item.value} value={item.value}>{item.label}</option>
                  ))}
                </select>
                <Button
                  variant="ghost"
                  className="h-7 px-2 text-xs"
                  onClick={() => loadReportEvents(selectedReportId, {
                    append: true,
                    offset: selectedReportOffset,
                    category: selectedCategory,
                  })}
                  disabled={isLoadingReportDetails || !hasMoreReportEvents}
                >
                  {isLoadingReportDetails ? 'Carregando...' : 'Carregar mais'}
                </Button>
              </div>
            ) : null}
            <div className="mt-2 max-h-[260px] space-y-2 overflow-y-auto pr-1">
              {selectedReportId === null ? (
                <p className="text-sm text-[var(--muted-foreground)]">Selecione um relatório para visualizar os eventos persistidos.</p>
              ) : selectedReportEvents.length === 0 ? (
                <p className="text-sm text-[var(--muted-foreground)]">Nenhum evento persistido encontrado para o relatório.</p>
              ) : (
                selectedReportEvents.map((event) => (
                  <article key={event.id} className="rounded-lg border border-[var(--border)] p-3">
                    <p className="text-sm font-medium">{event.eventName}</p>
                    <p className="mt-1 text-xs text-[var(--muted-foreground)]">
                      {event.eventTimestamp ? formatDate(event.eventTimestamp) : 'Sem timestamp'} · {event.eventCategory}
                    </p>
                    {event.metadata ? (
                      <pre className="mt-2 overflow-x-auto rounded bg-[var(--secondary)] p-2 text-xs">
{JSON.stringify(event.metadata, null, 2)}
                      </pre>
                    ) : null}
                  </article>
                ))
              )}
            </div>
          </div>
        </section>
      </div>
    </AppShell>
  );
}
