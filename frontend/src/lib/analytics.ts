type AnalyticsEventName =
  | 'auth_login_success'
  | 'auth_login_error'
  | 'auth_register_success'
  | 'auth_register_error'
  | 'chat_send_start'
  | 'chat_send_success'
  | 'chat_send_error'
  | 'chat_stop_generation'
  | 'chat_retry'
  | 'chat_clear'
  | 'chat_copy_message'
  | 'library_open_conversation'
  | 'library_toggle_pin'
  | 'prompts_use_template'
  | 'settings_save_preferences';

export interface AnalyticsEvent {
  event: AnalyticsEventName;
  timestamp: string;
  metadata?: Record<string, string | number | boolean | null | undefined>;
}

const STORAGE_KEY = 'ia-aggregator-analytics-events';
const MAX_EVENTS = 300;

const nowIso = () => new Date().toISOString();

const readEvents = (): AnalyticsEvent[] => {
  if (typeof window === 'undefined') return [];
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as AnalyticsEvent[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

const writeEvents = (events: AnalyticsEvent[]) => {
  if (typeof window === 'undefined') return;
  localStorage.setItem(STORAGE_KEY, JSON.stringify(events.slice(-MAX_EVENTS)));
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

  const current = readEvents();
  current.push(item);
  writeEvents(current);

  if (process.env.NODE_ENV !== 'production') {
    // eslint-disable-next-line no-console
    console.info('[analytics]', item);
  }

  return item;
};

export const getTrackedEvents = (): AnalyticsEvent[] => readEvents();

export const clearTrackedEvents = () => {
  if (typeof window === 'undefined') return;
  localStorage.removeItem(STORAGE_KEY);
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
