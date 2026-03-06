'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';

export default function LogoutPage() {
  const router = useRouter();

  useEffect(() => {
    useAuthStore.getState().logout();
    fetch('/api/auth/logout', {
      method: 'POST',
      credentials: 'include',
    }).finally(() => {
      router.replace('/login');
    });
  }, [router]);

  return (
    <main className="grid min-h-screen place-items-center bg-[var(--background)] text-[var(--foreground)]">
      <p className="text-sm text-[var(--muted-foreground)]">Encerrando sessão...</p>
    </main>
  );
}

