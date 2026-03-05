'use client';

import { useState } from 'react';
import { Check, Copy } from 'lucide-react';
import { cn } from '@/lib/cn';

interface CodeBlockProps {
  children: string;
  language?: string;
  className?: string;
}

export function CodeBlock({ children, language, className }: CodeBlockProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(children);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      /* ignore */
    }
  };

  return (
    <div className={cn('group relative my-3 overflow-hidden rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface-1)]', className)}>
      {/* Header bar */}
      <div className="flex items-center justify-between border-b border-[var(--border)] bg-[var(--surface-2)] px-4 py-2">
        <span className="text-[0.65rem] font-medium uppercase tracking-widest text-[var(--muted-foreground)]">
          {language || 'code'}
        </span>
        <button
          onClick={handleCopy}
          className={cn(
            'inline-flex items-center gap-1.5 rounded-[var(--radius-sm)] px-2 py-1',
            'text-[0.65rem] font-medium transition-colors',
            copied
              ? 'text-[var(--success)]'
              : 'text-[var(--muted-foreground)] hover:text-[var(--foreground)] hover:bg-[var(--accent)]'
          )}
          aria-label="Copiar código"
        >
          {copied ? (
            <><Check className="h-3 w-3" /> Copiado</>
          ) : (
            <><Copy className="h-3 w-3" /> Copiar</>
          )}
        </button>
      </div>
      {/* Code content */}
      <pre className="overflow-x-auto p-4">
        <code className="text-[0.8rem] leading-relaxed font-mono text-[var(--foreground)]">
          {children}
        </code>
      </pre>
    </div>
  );
}
