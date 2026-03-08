'use client';

import Link from 'next/link';
import { ElementType, ReactNode } from 'react';
import { cn } from '@/lib/cn';

type SidebarNavItemProps = {
  href?: string;
  label: string;
  icon: ElementType;
  active?: boolean;
  badge?: ReactNode;
  shortcut?: ReactNode;
  onClick?: () => void;
  className?: string;
};

export function SidebarNavItem({
  href,
  label,
  icon: Icon,
  active,
  badge,
  shortcut,
  onClick,
  className,
}: SidebarNavItemProps) {
  const classes = cn(
    'flex h-9 items-center gap-3 rounded-[var(--radius-md)] px-3 text-[13px] transition-colors',
    active
      ? 'bg-[var(--surface-active)] text-[var(--foreground)]'
      : 'text-[var(--muted-foreground)] hover:bg-[var(--surface-hover)] hover:text-[var(--foreground)]',
    className,
  );

  const content = (
    <>
      <Icon className="h-4 w-4 shrink-0" />
      <span className="min-w-0 flex-1 truncate">{label}</span>
      {badge}
      {shortcut ? <span className="shrink-0 text-[11px] text-[var(--subtle-foreground)]">{shortcut}</span> : null}
    </>
  );

  if (href) {
    return (
      <Link href={href} onClick={onClick} className={classes}>
        {content}
      </Link>
    );
  }

  return (
    <button type="button" onClick={onClick} className={classes}>
      {content}
    </button>
  );
}
