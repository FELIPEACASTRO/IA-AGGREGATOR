'use client';

import { useEffect } from 'react';
import { useAuthStore } from '@/stores/auth-store';
import { useRouter } from 'next/navigation';

export default function ChatPage() {
  const { user, isAuthenticated, isLoading, fetchUser } = useAuthStore();
  const router = useRouter();

  useEffect(() => {
    fetchUser();
  }, [fetchUser]);

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push('/login');
    }
  }, [isLoading, isAuthenticated, router]);

  if (isLoading) {
    return (
      <main className="flex min-h-screen items-center justify-center">
        <div className="text-[var(--muted-foreground)]">Carregando...</div>
      </main>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return (
    <main className="flex min-h-screen flex-col">
      {/* Header */}
      <header className="flex items-center justify-between px-6 py-4 border-b border-[var(--border)]">
        <h1 className="text-xl font-bold">IA Aggregator</h1>
        <div className="flex items-center gap-4">
          <span className="text-sm text-[var(--muted-foreground)]">
            {user?.fullName}
          </span>
          <button
            onClick={() => {
              useAuthStore.getState().logout();
              router.push('/login');
            }}
            className="text-sm text-[var(--destructive)] hover:underline"
          >
            Sair
          </button>
        </div>
      </header>

      {/* Chat area */}
      <div className="flex flex-1">
        {/* Sidebar */}
        <aside className="w-64 border-r border-[var(--border)] p-4 hidden md:block">
          <button className="w-full py-2 px-4 bg-[var(--primary)] text-[var(--primary-foreground)] rounded-lg font-medium hover:opacity-90 transition text-sm">
            Nova Conversa
          </button>
          <div className="mt-4 text-sm text-[var(--muted-foreground)]">
            Nenhuma conversa ainda
          </div>
        </aside>

        {/* Main chat */}
        <div className="flex-1 flex flex-col items-center justify-center p-8">
          <div className="text-center max-w-md">
            <h2 className="text-2xl font-bold mb-2">
              Olá, {user?.fullName?.split(' ')[0]}!
            </h2>
            <p className="text-[var(--muted-foreground)] mb-6">
              Selecione um modelo de IA e comece uma conversa.
            </p>
            <div className="grid grid-cols-2 gap-3">
              {['GPT-4o', 'Claude 3.5', 'Gemini Pro', 'Llama 3'].map(
                (model) => (
                  <button
                    key={model}
                    className="p-3 border border-[var(--border)] rounded-lg text-sm hover:bg-[var(--secondary)] transition"
                    disabled
                  >
                    {model}
                  </button>
                )
              )}
            </div>
            <p className="text-xs text-[var(--muted-foreground)] mt-4">
              Integração com provedores de IA em breve
            </p>
          </div>
        </div>
      </div>
    </main>
  );
}
