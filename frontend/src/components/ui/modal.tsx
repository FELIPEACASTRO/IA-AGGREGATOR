'use client';

import { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from '@/lib/cn';

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title?: string;
  description?: string;
  children: React.ReactNode;
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
  const overlayRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return undefined;
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [open, onClose]);

  useEffect(() => {
    document.body.style.overflow = open ? 'hidden' : '';
    return () => {
      document.body.style.overflow = '';
    };
  }, [open]);

  if (typeof document === 'undefined') return null;

  return createPortal(
    <AnimatePresence>
      {open && (
        <div
          ref={overlayRef}
          className="fixed inset-0 z-[var(--z-modal)] flex items-center justify-center p-4"
          role="dialog"
          aria-modal="true"
          aria-label={title}
        >
          <motion.div
            className="absolute inset-0 bg-[rgba(3,8,18,0.72)] backdrop-blur-md"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
            onClick={onClose}
          />

          <motion.div
            className={cn('glass relative w-full rounded-[var(--radius-xl)] shadow-[var(--shadow-xl)]', sizeWidths[size], className)}
            initial={{ opacity: 0, scale: 0.96, y: 12 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.96, y: 12 }}
            transition={{ duration: 0.2, ease: [0.34, 1.56, 0.64, 1] }}
          >
            {(title || description) && (
              <div className="border-b border-[var(--glass-border)] px-6 py-5">
                {title && <h2 className="text-[var(--text-xl)] font-semibold text-[var(--foreground)]">{title}</h2>}
                {description && <p className="mt-1.5 text-[var(--text-sm)] text-[var(--muted-foreground)]">{description}</p>}
              </div>
            )}
            <div className="p-6">{children}</div>
            <button
              onClick={onClose}
              className="absolute right-4 top-4 inline-flex h-9 w-9 items-center justify-center rounded-full text-[var(--muted-foreground)] hover:bg-[var(--surface-2)] hover:text-[var(--foreground)]"
              aria-label="Fechar"
            >
              <X className="h-4 w-4" />
            </button>
          </motion.div>
        </div>
      )}
    </AnimatePresence>,
    document.body
  );
}
