'use client';

import { ChatMessage } from '@/stores/chat-store';
import { Avatar } from '@/components/ui/avatar';
import { MessageMarkdown } from '@/components/chat/message-markdown';
import { MessageActions } from '@/components/chat/message-actions';

interface MessageBubbleProps {
  message: ChatMessage;
  onRetry?: () => void;
  onFeedback?: (type: 'up' | 'down') => void;
  feedback?: 'up' | 'down' | null;
}

export function MessageBubble({ message, onRetry, onFeedback, feedback }: MessageBubbleProps) {
  if (message.role === 'user') {
    return (
      <div className="group flex justify-end">
        <div className="max-w-[80%] md:max-w-none" style={{ maxWidth: '75ch' }}>
          <div className="rounded-[12px] bg-[var(--user-bubble)] px-4 py-3">
            <p className="whitespace-pre-wrap font-[var(--font-chat-ui)] text-[16px] leading-6 text-[var(--text-100)]">
              {message.content}
            </p>
          </div>
          <MessageActions role="user" content={message.content} />
        </div>
      </div>
    );
  }

  if (message.role === 'error') {
    return (
      <div className="flex gap-3">
        <Avatar name="!" size="sm" className="mt-0.5 border-transparent bg-[var(--destructive)] text-white" />
        <div className="min-w-0 flex-1">
          <div className="rounded-[var(--radius-lg)] border border-[var(--destructive)]/20 bg-[var(--destructive-light)] px-4 py-3">
            <p className="text-[14px] text-[var(--destructive)]">{message.content}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="group flex gap-3">
      <Avatar name="L" size="sm" accent className="mt-0.5 shrink-0" />
      <div className="min-w-0 flex-1">
        <p className="mb-2 text-[11px] uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Lume</p>
        <MessageMarkdown content={message.content} />

        {message.modelUsed || message.providerUsed || message.agentUsed ? (
          <div className="mt-3 flex flex-wrap gap-1.5">
            {message.modelUsed ? (
              <span className="inline-flex items-center rounded-[var(--radius-full)] bg-[var(--surface-hover)] px-2 py-0.5 text-[11px] text-[var(--muted-foreground)]">
                {message.modelUsed}
              </span>
            ) : null}
            {message.providerUsed ? (
              <span className="inline-flex items-center rounded-[var(--radius-full)] bg-[var(--surface-hover)] px-2 py-0.5 text-[11px] text-[var(--muted-foreground)]">
                {message.providerUsed}
              </span>
            ) : null}
            {message.agentUsed ? (
              <span className="inline-flex items-center rounded-[var(--radius-full)] bg-[var(--surface-hover)] px-2 py-0.5 text-[11px] text-[var(--muted-foreground)]">
                {message.agentUsed}
                {message.agentVersion ? ` ${message.agentVersion}` : ''}
              </span>
            ) : null}
            {message.fallbackUsed ? (
              <span className="inline-flex items-center rounded-[var(--radius-full)] bg-[var(--warning-light)] px-2 py-0.5 text-[11px] text-[var(--warning)]">
                fallback
              </span>
            ) : null}
          </div>
        ) : null}

        <MessageActions
          role="assistant"
          content={message.content}
          onRetry={onRetry}
          onFeedback={onFeedback}
          feedback={feedback}
        />
      </div>
    </div>
  );
}
