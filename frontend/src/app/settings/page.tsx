'use client';

import { AppShell } from '@/components/app/app-shell';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useAuthStore } from '@/stores/auth-store';
import { useState } from 'react';
import { toast } from '@/stores/toast-store';
import { trackEvent } from '@/lib/analytics';
import Link from 'next/link';

export default function SettingsPage() {
  const user = useAuthStore((state) => state.user);
  const [fullName, setFullName] = useState(user?.fullName || '');
  const [locale, setLocale] = useState('pt-BR');

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    trackEvent('settings_save_preferences', { locale, hasName: Boolean(fullName) });
    toast.success('Preferências salvas', `Idioma: ${locale} · Nome: ${fullName || 'não informado'}`);
  };

  return (
    <AppShell title="Configurações" subtitle="Preferências da conta e experiência">
      <form className="max-w-2xl space-y-4 p-6" onSubmit={handleSubmit}>
        <div className="space-y-2">
          <label htmlFor="fullName" className="text-sm font-medium">
            Nome de exibição
          </label>
          <Input
            id="fullName"
            value={fullName}
            onChange={(event) => setFullName(event.target.value)}
            placeholder="Seu nome"
          />
        </div>
        <div className="space-y-2">
          <label htmlFor="locale" className="text-sm font-medium">
            Idioma padrão
          </label>
          <select
            id="locale"
            value={locale}
            onChange={(event) => setLocale(event.target.value)}
            className="w-full rounded-lg border border-[var(--border)] bg-[var(--background)] px-4 py-2.5 text-sm"
          >
            <option value="pt-BR">Português (Brasil)</option>
            <option value="en-US">English (US)</option>
            <option value="es-ES">Español</option>
          </select>
        </div>
        <Button type="submit">Salvar preferências</Button>
        <div className="pt-2">
          <Link href="/settings/analytics" className="text-sm text-[var(--primary)] hover:underline">
            Abrir diagnóstico de analytics
          </Link>
        </div>
      </form>
    </AppShell>
  );
}
