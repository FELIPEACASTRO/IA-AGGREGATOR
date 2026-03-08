'use client';

import { useMemo } from 'react';
import { Conversation, useChatStore } from '@/stores/chat-store';
import { ConversationItem } from '@/components/chat/conversation-item';

interface ConversationListProps {
  searchTerm: string;
}

interface DateGroup {
  label: string;
  conversations: Conversation[];
}

function groupByDate(conversations: Conversation[]): DateGroup[] {
  const oneDay = 86400000;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const todayMs = today.getTime();
  const yesterdayMs = todayMs - oneDay;
  const weekMs = todayMs - 7 * oneDay;
  const monthMs = todayMs - 30 * oneDay;

  const groups: Record<string, Conversation[]> = {
    Hoje: [],
    Ontem: [],
    'Ultimos 7 dias': [],
    'Ultimos 30 dias': [],
    'Mais antigos': [],
  };

  for (const conversation of conversations) {
    const updatedAt = conversation.updatedAt;
    if (updatedAt >= todayMs) groups.Hoje.push(conversation);
    else if (updatedAt >= yesterdayMs) groups.Ontem.push(conversation);
    else if (updatedAt >= weekMs) groups['Ultimos 7 dias'].push(conversation);
    else if (updatedAt >= monthMs) groups['Ultimos 30 dias'].push(conversation);
    else groups['Mais antigos'].push(conversation);
  }

  return Object.entries(groups)
    .filter(([, entries]) => entries.length > 0)
    .map(([label, entries]) => ({ label, conversations: entries }));
}

export function ConversationList({ searchTerm }: ConversationListProps) {
  const rawConversations = useChatStore((state) => state.conversations);
  const activeConversationId = useChatStore((state) => state.activeConversationId);
  const conversations = useMemo(
    () => (Array.isArray(rawConversations) ? rawConversations : []),
    [rawConversations],
  );

  const filtered = useMemo(
    () =>
      conversations.filter((conversation) =>
        conversation.title.toLowerCase().includes(searchTerm.toLowerCase()),
      ),
    [conversations, searchTerm],
  );

  const groups = useMemo(() => groupByDate(filtered), [filtered]);

  if (filtered.length === 0) {
    return (
      <div className="px-4 py-8 text-center">
        <p className="text-[12px] text-[var(--subtle-foreground)]">
          {searchTerm ? 'Nenhuma conversa encontrada.' : 'Nenhuma conversa ainda.'}
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4 pb-3">
      {groups.map((group) => (
        <div key={group.label}>
          <p className="px-4 pb-2 text-[11px] uppercase tracking-[0.12em] text-[var(--subtle-foreground)]">
            {group.label}
          </p>
          <div className="space-y-0.5">
            {group.conversations.map((conversation) => (
              <ConversationItem
                key={conversation.id}
                conversation={conversation}
                isActive={conversation.id === activeConversationId}
              />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
