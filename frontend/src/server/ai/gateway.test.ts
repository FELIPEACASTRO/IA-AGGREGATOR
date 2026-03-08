import { invokeChatGateway } from '@/server/ai/gateway';

describe('invokeChatGateway', () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    global.fetch = jest.fn();
  });

  afterAll(() => {
    global.fetch = originalFetch;
  });

  it('propaga erro claro do backend canônico', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: false,
      status: 422,
      json: async () => ({
        success: false,
        message: 'Provider OpenAI nao configurado',
      }),
    }) as jest.Mock;

    await expect(
      invokeChatGateway({
        prompt: 'Explique o que e fallback de modelos em uma frase.',
        preferredModel: 'gpt-4o-mini',
      })
    ).rejects.toThrow(/nao configurado/i);
  });

  it('retorna resposta live do backend canônico', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          content: 'Fallback e a troca automatica para um modelo alternativo quando necessario.',
          modelUsed: 'gpt-4o-mini',
          providerUsed: 'OpenAI',
          fallbackUsed: false,
          attempts: 1,
          requestId: 'req-openai',
          usage: {
            inputTokens: 12,
            outputTokens: 18,
            totalTokens: 30,
          },
          estimatedCost: {
            providerId: 'openai',
            model: 'gpt-4o-mini',
            currency: 'USD',
            amount: 0.00011,
            supported: true,
          },
          latencyMs: 88,
          finishReason: 'completed',
        },
      }),
    }) as jest.Mock;

    const result = await invokeChatGateway({
      prompt: 'Explique o que e fallback de modelos em uma frase.',
      preferredModel: 'gpt-4o-mini',
    });

    expect(result.executionMode).toBe('live');
    expect(result.providerUsed).toBe('OpenAI');
    expect(result.modelUsed).toBe('gpt-4o-mini');
    expect(result.requestId).toBe('req-openai');
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringMatching(/\/api\/v1\/ai\/chat$/),
      expect.objectContaining({
        method: 'POST',
      })
    );
  });

  it('mantem metadata de fallback retornada pelo backend', async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          content: 'Resposta vinda do fallback configurado.',
          modelUsed: 'gpt-4o-mini',
          providerUsed: 'OpenAI',
          fallbackUsed: true,
          attempts: 2,
          requestId: 'req-fallback',
          usage: {
            inputTokens: 12,
            outputTokens: 18,
            totalTokens: 30,
          },
          estimatedCost: {
            providerId: 'openai',
            model: 'gpt-4o-mini',
            currency: 'USD',
            amount: 0.00011,
            supported: true,
          },
          latencyMs: 101,
          finishReason: 'completed',
        },
      }),
    }) as jest.Mock;

    const result = await invokeChatGateway({
      prompt: 'Responda com uma frase curta.',
      preferredModel: 'claude-3-5-haiku',
    });

    expect(result.executionMode).toBe('live');
    expect(result.fallbackUsed).toBe(true);
    expect(result.providerUsed).toBe('OpenAI');
    expect(result.modelUsed).toBe('gpt-4o-mini');
  });
});
