'use client';

import Link from 'next/link';
import { useState } from 'react';
import { toast } from '@/stores/toast-store';
import { trackEvent } from '@/lib/analytics';
import { AppLayout } from '@/components/app/app-layout';
import { Button } from '@/components/ui/button';
import { Field, SelectField } from '@/components/ui/form-field';
import { RadioCardGroup } from '@/components/ui/radio-card-group';
import { ToggleSwitch } from '@/components/ui/toggle-switch';
import { useAuthStore } from '@/stores/auth-store';
import { ChatFontMode, Theme, useThemeStore } from '@/stores/theme-store';
import {
  Bell,
  Database,
  Globe2,
  LineChart,
  Monitor,
  Moon,
  Save,
  Shield,
  Sparkles,
  Sun,
  Type,
  UserRound,
} from 'lucide-react';

const themeOptions: { value: Theme; label: string; icon: React.ElementType; description: string }[] = [
  { value: 'light', label: 'Claro', icon: Sun, description: 'Superficies claras com contraste editorial.' },
  { value: 'dark', label: 'Escuro', icon: Moon, description: 'Tema principal com tons quentes e foco em leitura.' },
  { value: 'system', label: 'Sistema', icon: Monitor, description: 'Segue automaticamente a preferencia do dispositivo.' },
];

const chatFontOptions: { value: ChatFontMode; label: string; icon: React.ElementType; description: string }[] = [
  { value: 'default', label: 'Padrao', icon: Sparkles, description: 'UI em sans e respostas em serif.' },
  { value: 'sans', label: 'Sans', icon: Type, description: 'Tudo em sans para leitura compacta e neutra.' },
  { value: 'system', label: 'Sistema', icon: Monitor, description: 'Usa a pilha tipografica nativa do sistema.' },
  { value: 'dyslexia', label: 'Acessivel', icon: Shield, description: 'Prioriza legibilidade com fonte acessivel.' },
];

const localeOptions = [
  { value: 'pt-BR', label: 'Portugues (Brasil)' },
  { value: 'en-US', label: 'English (US)' },
  { value: 'es-ES', label: 'Espanol' },
];

function Section({
  id,
  title,
  desc,
  icon: Icon,
  children,
}: {
  id: string;
  title: string;
  desc: string;
  icon: React.ElementType;
  children: React.ReactNode;
}) {
  return (
    <section id={id} className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface)] p-5 shadow-[var(--shadow-xs)]">
      <div className="mb-4 flex items-start gap-3">
        <span className="inline-flex h-10 w-10 items-center justify-center rounded-[var(--radius-md)] bg-[var(--accent-gold-light)] text-[var(--accent-gold)]">
          <Icon className="h-4 w-4" />
        </span>
        <div>
          <h2 className="font-[var(--font-serif)] text-[22px] font-medium tracking-[-0.03em] text-[var(--foreground)]">
            {title}
          </h2>
          <p className="mt-1 text-[13px] text-[var(--muted-foreground)]">{desc}</p>
        </div>
      </div>
      {children}
    </section>
  );
}

export default function SettingsPage() {
  const user = useAuthStore((state) => state.user);
  const theme = useThemeStore((state) => state.theme);
  const setTheme = useThemeStore((state) => state.setTheme);
  const chatFontMode = useThemeStore((state) => state.chatFontMode);
  const setChatFontMode = useThemeStore((state) => state.setChatFontMode);

  const [fullName, setFullName] = useState(user?.fullName || '');
  const [locale, setLocale] = useState('pt-BR');
  const [notifChat, setNotifChat] = useState(true);
  const [notifUpdates, setNotifUpdates] = useState(false);

  const handleSave = (event: React.FormEvent) => {
    event.preventDefault();
    trackEvent('settings_save_preferences', { locale, hasName: Boolean(fullName), theme, chatFontMode });
    toast.success('Preferencias salvas', 'Aparencia e preferencias aplicadas com sucesso.');
  };

  const jumpTo = (id: string) => {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  return (
    <AppLayout>
      <div className="mx-auto grid max-w-6xl gap-6 px-6 py-8 lg:grid-cols-[220px_1fr]">
        <aside className="hidden self-start lg:block">
          <div className="sticky top-6 rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface)] p-3">
            {[
              { id: 'profile', label: 'Perfil' },
              { id: 'appearance', label: 'Aparencia' },
              { id: 'notifications', label: 'Notificacoes' },
              { id: 'privacy', label: 'Privacidade' },
              { id: 'analytics', label: 'Analytics' },
            ].map((item) => (
              <button
                key={item.id}
                type="button"
                onClick={() => jumpTo(item.id)}
                className="flex h-9 w-full items-center rounded-[var(--radius-md)] px-3 text-left text-[13px] text-[var(--muted-foreground)] hover:bg-[var(--surface-hover)] hover:text-[var(--foreground)]"
              >
                {item.label}
              </button>
            ))}
          </div>
        </aside>

        <div>
          <div className="mb-8">
            <h1 className="font-[var(--font-serif)] text-[32px] font-medium tracking-[-0.04em] text-[var(--foreground)]">
              Configuracoes
            </h1>
            <p className="mt-2 max-w-2xl text-[14px] text-[var(--muted-foreground)]">
              Ajuste identidade visual, tipografia do chat e preferencias principais do workspace pessoal.
            </p>
          </div>

          <form onSubmit={handleSave} className="space-y-5">
            <Section id="profile" title="Perfil" desc="Dados basicos e idioma principal." icon={UserRound}>
              <div className="grid gap-4 md:grid-cols-2">
                <Field
                  id="fullName"
                  label="Nome de exibicao"
                  value={fullName}
                  onChange={(event) => setFullName(event.target.value)}
                  placeholder="Seu nome"
                />
                <Field id="email" label="E-mail" value={user?.email || ''} disabled />
              </div>
              <div className="mt-4 grid gap-4 md:grid-cols-2">
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

            <Section id="appearance" title="Aparencia" desc="Tema e tipografia do chat." icon={Monitor}>
              <div>
                <p className="mb-2 text-[13px] font-medium text-[var(--muted-foreground)]">Tema</p>
                <RadioCardGroup
                  value={theme}
                  onChange={(value) => setTheme(value as Theme)}
                  options={themeOptions}
                  columns={3}
                />
              </div>

              <div className="mt-5">
                <p className="mb-2 text-[13px] font-medium text-[var(--muted-foreground)]">Fonte do chat</p>
                <RadioCardGroup
                  value={chatFontMode}
                  onChange={(value) => setChatFontMode(value as ChatFontMode)}
                  options={chatFontOptions}
                  columns={2}
                />
              </div>
            </Section>

            <Section id="notifications" title="Notificacoes" desc="Alertas visuais do workspace e do chat." icon={Bell}>
              <div className="space-y-3">
                {[
                  {
                    id: 'chat',
                    label: 'Respostas do chat',
                    desc: 'Mostra alerta quando a IA conclui uma resposta.',
                    value: notifChat,
                    setValue: setNotifChat,
                  },
                  {
                    id: 'updates',
                    label: 'Novidades da plataforma',
                    desc: 'Avisos sobre novos modelos, templates e recursos.',
                    value: notifUpdates,
                    setValue: setNotifUpdates,
                  },
                ].map((item) => (
                  <div key={item.id} className="flex items-center justify-between gap-4 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--background)] px-4 py-3">
                    <div>
                      <p className="text-[13px] font-medium text-[var(--foreground)]">{item.label}</p>
                      <p className="mt-1 text-[12px] text-[var(--muted-foreground)]">{item.desc}</p>
                    </div>
                    <ToggleSwitch
                      checked={item.value}
                      onCheckedChange={item.setValue}
                      ariaLabel={item.label}
                    />
                  </div>
                ))}
              </div>
            </Section>

            <Section id="privacy" title="Privacidade e dados" desc="Estado local e acessos do navegador." icon={Database}>
              <div className="space-y-3">
                <div className="rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--background)] p-4">
                  <p className="text-[13px] font-medium text-[var(--foreground)]">Dados locais</p>
                  <p className="mt-1 text-[12px] leading-relaxed text-[var(--muted-foreground)]">
                    Conversas, preferencia de tema e fonte do chat ficam persistidos neste navegador.
                  </p>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  className="w-full justify-center text-[var(--destructive)]"
                  onClick={() => {
                    if (!window.confirm('Limpar todos os dados locais? Esta acao e irreversivel.')) return;
                    localStorage.clear();
                    toast.success('Dados locais removidos');
                  }}
                >
                  Limpar dados locais
                </Button>
              </div>
            </Section>

            <Section id="analytics" title="Analytics" desc="Acesse insights e diagnosticos do frontend." icon={LineChart}>
              <div className="grid gap-2 sm:grid-cols-2">
                <Link
                  href="/settings/analytics"
                  className="inline-flex items-center justify-between rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--background)] px-4 py-3 text-[13px] font-medium text-[var(--foreground)] hover:bg-[var(--surface-hover)]"
                >
                  Abrir insights
                  <LineChart className="h-4 w-4 text-[var(--muted-foreground)]" />
                </Link>
                <Link
                  href="/settings/analytics/debug"
                  className="inline-flex items-center justify-between rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--background)] px-4 py-3 text-[13px] font-medium text-[var(--foreground)] hover:bg-[var(--surface-hover)]"
                >
                  Abrir diagnostico
                  <Database className="h-4 w-4 text-[var(--muted-foreground)]" />
                </Link>
              </div>
            </Section>

            <div className="flex items-center gap-3">
              <Button type="submit" variant="primary" size="lg">
                <Save className="h-4 w-4" />
                Salvar preferencias
              </Button>
              <p className="text-[12px] text-[var(--muted-foreground)]">
                Preferencias persistidas localmente no dispositivo atual.
              </p>
            </div>
          </form>
        </div>
      </div>
    </AppLayout>
  );
}
