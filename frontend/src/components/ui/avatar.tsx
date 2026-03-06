import { cn } from '@/lib/cn';

const COLORS = ['#6073ff', '#8b6cff', '#f25d9c', '#4ed9a7', '#77b8ff', '#ffbf66'];

function getColorForName(name: string): string {
  let hash = 0;
  for (let index = 0; index < name.length; index += 1) {
    hash = name.charCodeAt(index) + ((hash << 5) - hash);
  }
  return COLORS[Math.abs(hash) % COLORS.length];
}

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
  return name.slice(0, 2).toUpperCase();
}

interface AvatarProps {
  name?: string;
  src?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
}

const sizeStyles = {
  sm: 'h-8 w-8 text-[0.62rem]',
  md: 'h-10 w-10 text-[0.75rem]',
  lg: 'h-12 w-12 text-[0.92rem]',
  xl: 'h-16 w-16 text-[1.02rem]',
};

export function Avatar({ name = 'LU', src, size = 'md', className }: AvatarProps) {
  const initials = getInitials(name);
  const color = getColorForName(name);

  if (src) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img src={src} alt={name} className={cn('rounded-full object-cover ring-1 ring-[var(--border)]', sizeStyles[size], className)} />
    );
  }

  return (
    <span
      className={cn(
        'inline-flex items-center justify-center rounded-full font-semibold text-white select-none shrink-0 ring-1 ring-white/10 shadow-[var(--shadow-sm)]',
        sizeStyles[size],
        className,
      )}
      style={{ background: `linear-gradient(135deg, ${color} 0%, #08111f 160%)` }}
      aria-label={name}
    >
      {initials}
    </span>
  );
}
