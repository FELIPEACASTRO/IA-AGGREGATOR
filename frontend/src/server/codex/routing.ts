import { InternetMode, RouteExecutionStatus, RouteServiceClass, TaskMode } from '@prisma/client';

type RoutePlanInput = {
  mode: TaskMode;
  prompt: string;
  internetMode: InternetMode;
  bestOfN: number;
  repositoryName?: string | null;
  sourceRef?: string | null;
  attachments: string[];
  imageInputs: string[];
  voiceTranscript?: string | null;
};

type RoutePlan = {
  serviceClass: RouteServiceClass;
  taskClass: string;
  policyKey: string;
  policyVersion: string;
  selectedProvider: string;
  selectedModel: string;
  fallbackProvider: string;
  fallbackModel: string;
  estimatedInputTokens: number;
  estimatedOutputTokens: number;
  estimatedCost: number;
  creditsToBurn: number;
  reason: string;
};

type UsageOutcomeInput = {
  taskMode: TaskMode;
  prompt: string;
  diffFileCount: number;
  artifactBytes: number;
  fallbackUsed: boolean;
  estimatedInputTokens: number;
  estimatedOutputTokens: number;
  estimatedCost: number;
  latencyMs: number;
};

type UsageOutcome = {
  inputTokens: number;
  outputTokens: number;
  actualCost: number;
  status: RouteExecutionStatus;
  metadata: Record<string, unknown>;
};

const POLICY_VERSION = '2026-03-09';

const MODEL_DEFAULTS: Record<RouteServiceClass, { provider: string; model: string; fallbackProvider: string; fallbackModel: string; creditMultiplier: number; outputFactor: number; costPer1k: number }> = {
  FAST: {
    provider: process.env.CODEX_FAST_PROVIDER || 'openai',
    model: process.env.CODEX_FAST_MODEL || 'gpt-4.1-mini',
    fallbackProvider: process.env.CODEX_FAST_FALLBACK_PROVIDER || 'openai',
    fallbackModel: process.env.CODEX_FAST_FALLBACK_MODEL || 'gpt-4.1-nano',
    creditMultiplier: 1,
    outputFactor: 1.1,
    costPer1k: 0.004,
  },
  BALANCED: {
    provider: process.env.CODEX_BALANCED_PROVIDER || 'openai',
    model: process.env.CODEX_BALANCED_MODEL || 'gpt-4.1',
    fallbackProvider: process.env.CODEX_BALANCED_FALLBACK_PROVIDER || 'anthropic',
    fallbackModel: process.env.CODEX_BALANCED_FALLBACK_MODEL || 'claude-3-7-sonnet',
    creditMultiplier: 2,
    outputFactor: 1.5,
    costPer1k: 0.012,
  },
  PREMIUM: {
    provider: process.env.CODEX_PREMIUM_PROVIDER || 'openai',
    model: process.env.CODEX_PREMIUM_MODEL || 'o3',
    fallbackProvider: process.env.CODEX_PREMIUM_FALLBACK_PROVIDER || 'anthropic',
    fallbackModel: process.env.CODEX_PREMIUM_FALLBACK_MODEL || 'claude-3-7-sonnet',
    creditMultiplier: 4,
    outputFactor: 1.8,
    costPer1k: 0.03,
  },
  RESEARCH: {
    provider: process.env.CODEX_RESEARCH_PROVIDER || 'openai',
    model: process.env.CODEX_RESEARCH_MODEL || 'gpt-4.1',
    fallbackProvider: process.env.CODEX_RESEARCH_FALLBACK_PROVIDER || 'perplexity',
    fallbackModel: process.env.CODEX_RESEARCH_FALLBACK_MODEL || 'sonar-pro',
    creditMultiplier: 3,
    outputFactor: 1.7,
    costPer1k: 0.018,
  },
  MULTIMODAL: {
    provider: process.env.CODEX_MULTIMODAL_PROVIDER || 'openai',
    model: process.env.CODEX_MULTIMODAL_MODEL || 'gpt-4.1',
    fallbackProvider: process.env.CODEX_MULTIMODAL_FALLBACK_PROVIDER || 'google',
    fallbackModel: process.env.CODEX_MULTIMODAL_FALLBACK_MODEL || 'gemini-2.0-flash',
    creditMultiplier: 5,
    outputFactor: 2,
    costPer1k: 0.025,
  },
};

function estimateTokens(text: string) {
  return Math.max(64, Math.ceil(text.trim().length / 4));
}

function decideServiceClass(input: RoutePlanInput): RouteServiceClass {
  const multimodal = input.imageInputs.length > 0 || Boolean(input.voiceTranscript?.trim());
  if (multimodal) return 'MULTIMODAL';
  if (input.internetMode !== 'OFF' || input.attachments.length > 0 || Boolean(input.sourceRef?.trim())) return 'RESEARCH';
  if (input.bestOfN >= 3) return 'PREMIUM';
  if (input.mode === 'ASK') return 'FAST';
  return 'BALANCED';
}

export function buildRoutePlan(input: RoutePlanInput): RoutePlan {
  const serviceClass = decideServiceClass(input);
  const modelDefaults = MODEL_DEFAULTS[serviceClass];
  const promptTokens = estimateTokens(input.prompt);
  const repoBonus = input.repositoryName ? Math.min(160, Math.ceil(input.repositoryName.length / 3)) : 0;
  const attachmentBonus = input.attachments.length * 120;
  const multimodalBonus = (input.imageInputs.length * 180) + (input.voiceTranscript ? estimateTokens(input.voiceTranscript) : 0);
  const estimatedInputTokens = promptTokens + repoBonus + attachmentBonus + multimodalBonus;
  const estimatedOutputTokens = Math.ceil(estimatedInputTokens * modelDefaults.outputFactor);
  const estimatedCost = Number((((estimatedInputTokens + estimatedOutputTokens) / 1000) * modelDefaults.costPer1k).toFixed(4));
  const reasonParts = [
    `mode=${input.mode.toLowerCase()}`,
    `service_class=${serviceClass.toLowerCase()}`,
    input.internetMode !== 'OFF' ? `internet=${input.internetMode.toLowerCase()}` : null,
    input.attachments.length > 0 ? `attachments=${input.attachments.length}` : null,
    input.bestOfN > 1 ? `best_of_n=${input.bestOfN}` : null,
  ].filter(Boolean);

  return {
    serviceClass,
    taskClass: input.mode === 'CODE' ? 'engineering_task' : 'assistant_response',
    policyKey: 'codex-router-default',
    policyVersion: POLICY_VERSION,
    selectedProvider: modelDefaults.provider,
    selectedModel: modelDefaults.model,
    fallbackProvider: modelDefaults.fallbackProvider,
    fallbackModel: modelDefaults.fallbackModel,
    estimatedInputTokens,
    estimatedOutputTokens,
    estimatedCost,
    creditsToBurn: modelDefaults.creditMultiplier,
    reason: reasonParts.join(', '),
  };
}

export function buildUsageOutcome(input: UsageOutcomeInput): UsageOutcome {
  const inputTokens = Math.max(input.estimatedInputTokens, estimateTokens(input.prompt));
  const artifactTokens = Math.ceil(input.artifactBytes / 4);
  const diffBonus = input.diffFileCount * (input.taskMode === 'CODE' ? 140 : 40);
  const fallbackPenalty = input.fallbackUsed ? 180 : 0;
  const outputTokens = Math.max(input.estimatedOutputTokens, artifactTokens + diffBonus + fallbackPenalty);
  const actualCost = Number((input.estimatedCost * (input.fallbackUsed ? 1.2 : 1) + (input.diffFileCount * 0.0025)).toFixed(4));

  return {
    inputTokens,
    outputTokens,
    actualCost,
    status: 'COMPLETED',
    metadata: {
      latencyMs: input.latencyMs,
      artifactBytes: input.artifactBytes,
      diffFileCount: input.diffFileCount,
      fallbackUsed: input.fallbackUsed,
    },
  };
}
