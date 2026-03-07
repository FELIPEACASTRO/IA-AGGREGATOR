'use client';

import { useEffect, useState } from 'react';
import { useToastStore, ToastItem } from '@/stores/toast-store';
import { cn } from '@/lib/cn';
import { X, CheckCircle, AlertCircle, Info } from 'lucide-react';

const icons: Record<string, React.ElementType> = {
  success: CheckCircle,
  error: AlertCircle,
  info: Info,
};

const borderColor: Record<string, string> = {
  success: 'border-l-[var(--success)]',
  error: 'border-l-[var(--destructive)]',
  info: 'border-l-[var(--info)]',
};

const iconColor: Record<string, string> = {
  success: 'text-[var(--success)]',
  error: 'text-[var(--destructive)]',
  info: 'text-[var(--info)]',
};

function ToastItemComponent({ toast, onDismiss }: { toast: ToastItem; onDismiss: () => void }) {
  const Icon = icons[toast.variant];

  return (
    <div
      role="status"
      aria-live="polite"
      className={cn(
        'flex items-start gap-3 rounded-[var(--radius-lg)] border border-[var(--border)] border-l-4 bg-[var(--surface)] px-4 py-3 shadow-[var(--shadow-md)] fade-in',
        borderColor[toast.variant],
      )}
    >
      <Icon className={cn('mt-0.5 h-4 w-4 shrink-0', iconColor[toast.variant])} />
      <div className="flex-1 min-w-0">
        <p className="text-[13px] font-medium text-[var(--foreground)]">{toast.title}</p>
        {toast.description && (
          <p className="mt-0.5 text-[12px] text-[var(--muted-foreground)]">{toast.description}</p>
        )}
      </div>
      <button
        onClick={onDismiss}
        className="shrink-0 rounded-[var(--radius-sm)] p-0.5 text-[var(--muted-foreground)] hover:text-[var(--foreground)] transition-colors"
        aria-label="Fechar notificacao"
      >
        <X className="h-3.5 w-3.5" />
      </button>
    </div>
  );
}

export function ToastViewport() {
  const toasts = useToastStore((s) => s.toasts);
  const removeToast = useToastStore((s) => s.removeToast);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted || toasts.length === 0) return null;

  return (
    <div className="fixed bottom-4 right-4 z-[var(--z-toast)] flex flex-col gap-2 w-[360px] max-w-[calc(100vw-2rem)]">
      {toasts.map((t) => (
        <ToastItemComponent key={t.id} toast={t} onDismiss={() => removeToast(t.id)} />
      ))}
    </div>
  );
}
