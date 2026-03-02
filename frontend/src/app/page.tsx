export default function Home() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-8">
      <div className="text-center">
        <h1 className="text-4xl font-bold mb-4">IA Aggregator</h1>
        <p className="text-lg text-[var(--muted-foreground)] mb-8">
          Plataforma de agregação de provedores de IA
        </p>
        <div className="flex gap-4 justify-center">
          <a
            href="/login"
            className="px-6 py-3 bg-[var(--primary)] text-[var(--primary-foreground)] rounded-lg font-medium hover:opacity-90 transition"
          >
            Entrar
          </a>
          <a
            href="/register"
            className="px-6 py-3 border border-[var(--border)] rounded-lg font-medium hover:bg-[var(--secondary)] transition"
          >
            Criar Conta
          </a>
        </div>
      </div>
    </main>
  );
}
