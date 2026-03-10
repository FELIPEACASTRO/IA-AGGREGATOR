import axios from 'axios';
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
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, fullName: string) => Promise<void>;
  logout: () => Promise<void>;
  fetchUser: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: true,

  login: async (email, password) => {
    await axios.post('/api/auth/login', { email, password });
    set({ isAuthenticated: true });

    // Fetch user profile
    const userRes = await axios.get('/api/auth/session');
    set({ user: userRes.data.data });
  },

  register: async (email, password, fullName) => {
    await axios.post('/api/auth/register', { email, password, fullName });
    set({ isAuthenticated: true });

    const userRes = await axios.get('/api/auth/session');
    set({ user: userRes.data.data });
  },

  logout: async () => {
    try {
      await axios.post('/api/auth/logout', {}).catch(() => {});
    } finally {
      set({ user: null, isAuthenticated: false });
    }
  },

  fetchUser: async () => {
    try {
      const { data } = await axios.get('/api/auth/session');
      set({ user: data.data, isAuthenticated: true, isLoading: false });
    } catch {
      set({ user: null, isAuthenticated: false, isLoading: false });
    }
  },
}));
