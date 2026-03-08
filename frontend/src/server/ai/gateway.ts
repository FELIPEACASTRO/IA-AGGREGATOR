import { getAiAgent } from '@/lib/ai/agent-catalog';
import { DEFAULT_CHAT_MODEL_IDS, getAiModel } from '@/lib/ai/provider-registry';
import { fetchBackend, getBackendBaseUrl } from '@/server/backend-proxy';

type ChatGatewayInput = {
  prompt: string;
  preferredModel?: string;
  agentId?: string;
  provider?: string;
  systemPrompt?: string;
  temperature?: number;
  maxTokens?: number;
  fallbackProviders?: string[];
};

type ApiEnvelope<T> = {
  success: boolean;
  data?: T;
  message?: string;
  errorCode?: string;
};

type BackendUsage = {
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
};

type BackendCost = {
  providerId: string;
  model: string;
  currency: string;
  amount?: number | null;
  supported: boolean;
};

type BackendChatResponse = {
  content: string;
  modelUsed: string;
  providerUsed: string;
  fallbackUsed: boolean;
  attempts: number;
  requestId?: string | null;
  usage?: BackendUsage | null;
  estimatedCost?: BackendCost | null;
  latencyMs?: number | null;
  finishReason?: string | null;
};

export type ChatGatewayResult = BackendChatResponse & {
  agentUsed: string;
  agentVersion: string;
  executionMode: 'live';
  warnings: string[];
};

function chooseAgent(agentId?: string, preferredModel?: string) {
  const direct = getAiAgent(agentId);
  if (direct) return direct;

  const selectedModel = getAiModel(preferredModel);
  if (selectedModel?.kind === 'search') {
    const researchAgent = getAiAgent('chat-research');
    if (researchAgent) return researchAgent;
  }
  if (selectedModel?.kind === 'code') {
    const codeAgent = getAiAgent('codex-code');
    if (codeAgent) return codeAgent;
  }

  const fallback = getAiAgent('chat-general');
  if (!fallback) {
    throw new Error('Catalogo de agentes invalido: chat-general ausente');
  }
  return fallback;
}

function resolvePreferredModel(input: ChatGatewayInput, agentPreferredModels: string[]) {
  if (input.preferredModel?.trim()) {
    return input.preferredModel.trim();
  }

  return agentPreferredModels.find((modelId) => DEFAULT_CHAT_MODEL_IDS.includes(modelId)) ?? agentPreferredModels[0];
}

async function readEnvelope<T>(response: Response): Promise<T> {
  const payload = (await response.json().catch(() => null)) as ApiEnvelope<T> | null;
  if (!response.ok || !payload?.success || !payload.data) {
    throw new Error(payload?.message || `Falha ao chamar o backend canonico de IA (${response.status})`);
  }
  return payload.data;
}

export async function invokeChatGateway(input: ChatGatewayInput): Promise<ChatGatewayResult> {
  const agent = chooseAgent(input.agentId, input.preferredModel);
  const preferredModel = resolvePreferredModel(input, agent.preferredModelIds);

  const response = await fetchBackend('/api/v1/ai/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      prompt: input.prompt,
      preferredModel,
      provider: input.provider,
      systemPrompt: input.systemPrompt,
      temperature: input.temperature,
      maxTokens: input.maxTokens,
      fallbackProviders: input.fallbackProviders,
    }),
  });

  const result = await readEnvelope<BackendChatResponse>(response);
  return {
    ...result,
    requestId: result.requestId ?? null,
    usage: result.usage ?? null,
    estimatedCost: result.estimatedCost ?? null,
    latencyMs: result.latencyMs ?? null,
    finishReason: result.finishReason ?? 'completed',
    agentUsed: agent.label,
    agentVersion: agent.version,
    executionMode: 'live',
    warnings: [`Backend canonico: ${getBackendBaseUrl()}`],
  };
}
