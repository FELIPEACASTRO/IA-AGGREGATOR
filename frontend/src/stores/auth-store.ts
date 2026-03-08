import { create } from 'zustand';
import type {
  SessionContext,
  SessionUser,
  WorkspaceDto,
  WorkspaceMembershipDto,
  WorkspaceRole,
} from '@/lib/contracts/platform';

type AuthState = {
  user: SessionUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  workspaces: WorkspaceMembershipDto[];
  currentWorkspace: WorkspaceDto | null;
  currentRole: WorkspaceRole | null;
  fetchUser: () => Promise<void>;
  switchWorkspace: (workspaceId: string) => Promise<void>;
  logout: () => Promise<void>;
};

type ApiEnvelope<T> = {
  success: boolean;
  data?: T;
  message?: string;
};

const unauthenticatedState = {
  user: null,
  isAuthenticated: false,
  workspaces: [],
  currentWorkspace: null,
  currentRole: null,
};

async function fetchSession(): Promise<SessionContext | null> {
  const response = await fetch('/api/v1/session', {
    credentials: 'include',
    cache: 'no-store',
  });

  if (response.status === 401) {
    return null;
  }

  const payload = (await response.json()) as ApiEnvelope<SessionContext>;
  if (!response.ok || !payload.success || !payload.data) {
    throw new Error(payload.message || 'Falha ao carregar sessao');
  }

  return payload.data;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  ...unauthenticatedState,
  isLoading: true,

  fetchUser: async () => {
    set({ isLoading: true });

    try {
      const session = await fetchSession();

      if (!session) {
        set({
          ...unauthenticatedState,
          isLoading: false,
        });
        return;
      }

      set({
        user: session.user,
        isAuthenticated: true,
        isLoading: false,
        workspaces: session.workspaces,
        currentWorkspace: session.currentWorkspace,
        currentRole: session.currentRole,
      });
    } catch {
      set({
        ...unauthenticatedState,
        isLoading: false,
      });
    }
  },

  switchWorkspace: async (workspaceId: string) => {
    const currentWorkspaceId = get().currentWorkspace?.id;
    if (!workspaceId || workspaceId === currentWorkspaceId) {
      return;
    }

    const response = await fetch('/api/v1/workspaces/current', {
      method: 'PATCH',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ workspaceId }),
    });

    if (!response.ok) {
      const payload = (await response.json().catch(() => null)) as ApiEnvelope<{ workspaceId: string }> | null;
      throw new Error(payload?.message || 'Falha ao trocar workspace');
    }

    await get().fetchUser();
  },

  logout: async () => {
    try {
      await fetch('/api/auth/logout', {
        method: 'POST',
        credentials: 'include',
      });
    } finally {
      if (typeof window !== 'undefined') {
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
      }

      set({
        ...unauthenticatedState,
        isLoading: false,
      });
    }
  },
}));
