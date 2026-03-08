import axios from 'axios';
import { toast } from '@/stores/toast-store';

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
      description: 'A requisicao demorou alem do esperado. Tente novamente.',
    };
  }

  if (!axiosError?.response) {
    return {
      title: 'Falha de conexao',
      description: 'Nao foi possivel conectar ao servidor. Verifique sua rede.',
    };
  }

  const status = axiosError.response.status;

  if (typeof status === 'number' && status >= 500) {
    return {
      title: 'Erro interno do servidor',
      description: 'O servico esta instavel no momento. Tente novamente em instantes.',
    };
  }

  if (status === 429) {
    return {
      title: 'Muitas requisicoes',
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
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
  timeout: 30000,
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error?.response?.status === 401) {
      toast.info('Sessao expirada', 'Faca login novamente para continuar.');
      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
      return Promise.reject(error);
    }

    const payload = getGlobalErrorToastPayload(error);
    if (payload) {
      showGlobalErrorToast(payload);
    }

    return Promise.reject(error);
  }
);

export default api;
