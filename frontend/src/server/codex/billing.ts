import { subDays } from 'date-fns';
import { codexDb } from '@/server/codex/db';

type BillingPlan = {
  id: string;
  name: string;
  price: string;
  desc: string;
  tokens: number;
  models: number;
  current: boolean;
  features: string[];
  gradient?: string;
};

type WeeklyUsagePoint = {
  day: string;
  tokens: number;
};

const PLAN_DEFS: Omit<BillingPlan, 'current'>[] = [
  {
    id: 'starter',
    name: 'Starter',
    price: 'Grátis',
    desc: 'Ideal para explorar e validar',
    tokens: 50000,
    models: 5,
    features: ['5 modelos disponíveis', '50k tokens/mês', 'Histórico 30 dias', 'Suporte por e-mail']
  },
  {
    id: 'pro',
    name: 'Pro',
    price: 'R$ 49/mês',
    desc: 'Para profissionais e equipes',
    tokens: 500000,
    models: 13,
    features: ['13+ modelos disponíveis', '500k tokens/mês', 'Histórico ilimitado', 'Canvas Mode', 'Suporte prioritário', 'API access'],
    gradient: 'var(--brand-gradient)'
  },
  {
    id: 'enterprise',
    name: 'Enterprise',
    price: 'Personalizado',
    desc: 'Escala e segurança corporativa',
    tokens: -1,
    models: 13,
    features: ['Tokens ilimitados', 'SSO / SAML', 'SLA dedicado', 'Integrações customizadas', 'Contato comercial']
  }
];

const DAY_LABELS = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];

function normalizeTokenAmount(metric: string, unit: string, amount: number) {
  if (unit.toLowerCase().includes('token') || metric.toLowerCase().includes('token')) return amount;
  return 0;
}

export async function resolveBillingData(workspaceId: string) {
  const now = new Date();
  const monthKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  const since = subDays(now, 45);

  const [usageEntries, creditBalance, currentPlanConfig] = await Promise.all([
    codexDb.usageEntry.findMany({
      where: {
        workspaceId,
        createdAt: { gte: since }
      },
      orderBy: { createdAt: 'asc' }
    }),
    codexDb.creditBalance.findUnique({ where: { workspaceId } }),
    codexDb.managedConfig.findUnique({
      where: {
        workspaceId_configKey: {
          workspaceId,
          configKey: 'billing.currentPlanId',
        },
      },
    }),
  ]);

  const monthlyTokenEntries = usageEntries.filter((entry) => entry.period === monthKey);
  const tokensFromUsage = monthlyTokenEntries.reduce((sum, entry) => {
    return sum + normalizeTokenAmount(entry.metric, entry.unit, entry.amount);
  }, 0);

  const planValue = currentPlanConfig?.configValue;
  const selectedPlanId = (typeof planValue === 'string' ? planValue : null) as BillingPlan['id'] | null;
  const tokensUsed = Math.max(0, Math.round(tokensFromUsage));

  const plans: BillingPlan[] = PLAN_DEFS.map((plan) => ({
    ...plan,
    current: plan.id === selectedPlanId
  }));

  const selectedPlan = plans.find((plan) => plan.id === selectedPlanId) ?? null;
  const monthlyLimit = selectedPlan?.tokens && selectedPlan.tokens > 0 ? selectedPlan.tokens : 0;

  const weeklyUsage: WeeklyUsagePoint[] = Array.from({ length: 7 }).map((_, index) => {
    const date = subDays(now, 6 - index);
    const dayStart = new Date(date);
    dayStart.setHours(0, 0, 0, 0);
    const dayEnd = new Date(date);
    dayEnd.setHours(23, 59, 59, 999);

    const dayUsage = usageEntries
      .filter((entry) => entry.createdAt >= dayStart && entry.createdAt <= dayEnd)
      .reduce((sum, entry) => sum + normalizeTokenAmount(entry.metric, entry.unit, entry.amount), 0);

    return {
      day: DAY_LABELS[date.getDay()],
      tokens: Math.round(dayUsage)
    };
  });

  return {
    plans,
    monthlyUsage: weeklyUsage,
    current: {
      currentPlanId: selectedPlanId,
      currentPlanName: selectedPlan?.name ?? 'Indisponivel',
      tokensUsed,
      monthlyLimit,
      pct: monthlyLimit > 0 ? Math.min(100, Math.round((tokensUsed / monthlyLimit) * 100)) : 0,
      balance: Math.round(creditBalance?.balance ?? 0),
      includedUsageLeft: Math.round(creditBalance?.includedUsageLeft ?? 0),
      usageAvailable: tokensFromUsage > 0 || usageEntries.some((entry) => normalizeTokenAmount(entry.metric, entry.unit, entry.amount) > 0),
    },
  };
}
