import { expect, test } from '@playwright/test';
import {
  createRandomUser,
  createConversationFixture,
  loginUserViaUi,
  mockAuthApi,
} from './support/auth';

test('login e navegacao principal nas superficies consumer', async ({ page }) => {
  const user = createRandomUser();
  const seededConversation = createConversationFixture({
    title: 'Roadmap de release',
    pinned: true,
    messages: [
      {
        id: 'msg-1',
        role: 'user',
        content: 'Preciso validar o release da semana.',
        timestamp: Date.now(),
      },
    ],
  });

  await mockAuthApi(page, user, { authenticated: false, conversations: [seededConversation] });
  await loginUserViaUi(page, user);

  await page.goto('/chat');
  await expect(page.getByRole('link', { name: 'Chat' })).toBeVisible();
  await expect(page.getByText(/Roadmap de release/i)).toBeVisible();

  await page.keyboard.press('Control+K');
  await page.getByPlaceholder(/Buscar ou executar uma acao/i).fill('billing');
  await page.getByRole('button', { name: /Abrir Billing/i }).click();
  await expect(page).toHaveURL(/\/billing/);
  await expect(page.getByRole('heading', { name: /Plano|Billing/i })).toBeVisible();

  await page.goto('/library');
  await expect(page).toHaveURL(/\/library/);
  await expect(page.getByRole('heading', { name: /Biblioteca/i })).toBeVisible();

  await page.goto('/prompts');
  await expect(page).toHaveURL(/\/prompts/);
  await expect(page.getByRole('heading', { name: /Templates/i })).toBeVisible();

  await page.goto('/settings');
  await expect(page).toHaveURL(/\/settings/);
  await expect(page.getByRole('heading', { name: /Configuracoes/i })).toBeVisible();

  await page.getByRole('link', { name: /Abrir insights/i }).click();
  await expect(page).toHaveURL(/\/settings\/analytics/);
  await expect(page.getByRole('heading', { name: /Analytics/i })).toBeVisible();
});
