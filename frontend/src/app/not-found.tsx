import Link from 'next/link';

export default function NotFound() {
  return (
    <main className="grid min-h-screen place-items-center bg-[var(--background)] text-[var(--foreground)] px-4">
      <div className="max-w-md rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-6 text-center">
        <p className="text-xs uppercase tracking-[0.16em] text-[var(--subtle-foreground)]">404</p>
        <h1 className="mt-2 text-2xl font-semibold tracking-[-0.04em]">Página não encontrada</h1>
        <p className="mt-2 text-sm text-[var(--muted-foreground)]">A rota solicitada não está disponível neste workspace.</p>
        <Link href="/codex" className="mt-4 inline-flex rounded-full border border-[var(--border)] px-4 py-2 text-sm">
          Voltar para /codex
        </Link>
      </div>
    </main>
  );
}

