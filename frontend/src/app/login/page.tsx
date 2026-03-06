'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';

import { AuthShell } from '@/components/app/auth-shell';
import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Field } from '@/components/ui/form-field';
import { trackEvent } from '@/lib/analytics';
import { useAuthStore } from '@/stores/auth-store';

export default function LoginPage() {
  const t = useTranslations();
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
      const message = axiosErr?.response?.data?.message || (err instanceof Error ? err.message : t('auth.login.defaultError'));
      trackEvent('auth_login_error', { message });
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell title={t('auth.login.title')} subtitle={t('auth.login.subtitle')}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error ? <Alert variant="error">{error}</Alert> : null}

        <Field
          id="email"
          type="email"
          label={t('auth.login.email')}
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
          placeholder={t('auth.login.emailPlaceholder')}
          autoComplete="email"
        />

        <Field
          id="password"
          type="password"
          label={t('auth.login.password')}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
          placeholder={t('auth.login.passwordPlaceholder')}
          autoComplete="current-password"
        />

        <Button type="submit" variant="brand" size="lg" disabled={loading} className="w-full">
          {loading ? t('auth.login.submitting') : t('auth.login.submit')}
        </Button>
      </form>

      <p className="mt-5 text-center text-[0.82rem] text-[var(--muted-foreground)]">
        {t('auth.login.noAccount')}{' '}
        <Link href="/register" className="font-semibold text-[var(--foreground)] hover:text-[var(--brand-primary)]">
          {t('auth.login.createAccess')}
        </Link>
      </p>
    </AuthShell>
  );
}
