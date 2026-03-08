import { expect, test } from '@playwright/test';
import {
  createConversationFixture,
  createRandomUser,
  mockAuthApi,
} from './support/auth';

test('library filtra, fixa e reabre conversas', async ({ page }) => {
  const user = createRandomUser();
  const conversations = [
    createConversationFixture({
      id: 'conv-release',
      title: 'Plano de release',
      pinned: false,
      messages: [
        {
          id: 'msg-release',
          role: 'user',
          content: 'Preciso revisar riscos do release.',
          timestamp: Date.now(),
        },
      ],
    }),
    createConversationFixture({
      id: 'conv-growth',
      title: 'Estratégia de growth',
      pinned: true,
      messages: [
        {
          id: 'msg-growth',
          role: 'user',
          content: 'Mapeie os experimentos do trimestre.',
          timestamp: Date.now() - 1000,
        },
      ],
    }),
  ];

  await mockAuthApi(page, user, { authenticated: true, conversations });

  await page.goto('/library');
  await page.getByPlaceholder(/Buscar conversas/i).fill('release');
  await expect(page.getByText(/Plano de release/i)).toBeVisible();
  await expect(page.getByText(/Estratégia de growth/i)).toBeHidden();

  await page.getByRole('button', { name: /Fixar conversa/i }).click();
  await page.getByRole('button', { name: /^Fixadas/i }).click();
  await expect(page.getByText(/Plano de release/i)).toBeVisible();

  await page.getByRole('button', { name: /^Abrir$/i }).click();
  await expect(page).toHaveURL(/\/chat/);
  await expect(page.getByText(/Preciso revisar riscos do release/i)).toBeVisible();
});

test('prompts preenche o chat a partir de um template', async ({ page }) => {
  const user = createRandomUser();
  await mockAuthApi(page, user, { authenticated: true });

  await page.goto('/prompts');
  await page.getByPlaceholder(/Buscar templates/i).fill('plano');
  await page.getByRole('button', { name: /Planejamento/i }).click();
  await page.getByRole('button', { name: /Usar template/i }).first().click();

  await expect(page).toHaveURL(/\/chat\?prompt=/);
  await expect(page.getByPlaceholder(/Como posso ajudar/i)).toHaveValue(/Crie um plano de a/i);
});
