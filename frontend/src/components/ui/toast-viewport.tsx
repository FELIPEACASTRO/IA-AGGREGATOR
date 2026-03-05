'use client';

import { useToastStore } from '@/stores/toast-store';
import { cn } from '@/lib/cn';

const variantClass = {
  success: 'border-emerald-300 bg-emerald-50 text-emerald-800 dark:border-emerald-900/50 dark:bg-emerald-950/30 dark:text-emerald-300',
  error: 'border-red-300 bg-red-50 text-red-800 dark:border-red-900/50 dark:bg-red-950/30 dark:text-red-300',
  info: 'border-[var(--border)] bg-[var(--background)] text-[var(--foreground)]',
};

export function ToastViewport() {
  const { toasts, removeToast } = useToastStore();

  if (toasts.length === 0) return null;

  return (
    <div className="pointer-events-none fixed right-4 top-4 z-[100] flex w-full max-w-sm flex-col gap-2">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          role="status"
          aria-live="polite"
          className={cn(
            'pointer-events-auto rounded-xl border p-3 shadow-md backdrop-blur-sm',
            variantClass[toast.variant]
          )}
        >
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-sm font-semibold">{toast.title}</p>
              {toast.description ? (
                <p className="mt-1 text-xs opacity-90">{toast.description}</p>
              ) : null}
            </div>
            <button
              type="button"
              onClick={() => removeToast(toast.id)}
              className="text-xs opacity-70 hover:opacity-100"
              aria-label="Fechar notificação"
            >
              ✕
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
