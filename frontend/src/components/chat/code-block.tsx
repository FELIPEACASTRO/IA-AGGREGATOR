'use client';

import { useState } from 'react';
import { cn } from '@/lib/cn';
import { Copy, Check } from 'lucide-react';

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
      setTimeout(() => setCopied(false), 1500);
    } catch {}
  };

  return (
    <div className={cn('relative rounded-[var(--radius-lg)] border border-[var(--border)] overflow-hidden my-3', className)}>
      {/* Header bar */}
      <div className="flex items-center justify-between bg-[var(--code-header-bg)] px-4 py-2 border-b border-[var(--code-border)]">
        <span className="text-[11px] font-medium text-[var(--code-muted)]">
          {language || 'code'}
        </span>
        <button
          onClick={handleCopy}
          className="inline-flex items-center gap-1 rounded-[var(--radius-sm)] px-2 py-0.5 text-[11px] text-[var(--code-muted)] hover:text-[var(--code-text)] hover:bg-[var(--code-border)] transition-colors"
        >
          {copied ? (
            <>
              <Check className="h-3.5 w-3.5" /> Copiado
            </>
          ) : (
            <>
              <Copy className="h-3.5 w-3.5" /> Copiar
            </>
          )}
        </button>
      </div>

      {/* Code content */}
      <pre className="bg-[var(--code-bg)] p-4 overflow-x-auto">
        <code className="text-[13px] leading-relaxed font-[var(--font-mono)] text-[var(--code-text)]">
          {children}
        </code>
      </pre>
    </div>
  );
}
