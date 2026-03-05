import Link from 'next/link';
import { Button } from '@/components/ui/button';

export default function Home() {
  return (
    <main className="flex min-h-screen items-center justify-center p-6">
      <section className="w-full max-w-3xl rounded-2xl border border-[var(--border)] bg-[var(--background)] p-8 shadow-sm">
        <p className="text-sm font-medium text-[var(--muted-foreground)]">AI Workspace</p>
        <h1 className="mt-2 text-4xl font-bold tracking-tight">IA Aggregator</h1>
        <p className="mt-4 max-w-2xl text-base text-[var(--muted-foreground)]">
          Orquestre múltiplos modelos de IA em um único fluxo com fallback inteligente,
          roteamento configurável e experiência de chat pronta para produção.
        </p>
        <div className="mt-8 flex flex-wrap gap-3">
          <Link href="/login">
            <Button className="px-6">Entrar</Button>
          </Link>
          <Link href="/register">
            <Button variant="secondary" className="px-6">
              Criar Conta
            </Button>
          </Link>
        </div>
        <div className="mt-10 grid gap-3 text-sm text-[var(--muted-foreground)] sm:grid-cols-3">
          <div className="rounded-lg border border-[var(--border)] p-3">Roteamento entre modelos</div>
          <div className="rounded-lg border border-[var(--border)] p-3">Fallback automático</div>
          <div className="rounded-lg border border-[var(--border)] p-3">Guardrails e telemetria</div>
        </div>
      </section>
    </main>
  );
}
