import { expect, test } from '@playwright/test';

const releaseEmail = process.env.E2E_RELEASE_EMAIL;
const releasePassword = process.env.E2E_RELEASE_PASSWORD;

test.skip(!releaseEmail || !releasePassword, 'Defina E2E_RELEASE_EMAIL e E2E_RELEASE_PASSWORD para executar a suite real de release.');

test('login real cria sessao e expõe contexto no BFF', async ({ page }) => {
  await page.goto('/login');

  await page.getByLabel(/E-mail/i).fill(releaseEmail!);
  await page.getByLabel(/^Senha$/i).fill(releasePassword!);
  await page.getByRole('button', { name: /Entrar/i }).click();

  await expect(page).toHaveURL(/\/codex/);
  await expect(page.getByRole('heading', { name: /Cloud Tasks/i })).toBeVisible();

  const sessionPayload = await page.evaluate(async () => {
    const response = await fetch('/api/v1/session', {
      credentials: 'include',
      cache: 'no-store',
    });

    return {
      status: response.status,
      body: await response.json(),
    };
  });

  expect(sessionPayload.status).toBe(200);
  expect(sessionPayload.body?.success).toBe(true);
  expect(sessionPayload.body?.data?.user?.email).toBe(releaseEmail);
  expect(sessionPayload.body?.data?.currentWorkspace?.id).toBeTruthy();
});
