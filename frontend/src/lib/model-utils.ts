import type { ModelCapability } from './model-catalog';

export type Tier = 'fast' | 'balanced' | 'powerful';

export const PROVIDER_COLORS: Record<string, string> = {
  OpenAI: '#4ed9a7',
  Anthropic: '#f25d9c',
  Google: '#77b8ff',
  DeepSeek: '#8b6cff',
  Groq: '#ff8f5e',
  Mistral: '#ff6b6b',
  Cohere: '#ffbf66',
  Perplexity: '#6ee7b7',
};

export function deriveTier(model: ModelCapability): Tier {
  const ctx = model.maxContextTokens;
  if (ctx >= 200000) return 'powerful';
  if (ctx >= 100000) return 'balanced';
  return 'fast';
}

export function getModelMeta(
  modelId: string,
  availableModels: ModelCapability[],
): { tier: Tier; color: string } {
  const model = availableModels.find((m) => m.id === modelId);
  if (!model) return { tier: 'balanced', color: '#6073ff' };
  return {
    tier: deriveTier(model),
    color: PROVIDER_COLORS[model.provider] ?? '#6073ff',
  };
}
