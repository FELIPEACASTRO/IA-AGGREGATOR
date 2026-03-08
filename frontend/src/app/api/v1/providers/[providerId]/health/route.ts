import { fail, ok } from '@/server/codex/http';
import { fetchBackend } from '@/server/backend-proxy';

export const runtime = 'nodejs';

export async function GET(
  _request: Request,
  { params }: { params: Promise<{ providerId: string }> }
) {
  const { providerId } = await params;
  const response = await fetchBackend(`/api/v1/ai/providers/${providerId}/health`);
  if (!response.ok) {
    return fail('Provider nao encontrado', 404);
  }

  const payload = (await response.json()) as {
    success?: boolean;
    data?: {
      providerId: string;
      providerName: string;
      circuitState?: string;
      configured: boolean;
      requiredSecrets?: string[];
      supportsStreaming?: boolean;
      defaultModel?: string | null;
    };
  };
  const provider = payload.data;
  if (!provider) {
    return fail('Provider nao encontrado', 404);
  }

  return ok({
    providerId: provider.providerId,
    providerName: provider.providerName,
    status: provider.circuitState?.toLowerCase() ?? 'unknown',
    configured: provider.configured,
    missingKeys: provider.requiredSecrets ?? [],
    liveChatReady: provider.configured && Boolean(provider.supportsStreaming),
    defaultModel: provider.defaultModel ?? null,
  });
}
