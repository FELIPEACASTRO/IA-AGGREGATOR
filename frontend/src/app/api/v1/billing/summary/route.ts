import { ok, requireCodexContext } from '@/server/codex/http';
import { resolveBillingData } from '@/server/codex/billing';

export const runtime = 'nodejs';

export async function GET() {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;

  const data = await resolveBillingData(context.context.workspace.id);
  return ok(data);
}
