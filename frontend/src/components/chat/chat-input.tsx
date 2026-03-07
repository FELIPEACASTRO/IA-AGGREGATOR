'use client';

import { useRef, useState, useCallback } from 'react';
import { useChatStore } from '@/stores/chat-store';
import { cn } from '@/lib/cn';
import { createPerfTimer, trackEvent } from '@/lib/analytics';
import { ModelSelector } from './model-selector';
import { ArrowUp, Square, Plus } from 'lucide-react';

interface ChatInputProps {
  className?: string;
}

export function ChatInput({ className }: ChatInputProps) {
  const {
    isSending,
    activeConversationId,
    selectedModel,
    sendMessage,
    stopGenerating,
    createConversation,
  } = useChatStore();

  const [input, setInput] = useState('');
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  const handleSend = useCallback(async () => {
    const trimmed = input.trim();
    if (!trimmed || isSending) return;

    const timer = createPerfTimer();
    trackEvent('chat_send_start', { model: selectedModel, promptLength: trimmed.length });

    if (!activeConversationId) {
      createConversation();
    }

    setInput('');

    if (inputRef.current) {
      inputRef.current.style.height = 'auto';
    }

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
  }, [input, isSending, selectedModel, activeConversationId, sendMessage, createConversation]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInput(e.target.value);
    const el = e.target;
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 200)}px`;
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

  // Expose setInput for external use (suggestion clicks)
  const setInputExternal = useCallback((text: string) => {
    setInput(text);
    inputRef.current?.focus();
  }, []);

  // Attach to window for parent component access
  if (typeof window !== 'undefined') {
    (window as unknown as Record<string, unknown>).__chatSetInput = setInputExternal;
  }

  return (
    <div className={cn('bg-[var(--background)] px-4 py-3', className)}>
      <div className="mx-auto w-full max-w-[var(--chat-max-width)]">
        {/* Input box - Claude exact: rounded-[20px], transparent border, shadow system */}
        <div
          ref={containerRef}
          className="flex flex-col bg-[var(--input-bg)] items-stretch transition-all duration-200 relative rounded-[20px] cursor-text border border-transparent"
          style={{ boxShadow: 'var(--input-shadow)' }}
          onClick={() => inputRef.current?.focus()}
          onMouseEnter={() => {
            if (!containerRef.current?.contains(document.activeElement)) {
              updateShadow('hover');
            }
          }}
          onMouseLeave={() => {
            if (!containerRef.current?.contains(document.activeElement)) {
              updateShadow('default');
            }
          }}
          onFocusCapture={() => updateShadow('focus')}
          onBlurCapture={(e) => {
            if (!containerRef.current?.contains(e.relatedTarget)) {
              updateShadow('default');
            }
          }}
        >
          {/* Inner content: m-3.5 gap-3 (Claude exact) */}
          <div className="flex flex-col m-3.5 gap-3">
            {/* Textarea wrapper */}
            <div className="relative">
              <div className="w-full overflow-y-auto break-words max-h-96 min-h-[1.5rem] pl-1.5 pt-0.5">
                <textarea
                  ref={inputRef}
                  value={input}
                  onChange={handleInput}
                  onKeyDown={handleKeyDown}
                  rows={1}
                  placeholder="Envie uma mensagem..."
                  disabled={isSending}
                  className="block w-full resize-none bg-transparent text-[var(--foreground)] placeholder:text-[var(--subtle-foreground)] focus:outline-none disabled:opacity-50"
                  style={{
                    maxHeight: '200px',
                    minHeight: '24px',
                    fontSize: '16px',
                    lineHeight: '1.5',
                    outline: 'none',
                  }}
                />
              </div>
            </div>

            {/* Bottom controls */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-1">
                <button
                  type="button"
                  className="flex h-8 w-8 items-center justify-center rounded-lg text-[var(--muted-foreground)] hover:bg-[var(--surface-hover)] hover:text-[var(--foreground)] transition-colors"
                  aria-label="Mais opcoes"
                >
                  <Plus className="h-[18px] w-[18px]" />
                </button>
              </div>

              <div className="flex items-center gap-2">
                <ModelSelector />

                {isSending ? (
                  <button
                    type="button"
                    onClick={() => {
                      stopGenerating();
                      trackEvent('chat_stop_generation');
                    }}
                    className="flex h-8 w-8 items-center justify-center rounded-full bg-[var(--primary)] text-[var(--primary-foreground)] transition-all hover:opacity-80"
                    aria-label="Parar geracao"
                  >
                    <Square className="h-3.5 w-3.5" />
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={handleSend}
                    disabled={!input.trim()}
                    className={cn(
                      'flex h-8 w-8 items-center justify-center rounded-full transition-all',
                      input.trim()
                        ? 'bg-[var(--primary)] text-[var(--primary-foreground)] hover:opacity-80'
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
