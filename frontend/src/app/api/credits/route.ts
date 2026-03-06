import { z } from 'zod';
import { codexDb } from '@/server/codex/db';
import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

const purchaseSchema = z.object({
  amount: z.number().positive(),
  currency: z.string().default('USD'),
});

export async function GET() {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const workspaceId = context.context.workspace.id;

  const [balance, ledger] = await Promise.all([
    codexDb.creditBalance.findUnique({ where: { workspaceId } }),
    codexDb.creditLedgerEntry.findMany({
      where: { workspaceId },
      orderBy: { createdAt: 'desc' },
      take: 200,
    }),
  ]);

  return ok({
    balance,
    ledger,
  });
}

export async function POST(request: Request) {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;
  const parsed = purchaseSchema.safeParse(await request.json());
  if (!parsed.success) return fail('Payload invalido', 400, parsed.error.flatten());
  const workspaceId = context.context.workspace.id;
  const payload = parsed.data;

  const billingIntent = await codexDb.billingIntent.create({
    data: {
      workspaceId,
      amount: payload.amount,
      currency: payload.currency,
      status: 'COMPLETED',
      providerRef: `manual-${Date.now()}`,
    },
  });

  await codexDb.creditBalance.upsert({
    where: { workspaceId },
    update: {
      balance: {
        increment: payload.amount,
      },
    },
    create: {
      workspaceId,
      balance: payload.amount,
      includedUsageLeft: 0,
    },
  });

  await codexDb.creditLedgerEntry.create({
    data: {
      workspaceId,
      type: 'PURCHASE',
      amount: payload.amount,
      description: `Manual purchase ${payload.currency}`,
    },
  });

  return ok(
    {
      billingIntentId: billingIntent.id,
      status: 'completed',
    },
    201
  );
}

