import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import api from '@/lib/api';

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'error';
  content: string;
  modelUsed?: string;
  providerUsed?: string;
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

interface ChatState {
  conversations: Conversation[];
  activeConversationId: string | null;
  selectedModel: string;
  isSending: boolean;
  isStreaming: boolean;
  activeRequestController: AbortController | null;
  activeStreamId: string | null;
  availableModels: { id: string; label: string; provider: string }[];

  setSelectedModel: (model: string) => void;
  createConversation: () => string;
  setActiveConversation: (id: string) => void;
  renameConversation: (id: string, title: string) => void;
  toggleConversationPinned: (id: string) => void;
  clearConversationMessages: (id: string) => void;
  sendMessage: (prompt: string) => Promise<void>;
  stopGenerating: () => void;
  deleteConversation: (id: string) => void;
}

const AVAILABLE_MODELS = [
  { id: 'gpt-4o-mini', label: 'GPT-4o Mini', provider: 'OpenAI' },
  { id: 'gpt-4.1-mini', label: 'GPT-4.1 Mini', provider: 'OpenAI' },
  { id: 'claude-3-5-haiku', label: 'Claude 3.5 Haiku', provider: 'Anthropic' },
  { id: 'gemini-1.5-flash', label: 'Gemini 1.5 Flash', provider: 'Google' },
  { id: 'deepseek-chat', label: 'DeepSeek Chat', provider: 'DeepSeek' },
  { id: 'deepseek-reasoner', label: 'DeepSeek Reasoner', provider: 'DeepSeek' },
  { id: 'llama-3.1-8b-instant', label: 'Llama 3.1 8B', provider: 'Groq' },
  { id: 'llama-3.1-70b-versatile', label: 'Llama 3.1 70B', provider: 'Groq' },
  { id: 'mistral-small-latest', label: 'Mistral Small', provider: 'Mistral' },
  { id: 'mistral-large-latest', label: 'Mistral Large', provider: 'Mistral' },
  { id: 'command-r', label: 'Command R', provider: 'Cohere' },
  { id: 'command-r-plus', label: 'Command R+', provider: 'Cohere' },
  { id: 'sonar', label: 'Sonar', provider: 'Perplexity' },
  { id: 'sonar-pro', label: 'Sonar Pro', provider: 'Perplexity' },
];

let nextMsgId = 1;
const uid = () => `msg-${Date.now()}-${nextMsgId++}`;
const convUid = () => `conv-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

const sortConversations = (conversations: Conversation[]) =>
  [...conversations].sort((a, b) => {
    if (a.pinned !== b.pinned) return a.pinned ? -1 : 1;
    return b.updatedAt - a.updatedAt;
  });

export const useChatStore = create<ChatState>()(
  persist(
    (set, get) => ({
      conversations: [],
      activeConversationId: null,
      selectedModel: 'gpt-4o-mini',
      isSending: false,
      isStreaming: false,
      activeRequestController: null,
      activeStreamId: null,
      availableModels: AVAILABLE_MODELS,

      setSelectedModel: (model) =>
        set((s) => ({
          selectedModel: model,
          conversations: sortConversations(
            s.conversations.map((conversation) =>
              conversation.id === s.activeConversationId
                ? { ...conversation, model, updatedAt: Date.now() }
                : conversation
            )
          ),
        })),

      createConversation: () => {
        const id = convUid();
        const now = Date.now();
        const conv: Conversation = {
          id,
          title: 'Nova Conversa',
          model: get().selectedModel,
          pinned: false,
          messages: [],
          createdAt: now,
          updatedAt: now,
        };
        set((s) => ({
          conversations: sortConversations([conv, ...s.conversations]),
          activeConversationId: id,
        }));
        return id;
      },

      setActiveConversation: (id) =>
        set((s) => {
          const conversation = s.conversations.find((item) => item.id === id);
          return {
            activeConversationId: id,
            selectedModel: conversation?.model || s.selectedModel,
          };
        }),

      renameConversation: (id, title) =>
        set((s) => ({
          conversations: sortConversations(
            s.conversations.map((conversation) =>
              conversation.id === id
                ? {
                    ...conversation,
                    title: title.trim() || conversation.title,
                    updatedAt: Date.now(),
                  }
                : conversation
            )
          ),
        })),

      toggleConversationPinned: (id) =>
        set((s) => ({
          conversations: sortConversations(
            s.conversations.map((conversation) =>
              conversation.id === id
                ? {
                    ...conversation,
                    pinned: !conversation.pinned,
                    updatedAt: Date.now(),
                  }
                : conversation
            )
          ),
        })),

      clearConversationMessages: (id) =>
        set((s) => ({
          conversations: sortConversations(
            s.conversations.map((conversation) =>
              conversation.id === id
                ? {
                    ...conversation,
                    messages: [],
                    title: 'Nova Conversa',
                    updatedAt: Date.now(),
                  }
                : conversation
            )
          ),
        })),

      sendMessage: async (prompt: string) => {
        const state = get();
        let convId = state.activeConversationId;

        const requestController = new AbortController();
        const streamId = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

        if (!convId) {
          convId = get().createConversation();
        }

        const userMsg: ChatMessage = {
          id: uid(),
          role: 'user',
          content: prompt,
          timestamp: Date.now(),
        };

        set((s) => ({
          isSending: true,
          isStreaming: false,
          activeRequestController: requestController,
          activeStreamId: streamId,
          conversations: sortConversations(
            s.conversations.map((c) =>
              c.id === convId
                ? {
                    ...c,
                    messages: [...c.messages, userMsg],
                    title: c.messages.length === 0 ? prompt.slice(0, 40) : c.title,
                    updatedAt: Date.now(),
                  }
                : c
            )
          ),
        }));

        try {
          const conv = get().conversations.find((c) => c.id === convId);
          const model = conv?.model || state.selectedModel;

          const { data } = await api.post('/ai/chat', {
            prompt,
            preferredModel: model,
          }, {
            signal: requestController.signal,
          });

          const assistantMsg: ChatMessage = {
            id: uid(),
            role: 'assistant',
            content: '',
            modelUsed: data.data.modelUsed,
            providerUsed: data.data.providerUsed,
            fallbackUsed: data.data.fallbackUsed,
            attempts: data.data.attempts,
            timestamp: Date.now(),
          };

          set((s) => ({
            isStreaming: true,
            conversations: sortConversations(
              s.conversations.map((c) =>
                c.id === convId
                  ? { ...c, messages: [...c.messages, assistantMsg], updatedAt: Date.now() }
                  : c
              )
            ),
          }));

          const fullContent = String(data?.data?.content || '');
          for (let i = 0; i < fullContent.length; i += 4) {
            const current = get();
            if (current.activeStreamId !== streamId) break;

            const chunk = fullContent.slice(i, i + 4);
            set((s) => ({
              conversations: sortConversations(
                s.conversations.map((conversation) => {
                  if (conversation.id !== convId) return conversation;
                  return {
                    ...conversation,
                    updatedAt: Date.now(),
                    messages: conversation.messages.map((message) =>
                      message.id === assistantMsg.id
                        ? { ...message, content: `${message.content}${chunk}` }
                        : message
                    ),
                  };
                })
              ),
            }));

            await sleep(12);
          }

          const afterStream = get();
          if (afterStream.activeStreamId === streamId) {
            set({
              isSending: false,
              isStreaming: false,
              activeRequestController: null,
              activeStreamId: null,
            });
          }
        } catch (err: unknown) {
          const isAbort =
            (err instanceof Error && err.name === 'CanceledError') ||
            (err instanceof Error && err.name === 'AbortError');

          if (isAbort) {
            set({
              isSending: false,
              isStreaming: false,
              activeRequestController: null,
              activeStreamId: null,
            });
            return;
          }

          const axiosErr = err as { response?: { data?: { message?: string } } };
          const errorContent =
            axiosErr?.response?.data?.message ||
            (err instanceof Error ? err.message : 'Erro ao enviar mensagem');

          const errorMsg: ChatMessage = {
            id: uid(),
            role: 'error',
            content: errorContent,
            timestamp: Date.now(),
          };

          set((s) => ({
            isSending: false,
            isStreaming: false,
            activeRequestController: null,
            activeStreamId: null,
            conversations: sortConversations(
              s.conversations.map((c) =>
                c.id === convId
                  ? { ...c, messages: [...c.messages, errorMsg], updatedAt: Date.now() }
                  : c
              )
            ),
          }));
        }
      },

      stopGenerating: () => {
        const { activeRequestController } = get();
        activeRequestController?.abort();
        set({
          isSending: false,
          isStreaming: false,
          activeRequestController: null,
          activeStreamId: null,
        });
      },

      deleteConversation: (id) =>
        set((s) => {
          const remaining = s.conversations.filter((c) => c.id !== id);
          const nextActive =
            s.activeConversationId === id ? remaining[0]?.id || null : s.activeConversationId;

          return {
            conversations: sortConversations(remaining),
            activeConversationId: nextActive,
            selectedModel:
              remaining.find((conversation) => conversation.id === nextActive)?.model ||
              s.selectedModel,
          };
        }),
    }),
    {
      name: 'ia-aggregator-chat-store',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        conversations: state.conversations,
        activeConversationId: state.activeConversationId,
        selectedModel: state.selectedModel,
      }),
    }
  )
);
