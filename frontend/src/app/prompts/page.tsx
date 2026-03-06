'use client';

import { AppShell } from '@/components/app/app-shell';
import { useRouter } from 'next/navigation';
import { trackEvent } from '@/lib/analytics';
import { motion } from 'framer-motion';
import { cn } from '@/lib/cn';
import { useMemo, useState } from 'react';
import { PageSection, PageStack } from '@/components/app/page-blueprint';
import {
  BarChart3,
  ChevronRight,
  FileText,
  Hash,
  Mail,
  Search,
  Sparkles,
  Star,
  Target,
  WandSparkles,
  Workflow,
} from 'lucide-react';

type Category = 'all' | 'analysis' | 'writing' | 'planning';

const templates = [
  {
    category: 'analysis' as Category,
    icon: BarChart3,
    iconColor: 'text-[var(--brand-primary)]',
    iconBg: 'bg-[var(--brand-primary)]/10',
    title: 'Resumo Executivo',
    description: 'Condensa informacoes em topicos estrategicos para lideranca.',
    tag: 'Analise',
    prompt: 'Crie um resumo executivo estruturado em até 7 bullets com foco em insights e decisões estratégicas sobre o seguinte tema:',
    uses: 847,
  },
  {
    category: 'planning' as Category,
    icon: Target,
    iconColor: 'text-[var(--success)]',
    iconBg: 'bg-[var(--success)]/10',
    title: 'Plano de Acao',
    description: 'Transforma objetivos em plano estruturado com etapas, donos e riscos.',
    tag: 'Planejamento',
    prompt: 'Crie um plano de ação detalhado com: etapas claras, responsáveis, prazos estimados, dependências e principais riscos para:',
    uses: 652,
  },
  {
    category: 'writing' as Category,
    icon: Mail,
    iconColor: 'text-[var(--brand-secondary)]',
    iconBg: 'bg-[var(--brand-secondary)]/10',
    title: 'E-mail Profissional',
    description: 'Rascunho claro, objetivo e com tom apropriado para comunicacoes formais.',
    tag: 'Escrita',
    prompt: 'Escreva um e-mail profissional com tom cordial, objetivo e estrutura clara (assunto, abertura, corpo, encerramento) sobre:',
    uses: 1203,
  },
  {
    category: 'analysis' as Category,
    icon: BarChart3,
    iconColor: 'text-[var(--warning)]',
    iconBg: 'bg-[var(--warning)]/10',
    title: 'Analise Comparativa',
    description: 'Compara duas ou mais alternativas com criterios objetivos e recomendacao final.',
    tag: 'Analise',
    prompt: 'Compare as alternativas abaixo em uma tabela com critérios objetivos (custo, tempo, risco, benefícios) e conclua com uma recomendação justificada:',
    uses: 489,
  },
  {
    category: 'writing' as Category,
    icon: FileText,
    iconColor: 'text-[var(--muted-foreground)]',
    iconBg: 'bg-[var(--surface-3)]',
    title: 'Documento Tecnico',
    description: 'Estrutura clara para documentacao tecnica, RFCs ou especificacoes.',
    tag: 'Escrita',
    prompt: 'Escreva um documento técnico com seções: Objetivo, Contexto, Solução proposta, Requisitos, Considerações e Plano de implementação para:',
    uses: 312,
  },
  {
    category: 'planning' as Category,
    icon: Hash,
    iconColor: 'text-[var(--brand-primary)]',
    iconBg: 'bg-[var(--brand-primary)]/10',
    title: 'OKRs e Metas',
    description: 'Define Objectives & Key Results claros e mensuraveis para equipes.',
    tag: 'Planejamento',
    prompt: 'Defina 3 Objectives e 3 Key Results cada para o seguinte contexto de equipe ou área. Seja específico, mensurável e com prazo trimestral:',
    uses: 278,
  },
];

const categories: { value: Category; label: string }[] = [
  { value: 'all', label: 'Todos' },
  { value: 'analysis', label: 'Analise' },
  { value: 'planning', label: 'Planejamento' },
  { value: 'writing', label: 'Escrita' },
];

function TopMetric({ label, value, helper }: { label: string; value: string; helper: string }) {
  return (
    <div className="rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-4">
      <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">{label}</p>
      <p className="mt-2 text-[var(--text-2xl)] font-semibold text-[var(--foreground)]">{value}</p>
      <p className="mt-1 text-[var(--text-xs)] text-[var(--muted-foreground)]">{helper}</p>
    </div>
  );
}

export default function PromptsPage() {
  const router = useRouter();
  const [activeCategory, setActiveCategory] = useState<Category>('all');
  const [search, setSearch] = useState('');

  const filtered = useMemo(
    () => templates.filter((template) => {
      const matchCategory = activeCategory === 'all' || template.category === activeCategory;
      const content = `${template.title} ${template.description}`.toLowerCase();
      return matchCategory && content.includes(search.toLowerCase());
    }),
    [activeCategory, search],
  );

  const topTemplate = filtered[0] ?? templates[0];

  return (
    <AppShell
      title="Templates"
      subtitle="Prompts curados para acelerar qualquer tarefa"
      headerActions={
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-[var(--muted-foreground)]" />
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Buscar templates..."
            className="lume-field h-11 w-[min(18rem,50vw)] pl-9"
          />
        </div>
      }
    >
      <PageStack>
        <PageSection className="p-5 md:p-6">
          <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr] xl:items-start">
            <div>
              <span className="lume-kicker">
                <Workflow className="h-3.5 w-3.5" /> Prompt operating system
              </span>
              <h2 className="mt-5 max-w-3xl text-[var(--text-3xl)] font-semibold text-[var(--foreground)]">
                Estruture seus fluxos como um catalogo de playbooks, nao como uma lista solta de prompts.
              </h2>
              <p className="mt-3 max-w-2xl text-[var(--text-sm)] text-[var(--muted-foreground)]">
                O objetivo aqui e operacional: descoberta clara, contexto rapido e execucao em um clique com CTA recorrente.
              </p>
              <div className="mt-5 grid gap-4 sm:grid-cols-3">
                <TopMetric label="Catalogo" value={String(templates.length)} helper="templates ativos no workspace" />
                <TopMetric label="Categorias" value={String(categories.length - 1)} helper="analise, planejamento e escrita" />
                <TopMetric label="Mais usado" value={topTemplate.title} helper={`${topTemplate.uses.toLocaleString('pt-BR')} usos acumulados`} />
              </div>
            </div>

            <div className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-5 shadow-[var(--shadow-sm)]">
              <p className="text-[0.72rem] font-semibold uppercase tracking-[0.18em] text-[var(--subtle-foreground)]">Template em foco</p>
              <div className="mt-4 flex items-start gap-3">
                <div className={cn('inline-flex h-11 w-11 items-center justify-center rounded-[18px] border border-[var(--border)]', topTemplate.iconBg)}>
                  <topTemplate.icon className={cn('h-5 w-5', topTemplate.iconColor)} />
                </div>
                <div>
                  <h3 className="text-[var(--text-xl)] font-semibold text-[var(--foreground)]">{topTemplate.title}</h3>
                  <p className="mt-1 text-[var(--text-sm)] text-[var(--muted-foreground)]">{topTemplate.description}</p>
                </div>
              </div>
              <div className="mt-5 rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.52)] p-4">
                <p className="text-[0.78rem] leading-relaxed text-[var(--muted-foreground)]">{topTemplate.prompt}</p>
              </div>
              <button
                onClick={() => {
                  trackEvent('prompts_use_template', { template: topTemplate.title });
                  router.push(`/chat?prompt=${encodeURIComponent(topTemplate.prompt)}`);
                }}
                className="mt-5 inline-flex items-center gap-2 rounded-[var(--radius-pill)] bg-[var(--brand-primary)] px-5 py-3 text-[var(--text-xs)] font-semibold text-white shadow-[var(--shadow-brand)] hover:-translate-y-0.5"
              >
                <WandSparkles className="h-4 w-4" /> Usar template
                <ChevronRight className="h-3.5 w-3.5" />
              </button>
            </div>
          </div>
        </PageSection>

        <section className="flex flex-wrap items-center gap-2">
          {categories.map((category) => (
            <button
              key={category.value}
              onClick={() => setActiveCategory(category.value)}
              className={cn(
                'rounded-[var(--radius-pill)] border px-4 py-2 text-[var(--text-xs)] font-semibold transition-colors',
                activeCategory === category.value
                  ? 'border-[var(--brand-primary)] bg-[rgba(96,115,255,0.1)] text-[var(--brand-primary)]'
                  : 'border-[var(--border)] bg-[rgba(255,255,255,0.03)] text-[var(--muted-foreground)] hover:text-[var(--foreground)]',
              )}
            >
              {category.label}
              {category.value !== 'all' ? (
                <span className="ml-2 text-[0.68rem] text-[var(--subtle-foreground)]">
                  {templates.filter((template) => template.category === category.value).length}
                </span>
              ) : null}
            </button>
          ))}
          <div className="ml-auto inline-flex items-center gap-2 rounded-[var(--radius-pill)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] px-4 py-2 text-[var(--text-xs)] text-[var(--muted-foreground)]">
            <Sparkles className="h-3.5 w-3.5 text-[var(--brand-primary)]" />
            {filtered.length} template{filtered.length !== 1 ? 's' : ''}
          </div>
        </section>

        <section className="grid gap-4 md:grid-cols-2 2xl:grid-cols-3">
          {filtered.map((template, index) => (
            <motion.article
              key={template.title}
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.24, delay: index * 0.03 }}
              className="lume-panel-soft flex h-full flex-col rounded-[var(--radius-2xl)] p-5"
            >
              <div className="flex items-start justify-between gap-3">
                <div className={cn('inline-flex h-11 w-11 items-center justify-center rounded-[18px] border border-[var(--border)]', template.iconBg)}>
                  <template.icon className={cn('h-5 w-5', template.iconColor)} />
                </div>
                <div className="flex items-center gap-2 text-[0.72rem] text-[var(--subtle-foreground)]">
                  <span className="inline-flex items-center gap-1 rounded-full border border-[var(--border)] px-2.5 py-1">
                    <Star className="h-3 w-3" /> {template.uses.toLocaleString('pt-BR')}
                  </span>
                  <span className={cn('rounded-full px-2.5 py-1 font-semibold', template.iconBg, template.iconColor)}>{template.tag}</span>
                </div>
              </div>

              <h3 className="mt-5 text-[var(--text-lg)] font-semibold text-[var(--foreground)]">{template.title}</h3>
              <p className="mt-2 flex-1 text-[var(--text-sm)] leading-relaxed text-[var(--muted-foreground)]">{template.description}</p>

              <div className="mt-4 rounded-[var(--radius-xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.52)] p-4">
                <p className="line-clamp-4 font-mono text-[0.78rem] leading-relaxed text-[var(--muted-foreground)]">{template.prompt}</p>
              </div>

              <button
                onClick={() => {
                  trackEvent('prompts_use_template', { template: template.title });
                  router.push(`/chat?prompt=${encodeURIComponent(template.prompt)}`);
                }}
                className="mt-5 inline-flex w-full items-center justify-center gap-2 rounded-[var(--radius-pill)] bg-[var(--brand-primary)] px-5 py-3 text-[var(--text-xs)] font-semibold text-white shadow-[var(--shadow-brand)] hover:-translate-y-0.5"
              >
                <WandSparkles className="h-4 w-4" /> Usar template
                <ChevronRight className="ml-auto h-3.5 w-3.5" />
              </button>
            </motion.article>
          ))}
        </section>
      </PageStack>
    </AppShell>
  );
}
