import api from '@/lib/api';

type AnalyticsEventName =
  | 'page_view'
  | 'auth_login_success'
  | 'auth_login_error'
  | 'auth_register_success'
  | 'auth_register_error'
  | 'onboarding_start'
  | 'onboarding_complete'
  | 'onboarding_skip'
  | 'chat_send_start'
  | 'chat_send_success'
  | 'chat_send_error'
  | 'chat_stop_generation'
  | 'chat_retry'
  | 'chat_clear'
  | 'chat_copy_message'
  | 'chat_change_model'
  | 'chat_feedback'
  | 'library_open_conversation'
  | 'library_toggle_pin'
  | 'prompts_use_template'
  | 'settings_save_preferences';

export interface AnalyticsEvent {
  event: AnalyticsEventName;
  timestamp: string;
  metadata?: Record<string, string | number | boolean | null | undefined>;
}

interface AnalyticsReportPayload {
  source: string;
  generatedAt: string;
  totalEvents: number;
  counters: Record<string, number>;
  events: AnalyticsEvent[];
}

const MAX_EVENTS = 300;
const FLUSH_INTERVAL_MS = 30000;

let isFlushing = false;
let lifecycleInitialized = false;
let trackedEvents: AnalyticsEvent[] = [];
let lastTrackedPath: string | null = null;

const nowIso = () => new Date().toISOString();

const buildCounters = (events: AnalyticsEvent[]): Record<string, number> => {
  return events.reduce<Record<string, number>>((acc, item) => {
    acc[item.event] = (acc[item.event] ?? 0) + 1;
    return acc;
  }, {});
};

export const trackEvent = (
  event: AnalyticsEventName,
  metadata?: AnalyticsEvent['metadata']
): AnalyticsEvent => {
  const item: AnalyticsEvent = {
    event,
    timestamp: nowIso(),
    metadata,
  };

  trackedEvents = [...trackedEvents, item].slice(-MAX_EVENTS);

  if (process.env.NODE_ENV !== 'production') {
    console.info('[analytics]', item);
  }

  return item;
};

export const getTrackedEvents = (): AnalyticsEvent[] => [...trackedEvents];

export const trackPageView = (pathname: string) => {
  if (typeof window === 'undefined') return;
  const normalized = pathname || '/';
  if (lastTrackedPath === normalized) return;
  lastTrackedPath = normalized;
  trackEvent('page_view', {
    pathname: normalized,
    referrer: document.referrer || null,
  });
};

export const flushTrackedEvents = async (source = 'frontend-web'): Promise<boolean> => {
  if (typeof window === 'undefined' || isFlushing) return false;

  const events = getTrackedEvents();
  if (events.length === 0) return true;

  isFlushing = true;
  try {
    const payload: AnalyticsReportPayload = {
      source,
      generatedAt: nowIso(),
      totalEvents: events.length,
      counters: buildCounters(events),
      events,
    };
    await api.post('/analytics/events', payload);
    trackedEvents = [];
    return true;
  } catch {
    return false;
  } finally {
    isFlushing = false;
  }
};

export const initializeAnalyticsLifecycle = () => {
  if (typeof window === 'undefined' || lifecycleInitialized) return;

  lifecycleInitialized = true;

  const flushOnPageHide = () => {
    if (trackedEvents.length === 0) return;

    const payload: AnalyticsReportPayload = {
      source: 'frontend-web-pagehide',
      generatedAt: nowIso(),
      totalEvents: trackedEvents.length,
      counters: buildCounters(trackedEvents),
      events: getTrackedEvents(),
    };

    if (typeof navigator.sendBeacon === 'function') {
      const body = new Blob([JSON.stringify(payload)], { type: 'application/json' });
      if (navigator.sendBeacon('/api/v1/analytics/events', body)) {
        trackedEvents = [];
        return;
      }
    }

    void flushTrackedEvents('frontend-web-pagehide');
  };

  const flushOnVisibilityHidden = () => {
    if (document.visibilityState === 'hidden') {
      void flushTrackedEvents('frontend-web-visibility');
    }
  };

  window.addEventListener('pagehide', flushOnPageHide);
  document.addEventListener('visibilitychange', flushOnVisibilityHidden);

  window.setInterval(() => {
    const queued = trackedEvents.length;
    if (queued >= 8) {
      void flushTrackedEvents('frontend-web-interval');
    }
  }, FLUSH_INTERVAL_MS);
};

export const clearTrackedEvents = () => {
  trackedEvents = [];
  lastTrackedPath = null;
};

export const createPerfTimer = () => {
  const startedAt = typeof performance !== 'undefined' ? performance.now() : Date.now();
  return {
    elapsedMs: () => {
      const current = typeof performance !== 'undefined' ? performance.now() : Date.now();
      return Math.round(current - startedAt);
    },
  };
};
