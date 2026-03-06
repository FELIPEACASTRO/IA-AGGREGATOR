'use client';

import { useEffect, useMemo, useState } from 'react';
import { AppShell } from '@/components/app/app-shell';
import { Button } from '@/components/ui/button';
import { clearTrackedEvents, flushTrackedEvents, getTrackedEvents, type AnalyticsEvent } from '@/lib/analytics';
import {
  analyticsService,
  type AnalyticsPersistedReport as PersistedReport,
  type AnalyticsPersistedReportEvent as PersistedReportEvent,
} from '@/lib/services/analytics-service';
import { toast } from '@/stores/toast-store';
import { Activity, Filter, LineChart, Radar } from 'lucide-react';

const formatDate = (value: string) => {
  try {
    return new Date(value).toLocaleString('pt-BR');
  } catch {
    return value;
  }
};

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

const COHORT_WINDOW_OPTIONS = [
  { value: 'all', label: 'Todas janelas' },
  { value: '2w', label: 'Últimas 2 semanas' },
  { value: '4w', label: 'Últimas 4 semanas' },
  { value: '8w', label: 'Últimas 8 semanas' },
  { value: '12w', label: 'Últimas 12 semanas' },
] as const;

const PERIOD_COMPARE_OPTIONS = [
  { value: 7, label: 'Últimos 7 dias' },
  { value: 14, label: 'Últimos 14 dias' },
  { value: 30, label: 'Últimos 30 dias' },
] as const;

const ANALYTICS_FILTERS_KEY = 'ia-analytics-filters-v1';

const sleep = (ms: number) => new Promise<void>((resolve) => {
  setTimeout(resolve, ms);
});

const csvValue = (value: unknown) => {
  if (value === null || value === undefined) return '""';
  const escaped = String(value).replace(/"/g, '""');
  return `"${escaped}"`;
};

type FunnelStep = {
  key: string;
  label: string;
  count: number;
  rate: number;
};

type RetentionTimelinePoint = {
  day: string;
  events: number;
};

type RetentionMetrics = {
  cohorts: number;
  eligibleD1: number;
  eligibleD7: number;
  d1Retained: number;
  d7Retained: number;
  d1Rate: number;
  d7Rate: number;
  timeline: RetentionTimelinePoint[];
};

type CohortWindow = (typeof COHORT_WINDOW_OPTIONS)[number]['value'];
type PeriodCompareWindow = (typeof PERIOD_COMPARE_OPTIONS)[number]['value'];

type PersistedAnalyticsFilters = {
  historyFromDate?: string;
  historyToDate?: string;
  historySortBy?: string;
  historySortDir?: 'asc' | 'desc';
  historySourceFilter?: string;
  selectedCohortWindow?: CohortWindow;
  periodCompareWindow?: PeriodCompareWindow;
  periodCompareSource?: string;
  compareSourceA?: string;
  compareSourceB?: string;
  selectedCategory?: string;
};

function SummaryTile({
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

const FUNNEL_DEFINITIONS = [
  { key: 'page_view', label: 'Page Views' },
  { key: 'onboarding_start', label: 'Onboarding Start' },
  { key: 'onboarding_complete', label: 'Onboarding Complete' },
  { key: 'chat_send_start', label: 'Primeiro Prompt' },
  { key: 'chat_send_success', label: 'Primeira Resposta Útil' },
];

const RETENTION_ACTIVITY_EVENTS = new Set([
  'page_view',
  'chat_send_start',
  'chat_send_success',
  'library_open_conversation',
  'prompts_use_template',
]);

const toIsoDay = (value?: string | null): string | null => {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return date.toISOString().slice(0, 10);
};

const addDays = (isoDay: string, offset: number) => {
  const date = new Date(`${isoDay}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + offset);
  return date.toISOString().slice(0, 10);
};

const cohortWindowDays = (window: CohortWindow): number | null => {
  if (window === '2w') return 14;
  if (window === '4w') return 28;
  if (window === '8w') return 56;
  if (window === '12w') return 84;
  return null;
};

const buildFunnel = (counterMap: Record<string, number>): FunnelStep[] => {
  let previous = 0;
  return FUNNEL_DEFINITIONS.map((step, index) => {
    const count = counterMap[step.key] ?? 0;
    const rate = index === 0
      ? 100
      : previous > 0
        ? Math.round((count / previous) * 100)
        : 0;
    previous = count;
    return {
      key: step.key,
      label: step.label,
      count,
      rate,
    };
  });
};

const aggregateEventCounters = (items: PersistedReportEvent[]) => {
  return items.reduce<Record<string, number>>((acc, item) => {
    acc[item.eventName] = (acc[item.eventName] ?? 0) + 1;
    return acc;
  }, {});
};

const aggregateReportCounters = (reports: PersistedReport[]) => {
  return reports.reduce<Record<string, number>>((acc, report) => {
    const counters = report.counters ?? {};
    Object.entries(counters).forEach(([key, value]) => {
      acc[key] = (acc[key] ?? 0) + (value ?? 0);
    });
    return acc;
  }, {});
};

const reportTimestampMs = (report: PersistedReport): number | null => {
  const value = report.receivedAt ?? report.generatedAt;
  if (!value) return null;
  const parsed = new Date(value).getTime();
  return Number.isNaN(parsed) ? null : parsed;
};

const buildRetentionMetrics = (
  items: Array<{ eventName: string; timestamp?: string | null }>,
  options?: { windowDays?: number | null }
): RetentionMetrics => {
  const dayTotals = new Map<string, number>();
  const dayHasActivity = new Map<string, boolean>();
  const onboardingDays = new Set<string>();
  const pageViewDays = new Set<string>();

  items.forEach((item) => {
    const day = toIsoDay(item.timestamp);
    if (!day) return;

    dayTotals.set(day, (dayTotals.get(day) ?? 0) + 1);
    if (item.eventName === 'onboarding_complete') onboardingDays.add(day);
    if (item.eventName === 'page_view') pageViewDays.add(day);
    if (RETENTION_ACTIVITY_EVENTS.has(item.eventName)) {
      dayHasActivity.set(day, true);
    }
  });

  const days = [...dayTotals.keys()].sort((a, b) => a.localeCompare(b));
  if (days.length === 0) {
    return {
      cohorts: 0,
      eligibleD1: 0,
      eligibleD7: 0,
      d1Retained: 0,
      d7Retained: 0,
      d1Rate: 0,
      d7Rate: 0,
      timeline: [],
    };
  }

  const maxDay = days[days.length - 1];
  const cutoffDay = options?.windowDays
    ? addDays(maxDay, -(options.windowDays - 1))
    : null;
  const cohortDays = [...(onboardingDays.size > 0 ? onboardingDays : pageViewDays)]
    .filter((day) => (cutoffDay ? day >= cutoffDay : true))
    .sort((a, b) => a.localeCompare(b));

  let eligibleD1 = 0;
  let eligibleD7 = 0;
  let d1Retained = 0;
  let d7Retained = 0;

  cohortDays.forEach((cohortDay) => {
    const day1 = addDays(cohortDay, 1);
    const day7 = addDays(cohortDay, 7);

    if (day1 <= maxDay) {
      eligibleD1 += 1;
      if (dayHasActivity.get(day1)) d1Retained += 1;
    }
    if (day7 <= maxDay) {
      eligibleD7 += 1;
      if (dayHasActivity.get(day7)) d7Retained += 1;
    }
  });

  const timelineBase = cutoffDay ? days.filter((day) => day >= cutoffDay) : days;
  const timeline = timelineBase.slice(-14).map((day) => ({
    day,
    events: dayTotals.get(day) ?? 0,
  }));

  return {
    cohorts: cohortDays.length,
    eligibleD1,
    eligibleD7,
    d1Retained,
    d7Retained,
    d1Rate: eligibleD1 > 0 ? Math.round((d1Retained / eligibleD1) * 100) : 0,
    d7Rate: eligibleD7 > 0 ? Math.round((d7Retained / eligibleD7) * 100) : 0,
    timeline,
  };
};

export default function AnalyticsDiagnosticsPage() {
  const [events, setEvents] = useState<AnalyticsEvent[]>([]);
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
  const [historySourceFilter, setHistorySourceFilter] = useState('all');
  const [selectedCohortWindow, setSelectedCohortWindow] = useState<CohortWindow>('4w');
  const [periodCompareWindow, setPeriodCompareWindow] = useState<PeriodCompareWindow>(7);
  const [periodCompareSource, setPeriodCompareSource] = useState('all');
  const [compareSourceA, setCompareSourceA] = useState('');
  const [compareSourceB, setCompareSourceB] = useState('');
  const [selectedReportId, setSelectedReportId] = useState<string | null>(null);
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [selectedReportEvents, setSelectedReportEvents] = useState<PersistedReportEvent[]>([]);
  const [selectedReportOffset, setSelectedReportOffset] = useState(0);
  const [hasMoreReportEvents, setHasMoreReportEvents] = useState(false);
  const [settingsHydrated, setSettingsHydrated] = useState(false);

  useEffect(() => {
    setEvents(getTrackedEvents());
  }, []);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    try {
      const raw = localStorage.getItem(ANALYTICS_FILTERS_KEY);
      if (!raw) {
        setSettingsHydrated(true);
        return;
      }

      const parsed = JSON.parse(raw) as PersistedAnalyticsFilters;

      if (typeof parsed.historyFromDate === 'string') setHistoryFromDate(parsed.historyFromDate);
      if (typeof parsed.historyToDate === 'string') setHistoryToDate(parsed.historyToDate);

      if (REPORT_SORT_OPTIONS.some((option) => option.value === parsed.historySortBy)) {
        setHistorySortBy(parsed.historySortBy as string);
      }

      if (parsed.historySortDir === 'asc' || parsed.historySortDir === 'desc') {
        setHistorySortDir(parsed.historySortDir);
      }

      if (typeof parsed.historySourceFilter === 'string') setHistorySourceFilter(parsed.historySourceFilter);
      if (typeof parsed.periodCompareSource === 'string') setPeriodCompareSource(parsed.periodCompareSource);
      if (typeof parsed.compareSourceA === 'string') setCompareSourceA(parsed.compareSourceA);
      if (typeof parsed.compareSourceB === 'string') setCompareSourceB(parsed.compareSourceB);

      if (COHORT_WINDOW_OPTIONS.some((option) => option.value === parsed.selectedCohortWindow)) {
        setSelectedCohortWindow(parsed.selectedCohortWindow as CohortWindow);
      }

      if (PERIOD_COMPARE_OPTIONS.some((option) => option.value === parsed.periodCompareWindow)) {
        setPeriodCompareWindow(parsed.periodCompareWindow as PeriodCompareWindow);
      }

      if (EVENT_CATEGORIES.some((option) => option.value === parsed.selectedCategory)) {
        setSelectedCategory(parsed.selectedCategory as string);
      }
    } catch {
      // ignore malformed persisted filters
    } finally {
      setSettingsHydrated(true);
    }
  }, []);

  useEffect(() => {
    if (!settingsHydrated || typeof window === 'undefined') return;

    const payload: PersistedAnalyticsFilters = {
      historyFromDate,
      historyToDate,
      historySortBy,
      historySortDir,
      historySourceFilter,
      selectedCohortWindow,
      periodCompareWindow,
      periodCompareSource,
      compareSourceA,
      compareSourceB,
      selectedCategory,
    };

    localStorage.setItem(ANALYTICS_FILTERS_KEY, JSON.stringify(payload));
  }, [
    settingsHydrated,
    historyFromDate,
    historyToDate,
    historySortBy,
    historySortDir,
    historySourceFilter,
    selectedCohortWindow,
    periodCompareWindow,
    periodCompareSource,
    compareSourceA,
    compareSourceB,
    selectedCategory,
  ]);

  const counters = useMemo(() => {
    const counts: Record<string, number> = {};
    events.forEach((event) => {
      counts[event.event] = (counts[event.event] || 0) + 1;
    });
    return counts;
  }, [events]);

  const localFunnel = useMemo(() => buildFunnel(counters), [counters]);

  const availableSources = useMemo(
    () => ['all', ...new Set(persistedReports.map((report) => report.source))],
    [persistedReports]
  );

  const sourceOptions = useMemo(
    () => [...new Set(persistedReports.map((report) => report.source))],
    [persistedReports]
  );

  useEffect(() => {
    if (sourceOptions.length === 0) {
      setCompareSourceA('');
      setCompareSourceB('');
      return;
    }

    if (!compareSourceA || !sourceOptions.includes(compareSourceA)) {
      setCompareSourceA(sourceOptions[0]);
    }

    if (!compareSourceB || !sourceOptions.includes(compareSourceB) || compareSourceB === compareSourceA) {
      const fallback = sourceOptions.find((source) => source !== (compareSourceA || sourceOptions[0])) ?? sourceOptions[0];
      setCompareSourceB(fallback);
    }
  }, [sourceOptions, compareSourceA, compareSourceB]);

  const visiblePersistedReports = useMemo(
    () => (historySourceFilter === 'all'
      ? persistedReports
      : persistedReports.filter((report) => report.source === historySourceFilter)),
    [persistedReports, historySourceFilter]
  );

  const persistedCounters = useMemo(
    () => aggregateEventCounters(selectedReportEvents),
    [selectedReportEvents]
  );

  const persistedFunnel = useMemo(
    () => buildFunnel(persistedCounters),
    [persistedCounters]
  );

  const sourceCountersA = useMemo(
    () => aggregateReportCounters(persistedReports.filter((report) => report.source === compareSourceA)),
    [persistedReports, compareSourceA]
  );

  const sourceCountersB = useMemo(
    () => aggregateReportCounters(persistedReports.filter((report) => report.source === compareSourceB)),
    [persistedReports, compareSourceB]
  );

  const sourceFunnelA = useMemo(() => buildFunnel(sourceCountersA), [sourceCountersA]);
  const sourceFunnelB = useMemo(() => buildFunnel(sourceCountersB), [sourceCountersB]);

  const periodComparison = useMemo(() => {
    const sourceScoped = periodCompareSource === 'all'
      ? persistedReports
      : persistedReports.filter((report) => report.source === periodCompareSource);

    const nowMs = Date.now();
    const dayMs = 24 * 60 * 60 * 1000;
    const currentStartMs = nowMs - (periodCompareWindow * dayMs);
    const previousStartMs = currentStartMs - (periodCompareWindow * dayMs);

    const inCurrent: PersistedReport[] = [];
    const inPrevious: PersistedReport[] = [];

    sourceScoped.forEach((report) => {
      const ts = reportTimestampMs(report);
      if (ts === null) return;
      if (ts >= currentStartMs && ts <= nowMs) {
        inCurrent.push(report);
      } else if (ts >= previousStartMs && ts < currentStartMs) {
        inPrevious.push(report);
      }
    });

    const currentCounters = aggregateReportCounters(inCurrent);
    const previousCounters = aggregateReportCounters(inPrevious);

    return {
      currentReports: inCurrent.length,
      previousReports: inPrevious.length,
      currentFunnel: buildFunnel(currentCounters),
      previousFunnel: buildFunnel(previousCounters),
    };
  }, [persistedReports, periodCompareSource, periodCompareWindow]);

  const localActivationRate = useMemo(() => {
    const started = counters.onboarding_start ?? 0;
    const completed = counters.onboarding_complete ?? 0;
    if (started === 0) return 0;
    return Math.round((completed / started) * 100);
  }, [counters]);

  const localRetention = useMemo(
    () => buildRetentionMetrics(events.map((event) => ({
      eventName: event.event,
      timestamp: event.timestamp,
    })), { windowDays: cohortWindowDays(selectedCohortWindow) }),
    [events, selectedCohortWindow]
  );

  const persistedRetention = useMemo(
    () => buildRetentionMetrics(selectedReportEvents.map((event) => ({
      eventName: event.eventName,
      timestamp: event.eventTimestamp ?? event.createdAt,
    })), { windowDays: cohortWindowDays(selectedCohortWindow) }),
    [selectedReportEvents, selectedCohortWindow]
  );

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
    if (visiblePersistedReports.length === 0) return;

    const header = ['id', 'source', 'generatedAt', 'receivedAt', 'totalEvents', 'counters'];
    const rows = visiblePersistedReports.map((report) => [
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
    toast.success('CSV exportado', `${visiblePersistedReports.length} relatório(s) exportado(s).`);
  };

  const sendReport = async () => {
    if (events.length === 0 || isSendingReport) return;

    setIsSendingReport(true);

    try {
      let delivered = false;
      for (let attempt = 1; attempt <= REPORT_MAX_ATTEMPTS; attempt += 1) {
        const ok = await flushTrackedEvents(`frontend-manual-${attempt}`);
        if (ok) {
          delivered = true;
          break;
        }
        const backoffMs = REPORT_BASE_DELAY_MS * 2 ** (attempt - 1);
        await sleep(backoffMs);
      }

      if (!delivered) {
        throw new Error('Failed to send analytics report');
      }

      setEvents(getTrackedEvents());
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
      const reports = await analyticsService.listReports({
        limit: REPORT_HISTORY_PAGE_SIZE,
        page,
        sortBy: historySortBy,
        sortDir: historySortDir,
        from,
        to,
      });
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
      const reportEvents = await analyticsService.listReportEvents(reportId, {
        limit: REPORT_EVENT_PAGE_SIZE,
        offset,
        category,
      });
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

  const activeFilterCount = useMemo(() => {
    let count = 0;
    if (historyFromDate) count += 1;
    if (historyToDate) count += 1;
    if (historySortBy !== 'receivedAt') count += 1;
    if (historySortDir !== 'desc') count += 1;
    if (historySourceFilter !== 'all') count += 1;
    if (selectedCohortWindow !== '4w') count += 1;
    if (periodCompareWindow !== 7) count += 1;
    if (periodCompareSource !== 'all') count += 1;
    if (selectedCategory !== 'all') count += 1;
    return count;
  }, [
    historyFromDate,
    historyToDate,
    historySortBy,
    historySortDir,
    historySourceFilter,
    selectedCohortWindow,
    periodCompareWindow,
    periodCompareSource,
    selectedCategory,
  ]);

  const resetFilters = () => {
    setHistoryFromDate('');
    setHistoryToDate('');
    setHistorySortBy('receivedAt');
    setHistorySortDir('desc');
    setHistorySourceFilter('all');
    setSelectedCohortWindow('4w');
    setPeriodCompareWindow(7);
    setPeriodCompareSource('all');
    setCompareSourceA('');
    setCompareSourceB('');
    setSelectedCategory('all');
    setSelectedReportOffset(0);
    setHasMoreReportEvents(false);
    toast.info('Filtros resetados', 'Os filtros de analytics voltaram ao padrão.');
  };

  const summaryCards = [
    {
      label: 'Eventos locais',
      value: events.length.toLocaleString('pt-BR'),
      helper: 'capturados no browser atual',
      icon: Activity,
    },
    {
      label: 'Ativacao',
      value: `${localActivationRate}%`,
      helper: 'conclusao do onboarding local',
      icon: Radar,
    },
    {
      label: 'Retencao D7',
      value: `${localRetention.d7Rate}%`,
      helper: `${localRetention.d7Retained}/${localRetention.eligibleD7} coortes elegiveis`,
      icon: LineChart,
    },
    {
      label: 'Filtros ativos',
      value: String(activeFilterCount),
      helper: `${persistedReports.length} relatorios em memoria`,
      icon: Filter,
    },
  ];

  return (

    <AppShell
      title="Analytics"
      subtitle="Diagnóstico local de eventos UX para Product/Data"
      headerActions={
        <div className="flex flex-wrap items-center gap-2">
          {activeFilterCount > 0 && (
            <span className="inline-flex items-center rounded-full border border-[var(--brand-primary)]/40 bg-[var(--brand-primary)]/10 px-2.5 py-1 text-[10px] font-semibold text-[var(--brand-primary)]">
              {activeFilterCount} filtro(s) ativo(s)
            </span>
          )}
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
          <Button variant="ghost" onClick={resetFilters} disabled={activeFilterCount === 0}>
            Resetar filtros
          </Button>
          <Button variant="destructive" onClick={clearAll} disabled={events.length === 0}>
            Limpar eventos
          </Button>
        </div>
      }
    >
      <div className="space-y-4 py-6">
        <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {summaryCards.map((card) => (
            <SummaryTile
              key={card.label}
              label={card.label}
              value={card.value}
              helper={card.helper}
              icon={card.icon}
            />
          ))}
        </section>

        <div className="grid gap-4 lg:grid-cols-[320px_1fr]">
        <aside className="lume-panel rounded-[var(--radius-2xl)] p-4">
          <p className="text-sm font-semibold">Resumo</p>
          <p className="mt-1 text-xs text-[var(--muted-foreground)]">
            Total de eventos: {events.length}
          </p>
          <p className="mt-1 text-xs text-[var(--muted-foreground)]">
            Ativação onboarding: {localActivationRate}%
          </p>
          <p className="mt-1 text-xs text-[var(--muted-foreground)]">
            Janela de coorte: {COHORT_WINDOW_OPTIONS.find((item) => item.value === selectedCohortWindow)?.label}
          </p>
          <div className="mt-3 space-y-2">
            <p className="text-xs font-semibold uppercase tracking-wide text-[var(--muted-foreground)]">Funil local</p>
            {localFunnel.map((step, index) => (
              <div key={step.key} className="rounded-[12px] border border-[var(--border)] bg-[var(--secondary)] px-3 py-2">
                <div className="flex items-center justify-between gap-2">
                  <p className="truncate text-xs font-medium">{index + 1}. {step.label}</p>
                  <span className="text-xs font-semibold tabular-nums">{step.count}</span>
                </div>
                <p className="text-[11px] text-[var(--muted-foreground)]">Conversão da etapa anterior: {step.rate}%</p>
              </div>
            ))}
          </div>
          <div className="mt-3 rounded-[12px] border border-[var(--border)] bg-[var(--secondary)] p-3">
            <p className="text-xs font-semibold uppercase tracking-wide text-[var(--muted-foreground)]">Retenção local (proxy)</p>
            <div className="mt-2 grid grid-cols-2 gap-2">
              <div className="rounded-[10px] border border-[var(--border)] bg-[var(--background)] px-2.5 py-2">
                <p className="text-[11px] text-[var(--muted-foreground)]">D1</p>
                <p className="text-sm font-semibold tabular-nums">{localRetention.d1Rate}%</p>
                <p className="text-[10px] text-[var(--muted-foreground)]">{localRetention.d1Retained}/{localRetention.eligibleD1} coortes elegíveis</p>
              </div>
              <div className="rounded-[10px] border border-[var(--border)] bg-[var(--background)] px-2.5 py-2">
                <p className="text-[11px] text-[var(--muted-foreground)]">D7</p>
                <p className="text-sm font-semibold tabular-nums">{localRetention.d7Rate}%</p>
                <p className="text-[10px] text-[var(--muted-foreground)]">{localRetention.d7Retained}/{localRetention.eligibleD7} coortes elegíveis</p>
              </div>
            </div>
            <p className="mt-2 text-[10px] text-[var(--muted-foreground)]">Base de coortes: {localRetention.cohorts} dia(s) com onboarding/page view.</p>
            {localRetention.timeline.length > 0 && (
              <div className="mt-2">
                <div className="flex h-12 items-end gap-1">
                  {localRetention.timeline.map((point) => {
                    const max = Math.max(...localRetention.timeline.map((item) => item.events), 1);
                    const height = Math.max(4, Math.round((point.events / max) * 40));
                    return (

                      <div key={point.day} className="group flex-1">
                        <div className="w-full rounded-t-sm bg-[var(--brand-primary)]/60" style={{ height: `${height}px` }} title={`${point.day}: ${point.events}`} />
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
          <div className="mt-3 space-y-2">
            {Object.keys(counters).length === 0 ? (
              <p className="text-sm text-[var(--muted-foreground)]">Sem eventos capturados.</p>
            ) : (
              Object.entries(counters).map(([name, count]) => (
                <div key={name} className="rounded-[12px] border border-[var(--border)] bg-[var(--secondary)] px-3 py-2">
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
                  className="rounded-[12px] border border-[var(--border)] bg-[var(--background)] px-2 py-1 text-xs transition-[all] duration-200 ease-[var(--ease-standard)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
                />
                <input
                  type="date"
                  aria-label="Data final do histórico"
                  value={historyToDate}
                  onChange={(event) => setHistoryToDate(event.target.value)}
                  className="rounded-[12px] border border-[var(--border)] bg-[var(--background)] px-2 py-1 text-xs transition-[all] duration-200 ease-[var(--ease-standard)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
                />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <select
                  aria-label="Ordenar histórico por"
                  value={historySortBy}
                  onChange={(event) => setHistorySortBy(event.target.value)}
                  className="rounded-[12px] border border-[var(--border)] bg-[var(--background)] px-2 py-1 text-xs transition-[all] duration-200 ease-[var(--ease-standard)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
                >
                  {REPORT_SORT_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
                <select
                  aria-label="Direção da ordenação do histórico"
                  value={historySortDir}
                  onChange={(event) => setHistorySortDir(event.target.value as 'asc' | 'desc')}
                  className="rounded-[12px] border border-[var(--border)] bg-[var(--background)] px-2 py-1 text-xs transition-[all] duration-200 ease-[var(--ease-standard)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
                >
                  <option value="desc">Descendente</option>
                  <option value="asc">Ascendente</option>
                </select>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <select
                  aria-label="Filtrar relatórios por fonte"
                  value={historySourceFilter}
                  onChange={(event) => setHistorySourceFilter(event.target.value)}
                  className="rounded-[12px] border border-[var(--border)] bg-[var(--background)] px-2 py-1 text-xs transition-[all] duration-200 ease-[var(--ease-standard)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
                >
                  {availableSources.map((source) => (
                    <option key={source} value={source}>{source === 'all' ? 'Todas as fontes' : source}</option>
                  ))}
                </select>
                <select
                  aria-label="Janela de coorte"
                  value={selectedCohortWindow}
                  onChange={(event) => setSelectedCohortWindow(event.target.value as CohortWindow)}
                  className="rounded-[12px] border border-[var(--border)] bg-[var(--background)] px-2 py-1 text-xs transition-[all] duration-200 ease-[var(--ease-standard)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
                >
                  {COHORT_WINDOW_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
              </div>
              <Button
                variant="ghost"
                className="w-full text-xs"
                onClick={exportPersistedReportsCsv}
                disabled={visiblePersistedReports.length === 0}
              >
                Exportar histórico CSV
              </Button>
            </div>
            <div className="mt-2 space-y-2">
              {visiblePersistedReports.length === 0 ? (
                <p className="text-xs text-[var(--muted-foreground)]">Nenhum histórico carregado.</p>
              ) : (
                <>
                  {visiblePersistedReports.map((report) => (
                    <div key={report.id} className="rounded-[12px] border border-[var(--border)] bg-[var(--secondary)] px-3 py-2">
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

        <section className="lume-panel rounded-[var(--radius-2xl)] p-4">
          <p className="text-sm font-semibold">Eventos</p>
          <div className="mt-3 max-h-[520px] space-y-2 overflow-y-auto pr-1">
            {events.length === 0 ? (
              <p className="text-sm text-[var(--muted-foreground)]">
                Nenhum evento encontrado. Interaja com o sistema e clique em Atualizar.
              </p>
            ) : (
              [...events].reverse().map((event, index) => (
                <article key={`${event.timestamp}-${index}`} className="rounded-[12px] border border-[var(--border)] p-3">
                  <p className="text-sm font-medium">{event.event}</p>
                  <p className="mt-1 text-xs text-[var(--muted-foreground)]">{formatDate(event.timestamp)}</p>
                  {event.metadata ? (
                    <pre className="mt-2 overflow-x-auto rounded-[10px] bg-[var(--secondary)] p-2 text-xs">
{JSON.stringify(event.metadata, null, 2)}
                    </pre>
                  ) : null}
                </article>
              ))
            )}
          </div>

          <div className="mt-5 border-t border-[var(--border)] pt-4">
            <p className="text-sm font-semibold">Detalhes do relatório persistido</p>
            <div className="mt-3 rounded-[12px] border border-[var(--border)] bg-[var(--secondary)] p-3">
              <p className="text-xs font-semibold uppercase tracking-wide text-[var(--muted-foreground)]">Comparação por fonte (funil agregado)</p>
              <div className="mt-2 grid gap-2 sm:grid-cols-2">
                <select
                  aria-label="Fonte A"
                  value={compareSourceA}
                  onChange={(event) => setCompareSourceA(event.target.value)}
                  className="rounded-[12px] border border-[var(--border)] bg-[var(--background)] px-2 py-1 text-xs transition-[all] duration-200 ease-[var(--ease-standard)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
                >
                  {sourceOptions.map((source) => (
                    <option key={`a-${source}`} value={source}>{source || 'Fonte A'}</option>
                  ))}
                </select>
                <select
                  aria-label="Fonte B"
                  value={compareSourceB}
                  onChange={(event) => setCompareSourceB(event.target.value)}
                  className="rounded-[12px] border border-[var(--border)] bg-[var(--background)] px-2 py-1 text-xs transition-[all] duration-200 ease-[var(--ease-standard)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
                >
                  {sourceOptions.map((source) => (
                    <option key={`b-${source}`} value={source}>{source || 'Fonte B'}</option>
                  ))}
                </select>
              </div>
              {sourceOptions.length < 2 ? (
                <p className="mt-2 text-xs text-[var(--muted-foreground)]">Carregue pelo menos duas fontes no histórico para comparar.</p>
              ) : (
                <div className="mt-3 space-y-2">
                  {FUNNEL_DEFINITIONS.map((step, index) => {
                    const a = sourceFunnelA[index];
                    const b = sourceFunnelB[index];
                    const deltaCount = (a?.count ?? 0) - (b?.count ?? 0);
                    const deltaRate = (a?.rate ?? 0) - (b?.rate ?? 0);
                    return (

                      <div key={`cmp-${step.key}`} className="rounded-[10px] border border-[var(--border)] bg-[var(--background)] px-2.5 py-2">
                        <div className="flex items-center justify-between gap-2">
                          <p className="truncate text-xs font-medium">{index + 1}. {step.label}</p>
                          <span className="text-[11px] text-[var(--muted-foreground)]">
                            Δ {deltaCount >= 0 ? '+' : ''}{deltaCount}
                          </span>
                        </div>
                        <p className="mt-0.5 text-[11px] text-[var(--muted-foreground)]">
                          {compareSourceA}: {a?.count ?? 0} ({a?.rate ?? 0}%) · {compareSourceB}: {b?.count ?? 0} ({b?.rate ?? 0}%)
                        </p>
                        <p className={`text-[11px] ${deltaRate >= 0 ? 'text-[var(--success)]' : 'text-[var(--destructive)]'}`}>
                          Δ conversão: {deltaRate >= 0 ? '+' : ''}{deltaRate} p.p.
                        </p>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
            <div className="mt-3 rounded-[12px] border border-[var(--border)] bg-[var(--secondary)] p-3">
              <p className="text-xs font-semibold uppercase tracking-wide text-[var(--muted-foreground)]">Comparação temporal (período atual vs anterior)</p>
              <div className="mt-2 grid gap-2 sm:grid-cols-2">
                <select
                  aria-label="Janela da comparação temporal"
                  value={String(periodCompareWindow)}
                  onChange={(event) => setPeriodCompareWindow(Number(event.target.value) as PeriodCompareWindow)}
                  className="rounded-[12px] border border-[var(--border)] bg-[var(--background)] px-2 py-1 text-xs transition-[all] duration-200 ease-[var(--ease-standard)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
                >
                  {PERIOD_COMPARE_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
                <select
                  aria-label="Fonte para comparação temporal"
                  value={periodCompareSource}
                  onChange={(event) => setPeriodCompareSource(event.target.value)}
                  className="rounded-[12px] border border-[var(--border)] bg-[var(--background)] px-2 py-1 text-xs transition-[all] duration-200 ease-[var(--ease-standard)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
                >
                  {availableSources.map((source) => (
                    <option key={`period-${source}`} value={source}>{source === 'all' ? 'Todas as fontes' : source}</option>
                  ))}
                </select>
              </div>
              <p className="mt-2 text-[11px] text-[var(--muted-foreground)]">
                Relatórios no período atual: {periodComparison.currentReports} · período anterior: {periodComparison.previousReports}
              </p>
              <div className="mt-3 space-y-2">
                {FUNNEL_DEFINITIONS.map((step, index) => {
                  const current = periodComparison.currentFunnel[index];
                  const previous = periodComparison.previousFunnel[index];
                  const deltaCount = (current?.count ?? 0) - (previous?.count ?? 0);
                  const deltaRate = (current?.rate ?? 0) - (previous?.rate ?? 0);
                  return (

                    <div key={`period-${step.key}`} className="rounded-[10px] border border-[var(--border)] bg-[var(--background)] px-2.5 py-2">
                      <div className="flex items-center justify-between gap-2">
                        <p className="truncate text-xs font-medium">{index + 1}. {step.label}</p>
                        <span className={`text-[11px] ${deltaCount >= 0 ? 'text-[var(--success)]' : 'text-[var(--destructive)]'}`}>
                          Δ {deltaCount >= 0 ? '+' : ''}{deltaCount}
                        </span>
                      </div>
                      <p className="mt-0.5 text-[11px] text-[var(--muted-foreground)]">
                        Atual: {current?.count ?? 0} ({current?.rate ?? 0}%) · Anterior: {previous?.count ?? 0} ({previous?.rate ?? 0}%)
                      </p>
                      <p className={`text-[11px] ${deltaRate >= 0 ? 'text-[var(--success)]' : 'text-[var(--destructive)]'}`}>
                        Δ conversão: {deltaRate >= 0 ? '+' : ''}{deltaRate} p.p.
                      </p>
                    </div>
                  );
                })}
              </div>
            </div>
            {selectedReportId ? (
              <div className="mt-3 rounded-[12px] border border-[var(--border)] bg-[var(--secondary)] p-3">
                <p className="text-xs font-semibold uppercase tracking-wide text-[var(--muted-foreground)]">Funil persistido (relatório selecionado)</p>
                <div className="mt-2 grid gap-2 sm:grid-cols-2">
                  {persistedFunnel.map((step, index) => (
                    <div key={step.key} className="rounded-[10px] border border-[var(--border)] bg-[var(--background)] px-2.5 py-2">
                      <div className="flex items-center justify-between gap-2">
                        <p className="truncate text-xs font-medium">{index + 1}. {step.label}</p>
                        <span className="text-xs font-semibold tabular-nums">{step.count}</span>
                      </div>
                      <p className="text-[11px] text-[var(--muted-foreground)]">Conversão: {step.rate}%</p>
                    </div>
                  ))}
                </div>
                <div className="mt-3 grid grid-cols-2 gap-2">
                  <div className="rounded-[10px] border border-[var(--border)] bg-[var(--background)] px-2.5 py-2">
                    <p className="text-[11px] text-[var(--muted-foreground)]">D1</p>
                    <p className="text-sm font-semibold tabular-nums">{persistedRetention.d1Rate}%</p>
                    <p className="text-[10px] text-[var(--muted-foreground)]">{persistedRetention.d1Retained}/{persistedRetention.eligibleD1}</p>
                  </div>
                  <div className="rounded-[10px] border border-[var(--border)] bg-[var(--background)] px-2.5 py-2">
                    <p className="text-[11px] text-[var(--muted-foreground)]">D7</p>
                    <p className="text-sm font-semibold tabular-nums">{persistedRetention.d7Rate}%</p>
                    <p className="text-[10px] text-[var(--muted-foreground)]">{persistedRetention.d7Retained}/{persistedRetention.eligibleD7}</p>
                  </div>
                </div>
                <p className="mt-2 text-[10px] text-[var(--muted-foreground)]">Base de coortes: {persistedRetention.cohorts} dia(s).</p>
              </div>
            ) : null}
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
                  className="rounded-[12px] border border-[var(--border)] bg-[var(--background)] px-2 py-1 text-xs transition-[all] duration-200 ease-[var(--ease-standard)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]"
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
                  <article key={event.id} className="rounded-[12px] border border-[var(--border)] p-3">
                    <p className="text-sm font-medium">{event.eventName}</p>
                    <p className="mt-1 text-xs text-[var(--muted-foreground)]">
                      {event.eventTimestamp ? formatDate(event.eventTimestamp) : 'Sem timestamp'} · {event.eventCategory}
                    </p>
                    {event.metadata ? (
                      <pre className="mt-2 overflow-x-auto rounded-[10px] bg-[var(--secondary)] p-2 text-xs">
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
      </div>
    </AppShell>
  );
}



