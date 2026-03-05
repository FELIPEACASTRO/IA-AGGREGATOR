import { test, expect } from '@playwright/test';
import { createRandomUser } from './support/auth';

test('analytics persiste filtros e permite resetar para padrão', async ({ page }) => {
  const user = createRandomUser();

  await page.addInitScript(() => {
    localStorage.setItem('access_token', 'mock-access-token');
    localStorage.setItem('refresh_token', 'mock-refresh-token');
    localStorage.setItem('ia-onboarding-done', '1');
  });

  await page.context().addCookies([
    { name: 'access_token', value: 'mock-access-token', url: 'http://localhost:3001' },
    { name: 'refresh_token', value: 'mock-refresh-token', url: 'http://localhost:3001' },
  ]);

  await page.route('**/api/v1/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          id: 'user-e2e-analytics',
          email: user.email,
          fullName: user.fullName,
          role: 'USER',
          status: 'ACTIVE',
        },
      }),
    });
  });

  await page.goto('/settings/analytics');
  await expect(page).toHaveURL(/\/settings\/analytics/);
  await expect(page.getByRole('heading', { name: 'Analytics' })).toBeVisible();

  const fromDate = page.getByLabel('Data inicial do histórico');
  const toDate = page.getByLabel('Data final do histórico');
  const sortBy = page.getByLabel('Ordenar histórico por');
  const sortDir = page.getByLabel('Direção da ordenação do histórico');
  const cohortWindow = page.getByLabel('Janela de coorte');
  const periodWindow = page.getByLabel('Janela da comparação temporal');

  await fromDate.fill('2026-02-01');
  await toDate.fill('2026-02-28');
  await sortBy.selectOption('source');
  await sortDir.selectOption('asc');
  await cohortWindow.selectOption('8w');
  await periodWindow.selectOption('14');

  await expect(page.getByText(/filtro\(s\) ativo\(s\)/i)).toBeVisible();

  await page.reload();
  await expect(page).toHaveURL(/\/settings\/analytics/);

  await expect(fromDate).toHaveValue('2026-02-01');
  await expect(toDate).toHaveValue('2026-02-28');
  await expect(sortBy).toHaveValue('source');
  await expect(sortDir).toHaveValue('asc');
  await expect(cohortWindow).toHaveValue('8w');
  await expect(periodWindow).toHaveValue('14');

  const resetButton = page.getByRole('button', { name: 'Resetar filtros' });
  await resetButton.click();

  await expect(fromDate).toHaveValue('');
  await expect(toDate).toHaveValue('');
  await expect(sortBy).toHaveValue('receivedAt');
  await expect(sortDir).toHaveValue('desc');
  await expect(cohortWindow).toHaveValue('4w');
  await expect(periodWindow).toHaveValue('7');
  await expect(resetButton).toBeDisabled();
});
