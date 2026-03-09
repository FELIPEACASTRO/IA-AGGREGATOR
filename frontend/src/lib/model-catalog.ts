export type ModelCapability = {
  id: string;
  label: string;
  provider: string;
  maxContextTokens: number;
};

// Static fallback catalog used when backend is unreachable.
// In production, models are fetched dynamically from GET /api/v1/ai/models
const FALLBACK_CATALOG: ModelCapability[] = [
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

let cachedModels: ModelCapability[] | null = null;

/**
 * Fetch models from backend API. Falls back to static catalog if unavailable.
 */
export async function fetchModelCatalog(): Promise<ModelCapability[]> {
  if (cachedModels) return cachedModels;

  try {
    const API_BASE = process.env.NEXT_PUBLIC_API_URL || '';
    const response = await fetch(`${API_BASE}/api/v1/ai/models`, { cache: 'no-store' });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const payload = await response.json() as { success: boolean; data?: Array<{ modelId: string; displayName: string; provider: string; contextWindow: number | null }> };
    if (payload.success && payload.data) {
      cachedModels = payload.data.map((m) => ({
        id: m.modelId,
        label: m.displayName || m.modelId,
        provider: m.provider,
        maxContextTokens: m.contextWindow ?? 128000,
      }));
      return cachedModels;
    }
  } catch {
    // Backend unavailable — use fallback
  }

  return FALLBACK_CATALOG;
}

/**
 * Synchronous model catalog for use in stores and components.
 * Returns cached models if available, otherwise the fallback catalog.
 */
export const MODEL_CATALOG: ModelCapability[] = FALLBACK_CATALOG;

export const MODEL_CAPABILITIES_BY_ID = Object.fromEntries(
  FALLBACK_CATALOG.map((model) => [model.id, model])
) as Record<string, ModelCapability>;

export function getModelCapability(modelId: string | undefined): ModelCapability | undefined {
  if (!modelId) return undefined;
  return MODEL_CAPABILITIES_BY_ID[modelId];
}
