'use client';

import { useEffect, useCallback, ReactNode } from 'react';
import { X } from 'lucide-react';
import { cn } from '@/lib/cn';

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title?: string;
  description?: string;
  children: ReactNode;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
}

const sizeWidths = {
  sm: 'max-w-sm',
  md: 'max-w-md',
  lg: 'max-w-lg',
  xl: 'max-w-2xl',
};

export function Modal({ open, onClose, title, description, children, size = 'md', className }: ModalProps) {
  const handleEscape = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    },
    [onClose],
  );

  useEffect(() => {
    if (!open) return;
    document.addEventListener('keydown', handleEscape);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.body.style.overflow = '';
    };
  }, [open, handleEscape]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[var(--z-modal)] flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} aria-hidden />
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className={cn(
          'relative w-full rounded-[var(--radius-xl)] border border-[var(--border)] bg-[var(--surface)] shadow-[var(--shadow-lg)] fade-in',
          sizeWidths[size],
          className,
        )}
      >
        {(title || description) && (
          <div className="border-b border-[var(--border)] px-6 py-4">
            {title && <h2 className="text-[16px] font-semibold text-[var(--foreground)]">{title}</h2>}
            {description && <p className="mt-1 text-[13px] text-[var(--muted-foreground)]">{description}</p>}
          </div>
        )}
        <div className="p-6">{children}</div>
        <button
          onClick={onClose}
          className="absolute right-3 top-3 inline-flex h-8 w-8 items-center justify-center rounded-[var(--radius-md)] text-[var(--muted-foreground)] hover:bg-[var(--surface-hover)] hover:text-[var(--foreground)] transition-colors"
          aria-label="Fechar"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
