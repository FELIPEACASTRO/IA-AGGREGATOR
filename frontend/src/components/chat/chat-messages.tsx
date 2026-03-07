'use client';

import { useRef, useEffect, useState, useCallback } from 'react';
import { useChatStore, Conversation } from '@/stores/chat-store';
import { MessageBubble } from './message-bubble';
import { StreamingIndicator } from './streaming-indicator';
import { ArrowDown } from 'lucide-react';

interface ChatMessagesProps {
  conversation: Conversation;
}

export function ChatMessages({ conversation }: ChatMessagesProps) {
  const { isSending } = useChatStore();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [autoScrollPaused, setAutoScrollPaused] = useState(false);
  const [feedbacks, setFeedbacks] = useState<Record<string, 'up' | 'down'>>({});

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    setAutoScrollPaused(false);
  }, []);

  useEffect(() => {
    if (!autoScrollPaused) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [conversation?.messages, autoScrollPaused]);

  const handleScroll = () => {
    const el = containerRef.current;
    if (!el) return;
    const distanceFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
    setAutoScrollPaused(distanceFromBottom > 100);
  };

  const handleRetry = async () => {
    if (!conversation || isSending) return;
    const last = [...conversation.messages].reverse().find((m) => m.role === 'user');
    if (!last) return;
    await useChatStore.getState().sendMessage(last.content);
  };

  const handleFeedback = (msgId: string, type: 'up' | 'down') => {
    setFeedbacks((prev) => ({ ...prev, [msgId]: type }));
  };

  return (
    <div
      ref={containerRef}
      className="flex-1 overflow-y-auto"
      onScroll={handleScroll}
    >
      <div className="mx-auto w-full max-w-[var(--chat-max-width)] space-y-6 px-4 py-6 md:px-6">
        {conversation.messages.map((msg) => (
          <MessageBubble
            key={msg.id}
            message={msg}
            onRetry={msg.role === 'assistant' ? handleRetry : undefined}
            onFeedback={(type) => handleFeedback(msg.id, type)}
            feedback={feedbacks[msg.id]}
          />
        ))}

        {isSending && <StreamingIndicator />}

        <div ref={messagesEndRef} />
      </div>

      {/* Scroll to bottom button */}
      {autoScrollPaused && (
        <div className="sticky bottom-4 z-10 flex justify-center pointer-events-none">
          <button
            onClick={scrollToBottom}
            className="pointer-events-auto flex items-center gap-1.5 rounded-[var(--radius-full)] border border-[var(--border)] bg-[var(--surface)] px-3 py-1.5 text-[12px] font-medium text-[var(--foreground)] shadow-[var(--shadow-md)] hover:bg-[var(--surface-hover)] transition-colors"
          >
            <ArrowDown className="h-3 w-3" />
            Novas mensagens
          </button>
        </div>
      )}
    </div>
  );
}
