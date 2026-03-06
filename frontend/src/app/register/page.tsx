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

export default function RegisterPage() {
  const t = useTranslations();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const register = useAuthStore((state) => state.register);
  const router = useRouter();
  const passwordChecks = {
    minLength: password.length >= 8,
    uppercase: /[A-Z]/.test(password),
    number: /\d/.test(password),
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError('');

    if (password !== confirmPassword) {
      setError(t('auth.register.errors.passwordMismatch'));
      return;
    }

    if (password.length < 8) {
      setError(t('auth.register.errors.passwordMinLength'));
      return;
    }

    setLoading(true);
    try {
      await register(email, password, fullName);
      trackEvent('auth_register_success');
      router.push('/welcome');
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      const message = axiosErr?.response?.data?.message || (err instanceof Error ? err.message : t('auth.register.errors.default'));
      trackEvent('auth_register_error', { message });
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell title={t('auth.register.title')} subtitle={t('auth.register.subtitle')}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error ? <Alert variant="error">{error}</Alert> : null}

        <Field
          id="fullName"
          type="text"
          label={t('auth.register.fullName')}
          value={fullName}
          onChange={(event) => setFullName(event.target.value)}
          required
          placeholder={t('auth.register.fullNamePlaceholder')}
          autoComplete="name"
        />

        <Field
          id="email"
          type="email"
          label={t('auth.register.email')}
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
          placeholder={t('auth.register.emailPlaceholder')}
          autoComplete="email"
        />

        <Field
          id="password"
          type="password"
          label={t('auth.register.password')}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
          minLength={8}
          placeholder={t('auth.register.passwordPlaceholder')}
          autoComplete="new-password"
        />
        <div className="grid gap-1 rounded-[var(--radius-md)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] px-3 py-2.5 text-[0.72rem] text-[var(--muted-foreground)]">
          <span className={passwordChecks.minLength ? 'text-[var(--success)]' : undefined}>
            {passwordChecks.minLength ? '✓' : '•'} {t('auth.register.passwordChecklist.minLength')}
          </span>
          <span className={passwordChecks.uppercase ? 'text-[var(--success)]' : undefined}>
            {passwordChecks.uppercase ? '✓' : '•'} {t('auth.register.passwordChecklist.uppercase')}
          </span>
          <span className={passwordChecks.number ? 'text-[var(--success)]' : undefined}>
            {passwordChecks.number ? '✓' : '•'} {t('auth.register.passwordChecklist.number')}
          </span>
        </div>

        <Field
          id="confirmPassword"
          type="password"
          label={t('auth.register.confirmPassword')}
          value={confirmPassword}
          onChange={(event) => setConfirmPassword(event.target.value)}
          required
          placeholder={t('auth.register.confirmPasswordPlaceholder')}
          autoComplete="new-password"
        />

        <Button type="submit" variant="brand" size="lg" disabled={loading} className="w-full">
          {loading ? t('auth.register.submitting') : t('auth.register.submit')}
        </Button>
      </form>

      <p className="mt-5 text-center text-[0.82rem] text-[var(--muted-foreground)]">
        {t('auth.register.hasAccount')}{' '}
        <Link href="/login" className="font-semibold text-[var(--foreground)] hover:text-[var(--brand-primary)]">
          {t('auth.register.login')}
        </Link>
      </p>
    </AuthShell>
  );
}
