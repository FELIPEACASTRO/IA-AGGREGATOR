import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import RegisterPage from './page';

const pushMock = jest.fn();
const registerMock = jest.fn();

const authState = {
  user: null,
  isAuthenticated: false,
  isLoading: false,
  login: jest.fn(),
  register: registerMock,
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

describe('RegisterPage', () => {
  beforeEach(() => {
    pushMock.mockReset();
    registerMock.mockReset();
  });

  it('validates password confirmation before submit', async () => {
    render(<RegisterPage />);

    fireEvent.change(screen.getByLabelText(/nome completo/i), { target: { value: 'User Test' } });
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'user@test.com' } });
    fireEvent.change(screen.getByLabelText(/^senha$/i), { target: { value: 'Password123!' } });
    fireEvent.change(screen.getByLabelText(/confirmar senha/i), { target: { value: 'Password999!' } });
    fireEvent.click(screen.getByRole('button', { name: 'Criar Conta' }));

    expect(await screen.findByText('As senhas não coincidem')).toBeInTheDocument();
    expect(registerMock).not.toHaveBeenCalled();
  });

  it('validates minimum password length', async () => {
    render(<RegisterPage />);

    fireEvent.change(screen.getByLabelText(/nome completo/i), { target: { value: 'User Test' } });
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'user@test.com' } });
    fireEvent.change(screen.getByLabelText(/^senha$/i), { target: { value: '123' } });
    fireEvent.change(screen.getByLabelText(/confirmar senha/i), { target: { value: '123' } });
    fireEvent.click(screen.getByRole('button', { name: 'Criar Conta' }));

    expect(await screen.findByText('A senha deve ter pelo menos 8 caracteres')).toBeInTheDocument();
    expect(registerMock).not.toHaveBeenCalled();
  });

  it('submits registration and redirects on success', async () => {
    registerMock.mockResolvedValue(undefined);
    render(<RegisterPage />);

    fireEvent.change(screen.getByLabelText(/nome completo/i), { target: { value: 'User Test' } });
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'user@test.com' } });
    fireEvent.change(screen.getByLabelText(/^senha$/i), { target: { value: 'Password123!' } });
    fireEvent.change(screen.getByLabelText(/confirmar senha/i), { target: { value: 'Password123!' } });
    fireEvent.click(screen.getByRole('button', { name: 'Criar Conta' }));

    await waitFor(() => {
      expect(registerMock).toHaveBeenCalledWith('user@test.com', 'Password123!', 'User Test');
      expect(pushMock).toHaveBeenCalledWith('/welcome');
    });
  });

  it('shows backend error when registration fails', async () => {
    registerMock.mockRejectedValue({ response: { data: { message: 'Email já cadastrado' } } });
    render(<RegisterPage />);

    fireEvent.change(screen.getByLabelText(/nome completo/i), { target: { value: 'User Test' } });
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'user@test.com' } });
    fireEvent.change(screen.getByLabelText(/^senha$/i), { target: { value: 'Password123!' } });
    fireEvent.change(screen.getByLabelText(/confirmar senha/i), { target: { value: 'Password123!' } });
    fireEvent.click(screen.getByRole('button', { name: 'Criar Conta' }));

    expect(await screen.findByText('Email já cadastrado')).toBeInTheDocument();
    expect(pushMock).not.toHaveBeenCalled();
  });
});
