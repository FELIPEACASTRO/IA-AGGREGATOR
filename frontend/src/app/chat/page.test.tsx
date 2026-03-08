import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import ChatPage from './page';
import type { ChatMessage } from '@/stores/chat-store';

const pushMock = jest.fn();

let searchPrompt: string | null = null;

type MockConversation = {
  id: string;
  title: string;
  model: string;
  pinned: boolean;
  messages: ChatMessage[];
  createdAt: number;
  updatedAt: number;
};

const chatStoreState = {
  conversations: [
    {
      id: 'conv-1',
      title: 'Conversa de teste',
      model: 'gpt-4o-mini',
      pinned: false,
      messages: [] as ChatMessage[],
      createdAt: Date.now(),
      updatedAt: Date.now(),
    },
  ] as MockConversation[],
  activeConversationId: 'conv-1',
  selectedModel: 'gpt-4o-mini',
  isSending: false,
  isStreaming: false,
  availableModels: [{ id: 'gpt-4o-mini', label: 'GPT-4o Mini', provider: 'OpenAI' }],
  setSelectedModel: jest.fn(),
  createConversation: jest.fn(),
  setActiveConversation: jest.fn(),
  renameConversation: jest.fn(),
  toggleConversationPinned: jest.fn(),
  clearConversationMessages: jest.fn(),
  sendMessage: jest.fn().mockResolvedValue(undefined),
  stopGenerating: jest.fn(),
  deleteConversation: jest.fn(),
};

jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: pushMock,
  }),
  usePathname: () => '/chat',
  useSearchParams: () => ({
    get: (key: string) => (key === 'prompt' ? searchPrompt : null),
  }),
}));

const authState = {
  user: { fullName: 'Docker Admin', email: 'dockeradmin@ia-aggregator.local' },
  isAuthenticated: true,
  isLoading: false,
  fetchUser: jest.fn(),
  workspaces: [],
  currentWorkspace: null,
  currentRole: null,
  switchWorkspace: jest.fn(),
  logout: jest.fn(),
};

jest.mock('@/stores/auth-store', () => ({
  useAuthStore: Object.assign((selector: (state: typeof authState) => unknown) => selector(authState), {
    getState: () => authState,
  }),
}));

jest.mock('@/stores/chat-store', () => ({
  useChatStore: (selector?: (state: typeof chatStoreState) => unknown) =>
    selector ? selector(chatStoreState) : chatStoreState,
}));

describe('ChatPage', () => {
  beforeEach(() => {
    pushMock.mockReset();
    searchPrompt = null;
    Object.values(chatStoreState).forEach((value) => {
      if (typeof value === 'function' && 'mockReset' in value) {
        (value as jest.Mock).mockReset();
      }
    });

    chatStoreState.sendMessage = jest.fn().mockResolvedValue(undefined);
    chatStoreState.createConversation = jest.fn();
    chatStoreState.stopGenerating = jest.fn();
    chatStoreState.conversations[0].messages = [];
    chatStoreState.isSending = false;
  });

  it('creates a new conversation when clicking Nova conversa', () => {
    render(<ChatPage />);

    fireEvent.click(screen.getAllByRole('button', { name: /nova conversa/i })[0]);

    expect(chatStoreState.createConversation).toHaveBeenCalledTimes(1);
  });

  it('sends message when pressing Enter in textarea', async () => {
    render(<ChatPage />);

    const textarea = screen.getByPlaceholderText(/como posso ajudar voce hoje/i);
    fireEvent.change(textarea, { target: { value: 'Mensagem de teste' } });
    fireEvent.keyDown(textarea, { key: 'Enter', code: 'Enter', shiftKey: false });

    await waitFor(() => {
      expect(chatStoreState.sendMessage).toHaveBeenCalledWith('Mensagem de teste');
    });
  });

  it('stops generation when clicking Parar', () => {
    chatStoreState.conversations[0].messages = [
      { id: 'msg-1', role: 'user', content: 'Mensagem', timestamp: Date.now() },
    ];
    chatStoreState.isSending = true;
    render(<ChatPage />);

    const stopButtons = screen.getAllByRole('button', { name: /parar geracao/i });
    fireEvent.click(stopButtons[0]);

    expect(chatStoreState.stopGenerating).toHaveBeenCalledTimes(1);
    chatStoreState.isSending = false;
  });

  it('prefills input from prompt query parameter', async () => {
    searchPrompt = 'Prompt vindo da biblioteca';
    render(<ChatPage />);

    const textarea = screen.getByPlaceholderText(/como posso ajudar voce hoje/i) as HTMLTextAreaElement;

    await waitFor(() => {
      expect(textarea.value).toBe('Prompt vindo da biblioteca');
    });
  });
});
