import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import LoginPage from './page';

const pushMock = jest.fn();
const loginMock = jest.fn();

const authState = {
  user: null,
  isAuthenticated: false,
  isLoading: false,
  login: loginMock,
  register: jest.fn(),
  logout: jest.fn(),
  fetchUser: jest.fn(),
};

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: pushMock }),
}));

jest.mock('@/stores/auth-store', () => ({
  useAuthStore: Object.assign((selector: (state: typeof authState) => unknown) => selector(authState), {
    getState: () => authState,
  }),
}));

describe('LoginPage', () => {
  beforeEach(() => {
    pushMock.mockReset();
    loginMock.mockReset();
  });

  it('submits credentials and redirects on success', async () => {
    loginMock.mockResolvedValue(undefined);
    render(<LoginPage />);

    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'user@test.com' } });
    fireEvent.change(screen.getByLabelText(/^senha$/i), { target: { value: 'Password123!' } });
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    await waitFor(() => {
      expect(loginMock).toHaveBeenCalledWith('user@test.com', 'Password123!');
      expect(pushMock).toHaveBeenCalledWith('/chat');
    });
  });

  it('shows backend error message when login fails', async () => {
    loginMock.mockRejectedValue({ response: { data: { message: 'Credenciais inválidas' } } });
    render(<LoginPage />);

    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'user@test.com' } });
    fireEvent.change(screen.getByLabelText(/^senha$/i), { target: { value: 'wrong-pass' } });
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(await screen.findByText('Credenciais inválidas')).toBeInTheDocument();
    expect(pushMock).not.toHaveBeenCalled();
  });

  it('shows loading state while awaiting login', async () => {
    let resolveLogin: () => void = () => {};
    loginMock.mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          resolveLogin = resolve;
        })
    );

    render(<LoginPage />);

    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'user@test.com' } });
    fireEvent.change(screen.getByLabelText(/^senha$/i), { target: { value: 'Password123!' } });
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }));

    expect(screen.getByRole('button', { name: 'Entrando...' })).toBeDisabled();

    resolveLogin();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Entrar' })).toBeEnabled();
    });
  });
});
