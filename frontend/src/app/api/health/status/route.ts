import { ok } from '@/server/codex/http';

export const runtime = 'nodejs';

type ApiHealth = 'online' | 'degraded' | 'offline';

function withTimeout<T>(promise: Promise<T>, timeoutMs = 2500): Promise<T> {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('timeout')), timeoutMs);
    promise
      .then((value) => {
        clearTimeout(timer);
        resolve(value);
      })
      .catch((error) => {
        clearTimeout(timer);
        reject(error);
      });
  });
}

export async function GET() {
  const base = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
  const healthUrl = `${base}/actuator/health`;

  try {
    const response = await withTimeout(
      fetch(healthUrl, { cache: 'no-store' }),
      2500
    );

    if (!response.ok) {
      return ok({ status: 'degraded' as ApiHealth, source: healthUrl, httpStatus: response.status });
    }

    const body = await response.json().catch(() => null) as { status?: string } | null;
    const backendStatus = body?.status?.toUpperCase();

    if (backendStatus === 'UP') {
      return ok({ status: 'online' as ApiHealth, source: healthUrl, httpStatus: response.status });
    }

    return ok({ status: 'degraded' as ApiHealth, source: healthUrl, httpStatus: response.status });
  } catch {
    return ok({ status: 'offline' as ApiHealth, source: healthUrl });
  }
}
