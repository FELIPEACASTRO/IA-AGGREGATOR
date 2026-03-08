import { create } from 'zustand';
import { MODEL_CATALOG } from '@/lib/model-catalog';
import type { ChatConversationDto, ConsumerChatMessageDto } from '@/lib/contracts/platform';

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'error';
  content: string;
  modelUsed?: string;
  providerUsed?: string;
  agentUsed?: string;
  agentVersion?: string;
  fallbackUsed?: boolean;
  attempts?: number;
  timestamp: number;
}

export interface Conversation {
  id: string;
  title: string;
  model: string;
  pinned: boolean;
  messages: ChatMessage[];
  createdAt: number;
  updatedAt: number;
}

type ConversationRecord = ChatConversationDto & {
  messages: ConsumerChatMessageDto[];
};

type ApiEnvelope<T> = {
  success: boolean;
  data?: T;
  message?: string;
};

interface ChatState {
  conversations: Conversation[];
  activeConversationId: string | null;
  selectedModel: string;
  isLoaded: boolean;
  isSending: boolean;
  isStreaming: boolean;
  activeRequestController: AbortController | null;
  activeStreamId: string | null;
  availableModels: { id: string; label: string; provider: string; maxContextTokens: number }[];

  loadConversations: () => Promise<void>;
  setSelectedModel: (model: string) => void;
  createConversation: () => Promise<string>;
  setActiveConversation: (id: string) => void;
  renameConversation: (id: string, title: string) => Promise<void>;
  toggleConversationPinned: (id: string) => Promise<void>;
  clearConversationMessages: (id: string) => Promise<void>;
  sendMessage: (prompt: string) => Promise<void>;
  stopGenerating: () => void;
  deleteConversation: (id: string) => Promise<void>;
}

const AVAILABLE_MODELS = MODEL_CATALOG;

function sortConversations(conversations: Conversation[]) {
  return [...conversations].sort((a, b) => {
    if (a.pinned !== b.pinned) return a.pinned ? -1 : 1;
    return b.updatedAt - a.updatedAt;
  });
}

function mapMessage(message: ConsumerChatMessageDto): ChatMessage {
  return {
    id: message.id,
    role: message.role,
    content: message.content,
    modelUsed: message.modelUsed ?? undefined,
    providerUsed: message.providerUsed ?? undefined,
    agentUsed: message.agentUsed ?? undefined,
    agentVersion: message.agentVersion ?? undefined,
    fallbackUsed: message.fallbackUsed ?? undefined,
    attempts: message.attempts ?? undefined,
    timestamp: Date.parse(message.createdAt),
  };
}

function mapConversation(record: ConversationRecord): Conversation {
  return {
    id: record.id,
    title: record.title,
    model: record.model,
    pinned: record.pinned,
    messages: record.messages.map(mapMessage),
    createdAt: Date.parse(record.createdAt),
    updatedAt: Date.parse(record.updatedAt),
  };
}

async function readEnvelope<T>(response: Response): Promise<T> {
  const payload = (await response.json().catch(() => null)) as ApiEnvelope<T> | null;
  if (!response.ok || !payload?.success || !payload.data) {
    throw new Error(payload?.message || 'Falha ao comunicar com o servidor');
  }
  return payload.data;
}

async function requestJson<T>(input: string, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    credentials: 'include',
    cache: 'no-store',
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
  });
  return readEnvelope<T>(response);
}

function reconcileConversations(
  conversations: Conversation[],
  updatedConversation: Conversation,
  activeConversationId: string | null,
  fallbackModel: string
) {
  const nextConversations = sortConversations([
    updatedConversation,
    ...conversations.filter((conversation) => conversation.id !== updatedConversation.id),
  ]);

  const nextActiveConversationId = activeConversationId ?? updatedConversation.id;
  const nextActiveConversation =
    nextConversations.find((conversation) => conversation.id === nextActiveConversationId) ??
    nextConversations[0];

  return {
    conversations: nextConversations,
    activeConversationId: nextActiveConversation?.id ?? null,
    selectedModel: nextActiveConversation?.model ?? fallbackModel,
  };
}

export const useChatStore = create<ChatState>((set, get) => ({
  conversations: [],
  activeConversationId: null,
  selectedModel: 'gpt-4o-mini',
  isLoaded: false,
  isSending: false,
  isStreaming: false,
  activeRequestController: null,
  activeStreamId: null,
  availableModels: AVAILABLE_MODELS,

  loadConversations: async () => {
    try {
      const data = await requestJson<{ conversations: ConversationRecord[] }>('/api/v1/chat/conversations');
      const records = Array.isArray(data.conversations) ? data.conversations : [];
      const conversations = sortConversations(records.map(mapConversation));

      set((state) => {
        const activeConversationId =
          state.activeConversationId &&
          conversations.some((conversation) => conversation.id === state.activeConversationId)
            ? state.activeConversationId
            : conversations[0]?.id ?? null;
        const selectedModel =
          conversations.find((conversation) => conversation.id === activeConversationId)?.model ??
          state.selectedModel;

        return {
          conversations,
          activeConversationId,
          selectedModel,
          isLoaded: true,
        };
      });
    } catch {
      set((state) => ({
        conversations: [],
        activeConversationId: null,
        selectedModel: state.selectedModel,
        isLoaded: true,
      }));
    }
  },

  setSelectedModel: (model) => {
    set((state) => ({
      selectedModel: model,
      conversations: sortConversations(
        state.conversations.map((conversation) =>
          conversation.id === state.activeConversationId
            ? { ...conversation, model, updatedAt: Date.now() }
            : conversation
        )
      ),
    }));

    const activeConversationId = get().activeConversationId;
    if (!activeConversationId) return;

    void requestJson<{ conversation: ConversationRecord }>(`/api/v1/chat/conversations/${activeConversationId}`, {
      method: 'PATCH',
      body: JSON.stringify({ model }),
    })
      .then((data) => {
        const updatedConversation = mapConversation(data.conversation);
        set((state) => reconcileConversations(
          state.conversations,
          updatedConversation,
          state.activeConversationId,
          state.selectedModel
        ));
      })
      .catch(() => undefined);
  },

  createConversation: async () => {
    const model = get().selectedModel;
    const data = await requestJson<{ conversation: ConversationRecord }>('/api/v1/chat/conversations', {
      method: 'POST',
      body: JSON.stringify({ model }),
    });

    const conversation = mapConversation(data.conversation);
    set((state) => reconcileConversations(state.conversations, conversation, conversation.id, model));
    return conversation.id;
  },

  setActiveConversation: (id) =>
    set((state) => {
      const conversation = state.conversations.find((item) => item.id === id);
      return {
        activeConversationId: id,
        selectedModel: conversation?.model || state.selectedModel,
      };
    }),

  renameConversation: async (id, title) => {
    const data = await requestJson<{ conversation: ConversationRecord }>(`/api/v1/chat/conversations/${id}`, {
      method: 'PATCH',
      body: JSON.stringify({ title }),
    });
    const conversation = mapConversation(data.conversation);
    set((state) => reconcileConversations(state.conversations, conversation, state.activeConversationId, state.selectedModel));
  },

  toggleConversationPinned: async (id) => {
    const current = get().conversations.find((conversation) => conversation.id === id);
    if (!current) return;

    const data = await requestJson<{ conversation: ConversationRecord }>(`/api/v1/chat/conversations/${id}`, {
      method: 'PATCH',
      body: JSON.stringify({ pinned: !current.pinned }),
    });
    const conversation = mapConversation(data.conversation);
    set((state) => reconcileConversations(state.conversations, conversation, state.activeConversationId, state.selectedModel));
  },

  clearConversationMessages: async (id) => {
    const data = await requestJson<{ conversation: ConversationRecord }>(`/api/v1/chat/conversations/${id}`, {
      method: 'PATCH',
      body: JSON.stringify({ clearMessages: true }),
    });
    const conversation = mapConversation(data.conversation);
    set((state) => reconcileConversations(state.conversations, conversation, state.activeConversationId, state.selectedModel));
  },

  sendMessage: async (prompt: string) => {
    const trimmed = prompt.trim();
    if (!trimmed) return;

    let conversationId = get().activeConversationId;
    if (!conversationId) {
      conversationId = await get().createConversation();
    }

    const requestController = new AbortController();
    const streamId = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
    const selectedModel =
      get().conversations.find((conversation) => conversation.id === conversationId)?.model ??
      get().selectedModel;

    set({
      isSending: true,
      isStreaming: true,
      activeRequestController: requestController,
      activeStreamId: streamId,
    });

    try {
      const response = await fetch(`/api/v1/chat/conversations/${conversationId}/messages`, {
        method: 'POST',
        credentials: 'include',
        cache: 'no-store',
        signal: requestController.signal,
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          prompt: trimmed,
          preferredModel: selectedModel,
        }),
      });

      const data = await readEnvelope<{ conversation: ConversationRecord }>(response);
      const conversation = mapConversation(data.conversation);

      set((state) => ({
        ...reconcileConversations(state.conversations, conversation, conversationId, state.selectedModel),
        isSending: false,
        isStreaming: false,
        activeRequestController: null,
        activeStreamId: null,
      }));
    } catch (error) {
      const isAbort =
        (error instanceof Error && error.name === 'AbortError') ||
        (error instanceof Error && error.name === 'CanceledError');

      if (!isAbort) {
        await get().loadConversations().catch(() => undefined);
      }

      set({
        isSending: false,
        isStreaming: false,
        activeRequestController: null,
        activeStreamId: null,
      });

      if (!isAbort) {
        throw error;
      }
    }
  },

  stopGenerating: () => {
    get().activeRequestController?.abort();
    set({
      isSending: false,
      isStreaming: false,
      activeRequestController: null,
      activeStreamId: null,
    });
  },

  deleteConversation: async (id) => {
    await requestJson<{ deleted: boolean }>(`/api/v1/chat/conversations/${id}`, {
      method: 'DELETE',
    });

    set((state) => {
      const conversations = sortConversations(
        state.conversations.filter((conversation) => conversation.id !== id)
      );
      const activeConversationId =
        state.activeConversationId === id ? conversations[0]?.id ?? null : state.activeConversationId;
      const selectedModel =
        conversations.find((conversation) => conversation.id === activeConversationId)?.model ??
        state.selectedModel;

      return {
        conversations,
        activeConversationId,
        selectedModel,
      };
    });
  },
}));
