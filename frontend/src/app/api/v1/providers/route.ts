import { listProviderRuntimeSummaries } from '@/server/ai/runtime';
import { ok } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET() {
  return ok({
    providers: listProviderRuntimeSummaries(),
  });
}
