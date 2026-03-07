'use client';

import { cn } from '@/lib/cn';

interface AvatarProps {
  name?: string;
  src?: string;
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
  accent?: boolean;
}

const sizeStyles: Record<string, string> = {
  xs: 'h-6 w-6 text-[10px]',
  sm: 'h-7 w-7 text-[11px]',
  md: 'h-8 w-8 text-[12px]',
  lg: 'h-10 w-10 text-[14px]',
  xl: 'h-12 w-12 text-[16px]',
};

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
  return name.slice(0, 2).toUpperCase();
}

export function Avatar({ name = 'U', src, size = 'md', className, accent }: AvatarProps) {
  const initials = getInitials(name);

  if (src) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={src}
        alt={name}
        className={cn('shrink-0 rounded-full object-cover', sizeStyles[size], className)}
      />
    );
  }

  return (
    <span
      className={cn(
        'inline-flex shrink-0 items-center justify-center rounded-full font-semibold select-none',
        accent
          ? 'bg-[var(--accent)] text-white'
          : 'bg-[var(--surface-hover)] text-[var(--muted-foreground)]',
        sizeStyles[size],
        className,
      )}
      title={name}
    >
      {initials}
    </span>
  );
}
