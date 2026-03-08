'use client';

import { ReactNode, useCallback, useEffect, useState } from 'react';
import { PanelLeft, Search, SquarePen } from 'lucide-react';
import { useChatStore } from '@/stores/chat-store';
import { LumeLogo } from '@/components/ui/lume-logo';
import { openCommandPalette } from '@/components/ui/command-palette';
import { cn } from '@/lib/cn';

interface ChatLayoutProps {
  sidebar: ReactNode;
  children: ReactNode;
}

const SIDEBAR_KEY = 'lume-sidebar-collapsed';

export function ChatLayout({ sidebar, children }: ChatLayoutProps) {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [mobileOpen, setMobileOpen] = useState(false);
  const createConversation = useChatStore((state) => state.createConversation);

  useEffect(() => {
    const saved = localStorage.getItem(SIDEBAR_KEY);
    if (saved === 'false') setSidebarOpen(false);
  }, []);

  const toggleSidebar = useCallback(() => {
    setSidebarOpen((previous) => {
      const next = !previous;
      localStorage.setItem(SIDEBAR_KEY, String(next));
      return next;
    });
  }, []);

  useEffect(() => {
    const handler = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'b') {
        event.preventDefault();
        toggleSidebar();
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [toggleSidebar]);

  useEffect(() => {
    if (!mobileOpen) return;
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setMobileOpen(false);
    };
    document.body.style.overflow = 'hidden';
    window.addEventListener('keydown', handleEscape);
    return () => {
      document.body.style.overflow = '';
      window.removeEventListener('keydown', handleEscape);
    };
  }, [mobileOpen]);

  return (
    <div className="flex h-screen overflow-hidden bg-[var(--background)]">
      <aside
        className={cn(
          'hidden border-r border-[var(--border)] bg-[var(--surface-sidebar)] transition-[width] duration-150 md:flex md:flex-col',
          sidebarOpen ? 'w-[var(--sidebar-width)]' : 'w-0 overflow-hidden border-r-0',
        )}
        aria-label="Barra lateral"
      >
        {sidebar}
      </aside>

      {mobileOpen ? (
        <>
          <div
            className="fixed inset-0 z-[var(--z-modal)] bg-black/40 md:hidden"
            onClick={() => setMobileOpen(false)}
          />
          <aside
            className="fixed inset-y-0 left-0 z-[calc(var(--z-modal)+1)] flex w-[var(--sidebar-width)] flex-col border-r border-[var(--border)] bg-[var(--surface-sidebar)] shadow-[var(--shadow-lg)] md:hidden"
            aria-label="Barra lateral"
          >
            {sidebar}
          </aside>
        </>
      ) : null}

      <div className="flex min-w-0 flex-1 flex-col">
        <header
          className="flex h-14 items-center justify-between border-b border-[var(--border)] px-3"
          style={{ backgroundColor: 'color-mix(in srgb, var(--background) 88%, transparent)' }}
        >
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => {
                if (window.innerWidth < 768) setMobileOpen(true);
                else toggleSidebar();
              }}
              className="inline-flex h-9 w-9 items-center justify-center rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface)] text-[var(--foreground)]"
              aria-label="Alternar sidebar"
            >
              <PanelLeft className="h-4 w-4" />
            </button>
            <LumeLogo compact textClassName="hidden" />
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => openCommandPalette('consumer')}
              className="inline-flex h-9 items-center gap-2 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface)] px-3 text-[12px] text-[var(--muted-foreground)]"
            >
              <Search className="h-4 w-4" />
              <span className="hidden sm:inline">Ctrl+K</span>
            </button>
            {!sidebarOpen ? (
              <button
                type="button"
                onClick={() => void createConversation()}
                className="inline-flex h-9 w-9 items-center justify-center rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface)] text-[var(--foreground)]"
                aria-label="Nova conversa"
              >
                <SquarePen className="h-4 w-4" />
              </button>
            ) : null}
          </div>
        </header>

        <div className="flex min-h-0 flex-1 flex-col overflow-hidden">{children}</div>
      </div>
    </div>
  );
}
