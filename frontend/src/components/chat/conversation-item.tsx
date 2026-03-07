'use client';

import { useState } from 'react';
import { useChatStore, Conversation } from '@/stores/chat-store';
import { cn } from '@/lib/cn';
import { toast } from '@/stores/toast-store';
import { MoreHorizontal, Pencil, Trash2 } from 'lucide-react';

interface ConversationItemProps {
  conversation: Conversation;
  isActive: boolean;
}

export function ConversationItem({ conversation, isActive }: ConversationItemProps) {
  const [menuOpen, setMenuOpen] = useState(false);
  const { setActiveConversation, renameConversation, deleteConversation } = useChatStore();

  const handleSelect = () => {
    setActiveConversation(conversation.id);
  };

  const handleRename = () => {
    setMenuOpen(false);
    const title = window.prompt('Renomear conversa:', conversation.title);
    if (title?.trim()) {
      renameConversation(conversation.id, title.trim());
    }
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
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          handleSelect();
        }
      }}
      className={cn(
        'group relative flex items-center gap-2 py-1.5 rounded-lg px-4 cursor-pointer select-none transition-colors duration-75 overflow-hidden active:bg-[var(--surface-active)] active:scale-[1.0]',
        isActive
          ? 'bg-[var(--surface-hover)] text-[var(--foreground)]'
          : 'text-[var(--foreground-secondary)] hover:bg-[var(--surface-hover)]',
      )}
    >
      <p className="flex-1 truncate text-[13px]">{conversation.title}</p>

      {/* More menu trigger */}
      <div className="relative shrink-0">
        <button
          onClick={(e) => {
            e.stopPropagation();
            setMenuOpen((v) => !v);
          }}
          className={cn(
            'flex h-6 w-6 items-center justify-center rounded-[var(--radius-sm)] text-[var(--muted-foreground)] transition-all',
            'opacity-0 group-hover:opacity-100 hover:bg-[var(--surface-active)] hover:text-[var(--foreground)]',
            isActive && 'opacity-100',
          )}
          aria-label="Opcoes"
        >
          <MoreHorizontal className="h-3.5 w-3.5" />
        </button>

        {menuOpen && (
          <>
            <div
              className="fixed inset-0 z-40"
              onClick={(e) => {
                e.stopPropagation();
                setMenuOpen(false);
              }}
            />
            <div className="absolute right-0 top-7 z-50 min-w-[140px] overflow-hidden rounded-lg border border-[var(--border)] bg-[var(--surface)] shadow-[var(--shadow-lg)]">
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  handleRename();
                }}
                className="flex w-full items-center gap-2 px-3 py-2 text-[13px] text-[var(--foreground)] hover:bg-[var(--surface-hover)] transition-colors"
              >
                <Pencil className="h-3.5 w-3.5" /> Renomear
              </button>
              <div className="h-px bg-[var(--border)]" />
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  handleDelete();
                }}
                className="flex w-full items-center gap-2 px-3 py-2 text-[13px] text-[var(--destructive)] hover:bg-[var(--surface-hover)] transition-colors"
              >
                <Trash2 className="h-3.5 w-3.5" /> Excluir
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
