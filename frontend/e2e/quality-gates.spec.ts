import { expect, type Page, test } from '@playwright/test';
import { createRandomUser, installProductMocks, loginUserViaUi } from './support/auth';

function attachConsoleCollector(page: Page) {
  const issues: string[] = [];

  page.on('console', (message) => {
    if (message.type() !== 'error') return;
    const text = message.text();
    if (text.includes('favicon.ico')) return;
    issues.push(`console:${text}`);
  });

  page.on('pageerror', (error) => {
    issues.push(`pageerror:${error.message}`);
  });

  return {
    assertClean: async () => {
      await page.waitForTimeout(250);
      expect(issues, 'console errors should be empty').toEqual([]);
      issues.length = 0;
    },
  };
}

test.describe('Quality gates', () => {
  test('visual baseline desktop', async ({ page }) => {
    const user = createRandomUser();
    const consoleCollector = attachConsoleCollector(page);

    await installProductMocks(page, { authenticated: false, user });
    await page.setViewportSize({ width: 1440, height: 1024 });

    await page.goto('/login');
    await expect(page.getByRole('heading', { name: /Entrar no Lume/i })).toBeVisible();
    await expect(page).toHaveScreenshot('login-desktop.png', { fullPage: true });

    await loginUserViaUi(page, user);
    await expect(page).toHaveScreenshot('codex-desktop.png', { fullPage: true });
    await consoleCollector.assertClean();

    await page.goto('/chat');
    await expect(page.getByPlaceholder(/Como posso ajudar/i)).toBeVisible();
    await expect(page).toHaveScreenshot('chat-desktop.png', { fullPage: true });
    await consoleCollector.assertClean();

    await page.goto('/settings/analytics');
    await expect(page.getByText(/Participa/).first()).toBeVisible();
    await expect(page).toHaveScreenshot('analytics-desktop.png', { fullPage: true });
    await consoleCollector.assertClean();
  });

  test('keyboard smoke on login and command palette', async ({ page }) => {
    const user = createRandomUser();

    await installProductMocks(page, { authenticated: false, user });
    await page.goto('/login');

    const emailField = page.getByLabel(/E-mail/i);
    const passwordField = page.getByLabel(/^Senha$/i);

    await page.keyboard.press('Tab');
    await expect(emailField).toBeFocused();

    await emailField.press('Tab');
    await expect(passwordField).toBeFocused();

    await emailField.fill(user.email);
    await passwordField.fill(user.password);
    await passwordField.press('Enter');
    await expect(page).toHaveURL(/\/codex/);
    await expect(page.getByRole('heading', { name: /Cloud Tasks/i })).toBeVisible();

    await page.keyboard.press('Control+K');
    await expect(page.getByPlaceholder(/Buscar ou executar uma acao/i)).toBeVisible();

    await page.keyboard.type('usage');
    await expect(page.getByText(/Acoes rapidas/i)).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page.getByPlaceholder(/Buscar ou executar uma acao/i)).toBeHidden();
  });
});
