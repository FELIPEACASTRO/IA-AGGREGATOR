import { MODEL_CATALOG } from '@/lib/model-catalog';
import { ok } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET() {
  return ok({
    models: MODEL_CATALOG,
  });
}
