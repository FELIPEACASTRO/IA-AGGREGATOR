'use client';

import { TextareaHTMLAttributes, forwardRef, useRef, useCallback, useEffect } from 'react';
import { cn } from '@/lib/cn';

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  autoResize?: boolean;
  maxHeight?: number;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, autoResize = false, maxHeight = 200, onChange, ...props }, ref) => {
    const internalRef = useRef<HTMLTextAreaElement | null>(null);

    const setRef = useCallback(
      (node: HTMLTextAreaElement | null) => {
        internalRef.current = node;
        if (typeof ref === 'function') ref(node);
        else if (ref) ref.current = node;
      },
      [ref],
    );

    const resize = useCallback(() => {
      const el = internalRef.current;
      if (!el || !autoResize) return;
      el.style.height = 'auto';
      el.style.height = `${Math.min(el.scrollHeight, maxHeight)}px`;
    }, [autoResize, maxHeight]);

    useEffect(() => {
      resize();
    }, [props.value, resize]);

    return (
      <textarea
        ref={setRef}
        onChange={(e) => {
          onChange?.(e);
          resize();
        }}
        className={cn(
          'w-full resize-none rounded-[var(--radius-md)] border border-[var(--input-border)] bg-[var(--input-bg)]',
          'px-3 py-2.5 text-[14px] text-[var(--foreground)] placeholder:text-[var(--subtle-foreground)]',
          'transition-colors',
          'hover:border-[var(--border-strong)]',
          'focus:outline-none focus:ring-2 focus:ring-[var(--ring)] focus:border-[var(--accent)]',
          className,
        )}
        {...props}
      />
    );
  },
);

Textarea.displayName = 'Textarea';
