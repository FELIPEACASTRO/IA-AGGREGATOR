import { cookies } from 'next/headers';
import { backendGet } from '@/server/backend-proxy';

type CodexUserSession = {
  userId: string;
  email: string;
  name: string;
  role?: string;
  status?: string;
};

type BackendCurrentUser = {
  id: string;
  email: string;
  fullName?: string | null;
  role?: string | null;
  status?: string | null;
};

export async function getServerSession(): Promise<CodexUserSession | null> {
  const cookieStore = await cookies();
  const token = cookieStore.get('access_token')?.value;
  if (!token) return null;

  try {
    const currentUser = await backendGet<BackendCurrentUser>('/api/v1/auth/me', token);
    if (!currentUser?.id || !currentUser?.email) return null;

    return {
      userId: String(currentUser.id),
      email: currentUser.email,
      name: currentUser.fullName || currentUser.email,
      role: currentUser.role ?? undefined,
      status: currentUser.status ?? undefined,
    };
  } catch {
    return null;
  }
}

