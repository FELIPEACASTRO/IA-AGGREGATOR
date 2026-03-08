import { expect, test } from '@playwright/test';
import { createRandomUser, mockAuthApi } from './support/auth';

test('analytics atualiza metricas, envia relatorio e abre detalhes tecnicos', async ({ page }) => {
  const user = createRandomUser();
  await mockAuthApi(page, user, { authenticated: true });

  await page.goto('/settings');
  await expect(page.getByRole('heading', { name: /Configuracoes/i })).toBeVisible();

  await page.getByLabel(/Nome de exibicao/i).fill('QA Analytics E2E');
  await page.getByRole('button', { name: /Salvar preferencias/i }).click();

  await page.getByRole('link', { name: /Abrir diagnostico/i }).click();
  await expect(page).toHaveURL(/\/settings\/analytics\/debug/);

  await page.getByRole('button', { name: /^Atualizar$/i }).click();
  await page.getByRole('button', { name: /Enviar relat.o/i }).click();
  await expect(page.getByText(/Relat.rio enviado/i)).toBeVisible();

  await page.getByRole('button', { name: /Carregar hist.rico/i }).click();
  await expect(page.getByText(/frontend-web/i)).toBeVisible();

  await page.getByRole('button', { name: /Ver detalhes/i }).first().click();
  await expect(page.getByText(/settings_save_preferences/i).first()).toBeVisible();
});
