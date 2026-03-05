import axios from 'axios';
import { toast } from '@/stores/toast-store';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || '';
const GLOBAL_TOAST_DEDUPE_MS = 2000;

let lastGlobalToast = {
  key: '',
  at: 0,
};

export const getGlobalErrorToastPayload = (error: unknown): { title: string; description: string } | null => {
  const axiosError = error as {
    code?: string;
    response?: { status?: number; data?: { message?: string } };
  };

  if (axiosError?.code === 'ECONNABORTED') {
    return {
      title: 'Tempo de resposta excedido',
      description: 'A requisição demorou além do esperado. Tente novamente.',
    };
  }

  if (!axiosError?.response) {
    return {
      title: 'Falha de conexão',
      description: 'Não foi possível conectar ao servidor. Verifique sua rede.',
    };
  }

  const status = axiosError.response.status;

  if (typeof status === 'number' && status >= 500) {
    return {
      title: 'Erro interno do servidor',
      description: 'O serviço está instável no momento. Tente novamente em instantes.',
    };
  }

  if (status === 429) {
    return {
      title: 'Muitas requisições',
      description: 'Aguarde alguns segundos antes de tentar novamente.',
    };
  }

  return null;
};

const showGlobalErrorToast = (payload: { title: string; description: string }) => {
  const key = `${payload.title}:${payload.description}`;
  const now = Date.now();
  if (lastGlobalToast.key === key && now - lastGlobalToast.at < GLOBAL_TOAST_DEDUPE_MS) {
    return;
  }
  lastGlobalToast = { key, at: now };
  toast.error(payload.title, payload.description);
};

export const setAuthCookies = (accessToken: string, refreshToken: string) => {
  if (typeof document === 'undefined') return;
  document.cookie = `access_token=${accessToken}; Path=/; SameSite=Lax`;
  document.cookie = `refresh_token=${refreshToken}; Path=/; SameSite=Lax`;
};

export const clearAuthCookies = () => {
  if (typeof document === 'undefined') return;
  document.cookie = 'access_token=; Path=/; Max-Age=0; SameSite=Lax';
  document.cookie = 'refresh_token=; Path=/; Max-Age=0; SameSite=Lax';
};

export const api = axios.create({
  baseURL: `${API_BASE_URL}/api/v1`,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

// Request interceptor: attach JWT
api.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

// Response interceptor: handle 401 and refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem('refresh_token');
        if (!refreshToken) {
          throw new Error('No refresh token');
        }

        const { data } = await axios.post(`${API_BASE_URL}/api/v1/auth/refresh`, {
          refreshToken,
        });

        localStorage.setItem('access_token', data.data.accessToken);
        localStorage.setItem('refresh_token', data.data.refreshToken);
        setAuthCookies(data.data.accessToken, data.data.refreshToken);

        originalRequest.headers.Authorization = `Bearer ${data.data.accessToken}`;
        return api(originalRequest);
      } catch {
        localStorage.removeItem('access_token');
        localStorage.removeItem('refresh_token');
        clearAuthCookies();
        toast.info('Sessão expirada', 'Faça login novamente para continuar.');
        if (typeof window !== 'undefined') {
          window.location.href = '/login';
        }
      }
    }

    if (error?.response?.status !== 401) {
      const payload = getGlobalErrorToastPayload(error);
      if (payload) {
        showGlobalErrorToast(payload);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
