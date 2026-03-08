'use client';

import { FormEvent, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useAuthStore } from '@/stores/auth-store';
import { toast } from '@/stores/toast-store';

type LoginPayload = {
  success: boolean;
  data?: {
    accessToken?: string;
    refreshToken?: string;
  };
  message?: string;
};

export default function LoginPage() {
  const router = useRouter();
  const fetchUser = useAuthStore((state) => state.fetchUser);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const isLoading = useAuthStore((state) => state.isLoading);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      router.replace('/codex');
    }
  }, [isAuthenticated, isLoading, router]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitting(true);

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
      });

      const payload = (await response.json()) as LoginPayload;
      if (!response.ok || !payload.success) {
        throw new Error(payload.message || 'Falha ao autenticar');
      }

      if (typeof window !== 'undefined') {
        localStorage.setItem('access_token', payload.data?.accessToken || '');
        localStorage.setItem('refresh_token', payload.data?.refreshToken || '');
      }

      await fetchUser();
      router.replace('/codex');
    } catch (error) {
      toast.error('Falha de autenticacao', error instanceof Error ? error.message : 'Tente novamente.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-[var(--background)] px-4">
      <div className="w-full max-w-md rounded-[var(--radius-xl)] border border-[var(--border)] bg-[var(--surface)] p-8 shadow-[var(--shadow-lg)]">
        <div className="flex items-center gap-3">
          <span className="inline-flex h-10 w-10 items-center justify-center rounded-full bg-[var(--accent)] text-white">
            <Sparkles className="h-4 w-4" />
          </span>
          <div>
            <h1 className="text-[22px] font-semibold text-[var(--foreground)]">Entrar no Lume</h1>
            <p className="text-[13px] text-[var(--muted-foreground)]">Use suas credenciais da plataforma para continuar.</p>
          </div>
        </div>

        <form className="mt-8 space-y-4" onSubmit={handleSubmit}>
          <div className="space-y-1.5">
            <label className="text-[13px] font-medium text-[var(--foreground)]" htmlFor="email">
              E-mail
            </label>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="voce@empresa.com"
              required
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-[13px] font-medium text-[var(--foreground)]" htmlFor="password">
              Senha
            </label>
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Sua senha"
              required
            />
          </div>

          <Button className="w-full" type="submit" variant="primary" size="lg" disabled={submitting}>
            {submitting ? 'Entrando...' : 'Entrar'}
          </Button>
        </form>
      </div>
    </main>
  );
}
