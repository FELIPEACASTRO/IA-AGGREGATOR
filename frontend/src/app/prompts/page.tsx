'use client';

import type { ElementType } from 'react';
import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { trackEvent } from '@/lib/analytics';
import type { PromptTemplateDto } from '@/lib/contracts/platform';
import { AppLayout } from '@/components/app/app-layout';
import { FilterPill } from '@/components/ui/filter-pill';
import { SearchField } from '@/components/ui/search-field';
import { toast } from '@/stores/toast-store';
import {
  BarChart3,
  ChevronRight,
  FileText,
  Hash,
  Mail,
  Target,
  WandSparkles,
} from 'lucide-react';

type Category = 'all' | string;

const iconByCategory: Record<string, ElementType> = {
  analysis: BarChart3,
  planning: Target,
  writing: Mail,
  default: FileText,
  operations: Hash,
};

type ApiEnvelope<T> = {
  success: boolean;
  data?: T;
  message?: string;
};

export default function PromptsPage() {
  const router = useRouter();
  const [templates, setTemplates] = useState<PromptTemplateDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeCategory, setActiveCategory] = useState<Category>('all');
  const [search, setSearch] = useState('');

  useEffect(() => {
    let active = true;

    const run = async () => {
      setLoading(true);
      try {
        const response = await fetch('/api/v1/prompts', {
          cache: 'no-store',
          credentials: 'include',
        });
        const payload = (await response.json()) as ApiEnvelope<{ templates: PromptTemplateDto[] }>;
        if (!response.ok || !payload.success || !payload.data) {
          throw new Error(payload.message || 'Falha ao carregar templates');
        }
        if (active) {
          setTemplates(payload.data.templates);
        }
      } catch (error) {
        if (active) {
          toast.error('Falha ao carregar templates', error instanceof Error ? error.message : 'Tente novamente.');
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };

    void run();
    return () => {
      active = false;
    };
  }, []);

  const categories = useMemo(() => {
    const unique = Array.from(new Set(templates.map((template) => template.category))).sort();
    return [
      { value: 'all', label: 'Todos' },
      ...unique.map((category) => ({
        value: category,
        label:
          category === 'analysis'
            ? 'Analise'
            : category === 'planning'
              ? 'Planejamento'
              : category === 'writing'
                ? 'Escrita'
                : category,
      })),
    ];
  }, [templates]);

  const filtered = useMemo(
    () =>
      templates.filter((template) => {
        const matchCategory = activeCategory === 'all' || template.category === activeCategory;
        const content = `${template.title} ${template.description} ${template.prompt}`.toLowerCase();
        return matchCategory && content.includes(search.toLowerCase());
      }),
    [activeCategory, search, templates],
  );

  const applyTemplate = async (template: PromptTemplateDto) => {
    try {
      const response = await fetch(`/api/v1/prompts/${template.id}/use`, {
        method: 'POST',
        credentials: 'include',
      });
      const payload = (await response.json()) as ApiEnvelope<{ template: PromptTemplateDto }>;
      if (!response.ok || !payload.success || !payload.data) {
        throw new Error(payload.message || 'Falha ao registrar uso do template');
      }

      trackEvent('prompts_use_template', { template: template.title, category: template.category });
      router.push(`/chat?prompt=${encodeURIComponent(payload.data.template.prompt)}`);
    } catch (error) {
      toast.error('Falha ao aplicar template', error instanceof Error ? error.message : 'Tente novamente.');
    }
  };

  return (
    <AppLayout>
      <div className="mx-auto max-w-4xl space-y-6 px-6 py-8">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="font-[var(--font-serif)] text-[32px] font-medium tracking-[-0.04em] text-[var(--foreground)]">
              Templates
            </h1>
            <p className="mt-2 text-[14px] text-[var(--muted-foreground)]">
              Prompts curados e persistidos no workspace para acelerar qualquer tarefa.
            </p>
          </div>
          <SearchField value={search} onChange={setSearch} placeholder="Buscar templates..." className="w-56" />
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {categories.map((cat) => (
            <FilterPill
              key={cat.value}
              onClick={() => setActiveCategory(cat.value)}
              active={activeCategory === cat.value}
              count={cat.value !== 'all' ? templates.filter((template) => template.category === cat.value).length : undefined}
            >
              {cat.label}
            </FilterPill>
          ))}
          <span className="ml-auto text-[12px] text-[var(--muted-foreground)]">
            {filtered.length} template{filtered.length !== 1 ? 's' : ''}
          </span>
        </div>

        {loading ? (
          <div className="rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface)] p-8 text-center text-[14px] text-[var(--muted-foreground)]">
            Carregando templates...
          </div>
        ) : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {filtered.map((template) => {
              const Icon = iconByCategory[template.category] ?? iconByCategory.default;
              return (
                <article
                  key={template.id}
                  className="flex flex-col rounded-[var(--radius-lg)] border border-[var(--border)] bg-[var(--surface)] p-5 shadow-[var(--shadow-xs)]"
                >
                  <div className="flex items-start justify-between gap-2">
                    <Icon className="h-5 w-5 shrink-0 text-[var(--muted-foreground)]" />
                    <span className="rounded-[var(--radius-full)] border border-[var(--border)] px-2 py-0.5 text-[11px] font-medium text-[var(--muted-foreground)]">
                      {template.tag || template.scope}
                    </span>
                  </div>
                  <h3 className="mt-3 font-[var(--font-serif)] text-[22px] font-medium tracking-[-0.03em] text-[var(--foreground)]">
                    {template.title}
                  </h3>
                  <p className="mt-1 flex-1 text-[13px] leading-relaxed text-[var(--muted-foreground)]">
                    {template.description}
                  </p>
                  <div className="mt-3 rounded-[var(--radius-md)] border border-[var(--border)] bg-[var(--background)] p-3">
                    <p className="line-clamp-3 font-mono text-[12px] leading-relaxed text-[var(--muted-foreground)]">
                      {template.prompt}
                    </p>
                  </div>
                  <button
                    onClick={() => void applyTemplate(template)}
                    className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-[var(--radius-md)] bg-[var(--primary)] px-4 py-2.5 text-[13px] font-medium text-[var(--primary-foreground)] transition-opacity hover:opacity-90"
                  >
                    <WandSparkles className="h-3.5 w-3.5" /> Usar template
                    <ChevronRight className="ml-auto h-3.5 w-3.5" />
                  </button>
                </article>
              );
            })}
          </div>
        )}
      </div>
    </AppLayout>
  );
}
