import {
  AI_PROVIDER_CATALOG,
  AiProviderDescriptor,
  getAiProvider,
  listModelsForProvider,
} from '@/lib/ai/provider-registry';

export type ProviderRuntimeStatus = 'configured' | 'partial' | 'missing';

export interface ProviderRuntimeSummary {
  id: string;
  slug: string;
  name: string;
  category: string;
  protocol: string;
  summary: string;
  docsUrl: string;
  defaultBaseUrl?: string;
  resolvedBaseUrl?: string;
  supportsLiveChat: boolean;
  configured: boolean;
  status: ProviderRuntimeStatus;
  missingKeys: string[];
  envKeys: string[];
  models: ReturnType<typeof listModelsForProvider>;
  supportedAgentIds: string[];
}

function getEnv(key: string) {
  const value = process.env[key];
  return typeof value === 'string' && value.trim().length > 0 ? value.trim() : undefined;
}

export function getProviderRuntimeSecret(provider: AiProviderDescriptor, envKey: string) {
  return getEnv(envKey);
}

export function getResolvedBaseUrl(provider: AiProviderDescriptor) {
  switch (provider.id) {
    case 'deepseek':
      return getEnv('DEEPSEEK_BASE_URL') || provider.defaultBaseUrl;
    case 'groq':
      return getEnv('GROQ_BASE_URL') || provider.defaultBaseUrl;
    case 'perplexity':
      return getEnv('PERPLEXITY_BASE_URL') || provider.defaultBaseUrl;
    case 'azure-openai':
      return getEnv('AZURE_OPENAI_ENDPOINT') || provider.defaultBaseUrl;
    default:
      return provider.defaultBaseUrl;
  }
}

export function summarizeProvider(providerId: string): ProviderRuntimeSummary | undefined {
  const provider = getAiProvider(providerId);
  if (!provider) return undefined;

  const missingKeys = provider.env
    .filter((item) => item.required && !getProviderRuntimeSecret(provider, item.key))
    .map((item) => item.key);

  const configuredCount = provider.env.filter((item) => getProviderRuntimeSecret(provider, item.key)).length;
  const status: ProviderRuntimeStatus =
    missingKeys.length === 0
      ? 'configured'
      : configuredCount > 0
        ? 'partial'
        : 'missing';

  return {
    id: provider.id,
    slug: provider.slug,
    name: provider.name,
    category: provider.category,
    protocol: provider.protocol,
    summary: provider.summary,
    docsUrl: provider.docsUrl,
    defaultBaseUrl: provider.defaultBaseUrl,
    resolvedBaseUrl: getResolvedBaseUrl(provider),
    supportsLiveChat: provider.supportsLiveChat,
    configured: missingKeys.length === 0,
    status,
    missingKeys,
    envKeys: provider.env.map((item) => item.key),
    models: listModelsForProvider(provider.id),
    supportedAgentIds: provider.supportedAgentIds,
  };
}

export function listProviderRuntimeSummaries() {
  return AI_PROVIDER_CATALOG.map((provider) => summarizeProvider(provider.id)).filter(Boolean) as ProviderRuntimeSummary[];
}
