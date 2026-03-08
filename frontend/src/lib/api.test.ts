import { getGlobalErrorToastPayload } from '@/lib/api';

describe('getGlobalErrorToastPayload', () => {
  it('returns timeout feedback for ECONNABORTED', () => {
    const payload = getGlobalErrorToastPayload({ code: 'ECONNABORTED' });

    expect(payload).toEqual({
      title: 'Tempo de resposta excedido',
      description: 'A requisicao demorou alem do esperado. Tente novamente.',
    });
  });

  it('returns connectivity feedback when response is missing', () => {
    const payload = getGlobalErrorToastPayload({});

    expect(payload).toEqual({
      title: 'Falha de conexao',
      description: 'Nao foi possivel conectar ao servidor. Verifique sua rede.',
    });
  });

  it('returns server feedback for 5xx status', () => {
    const payload = getGlobalErrorToastPayload({ response: { status: 503 } });

    expect(payload).toEqual({
      title: 'Erro interno do servidor',
      description: 'O servico esta instavel no momento. Tente novamente em instantes.',
    });
  });

  it('returns rate-limit feedback for 429', () => {
    const payload = getGlobalErrorToastPayload({ response: { status: 429 } });

    expect(payload).toEqual({
      title: 'Muitas requisicoes',
      description: 'Aguarde alguns segundos antes de tentar novamente.',
    });
  });

  it('returns null for non-global 4xx errors', () => {
    const payload = getGlobalErrorToastPayload({ response: { status: 400 } });
    expect(payload).toBeNull();
  });
});
