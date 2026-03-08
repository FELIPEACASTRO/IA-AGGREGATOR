import { CHAT_MODEL_CATALOG } from '@/lib/ai/provider-registry';

export type ModelCapability = {
  id: string;
  label: string;
  providerId: string;
  provider: string;
  kind: string;
  description: string;
  availability: 'free' | 'trial' | 'paid' | 'mixed';
  maxContextTokens: number;
  tags: string[];
};

export const MODEL_CATALOG: ModelCapability[] = CHAT_MODEL_CATALOG.map((item) => ({
  id: item.id,
  label: item.label,
  providerId: item.providerId,
  provider: item.provider,
  kind: item.kind,
  description: item.description,
  availability: item.availability,
  maxContextTokens: item.maxContextTokens ?? 0,
  tags: item.tags,
}));

export const MODEL_CAPABILITIES_BY_ID = Object.fromEntries(
  MODEL_CATALOG.map((model) => [model.id, model])
) as Record<string, ModelCapability>;

export function getModelCapability(modelId: string | undefined): ModelCapability | undefined {
  if (!modelId) return undefined;
  return MODEL_CAPABILITIES_BY_ID[modelId];
}
