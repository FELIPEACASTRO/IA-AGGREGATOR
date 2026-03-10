import { codexDb } from '@/server/codex/db';
import { fail, ok } from '@/server/codex/http';
import { verifyGitHubWebhookSignature } from '@/server/codex/webhook-signature';

export const runtime = 'nodejs';

type GitHubWebhookPayload = {
  repository?: {
    full_name?: string;
  };
  comment?: {
    body?: string;
  };
};

export async function POST(request: Request) {
  const rawBody = await request.text();
  const signature = request.headers.get('x-hub-signature-256');
  const isValidSignature = verifyGitHubWebhookSignature({
    payload: rawBody,
    signatureHeader: signature,
    secret: process.env.GITHUB_WEBHOOK_SECRET,
  });
  if (!isValidSignature) {
    return fail('Assinatura GitHub invalida', 401);
  }

  let body: GitHubWebhookPayload = {};
  if (rawBody) {
    try {
      body = (JSON.parse(rawBody) as GitHubWebhookPayload) ?? {};
    } catch {
      return fail('Payload GitHub invalido', 400);
    }
  }
  const event = request.headers.get('x-github-event') || 'unknown';
  const workspace = await codexDb.workspace.findFirst({
    orderBy: { createdAt: 'asc' },
  });
  if (!workspace) return ok({ received: true, ignored: true });

  await codexDb.auditLog.create({
    data: {
      workspaceId: workspace.id,
      action: 'github.webhook.received',
      targetType: 'webhook',
      metadata: {
        event,
      },
    },
  });

  if (event === 'pull_request_review_comment' || event === 'issue_comment') {
    const repositoryFullName = body?.repository?.full_name as string | undefined;
    const repository = repositoryFullName
      ? await codexDb.gitRepository.findFirst({
          where: {
            workspaceId: workspace.id,
            fullName: repositoryFullName,
          },
        })
      : null;

    const reviewRun = await codexDb.gitHubReviewRun.create({
      data: {
        workspaceId: workspace.id,
        repositoryId: repository?.id,
        source: 'github-webhook',
        status: 'received',
        summary: `Webhook ${event} recebido`,
      },
    });

    if (body?.comment?.body && String(body.comment.body).toLowerCase().includes('review')) {
      await codexDb.reviewFinding.create({
        data: {
          reviewRunId: reviewRun.id,
          title: 'Revisao solicitada via comentario',
          body: 'Solicitacao de review recebida. Pipeline de review deve ser executado.',
          severity: 'P2',
        },
      });
    }
  }

  return ok({ received: true, event });
}

