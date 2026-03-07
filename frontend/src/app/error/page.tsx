import Link from 'next/link';

export default function ErrorRoutePage() {
  return (
    <main className="grid min-h-screen place-items-center bg-[var(--background)] text-[var(--foreground)] px-4">
      <div className="max-w-md rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[var(--surface)] p-6 text-center">
        <p className="text-[12px] uppercase tracking-[0.16em] text-[var(--subtle-foreground)]">Error</p>
        <h1 className="mt-2 text-[24px] font-semibold tracking-[-0.04em]">Ocorreu uma falha</h1>
        <p className="mt-2 text-[14px] text-[var(--muted-foreground)]">Use os logs da task e as evidencias para diagnostico.</p>
        <Link href="/chat" className="mt-4 inline-flex rounded-full border border-[var(--border)] px-4 py-2 text-[14px]">
          Voltar ao inicio
        </Link>
      </div>
    </main>
  );
}

