import { test, expect } from '@playwright/test';
import { createRandomUser, registerUserViaUi } from './support/auth';

const runRealAi = process.env.E2E_REAL_AI === 'true';
const model = process.env.E2E_AI_MODEL || 'gpt-4o-mini';

test.skip(!runRealAi, 'Defina E2E_REAL_AI=true e credenciais dos providers para executar integração real de IA.');

test('integração real de IA retorna resposta no chat', async ({ page }) => {
  const user = createRandomUser();
  await registerUserViaUi(page, user);

  await page.getByLabel('Selecionar modelo').selectOption(model);

  await page.getByPlaceholder('Digite sua mensagem... (Enter envia, Shift+Enter quebra linha)').fill(
    'Responda em uma frase curta em português explicando o que é fallback de modelos.'
  );

  const responsePromise = page.waitForResponse((response) => {
    return response.url().includes('/api/v1/ai/chat') && response.request().method() === 'POST';
  });

  await page.getByRole('button', { name: /Enviar mensagem|Enviar/i }).click();

  const chatResponse = await responsePromise;
  expect(chatResponse.status()).toBe(200);

  await expect(page.getByText('Responda em uma frase curta em português explicando o que é fallback de modelos.')).toBeVisible();

  const copyButtons = page.getByRole('button', { name: 'Copiar' });
  await expect(copyButtons.nth(1)).toBeVisible();
});
