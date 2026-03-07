'use client';

import { forwardRef, ButtonHTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

type Size = 'sm' | 'md' | 'lg';

interface IconButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  size?: Size;
  variant?: 'ghost' | 'outline';
}

const sizeStyles: Record<Size, string> = {
  sm: 'h-7 w-7',
  md: 'h-8 w-8',
  lg: 'h-9 w-9',
};

export const IconButton = forwardRef<HTMLButtonElement, IconButtonProps>(
  ({ className, size = 'md', variant = 'ghost', ...props }, ref) => {
    return (
      <button
        ref={ref}
        className={cn(
          'inline-flex items-center justify-center rounded-[var(--radius-md)] transition-colors select-none',
          'text-[var(--muted-foreground)] hover:text-[var(--foreground)]',
          'disabled:pointer-events-none disabled:opacity-40',
          variant === 'ghost' && 'hover:bg-[var(--surface-hover)]',
          variant === 'outline' && 'border border-[var(--border)] hover:bg-[var(--surface-hover)]',
          sizeStyles[size],
          className,
        )}
        {...props}
      />
    );
  },
);

IconButton.displayName = 'IconButton';
