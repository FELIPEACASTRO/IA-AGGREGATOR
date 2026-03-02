import { create } from 'zustand';
import api from '@/lib/api';

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
  logout: () => void;
  fetchUser: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: true,

  login: async (email, password) => {
    const { data } = await api.post('/auth/login', { email, password });
    localStorage.setItem('access_token', data.data.accessToken);
    localStorage.setItem('refresh_token', data.data.refreshToken);
    set({ isAuthenticated: true });

    // Fetch user profile
    const userRes = await api.get('/auth/me');
    set({ user: userRes.data.data });
  },

  register: async (email, password, fullName) => {
    const { data } = await api.post('/auth/register', { email, password, fullName });
    localStorage.setItem('access_token', data.data.accessToken);
    localStorage.setItem('refresh_token', data.data.refreshToken);
    set({ isAuthenticated: true });

    const userRes = await api.get('/auth/me');
    set({ user: userRes.data.data });
  },

  logout: () => {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    set({ user: null, isAuthenticated: false });
  },

  fetchUser: async () => {
    try {
      const token = localStorage.getItem('access_token');
      if (!token) {
        set({ isLoading: false });
        return;
      }
      const { data } = await api.get('/auth/me');
      set({ user: data.data, isAuthenticated: true, isLoading: false });
    } catch {
      set({ user: null, isAuthenticated: false, isLoading: false });
    }
  },
}));
