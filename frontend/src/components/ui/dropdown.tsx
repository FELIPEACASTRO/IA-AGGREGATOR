'use client';

import { useState, useRef, useEffect, useCallback, ReactNode } from 'react';
import { cn } from '@/lib/cn';
import { ChevronDown, Check } from 'lucide-react';

export interface DropdownOption {
  value: string;
  label: string;
  description?: string;
  badge?: ReactNode;
  icon?: ReactNode;
  disabled?: boolean;
}

interface DropdownProps {
  options: DropdownOption[];
  value?: string;
  onChange: (value: string) => void;
  placeholder?: string;
  triggerClassName?: string;
  className?: string;
  disabled?: boolean;
}

export function Dropdown({
  options,
  value,
  onChange,
  placeholder = 'Selecionar...',
  triggerClassName,
  className,
  disabled,
}: DropdownProps) {
  const [open, setOpen] = useState(false);
  const [highlighted, setHighlighted] = useState(-1);
  const containerRef = useRef<HTMLDivElement>(null);

  const selected = options.find((o) => o.value === value);

  const close = useCallback(() => {
    setOpen(false);
    setHighlighted(-1);
  }, []);

  useEffect(() => {
    if (!open) return;
    const handler = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        close();
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [open, close]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!open) {
      if (e.key === 'Enter' || e.key === ' ' || e.key === 'ArrowDown') {
        e.preventDefault();
        setOpen(true);
        setHighlighted(0);
      }
      return;
    }

    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setHighlighted((i) => Math.min(i + 1, options.length - 1));
        break;
      case 'ArrowUp':
        e.preventDefault();
        setHighlighted((i) => Math.max(i - 1, 0));
        break;
      case 'Enter':
        e.preventDefault();
        if (highlighted >= 0 && options[highlighted] && !options[highlighted].disabled) {
          onChange(options[highlighted].value);
          close();
        }
        break;
      case 'Escape':
        e.preventDefault();
        close();
        break;
    }
  };

  return (
    <div ref={containerRef} className={cn('relative', className)} onKeyDown={handleKeyDown}>
      <button
        type="button"
        onClick={() => !disabled && setOpen(!open)}
        disabled={disabled}
        className={cn(
          'flex items-center gap-2 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface)] px-3 py-1.5',
          'text-[13px] text-[var(--foreground)] hover:bg-[var(--surface-hover)] transition-colors',
          'focus:outline-none focus:ring-2 focus:ring-[var(--ring)]',
          'disabled:cursor-not-allowed disabled:opacity-50',
          triggerClassName,
        )}
        aria-expanded={open}
        aria-haspopup="listbox"
      >
        {selected?.icon && <span className="shrink-0">{selected.icon}</span>}
        <span className="truncate">{selected?.label || placeholder}</span>
        <ChevronDown
          className={cn(
            'h-3.5 w-3.5 shrink-0 text-[var(--muted-foreground)] transition-transform',
            open && 'rotate-180',
          )}
        />
      </button>

      {open && (
        <div
          className="absolute left-0 top-full z-[var(--z-dropdown)] mt-1 w-full min-w-[220px] overflow-hidden rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface)] shadow-[var(--shadow-lg)]"
          role="listbox"
        >
          <div className="max-h-[300px] overflow-y-auto py-1">
            {options.map((option, index) => (
              <button
                key={option.value}
                type="button"
                role="option"
                aria-selected={option.value === value}
                onClick={() => {
                  if (!option.disabled) {
                    onChange(option.value);
                    close();
                  }
                }}
                onMouseEnter={() => setHighlighted(index)}
                disabled={option.disabled}
                className={cn(
                  'flex w-full items-center gap-2 px-3 py-2 text-left text-[13px] transition-colors',
                  index === highlighted && 'bg-[var(--surface-hover)]',
                  option.disabled && 'opacity-40 cursor-not-allowed',
                )}
              >
                {option.icon && <span className="shrink-0">{option.icon}</span>}
                <span className="flex-1 min-w-0">
                  <span className="block truncate font-medium">{option.label}</span>
                  {option.description && (
                    <span className="block truncate text-[11px] text-[var(--muted-foreground)]">
                      {option.description}
                    </span>
                  )}
                </span>
                {option.badge && <span className="shrink-0">{option.badge}</span>}
                {option.value === value && (
                  <Check className="h-3.5 w-3.5 shrink-0 text-[var(--accent)]" />
                )}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
