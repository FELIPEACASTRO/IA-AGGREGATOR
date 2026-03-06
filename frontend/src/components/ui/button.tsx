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
  lg: 'min-h-[3rem] px-7 text-[0.92rem] font-semibold',
};

const variantClasses: Record<ButtonVariant, string> = {
  brand: 'text-white shadow-[var(--shadow-brand)] hover:-translate-y-0.5 hover:shadow-[var(--brand-glow)]',
  primary: 'bg-[var(--primary)] text-[var(--primary-foreground)] shadow-[var(--shadow-brand)] hover:-translate-y-0.5 hover:brightness-110',
  secondary: 'bg-[var(--secondary)] text-[var(--secondary-foreground)] border-[var(--border)] hover:border-[var(--border-hover)] hover:bg-[var(--card-hover)]',
  ghost: 'bg-transparent text-[var(--foreground-muted)] hover:bg-[rgba(255,255,255,0.04)] hover:text-[var(--foreground)]',
  outline: 'bg-transparent text-[var(--foreground)] border-[var(--border)] hover:border-[rgba(124,106,255,0.3)] hover:bg-[rgba(124,106,255,0.06)]',
  destructive: 'bg-[var(--destructive)] text-[var(--destructive-foreground)] shadow-[0_8px_32px_rgba(255,92,111,0.2)] hover:-translate-y-0.5 hover:brightness-110',
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = 'primary', size = 'md', style, ...props }, ref) => {
    const isBrand = variant === 'brand';
    return (
      <button
        ref={ref}
        className={cn(
          'inline-flex items-center justify-center gap-2 rounded-[var(--radius-pill)] border border-transparent',
          'transition-all duration-[var(--dur-base)]',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--background)]',
          'disabled:cursor-not-allowed disabled:opacity-40 disabled:shadow-none disabled:translate-y-0',
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
