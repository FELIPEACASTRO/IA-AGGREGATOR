import { getAiAgent } from '@/lib/ai/agent-catalog';
import {
  DEFAULT_CHAT_MODEL_IDS,
  getAiModel,
  getAiProvider,
} from '@/lib/ai/provider-registry';
import { getResolvedBaseUrl, getProviderRuntimeSecret, summarizeProvider } from '@/server/ai/runtime';

type ChatGatewayInput = {
  prompt: string;
  preferredModel?: string;
  agentId?: string;
};

export type ChatGatewayResult = {
  content: string;
  modelUsed: string;
  providerUsed: string;
  fallbackUsed: boolean;
  attempts: number;
  agentUsed: string;
  agentVersion: string;
  executionMode: 'live' | 'mock';
  warnings: string[];
};

type ProviderInvocationContext = {
  providerId: string;
  modelId: string;
  prompt: string;
};

function unique<T>(items: T[]) {
  return Array.from(new Set(items));
}

function withTimeout(signal?: AbortSignal, timeoutMs = 30000) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);

  if (signal) {
    signal.addEventListener('abort', () => controller.abort(), { once: true });
  }

  return {
    signal: controller.signal,
    dispose: () => clearTimeout(timeout),
  };
}

function parseOpenAiResponsesText(payload: Record<string, unknown>) {
  if (typeof payload.output_text === 'string' && payload.output_text.trim()) {
    return payload.output_text.trim();
  }

  const output = Array.isArray(payload.output) ? payload.output : [];
  for (const item of output) {
    const current = item as { content?: Array<{ text?: string }> };
    if (!Array.isArray(current.content)) continue;
    const text = current.content
      .map((entry) => (typeof entry?.text === 'string' ? entry.text : ''))
      .join('')
      .trim();
    if (text) return text;
  }

  return '';
}

function parseOpenAiChatText(payload: Record<string, unknown>) {
  const choices = Array.isArray(payload.choices) ? payload.choices : [];
  const firstChoice = choices[0] as
    | {
        message?: {
          content?: string | Array<{ text?: string; type?: string }>;
        };
      }
    | undefined;

  const messageContent = firstChoice?.message?.content;
  if (typeof messageContent === 'string') return messageContent.trim();
  if (Array.isArray(messageContent)) {
    return messageContent
      .map((item) => (typeof item?.text === 'string' ? item.text : ''))
      .join('')
      .trim();
  }

  return '';
}

function parseAnthropicText(payload: Record<string, unknown>) {
  const content = Array.isArray(payload.content) ? payload.content : [];
  return content
    .map((item) => {
      const current = item as { type?: string; text?: string };
      return current.type === 'text' && typeof current.text === 'string' ? current.text : '';
    })
    .join('')
    .trim();
}

function parseGeminiText(payload: Record<string, unknown>) {
  const candidates = Array.isArray(payload.candidates) ? payload.candidates : [];
  const first = candidates[0] as { content?: { parts?: Array<{ text?: string }> } } | undefined;
  const parts = Array.isArray(first?.content?.parts) ? first?.content?.parts : [];
  return parts
    .map((part) => (typeof part?.text === 'string' ? part.text : ''))
    .join('')
    .trim();
}

function parseCohereText(payload: Record<string, unknown>) {
  const message = payload.message as
    | {
        content?: Array<{ type?: string; text?: string }>;
      }
    | undefined;
  const content = Array.isArray(message?.content) ? message.content : [];
  return content
    .map((entry) => (entry?.type === 'text' && typeof entry.text === 'string' ? entry.text : ''))
    .join('')
    .trim();
}

async function fetchJson(url: string, init: RequestInit, signal?: AbortSignal) {
  const scoped = withTimeout(signal);
  try {
    const response = await fetch(url, {
      ...init,
      signal: scoped.signal,
      cache: 'no-store',
    });

    const payload = (await response.json().catch(() => ({}))) as Record<string, unknown>;
    if (!response.ok) {
      const message =
        (typeof payload.error === 'object' &&
          payload.error &&
          'message' in payload.error &&
          typeof payload.error.message === 'string' &&
          payload.error.message) ||
        (typeof payload.message === 'string' && payload.message) ||
        `Provider request failed with status ${response.status}`;
      throw new Error(message);
    }

    return payload;
  } finally {
    scoped.dispose();
  }
}

async function invokeOpenAiResponses(input: ProviderInvocationContext) {
  const provider = getAiProvider(input.providerId);
  if (!provider) throw new Error('Provider nao encontrado');
  const apiKey = getProviderRuntimeSecret(provider, 'OPENAI_API_KEY');
  if (!apiKey) throw new Error('OPENAI_API_KEY ausente');

  const payload = await fetchJson(
    `${getResolvedBaseUrl(provider)}/responses`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify({
        model: input.modelId,
        input: input.prompt,
      }),
    }
  );

  const content = parseOpenAiResponsesText(payload);
  if (!content) throw new Error('OpenAI retornou resposta sem texto legivel');
  return content;
}

async function invokeOpenAiCompatible(input: ProviderInvocationContext) {
  const provider = getAiProvider(input.providerId);
  if (!provider) throw new Error('Provider nao encontrado');

  const apiKeyEnvKey =
    provider.env.find((item) => item.key.endsWith('_API_KEY'))?.key ||
    provider.env.find((item) => item.key.endsWith('_TOKEN'))?.key;

  if (!apiKeyEnvKey) throw new Error(`Provider ${provider.name} sem configuracao de API key`);
  const apiKey = getProviderRuntimeSecret(provider, apiKeyEnvKey);
  if (!apiKey) throw new Error(`${apiKeyEnvKey} ausente`);

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${apiKey}`,
  };

  if (provider.id === 'openrouter') {
    const referer = process.env.OPENROUTER_HTTP_REFERER;
    const title = process.env.OPENROUTER_APP_TITLE;
    if (referer) headers['HTTP-Referer'] = referer;
    if (title) headers['X-Title'] = title;
  }

  const payload = await fetchJson(
    `${getResolvedBaseUrl(provider)}/chat/completions`,
    {
      method: 'POST',
      headers,
      body: JSON.stringify({
        model: input.modelId,
        temperature: 0.2,
        messages: [{ role: 'user', content: input.prompt }],
      }),
    }
  );

  const content = parseOpenAiChatText(payload);
  if (!content) throw new Error(`${provider.name} retornou resposta sem texto legivel`);
  return content;
}

async function invokeAnthropic(input: ProviderInvocationContext) {
  const provider = getAiProvider(input.providerId);
  if (!provider) throw new Error('Provider nao encontrado');
  const apiKey = getProviderRuntimeSecret(provider, 'ANTHROPIC_API_KEY');
  if (!apiKey) throw new Error('ANTHROPIC_API_KEY ausente');

  const payload = await fetchJson(
    `${getResolvedBaseUrl(provider)}/messages`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': apiKey,
        'anthropic-version': process.env.ANTHROPIC_VERSION || '2023-06-01',
      },
      body: JSON.stringify({
        model: input.modelId,
        max_tokens: 1024,
        messages: [{ role: 'user', content: input.prompt }],
      }),
    }
  );

  const content = parseAnthropicText(payload);
  if (!content) throw new Error('Anthropic retornou resposta sem texto legivel');
  return content;
}

async function invokeGemini(input: ProviderInvocationContext) {
  const provider = getAiProvider(input.providerId);
  if (!provider) throw new Error('Provider nao encontrado');
  const apiKey = getProviderRuntimeSecret(provider, 'GEMINI_API_KEY');
  if (!apiKey) throw new Error('GEMINI_API_KEY ausente');

  const payload = await fetchJson(
    `${getResolvedBaseUrl(provider)}/models/${input.modelId}:generateContent?key=${encodeURIComponent(apiKey)}`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        contents: [
          {
            role: 'user',
            parts: [{ text: input.prompt }],
          },
        ],
      }),
    }
  );

  const content = parseGeminiText(payload);
  if (!content) throw new Error('Gemini retornou resposta sem texto legivel');
  return content;
}

async function invokeCohere(input: ProviderInvocationContext) {
  const provider = getAiProvider(input.providerId);
  if (!provider) throw new Error('Provider nao encontrado');
  const apiKey = getProviderRuntimeSecret(provider, 'COHERE_API_KEY');
  if (!apiKey) throw new Error('COHERE_API_KEY ausente');

  const payload = await fetchJson(
    `${getResolvedBaseUrl(provider)}/chat`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify({
        model: input.modelId,
        messages: [{ role: 'user', content: input.prompt }],
      }),
    }
  );

  const content = parseCohereText(payload);
  if (!content) throw new Error('Cohere retornou resposta sem texto legivel');
  return content;
}

async function invokeAzureOpenAi(input: ProviderInvocationContext) {
  const provider = getAiProvider(input.providerId);
  if (!provider) throw new Error('Provider nao encontrado');

  const apiKey = getProviderRuntimeSecret(provider, 'AZURE_OPENAI_API_KEY');
  const endpoint = getProviderRuntimeSecret(provider, 'AZURE_OPENAI_ENDPOINT');
  const apiVersion = getProviderRuntimeSecret(provider, 'AZURE_OPENAI_API_VERSION');
  const deployment = input.modelId === 'azure-deployment-default'
    ? getProviderRuntimeSecret(provider, 'AZURE_OPENAI_DEPLOYMENT')
    : input.modelId;

  if (!apiKey || !endpoint || !apiVersion || !deployment) {
    throw new Error('Configuracao incompleta do Azure OpenAI');
  }

  const payload = await fetchJson(
    `${endpoint}/openai/deployments/${deployment}/chat/completions?api-version=${encodeURIComponent(apiVersion)}`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'api-key': apiKey,
      },
      body: JSON.stringify({
        messages: [{ role: 'user', content: input.prompt }],
        temperature: 0.2,
      }),
    }
  );

  const content = parseOpenAiChatText(payload);
  if (!content) throw new Error('Azure OpenAI retornou resposta sem texto legivel');
  return content;
}

async function invokeProviderText(input: ProviderInvocationContext) {
  const provider = getAiProvider(input.providerId);
  if (!provider) throw new Error('Provider nao encontrado');

  switch (provider.protocol) {
    case 'openai-responses':
      return invokeOpenAiResponses(input);
    case 'openai-compatible':
    case 'mistral-chat':
      return invokeOpenAiCompatible(input);
    case 'anthropic-messages':
      return invokeAnthropic(input);
    case 'google-gemini':
      return invokeGemini(input);
    case 'cohere-chat-v2':
      return invokeCohere(input);
    case 'azure-openai':
      return invokeAzureOpenAi(input);
    default:
      throw new Error(`Provider ${provider.name} ainda nao possui adapter de chat habilitado`);
  }
}

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
    throw new Error('Agent catalog misconfigured: chat-general is missing');
  }
  return fallback;
}

function buildModelAttemptOrder(preferredModel: string | undefined, agentPreferredModels: string[]) {
  return unique([preferredModel, ...agentPreferredModels, ...DEFAULT_CHAT_MODEL_IDS].filter(Boolean) as string[]);
}

function buildMockContent(prompt: string, providerName: string, modelLabel: string, agentLabel: string) {
  const normalized = prompt.toLowerCase();
  if (normalized.includes('fallback de modelos')) {
    return 'Fallback de modelos e a troca automatica para um modelo alternativo quando o preferido falha, excede limite ou fica indisponivel.';
  }

  if (normalized.includes('resuma') || normalized.includes('resumo')) {
    return `Resumo simulado por ${agentLabel} usando ${providerName}/${modelLabel}: ${prompt.slice(0, 140)}.`;
  }

  return `Resposta simulada por ${agentLabel} usando ${providerName}/${modelLabel}. Nenhuma API key ativa foi encontrada para execucao live neste ambiente.`;
}

export async function invokeChatGateway(input: ChatGatewayInput): Promise<ChatGatewayResult> {
  const agent = chooseAgent(input.agentId, input.preferredModel);
  const attemptOrder = buildModelAttemptOrder(input.preferredModel, agent.preferredModelIds);
  const warnings: string[] = [];
  const errors: string[] = [];

  for (let index = 0; index < attemptOrder.length; index += 1) {
    const modelId = attemptOrder[index];
    const model = getAiModel(modelId);
    if (!model) {
      warnings.push(`Modelo ${modelId} nao encontrado no catalogo.`);
      continue;
    }

    const provider = getAiProvider(model.providerId);
    if (!provider) {
      warnings.push(`Provider ${model.providerId} nao encontrado para o modelo ${modelId}.`);
      continue;
    }

    const runtime = summarizeProvider(provider.id);
    if (!runtime?.configured || !provider.supportsLiveChat) {
      warnings.push(`Provider ${provider.name} indisponivel; fallback aplicado.`);
      continue;
    }

    try {
      const content = await invokeProviderText({
        providerId: provider.id,
        modelId: model.id,
        prompt: input.prompt,
      });

      return {
        content,
        modelUsed: model.id,
        providerUsed: provider.name,
        fallbackUsed: index > 0,
        attempts: index + 1,
        agentUsed: agent.label,
        agentVersion: agent.version,
        executionMode: 'live',
        warnings,
      };
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Erro desconhecido';
      errors.push(`${provider.name}: ${message}`);
      warnings.push(`Tentativa ${index + 1} falhou em ${provider.name}; aplicando fallback.`);
    }
  }

  const fallbackModel = getAiModel(attemptOrder[0]) || getAiModel(DEFAULT_CHAT_MODEL_IDS[0]);
  const fallbackProvider = fallbackModel ? getAiProvider(fallbackModel.providerId) : undefined;

  return {
    content: buildMockContent(
      input.prompt,
      fallbackProvider?.name || 'Lume Gateway',
      fallbackModel?.label || 'fallback',
      agent.label
    ),
    modelUsed: fallbackModel?.id || 'mock-fallback',
    providerUsed: fallbackProvider?.name || 'Mock',
    fallbackUsed: errors.length > 0,
    attempts: Math.max(1, errors.length),
    agentUsed: agent.label,
    agentVersion: agent.version,
    executionMode: 'mock',
    warnings: [...warnings, ...errors],
  };
}
