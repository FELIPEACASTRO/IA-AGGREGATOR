'use client';

import { useRef, useState, useCallback } from 'react';
import { useAuthStore } from '@/stores/auth-store';
import { useChatStore } from '@/stores/chat-store';
import { createPerfTimer, trackEvent } from '@/lib/analytics';
import { ModelSelector } from './model-selector';
import { cn } from '@/lib/cn';
import { ArrowUp, Plus, Pen, GraduationCap, Code2, Home, Lightbulb } from 'lucide-react';

function getGreeting(): string {
  const hour = new Date().getHours();
  if (hour < 12) return 'Bom dia';
  if (hour < 18) return 'Boa tarde';
  return 'Boa noite';
}

/* Claude.ai sparkle icon — 8-ray radial asterisk, FILLED (not stroke), terracotta accent */
function SparkleIcon({ className }: { className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="28"
      height="28"
      viewBox="0 0 28 28"
      fill="currentColor"
      className={className}
    >
      <path d="M14 0C14.5 0 14.8 0.3 14.8 0.8L14.8 10.1C14.8 10.6 15.2 11 15.6 11.3L15.9 11.4C16 11.5 16.2 11.5 16.3 11.5L17.9 11.2L25.2 9.5C25.7 9.4 26.1 9.6 26.3 10C26.5 10.5 26.3 10.9 25.9 11.2L18 15.8C17.5 16.1 17.3 16.7 17.5 17.2L20.5 25C20.7 25.5 20.5 26 20.1 26.2C19.7 26.5 19.2 26.4 18.9 26L14.6 19.8C14.3 19.4 13.7 19.4 13.4 19.8L9.1 26C8.8 26.4 8.3 26.5 7.9 26.2C7.5 26 7.3 25.5 7.5 25L10.5 17.2C10.7 16.7 10.5 16.1 10 15.8L2.1 11.2C1.7 10.9 1.5 10.5 1.7 10C1.9 9.6 2.3 9.4 2.8 9.5L10.1 11.2L11.7 11.5C11.8 11.5 12 11.5 12.1 11.4L12.4 11.3C12.8 11 13.2 10.6 13.2 10.1L13.2 0.8C13.2 0.3 13.5 0 14 0Z" />
    </svg>
  );
}

const quickActions = [
  { icon: Pen, label: 'Escrever' },
  { icon: GraduationCap, label: 'Aprender' },
  { icon: Code2, label: 'Codigo' },
  { icon: Home, label: 'Assuntos pessoais' },
  { icon: Lightbulb, label: 'Escolha do Lume' },
];

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
      <div className="mx-auto flex w-full flex-col items-center gap-7 max-w-2xl">

        {/* Greeting — Claude exact: serif font, weight ~290, sparkle icon filled */}
        <h1
          className="text-[var(--foreground-secondary)] w-full text-center flex items-center justify-center gap-2"
          style={{
            lineHeight: 1.5,
            fontSize: 'clamp(1.875rem, 1.2rem + 2vw, 2.5rem)',
            fontFamily: "var(--font-display), Georgia, 'Times New Roman', serif",
            fontWeight: 300,
            letterSpacing: 'normal',
          }}
        >
          <SparkleIcon className="text-[var(--accent)] shrink-0" />
          {getGreeting()}, {firstName}
        </h1>

        {/* Input box — Claude exact: rounded-[20px], border 0.8px transparent, shadow system */}
        <div className="w-full">
          <div
            ref={containerRef}
            className="flex flex-col bg-[var(--input-bg)] items-stretch transition-all duration-200 relative rounded-[20px] cursor-text"
            style={{
              boxShadow: 'var(--input-shadow)',
              border: '0.8px solid transparent',
            }}
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

              {/* Bottom controls — Claude exact: + left, model selector + send right */}
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

          {/* Quick action chips — Claude exact: 14px, weight 400, h-8, rounded-[8px], border 0.8px */}
          <div className="mt-4 flex flex-wrap items-center justify-center gap-2">
            {quickActions.map(({ icon: Icon, label }) => (
              <button
                key={label}
                className="inline-flex items-center gap-1.5 h-8 px-[10px] rounded-[8px] text-[14px] font-normal text-[var(--foreground-secondary)] bg-[var(--surface-sidebar)] hover:bg-[var(--surface-hover)] transition-colors"
                style={{ border: '0.8px solid var(--sidebar-border-color)' }}
              >
                <Icon className="h-4 w-4" />
                {label}
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
