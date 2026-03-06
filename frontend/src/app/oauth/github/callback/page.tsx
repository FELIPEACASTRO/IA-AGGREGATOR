import { redirect } from 'next/navigation';

export const dynamic = 'force-dynamic';

export default async function GitHubOauthCallbackPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const read = (key: string) => {
    const value = params[key];
    return Array.isArray(value) ? value[0] : value;
  };

  const code = read('code');
  const error = read('error');
  const query = new URLSearchParams();
  if (code) query.set('code', code);
  if (error) query.set('error', error);
  const suffix = query.toString();
  redirect(`/api/oauth/github/callback${suffix ? `?${suffix}` : ''}`);
}

