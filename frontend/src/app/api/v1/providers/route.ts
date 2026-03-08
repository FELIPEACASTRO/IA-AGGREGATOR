import { AI_PROVIDER_CATALOG } from '@/lib/ai/provider-registry';
import { fetchBackend } from '@/server/backend-proxy';
import { ok } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET() {
  const response = await fetchBackend('/api/v1/ai/providers');
  const payload = (await response.json()) as {
    success?: boolean;
    data?: {
      providers?: Array<{
        providerId: string;
        providerName: string;
        configured: boolean;
        enabled: boolean;
        reachable: boolean;
        circuitState: string;
        supportsStreaming: boolean;
        defaultModel?: string | null;
        supportedModels?: string[];
        requiredSecrets?: string[];
      }>;
    };
  };

  const backendProviders = payload.data?.providers ?? [];
  const providers = AI_PROVIDER_CATALOG.map((provider) => {
    const backendProvider = backendProviders.find((item) => item.providerId === provider.id);

    return {
      id: provider.id,
      slug: provider.slug,
      name: provider.name,
      category: provider.category,
      protocol: provider.protocol,
      summary: provider.summary,
      docsUrl: provider.docsUrl,
      configured: backendProvider?.configured ?? false,
      status: backendProvider?.configured ? backendProvider.circuitState?.toLowerCase() || 'configured' : 'missing',
      missingKeys: backendProvider?.configured ? [] : provider.env.filter((item) => item.required).map((item) => item.key),
      supportsLiveChat: provider.supportsLiveChat,
      resolvedBaseUrl: provider.defaultBaseUrl,
      models: provider.models.map((model) => ({
        id: model.id,
        label: model.label,
        availability: model.availability,
      })),
    };
  });

  return ok({ providers });
}
