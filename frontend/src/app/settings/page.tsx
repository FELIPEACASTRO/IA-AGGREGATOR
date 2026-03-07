'use client';

import { AppLayout } from '@/components/app/app-layout';
import { useAuthStore } from '@/stores/auth-store';
import { useThemeStore } from '@/stores/theme-store';
import Link from 'next/link';
import { useState } from 'react';
import { toast } from '@/stores/toast-store';
import { trackEvent } from '@/lib/analytics';
import { cn } from '@/lib/cn';
import { Button } from '@/components/ui/button';
import { Field, SelectField } from '@/components/ui/form-field';
import {
  Bell,
  Database,
  Globe2,
  LineChart,
  Monitor,
  Moon,
  Save,
  Shield,
  Sun,
  Trash2,
  UserRound,
} from 'lucide-react';

type ThemeOption = 'light' | 'dark' | 'system';

const themeOptions: { value: ThemeOption; label: string; icon: React.ElementType; desc: string }[] = [
  { value: 'light', label: 'Claro', icon: Sun, desc: 'Tema claro' },
  { value: 'dark', label: 'Escuro', icon: Moon, desc: 'Tema escuro' },
  { value: 'system', label: 'Sistema', icon: Monitor, desc: 'Segue o dispositivo' },
];

const localeOptions = [
  { value: 'pt-BR', label: 'Portugues (Brasil)' },
  { value: 'en-US', label: 'English (US)' },
  { value: 'es-ES', label: 'Espanol' },
];

function Section({ title, desc, icon: Icon, children }: { title: string; desc: string; icon: React.ElementType; children: React.ReactNode }) {
  return (
    <section className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface)] p-5">
      <div className="flex items-start gap-3 mb-4">
        <Icon className="h-5 w-5 shrink-0 text-[var(--muted-foreground)] mt-0.5" />
        <div>
          <h2 className="text-[15px] font-semibold text-[var(--foreground)]">{title}</h2>
          <p className="mt-0.5 text-[13px] text-[var(--muted-foreground)]">{desc}</p>
        </div>
      </div>
      {children}
    </section>
  );
}

export default function SettingsPage() {
  const user = useAuthStore((s) => s.user);
  const { theme, setTheme } = useThemeStore();
  const [fullName, setFullName] = useState(user?.fullName || '');
  const [locale, setLocale] = useState('pt-BR');
  const [notifChat, setNotifChat] = useState(true);
  const [notifUpdates, setNotifUpdates] = useState(false);

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    trackEvent('settings_save_preferences', { locale, hasName: Boolean(fullName) });
    toast.success('Preferencias salvas', 'Alterações aplicadas com sucesso.');
  };

  return (
    <AppLayout>
      <div className="mx-auto max-w-2xl px-6 py-8">
        <h1 className="text-[24px] font-semibold text-[var(--foreground)]">Configurações</h1>
        <p className="mt-1 text-[14px] text-[var(--muted-foreground)]">Personalize sua conta e experiência.</p>

        <form onSubmit={handleSave} className="mt-8 space-y-5">
          {/* Profile */}
          <Section title="Perfil" desc="Dados da conta." icon={UserRound}>
            <div className="grid gap-4 md:grid-cols-2">
              <Field
                id="fullName"
                label="Nome de exibição"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Seu nome"
              />
              <Field id="email" label="E-mail" value={user?.email || ''} disabled />
            </div>
          </Section>

          {/* Theme */}
          <Section title="Aparência" desc="Escolha o tema visual." icon={Monitor}>
            <div className="space-y-4">
              <div>
                <p className="mb-2 text-[13px] font-medium text-[var(--muted-foreground)]">Tema</p>
                <div className="grid gap-2 md:grid-cols-3">
                  {themeOptions.map((opt) => (
                    <button
                      key={opt.value}
                      type="button"
                      onClick={() => setTheme(opt.value)}
                      className={cn(
                        'rounded-[var(--radius-md)] border p-3 text-left transition-colors',
                        theme === opt.value
                          ? 'border-[var(--accent)] bg-[var(--accent-light)]'
                          : 'border-[var(--border)] hover:bg-[var(--surface-hover)]',
                      )}
                    >
                      <opt.icon className={cn('h-4 w-4', theme === opt.value ? 'text-[var(--accent)]' : 'text-[var(--muted-foreground)]')} />
                      <p className={cn('mt-2 text-[13px] font-medium', theme === opt.value ? 'text-[var(--accent)]' : 'text-[var(--foreground)]')}>
                        {opt.label}
                      </p>
                      <p className="mt-0.5 text-[11px] text-[var(--muted-foreground)]">{opt.desc}</p>
                    </button>
                  ))}
                </div>
              </div>
              <SelectField
                id="locale"
                label="Idioma"
                value={locale}
                onChange={setLocale}
                options={localeOptions}
                icon={<Globe2 className="h-4 w-4" />}
              />
            </div>
          </Section>

          {/* Notifications */}
          <Section title="Notificações" desc="Ajuste alertas." icon={Bell}>
            <div className="space-y-3">
              {[
                { id: 'chat', label: 'Respostas do chat', desc: 'Alerta quando a IA terminar de responder', value: notifChat, setValue: setNotifChat },
                { id: 'updates', label: 'Novidades da plataforma', desc: 'Novos modelos e funcionalidades', value: notifUpdates, setValue: setNotifUpdates },
              ].map((item) => (
                <div key={item.id} className="flex items-center justify-between gap-4 rounded-[var(--radius-md)] border border-[var(--border)] p-3">
                  <div>
                    <p className="text-[13px] font-medium text-[var(--foreground)]">{item.label}</p>
                    <p className="mt-0.5 text-[12px] text-[var(--muted-foreground)]">{item.desc}</p>
                  </div>
                  <button
                    type="button"
                    role="switch"
                    aria-checked={item.value}
                    onClick={() => item.setValue((v) => !v)}
                    className={cn(
                      'relative inline-flex h-6 w-10 shrink-0 rounded-full transition-colors',
                      item.value ? 'bg-[var(--accent)]' : 'bg-[var(--surface-active)]',
                    )}
                  >
                    <span
                      className={cn(
                        'absolute top-0.5 left-0.5 inline-flex h-5 w-5 rounded-full bg-white shadow-[var(--shadow-xs)] transition-transform',
                        item.value ? 'translate-x-4' : 'translate-x-0',
                      )}
                    />
                  </button>
                </div>
              ))}
            </div>
          </Section>

          {/* Privacy */}
          <Section title="Privacidade e dados" desc="Gerenciamento de dados locais." icon={Shield}>
            <div className="space-y-3">
              <div className="rounded-[var(--radius-md)] border border-[var(--border)] p-3">
                <p className="inline-flex items-center gap-2 text-[13px] font-medium text-[var(--foreground)]">
                  <Database className="h-4 w-4 text-[var(--muted-foreground)]" /> Dados locais
                </p>
                <p className="mt-1 text-[12px] text-[var(--muted-foreground)]">
                  Conversas e preferências ficam armazenados localmente neste navegador.
                </p>
              </div>
              <Button
                type="button"
                variant="outline"
                className="w-full justify-center text-[var(--destructive)]"
                onClick={() => {
                  if (!window.confirm('Limpar todos os dados locais? Esta ação é irreversível.')) return;
                  localStorage.clear();
                  toast.success('Dados limpos');
                }}
              >
                <Trash2 className="h-4 w-4" /> Limpar todos os dados locais
              </Button>
            </div>
          </Section>

          {/* Analytics */}
          <Section title="Analytics" desc="Acesse insights e diagnósticos." icon={LineChart}>
            <div className="grid gap-2 sm:grid-cols-2">
              <Link
                href="/settings/analytics"
                className="inline-flex items-center justify-between rounded-[var(--radius-md)] border border-[var(--border)] px-4 py-2.5 text-[13px] font-medium text-[var(--foreground)] hover:bg-[var(--surface-hover)] transition-colors"
              >
                Abrir insights
                <LineChart className="h-4 w-4 text-[var(--muted-foreground)]" />
              </Link>
              <Link
                href="/settings/analytics/debug"
                className="inline-flex items-center justify-between rounded-[var(--radius-md)] border border-[var(--border)] px-4 py-2.5 text-[13px] font-medium text-[var(--foreground)] hover:bg-[var(--surface-hover)] transition-colors"
              >
                Abrir diagnóstico
                <Database className="h-4 w-4 text-[var(--muted-foreground)]" />
              </Link>
            </div>
          </Section>

          {/* Save */}
          <div className="flex items-center gap-3">
            <Button type="submit" variant="primary" size="lg">
              <Save className="h-4 w-4" /> Salvar preferências
            </Button>
            <p className="text-[12px] text-[var(--muted-foreground)]">Preferências salvas localmente.</p>
          </div>
        </form>
      </div>
    </AppLayout>
  );
}
