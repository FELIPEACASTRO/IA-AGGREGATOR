import { ReactNode } from 'react';

type AuthShellProps = {
  title: string;
  subtitle: string;
  children: ReactNode;
};

export function AuthShell({ title, subtitle, children }: AuthShellProps) {
  return (
    <main className="flex min-h-screen items-center justify-center p-4">
      <section className="w-full max-w-md space-y-6 rounded-2xl border border-[var(--border)] bg-[var(--background)] p-6 shadow-sm">
        <header className="text-center">
          <h1 className="text-3xl font-bold tracking-tight">{title}</h1>
          <p className="mt-2 text-sm text-[var(--muted-foreground)]">{subtitle}</p>
        </header>
        {children}
      </section>
    </main>
  );
}
