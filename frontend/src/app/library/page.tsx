'use client';

import { AppShell } from '@/components/app/app-shell';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useState } from 'react';
import { useChatStore } from '@/stores/chat-store';
import { useRouter } from 'next/navigation';
import { toast } from '@/stores/toast-store';
import { trackEvent } from '@/lib/analytics';

export default function LibraryPage() {
  const [query, setQuery] = useState('');
  const router = useRouter();
  const { conversations, setActiveConversation, toggleConversationPinned } = useChatStore();

  const filtered = conversations.filter((conversation) =>
    conversation.title.toLowerCase().includes(query.toLowerCase())
  );

  return (
    <AppShell
      title="Biblioteca"
      subtitle="Busque e retome conversas importantes"
      headerActions={
        <Input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Buscar por título..."
          className="w-64"
          aria-label="Buscar na biblioteca"
        />
      }
    >
      <div className="p-6 space-y-3">
        {filtered.length === 0 ? (
          <p className="rounded-lg border border-[var(--border)] p-4 text-sm text-[var(--muted-foreground)]">
            Nenhuma conversa encontrada para esta busca.
          </p>
        ) : (
          filtered.map((conversation) => (
            <article key={conversation.id} className="rounded-xl border border-[var(--border)] p-4">
              <h3 className="font-medium">{conversation.title}</h3>
              <p className="mt-1 text-sm text-[var(--muted-foreground)]">
                Modelo: {conversation.model} · Atualizado em{' '}
                {new Date(conversation.updatedAt).toLocaleString('pt-BR')}
              </p>
              <div className="mt-3 flex items-center gap-2">
                <Button
                  variant="secondary"
                  onClick={() => {
                    setActiveConversation(conversation.id);
                    trackEvent('library_open_conversation', { conversationId: conversation.id });
                    router.push('/chat');
                    toast.success('Conversa aberta', `"${conversation.title}" carregada no chat.`);
                  }}
                >
                  Abrir
                </Button>
                <Button
                  variant="ghost"
                  onClick={() => {
                    toggleConversationPinned(conversation.id);
                    trackEvent('library_toggle_pin', {
                      conversationId: conversation.id,
                      pinned: !conversation.pinned,
                    });
                    toast.info(
                      conversation.pinned ? 'Conversa desafixada' : 'Conversa fixada',
                      conversation.title
                    );
                  }}
                >
                  {conversation.pinned ? 'Desafixar' : 'Fixar'}
                </Button>
              </div>
            </article>
          ))
        )}
      </div>
    </AppShell>
  );
}
