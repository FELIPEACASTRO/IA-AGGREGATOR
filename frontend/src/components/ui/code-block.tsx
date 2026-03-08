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
    <div
      className={cn(
        'group relative my-3 overflow-hidden rounded-[var(--radius-lg)] border border-[var(--code-border)] bg-[var(--code-bg)]',
        className,
      )}
    >
      <div className="flex items-center justify-between border-b border-[var(--code-border)] bg-[var(--code-header-bg)] px-4 py-2">
        <span className="text-[0.65rem] font-medium uppercase tracking-[0.1em] text-[var(--code-muted)]">
          {language || 'code'}
        </span>
        <button
          onClick={handleCopy}
          className={cn(
            'inline-flex h-8 items-center gap-1.5 rounded-[var(--radius-md)] px-2.5',
            'text-[0.65rem] font-medium transition-colors',
            copied
              ? 'text-[var(--success)]'
              : 'text-[var(--code-muted)] hover:bg-[var(--code-border)] hover:text-[var(--code-text)]',
          )}
          aria-label="Copiar codigo"
        >
          {copied ? (
            <>
              <Check className="h-3 w-3" /> Copiado
            </>
          ) : (
            <>
              <Copy className="h-3 w-3" /> Copiar
            </>
          )}
        </button>
      </div>
      <pre className="overflow-x-auto bg-[var(--code-bg)] p-4">
        <code className="font-[var(--font-mono-stack)] text-[14px] leading-[1.65] text-[var(--code-text)]">
          {children}
        </code>
      </pre>
    </div>
  );
}
