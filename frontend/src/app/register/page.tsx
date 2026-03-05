'use client';

import { useState } from 'react';
import { useAuthStore } from '@/stores/auth-store';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { AuthShell } from '@/components/app/auth-shell';
import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { trackEvent } from '@/lib/analytics';

export default function RegisterPage() {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const register = useAuthStore((s) => s.register);
  const router = useRouter();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (password !== confirmPassword) {
      setError('As senhas não coincidem');
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
      const message =
        axiosErr?.response?.data?.message ||
        (err instanceof Error ? err.message : 'Erro ao criar conta');
      trackEvent('auth_register_error', { message });
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell title="Criar Conta" subtitle="Configure seu acesso à plataforma IA Aggregator">
      <form onSubmit={handleSubmit} className="space-y-4">
        {error ? <Alert variant="error">{error}</Alert> : null}

        <div className="space-y-2">
          <label htmlFor="fullName" className="text-sm font-medium">
            Nome Completo
          </label>
          <Input
            id="fullName"
            type="text"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            required
            placeholder="Seu nome completo"
            autoComplete="name"
          />
        </div>

        <div className="space-y-2">
          <label htmlFor="email" className="text-sm font-medium">
            Email
          </label>
          <Input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            placeholder="seu@email.com"
            autoComplete="email"
          />
        </div>

        <div className="space-y-2">
          <label htmlFor="password" className="text-sm font-medium">
            Senha
          </label>
          <Input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            placeholder="Mínimo 8 caracteres"
            autoComplete="new-password"
          />
        </div>

        <div className="space-y-2">
          <label htmlFor="confirmPassword" className="text-sm font-medium">
            Confirmar Senha
          </label>
          <Input
            id="confirmPassword"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
            placeholder="Repita sua senha"
            autoComplete="new-password"
          />
        </div>

        <Button type="submit" disabled={loading} className="w-full">
          {loading ? 'Criando conta...' : 'Criar Conta'}
        </Button>
      </form>

      <p className="text-center text-sm text-[var(--muted-foreground)]">
        Já tem uma conta?{' '}
        <Link href="/login" className="text-[var(--primary)] hover:underline">
          Entrar
        </Link>
      </p>
    </AuthShell>
  );
}
