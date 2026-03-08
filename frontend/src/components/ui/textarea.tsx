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
      const element = internalRef.current;
      if (!element || !autoResize) return;
      element.style.height = 'auto';
      element.style.height = `${Math.min(element.scrollHeight, maxHeight)}px`;
    }, [autoResize, maxHeight]);

    useEffect(() => {
      resize();
    }, [props.value, resize]);

    return (
      <textarea
        ref={setRef}
        onChange={(event) => {
          onChange?.(event);
          resize();
        }}
        className={cn(
          'min-h-11 w-full resize-none rounded-[9.6px] border border-[var(--input-border)] bg-[var(--input-bg)]',
          'px-3 py-3 text-[14px] text-[var(--foreground)] placeholder:text-[var(--subtle-foreground)]',
          'transition-colors hover:border-[var(--border-strong)]',
          'focus:border-[var(--accent)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)]',
          className,
        )}
        {...props}
      />
    );
  },
);

Textarea.displayName = 'Textarea';
