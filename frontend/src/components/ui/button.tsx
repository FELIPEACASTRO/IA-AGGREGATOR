'use client';

import { forwardRef, ButtonHTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'destructive' | 'outline';
type ButtonSize = 'sm' | 'md' | 'lg';

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
};

const sizeClasses: Record<ButtonSize, string> = {
  sm: 'h-8 px-3 text-[12px]',
  md: 'h-9 px-4 text-[13px]',
  lg: 'h-11 px-5 text-[14px]',
};

const variantClasses: Record<ButtonVariant, string> = {
  primary:
    'border border-[var(--border-strong)] bg-[var(--primary)] text-[var(--primary-foreground)] shadow-[var(--shadow-xs)] hover:-translate-y-px hover:shadow-[var(--shadow-sm)] active:translate-y-0',
  secondary:
    'border border-[var(--border)] bg-[var(--surface)] text-[var(--foreground)] hover:bg-[var(--surface-hover)] active:bg-[var(--surface-active)]',
  ghost:
    'text-[var(--muted-foreground)] hover:bg-[var(--surface-hover)] hover:text-[var(--foreground)] active:bg-[var(--surface-active)]',
  destructive:
    'bg-[var(--destructive)] text-white hover:opacity-90 active:opacity-80',
  outline:
    'border border-[var(--border-strong)] bg-transparent text-[var(--foreground)] hover:bg-[var(--surface-hover)] active:bg-[var(--surface-active)]',
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = 'primary', size = 'md', ...props }, ref) => {
    return (
      <button
        ref={ref}
        className={cn(
          'inline-flex items-center justify-center gap-2 rounded-[var(--radius-md)] font-medium transition-all select-none',
          'disabled:pointer-events-none disabled:opacity-40',
          sizeClasses[size],
          variantClasses[variant],
          className,
        )}
        {...props}
      />
    );
  },
);

Button.displayName = 'Button';
