'use client';

import { AppShell } from '@/components/app/app-shell';
import { Button } from '@/components/ui/button';
import { useRouter } from 'next/navigation';
import { trackEvent } from '@/lib/analytics';

const templates = [
  {
    title: 'Resumo Executivo',
    description: 'Gera um resumo em tópicos para liderança.',
    prompt: 'Resuma o texto abaixo em até 7 bullets com foco executivo:',
  },
  {
    title: 'Plano de Ação',
    description: 'Transforma objetivo em plano com etapas e riscos.',
    prompt: 'Crie um plano de ação detalhado com etapas, donos e riscos:',
  },
  {
    title: 'E-mail Profissional',
    description: 'Rascunho claro e objetivo para comunicação formal.',
    prompt: 'Escreva um e-mail profissional com tom cordial sobre:',
  },
];

export default function PromptsPage() {
  const router = useRouter();

  return (
    <AppShell title="Prompts" subtitle="Templates reutilizáveis para acelerar tarefas">
      <div className="grid gap-4 p-6 md:grid-cols-2 xl:grid-cols-3">
        {templates.map((template) => (
          <article key={template.title} className="rounded-xl border border-[var(--border)] p-4">
            <h3 className="font-semibold">{template.title}</h3>
            <p className="mt-1 text-sm text-[var(--muted-foreground)]">{template.description}</p>
            <p className="mt-3 rounded-lg bg-[var(--secondary)] p-2 text-xs">{template.prompt}</p>
            <Button
              variant="secondary"
              className="mt-3 w-full"
              onClick={() => {
                trackEvent('prompts_use_template', { template: template.title });
                router.push(`/chat?prompt=${encodeURIComponent(template.prompt)}`);
              }}
            >
              Usar template
            </Button>
          </article>
        ))}
      </div>
    </AppShell>
  );
}
