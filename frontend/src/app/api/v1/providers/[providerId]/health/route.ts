import { fail, ok } from '@/server/codex/http';
import { summarizeProvider } from '@/server/ai/runtime';

export const runtime = 'nodejs';

export async function GET(
  _request: Request,
  { params }: { params: Promise<{ providerId: string }> }
) {
  const { providerId } = await params;
  const provider = summarizeProvider(providerId);
  if (!provider) {
    return fail('Provider nao encontrado', 404);
  }

  return ok({
    providerId: provider.id,
    providerName: provider.name,
    status: provider.status,
    configured: provider.configured,
    missingKeys: provider.missingKeys,
    liveChatReady: provider.configured && provider.supportsLiveChat,
    resolvedBaseUrl: provider.resolvedBaseUrl,
  });
}
