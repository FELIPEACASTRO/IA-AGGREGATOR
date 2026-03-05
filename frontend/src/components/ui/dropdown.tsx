'use client';

import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { ChevronDown } from 'lucide-react';
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

  const selected = options.find((o) => o.value === value);

  const openDropdown = () => {
    if (disabled) return;
    if (!triggerRef.current) return;
    const rect = triggerRef.current.getBoundingClientRect();
    setCoords({ top: rect.bottom + 4, left: rect.left, width: rect.width });
    setOpen(true);
    setHighlighted(Math.max(0, options.findIndex((o) => o.value === value)));
  };

  const close = () => setOpen(false);

  const select = (val: string) => {
    onChange(val);
    close();
  };

  // Close on outside click
  useEffect(() => {
    if (!open) return;
    const handler = (e: MouseEvent) => {
      if (!listRef.current?.contains(e.target as Node) && !triggerRef.current?.contains(e.target as Node)) {
        close();
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [open]);

  // Keyboard navigation
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (!open) { openDropdown(); return; }
      setHighlighted((h) => Math.min(h + 1, options.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlighted((h) => Math.max(h - 1, 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (open) select(options[highlighted].value);
      else openDropdown();
    } else if (e.key === 'Escape') {
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
          'flex w-full items-center justify-between gap-2 rounded-[var(--radius-md)] border border-[var(--border)]',
          'bg-[var(--surface-1)] px-3 py-2 text-[var(--text-sm)] text-[var(--foreground)]',
          'hover:border-[var(--border-strong)] hover:bg-[var(--surface-2)] transition-colors',
          'focus-visible:ring-2 focus-visible:ring-[var(--ring)]',
          'disabled:cursor-not-allowed disabled:opacity-50',
          triggerClassName
        )}
      >
        <span className="flex items-center gap-2 min-w-0">
          {selected?.icon && <span className="shrink-0">{selected.icon}</span>}
          <span className="truncate">{selected?.label || placeholder}</span>
        </span>
        <ChevronDown
          className={cn('h-4 w-4 shrink-0 text-[var(--muted-foreground)] transition-transform', open && 'rotate-180')}
        />
      </button>

      {typeof document !== 'undefined' &&
        createPortal(
          <AnimatePresence>
            {open && (
              <motion.ul
                ref={listRef}
                role="listbox"
                className={cn(
                  'glass fixed z-[var(--z-dropdown)] overflow-hidden rounded-[var(--radius-lg)] shadow-[var(--shadow-xl)]',
                  'py-1'
                )}
                style={{ top: coords.top, left: coords.left, minWidth: coords.width }}
                initial={{ opacity: 0, y: -6, scale: 0.97 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: -6, scale: 0.97 }}
                transition={{ duration: 0.15 }}
              >
                {options.map((opt, i) => (
                  <li
                    key={opt.value}
                    role="option"
                    aria-selected={opt.value === value}
                    onClick={() => !opt.disabled && select(opt.value)}
                    onMouseEnter={() => setHighlighted(i)}
                    className={cn(
                      'flex cursor-pointer items-center gap-3 px-3 py-2.5 transition-colors',
                      i === highlighted && 'bg-[var(--surface-3)]',
                      opt.value === value && 'text-[var(--brand-primary)]',
                      opt.disabled && 'cursor-not-allowed opacity-40'
                    )}
                  >
                    {opt.icon && <span className="shrink-0 text-[var(--muted-foreground)]">{opt.icon}</span>}
                    <span className="flex-1 min-w-0">
                      <span className="block text-[var(--text-sm)] font-medium truncate">{opt.label}</span>
                      {opt.description && (
                        <span className="block text-[0.65rem] text-[var(--muted-foreground)] truncate">
                          {opt.description}
                        </span>
                      )}
                    </span>
                    {opt.badge && <span className="shrink-0">{opt.badge}</span>}
                    {opt.value === value && (
                      <span className="ml-auto text-[var(--brand-primary)] text-xs">✓</span>
                    )}
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
