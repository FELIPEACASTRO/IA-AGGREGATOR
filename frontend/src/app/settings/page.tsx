'use client';

import { AppShell } from '@/components/app/app-shell';
import { useAuthStore } from '@/stores/auth-store';
import { useThemeStore } from '@/stores/theme-store';
import { useState } from 'react';
import { toast } from '@/stores/toast-store';
import { trackEvent } from '@/lib/analytics';
import { motion } from 'framer-motion';
import { cn } from '@/lib/cn';
import {
  Globe2, Moon, Sun, Monitor, UserRound, Bell, Shield,
  Database, ChevronRight, Save, Trash2,
} from 'lucide-react';

type ThemeOption = 'light' | 'dark' | 'system';

const themeOptions: { value: ThemeOption; label: string; icon: React.ElementType }[] = [
  { value: 'light', label: 'Claro', icon: Sun },
  { value: 'dark', label: 'Escuro', icon: Moon },
  { value: 'system', label: 'Sistema', icon: Monitor },
];

const localeOptions = [
  { value: 'pt-BR', label: 'Português (Brasil)' },
  { value: 'en-US', label: 'English (US)' },
  { value: 'es-ES', label: 'Español' },
];

function SectionCard({ title, icon: Icon, children }: { title: string; icon: React.ElementType; children: React.ReactNode }) {
  return (
    <div className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[var(--surface-1)] overflow-hidden">
      <div className="flex items-center gap-2.5 border-b border-[var(--border)] px-5 py-3.5">
        <Icon className="h-4 w-4 text-[var(--muted-foreground)]" />
        <h2 className="text-[var(--text-sm)] font-semibold">{title}</h2>
      </div>
      <div className="p-5">{children}</div>
    </div>
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
    toast.success('Preferências salvas', 'Alterações aplicadas com sucesso.');
  };

  return (
    <AppShell title="Configurações" subtitle="Personalize sua conta e experiência">
      <motion.form onSubmit={handleSave} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }} className="py-6 max-w-2xl space-y-5">

        {/* Profile */}
        <SectionCard title="Perfil" icon={UserRound}>
          <div className="space-y-4">
            <div>
              <label htmlFor="fullName" className="block text-[var(--text-xs)] font-medium text-[var(--muted-foreground)] mb-1.5">
                Nome de exibição
              </label>
              <input id="fullName" value={fullName} onChange={(e) => setFullName(e.target.value)}
                placeholder="Seu nome"
                className="w-full rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface-2)] px-3 py-2.5 text-[var(--text-sm)] placeholder:text-[var(--muted-foreground)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)] transition-shadow" />
            </div>
            <div>
              <label className="block text-[var(--text-xs)] font-medium text-[var(--muted-foreground)] mb-1.5">
                E-mail
              </label>
              <input value={user?.email || ''} disabled
                className="w-full rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface-2)] px-3 py-2.5 text-[var(--text-sm)] text-[var(--muted-foreground)] cursor-not-allowed opacity-60" />
            </div>
          </div>
        </SectionCard>

        {/* Appearance */}
        <SectionCard title="Aparência" icon={Monitor}>
          <div className="space-y-4">
            <div>
              <label className="block text-[var(--text-xs)] font-medium text-[var(--muted-foreground)] mb-2">Tema</label>
              <div className="grid grid-cols-3 gap-2">
                {themeOptions.map((opt) => (
                  <button key={opt.value} type="button" onClick={() => setTheme(opt.value)}
                    className={cn('flex items-center justify-center gap-2 rounded-[var(--radius-md)] border py-2.5 text-[var(--text-xs)] font-medium transition-all',
                      theme === opt.value
                        ? 'border-[var(--brand-primary)] bg-[var(--brand-primary)]/10 text-[var(--brand-primary)]'
                        : 'border-[var(--border)] bg-[var(--surface-2)] text-[var(--muted-foreground)] hover:border-[var(--border-strong)]')}>
                    <opt.icon className="h-3.5 w-3.5" /> {opt.label}
                  </button>
                ))}
              </div>
            </div>
            <div>
              <label htmlFor="locale" className="block text-[var(--text-xs)] font-medium text-[var(--muted-foreground)] mb-1.5 flex items-center gap-1">
                <Globe2 className="h-3.5 w-3.5" /> Idioma
              </label>
              <select id="locale" value={locale} onChange={(e) => setLocale(e.target.value)}
                className="w-full rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface-2)] px-3 py-2.5 text-[var(--text-sm)] focus:outline-none focus:ring-2 focus:ring-[var(--ring)] transition-shadow">
                {localeOptions.map((opt) => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </div>
          </div>
        </SectionCard>

        {/* Notifications */}
        <SectionCard title="Notificações" icon={Bell}>
          <div className="space-y-3">
            {[
              { id: 'chat', label: 'Respostas do chat', desc: 'Alerta quando a IA terminar de responder', value: notifChat, set: setNotifChat },
              { id: 'updates', label: 'Novidades da plataforma', desc: 'Novos modelos e funcionalidades', value: notifUpdates, set: setNotifUpdates },
            ].map((item) => (
              <div key={item.id} className="flex items-center justify-between rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface-2)] px-4 py-3">
                <div>
                  <p className="text-[var(--text-sm)] font-medium">{item.label}</p>
                  <p className="text-[var(--text-xs)] text-[var(--muted-foreground)]">{item.desc}</p>
                </div>
                <button type="button" role="switch" aria-checked={item.value} onClick={() => item.set((v) => !v)}
                  className={cn('relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors focus:outline-none focus:ring-2 focus:ring-[var(--ring)] focus:ring-offset-2',
                    item.value ? 'bg-[var(--brand-primary)]' : 'bg-[var(--surface-3)]')}>
                  <span className={cn('pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow-sm ring-0 transition-transform',
                    item.value ? 'translate-x-4' : 'translate-x-0')} />
                </button>
              </div>
            ))}
          </div>
        </SectionCard>

        {/* Privacy */}
        <SectionCard title="Privacidade e dados" icon={Shield}>
          <div className="space-y-3">
            <div className="rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface-2)] px-4 py-3 flex items-center justify-between">
              <div>
                <p className="text-[var(--text-sm)] font-medium flex items-center gap-1.5"><Database className="h-3.5 w-3.5 text-[var(--muted-foreground)]" /> Dados locais</p>
                <p className="text-[var(--text-xs)] text-[var(--muted-foreground)]">Conversas e preferências armazenadas localmente</p>
              </div>
              <ChevronRight className="h-4 w-4 text-[var(--muted-foreground)]" />
            </div>
            <button type="button"
              onClick={() => { if (window.confirm('Limpar todos os dados locais? Esta ação é irreversível.')) { localStorage.clear(); toast.success('Dados limpos'); }}}
              className="flex items-center gap-2 rounded-[var(--radius-md)] border border-[var(--destructive)]/30 bg-[var(--destructive)]/5 px-4 py-3 text-[var(--text-sm)] text-[var(--destructive)] hover:bg-[var(--destructive)]/10 transition-colors w-full">
              <Trash2 className="h-3.5 w-3.5" /> Limpar todos os dados locais
            </button>
          </div>
        </SectionCard>

        <div className="flex items-center gap-3 pt-2">
          <button type="submit"
            className="inline-flex items-center gap-2 rounded-[var(--radius-md)] px-5 py-2.5 text-[var(--text-sm)] font-medium text-white shadow-[var(--shadow-brand)] hover:opacity-90 transition-opacity"
            style={{ background: 'var(--brand-gradient)' }}>
            <Save className="h-3.5 w-3.5" /> Salvar preferências
          </button>
          <p className="text-[var(--text-xs)] text-[var(--muted-foreground)]">Preview — dados salvos localmente</p>
        </div>
      </motion.form>
    </AppShell>
  );
}
