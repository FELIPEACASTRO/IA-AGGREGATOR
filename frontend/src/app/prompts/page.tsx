'use client';

import { AppShell } from '@/components/app/app-shell';
import { useRouter } from 'next/navigation';
import { trackEvent } from '@/lib/analytics';
import { motion } from 'framer-motion';
import { cn } from '@/lib/cn';
import { useEffect, useMemo, useState } from 'react';
import { PageSection, PageStack } from '@/components/app/page-blueprint';
import {
  BarChart3,
  ChevronRight,
  FileText,
  Mail,
  Search,
  Sparkles,
  Star,
  Target,
  WandSparkles,
  Workflow,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

type Category = 'all' | 'analysis' | 'writing' | 'planning';

type Template = {
  category: Category;
  icon: LucideIcon;
  iconColor: string;
  iconBg: string;
  title: string;
  description: string;
  tag: string;
  prompt: string;
  uses: number;
};

const CATEGORY_STYLE: Record<string, { icon: LucideIcon; color: string; bg: string; tag: string }> = {
  analysis:  { icon: BarChart3, color: 'text-[var(--brand-primary)]', bg: 'bg-[var(--brand-primary)]/10', tag: 'Analise' },
  planning:  { icon: Target, color: 'text-[var(--success)]', bg: 'bg-[var(--success)]/10', tag: 'Planejamento' },
  writing:   { icon: Mail, color: 'text-[var(--brand-secondary)]', bg: 'bg-[var(--brand-secondary)]/10', tag: 'Escrita' },
  custom:    { icon: FileText, color: 'text-[var(--muted-foreground)]', bg: 'bg-[var(--surface-3)]', tag: 'Custom' },
};

const FILTER_TABS: { value: Category; label: string }[] = [
  { value: 'all', label: 'Todos' },
  { value: 'analysis', label: 'Analise' },
  { value: 'planning', label: 'Planejamento' },
  { value: 'writing', label: 'Escrita' },
];

function mapCategoryFromBackend(cat?: string): Category {
  if (!cat) return 'writing';
  const lower = cat.toLowerCase();
  if (lower.includes('analy') || lower === 'analysis') return 'analysis';
  if (lower.includes('plan') || lower === 'planning') return 'planning';
  return 'writing';
}

function mapBackendAsset(asset: {
  name: string;
  description?: string;
  content: string;
  usageCount?: number;
  metadata?: Record<string, string>;
}): Template {
  const cat = mapCategoryFromBackend(asset.metadata?.category);
  const style = CATEGORY_STYLE[cat] ?? CATEGORY_STYLE.custom;
  return {
    category: cat,
    icon: style.icon,
    iconColor: style.color,
    iconBg: style.bg,
    title: asset.name,
    description: asset.description ?? '',
    tag: style.tag,
    prompt: asset.content,
    uses: asset.usageCount ?? 0,
  };
}

const FALLBACK_TEMPLATES: Template[] = [
  { category: 'analysis', icon: BarChart3, iconColor: 'text-[var(--brand-primary)]', iconBg: 'bg-[var(--brand-primary)]/10', title: 'Resumo Executivo', description: 'Condensa informações em topicos estrategicos para lideranca.', tag: 'Analise', prompt: 'Crie um resumo executivo estruturado em até 7 bullets com foco em insights e decisões estratégicas sobre o seguinte tema:', uses: 0 },
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
  const [templates, setTemplates] = useState<Template[]>(FALLBACK_TEMPLATES);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const API_BASE = process.env.NEXT_PUBLIC_API_URL || '';
    fetch(`${API_BASE}/api/v1/library/assets?type=PROMPT_TEMPLATE&size=50`, { cache: 'no-store' })
      .then((res) => { if (!res.ok) throw new Error(`HTTP ${res.status}`); return res.json(); })
      .then((data: unknown) => {
        if (Array.isArray(data) && data.length > 0) {
          setTemplates(data.map((a) => mapBackendAsset(a as Parameters<typeof mapBackendAsset>[0])));
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const filtered = useMemo(
    () => templates.filter((template) => {
      const matchCategory = activeCategory === 'all' || template.category === activeCategory;
      const content = `${template.title} ${template.description}`.toLowerCase();
      return matchCategory && content.includes(search.toLowerCase());
    }),
    [activeCategory, search, templates],
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
                Estruture seus fluxos como um catálogo de playbooks, não como uma lista solta de prompts.
              </h2>
              <p className="mt-3 max-w-2xl text-[var(--text-sm)] text-[var(--muted-foreground)]">
                O objetivo aqui e operacional: descoberta clara, contexto rapido e execucao em um clique com CTA recorrente.
              </p>
              <div className="mt-5 grid gap-4 sm:grid-cols-3">
                <TopMetric label="Catalogo" value={loading ? '...' : String(templates.length)} helper="templates ativos no workspace" />
                <TopMetric label="Categorias" value={String(FILTER_TABS.length - 1)} helper="analise, planejamento e escrita" />
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
          {FILTER_TABS.map((category) => (
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
