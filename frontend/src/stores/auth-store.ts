import { create } from 'zustand';

interface User {
  id: string;
  email: string;
  fullName: string;
  avatarUrl?: string;
  role: string;
  status: string;
}

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  fetchUser: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: true,

  fetchUser: async () => {
    // Auth disabled — auto-authenticate with default user
    set({
      user: { id: 'local-user', email: 'user@lume.ai', fullName: 'Lume User', role: 'ADMIN', status: 'ACTIVE' },
      isAuthenticated: true,
      isLoading: false,
    });
  },
}));
