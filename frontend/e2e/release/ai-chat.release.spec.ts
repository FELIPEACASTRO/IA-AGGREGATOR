import { expect, test } from '@playwright/test';
import { loginUserViaUi } from '../support/auth';

const runRealAi = process.env.RUN_REAL_AI_TESTS === 'true';
const email = process.env.E2E_RELEASE_EMAIL;
const password = process.env.E2E_RELEASE_PASSWORD;
const model = process.env.E2E_AI_MODEL || 'gpt-4o-mini';

test.skip(
  !runRealAi || !email || !password,
  'Defina RUN_REAL_AI_TESTS=true, E2E_RELEASE_EMAIL e E2E_RELEASE_PASSWORD para executar o fluxo real de IA.'
);

test('chat real responde via backend canonico', async ({ page }) => {
  await loginUserViaUi(page, {
    email: email!,
    password: password!,
    fullName: 'Release User',
    workspaceName: 'Release Workspace',
  });

  await page.goto('/chat');

  const modelButton = page.getByRole('button', { name: new RegExp(model, 'i') });
  if (await modelButton.isVisible().catch(() => false)) {
    await modelButton.click();
    const modelOption = page.getByRole('option', { name: new RegExp(model, 'i') });
    if (await modelOption.isVisible().catch(() => false)) {
      await modelOption.click();
    } else {
      await page.keyboard.press('Escape');
    }
  }

  const composer = page.getByPlaceholder(/Como posso ajudar|Responder/i);
  await composer.fill('Responda em uma frase curta explicando o que e fallback de provedores.');

  const responsePromise = page.waitForResponse((response) => {
    return response.url().includes('/api/v1/ai/chat') && response.request().method() === 'POST';
  });

  await page.getByRole('button', { name: /Enviar mensagem/i }).click();

  const response = await responsePromise;
  expect(response.status()).toBe(200);

  const body = (await response.json()) as {
    success?: boolean;
    data?: {
      content?: string;
      requestId?: string;
      providerUsed?: string;
      modelUsed?: string;
    };
  };

  expect(body.success).toBe(true);
  expect(body.data?.requestId).toBeTruthy();
  expect(body.data?.providerUsed).toBeTruthy();
  expect(body.data?.modelUsed).toBeTruthy();
  expect(body.data?.content?.trim().length).toBeGreaterThan(0);

  await expect(
    page.getByText('Responda em uma frase curta explicando o que e fallback de provedores.').first()
  ).toBeVisible();
});
