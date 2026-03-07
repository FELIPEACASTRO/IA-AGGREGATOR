'use client';

import { ChatMessage } from '@/stores/chat-store';
import { Avatar } from '@/components/ui/avatar';
import { MessageMarkdown } from './message-markdown';
import { MessageActions } from './message-actions';

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
        <div className="max-w-[80%] md:max-w-[70%]">
          <div className="rounded-2xl rounded-br-md bg-[var(--user-bubble)] px-4 py-3">
            <p className="text-[15px] text-[var(--foreground)] whitespace-pre-wrap leading-relaxed">
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
        <Avatar name="!" size="sm" className="mt-0.5 bg-[var(--destructive)] text-white" />
        <div className="flex-1 min-w-0">
          <div className="rounded-2xl rounded-bl-md border border-[var(--destructive)]/20 bg-[var(--destructive-light)] px-4 py-3">
            <p className="text-[14px] text-[var(--destructive)]">{message.content}</p>
          </div>
        </div>
      </div>
    );
  }

  // Assistant message
  return (
    <div className="group flex gap-3">
      <Avatar name="L" size="sm" accent className="mt-0.5 shrink-0" />
      <div className="flex-1 min-w-0">
        <MessageMarkdown content={message.content} />

        {/* Model metadata */}
        {(message.modelUsed || message.providerUsed) && (
          <div className="mt-2 flex flex-wrap gap-1.5">
            {message.modelUsed && (
              <span className="inline-flex items-center rounded-[var(--radius-full)] bg-[var(--surface-hover)] px-2 py-0.5 text-[11px] text-[var(--muted-foreground)]">
                {message.modelUsed}
              </span>
            )}
            {message.providerUsed && (
              <span className="inline-flex items-center rounded-[var(--radius-full)] bg-[var(--surface-hover)] px-2 py-0.5 text-[11px] text-[var(--muted-foreground)]">
                {message.providerUsed}
              </span>
            )}
            {message.fallbackUsed && (
              <span className="inline-flex items-center rounded-[var(--radius-full)] bg-[var(--warning-light)] px-2 py-0.5 text-[11px] text-[var(--warning)]">
                fallback
              </span>
            )}
          </div>
        )}

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
