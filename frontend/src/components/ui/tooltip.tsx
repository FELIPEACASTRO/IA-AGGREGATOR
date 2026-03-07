'use client';

import { ReactNode, useState } from 'react';
import { cn } from '@/lib/cn';

interface TooltipProps {
  content: string;
  children: ReactNode;
  side?: 'top' | 'bottom';
  className?: string;
}

export function Tooltip({ content, children, side = 'top', className }: TooltipProps) {
  const [visible, setVisible] = useState(false);

  return (
    <span
      className="relative inline-flex"
      onMouseEnter={() => setVisible(true)}
      onMouseLeave={() => setVisible(false)}
    >
      {children}
      {visible && (
        <span
          role="tooltip"
          className={cn(
            'absolute left-1/2 z-[var(--z-tooltip)] -translate-x-1/2 whitespace-nowrap rounded-[var(--radius-md)] bg-[var(--foreground)] px-2.5 py-1 text-[12px] text-[var(--background)] shadow-[var(--shadow-md)] pointer-events-none',
            side === 'top' && 'bottom-full mb-1.5',
            side === 'bottom' && 'top-full mt-1.5',
            className,
          )}
        >
          {content}
        </span>
      )}
    </span>
  );
}
