import { expect, test } from '@playwright/test';
import { createRandomUser, mockAuthApi } from './support/auth';

test('chat suporta prefill, troca de modelo e envio de mensagem', async ({ page }) => {
  const user = createRandomUser();
  await mockAuthApi(page, user, { authenticated: true });

  await page.goto('/chat');

  const composer = page.getByPlaceholder(/Como posso ajudar/i);
  await expect(composer).toBeVisible();

  await page.getByRole('button', { name: /Estrategia/i }).click();
  await expect(composer).toHaveValue(/Monte um plano de acao/i);

  await page.getByRole('button', { name: /GPT-4o Mini/i }).click();
  await page.getByRole('option', { name: /Claude 3.5 Haiku/i }).click();
  await expect(page.getByRole('button', { name: /Claude 3.5 Haiku/i })).toBeVisible();

  await composer.fill('Preciso de um plano de rollout com validacao de release.');
  await page.getByRole('button', { name: /Enviar mensagem/i }).click();

  await expect(page.getByText(/Preciso de um plano de rollout/i).first()).toBeVisible();
  await expect(page.getByText(/Resposta simulada para:/i).first()).toBeVisible();
  await expect(page.getByText(/Anthropic/i).first()).toBeVisible();
});

test('chat permite interromper geracao em streaming', async ({ page }) => {
  const user = createRandomUser();
  await mockAuthApi(page, user, { authenticated: true });

  await page.goto('/chat');

  const composer = page.getByPlaceholder(/Como posso ajudar/i);
  await composer.fill('stream test para validar interrupcao');
  await page.getByRole('button', { name: /Enviar mensagem/i }).click();

  const stopButton = page.getByRole('button', { name: /Parar geracao/i });
  await expect(stopButton).toBeVisible();
  await stopButton.click();

  await expect(page.getByRole('button', { name: /Enviar mensagem/i })).toBeVisible();
  await expect(page.getByText(/chunk-1/i).first()).toBeVisible();
});
