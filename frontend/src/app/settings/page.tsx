'use client';

import { AppShell } from '@/components/app/app-shell';
import { useAuthStore } from '@/stores/auth-store';
import { useThemeStore } from '@/stores/theme-store';
import Link from 'next/link';
import { useState } from 'react';
import { toast } from '@/stores/toast-store';
import { trackEvent } from '@/lib/analytics';
import { motion } from 'framer-motion';
import { cn } from '@/lib/cn';
import { Button } from '@/components/ui/button';
import { Field, SelectField } from '@/components/ui/form-field';
import { PageSection, PageSplit, PageStack } from '@/components/app/page-blueprint';
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
  Trash2,
  UserRound,
} from 'lucide-react';

type ThemeOption = 'light' | 'dark' | 'system';

const themeOptions: { value: ThemeOption; label: string; icon: React.ElementType; helper: string }[] = [
  { value: 'light', label: 'Claro', icon: Sun, helper: 'fallback secundario' },
  { value: 'dark', label: 'Escuro', icon: Moon, helper: 'direcao principal' },
  { value: 'system', label: 'Sistema', icon: Monitor, helper: 'segue o dispositivo' },
];

const localeOptions = [
  { value: 'pt-BR', label: 'Portugues (Brasil)' },
  { value: 'en-US', label: 'English (US)' },
  { value: 'es-ES', label: 'Espanol' },
];

function SectionCard({ title, helper, icon: Icon, children }: { title: string; helper: string; icon: React.ElementType; children: React.ReactNode }) {
  return (
    <section className="lume-panel-soft rounded-[var(--radius-2xl)] p-5">
      <div className="flex items-start gap-3">
        <span className="inline-flex h-11 w-11 items-center justify-center rounded-[18px] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] text-[var(--brand-primary)]">
          <Icon className="h-5 w-5" />
        </span>
        <div>
          <h2 className="text-[var(--text-lg)] font-semibold text-[var(--foreground)]">{title}</h2>
          <p className="mt-1 text-[var(--text-xs)] text-[var(--muted-foreground)]">{helper}</p>
        </div>
      </div>
      <div className="mt-5">{children}</div>
    </section>
  );
}

export default function SettingsPage() {
  const user = useAuthStore((state) => state.user);
  const { theme, setTheme } = useThemeStore();
  const [fullName, setFullName] = useState(user?.fullName || '');
  const [locale, setLocale] = useState('pt-BR');
  const [notifChat, setNotifChat] = useState(true);
  const [notifUpdates, setNotifUpdates] = useState(false);

  const handleSave = (event: React.FormEvent) => {
    event.preventDefault();
    trackEvent('settings_save_preferences', { locale, hasName: Boolean(fullName) });
    toast.success('Preferencias salvas', 'Alteracoes aplicadas com sucesso.');
  };

  return (
    <AppShell title="Configuracoes" subtitle="Personalize sua conta e experiencia">
      <motion.form
        onSubmit={handleSave}
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="py-6"
      >
        <PageSplit
          left={
            <aside className="space-y-4">
              <PageSection>
                <span className="lume-kicker">
                  <Sparkles className="h-3.5 w-3.5" /> Workspace profile
                </span>
                <div className="mt-5 rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-5">
                  <div className="inline-flex h-14 w-14 items-center justify-center rounded-[20px] bg-[var(--brand-gradient)] text-white shadow-[var(--shadow-brand)]">
                    <UserRound className="h-6 w-6" />
                  </div>
                  <p className="mt-4 text-[var(--text-xl)] font-semibold text-[var(--foreground)]">{user?.fullName || 'Conta Lume'}</p>
                  <p className="mt-1 text-[var(--text-sm)] text-[var(--muted-foreground)]">{user?.email}</p>
                  <div className="mt-5 grid gap-3">
                    <div className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.52)] p-4">
                      <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Perfil</p>
                      <p className="mt-2 text-[var(--text-sm)] text-[var(--muted-foreground)]">Preferencias salvas localmente para iteracao rapida do workspace.</p>
                    </div>
                    <div className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.52)] p-4">
                      <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Tema</p>
                      <p className="mt-2 text-[var(--text-sm)] text-[var(--muted-foreground)]">Dark-first com opcao de override local para homologacao.</p>
                    </div>
                  </div>
                </div>
              </PageSection>
            </aside>
          }
          right={
            <PageStack className="space-y-4 py-0">
          <SectionCard title="Perfil" helper="Dados principais da conta visivel no workspace." icon={UserRound}>
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
          </SectionCard>

          <SectionCard title="Aparencia" helper="Escala visual consistente em todas as paginas do Lume." icon={Monitor}>
            <div className="space-y-4">
              <div>
                <p className="mb-2 text-[var(--text-xs)] font-semibold text-[var(--muted-foreground)]">Tema</p>
                <div className="grid gap-3 md:grid-cols-3">
                  {themeOptions.map((option) => (
                    <button
                      key={option.value}
                      type="button"
                      onClick={() => setTheme(option.value)}
                      className={cn(
                        'rounded-[var(--radius-xl)] border p-4 text-left transition-colors',
                        theme === option.value
                          ? 'border-[var(--brand-primary)] bg-[rgba(96,115,255,0.1)] text-[var(--brand-primary)]'
                          : 'border-[var(--border)] bg-[rgba(255,255,255,0.03)] text-[var(--muted-foreground)] hover:text-[var(--foreground)]',
                      )}
                    >
                      <option.icon className="h-5 w-5" />
                      <p className="mt-3 text-[var(--text-sm)] font-semibold">{option.label}</p>
                      <p className="mt-1 text-[0.72rem]">{option.helper}</p>
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
          </SectionCard>

          <SectionCard title="Notificacoes" helper="Ajuste o que merece interrupcao no seu fluxo." icon={Bell}>
            <div className="space-y-3">
              {[
                { id: 'chat', label: 'Respostas do chat', desc: 'Alerta quando a IA terminar de responder', value: notifChat, setValue: setNotifChat },
                { id: 'updates', label: 'Novidades da plataforma', desc: 'Novos modelos e funcionalidades', value: notifUpdates, setValue: setNotifUpdates },
              ].map((item) => (
                <div key={item.id} className="flex items-center justify-between gap-4 rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-4">
                  <div>
                    <p className="text-[var(--text-sm)] font-semibold text-[var(--foreground)]">{item.label}</p>
                    <p className="mt-1 text-[var(--text-xs)] text-[var(--muted-foreground)]">{item.desc}</p>
                  </div>
                  <button
                    type="button"
                    role="switch"
                    aria-checked={item.value}
                    onClick={() => item.setValue((current) => !current)}
                    className={cn(
                      'relative inline-flex h-7 w-12 shrink-0 rounded-full border border-transparent transition-colors',
                      item.value ? 'bg-[var(--brand-primary)]' : 'bg-[var(--surface-3)]',
                    )}
                  >
                    <span
                      className={cn(
                        'absolute left-1 top-1 inline-flex h-5 w-5 rounded-full bg-white shadow-[var(--shadow-sm)] transition-transform',
                        item.value ? 'translate-x-5' : 'translate-x-0',
                      )}
                    />
                  </button>
                </div>
              ))}
            </div>
          </SectionCard>

          <SectionCard title="Privacidade e dados" helper="Dados locais para testes e iteracao rapida da experiencia." icon={Shield}>
            <div className="space-y-3">
              <div className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-4">
                <p className="inline-flex items-center gap-2 text-[var(--text-sm)] font-semibold text-[var(--foreground)]">
                  <Database className="h-4 w-4 text-[var(--brand-primary)]" /> Dados locais
                </p>
                <p className="mt-2 text-[var(--text-xs)] text-[var(--muted-foreground)]">Conversas, preferencias e analytics de frontend ficam armazenados localmente neste ambiente.</p>
              </div>
              <Button
                type="button"
                variant="outline"
                className="w-full justify-center border-[rgba(255,107,135,0.25)] text-[var(--destructive)] hover:bg-[rgba(255,107,135,0.08)]"
                onClick={() => {
                  if (!window.confirm('Limpar todos os dados locais? Esta acao e irreversivel.')) return;
                  localStorage.clear();
                  toast.success('Dados limpos');
                }}
              >
                <Trash2 className="h-4 w-4" /> Limpar todos os dados locais
              </Button>
            </div>
          </SectionCard>

          <SectionCard title="Analytics" helper="Separe leitura executiva de diagnostico tecnico." icon={LineChart}>
            <div className="grid gap-3 sm:grid-cols-2">
              <Link
                href="/settings/analytics"
                className="inline-flex items-center justify-between rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-4 py-3 text-[var(--text-sm)] font-semibold text-[var(--foreground)] hover:border-[var(--border-strong)]"
              >
                Abrir insights
                <LineChart className="h-4 w-4 text-[var(--brand-primary)]" />
              </Link>
              <Link
                href="/settings/analytics/debug"
                className="inline-flex items-center justify-between rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-4 py-3 text-[var(--text-sm)] font-semibold text-[var(--foreground)] hover:border-[var(--border-strong)]"
              >
                Abrir diagnostico
                <Database className="h-4 w-4 text-[var(--warning)]" />
              </Link>
            </div>
          </SectionCard>

          <div className="flex flex-wrap items-center gap-3">
            <Button type="submit" variant="brand" size="lg">
              <Save className="h-4 w-4" /> Salvar preferencias
            </Button>
            <p className="text-[var(--text-xs)] text-[var(--muted-foreground)]">Preview local do workspace. Nenhuma preferencia sai do navegador nesta tela.</p>
          </div>
            </PageStack>
          }
        />
      </motion.form>
    </AppShell>
  );
}
