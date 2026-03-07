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
          <div className="max-w-lg rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[var(--surface)] p-6">
            <p className="text-[12px] uppercase tracking-[0.16em] text-[var(--subtle-foreground)]">Runtime Error</p>
            <h1 className="mt-2 text-[24px] font-semibold tracking-[-0.03em]">Falha ao processar a rota</h1>
            <p className="mt-2 text-[14px] text-[var(--muted-foreground)]">{error.message}</p>
            <div className="mt-4 flex gap-2">
              <button className="rounded-full border border-[var(--border)] px-4 py-2 text-[14px]" onClick={() => reset()}>
                Tentar novamente
              </button>
              <Link href="/chat" className="rounded-full border border-[var(--border)] px-4 py-2 text-[14px]">
                Ir para dashboard
              </Link>
            </div>
          </div>
        </main>
      </body>
    </html>
  );
}

