/** @jest-environment node */

import { POST } from '@/app/api/v1/ai/chat/route';

describe('POST /api/v1/ai/chat', () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          content: 'Resposta real de teste',
          modelUsed: 'gpt-4o-mini',
          providerUsed: 'OpenAI',
          fallbackUsed: false,
          attempts: 1,
          requestId: 'req-123',
          usage: {
            inputTokens: 10,
            outputTokens: 20,
            totalTokens: 30,
          },
          estimatedCost: {
            providerId: 'openai',
            model: 'gpt-4o-mini',
            currency: 'USD',
            amount: 0.00012,
            supported: true,
          },
          latencyMs: 120,
          finishReason: 'completed',
        },
      }),
    }) as jest.Mock;
  });

  afterAll(() => {
    global.fetch = originalFetch;
  });

  it('returns success payload with gateway metadata', async () => {
    const request = new Request('http://localhost/api/v1/ai/chat', {
      method: 'POST',
      body: JSON.stringify({
        prompt: 'Explique fallback de modelos em uma frase.',
        preferredModel: 'gpt-4o-mini',
      }),
      headers: {
        'Content-Type': 'application/json',
      },
    });

    const response = await POST(request);
    const payload = await response.json();

    expect(response.status).toBe(200);
    expect(payload.success).toBe(true);
    expect(payload.data.modelUsed).toBe('gpt-4o-mini');
    expect(payload.data.agentVersion).toBe('v1');
    expect(payload.data.executionMode).toBe('live');
    expect(payload.data.requestId).toBe('req-123');
  });
});
