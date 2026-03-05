import { test, expect } from '@playwright/test';
import { createRandomUser, mockAuthApi, registerUserViaUi } from './support/auth';

test('registro e navegação nas principais telas', async ({ page }) => {
  const user = createRandomUser();
  await mockAuthApi(page, user);
  await registerUserViaUi(page, user);

  await page.goto('/library');
  await expect(page).toHaveURL(/\/library/);
  await expect(page.getByRole('heading', { name: 'Biblioteca' })).toBeVisible();

  await page.goto('/prompts');
  await expect(page).toHaveURL(/\/prompts/);
  await expect(page.getByRole('heading', { name: 'Prompts' })).toBeVisible();

  await page.goto('/billing');
  await expect(page).toHaveURL(/\/billing/);
  await expect(page.getByRole('heading', { name: 'Plano' })).toBeVisible();

  await page.goto('/settings');
  await expect(page).toHaveURL(/\/settings/);
  await expect(page.getByRole('heading', { name: 'Configurações' })).toBeVisible();

  await page.getByRole('link', { name: 'Abrir diagnóstico de analytics' }).click();
  await expect(page).toHaveURL(/\/settings\/analytics/);
  await expect(page.getByRole('heading', { name: 'Analytics' })).toBeVisible();
});
