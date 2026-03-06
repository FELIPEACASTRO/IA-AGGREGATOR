'use client';

import { useToastStore } from '@/stores/toast-store';
import { cn } from '@/lib/cn';

const variantClass = {
  success: 'border-[rgba(78,217,167,0.28)] bg-[rgba(78,217,167,0.12)] text-[var(--foreground)]',
  error: 'border-[rgba(255,107,135,0.28)] bg-[rgba(255,107,135,0.12)] text-[var(--foreground)]',
  info: 'border-[var(--border)] bg-[rgba(20,35,59,0.94)] text-[var(--foreground)]',
};

export function ToastViewport() {
  const { toasts, removeToast } = useToastStore();

  if (toasts.length === 0) return null;

  return (
    <div className="pointer-events-none fixed right-4 top-4 z-[var(--z-toast)] flex w-full max-w-sm flex-col gap-3">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          role="status"
          aria-live="polite"
          className={cn(
            'pointer-events-auto rounded-[var(--radius-lg)] border px-4 py-3 shadow-[var(--shadow-lg)] backdrop-blur-md',
            variantClass[toast.variant]
          )}
        >
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-[var(--text-sm)] font-semibold">{toast.title}</p>
              {toast.description ? <p className="mt-1 text-[0.74rem] text-[var(--muted-foreground)]">{toast.description}</p> : null}
            </div>
            <button
              type="button"
              onClick={() => removeToast(toast.id)}
              className="rounded-full p-1 text-[var(--muted-foreground)] hover:bg-[var(--surface-2)] hover:text-[var(--foreground)]"
              aria-label="Fechar notificacao"
            >
              x
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
