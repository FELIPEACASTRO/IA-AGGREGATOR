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
  const conversations = useChatStore((state) => state.conversations);
  const activeConversationId = useChatStore((state) => state.activeConversationId);
  const createConversation = useChatStore((state) => state.createConversation);
  const loadConversations = useChatStore((state) => state.loadConversations);
  const setActiveConversation = useChatStore((state) => state.setActiveConversation);
  const isLoaded = useChatStore((state) => state.isLoaded);
  const searchParams = useSearchParams();

  const activeConversation = conversations.find((c) => c.id === activeConversationId);
  const hasMessages = activeConversation && activeConversation.messages.length > 0;
  const promptPrefill = searchParams.get('prompt');
  const conversationId = searchParams.get('conversationId');

  useEffect(() => {
    void loadConversations();
  }, [loadConversations]);

  // Ctrl+N for new conversation
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'n') {
        e.preventDefault();
        void createConversation();
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [createConversation]);

  useEffect(() => {
    if (!conversationId) return;
    if (!conversations.some((conversation) => conversation.id === conversationId)) return;
    setActiveConversation(conversationId);
  }, [conversationId, conversations, setActiveConversation]);

  useEffect(() => {
    if (!promptPrefill || typeof window === 'undefined') return;

    const target = window as unknown as {
      __chatSetInput?: (value: string) => void;
    };

    target.__chatSetInput?.(promptPrefill);
  }, [promptPrefill]);

  if (isLoading || !isLoaded) {
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
