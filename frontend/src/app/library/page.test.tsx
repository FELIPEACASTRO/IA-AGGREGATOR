import { fireEvent, render, screen } from '@testing-library/react';
import LibraryPage from './page';

const pushMock = jest.fn();
const setActiveConversationMock = jest.fn();
const toggleConversationPinnedMock = jest.fn();

const authState = {
  user: {
    fullName: 'Docker Admin',
    email: 'dockeradmin@ia-aggregator.local',
  },
  logout: jest.fn(),
};

const chatState = {
  conversations: [
    {
      id: 'conv-1',
      title: 'Planejamento trimestral',
      model: 'gpt-4o-mini',
      pinned: false,
      messages: [],
      createdAt: Date.now(),
      updatedAt: Date.now(),
    },
    {
      id: 'conv-2',
      title: 'Resumo executivo',
      model: 'claude-3-5-haiku',
      pinned: true,
      messages: [],
      createdAt: Date.now(),
      updatedAt: Date.now(),
    },
  ],
  setActiveConversation: setActiveConversationMock,
  toggleConversationPinned: toggleConversationPinnedMock,
};

jest.mock('next/navigation', () => ({
  useRouter: () => ({ push: pushMock }),
  usePathname: () => '/library',
}));

jest.mock('@/stores/chat-store', () => ({
  useChatStore: () => chatState,
}));

jest.mock('@/stores/auth-store', () => ({
  useAuthStore: Object.assign((selector: (state: typeof authState) => unknown) => selector(authState), {
    getState: () => authState,
  }),
}));

describe('LibraryPage', () => {
  beforeEach(() => {
    pushMock.mockReset();
    setActiveConversationMock.mockReset();
    toggleConversationPinnedMock.mockReset();
  });

  it('renders conversations from chat store', () => {
    render(<LibraryPage />);

    expect(screen.getByText('Planejamento trimestral')).toBeInTheDocument();
    expect(screen.getByText('Resumo executivo')).toBeInTheDocument();
  });

  it('filters conversations by search query', () => {
    render(<LibraryPage />);

    fireEvent.change(screen.getByPlaceholderText(/buscar/i), {
      target: { value: 'Resumo' },
    });

    expect(screen.queryByText('Planejamento trimestral')).not.toBeInTheDocument();
    expect(screen.getByText('Resumo executivo')).toBeInTheDocument();
  });

  it('opens selected conversation in chat', () => {
    render(<LibraryPage />);

    fireEvent.click(screen.getAllByRole('button', { name: 'Abrir' })[0]);

    expect(setActiveConversationMock).toHaveBeenCalledWith('conv-1');
    expect(pushMock).toHaveBeenCalledWith('/chat');
  });

  it('toggles pin action from list', () => {
    render(<LibraryPage />);

    fireEvent.click(screen.getAllByRole('button', { name: /fixar|desafixar/i })[1]);

    expect(toggleConversationPinnedMock).toHaveBeenCalledWith('conv-2');
  });
});
