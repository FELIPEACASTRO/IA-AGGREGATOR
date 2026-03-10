import { createCipheriv, createHash, randomBytes } from 'node:crypto';
import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';
import { codexDb } from '@/server/codex/db';
import { getServerSession } from '@/server/codex/auth';
import { ensureWorkspaceForUser } from '@/server/codex/seed';

export const runtime = 'nodejs';

type GitHubTokenResponse = {
  access_token?: string;
  refresh_token?: string;
  scope?: string;
  token_type?: string;
  error?: string;
  error_description?: string;
};

type GitHubUserResponse = {
  id?: number;
  login?: string;
};

function deriveEncryptionKey(clientSecret: string) {
  const explicitKey = process.env.GITHUB_TOKEN_ENCRYPTION_KEY;
  if (explicitKey) {
    const raw = Buffer.from(explicitKey, 'base64');
    if (raw.length === 32) {
      return raw;
    }
  }

  return createHash('sha256').update(clientSecret, 'utf-8').digest();
}

function encryptSecret(value: string, key: Buffer) {
  const iv = randomBytes(12);
  const cipher = createCipheriv('aes-256-gcm', key, iv);
  const encrypted = Buffer.concat([cipher.update(value, 'utf-8'), cipher.final()]);
  const authTag = cipher.getAuthTag();
  return `enc:v1:${iv.toString('base64url')}:${authTag.toString('base64url')}:${encrypted.toString('base64url')}`;
}

async function exchangeCodeForToken(input: {
  code: string;
  clientId: string;
  clientSecret: string;
  redirectUri: string;
}) {
  const body = new URLSearchParams({
    client_id: input.clientId,
    client_secret: input.clientSecret,
    code: input.code,
    redirect_uri: input.redirectUri,
  });

  const response = await fetch('https://github.com/login/oauth/access_token', {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: body.toString(),
    cache: 'no-store',
  });

  const payload = (await response.json().catch(() => ({}))) as GitHubTokenResponse;
  if (!response.ok || payload.error || !payload.access_token) {
    throw new Error(payload.error_description || payload.error || 'token_exchange_failed');
  }
  return {
    accessToken: payload.access_token,
    refreshToken: payload.refresh_token,
    scope: payload.scope,
  };
}

async function fetchGitHubUser(accessToken: string) {
  const response = await fetch('https://api.github.com/user', {
    method: 'GET',
    headers: {
      Accept: 'application/vnd.github+json',
      Authorization: `Bearer ${accessToken}`,
      'X-GitHub-Api-Version': '2022-11-28',
      'User-Agent': 'ia-aggregator',
    },
    cache: 'no-store',
  });

  if (!response.ok) {
    throw new Error('github_user_fetch_failed');
  }

  return (await response.json()) as GitHubUserResponse;
}

export async function GET(request: Request) {
  const cookieStore = await cookies();
  const session = await getServerSession();
  if (!session) {
    return NextResponse.redirect(new URL('/login?error=session', request.url));
  }

  const workspace = (await ensureWorkspaceForUser(session)).workspace;
  const { searchParams } = new URL(request.url);
  const code = searchParams.get('code');
  const state = searchParams.get('state');
  const denied = searchParams.get('error');
  const storedState = cookieStore.get('github_oauth_state')?.value;

  if (denied) {
    const deniedResponse = NextResponse.redirect(new URL('/codex/settings/connectors?github=denied', request.url));
    deniedResponse.cookies.delete('github_oauth_state');
    return deniedResponse;
  }

  if (!state || !storedState || state !== storedState) {
    const invalidStateResponse = NextResponse.redirect(
      new URL('/codex/settings/connectors?github=invalid_state', request.url)
    );
    invalidStateResponse.cookies.delete('github_oauth_state');
    return invalidStateResponse;
  }

  if (!code) {
    const missingCodeResponse = NextResponse.redirect(
      new URL('/codex/settings/connectors?github=missing_code', request.url)
    );
    missingCodeResponse.cookies.delete('github_oauth_state');
    return missingCodeResponse;
  }

  const clientId = process.env.GITHUB_CLIENT_ID;
  const clientSecret = process.env.GITHUB_CLIENT_SECRET;
  const redirectUri = process.env.GITHUB_OAUTH_REDIRECT_URI || 'http://localhost:3001/oauth/github/callback';
  if (!clientId || !clientSecret) {
    const notConfiguredResponse = NextResponse.redirect(
      new URL('/codex/settings/connectors?github=not_configured', request.url)
    );
    notConfiguredResponse.cookies.delete('github_oauth_state');
    return notConfiguredResponse;
  }

  try {
    const tokenPayload = await exchangeCodeForToken({
      code,
      clientId,
      clientSecret,
      redirectUri,
    });
    const githubUser = await fetchGitHubUser(tokenPayload.accessToken);

    const accountLogin = githubUser.login || session.email.split('@')[0];
    const installationExternalId = githubUser.id ? String(githubUser.id) : `user:${accountLogin}`;
    const externalId = githubUser.id ? String(githubUser.id) : accountLogin;
    const scopes = tokenPayload.scope
      ? tokenPayload.scope
          .split(',')
          .map((scope) => scope.trim())
          .filter(Boolean)
      : ['repo', 'read:user', 'user:email'];

    const encryptionKey = deriveEncryptionKey(clientSecret);
    const encryptedAccessToken = encryptSecret(tokenPayload.accessToken, encryptionKey);
    const encryptedRefreshToken = tokenPayload.refreshToken
      ? encryptSecret(tokenPayload.refreshToken, encryptionKey)
      : null;

    await codexDb.gitHubInstallation.upsert({
      where: { installationExternalId },
      update: {
        workspaceId: workspace.id,
        accountLogin,
        status: 'CONNECTED',
      },
      create: {
        workspaceId: workspace.id,
        accountLogin,
        installationExternalId,
        status: 'CONNECTED',
      },
    });

    await codexDb.oAuthConnection.upsert({
      where: {
        provider_externalId: {
          provider: 'GITHUB',
          externalId,
        },
      },
      update: {
        status: 'CONNECTED',
        scopes,
        accessToken: encryptedAccessToken,
        refreshToken: encryptedRefreshToken,
      },
      create: {
        userId: session.userId,
        provider: 'GITHUB',
        externalId,
        scopes,
        status: 'CONNECTED',
        accessToken: encryptedAccessToken,
        refreshToken: encryptedRefreshToken,
      },
    });

    const successResponse = NextResponse.redirect(new URL('/codex/settings/connectors?github=connected', request.url));
    successResponse.cookies.delete('github_oauth_state');
    return successResponse;
  } catch {
    const errorResponse = NextResponse.redirect(new URL('/codex/settings/connectors?github=token_error', request.url));
    errorResponse.cookies.delete('github_oauth_state');
    return errorResponse;
  }
}
