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
  it('renders billing summary cards and action', () => {
    render(<BillingPage />);

    expect(screen.getByText('Plano Atual')).toBeInTheDocument();
    expect(screen.getByText('Uso Mensal')).toBeInTheDocument();
    expect(screen.getByText('Crédito Restante')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /solicitar upgrade/i })).toBeInTheDocument();
  });
});
