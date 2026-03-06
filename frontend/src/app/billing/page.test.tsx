import { render, screen } from '@testing-library/react';
import BillingPage from './page';

const authState = {
  user: {
    fullName: 'Docker Admin',
    email: 'dockeradmin@ia-aggregator.local',
  },
  logout: jest.fn(),
};

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: jest.fn() }),
  usePathname: () => '/billing',
}));

jest.mock('@/stores/auth-store', () => ({
  useAuthStore: Object.assign((selector: (state: typeof authState) => unknown) => selector(authState), {
    getState: () => authState,
  }),
}));

describe('BillingPage', () => {
  beforeEach(() => {
    global.fetch = jest.fn(async () =>
      ({
        ok: true,
        json: async () => ({
          success: true,
          data: {
            plans: [
              {
                id: 'starter',
                name: 'Starter',
                price: 'Grátis',
                desc: 'Plano inicial',
                tokens: 50000,
                models: 5,
                current: true,
                features: ['5 modelos disponíveis'],
              },
              {
                id: 'pro',
                name: 'Pro',
                price: 'R$ 49/mês',
                desc: 'Plano profissional',
                tokens: 500000,
                models: 13,
                current: false,
                features: ['13+ modelos disponíveis'],
              },
            ],
            monthlyUsage: [
              { day: 'Seg', tokens: 1200 },
              { day: 'Ter', tokens: 1600 },
            ],
            current: {
              tokensUsed: 12400,
              monthlyLimit: 50000,
              pct: 25,
              estimatedFromRuns: false,
            },
          },
        }),
      }) as Response
    ) as jest.Mock;
  });

  it('renders billing summary cards and action', async () => {
    render(<BillingPage />);

    expect(screen.getByText('Plano e uso')).toBeInTheDocument();
    expect(await screen.findByText('Tokens usados')).toBeInTheDocument();
    expect(screen.getByText('Reset em')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /fazer upgrade|falar com vendas/i }).length).toBeGreaterThan(0);
  });
});
