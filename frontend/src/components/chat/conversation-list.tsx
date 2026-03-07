'use client';

import { useMemo } from 'react';
import { useChatStore, Conversation } from '@/stores/chat-store';
import { ConversationItem } from './conversation-item';

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

  for (const conv of conversations) {
    const t = conv.updatedAt;
    if (t >= todayMs) groups['Hoje'].push(conv);
    else if (t >= yesterdayMs) groups['Ontem'].push(conv);
    else if (t >= weekMs) groups['Ultimos 7 dias'].push(conv);
    else if (t >= monthMs) groups['Ultimos 30 dias'].push(conv);
    else groups['Mais antigos'].push(conv);
  }

  return Object.entries(groups)
    .filter(([, convs]) => convs.length > 0)
    .map(([label, conversations]) => ({ label, conversations }));
}

export function ConversationList({ searchTerm }: ConversationListProps) {
  const { conversations, activeConversationId } = useChatStore();

  const filtered = useMemo(
    () =>
      conversations.filter((c) =>
        c.title.toLowerCase().includes(searchTerm.toLowerCase()),
      ),
    [conversations, searchTerm],
  );

  const groups = useMemo(() => groupByDate(filtered), [filtered]);

  if (filtered.length === 0) {
    return (
      <div className="px-3 py-8 text-center">
        <p className="text-[13px] text-[var(--subtle-foreground)]">
          {searchTerm ? 'Nenhuma conversa encontrada.' : 'Nenhuma conversa ainda.'}
        </p>
      </div>
    );
  }

  return (
    <div className="py-0.5">
      {groups.map((group) => (
        <div key={group.label} className="mb-1">
          <p className="px-4 py-1.5 text-[11px] font-medium text-[var(--subtle-foreground)]">
            {group.label}
          </p>
          <div className="space-y-0.5">
            {group.conversations.map((conv) => (
              <ConversationItem
                key={conv.id}
                conversation={conv}
                isActive={conv.id === activeConversationId}
              />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
