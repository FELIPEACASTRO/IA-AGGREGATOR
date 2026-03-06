import { ReactNode } from 'react';
import Link from 'next/link';
import { ArrowRight, Lock, ShieldCheck, Sparkles, Bot } from 'lucide-react';

import { Badge } from '@/components/ui/badge';

type AuthShellProps = {
  title: string;
  subtitle: string;
  children: ReactNode;
};

const proofPoints = [
  'Workspace unico com multiplos modelos',
  'Biblioteca, templates e analytics no mesmo fluxo',
  'Experiencia dark-first consistente em desktop e mobile',
];

export function AuthShell({ title, subtitle, children }: AuthShellProps) {
  return (
    <main className="relative min-h-screen overflow-hidden px-4 py-8 sm:px-6 lg:px-8">
      <div className="pointer-events-none absolute inset-0" aria-hidden>
        <div className="absolute left-[8%] top-[-12%] h-[28rem] w-[28rem] rounded-full bg-[rgba(96,115,255,0.18)] blur-[120px]" />
        <div className="absolute bottom-[-14%] right-[6%] h-[26rem] w-[26rem] rounded-full bg-[rgba(242,93,156,0.14)] blur-[120px]" />
      </div>

      <div className="relative lume-section flex min-h-[calc(100vh-4rem)] items-center justify-center">
        <div className="grid w-full max-w-6xl overflow-hidden rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.76)] shadow-[var(--shadow-xl)] backdrop-blur-md lg:grid-cols-[1.15fr_0.85fr]">
          <section className="hidden border-r border-[var(--border)] bg-[linear-gradient(180deg,rgba(20,35,59,0.92)_0%,rgba(8,17,31,0.98)_100%)] p-10 lg:flex lg:flex-col">
            <Link href="/" className="inline-flex items-center gap-3 text-[var(--text-sm)] font-semibold text-[var(--foreground)]">
              <span className="inline-flex h-11 w-11 items-center justify-center rounded-[18px] text-white shadow-[var(--shadow-brand)]" style={{ background: 'var(--brand-gradient)' }}>
                <Bot className="h-5 w-5" />
              </span>
              <span className="text-[1.05rem] font-semibold tracking-[-0.04em]">Lume</span>
            </Link>

            <div className="mt-16 max-w-xl">
              <Badge variant="brand" dot>
                Premium AI workspace
              </Badge>
              <h1 className="mt-5 text-[clamp(2.6rem,4vw,4.3rem)] font-semibold tracking-[-0.06em] text-[var(--foreground)]">
                IA para quem quer operar com clareza, ritmo e controle.
              </h1>
              <p className="mt-5 max-w-lg text-[var(--text-base)] text-[var(--muted-foreground)]">
                Converse com os melhores modelos, organize ativos do time e transforme prompts em operacao real sem trocar de ambiente.
              </p>
            </div>

            <div className="mt-10 space-y-3">
              {proofPoints.map((point) => (
                <div key={point} className="flex items-start gap-3 rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-4 py-3">
                  <span className="mt-0.5 inline-flex h-8 w-8 items-center justify-center rounded-full bg-[rgba(96,115,255,0.12)] text-[var(--brand-primary)]">
                    <ShieldCheck className="h-4 w-4" />
                  </span>
                  <p className="text-[var(--text-sm)] text-[var(--foreground)]">{point}</p>
                </div>
              ))}
            </div>

            <div className="mt-auto flex items-center justify-between rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-4 py-4">
              <div>
                <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Lume access</p>
                <p className="mt-1 text-[var(--text-sm)] text-[var(--muted-foreground)]">Autenticacao segura, experiencia consistente e onboarding rapido.</p>
              </div>
              <ArrowRight className="h-4 w-4 text-[var(--brand-primary)]" />
            </div>
          </section>

          <section className="p-5 sm:p-8 lg:p-10">
            <div className="mx-auto flex w-full max-w-md flex-col justify-center">
              <div className="mb-8 flex items-center justify-between lg:hidden">
                <Link href="/" className="inline-flex items-center gap-2 text-[var(--text-sm)] font-semibold text-[var(--foreground)]">
                  <span className="inline-flex h-10 w-10 items-center justify-center rounded-[16px] text-white shadow-[var(--shadow-brand)]" style={{ background: 'var(--brand-gradient)' }}>
                    <Bot className="h-4 w-4" />
                  </span>
                  Lume
                </Link>
                <Badge variant="outline">Access</Badge>
              </div>

              <div className="mb-6">
                <span className="inline-flex items-center gap-2 rounded-full border border-[var(--border)] bg-[rgba(255,255,255,0.04)] px-3 py-1 text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">
                  <Lock className="h-3.5 w-3.5" /> Conta segura
                </span>
                <h2 className="mt-4 text-[var(--text-3xl)] font-semibold text-[var(--foreground)]">{title}</h2>
                <p className="mt-2 text-[var(--text-sm)] text-[var(--muted-foreground)]">{subtitle}</p>
              </div>

              <div className="lume-panel rounded-[var(--radius-xl)] p-5 sm:p-6">
                {children}
              </div>

              <div className="mt-5 flex items-center gap-2 text-[0.78rem] text-[var(--muted-foreground)]">
                <Sparkles className="h-3.5 w-3.5 text-[var(--brand-primary)]" />
                Experiencia otimizada para desktop e mobile.
              </div>
            </div>
          </section>
        </div>
      </div>
    </main>
  );
}
