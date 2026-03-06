'use client';

import Link from 'next/link';

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <html>
      <body className="bg-[var(--background)] text-[var(--foreground)]">
        <main className="grid min-h-screen place-items-center px-4">
          <div className="max-w-lg rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-6">
            <p className="text-xs uppercase tracking-[0.16em] text-[var(--subtle-foreground)]">Runtime Error</p>
            <h1 className="mt-2 text-xl font-semibold tracking-[-0.03em]">Falha ao processar a rota</h1>
            <p className="mt-2 text-sm text-[var(--muted-foreground)]">{error.message}</p>
            <div className="mt-4 flex gap-2">
              <button className="rounded-full border border-[var(--border)] px-4 py-2 text-sm" onClick={() => reset()}>
                Tentar novamente
              </button>
              <Link href="/codex" className="rounded-full border border-[var(--border)] px-4 py-2 text-sm">
                Ir para dashboard
              </Link>
            </div>
          </div>
        </main>
      </body>
    </html>
  );
}

