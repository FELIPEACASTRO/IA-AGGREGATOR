'use client';

import { Search } from 'lucide-react';
import { cn } from '@/lib/cn';

type SearchFieldProps = {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  className?: string;
  inputClassName?: string;
  ariaLabel?: string;
};

export function SearchField({
  value,
  onChange,
  placeholder = 'Buscar...',
  className,
  inputClassName,
  ariaLabel,
}: SearchFieldProps) {
  return (
    <div className={cn('relative', className)}>
      <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--subtle-foreground)]" />
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        aria-label={ariaLabel || placeholder}
        className={cn(
          'h-11 w-full rounded-[9.6px] border border-[var(--input-border)] bg-[var(--input-bg)] pl-10 pr-3 text-[14px] text-[var(--foreground)] outline-none transition-colors',
          'placeholder:text-[var(--subtle-foreground)] focus:border-[var(--accent)] focus:ring-2 focus:ring-[var(--ring)]',
          inputClassName,
        )}
      />
    </div>
  );
}
