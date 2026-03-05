'use client';

import { AppShell } from '@/components/app/app-shell';
import { useRouter } from 'next/navigation';
import { trackEvent } from '@/lib/analytics';
import { motion } from 'framer-motion';
import { cn } from '@/lib/cn';
import { useState } from 'react';
import {
  WandSparkles, Sparkles, BarChart3, FileText,
  Mail, Target, Search, Hash, Star, ChevronRight,
} from 'lucide-react';

type Category = 'all' | 'analysis' | 'writing' | 'planning';

const templates = [
  {
    category: 'analysis' as Category,
    icon: BarChart3,
    iconColor: 'text-[var(--brand-primary)]',
    iconBg: 'bg-[var(--brand-primary)]/10',
    title: 'Resumo Executivo',
    description: 'Condensa informações em tópicos estratégicos para liderança.',
    tag: 'Análise',
    prompt: 'Crie um resumo executivo estruturado em até 7 bullets com foco em insights e decisões estratégicas sobre o seguinte tema:',
    uses: 847,
  },
  {
    category: 'planning' as Category,
    icon: Target,
    iconColor: 'text-[var(--success)]',
    iconBg: 'bg-[var(--success)]/10',
    title: 'Plano de Ação',
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
    description: 'Rascunho claro, objetivo e com tom apropriado para comunicações formais.',
    tag: 'Escrita',
    prompt: 'Escreva um e-mail profissional com tom cordial, objetivo e estrutura clara (assunto, abertura, corpo, encerramento) sobre:',
    uses: 1203,
  },
  {
    category: 'analysis' as Category,
    icon: BarChart3,
    iconColor: 'text-[var(--warning)]',
    iconBg: 'bg-[var(--warning)]/10',
    title: 'Análise Comparativa',
    description: 'Compara duas ou mais alternativas com critérios objetivos e recomendação final.',
    tag: 'Análise',
    prompt: 'Compare as alternativas abaixo em uma tabela com critérios objetivos (custo, tempo, risco, benefícios) e conclua com uma recomendação justificada:',
    uses: 489,
  },
  {
    category: 'writing' as Category,
    icon: FileText,
    iconColor: 'text-[var(--muted-foreground)]',
    iconBg: 'bg-[var(--surface-3)]',
    title: 'Documento Técnico',
    description: 'Estrutura clara para documentação técnica, RFCs ou especificações.',
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
    description: 'Define Objectives & Key Results claros e mensuráveis para equipes.',
    tag: 'Planejamento',
    prompt: 'Defina 3 Objectives e 3 Key Results cada para o seguinte contexto de equipe ou área. Seja específico, mensurável e com prazo trimestral:',
    uses: 278,
  },
];

const categories: { value: Category; label: string }[] = [
  { value: 'all', label: 'Todos' },
  { value: 'analysis', label: 'Análise' },
  { value: 'planning', label: 'Planejamento' },
  { value: 'writing', label: 'Escrita' },
];

export default function PromptsPage() {
  const router = useRouter();
  const [activeCategory, setActiveCategory] = useState<Category>('all');
  const [search, setSearch] = useState('');

  const filtered = templates.filter((t) => {
    const matchCat = activeCategory === 'all' || t.category === activeCategory;
    const matchSearch = t.title.toLowerCase().includes(search.toLowerCase()) || t.description.toLowerCase().includes(search.toLowerCase());
    return matchCat && matchSearch;
  });

  return (
    <AppShell title="Templates" subtitle="Prompts curados para acelerar qualquer tarefa"
      headerActions={
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-[var(--muted-foreground)]" />
          <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Buscar templates..."
            className="h-8 w-44 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface-2)] pl-8 pr-3 text-[var(--text-xs)] placeholder:text-[var(--muted-foreground)] focus:outline-none focus:ring-1 focus:ring-[var(--ring)]" />
        </div>
      }>
      <div className="py-6 space-y-5">
        {/* Category filters */}
        <div className="flex flex-wrap gap-2">
          {categories.map((cat) => (
            <button key={cat.value} onClick={() => setActiveCategory(cat.value)}
              className={cn('rounded-[var(--radius-pill)] border px-3.5 py-1.5 text-[var(--text-xs)] font-medium transition-all',
                activeCategory === cat.value
                  ? 'border-[var(--brand-primary)] bg-[var(--brand-primary)]/10 text-[var(--brand-primary)]'
                  : 'border-[var(--border)] bg-[var(--surface-1)] text-[var(--muted-foreground)] hover:border-[var(--border-strong)]')}>
              {cat.label}
              {cat.value !== 'all' && (
                <span className="ml-1.5 text-[10px] opacity-60">
                  {templates.filter((t) => t.category === cat.value).length}
                </span>
              )}
            </button>
          ))}
          <div className="flex items-center gap-1.5 ml-auto text-[var(--text-xs)] text-[var(--muted-foreground)]">
            <Sparkles className="h-3.5 w-3.5 text-[var(--brand-primary)]" />
            {filtered.length} template{filtered.length !== 1 ? 's' : ''}
          </div>
        </div>

        {/* Template grid */}
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {filtered.map((template, i) => (
            <motion.article key={template.title} initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.25, delay: i * 0.04 }}
              className="group flex flex-col rounded-[var(--radius-xl)] border border-[var(--border)] bg-[var(--surface-1)] p-5 hover:border-[var(--border-strong)] hover:shadow-[var(--shadow-md)] transition-all">
              <div className="flex items-start justify-between mb-3">
                <div className={cn('flex h-9 w-9 items-center justify-center rounded-[var(--radius-lg)] shrink-0', template.iconBg)}>
                  <template.icon className={cn('h-4.5 w-4.5', template.iconColor)} />
                </div>
                <div className="flex items-center gap-2">
                  <span className="inline-flex items-center gap-1 text-[10px] text-[var(--subtle-foreground)]">
                    <Star className="h-2.5 w-2.5" /> {template.uses.toLocaleString()}
                  </span>
                  <span className={cn('rounded-full px-2 py-0.5 text-[10px] font-medium', template.iconBg, template.iconColor)}>
                    {template.tag}
                  </span>
                </div>
              </div>

              <h3 className="font-semibold text-[var(--text-base)] mb-1.5">{template.title}</h3>
              <p className="text-[var(--text-sm)] text-[var(--muted-foreground)] leading-relaxed flex-1 mb-4">{template.description}</p>

              <div className="rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--surface-2)] px-3 py-2.5 mb-4">
                <p className="text-[var(--text-xs)] text-[var(--muted-foreground)] leading-relaxed line-clamp-3 font-mono">
                  {template.prompt}
                </p>
              </div>

              <button onClick={() => { trackEvent('prompts_use_template', { template: template.title }); router.push(`/chat?prompt=${encodeURIComponent(template.prompt)}`); }}
                className="flex w-full items-center justify-center gap-2 rounded-[var(--radius-md)] bg-[var(--brand-primary)] py-2.5 text-[var(--text-xs)] font-medium text-white hover:opacity-90 active:scale-[0.98] transition-all">
                <WandSparkles className="h-3.5 w-3.5" /> Usar template
                <ChevronRight className="h-3 w-3 ml-auto" />
              </button>
            </motion.article>
          ))}
        </div>
      </div>
    </AppShell>
  );
}
