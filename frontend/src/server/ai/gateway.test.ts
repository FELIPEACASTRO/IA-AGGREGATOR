import { invokeChatGateway } from '@/server/ai/gateway';

describe('invokeChatGateway', () => {
  const originalEnv = process.env;
  const originalFetch = global.fetch;

  beforeEach(() => {
    process.env = { ...originalEnv };
    delete process.env.OPENAI_API_KEY;
    delete process.env.ANTHROPIC_API_KEY;
    delete process.env.GEMINI_API_KEY;
    global.fetch = jest.fn();
  });

  afterAll(() => {
    process.env = originalEnv;
    global.fetch = originalFetch;
  });

  it('returns a mock response when no live provider is configured', async () => {
    const result = await invokeChatGateway({
      prompt: 'Explique o que e fallback de modelos em uma frase.',
      preferredModel: 'gpt-4o-mini',
    });

    expect(result.executionMode).toBe('mock');
    expect(result.modelUsed).toBe('gpt-4o-mini');
    expect(result.providerUsed).toBe('OpenAI');
    expect(result.agentVersion).toBe('v1');
    expect(result.content.toLowerCase()).toContain('fallback de modelos');
  });

  it('returns a live response when the provider is configured', async () => {
    process.env.OPENAI_API_KEY = 'test-openai-key';
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        output_text: 'Fallback e a troca automatica para um modelo alternativo quando necessario.',
      }),
    }) as jest.Mock;

    const result = await invokeChatGateway({
      prompt: 'Explique o que e fallback de modelos em uma frase.',
      preferredModel: 'gpt-4o-mini',
    });

    expect(result.executionMode).toBe('live');
    expect(result.providerUsed).toBe('OpenAI');
    expect(result.modelUsed).toBe('gpt-4o-mini');
    expect(global.fetch).toHaveBeenCalledWith(
      'https://api.openai.com/v1/responses',
      expect.objectContaining({
        method: 'POST',
      })
    );
  });

  it('falls back to another configured provider when the preferred one is unavailable', async () => {
    process.env.OPENAI_API_KEY = 'test-openai-key';
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        output_text: 'Resposta vinda do fallback configurado.',
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
