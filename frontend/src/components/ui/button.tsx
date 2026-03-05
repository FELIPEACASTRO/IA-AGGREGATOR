import { ButtonHTMLAttributes, forwardRef } from 'react';
import { cn } from '@/lib/cn';

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'destructive' | 'brand' | 'outline';
type ButtonSize = 'sm' | 'md' | 'lg';

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
};

const sizeClasses: Record<ButtonSize, string> = {
  sm: 'px-2.5 py-1.5 text-[var(--text-xs)] font-medium',
  md: 'px-4 py-2.5 text-[var(--text-sm)] font-medium',
  lg: 'px-6 py-3 text-[var(--text-base)] font-semibold',
};

const variantClasses: Record<ButtonVariant, string> = {
  brand:
    'text-white shadow-[var(--shadow-brand)] hover:shadow-[var(--shadow-xl)] hover:opacity-90 active:scale-[0.97]',
  primary:
    'bg-[var(--primary)] text-[var(--primary-foreground)] hover:opacity-90 active:scale-[0.97] focus-visible:ring-[var(--ring)]',
  secondary:
    'bg-[var(--secondary)] text-[var(--secondary-foreground)] border-[var(--border)] hover:bg-[var(--accent)] active:scale-[0.97] focus-visible:ring-[var(--ring)]',
  ghost:
    'bg-transparent text-[var(--foreground)] hover:bg-[var(--surface-2)] active:scale-[0.97] focus-visible:ring-[var(--ring)]',
  outline:
    'bg-transparent text-[var(--foreground)] border-[var(--border-strong)] hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)] active:scale-[0.97]',
  destructive:
    'bg-[var(--destructive)] text-[var(--destructive-foreground)] hover:opacity-90 active:scale-[0.97] focus-visible:ring-[var(--destructive)]',
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = 'primary', size = 'md', style, ...props }, ref) => {
    const isBrand = variant === 'brand';
    return (
      <button
        ref={ref}
        className={cn(
          'relative inline-flex items-center justify-center gap-2 rounded-[var(--radius-md)] border border-transparent',
          sizeClasses[size],
          'transition-[transform,opacity,box-shadow,background-color,border-color] duration-[var(--dur-base)] ease-[var(--ease-standard)]',
          'disabled:opacity-50 disabled:cursor-not-allowed disabled:!scale-100',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--background)]',
          variantClasses[variant],
          className
        )}
        style={
          isBrand
            ? { background: 'var(--brand-gradient)', ...style }
            : style
        }
        {...props}
      />
    );
  }
);
Button.displayName = 'Button';

