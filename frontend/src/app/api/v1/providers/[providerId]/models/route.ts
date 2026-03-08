import { getAiProvider, listModelsForProvider } from '@/lib/ai/provider-registry';
import { fail, ok } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET(
  _request: Request,
  { params }: { params: Promise<{ providerId: string }> }
) {
  const { providerId } = await params;
  const provider = getAiProvider(providerId);
  if (!provider) {
    return fail('Provider nao encontrado', 404);
  }

  return ok({
    provider: {
      id: provider.id,
      slug: provider.slug,
      name: provider.name,
      category: provider.category,
      protocol: provider.protocol,
    },
    models: listModelsForProvider(providerId),
  });
}
