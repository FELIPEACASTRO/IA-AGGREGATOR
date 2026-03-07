'use client';

import { ReactNode, useState, useEffect, useCallback } from 'react';
import { cn } from '@/lib/cn';
import { PanelLeft } from 'lucide-react';
import { useChatStore } from '@/stores/chat-store';

interface ChatLayoutProps {
  sidebar: ReactNode;
  children: ReactNode;
}

const SIDEBAR_KEY = 'lume-sidebar-collapsed';

export function ChatLayout({ sidebar, children }: ChatLayoutProps) {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [mobileOpen, setMobileOpen] = useState(false);
  const { createConversation } = useChatStore();

  useEffect(() => {
    const saved = localStorage.getItem(SIDEBAR_KEY);
    if (saved === 'false') setSidebarOpen(false);
  }, []);

  const toggleSidebar = useCallback(() => {
    setSidebarOpen((prev) => {
      const next = !prev;
      localStorage.setItem(SIDEBAR_KEY, String(next));
      return next;
    });
  }, []);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'b') {
        e.preventDefault();
        toggleSidebar();
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [toggleSidebar]);

  useEffect(() => {
    if (!mobileOpen) return;
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setMobileOpen(false);
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
      {/* Desktop sidebar - Claude exact: border-r-0.5, border-border-300, bg-bg-100 */}
      <aside
        className={cn(
          'hidden md:flex flex-col bg-[var(--surface-sidebar)] transition-[width,border-color,background-color,box-shadow] duration-[35ms]',
          sidebarOpen
            ? 'w-[var(--sidebar-width)] border-r border-[var(--border-strong)]'
            : 'w-0 overflow-hidden border-r-0',
        )}
        aria-label="Barra lateral"
      >
        {sidebar}
      </aside>

      {/* Mobile sidebar overlay */}
      {mobileOpen && (
        <>
          <div
            className="fixed inset-0 z-[var(--z-modal)] bg-black/40 md:hidden"
            onClick={() => setMobileOpen(false)}
          />
          <aside
            className="fixed inset-y-0 left-0 z-[calc(var(--z-modal)+1)] flex w-[var(--sidebar-width)] flex-col border-r border-[var(--border-strong)] bg-[var(--surface-sidebar)] shadow-lg md:hidden"
            aria-label="Barra lateral"
          >
            {sidebar}
          </aside>
        </>
      )}

      {/* Main content */}
      <div className="flex flex-1 flex-col min-w-0">
        {/* Top bar - Claude exact: minimal, just toggle + optional new-chat */}
        <div className="flex items-center h-11 px-3 bg-[var(--background)]">
          <button
            onClick={() => {
              if (window.innerWidth < 768) {
                setMobileOpen(true);
              } else {
                toggleSidebar();
              }
            }}
            className="flex h-8 w-8 items-center justify-center rounded-lg text-[var(--muted-foreground)] hover:bg-[var(--surface-active)] hover:text-[var(--foreground)] transition-colors active:scale-95"
            aria-label="Toggle sidebar"
          >
            <PanelLeft className="h-4 w-4" />
          </button>

          {/* New chat button when sidebar is hidden */}
          {!sidebarOpen && (
            <button
              onClick={() => createConversation()}
              className="ml-1 flex h-8 w-8 items-center justify-center rounded-lg text-[var(--muted-foreground)] hover:bg-[var(--surface-active)] hover:text-[var(--foreground)] transition-colors active:scale-95"
              aria-label="Novo bate-papo"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M12 20h9" />
                <path d="M16.376 3.622a1 1 0 0 1 3.002 3.002L7.368 18.635a2 2 0 0 1-.855.506l-2.872.838a.5.5 0 0 1-.62-.62l.838-2.872a2 2 0 0 1 .506-.854z" />
              </svg>
            </button>
          )}
        </div>

        {/* Chat content */}
        <div className="flex flex-1 flex-col min-h-0 overflow-hidden">
          {children}
        </div>
      </div>
    </div>
  );
}
