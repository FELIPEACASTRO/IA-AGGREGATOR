import { fail, ok, requireCodexContext } from '@/server/codex/http';

export const runtime = 'nodejs';

export async function POST() {
  const context = await requireCodexContext();
  if ('error' in context) return context.error;

  const clientId = process.env.GITHUB_CLIENT_ID;
  const redirectUri =
    process.env.GITHUB_OAUTH_REDIRECT_URI || 'http://localhost:3001/oauth/github/callback';
  if (!clientId) {
    return fail('GITHUB_CLIENT_ID nao configurado', 400);
  }

  const url =
    `https://github.com/login/oauth/authorize?client_id=${encodeURIComponent(clientId)}` +
    `&redirect_uri=${encodeURIComponent(redirectUri)}` +
    '&scope=repo,read:user,user:email';

  return ok({
    authorizeUrl: url,
  });
}

