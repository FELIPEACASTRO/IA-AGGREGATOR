const BACKEND_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

/**
 * Server-side proxy for calling backend API endpoints.
 * Used from Next.js API routes and server components.
 */
export async function backendFetch<T>(
  path: string,
  options?: {
    method?: string;
    body?: unknown;
    token?: string;
    headers?: Record<string, string>;
  }
): Promise<T> {
  const url = `${BACKEND_URL}${path}`;
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options?.headers,
  };

  if (options?.token) {
    headers['Authorization'] = `Bearer ${options.token}`;
  }

  const response = await fetch(url, {
    method: options?.method || 'GET',
    headers,
    body: options?.body ? JSON.stringify(options.body) : undefined,
    cache: 'no-store',
  });

  if (!response.ok) {
    throw new Error(`Backend ${path} returned ${response.status}`);
  }

  return response.json() as Promise<T>;
}

/**
 * Fetch from backend with the ApiResponse wrapper format.
 * Extracts the `data` field from `{ success, data, message }`.
 */
export async function backendGet<T>(path: string, token?: string): Promise<T> {
  const result = await backendFetch<{ success: boolean; data: T }>(path, { token });
  return result.data;
}
