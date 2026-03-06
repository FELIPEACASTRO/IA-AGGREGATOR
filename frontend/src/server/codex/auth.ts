import { cookies } from 'next/headers';

type CodexUserSession = {
  userId: string;
  email: string;
  name: string;
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
  };
}

