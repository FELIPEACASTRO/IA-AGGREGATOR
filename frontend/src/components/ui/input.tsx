'use client';

import { forwardRef, InputHTMLAttributes, useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import { cn } from '@/lib/cn';

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  icon?: React.ReactNode;
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, type, icon, error, ...props }, ref) => {
    const [showPassword, setShowPassword] = useState(false);
    const isPassword = type === 'password';
    const resolvedType = isPassword ? (showPassword ? 'text' : 'password') : type;

    return (
      <div className="relative w-full">
        {icon && (
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--muted-foreground)]">
            {icon}
          </span>
        )}
        <input
          ref={ref}
          type={resolvedType}
          className={cn(
            'w-full rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface-1)]',
            'px-3 py-2.5 text-[var(--text-sm)] text-[var(--foreground)] placeholder:text-[var(--muted-foreground)]',
            'transition-all duration-[var(--dur-base)] ease-[var(--ease-standard)]',
            'hover:border-[var(--border-strong)]',
            'focus:outline-none focus:border-[var(--brand-primary)] focus:ring-2 focus:ring-[var(--ring)]',
            'disabled:cursor-not-allowed disabled:opacity-50',
            error && 'border-[var(--destructive)] focus:ring-[var(--destructive)]/30',
            icon && 'pl-9',
            isPassword && 'pr-10',
            className
          )}
          {...props}
        />
        {isPassword && (
          <button
            type="button"
            onClick={() => setShowPassword((v) => !v)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
            aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}
          >
            {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        )}
        {error && (
          <p className="mt-1.5 text-[0.7rem] text-[var(--destructive)]" role="alert">
            {error}
          </p>
        )}
      </div>
    );
  }
);
Input.displayName = 'Input';
