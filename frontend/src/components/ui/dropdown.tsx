'use client';

import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { ChevronDown, Check } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from '@/lib/cn';

export interface DropdownOption {
  value: string;
  label: string;
  description?: string;
  icon?: React.ReactNode;
  badge?: React.ReactNode;
  disabled?: boolean;
}

interface DropdownProps {
  options: DropdownOption[];
  value?: string;
  onChange: (value: string) => void;
  placeholder?: string;
  label?: string;
  className?: string;
  triggerClassName?: string;
  disabled?: boolean;
}

export function Dropdown({
  options,
  value,
  onChange,
  placeholder = 'Selecionar...',
  className,
  triggerClassName,
  disabled,
}: DropdownProps) {
  const [open, setOpen] = useState(false);
  const [coords, setCoords] = useState({ top: 0, left: 0, width: 0 });
  const triggerRef = useRef<HTMLButtonElement>(null);
  const listRef = useRef<HTMLUListElement>(null);
  const [highlighted, setHighlighted] = useState(0);

  const selected = options.find((option) => option.value === value);

  const openDropdown = () => {
    if (disabled || !triggerRef.current) return;
    const rect = triggerRef.current.getBoundingClientRect();
    setCoords({ top: rect.bottom + 8, left: rect.left, width: rect.width });
    setOpen(true);
    setHighlighted(Math.max(0, options.findIndex((option) => option.value === value)));
  };

  const close = () => setOpen(false);

  const select = (selectedValue: string) => {
    onChange(selectedValue);
    close();
  };

  useEffect(() => {
    if (!open) return undefined;
    const handler = (event: MouseEvent) => {
      if (!listRef.current?.contains(event.target as Node) && !triggerRef.current?.contains(event.target as Node)) {
        close();
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [open]);

  const handleKeyDown = (event: React.KeyboardEvent) => {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      if (!open) {
        openDropdown();
        return;
      }
      setHighlighted((current) => Math.min(current + 1, options.length - 1));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setHighlighted((current) => Math.max(current - 1, 0));
    } else if (event.key === 'Enter') {
      event.preventDefault();
      if (open && options[highlighted]) select(options[highlighted].value);
      else openDropdown();
    } else if (event.key === 'Escape') {
      close();
    }
  };

  return (
    <div className={cn('relative', className)}>
      <button
        ref={triggerRef}
        type="button"
        onClick={openDropdown}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={open}
        className={cn(
          'flex w-full items-center justify-between gap-3 rounded-[var(--radius-pill)] border border-[var(--input)]',
          'bg-[rgba(9,17,31,0.68)] px-4 py-2.5 text-[var(--text-sm)] text-[var(--foreground)] shadow-[inset_0_1px_0_rgba(255,255,255,0.03)]',
          'hover:border-[var(--border-strong)] hover:bg-[rgba(15,28,49,0.92)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--ring)]',
          'disabled:cursor-not-allowed disabled:opacity-50',
          triggerClassName,
        )}
      >
        <span className="flex min-w-0 items-center gap-2">
          {selected?.icon && <span className="shrink-0">{selected.icon}</span>}
          <span className="truncate">{selected?.label || placeholder}</span>
        </span>
        <ChevronDown className={cn('h-4 w-4 shrink-0 text-[var(--muted-foreground)] transition-transform', open && 'rotate-180')} />
      </button>

      {typeof document !== 'undefined' &&
        createPortal(
          <AnimatePresence>
            {open && (
              <motion.ul
                ref={listRef}
                role="listbox"
                className="glass fixed z-[var(--z-dropdown)] overflow-hidden rounded-[var(--radius-lg)] py-1.5 shadow-[var(--shadow-xl)]"
                style={{ top: coords.top, left: coords.left, minWidth: coords.width }}
                initial={{ opacity: 0, y: -8, scale: 0.98 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: -8, scale: 0.98 }}
                transition={{ duration: 0.16 }}
              >
                {options.map((option, index) => (
                  <li
                    key={option.value}
                    role="option"
                    aria-selected={option.value === value}
                    onClick={() => !option.disabled && select(option.value)}
                    onMouseEnter={() => setHighlighted(index)}
                    className={cn(
                      'flex cursor-pointer items-center gap-3 px-3.5 py-2.5 transition-colors',
                      index === highlighted && 'bg-[rgba(96,115,255,0.12)]',
                      option.value === value && 'text-[var(--foreground)]',
                      option.disabled && 'cursor-not-allowed opacity-40'
                    )}
                  >
                    {option.icon && <span className="shrink-0 text-[var(--muted-foreground)]">{option.icon}</span>}
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-[var(--text-sm)] font-medium">{option.label}</span>
                      {option.description && (
                        <span className="block truncate text-[0.68rem] text-[var(--muted-foreground)]">{option.description}</span>
                      )}
                    </span>
                    {option.badge && <span className="shrink-0">{option.badge}</span>}
                    {option.value === value && <Check className="h-3.5 w-3.5 text-[var(--brand-primary)]" />}
                  </li>
                ))}
              </motion.ul>
            )}
          </AnimatePresence>,
          document.body
        )}
    </div>
  );
}
