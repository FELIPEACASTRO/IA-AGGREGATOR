'use client';

import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { cn } from '@/lib/cn';

interface TooltipProps {
  content: React.ReactNode;
  children: React.ReactElement;
  delay?: number;
  side?: 'top' | 'bottom' | 'left' | 'right';
  className?: string;
}

export function Tooltip({ content, children, delay = 300, side = 'top', className }: TooltipProps) {
  const [visible, setVisible] = useState(false);
  const [coords, setCoords] = useState({ x: 0, y: 0 });
  const triggerRef = useRef<HTMLElement>(null);
  const timerRef = useRef<ReturnType<typeof setTimeout>>(null);

  const show = () => {
    timerRef.current = setTimeout(() => {
      if (!triggerRef.current) return;
      const rect = triggerRef.current.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;

      const offset = 10;
      setCoords(
        side === 'top'
          ? { x: centerX, y: rect.top - offset }
          : side === 'bottom'
          ? { x: centerX, y: rect.bottom + offset }
          : side === 'left'
          ? { x: rect.left - offset, y: centerY }
          : { x: rect.right + offset, y: centerY }
      );
      setVisible(true);
    }, delay);
  };

  const hide = () => {
    if (timerRef.current) clearTimeout(timerRef.current);
    setVisible(false);
  };

  useEffect(() => () => { if (timerRef.current) clearTimeout(timerRef.current); }, []);

  const getTransform = () => {
    if (side === 'top') return 'translateX(-50%) translateY(-100%)';
    if (side === 'bottom') return 'translateX(-50%)';
    if (side === 'left') return 'translateX(-100%) translateY(-50%)';
    return 'translateY(-50%)';
  };

  const child = children as React.ReactElement & { ref?: React.Ref<HTMLElement> };

  return (
    <>
      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
      {React.cloneElement(child as any, {
        ref: triggerRef,
        onMouseEnter: show,
        onMouseLeave: hide,
        onFocus: show,
        onBlur: hide,
      })}
      {typeof document !== 'undefined' &&
        createPortal(
          <AnimatePresence>
            {visible && (
              <motion.div
                role="tooltip"
                className={cn(
                  'glass fixed z-[var(--z-tooltip)] max-w-xs rounded-[var(--radius-md)] px-2.5 py-1.5',
                  'text-[0.7rem] font-medium text-[var(--foreground)] shadow-[var(--shadow-lg)]',
                  'pointer-events-none',
                  className
                )}
                style={{ left: coords.x, top: coords.y, transform: getTransform() }}
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.9 }}
                transition={{ duration: 0.1 }}
              >
                {content}
              </motion.div>
            )}
          </AnimatePresence>,
          document.body
        )}
    </>
  );
}

// Need React import for cloneElement
import React from 'react';
