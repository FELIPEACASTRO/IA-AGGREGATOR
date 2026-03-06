import { ButtonHTMLAttributes, forwardRef } from 'react';
import { cn } from '@/lib/cn';

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'destructive' | 'brand' | 'outline';
type ButtonSize = 'sm' | 'md' | 'lg';

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
};

const sizeClasses: Record<ButtonSize, string> = {
  sm: 'min-h-9 px-3.5 text-[0.78rem] font-semibold',
  md: 'min-h-11 px-5 text-[0.86rem] font-semibold',
  lg: 'min-h-12 px-6 text-[0.94rem] font-semibold',
};

const variantClasses: Record<ButtonVariant, string> = {
  brand: 'text-white shadow-[var(--shadow-brand)] hover:-translate-y-0.5 hover:shadow-[0_24px_56px_rgba(96,115,255,0.3)]',
  primary: 'bg-[var(--primary)] text-[var(--primary-foreground)] shadow-[var(--shadow-brand)] hover:-translate-y-0.5 hover:opacity-95',
  secondary: 'bg-[var(--surface-2)] text-[var(--foreground)] border-[var(--border)] hover:border-[var(--border-strong)] hover:bg-[var(--surface-3)]',
  ghost: 'bg-transparent text-[var(--muted-foreground)] hover:bg-[var(--surface-2)] hover:text-[var(--foreground)]',
  outline: 'bg-transparent text-[var(--foreground)] border-[var(--border)] hover:border-[var(--brand-primary)]/40 hover:bg-[rgba(96,115,255,0.08)]',
  destructive: 'bg-[var(--destructive)] text-[var(--destructive-foreground)] shadow-[0_16px_36px_rgba(255,107,135,0.2)] hover:-translate-y-0.5 hover:opacity-95',
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = 'primary', size = 'md', style, ...props }, ref) => {
    const isBrand = variant === 'brand';
    return (
      <button
        ref={ref}
        className={cn(
          'inline-flex items-center justify-center gap-2 rounded-[var(--radius-pill)] border border-transparent',
          'transition-[transform,opacity,box-shadow,background-color,border-color,color] duration-[var(--dur-base)] ease-[var(--ease-standard)]',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--background)]',
          'disabled:cursor-not-allowed disabled:opacity-45 disabled:shadow-none disabled:translate-y-0',
          sizeClasses[size],
          variantClasses[variant],
          className,
        )}
        style={isBrand ? { background: 'var(--brand-gradient)', ...style } : style}
        {...props}
      />
    );
  }
);

Button.displayName = 'Button';
