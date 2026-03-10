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

export const api = axios.create({
  baseURL: `${API_BASE_URL}/api/v1`,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        await axios.post('/api/auth/refresh', {});
        return api(originalRequest);
      } catch {
        void axios.post('/api/auth/logout').catch(() => undefined);
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
