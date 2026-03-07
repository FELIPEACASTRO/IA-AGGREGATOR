'use client';

import { Avatar } from '@/components/ui/avatar';

export function StreamingIndicator() {
  return (
    <div className="flex gap-3">
      <Avatar name="L" size="sm" accent className="mt-0.5 shrink-0" />
      <div className="flex items-center gap-1.5 py-2">
        <span className="pulse-dot" />
        <span className="pulse-dot" />
        <span className="pulse-dot" />
      </div>
    </div>
  );
}
