import { ReactNode } from 'react';
import Link from 'next/link';
import { Lock, ShieldCheck, Bot, Sparkles } from 'lucide-react';
import { Badge } from '@/components/ui/badge';

type AuthShellProps = {
  title: string;
  subtitle: string;
  children: ReactNode;
};

const proofPoints = [
  'Workspace unificado com multiplos modelos de IA',
  'Biblioteca, templates e analytics integrados',
  'Interface dark-first com experiencia premium',
];

export function AuthShell({ title, subtitle, children }: AuthShellProps) {
  return (
    <main className="relative flex min-h-screen items-center justify-center overflow-hidden px-4 py-8">
      {/* Ambient background effects */}
      <div className="pointer-events-none absolute inset-0" aria-hidden>
        <div className="absolute left-[10%] top-[-15%] h-[32rem] w-[32rem] rounded-full bg-[rgba(124,106,255,0.12)] blur-[140px]" />
        <div className="absolute bottom-[-10%] right-[8%] h-[28rem] w-[28rem] rounded-full bg-[rgba(255,126,179,0.08)] blur-[120px]" />
      </div>

      <div className="relative w-full max-w-[1120px]">
        <div className="grid overflow-hidden rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[var(--card)] shadow-[var(--shadow-xl)] lg:grid-cols-[1.1fr_0.9fr]">
          {/* ── Left: Value Proposition ── */}
          <section className="hidden border-r border-[var(--border)] bg-[var(--background)] p-10 lg:flex lg:flex-col lg:justify-between">
            <div>
              <Link href="/" className="inline-flex items-center gap-3">
                <span
                  className="inline-flex h-10 w-10 items-center justify-center rounded-[14px] text-white shadow-[var(--shadow-brand)]"
                  style={{ background: 'var(--brand-gradient)' }}
                >
                  <Bot className="h-[18px] w-[18px]" />
                </span>
                <span className="text-[0.94rem] font-bold tracking-[-0.04em] text-[var(--foreground)]">Lume</span>
              </Link>

              <div className="mt-14">
                <Badge variant="brand" dot>Premium AI Workspace</Badge>
                <h1 className="mt-5 text-[var(--text-4xl)] font-bold tracking-[-0.05em] text-[var(--foreground)]">
                  IA para quem quer operar com clareza e controle.
                </h1>
                <p className="mt-5 max-w-md text-[var(--text-base)] leading-relaxed text-[var(--foreground-secondary)]">
                  Converse com os melhores modelos, organize ativos e transforme prompts em resultados reais.
                </p>
              </div>
            </div>

            <div className="mt-10 space-y-2.5">
              {proofPoints.map((point) => (
                <div
                  key={point}
                  className="flex items-center gap-3 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--card)] px-4 py-3"
                >
                  <ShieldCheck className="h-4 w-4 shrink-0 text-[var(--brand-primary)]" />
                  <p className="text-[var(--text-sm)] text-[var(--foreground)]">{point}</p>
                </div>
              ))}
            </div>
          </section>

          {/* ── Right: Auth Form ── */}
          <section className="flex flex-col justify-center p-6 sm:p-8 lg:p-10">
            <div className="mx-auto w-full max-w-md">
              {/* Mobile logo */}
              <div className="mb-8 flex items-center justify-between lg:hidden">
                <Link href="/" className="inline-flex items-center gap-2">
                  <span
                    className="inline-flex h-9 w-9 items-center justify-center rounded-[12px] text-white shadow-[var(--shadow-brand)]"
                    style={{ background: 'var(--brand-gradient)' }}
                  >
                    <Bot className="h-4 w-4" />
                  </span>
                  <span className="text-[0.88rem] font-bold text-[var(--foreground)]">Lume</span>
                </Link>
                <Badge variant="outline">Access</Badge>
              </div>

              <div className="mb-6">
                <span className="inline-flex items-center gap-2 rounded-full border border-[var(--border)] bg-[rgba(255,255,255,0.02)] px-3 py-1 text-[0.66rem] font-semibold uppercase tracking-[0.16em] text-[var(--foreground-muted)]">
                  <Lock className="h-3 w-3" /> Conta segura
                </span>
                <h2 className="mt-4 text-[var(--text-2xl)] font-bold tracking-[-0.04em] text-[var(--foreground)]">{title}</h2>
                <p className="mt-2 text-[var(--text-sm)] text-[var(--foreground-secondary)]">{subtitle}</p>
              </div>

              <div className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[var(--background)] p-5 sm:p-6">
                {children}
              </div>

              <div className="mt-4 flex items-center gap-2 text-[0.74rem] text-[var(--foreground-muted)]">
                <Sparkles className="h-3 w-3 text-[var(--brand-primary)]" />
                Experiencia otimizada para desktop e mobile.
              </div>
            </div>
          </section>
        </div>
      </div>
    </main>
  );
}
