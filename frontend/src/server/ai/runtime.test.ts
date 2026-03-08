import { summarizeProvider } from '@/server/ai/runtime';

describe('provider runtime summary', () => {
  const originalEnv = process.env;

  beforeEach(() => {
    process.env = { ...originalEnv };
    delete process.env.OPENAI_API_KEY;
  });

  afterAll(() => {
    process.env = originalEnv;
  });

  it('marks provider as missing when required keys are absent', () => {
    const summary = summarizeProvider('openai');

    expect(summary).toBeDefined();
    expect(summary?.status).toBe('missing');
    expect(summary?.configured).toBe(false);
    expect(summary?.missingKeys).toContain('OPENAI_API_KEY');
  });

  it('marks provider as configured when required keys are present', () => {
    process.env.OPENAI_API_KEY = 'test-openai-key';

    const summary = summarizeProvider('openai');

    expect(summary?.status).toBe('configured');
    expect(summary?.configured).toBe(true);
    expect(summary?.missingKeys).toEqual([]);
  });
});
