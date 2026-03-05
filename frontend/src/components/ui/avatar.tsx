import { cn } from '@/lib/cn';

const COLORS = [
  '#7C3AED', '#06B6D4', '#10B981', '#F59E0B',
  '#EF4444', '#EC4899', '#8B5CF6', '#3B82F6',
];

function getColorForName(name: string): string {
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
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
  sm: 'h-7 w-7 text-[0.6rem]',
  md: 'h-9 w-9 text-xs',
  lg: 'h-11 w-11 text-sm',
  xl: 'h-14 w-14 text-base',
};

export function Avatar({ name = 'U', src, size = 'md', className }: AvatarProps) {
  const initials = getInitials(name);
  const color = getColorForName(name);

  if (src) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={src}
        alt={name}
        className={cn('rounded-full object-cover', sizeStyles[size], className)}
      />
    );
  }

  return (
    <span
      className={cn(
        'inline-flex items-center justify-center rounded-full font-semibold text-white select-none shrink-0',
        sizeStyles[size],
        className
      )}
      style={{ backgroundColor: color }}
      aria-label={name}
    >
      {initials}
    </span>
  );
}
