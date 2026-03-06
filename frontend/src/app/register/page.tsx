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

export default function RegisterPage() {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const register = useAuthStore((state) => state.register);
  const router = useRouter();

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError('');

    if (password !== confirmPassword) {
      setError('As senhas nao coincidem');
      return;
    }

    if (password.length < 8) {
      setError('A senha deve ter pelo menos 8 caracteres');
      return;
    }

    setLoading(true);
    try {
      await register(email, password, fullName);
      trackEvent('auth_register_success');
      router.push('/welcome');
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      const message = axiosErr?.response?.data?.message || (err instanceof Error ? err.message : 'Erro ao criar conta');
      trackEvent('auth_register_error', { message });
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell title="Criar conta" subtitle="Abra seu workspace no Lume e deixe o onboarding preparar o fluxo ideal para o seu uso.">
      <form onSubmit={handleSubmit} className="space-y-4">
        {error ? <Alert variant="error">{error}</Alert> : null}

        <Field
          id="fullName"
          type="text"
          label="Nome completo"
          value={fullName}
          onChange={(event) => setFullName(event.target.value)}
          required
          placeholder="Seu nome"
          autoComplete="name"
        />

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
          minLength={8}
          placeholder="Minimo de 8 caracteres"
          autoComplete="new-password"
        />

        <Field
          id="confirmPassword"
          type="password"
          label="Confirmar senha"
          value={confirmPassword}
          onChange={(event) => setConfirmPassword(event.target.value)}
          required
          placeholder="Repita a senha"
          autoComplete="new-password"
        />

        <Button type="submit" variant="brand" size="lg" disabled={loading} className="w-full">
          {loading ? 'Criando conta...' : 'Criar acesso ao Lume'}
        </Button>
      </form>

      <p className="mt-5 text-center text-[0.82rem] text-[var(--muted-foreground)]">
        Ja possui conta?{' '}
        <Link href="/login" className="font-semibold text-[var(--foreground)] hover:text-[var(--brand-primary)]">
          Entrar
        </Link>
      </p>
    </AuthShell>
  );
}
