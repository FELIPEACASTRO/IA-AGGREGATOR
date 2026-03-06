import { resolveBillingData } from '@/server/codex/billing';
import { ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET() {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;

  const data = await resolveBillingData(context.context.workspace.id);
  return ok({ plans: data.plans });
}
