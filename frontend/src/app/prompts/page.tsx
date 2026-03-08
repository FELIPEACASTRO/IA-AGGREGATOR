'use client';

import { useRouter } from 'next/navigation';
import { trackEvent } from '@/lib/analytics';
import { useMemo, useState } from 'react';
import { AppLayout } from '@/components/app/app-layout';
import { FilterPill } from '@/components/ui/filter-pill';
import { SearchField } from '@/components/ui/search-field';
import {
  BarChart3,
  ChevronRight,
  FileText,
  Hash,
  Mail,
  Target,
  WandSparkles,
} from 'lucide-react';

type Category = 'all' | 'analysis' | 'writing' | 'planning';

const templates = [
  {
    category: 'analysis' as Category,
    icon: BarChart3,
    title: 'Resumo Executivo',
    description: 'Condensa informações em topicos estrategicos para lideranca.',
    tag: 'Analise',
    prompt: 'Crie um resumo executivo estruturado em até 7 bullets com foco em insights e decisões estratégicas sobre o seguinte tema:',
  },
  {
    category: 'planning' as Category,
    icon: Target,
    title: 'Plano de Acao',
    description: 'Transforma objetivos em plano estruturado com etapas, donos e riscos.',
    tag: 'Planejamento',
    prompt: 'Crie um plano de ação detalhado com: etapas claras, responsáveis, prazos estimados, dependências e principais riscos para:',
  },
  {
    category: 'writing' as Category,
    icon: Mail,
    title: 'E-mail Profissional',
    description: 'Rascunho claro, objetivo e com tom apropriado para comunicações formais.',
    tag: 'Escrita',
    prompt: 'Escreva um e-mail profissional com tom cordial, objetivo e estrutura clara (assunto, abertura, corpo, encerramento) sobre:',
  },
  {
    category: 'analysis' as Category,
    icon: BarChart3,
    title: 'Analise Comparativa',
    description: 'Compara alternativas com criterios objetivos e recomendacao final.',
    tag: 'Analise',
    prompt: 'Compare as alternativas abaixo em uma tabela com critérios objetivos (custo, tempo, risco, benefícios) e conclua com uma recomendação justificada:',
  },
  {
    category: 'writing' as Category,
    icon: FileText,
    title: 'Documento Tecnico',
    description: 'Estrutura clara para documentação tecnica, RFCs ou especificações.',
    tag: 'Escrita',
    prompt: 'Escreva um documento técnico com seções: Objetivo, Contexto, Solução proposta, Requisitos, Considerações e Plano de implementação para:',
  },
  {
    category: 'planning' as Category,
    icon: Hash,
    title: 'OKRs e Metas',
    description: 'Define Objectives & Key Results claros e mensuraveis para equipes.',
    tag: 'Planejamento',
    prompt: 'Defina 3 Objectives e 3 Key Results cada para o seguinte contexto de equipe ou área. Seja específico, mensurável e com prazo trimestral:',
  },
];

const categories: { value: Category; label: string }[] = [
  { value: 'all', label: 'Todos' },
  { value: 'analysis', label: 'Analise' },
  { value: 'planning', label: 'Planejamento' },
  { value: 'writing', label: 'Escrita' },
];

export default function PromptsPage() {
  const router = useRouter();
  const [activeCategory, setActiveCategory] = useState<Category>('all');
  const [search, setSearch] = useState('');

  const filtered = useMemo(
    () =>
      templates.filter((t) => {
        const matchCategory = activeCategory === 'all' || t.category === activeCategory;
        const content = `${t.title} ${t.description}`.toLowerCase();
        return matchCategory && content.includes(search.toLowerCase());
      }),
    [activeCategory, search],
  );

  const applyTemplate = (template: (typeof templates)[0]) => {
    trackEvent('prompts_use_template', { template: template.title });
    router.push(`/chat?prompt=${encodeURIComponent(template.prompt)}`);
  };

  return (
    <AppLayout>
      <div className="mx-auto max-w-4xl px-6 py-8 space-y-6">
        {/* Header */}
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="font-[var(--font-serif)] text-[32px] font-medium tracking-[-0.04em] text-[var(--foreground)]">Templates</h1>
            <p className="mt-2 text-[14px] text-[var(--muted-foreground)]">
              Prompts curados para acelerar qualquer tarefa.
            </p>
          </div>
          <SearchField value={search} onChange={setSearch} placeholder="Buscar templates..." className="w-56" />
        </div>

        {/* Categories */}
        <div className="flex flex-wrap items-center gap-2">
          {categories.map((cat) => (
            <FilterPill
              key={cat.value}
              onClick={() => setActiveCategory(cat.value)}
              active={activeCategory === cat.value}
              count={cat.value !== 'all' ? templates.filter((t) => t.category === cat.value).length : undefined}
            >
              {cat.label}
            </FilterPill>
          ))}
          <span className="ml-auto text-[12px] text-[var(--muted-foreground)]">
            {filtered.length} template{filtered.length !== 1 ? 's' : ''}
          </span>
        </div>

        {/* Template cards */}
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {filtered.map((template) => (
            <article
              key={template.title}
              className="flex flex-col rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface)] p-5 shadow-[var(--shadow-xs)]"
            >
              <div className="flex items-start justify-between gap-2">
                <template.icon className="h-5 w-5 shrink-0 text-[var(--muted-foreground)]" />
                <span className="rounded-[var(--radius-full)] border border-[var(--border)] px-2 py-0.5 text-[11px] font-medium text-[var(--muted-foreground)]">
                  {template.tag}
                </span>
              </div>
              <h3 className="mt-3 font-[var(--font-serif)] text-[22px] font-medium tracking-[-0.03em] text-[var(--foreground)]">{template.title}</h3>
              <p className="mt-1 flex-1 text-[13px] leading-relaxed text-[var(--muted-foreground)]">
                {template.description}
              </p>
              <div className="mt-3 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--background)] p-3">
                <p className="line-clamp-3 font-mono text-[12px] leading-relaxed text-[var(--muted-foreground)]">
                  {template.prompt}
                </p>
              </div>
              <button
                onClick={() => applyTemplate(template)}
                className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-[var(--radius-md)] bg-[var(--primary)] px-4 py-2.5 text-[13px] font-medium text-[var(--primary-foreground)] hover:opacity-90 transition-opacity"
              >
                <WandSparkles className="h-3.5 w-3.5" /> Usar template
                <ChevronRight className="ml-auto h-3.5 w-3.5" />
              </button>
            </article>
          ))}
        </div>
      </div>
    </AppLayout>
  );
}
