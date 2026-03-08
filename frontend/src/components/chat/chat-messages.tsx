'use client';

import { useRef, useEffect, useState, useCallback } from 'react';
import { ArrowDown } from 'lucide-react';
import { Conversation, useChatStore } from '@/stores/chat-store';
import { MessageBubble } from '@/components/chat/message-bubble';
import { StreamingIndicator } from '@/components/chat/streaming-indicator';

interface ChatMessagesProps {
  conversation: Conversation;
}

export function ChatMessages({ conversation }: ChatMessagesProps) {
  const isSending = useChatStore((state) => state.isSending);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [autoScrollPaused, setAutoScrollPaused] = useState(false);
  const [feedbacks, setFeedbacks] = useState<Record<string, 'up' | 'down'>>({});

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    setAutoScrollPaused(false);
  }, []);

  useEffect(() => {
    if (!autoScrollPaused) messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [conversation.messages, autoScrollPaused]);

  const handleScroll = () => {
    const element = containerRef.current;
    if (!element) return;
    const distanceFromBottom = element.scrollHeight - element.scrollTop - element.clientHeight;
    setAutoScrollPaused(distanceFromBottom > 120);
  };

  const handleRetry = async () => {
    if (isSending) return;
    const last = [...conversation.messages].reverse().find((message) => message.role === 'user');
    if (!last) return;
    await useChatStore.getState().sendMessage(last.content);
  };

  return (
    <div ref={containerRef} className="flex-1 overflow-y-auto" onScroll={handleScroll}>
      <div className="mx-auto w-full max-w-[var(--chat-max-width)] space-y-8 px-4 py-8 md:px-6">
        {conversation.messages.map((message) => (
          <MessageBubble
            key={message.id}
            message={message}
            onRetry={message.role === 'assistant' ? handleRetry : undefined}
            onFeedback={(type) => setFeedbacks((previous) => ({ ...previous, [message.id]: type }))}
            feedback={feedbacks[message.id]}
          />
        ))}

        {isSending ? <StreamingIndicator /> : null}

        <div ref={messagesEndRef} />
      </div>

      {autoScrollPaused ? (
        <div className="sticky bottom-4 z-10 flex justify-center pointer-events-none">
          <button
            type="button"
            onClick={scrollToBottom}
            className="pointer-events-auto inline-flex h-9 items-center gap-1.5 rounded-[var(--radius-full)] border border-[var(--border)] px-3 text-[12px] font-medium text-[var(--text-100)] shadow-[var(--shadow-float)]"
            style={{ backgroundColor: 'rgba(48, 48, 46, 0.8)' }}
          >
            <ArrowDown className="h-3.5 w-3.5" />
            Novas mensagens
          </button>
        </div>
      ) : null}
    </div>
  );
}
