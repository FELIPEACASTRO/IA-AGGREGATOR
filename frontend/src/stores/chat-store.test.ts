import { useChatStore } from '@/stores/chat-store';

describe('useChatStore', () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    global.fetch = jest.fn();
    jest.clearAllMocks();

    useChatStore.setState({
      conversations: [],
      activeConversationId: null,
      selectedModel: 'gpt-4o-mini',
      isLoaded: false,
      isSending: false,
      isStreaming: false,
      activeRequestController: null,
      activeStreamId: null,
      availableModels: useChatStore.getState().availableModels,
    });
  });

  afterAll(() => {
    global.fetch = originalFetch;
  });

  it('loads conversations from the real BFF contract', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          conversations: [
            {
              id: 'conv-1',
              workspaceId: 'ws-1',
              createdById: 'user-1',
              title: 'Planejamento',
              model: 'gpt-4o-mini',
              pinned: false,
              archivedAt: null,
              lastMessageAt: null,
              createdAt: '2026-03-08T00:00:00.000Z',
              updatedAt: '2026-03-08T00:00:00.000Z',
              messages: [],
            },
          ],
        },
      }),
    });

    await useChatStore.getState().loadConversations();

    const state = useChatStore.getState();
    expect(state.isLoaded).toBe(true);
    expect(state.conversations).toHaveLength(1);
    expect(state.activeConversationId).toBe('conv-1');
  });

  it('creates a conversation via API using the selected model', async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          conversation: {
            id: 'conv-1',
            workspaceId: 'ws-1',
            createdById: 'user-1',
            title: 'Nova Conversa',
            model: 'claude-3-5-haiku',
            pinned: false,
            archivedAt: null,
            lastMessageAt: null,
            createdAt: '2026-03-08T00:00:00.000Z',
            updatedAt: '2026-03-08T00:00:00.000Z',
            messages: [],
          },
        },
      }),
    });

    useChatStore.getState().setSelectedModel('claude-3-5-haiku');
    const conversationId = await useChatStore.getState().createConversation();

    expect(conversationId).toBe('conv-1');
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/v1/chat/conversations',
      expect.objectContaining({
        method: 'POST',
        credentials: 'include',
      })
    );
    expect(useChatStore.getState().conversations[0]?.model).toBe('claude-3-5-haiku');
  });

  it('sends message and reconciles persisted conversation response', async () => {
    (global.fetch as jest.Mock)
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          success: true,
          data: {
            conversation: {
              id: 'conv-1',
              workspaceId: 'ws-1',
              createdById: 'user-1',
              title: 'Nova Conversa',
              model: 'gpt-4o-mini',
              pinned: false,
              archivedAt: null,
              lastMessageAt: null,
              createdAt: '2026-03-08T00:00:00.000Z',
              updatedAt: '2026-03-08T00:00:00.000Z',
              messages: [],
            },
          },
        }),
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          success: true,
          data: {
            conversation: {
              id: 'conv-1',
              workspaceId: 'ws-1',
              createdById: 'user-1',
              title: 'Ola IA',
              model: 'gpt-4o-mini',
              pinned: false,
              archivedAt: null,
              lastMessageAt: '2026-03-08T00:00:02.000Z',
              createdAt: '2026-03-08T00:00:00.000Z',
              updatedAt: '2026-03-08T00:00:02.000Z',
              messages: [
                {
                  id: 'msg-1',
                  conversationId: 'conv-1',
                  role: 'user',
                  content: 'Ola IA',
                  modelUsed: null,
                  providerUsed: null,
                  agentUsed: null,
                  agentVersion: null,
                  fallbackUsed: false,
                  attempts: null,
                  isComplete: true,
                  createdAt: '2026-03-08T00:00:01.000Z',
                  updatedAt: '2026-03-08T00:00:01.000Z',
                },
                {
                  id: 'msg-2',
                  conversationId: 'conv-1',
                  role: 'assistant',
                  content: 'Resposta persistida',
                  modelUsed: 'gpt-4o-mini',
                  providerUsed: 'OpenAI',
                  agentUsed: 'Lume Chat',
                  agentVersion: 'v1',
                  fallbackUsed: false,
                  attempts: 1,
                  isComplete: true,
                  createdAt: '2026-03-08T00:00:02.000Z',
                  updatedAt: '2026-03-08T00:00:02.000Z',
                },
              ],
            },
          },
        }),
      });

    await useChatStore.getState().sendMessage('Ola IA');

    const conversation = useChatStore.getState().conversations[0];
    expect(conversation.messages).toHaveLength(2);
    expect(conversation.messages[0].role).toBe('user');
    expect(conversation.messages[1].role).toBe('assistant');
    expect(conversation.messages[1].content).toBe('Resposta persistida');
    expect(useChatStore.getState().isSending).toBe(false);
    expect(useChatStore.getState().isStreaming).toBe(false);
  });

  it('clears messages from the persisted conversation', async () => {
    useChatStore.setState({
      conversations: [
        {
          id: 'conv-1',
          title: 'Conversa antiga',
          model: 'gpt-4o-mini',
          pinned: false,
          messages: [
            {
              id: 'm1',
              role: 'user',
              content: 'Mensagem',
              timestamp: Date.now(),
            },
          ],
          createdAt: Date.now(),
          updatedAt: Date.now(),
        },
      ],
      activeConversationId: 'conv-1',
      selectedModel: 'gpt-4o-mini',
      isLoaded: true,
      isSending: false,
      isStreaming: false,
      activeRequestController: null,
      activeStreamId: null,
      availableModels: useChatStore.getState().availableModels,
    });

    (global.fetch as jest.Mock).mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          conversation: {
            id: 'conv-1',
            workspaceId: 'ws-1',
            createdById: 'user-1',
            title: 'Nova Conversa',
            model: 'gpt-4o-mini',
            pinned: false,
            archivedAt: null,
            lastMessageAt: null,
            createdAt: '2026-03-08T00:00:00.000Z',
            updatedAt: '2026-03-08T00:00:03.000Z',
            messages: [],
          },
        },
      }),
    });

    await useChatStore.getState().clearConversationMessages('conv-1');

    const conversation = useChatStore.getState().conversations[0];
    expect(conversation.messages).toHaveLength(0);
    expect(conversation.title).toBe('Nova Conversa');
  });
});
