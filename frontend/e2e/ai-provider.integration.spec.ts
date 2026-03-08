import { expect, test } from '@playwright/test';
import { loginUserViaUi } from './support/auth';

const runRealAi = process.env.RUN_REAL_AI_TESTS === 'true';
const model = process.env.E2E_AI_MODEL || 'gpt-4o-mini';
const email = process.env.E2E_RELEASE_EMAIL;
const password = process.env.E2E_RELEASE_PASSWORD;

test.skip(
  !runRealAi || !email || !password,
  'Defina RUN_REAL_AI_TESTS=true, E2E_RELEASE_EMAIL e E2E_RELEASE_PASSWORD para executar integracao real de IA.'
);

test('integracao real de IA retorna resposta no chat', async ({ page }) => {
  await loginUserViaUi(page, { email: email!, password: password!, fullName: 'Release User', workspaceName: 'Release Workspace' });

  await page.goto('/chat');
  await page.getByRole('button', { name: /GPT-4o Mini/i }).click();
  await page.getByRole('option', { name: new RegExp(model, 'i') }).click();

  await page.getByPlaceholder(/Como posso ajudar|Responder/i).fill(
    'Responda em uma frase curta em portugues explicando o que e fallback de modelos.'
  );

  const responsePromise = page.waitForResponse((response) => {
    return response.url().includes('/api/v1/ai/chat') && response.request().method() === 'POST';
  });

  await page.getByRole('button', { name: /Enviar mensagem/i }).click();

  const chatResponse = await responsePromise;
  expect(chatResponse.status()).toBe(200);

  await expect(
    page.getByText('Responda em uma frase curta em portugues explicando o que e fallback de modelos.')
  ).toBeVisible();
});
