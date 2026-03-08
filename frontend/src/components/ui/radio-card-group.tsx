'use client';

import { ElementType, ReactNode } from 'react';
import { Check } from 'lucide-react';
import { cn } from '@/lib/cn';

type RadioCardOption = {
  value: string;
  label: string;
  description?: string;
  icon?: ElementType;
  meta?: ReactNode;
};

type RadioCardGroupProps = {
  value: string;
  onChange: (value: string) => void;
  options: RadioCardOption[];
  columns?: 2 | 3 | 4;
  className?: string;
};

const columnStyles: Record<NonNullable<RadioCardGroupProps['columns']>, string> = {
  2: 'md:grid-cols-2',
  3: 'md:grid-cols-3',
  4: 'md:grid-cols-4',
};

export function RadioCardGroup({
  value,
  onChange,
  options,
  columns = 3,
  className,
}: RadioCardGroupProps) {
  return (
    <div role="radiogroup" className={cn('grid gap-2', columnStyles[columns], className)}>
      {options.map((option) => {
        const active = option.value === value;
        const Icon = option.icon;

        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={active}
            onClick={() => onChange(option.value)}
            className={cn(
              'rounded-[var(--radius-md)] border px-3 py-3 text-left transition-colors',
              active
                ? 'border-[var(--accent)] bg-[var(--accent-light)]'
                : 'border-[var(--border)] bg-[var(--surface)] hover:bg-[var(--surface-hover)]',
            )}
          >
            <div className="flex items-start justify-between gap-2">
              <div className="flex min-w-0 items-start gap-2.5">
                {Icon ? (
                  <span className="inline-flex h-8 w-8 items-center justify-center rounded-[var(--radius-md)] bg-[var(--surface-hover)] text-[var(--accent-brand)]">
                    <Icon className="h-4 w-4" />
                  </span>
                ) : null}
                <div className="min-w-0">
                  <p className={cn('text-[13px] font-medium', active ? 'text-[var(--accent)]' : 'text-[var(--foreground)]')}>
                    {option.label}
                  </p>
                  {option.description ? (
                    <p className="mt-1 text-[11px] leading-relaxed text-[var(--muted-foreground)]">
                      {option.description}
                    </p>
                  ) : null}
                </div>
              </div>
              {active ? <Check className="mt-0.5 h-4 w-4 shrink-0 text-[var(--accent)]" /> : option.meta}
            </div>
          </button>
        );
      })}
    </div>
  );
}
