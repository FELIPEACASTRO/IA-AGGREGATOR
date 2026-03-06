import { NextResponse } from 'next/server';
import { codexDb } from '@/server/codex/db';
import { getServerSession } from '@/server/codex/auth';
import { ensureWorkspaceForUser } from '@/server/codex/seed';

export const runtime = 'nodejs';

export async function GET(request: Request) {
  const session = await getServerSession();
  if (!session) {
    return NextResponse.redirect(new URL('/login?error=session', request.url));
  }

  const workspace = (await ensureWorkspaceForUser(session)).workspace;
  const { searchParams } = new URL(request.url);
  const code = searchParams.get('code');
  const denied = searchParams.get('error');

  if (denied) {
    return NextResponse.redirect(new URL(`/codex/settings/connectors?github=denied`, request.url));
  }

  const installationExternalId = code ? `oauth-${code.slice(0, 8)}` : `manual-${Date.now()}`;
  await codexDb.gitHubInstallation.upsert({
    where: { installationExternalId },
    update: {
      workspaceId: workspace.id,
      accountLogin: session.email.split('@')[0],
      status: 'CONNECTED',
    },
    create: {
      workspaceId: workspace.id,
      accountLogin: session.email.split('@')[0],
      installationExternalId,
      status: 'CONNECTED',
    },
  });

  await codexDb.oAuthConnection.upsert({
    where: {
      provider_externalId: {
        provider: 'GITHUB',
        externalId: session.email,
      },
    },
    update: {
      status: 'CONNECTED',
      scopes: ['repo', 'read:user', 'user:email'],
      accessToken: code ? `token-from-${code.slice(0, 8)}` : null,
    },
    create: {
      userId: session.userId,
      provider: 'GITHUB',
      externalId: session.email,
      scopes: ['repo', 'read:user', 'user:email'],
      status: 'CONNECTED',
      accessToken: code ? `token-from-${code.slice(0, 8)}` : null,
    },
  });

  return NextResponse.redirect(new URL('/codex/settings/connectors?github=connected', request.url));
}
