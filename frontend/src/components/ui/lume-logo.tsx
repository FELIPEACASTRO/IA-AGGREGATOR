'use client';

import { cn } from '@/lib/cn';

type LumeLogoProps = {
  compact?: boolean;
  className?: string;
  iconClassName?: string;
  textClassName?: string;
};

export function LumeLogo({
  compact,
  className,
  iconClassName,
  textClassName,
}: LumeLogoProps) {
  return (
    <div className={cn('inline-flex items-center gap-3', className)}>
      <span
        className={cn(
          'inline-flex h-9 w-9 items-center justify-center rounded-full border border-[var(--border)] bg-[var(--accent-gold-light)] text-[var(--accent-gold)] shadow-[var(--shadow-xs)]',
          iconClassName,
        )}
      >
        <svg viewBox="0 0 28 28" className="h-4.5 w-4.5 fill-current" aria-hidden="true">
          <path d="M14 1.2c.36 0 .6.22.66.58l1.05 6.47c.08.48.46.86.94.94l6.47 1.05c.36.06.58.3.58.66s-.22.6-.58.66l-6.47 1.05a1.2 1.2 0 0 0-.94.94l-1.05 6.47c-.06.36-.3.58-.66.58s-.6-.22-.66-.58l-1.05-6.47a1.2 1.2 0 0 0-.94-.94l-6.47-1.05c-.36-.06-.58-.3-.58-.66s.22-.6.58-.66l6.47-1.05c.48-.08.86-.46.94-.94l1.05-6.47c.06-.36.3-.58.66-.58Zm7.6 16.06c.23 0 .4.14.45.36l.43 2.67c.05.24.23.42.47.47l2.67.43c.22.05.36.22.36.45s-.14.4-.36.45l-2.67.43c-.24.05-.42.23-.47.47l-.43 2.67c-.05.22-.22.36-.45.36s-.4-.14-.45-.36l-.43-2.67a.61.61 0 0 0-.47-.47l-2.67-.43a.46.46 0 0 1-.36-.45c0-.23.14-.4.36-.45l2.67-.43a.61.61 0 0 0 .47-.47l.43-2.67c.05-.22.22-.36.45-.36Z" />
        </svg>
      </span>
      {!compact ? (
        <span className={cn('font-[var(--font-serif)] text-[18px] font-medium tracking-[-0.04em] text-[var(--foreground)]', textClassName)}>
          Lume
        </span>
      ) : null}
    </div>
  );
}
