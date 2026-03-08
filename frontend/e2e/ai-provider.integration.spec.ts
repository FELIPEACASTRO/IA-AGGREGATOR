import { expect, test } from '@playwright/test';
import { createRandomUser, loginUserViaUi, mockAuthApi } from './support/auth';

const runRealAi = process.env.E2E_REAL_AI === 'true';
const model = process.env.E2E_AI_MODEL || 'gpt-4o-mini';

test.skip(!runRealAi, 'Defina E2E_REAL_AI=true e credenciais dos providers para executar integracao real de IA.');

test('integracao real de IA retorna resposta no chat', async ({ page }) => {
  const user = createRandomUser();
  await mockAuthApi(page, user, { authenticated: false });
  await loginUserViaUi(page, user);

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
