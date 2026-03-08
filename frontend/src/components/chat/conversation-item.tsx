'use client';

import { useState } from 'react';
import { MoreHorizontal, Pencil, Trash2 } from 'lucide-react';
import { Conversation, useChatStore } from '@/stores/chat-store';
import { cn } from '@/lib/cn';
import { toast } from '@/stores/toast-store';

interface ConversationItemProps {
  conversation: Conversation;
  isActive: boolean;
}

export function ConversationItem({ conversation, isActive }: ConversationItemProps) {
  const [menuOpen, setMenuOpen] = useState(false);
  const setActiveConversation = useChatStore((state) => state.setActiveConversation);
  const renameConversation = useChatStore((state) => state.renameConversation);
  const deleteConversation = useChatStore((state) => state.deleteConversation);

  const handleSelect = () => {
    setActiveConversation(conversation.id);
  };

  const handleRename = () => {
    setMenuOpen(false);
    const title = window.prompt('Renomear conversa:', conversation.title);
    if (title?.trim()) renameConversation(conversation.id, title.trim());
  };

  const handleDelete = () => {
    setMenuOpen(false);
    if (window.confirm('Excluir esta conversa?')) {
      deleteConversation(conversation.id);
      toast.success('Conversa excluida');
    }
  };

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={handleSelect}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          handleSelect();
        }
      }}
      className={cn(
        'group relative flex items-center gap-2 rounded-[var(--radius-md)] px-4 py-2 transition-colors',
        isActive
          ? 'bg-[var(--surface-active)] text-[var(--foreground)]'
          : 'text-[var(--foreground-secondary)] hover:bg-[var(--surface-hover)]',
      )}
    >
      <div className="min-w-0 flex-1">
        <p className="truncate text-[13px]">{conversation.title}</p>
        <p className="mt-0.5 truncate text-[11px] text-[var(--subtle-foreground)]">
          {new Date(conversation.updatedAt).toLocaleDateString('pt-BR')}
        </p>
      </div>

      <div className="relative shrink-0">
        <button
          type="button"
          onClick={(event) => {
            event.stopPropagation();
            setMenuOpen((value) => !value);
          }}
          className={cn(
            'inline-flex h-7 w-7 items-center justify-center rounded-[var(--radius-sm)] text-[var(--muted-foreground)] transition-all',
            'opacity-0 group-hover:opacity-100 hover:bg-[var(--surface-hover)] hover:text-[var(--foreground)]',
            isActive && 'opacity-100',
          )}
          aria-label="Opcoes da conversa"
        >
          <MoreHorizontal className="h-4 w-4" />
        </button>

        {menuOpen ? (
          <>
            <div className="fixed inset-0 z-40" onClick={() => setMenuOpen(false)} />
            <div className="absolute right-0 top-8 z-50 min-w-[160px] overflow-hidden rounded-[var(--radius-lg)] border border-[var(--border-strong)] bg-[var(--surface)] shadow-[var(--shadow-dropdown)]">
              <button
                type="button"
                onClick={(event) => {
                  event.stopPropagation();
                  handleRename();
                }}
                className="flex h-10 w-full items-center gap-2 px-3 text-[13px] text-[var(--foreground)] hover:bg-[var(--surface-hover)]"
              >
                <Pencil className="h-3.5 w-3.5" />
                Renomear
              </button>
              <button
                type="button"
                onClick={(event) => {
                  event.stopPropagation();
                  handleDelete();
                }}
                className="flex h-10 w-full items-center gap-2 px-3 text-[13px] text-[var(--destructive)] hover:bg-[var(--surface-hover)]"
              >
                <Trash2 className="h-3.5 w-3.5" />
                Excluir
              </button>
            </div>
          </>
        ) : null}
      </div>
    </div>
  );
}
