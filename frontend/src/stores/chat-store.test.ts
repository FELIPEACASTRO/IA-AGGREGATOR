import { useChatStore } from '@/stores/chat-store';
import api from '@/lib/api';

jest.mock('@/lib/api', () => ({
  __esModule: true,
  default: {
    post: jest.fn(),
  },
}));

describe('useChatStore', () => {
  const mockedApi = api as unknown as { post: jest.Mock };

  beforeEach(() => {
    localStorage.removeItem('ia-aggregator-chat-store');
    jest.clearAllMocks();

    useChatStore.setState({
      conversations: [],
      activeConversationId: null,
      selectedModel: 'gpt-4o-mini',
      isSending: false,
      isStreaming: false,
      activeRequestController: null,
      activeStreamId: null,
    });
  });

  it('creates conversation with current selected model', () => {
    useChatStore.getState().setSelectedModel('claude-3-5-haiku');
    const conversationId = useChatStore.getState().createConversation();

    const state = useChatStore.getState();
    const created = state.conversations.find((item) => item.id === conversationId);

    expect(created).toBeDefined();
    expect(created?.model).toBe('claude-3-5-haiku');
    expect(state.activeConversationId).toBe(conversationId);
  });

  it('updates active conversation model when model selector changes', () => {
    const conversationId = useChatStore.getState().createConversation();
    useChatStore.getState().setSelectedModel('gemini-1.5-flash');

    const conversation = useChatStore
      .getState()
      .conversations.find((item) => item.id === conversationId);

    expect(conversation?.model).toBe('gemini-1.5-flash');
  });

  it('sends message and appends assistant response', async () => {
    mockedApi.post.mockResolvedValue({
      data: {
        data: {
          content: 'Resposta simulada',
          modelUsed: 'gpt-4o-mini',
          providerUsed: 'openai',
          fallbackUsed: false,
          attempts: 1,
        },
      },
    });

    const conversationId = useChatStore.getState().createConversation();
    await useChatStore.getState().sendMessage('Olá IA');

    const conversation = useChatStore
      .getState()
      .conversations.find((item) => item.id === conversationId);

    expect(mockedApi.post).toHaveBeenCalledWith(
      '/ai/chat',
      expect.objectContaining({ prompt: 'Olá IA', preferredModel: 'gpt-4o-mini' }),
      expect.objectContaining({ signal: expect.any(AbortSignal) })
    );
    expect(conversation?.messages).toHaveLength(2);
    expect(conversation?.messages[0].role).toBe('user');
    expect(conversation?.messages[1].role).toBe('assistant');
    expect(conversation?.messages[1].content).toBe('Resposta simulada');
    expect(useChatStore.getState().isSending).toBe(false);
    expect(useChatStore.getState().isStreaming).toBe(false);
  });

  it('clears messages from active conversation', () => {
    const conversationId = useChatStore.getState().createConversation();

    useChatStore.setState((state) => ({
      conversations: state.conversations.map((conversation) =>
        conversation.id === conversationId
          ? {
              ...conversation,
              title: 'Conversa antiga',
              messages: [
                {
                  id: 'm1',
                  role: 'user',
                  content: 'Mensagem',
                  timestamp: Date.now(),
                },
              ],
            }
          : conversation
      ),
    }));

    useChatStore.getState().clearConversationMessages(conversationId);

    const conversation = useChatStore
      .getState()
      .conversations.find((item) => item.id === conversationId);

    expect(conversation?.messages).toHaveLength(0);
    expect(conversation?.title).toBe('Nova Conversa');
  });
});
