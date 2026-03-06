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
          <span className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--muted-foreground)]">
            {icon}
          </span>
        )}
        <input
          ref={ref}
          type={resolvedType}
          className={cn(
            'w-full rounded-[var(--radius-md)] border border-[var(--input)] bg-[rgba(9,17,31,0.68)]',
            'px-4 py-3 text-[var(--text-sm)] text-[var(--foreground)] placeholder:text-[var(--muted-foreground)]',
            'shadow-[inset_0_1px_0_rgba(255,255,255,0.03)] transition-[border-color,box-shadow,background-color] duration-[var(--dur-base)] ease-[var(--ease-standard)]',
            'hover:border-[var(--border-strong)] focus:outline-none focus:border-[rgba(96,115,255,0.48)] focus:shadow-[0_0_0_4px_rgba(96,115,255,0.12)]',
            'disabled:cursor-not-allowed disabled:opacity-50',
            error && 'border-[var(--destructive)] focus:border-[var(--destructive)] focus:shadow-[0_0_0_4px_rgba(255,107,135,0.12)]',
            icon && 'pl-11',
            isPassword && 'pr-12',
            className,
          )}
          {...props}
        />
        {isPassword && (
          <button
            type="button"
            onClick={() => setShowPassword((value) => !value)}
            className="absolute right-4 top-1/2 -translate-y-1/2 text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
            aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}
          >
            {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        )}
        {error && (
          <p className="mt-2 text-[0.72rem] text-[var(--destructive)]" role="alert">
            {error}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';
