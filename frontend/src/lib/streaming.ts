/**
 * SSE streaming utility for real-time AI chat responses.
 * Connects to the backend streaming endpoint and yields tokens as they arrive.
 */
const STREAM_API_PATH = '/api/ai/chat/stream';

export interface StreamCallbacks {
  onToken: (token: string) => void;
  onModelInfo: (info: { modelUsed: string; providerUsed: string }) => void;
  onDone: () => void;
  onError: (error: string) => void;
}

/**
 * Opens a streaming SSE connection to the AI chat endpoint.
 * Falls back to non-streaming if SSE fails.
 */
export async function streamChat(
  prompt: string,
  model: string,
  signal: AbortSignal,
  callbacks: StreamCallbacks
): Promise<void> {
  const response = await fetch(STREAM_API_PATH, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
    body: JSON.stringify({ prompt, preferredModel: model }),
    signal,
  });

  if (!response.ok) {
    const errorBody = await response.text().catch(() => 'Unknown error');
    throw new Error(`Stream request failed (${response.status}): ${errorBody}`);
  }

  const contentType = response.headers.get('content-type') || '';

  if (contentType.includes('text/event-stream')) {
    await readSSEStream(response, callbacks);
  } else if (contentType.includes('application/json')) {
    // Fallback: backend returned a non-streaming JSON response
    const data = await response.json();
    const content = data?.data?.content || data?.content || '';
    callbacks.onModelInfo({
      modelUsed: data?.data?.modelUsed || data?.modelUsed || model,
      providerUsed: data?.data?.providerUsed || data?.providerUsed || 'unknown',
    });
    callbacks.onToken(content);
    callbacks.onDone();
  } else {
    // Fallback: treat as plain text stream
    await readTextStream(response, callbacks);
  }
}

async function readSSEStream(response: Response, callbacks: StreamCallbacks): Promise<void> {
  const reader = response.body?.getReader();
  if (!reader) {
    throw new Error('Response body is not readable');
  }

  const decoder = new TextDecoder();
  let buffer = '';

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      for (const line of lines) {
        const trimmed = line.trim();
        if (!trimmed || trimmed.startsWith(':')) continue;

        if (trimmed.startsWith('data:')) {
          const payload = trimmed.slice(5).trim();

          if (payload === '[DONE]') {
            callbacks.onDone();
            return;
          }

          try {
            const parsed = JSON.parse(payload);
            if (parsed.modelUsed || parsed.providerUsed) {
              callbacks.onModelInfo({
                modelUsed: parsed.modelUsed || '',
                providerUsed: parsed.providerUsed || '',
              });
            }
            if (parsed.content || parsed.token || parsed.text) {
              callbacks.onToken(parsed.content || parsed.token || parsed.text);
            }
          } catch {
            // Not JSON — treat as raw text token
            callbacks.onToken(payload);
          }
        }
      }
    }
  } finally {
    reader.releaseLock();
  }

  callbacks.onDone();
}

async function readTextStream(response: Response, callbacks: StreamCallbacks): Promise<void> {
  const reader = response.body?.getReader();
  if (!reader) {
    throw new Error('Response body is not readable');
  }

  const decoder = new TextDecoder();

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      const text = decoder.decode(value, { stream: true });
      if (text) callbacks.onToken(text);
    }
  } finally {
    reader.releaseLock();
  }

  callbacks.onDone();
}
