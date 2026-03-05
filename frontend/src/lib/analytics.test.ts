import {
  clearTrackedEvents,
  createPerfTimer,
  getTrackedEvents,
  trackEvent,
} from '@/lib/analytics';

describe('analytics', () => {
  beforeEach(() => {
    clearTrackedEvents();
  });

  it('tracks events with metadata', () => {
    trackEvent('chat_send_start', { model: 'gpt-4o-mini' });

    const events = getTrackedEvents();
    expect(events).toHaveLength(1);
    expect(events[0].event).toBe('chat_send_start');
    expect(events[0].metadata?.model).toBe('gpt-4o-mini');
  });

  it('clears tracked events', () => {
    trackEvent('chat_retry');
    clearTrackedEvents();

    expect(getTrackedEvents()).toHaveLength(0);
  });

  it('returns elapsed milliseconds from timer', async () => {
    const timer = createPerfTimer();

    await new Promise((resolve) => setTimeout(resolve, 5));

    expect(timer.elapsedMs()).toBeGreaterThanOrEqual(0);
  });
});
