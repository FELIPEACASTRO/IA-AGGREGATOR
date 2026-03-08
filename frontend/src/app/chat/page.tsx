'use client';

import { useEffect, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { useChatStore } from '@/stores/chat-store';
import { ChatLayout } from '@/components/chat/chat-layout';
import { ChatSidebar } from '@/components/chat/chat-sidebar';
import { ChatMessages } from '@/components/chat/chat-messages';
import { ChatInput } from '@/components/chat/chat-input';
import { EmptyState } from '@/components/chat/empty-state';

function ChatPageContent() {
  const isLoading = useAuthStore((state) => state.isLoading);
  const { conversations, activeConversationId, createConversation } = useChatStore();
  const searchParams = useSearchParams();

  const activeConversation = conversations.find((c) => c.id === activeConversationId);
  const hasMessages = activeConversation && activeConversation.messages.length > 0;
  const promptPrefill = searchParams.get('prompt');

  // Ctrl+N for new conversation
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'n') {
        e.preventDefault();
        createConversation();
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [createConversation]);

  useEffect(() => {
    if (!promptPrefill || typeof window === 'undefined') return;

    const target = window as unknown as {
      __chatSetInput?: (value: string) => void;
    };

    target.__chatSetInput?.(promptPrefill);
  }, [promptPrefill]);

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[var(--background)]">
        <div className="flex gap-1.5">
          <span className="pulse-dot" />
          <span className="pulse-dot" />
          <span className="pulse-dot" />
        </div>
      </div>
    );
  }

  return (
    <ChatLayout sidebar={<ChatSidebar />}>
      {hasMessages ? (
        <>
          <ChatMessages conversation={activeConversation} />
          <ChatInput />
        </>
      ) : (
        <EmptyState />
      )}
    </ChatLayout>
  );
}

export default function ChatPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center bg-[var(--background)]">
          <div className="flex gap-1.5">
            <span className="pulse-dot" />
            <span className="pulse-dot" />
            <span className="pulse-dot" />
          </div>
        </div>
      }
    >
      <ChatPageContent />
    </Suspense>
  );
}
