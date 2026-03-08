/** @jest-environment node */

import { POST } from '@/app/api/v1/ai/chat/route';

describe('POST /api/v1/ai/chat', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    process.env = { ...originalEnv };
    delete process.env.OPENAI_API_KEY;
  });

  afterAll(() => {
    process.env = originalEnv;
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
  });
});
