export type ModelCapability = {
  id: string;
  label: string;
  provider: string;
  maxContextTokens: number;
};

export const MODEL_CATALOG: ModelCapability[] = [
  { id: 'gpt-4o-mini', label: 'GPT-4o Mini', provider: 'OpenAI', maxContextTokens: 128000 },
  { id: 'gpt-4.1-mini', label: 'GPT-4.1 Mini', provider: 'OpenAI', maxContextTokens: 128000 },
  { id: 'claude-3-5-haiku', label: 'Claude 3.5 Haiku', provider: 'Anthropic', maxContextTokens: 200000 },
  { id: 'gemini-1.5-flash', label: 'Gemini 1.5 Flash', provider: 'Google', maxContextTokens: 1048576 },
  { id: 'deepseek-chat', label: 'DeepSeek Chat', provider: 'DeepSeek', maxContextTokens: 64000 },
  { id: 'deepseek-reasoner', label: 'DeepSeek Reasoner', provider: 'DeepSeek', maxContextTokens: 64000 },
  { id: 'llama-3.1-8b-instant', label: 'Llama 3.1 8B', provider: 'Groq', maxContextTokens: 131072 },
  { id: 'llama-3.1-70b-versatile', label: 'Llama 3.1 70B', provider: 'Groq', maxContextTokens: 131072 },
  { id: 'mistral-small-latest', label: 'Mistral Small', provider: 'Mistral', maxContextTokens: 32000 },
  { id: 'mistral-large-latest', label: 'Mistral Large', provider: 'Mistral', maxContextTokens: 128000 },
  { id: 'command-r', label: 'Command R', provider: 'Cohere', maxContextTokens: 128000 },
  { id: 'command-r-plus', label: 'Command R+', provider: 'Cohere', maxContextTokens: 128000 },
  { id: 'sonar', label: 'Sonar', provider: 'Perplexity', maxContextTokens: 128000 },
  { id: 'sonar-pro', label: 'Sonar Pro', provider: 'Perplexity', maxContextTokens: 200000 }
];

export const MODEL_CAPABILITIES_BY_ID = Object.fromEntries(
  MODEL_CATALOG.map((model) => [model.id, model])
) as Record<string, ModelCapability>;

export function getModelCapability(modelId: string | undefined): ModelCapability | undefined {
  if (!modelId) return undefined;
  return MODEL_CAPABILITIES_BY_ID[modelId];
}
