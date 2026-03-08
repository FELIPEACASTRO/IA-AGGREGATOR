import { AI_AGENT_CATALOG } from '@/lib/ai/agent-catalog';
import { ok } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function GET() {
  return ok({
    agents: AI_AGENT_CATALOG,
  });
}
