import { fireEvent, render, screen } from '@testing-library/react';
import SettingsPage from './page';

const authState = {
  user: {
    fullName: 'Docker Admin',
    email: 'dockeradmin@ia-aggregator.local',
  },
  logout: jest.fn(),
};

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: jest.fn() }),
  usePathname: () => '/settings',
}));

jest.mock('@/stores/auth-store', () => ({
  useAuthStore: Object.assign((selector: (state: typeof authState) => unknown) => selector(authState), {
    getState: () => authState,
  }),
}));

describe('SettingsPage', () => {
  it('prefills full name from auth store', () => {
    render(<SettingsPage />);

    const input = screen.getByLabelText(/nome de exibição/i) as HTMLInputElement;
    expect(input.value).toBe('Docker Admin');
  });

  it('allows updating full name and locale', () => {
    render(<SettingsPage />);

    const nameInput = screen.getByLabelText(/nome de exibição/i) as HTMLInputElement;
    const localeSelect = screen.getByLabelText(/idioma padrão/i) as HTMLSelectElement;

    fireEvent.change(nameInput, { target: { value: 'Novo Nome' } });
    fireEvent.change(localeSelect, { target: { value: 'en-US' } });

    expect(nameInput.value).toBe('Novo Nome');
    expect(localeSelect.value).toBe('en-US');
  });
});
