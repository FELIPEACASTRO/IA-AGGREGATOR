'use client';

import { useRef, useState, useCallback } from 'react';
import { useAuthStore } from '@/stores/auth-store';
import { useChatStore } from '@/stores/chat-store';
import { createPerfTimer, trackEvent } from '@/lib/analytics';
import { ModelSelector } from './model-selector';
import { cn } from '@/lib/cn';
import { ArrowUp, Paperclip } from 'lucide-react';

function getGreeting(): string {
  const hour = new Date().getHours();
  if (hour < 12) return 'Bom dia';
  if (hour < 18) return 'Boa tarde';
  return 'Boa noite';
}

export function EmptyState() {
  const { user } = useAuthStore();
  const {
    isSending,
    selectedModel,
    sendMessage,
    createConversation,
  } = useChatStore();

  const [input, setInput] = useState('');
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  const firstName = user?.fullName?.split(' ')[0] || 'usuario';

  const handleSend = useCallback(async () => {
    const trimmed = input.trim();
    if (!trimmed || isSending) return;

    const timer = createPerfTimer();
    trackEvent('chat_send_start', { model: selectedModel, promptLength: trimmed.length });

    createConversation();
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
  }, [input, isSending, selectedModel, sendMessage, createConversation]);

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

  return (
    <div className="flex flex-1 flex-col items-center justify-center px-4">
      {/* Claude-exact layout: mx-auto, gap-7, max-w-2xl */}
      <div className="mx-auto flex w-full flex-col items-center gap-7 max-w-2xl">

        {/* Model pill (above greeting like Claude Web) */}
        <div className="inline-flex items-center gap-1.5 rounded-lg h-8 pl-2 pr-2.5 text-center text-[13px] bg-[var(--surface-active)] text-[var(--subtle-foreground)] select-none">
          <ModelSelector />
        </div>

        {/* Greeting - Claude exact: text-text-200, font-display, clamp font-size, line-height 1.5 */}
        <h1
          className="text-[var(--foreground-secondary)] w-full text-center font-semibold"
          style={{
            lineHeight: 1.5,
            fontSize: 'clamp(1.875rem, 1.2rem + 2vw, 2.5rem)',
            letterSpacing: '-0.02em',
          }}
        >
          {getGreeting()}, {firstName}.
        </h1>

        {/* Input box - Claude exact: rounded-[20px], border-transparent, shadow system */}
        <div className="w-full">
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
                <div className="w-full overflow-y-auto break-words max-h-96 min-h-[3rem] pl-1.5 pt-1.5">
                  <textarea
                    ref={inputRef}
                    value={input}
                    onChange={handleInput}
                    onKeyDown={handleKeyDown}
                    rows={2}
                    placeholder="Como posso ajudar voce hoje?"
                    disabled={isSending}
                    className="block w-full resize-none bg-transparent text-[var(--foreground)] placeholder:text-[var(--subtle-foreground)] focus:outline-none disabled:opacity-50"
                    style={{
                      maxHeight: '200px',
                      minHeight: '48px',
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
                    aria-label="Anexar arquivo"
                  >
                    <Paperclip className="h-[18px] w-[18px]" />
                  </button>
                </div>

                <div className="flex items-center gap-2">
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
                </div>
              </div>
            </div>
          </div>

          {/* Disclaimer */}
          <p className="mt-3 text-center text-[12px] text-[var(--subtle-foreground)]">
            Lume pode cometer erros. Verifique informacoes importantes.
          </p>
        </div>
      </div>
    </div>
  );
}
