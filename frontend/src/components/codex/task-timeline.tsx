'use client';

type TaskTimelineProps = {
  events: Array<{
    id: string;
    eventType: string;
    status?: string | null;
    message?: string | null;
    createdAt: string | Date;
  }>;
};

export function TaskTimeline({ events }: TaskTimelineProps) {
  return (
    <section className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4">
      <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Activity Timeline</h2>
      <div className="mt-3 space-y-3">
        {events.length === 0 && (
          <p className="text-sm text-[var(--muted-foreground)]">Sem eventos registrados.</p>
        )}
        {events.map((event) => (
          <div key={event.id} className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] px-3 py-2">
            <p className="text-xs font-semibold text-[var(--foreground)]">{event.eventType}</p>
            {event.message && <p className="text-xs text-[var(--muted-foreground)]">{event.message}</p>}
            <p className="mt-1 text-[0.68rem] text-[var(--subtle-foreground)]">
              {new Date(event.createdAt).toLocaleString('pt-BR')}
              {event.status ? ` • ${event.status}` : ''}
            </p>
          </div>
        ))}
      </div>
    </section>
  );
}

