'use client';

import Link from 'next/link';
import { formatDistanceToNow } from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { Archive, ArrowUpRight, RefreshCw, Square, Undo2 } from 'lucide-react';
import { CodexTask, codexApi } from '@/lib/codex-api';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';

type TaskListProps = {
  tasks: CodexTask[];
  onRefresh: () => Promise<void> | void;
};

const statusTone: Record<string, 'default' | 'success' | 'warning' | 'error'> = {
  queued: 'default',
  preparing_environment: 'default',
  running_agent: 'default',
  validating: 'default',
  pr_ready: 'default',
  completed: 'success',
  failed: 'error',
  cancelled: 'warning',
  archived: 'warning',
};

export function TaskList({ tasks, onRefresh }: TaskListProps) {
  if (tasks.length === 0) {
    return (
      <div className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-6">
        <p className="text-sm text-[var(--muted-foreground)]">
          Nenhuma task encontrada para o filtro atual.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {tasks.map((task) => (
        <article
          key={task.id}
          className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4 shadow-[var(--shadow-lg)]"
        >
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <Badge variant={task.mode === 'CODE' ? 'default' : 'outline'}>{task.mode}</Badge>
                <Badge variant={statusTone[task.status] || 'default'}>{task.status}</Badge>
                {task.pullRequest?.status && task.pullRequest.status !== 'none' && (
                  <Badge variant="success">PR {task.pullRequest.status}</Badge>
                )}
              </div>
              <h3 className="mt-2 truncate text-base font-semibold tracking-[-0.03em]">{task.title}</h3>
              <p className="mt-1 line-clamp-2 text-sm text-[var(--muted-foreground)]">{task.prompt}</p>
              <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-[var(--subtle-foreground)]">
                <span>{task.repository?.fullName || 'Sem repo'}</span>
                <span>•</span>
                <span>{task.environment?.name || 'Sem environment'}</span>
                <span>•</span>
                <span>{task.baseBranch || 'main'}</span>
                <span>•</span>
                <span>{formatDistanceToNow(new Date(task.updatedAt), { addSuffix: true, locale: ptBR })}</span>
              </div>
            </div>

            <div className="flex flex-wrap gap-2">
              <Link href={`/codex/tasks/${task.id}`}>
                <Button size="sm" variant="outline" className="inline-flex items-center gap-2">
                  <ArrowUpRight className="h-4 w-4" />
                  Open
                </Button>
              </Link>
              <Button
                size="sm"
                variant="outline"
                className="inline-flex items-center gap-2"
                onClick={async () => {
                  await codexApi.retryTask(task.id);
                  await onRefresh();
                }}
              >
                <RefreshCw className="h-4 w-4" />
                Retry
              </Button>
              <Button
                size="sm"
                variant="outline"
                className="inline-flex items-center gap-2"
                onClick={async () => {
                  await codexApi.cancelTask(task.id);
                  await onRefresh();
                }}
              >
                <Square className="h-4 w-4" />
                Cancel
              </Button>
              {task.status === 'archived' ? (
                <Button
                  size="sm"
                  variant="outline"
                  className="inline-flex items-center gap-2"
                  onClick={async () => {
                    await codexApi.unarchiveTask(task.id);
                    await onRefresh();
                  }}
                >
                  <Undo2 className="h-4 w-4" />
                  Unarchive
                </Button>
              ) : (
                <Button
                  size="sm"
                  variant="outline"
                  className="inline-flex items-center gap-2"
                  onClick={async () => {
                    await codexApi.archiveTask(task.id);
                    await onRefresh();
                  }}
                >
                  <Archive className="h-4 w-4" />
                  Archive
                </Button>
              )}
            </div>
          </div>
        </article>
      ))}
    </div>
  );
}
