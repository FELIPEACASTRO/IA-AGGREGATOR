import { cookies } from 'next/headers';

export type CodexUserSession = {
  userId: string;
  email: string;
  name: string;
  accessToken: string;
};

type BackendUserProfile = {
  id?: string;
  email?: string;
  fullName?: string;
  avatarUrl?: string | null;
  role?: string;
  status?: string;
};

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  const parts = token.split('.');
  if (parts.length < 2) return null;

  try {
    const raw = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const payload = Buffer.from(raw, 'base64').toString('utf-8');
    return JSON.parse(payload) as Record<string, unknown>;
  } catch {
    return null;
  }
}

export async function getServerSession(): Promise<CodexUserSession | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get('access_token')?.value;
  if (!token) return null;

  const decoded = decodeJwtPayload(token);
  if (!decoded) return null;

  const userId = String(decoded.sub ?? '');
  const email = String(decoded.email ?? '');
  const name = String(decoded.name ?? decoded.email ?? 'Codex User');
  if (!userId || !email) return null;

  return {
    userId,
    email,
    name,
    accessToken: token,
  };
}

export async function getBackendUserProfile(accessToken: string): Promise<BackendUserProfile | null> {
  const backendUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

  try {
    const response = await fetch(`${backendUrl}/api/v1/auth/me`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
      cache: 'no-store',
    });

    if (!response.ok) {
      return null;
    }

    const payload = (await response.json()) as {
      data?: BackendUserProfile;
    };

    return payload.data ?? null;
  } catch {
    return null;
  }
}

