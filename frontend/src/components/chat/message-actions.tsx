'use client';

import { useState } from 'react';
import { cn } from '@/lib/cn';
import { Copy, Check, RefreshCw, ThumbsUp, ThumbsDown } from 'lucide-react';

interface MessageActionsProps {
  role: 'user' | 'assistant' | 'error';
  content: string;
  onRetry?: () => void;
  onFeedback?: (type: 'up' | 'down') => void;
  feedback?: 'up' | 'down' | null;
}

export function MessageActions({ role, content, onRetry, onFeedback, feedback }: MessageActionsProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(content);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {}
  };

  return (
    <div className="flex items-center gap-0.5 mt-1 opacity-0 group-hover:opacity-100 transition-opacity">
      <button
        onClick={handleCopy}
        className="inline-flex items-center gap-1 rounded-[var(--radius-sm)] px-1.5 py-1 text-[12px] text-[var(--muted-foreground)] hover:bg-[var(--surface-hover)] hover:text-[var(--foreground)] transition-colors"
        title="Copiar"
      >
        {copied ? <Check className="h-3 w-3 text-[var(--success)]" /> : <Copy className="h-3 w-3" />}
      </button>

      {role === 'assistant' && onRetry && (
        <button
          onClick={onRetry}
          className="inline-flex items-center gap-1 rounded-[var(--radius-sm)] px-1.5 py-1 text-[12px] text-[var(--muted-foreground)] hover:bg-[var(--surface-hover)] hover:text-[var(--foreground)] transition-colors"
          title="Regenerar"
        >
          <RefreshCw className="h-3 w-3" />
        </button>
      )}

      {role === 'assistant' && onFeedback && (
        <>
          <button
            onClick={() => onFeedback('up')}
            className={cn(
              'rounded-[var(--radius-sm)] p-1 transition-colors',
              feedback === 'up'
                ? 'text-[var(--success)] bg-[var(--surface-hover)]'
                : 'text-[var(--muted-foreground)] hover:bg-[var(--surface-hover)] hover:text-[var(--foreground)]',
            )}
            title="Util"
          >
            <ThumbsUp className="h-3 w-3" />
          </button>
          <button
            onClick={() => onFeedback('down')}
            className={cn(
              'rounded-[var(--radius-sm)] p-1 transition-colors',
              feedback === 'down'
                ? 'text-[var(--destructive)] bg-[var(--surface-hover)]'
                : 'text-[var(--muted-foreground)] hover:bg-[var(--surface-hover)] hover:text-[var(--foreground)]',
            )}
            title="Nao util"
          >
            <ThumbsDown className="h-3 w-3" />
          </button>
        </>
      )}
    </div>
  );
}
