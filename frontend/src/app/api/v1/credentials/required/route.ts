import { AI_PROVIDER_KEY_CHECKLIST } from '@/lib/ai/provider-registry';
import { ok } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET() {
  return ok({
    providers: AI_PROVIDER_KEY_CHECKLIST,
  });
}
