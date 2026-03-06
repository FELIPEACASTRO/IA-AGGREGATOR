'use client';

import { FormEvent, useEffect, useMemo, useState } from 'react';
import { TaskMode } from '@prisma/client';
import { AudioLines, ImagePlus, Network, Play, RotateCcw, ShieldAlert, Sparkles } from 'lucide-react';
import { codexApi } from '@/lib/codex-api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';

type TaskComposerProps = {
  onCreated: () => Promise<void> | void;
  defaultPrompt?: string;
  defaultBranch?: string;
  defaultEnvironmentId?: string;
  defaultMode?: TaskMode;
};

type RepoOption = {
  id: string;
  fullName: string;
  defaultBranch: string;
};

type EnvironmentOption = {
  id: string;
  name: string;
  internetMode: string;
  defaultBranch: string;
};

export function TaskComposer({
  onCreated,
  defaultPrompt,
  defaultBranch,
  defaultEnvironmentId,
  defaultMode,
}: TaskComposerProps) {
  const [mode, setMode] = useState<TaskMode>(defaultMode ?? 'ASK');
  const [prompt, setPrompt] = useState(defaultPrompt ?? '');
  const [baseBranch, setBaseBranch] = useState(defaultBranch ?? 'main');
  const [bestOfN, setBestOfN] = useState(1);
  const [repoId, setRepoId] = useState('');
  const [environmentId, setEnvironmentId] = useState(defaultEnvironmentId ?? '');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [repositories, setRepositories] = useState<RepoOption[]>([]);
  const [environments, setEnvironments] = useState<EnvironmentOption[]>([]);
  const [imageInputs, setImageInputs] = useState<string[]>([]);
  const [voiceTranscript, setVoiceTranscript] = useState('');

  useEffect(() => {
    codexApi
      .listRepositories()
      .then((repos) => {
        const casted = repos as RepoOption[];
        setRepositories(casted);
        if (casted[0]) {
          setRepoId(casted[0].id);
          setBaseBranch(defaultBranch || casted[0].defaultBranch);
        }
      })
      .catch(() => {
        setRepositories([]);
      });
    codexApi
      .listEnvironments()
      .then((envs) => {
        const casted = envs as EnvironmentOption[];
        setEnvironments(casted);
        if (casted[0] && !defaultEnvironmentId) setEnvironmentId(casted[0].id);
      })
      .catch(() => {
        setEnvironments([]);
      });
  }, [defaultBranch, defaultEnvironmentId]);

  const selectedEnvironment = useMemo(
    () => environments.find((item) => item.id === environmentId),
    [environments, environmentId]
  );

  const canSubmit = prompt.trim().length > 3 && repoId && environmentId && !isSubmitting;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canSubmit) return;
    setIsSubmitting(true);
    setError(null);
    try {
      await codexApi.createTask({
        prompt,
        mode,
        repositoryId: repoId,
        environmentId,
        baseBranch,
        bestOfN,
        imageInputs,
        voiceTranscript: voiceTranscript.trim() || undefined,
        attachments: [],
      });
      setPrompt('');
      setImageInputs([]);
      setVoiceTranscript('');
      await onCreated();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao criar task');
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form
      onSubmit={submit}
      className="rounded-[var(--radius-2xl)] border border-[var(--border)] bg-[rgba(8,17,31,0.84)] p-4 shadow-[var(--shadow-lg)]"
    >
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <button
          type="button"
          onClick={() => setMode('ASK')}
          className={`rounded-full border px-3 py-1.5 text-xs ${mode === 'ASK' ? 'border-[rgba(92,214,168,0.4)] bg-[rgba(92,214,168,0.18)] text-[var(--foreground)]' : 'border-[var(--border)] text-[var(--muted-foreground)]'}`}
        >
          ASK
        </button>
        <button
          type="button"
          onClick={() => setMode('CODE')}
          className={`rounded-full border px-3 py-1.5 text-xs ${mode === 'CODE' ? 'border-[rgba(96,115,255,0.4)] bg-[rgba(96,115,255,0.2)] text-[var(--foreground)]' : 'border-[var(--border)] text-[var(--muted-foreground)]'}`}
        >
          CODE
        </button>
        <span className="inline-flex items-center gap-2 rounded-full border border-[var(--border)] px-2.5 py-1 text-[0.7rem] text-[var(--muted-foreground)]">
          <ShieldAlert className="h-3.5 w-3.5" />
          Agent sem internet apos setup
        </span>
        <span className="inline-flex items-center gap-2 rounded-full border border-[var(--border)] px-2.5 py-1 text-[0.7rem] text-[var(--muted-foreground)]">
          <Network className="h-3.5 w-3.5" />
          Policy: {selectedEnvironment?.internetMode ?? 'OFF'}
        </span>
      </div>

      <Textarea
        value={prompt}
        onChange={(event) => setPrompt(event.target.value)}
        placeholder="Descreva a task cloud. Exemplo: revise o fluxo de auth e proponha patch com testes."
        rows={5}
      />

      <div className="mt-3 grid gap-3 md:grid-cols-2 xl:grid-cols-5">
        <label className="space-y-1">
          <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Repositorio</span>
          <select
            value={repoId}
            onChange={(event) => setRepoId(event.target.value)}
            className="h-11 w-full rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] px-3 text-sm"
          >
            {repositories.map((repo) => (
              <option key={repo.id} value={repo.id}>
                {repo.fullName}
              </option>
            ))}
          </select>
        </label>
        <label className="space-y-1">
          <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Environment</span>
          <select
            value={environmentId}
            onChange={(event) => setEnvironmentId(event.target.value)}
            className="h-11 w-full rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] px-3 text-sm"
          >
            {environments.map((env) => (
              <option key={env.id} value={env.id}>
                {env.name}
              </option>
            ))}
          </select>
        </label>
        <label className="space-y-1">
          <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Branch</span>
          <Input value={baseBranch} onChange={(event) => setBaseBranch(event.target.value)} />
        </label>
        <label className="space-y-1">
          <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Best of N</span>
          <select
            value={bestOfN}
            onChange={(event) => setBestOfN(Number(event.target.value))}
            className="h-11 w-full rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.02)] px-3 text-sm"
          >
            {[1, 2, 3, 4, 5].map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        </label>
        <label className="space-y-1">
          <span className="text-xs uppercase tracking-[0.14em] text-[var(--subtle-foreground)]">Voice dictation</span>
          <Input
            value={voiceTranscript}
            onChange={(event) => setVoiceTranscript(event.target.value)}
            placeholder="Transcricao opcional"
          />
        </label>
      </div>

      <div className="mt-3 flex flex-wrap items-center gap-2">
        <button
          type="button"
          onClick={() => {
            const demo = `/uploads/reference-${Date.now()}.png`;
            setImageInputs((prev) => [...prev, demo]);
          }}
          className="inline-flex items-center gap-2 rounded-full border border-[var(--border)] px-3 py-2 text-xs text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
        >
          <ImagePlus className="h-4 w-4" />
          Attach image
        </button>
        <button
          type="button"
          onClick={() => setVoiceTranscript((prev) => `${prev} [dictation sample]`.trim())}
          className="inline-flex items-center gap-2 rounded-full border border-[var(--border)] px-3 py-2 text-xs text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
        >
          <AudioLines className="h-4 w-4" />
          Voice dictation
        </button>
        <button
          type="button"
          onClick={() => {
            setPrompt('');
            setImageInputs([]);
            setVoiceTranscript('');
          }}
          className="inline-flex items-center gap-2 rounded-full border border-[var(--border)] px-3 py-2 text-xs text-[var(--muted-foreground)] hover:text-[var(--foreground)]"
        >
          <RotateCcw className="h-4 w-4" />
          Reset
        </button>
        <div className="ml-auto flex items-center gap-2">
          <Button type="submit" disabled={!canSubmit} className="inline-flex items-center gap-2">
            <Play className="h-4 w-4" />
            {isSubmitting ? 'Launching...' : 'Run Cloud Task'}
          </Button>
        </div>
      </div>

      {(imageInputs.length > 0 || voiceTranscript.trim()) && (
        <div className="mt-3 rounded-[var(--radius-lg)] border border-[var(--border)] bg-[rgba(255,255,255,0.03)] p-3 text-xs text-[var(--muted-foreground)]">
          <div className="flex items-center gap-2 text-[var(--foreground)]">
            <Sparkles className="h-4 w-4" />
            Attachments context
          </div>
          {imageInputs.length > 0 && <p className="mt-2">Images: {imageInputs.length}</p>}
          {voiceTranscript.trim() && <p className="mt-1">Voice: {voiceTranscript}</p>}
        </div>
      )}

      {error && <p className="mt-3 text-sm text-[var(--destructive)]">{error}</p>}
    </form>
  );
}
