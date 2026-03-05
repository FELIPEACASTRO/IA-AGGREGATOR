import { TextareaHTMLAttributes, forwardRef } from 'react';
import { cn } from '@/lib/cn';

type TextareaProps = TextareaHTMLAttributes<HTMLTextAreaElement>;

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  { className, ...props },
  ref
) {
  return (
    <textarea
      ref={ref}
      className={cn(
        'w-full rounded-xl border border-[var(--border)] bg-[var(--background)] px-4 py-3 text-sm',
        'placeholder:text-[var(--muted-foreground)] resize-none',
        'focus:outline-none focus:ring-2 focus:ring-[var(--ring)]',
        className
      )}
      {...props}
    />
  );
});
