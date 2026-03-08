import { cookies } from 'next/headers';

const DEFAULT_BACKEND_BASE_URL = 'http://localhost:8080';

function stripLeadingSlash(input: string) {
  return input.startsWith('/') ? input.slice(1) : input;
}

export function getBackendBaseUrl() {
  return process.env.NEXT_PUBLIC_API_URL || DEFAULT_BACKEND_BASE_URL;
}

export async function getBackendAccessToken() {
  try {
    const cookieStore = await cookies();
    return cookieStore.get('access_token')?.value ?? null;
  } catch {
    return null;
  }
}

export async function fetchBackend(path: string, init: RequestInit = {}) {
  const token = await getBackendAccessToken();
  const headers = new Headers(init.headers);

  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json');
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  return fetch(`${getBackendBaseUrl()}/${stripLeadingSlash(path)}`, {
    ...init,
    headers,
    cache: 'no-store',
  });
}

export async function relayBackendResponse(response: Response) {
  const body = await response.text();
  const headers = new Headers();
  ['content-type', 'cache-control'].forEach((headerName) => {
    const headerValue = response.headers.get(headerName);
    if (headerValue) {
      headers.set(headerName, headerValue);
    }
  });

  return new Response(body, {
    status: response.status,
    headers,
  });
}
