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
        'w-full rounded-[var(--radius-md)] border border-[var(--input)] bg-[rgba(9,17,31,0.68)] px-4 py-3 text-[var(--text-sm)] text-[var(--foreground)]',
        'placeholder:text-[var(--muted-foreground)] shadow-[inset_0_1px_0_rgba(255,255,255,0.03)] resize-none',
        'transition-[border-color,box-shadow,background-color] duration-[var(--dur-base)] ease-[var(--ease-standard)]',
        'hover:border-[var(--border-strong)] focus:outline-none focus:border-[rgba(96,115,255,0.48)] focus:shadow-[0_0_0_4px_rgba(96,115,255,0.12)]',
        className
      )}
      {...props}
    />
  );
});
