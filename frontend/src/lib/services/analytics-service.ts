import api from '@/lib/api';

export type AnalyticsPersistedReport = {
  id: string;
  source: string;
  generatedAt?: string | null;
  receivedAt?: string | null;
  totalEvents: number;
  counters?: Record<string, number>;
};

export type AnalyticsPersistedReportEvent = {
  id: string;
  reportId: string;
  eventName: string;
  eventCategory: string;
  eventTimestamp?: string | null;
  createdAt?: string | null;
  metadata?: Record<string, unknown>;
};

type ListReportsParams = {
  page?: number;
  limit?: number;
  from?: string | null;
  to?: string | null;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
};

type ListReportEventsParams = {
  offset?: number;
  limit?: number;
  category?: string;
};

const getBaseEventsEndpoint = () => {
  return process.env.NEXT_PUBLIC_ANALYTICS_ENDPOINT
    || (process.env.NEXT_PUBLIC_API_URL
      ? `${process.env.NEXT_PUBLIC_API_URL}/api/v1/analytics/events`
      : '/api/v1/analytics/events');
};

const withSuffix = (suffix: string) => {
  const base = getBaseEventsEndpoint();
  if (suffix === 'events') return base;
  return base.replace(/\/events$/, `/${suffix}`);
};

export const analyticsService = {
  resolveEndpoint(suffix: string) {
    return withSuffix(suffix);
  },

  async listReports(params: ListReportsParams = {}) {
    const qs = new URLSearchParams();
    qs.set('page', String(params.page ?? 0));
    qs.set('limit', String(params.limit ?? 20));
    if (params.from) qs.set('from', params.from);
    if (params.to) qs.set('to', params.to);
    if (params.sortBy) qs.set('sortBy', params.sortBy);
    if (params.sortDir) qs.set('sortDir', params.sortDir);

    const { data } = await api.get<{ data?: AnalyticsPersistedReport[] }>(
      `${withSuffix('reports')}?${qs.toString()}`
    );

    return Array.isArray(data?.data) ? data.data : [];
  },

  async listReportEvents(reportId: string, params: ListReportEventsParams = {}) {
    const qs = new URLSearchParams();
    qs.set('offset', String(params.offset ?? 0));
    qs.set('limit', String(params.limit ?? 50));
    if (params.category && params.category !== 'all') qs.set('category', params.category);

    const { data } = await api.get<{ data?: AnalyticsPersistedReportEvent[] }>(
      `${withSuffix(`reports/${reportId}/events`)}?${qs.toString()}`
    );

    return Array.isArray(data?.data) ? data.data : [];
  },
};

