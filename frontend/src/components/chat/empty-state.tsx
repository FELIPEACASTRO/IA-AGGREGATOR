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

/* Claude.ai sparkle/asterisk icon in terracotta */
function SparkleIcon({ className }: { className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="32"
      height="32"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      <line x1="12" y1="2" x2="12" y2="22" />
      <line x1="2" y1="12" x2="22" y2="12" />
      <line x1="4.93" y1="4.93" x2="19.07" y2="19.07" />
      <line x1="19.07" y1="4.93" x2="4.93" y2="19.07" />
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

        {/* Greeting - Claude exact: font-weight 290, text-text-200, sparkle icon */}
        <h1
          className="text-[var(--foreground-secondary)] w-full text-center flex items-center justify-center gap-2"
          style={{
            lineHeight: 1.5,
            fontSize: 'clamp(1.875rem, 1.2rem + 2vw, 2.5rem)',
            fontWeight: 290,
            letterSpacing: 'normal',
          }}
        >
          <SparkleIcon className="text-[var(--accent)] shrink-0" />
          {getGreeting()}, {firstName}
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

              {/* Bottom controls - Claude exact: + left, model selector + controls right */}
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

          {/* Quick action chips - Claude exact: 14px, h-8, rounded-[8px], border 0.8px */}
          <div className="mt-4 flex flex-wrap items-center justify-center gap-2">
            {quickActions.map(({ icon: Icon, label }) => (
              <button
                key={label}
                className="inline-flex items-center gap-1.5 h-8 px-2.5 rounded-[8px] text-[14px] text-[var(--foreground-secondary)] bg-[var(--background)] hover:bg-[var(--surface-hover)] transition-colors"
                style={{ border: '0.8px solid hsla(var(--border-200) / 0.15)' }}
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
