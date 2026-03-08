'use client';

import { useRef, useState, useCallback, useEffect } from 'react';
import { ArrowUp, Plus, Square } from 'lucide-react';
import { useChatStore } from '@/stores/chat-store';
import { cn } from '@/lib/cn';
import { createPerfTimer, trackEvent } from '@/lib/analytics';
import { ModelSelector } from '@/components/chat/model-selector';

interface ChatInputProps {
  className?: string;
}

export function ChatInput({ className }: ChatInputProps) {
  const isSending = useChatStore((state) => state.isSending);
  const selectedModel = useChatStore((state) => state.selectedModel);
  const sendMessage = useChatStore((state) => state.sendMessage);
  const stopGenerating = useChatStore((state) => state.stopGenerating);

  const [input, setInput] = useState('');
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  const handleSend = useCallback(async () => {
    const trimmed = input.trim();
    if (!trimmed || isSending) return;

    const timer = createPerfTimer();
    trackEvent('chat_send_start', { model: selectedModel, promptLength: trimmed.length });

    setInput('');

    if (inputRef.current) inputRef.current.style.height = 'auto';

    try {
      await sendMessage(trimmed);
      trackEvent('chat_send_success', { latencyMs: timer.elapsedMs(), model: selectedModel });
    } catch (err: unknown) {
      trackEvent('chat_send_error', {
        latencyMs: timer.elapsedMs(),
        message: err instanceof Error ? err.message : 'unknown',
      });
    }

    inputRef.current?.focus();
  }, [input, isSending, selectedModel, sendMessage]);

  const handleKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      void handleSend();
    }
  };

  const handleInput = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInput(event.target.value);
    const element = event.target;
    element.style.height = 'auto';
    element.style.height = `${Math.min(element.scrollHeight, 200)}px`;
  };

  const updateShadow = (state: 'default' | 'hover' | 'focus') => {
    if (!containerRef.current) return;
    const map = {
      default: 'var(--input-shadow)',
      hover: 'var(--input-shadow-hover)',
      focus: 'var(--input-shadow-focus)',
    };
    containerRef.current.style.boxShadow = map[state];
  };

  const setInputExternal = useCallback((text: string) => {
    setInput(text);
    inputRef.current?.focus();
  }, []);

  useEffect(() => {
    if (typeof window === 'undefined') return;

    (window as unknown as Record<string, unknown>).__chatSetInput = setInputExternal;
    return () => {
      const target = window as unknown as Record<string, unknown>;
      if (target.__chatSetInput === setInputExternal) delete target.__chatSetInput;
    };
  }, [setInputExternal]);

  return (
    <div
      className={cn('border-t border-[var(--border)] px-4 py-4 backdrop-blur-sm', className)}
      style={{ backgroundColor: 'rgba(38, 38, 36, 0.55)' }}
    >
      <div className="mx-auto w-full max-w-[var(--chat-max-width)]">
        <div
          ref={containerRef}
          className="relative flex cursor-text flex-col rounded-[var(--radius-input)] bg-[var(--input-bg)] transition-all duration-200"
          style={{ boxShadow: 'var(--input-shadow)', border: '0.8px solid transparent' }}
          onClick={() => inputRef.current?.focus()}
          onMouseEnter={() => {
            if (!containerRef.current?.contains(document.activeElement)) updateShadow('hover');
          }}
          onMouseLeave={() => {
            if (!containerRef.current?.contains(document.activeElement)) updateShadow('default');
          }}
          onFocusCapture={() => updateShadow('focus')}
          onBlurCapture={(event) => {
            if (!containerRef.current?.contains(event.relatedTarget)) updateShadow('default');
          }}
        >
          <div className="m-3.5 flex flex-col gap-3">
            <div className="relative">
              <div className="min-h-[1.5rem] max-h-96 w-full overflow-y-auto break-words pl-1.5 pt-0.5">
                <textarea
                  ref={inputRef}
                  value={input}
                  onChange={handleInput}
                  onKeyDown={handleKeyDown}
                  rows={1}
                  placeholder="Responder..."
                  disabled={isSending}
                  className="block w-full resize-none bg-transparent font-[var(--font-chat-ui)] text-[16px] text-[var(--foreground)] placeholder:text-[var(--subtle-foreground)] focus:outline-none disabled:opacity-50"
                  style={{ maxHeight: '200px', minHeight: '24px', lineHeight: '1.5' }}
                />
              </div>
            </div>

            <div className="flex items-center justify-between">
              <button
                type="button"
                className="inline-flex h-8 w-8 items-center justify-center rounded-[var(--radius-md)] text-[var(--muted-foreground)] transition-colors hover:bg-[var(--surface-hover)] hover:text-[var(--foreground)]"
                aria-label="Mais opcoes"
              >
                <Plus className="h-4.5 w-4.5" />
              </button>

              <div className="flex items-center gap-2">
                <ModelSelector />
                {isSending ? (
                  <button
                    type="button"
                    onClick={() => {
                      stopGenerating();
                      trackEvent('chat_stop_generation');
                    }}
                    className="inline-flex h-8 w-8 items-center justify-center rounded-[var(--radius-md)] bg-[var(--surface-hover)] text-[var(--foreground)] transition-colors hover:bg-[var(--surface-active)]"
                    aria-label="Parar geracao"
                  >
                    <Square className="h-3.5 w-3.5" />
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={() => void handleSend()}
                    disabled={!input.trim()}
                    className={cn(
                      'inline-flex h-8 w-8 items-center justify-center rounded-[var(--radius-md)] transition-all',
                      input.trim()
                        ? 'bg-[var(--accent-send)] text-white hover:brightness-110'
                        : 'bg-[var(--surface-hover)] text-[var(--subtle-foreground)] cursor-not-allowed',
                    )}
                    aria-label="Enviar mensagem"
                  >
                    <ArrowUp className="h-4 w-4" strokeWidth={2.5} />
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
