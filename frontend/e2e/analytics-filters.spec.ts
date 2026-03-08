import { expect, test } from '@playwright/test';
import { createRandomUser, mockAuthApi } from './support/auth';

test('analytics diagnostico persiste filtros e permite resetar para padrao', async ({ page }) => {
  const user = createRandomUser();
  await mockAuthApi(page, user, { authenticated: true });

  await page.goto('/settings/analytics/debug');
  await expect(page.getByRole('heading', { name: /Analytics/i })).toBeVisible();

  const fromDate = page.getByLabel(/Data inicial do hist/);
  const toDate = page.getByLabel(/Data final do hist/);
  const sortBy = page.getByLabel(/Ordenar hist/);
  const sortDir = page.getByLabel(/Dire..o da ordena..o do hist/);
  const cohortWindow = page.getByLabel(/Janela de coorte/i);
  const periodWindow = page.getByLabel(/Janela da compara..o temporal/i);

  await fromDate.fill('2026-02-01');
  await toDate.fill('2026-02-28');
  await sortBy.selectOption('source');
  await sortDir.selectOption('asc');
  await cohortWindow.selectOption('8w');
  await periodWindow.selectOption('14');

  await expect(page.getByText(/Filtros ativos/i)).toBeVisible();

  await page.reload();
  await expect(fromDate).toHaveValue('2026-02-01');
  await expect(toDate).toHaveValue('2026-02-28');
  await expect(sortBy).toHaveValue('source');
  await expect(sortDir).toHaveValue('asc');
  await expect(cohortWindow).toHaveValue('8w');
  await expect(periodWindow).toHaveValue('14');

  const resetButton = page.getByRole('button', { name: /Resetar filtros/i });
  await resetButton.click();

  await expect(fromDate).toHaveValue('');
  await expect(toDate).toHaveValue('');
  await expect(sortBy).toHaveValue('receivedAt');
  await expect(sortDir).toHaveValue('desc');
  await expect(cohortWindow).toHaveValue('4w');
  await expect(periodWindow).toHaveValue('7');
  await expect(resetButton).toBeDisabled();
});
