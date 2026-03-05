import { test, expect } from '@playwright/test';
import { createRandomUser, mockAuthApi, registerUserViaUi } from './support/auth';

test('analytics envia relatório e carrega histórico/detalhes', async ({ page }) => {
  const user = createRandomUser();
  await mockAuthApi(page, user);
  await registerUserViaUi(page, user);

  await page.goto('/settings');
  await expect(page.getByRole('heading', { name: 'Configurações' })).toBeVisible();
  await page.getByLabel('Nome de exibição').fill('QA Analytics E2E');
  await page.getByRole('button', { name: 'Salvar preferências' }).click();

  await page.route('**/api/v1/analytics/events', async (route) => {
    if (route.request().method() === 'POST') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true }),
      });
      return;
    }

    await route.continue();
  });

  await page.route('**/api/v1/analytics/reports**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: [
          {
            id: 'report-e2e-1',
            source: 'frontend',
            generatedAt: new Date().toISOString(),
            receivedAt: new Date().toISOString(),
            totalEvents: 3,
            counters: { auth_login_success: 1, settings_save_preferences: 1, chat_send_start: 1 },
          },
        ],
      }),
    });
  });

  await page.route('**/api/v1/analytics/reports/report-e2e-1/events**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: [
          {
            id: 'event-e2e-1',
            reportId: 'report-e2e-1',
            eventName: 'settings_save_preferences',
            eventCategory: 'system',
            eventTimestamp: new Date().toISOString(),
            createdAt: new Date().toISOString(),
            metadata: { locale: 'pt-BR' },
          },
        ],
      }),
    });
  });

  await page.getByRole('link', { name: 'Abrir diagnóstico de analytics' }).click();
  await expect(page).toHaveURL(/\/settings\/analytics/);

  await page.getByRole('button', { name: 'Atualizar' }).click();
  await page.getByRole('button', { name: 'Enviar relatório' }).click();
  await expect(page.getByRole('status').filter({ hasText: 'Relatório enviado' })).toBeVisible();

  await page.getByRole('button', { name: 'Carregar histórico' }).click();
  await expect(page.getByText('frontend · 3 evento(s)')).toBeVisible();

  await page.getByRole('button', { name: 'Ver detalhes' }).first().click();
  await expect(page.getByText('settings_save_preferences').first()).toBeVisible();
});
