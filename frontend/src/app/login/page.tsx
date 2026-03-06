'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';

import { AuthShell } from '@/components/app/auth-shell';
import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Field } from '@/components/ui/form-field';
import { trackEvent } from '@/lib/analytics';
import { useAuthStore } from '@/stores/auth-store';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const login = useAuthStore((state) => state.login);
  const router = useRouter();

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login(email, password);
      trackEvent('auth_login_success');
      router.push('/chat');
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      const message = axiosErr?.response?.data?.message || (err instanceof Error ? err.message : 'Erro ao fazer login');
      trackEvent('auth_login_error', { message });
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell title="Entrar" subtitle="Acesse o Lume para abrir seus chats, templates e biblioteca em uma experiencia premium.">
      <form onSubmit={handleSubmit} className="space-y-4">
        {error ? <Alert variant="error">{error}</Alert> : null}

        <Field
          id="email"
          type="email"
          label="Email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
          placeholder="voce@empresa.com"
          autoComplete="email"
        />

        <Field
          id="password"
          type="password"
          label="Senha"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
          placeholder="Sua senha"
          autoComplete="current-password"
        />

        <Button type="submit" variant="brand" size="lg" disabled={loading} className="w-full">
          {loading ? 'Entrando...' : 'Entrar no Lume'}
        </Button>
      </form>

      <p className="mt-5 text-center text-[0.82rem] text-[var(--muted-foreground)]">
        Ainda nao tem conta?{' '}
        <Link href="/register" className="font-semibold text-[var(--foreground)] hover:text-[var(--brand-primary)]">
          Criar acesso
        </Link>
      </p>
    </AuthShell>
  );
}
