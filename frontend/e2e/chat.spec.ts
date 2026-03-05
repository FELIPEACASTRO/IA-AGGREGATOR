import { test, expect } from '@playwright/test';
import { createRandomUser, mockAuthApi, registerUserViaUi } from './support/auth';

test('chat envia prompt e renderiza resposta do assistente', async ({ page }) => {
  const user = createRandomUser();
  await mockAuthApi(page, user);
  await registerUserViaUi(page, user);

  await page.route('**/api/v1/ai/chat', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          content: 'Resposta E2E simulada com sucesso.',
          modelUsed: 'gpt-4o-mini',
          providerUsed: 'OpenAI',
          fallbackUsed: false,
          attempts: 1,
        },
      }),
    });
  });

  await page.getByPlaceholder('Digite sua mensagem... (Enter envia, Shift+Enter quebra linha)').fill('Olá, teste E2E.');
  await page.getByRole('button', { name: /Enviar mensagem|Enviar/i }).click();

  await expect(page.getByText('Olá, teste E2E.').first()).toBeVisible();
  await expect(page.getByText('Resposta E2E simulada com sucesso.').first()).toBeVisible();
  await expect(page.getByText('OpenAI').first()).toBeVisible();
});
