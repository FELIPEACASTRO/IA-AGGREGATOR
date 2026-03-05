import { expect, type Page } from '@playwright/test';

export type E2EUser = {
  fullName: string;
  email: string;
  password: string;
};

export const createRandomUser = (): E2EUser => {
  const suffix = `${Date.now()}${Math.floor(Math.random() * 10000)}`;
  return {
    fullName: `E2E User ${suffix}`,
    email: `e2e.${suffix}@example.com`,
    password: 'E2ePass!2345',
  };
};

export const mockAuthApi = async (page: Page, user: E2EUser) => {
  await page.route('**/api/v1/auth/register', async (route) => {
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          accessToken: 'mock-access-token',
          refreshToken: 'mock-refresh-token',
          tokenType: 'Bearer',
          expiresIn: 900,
        },
      }),
    });
  });

  await page.route('**/api/v1/auth/login', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          accessToken: 'mock-access-token',
          refreshToken: 'mock-refresh-token',
          tokenType: 'Bearer',
          expiresIn: 900,
        },
      }),
    });
  });

  await page.route('**/api/v1/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          id: 'user-e2e-1',
          email: user.email,
          fullName: user.fullName,
          role: 'USER',
          status: 'ACTIVE',
        },
      }),
    });
  });
};

export const registerUserViaUi = async (page: Page, user: E2EUser) => {
  await page.goto('/register');
  await page.waitForLoadState('networkidle');

  const fillForm = async () => {
    await page.getByLabel('Nome Completo').fill(user.fullName);
    await page.getByLabel('Email').fill(user.email);
    await page.getByLabel('Senha', { exact: true }).fill(user.password);
    await page.getByLabel('Confirmar Senha').fill(user.password);
  };

  await fillForm();

  const submitButton = page.getByRole('button', { name: 'Criar Conta' });
  for (let attempt = 0; attempt < 2; attempt += 1) {
    try {
      await submitButton.click({ timeout: 15_000, force: true });
    } catch {
      await page.getByLabel('Confirmar Senha').press('Enter');
    }

    await page.waitForTimeout(250);
    if (!page.url().includes('/register?')) {
      break;
    }

    await page.goto('/register');
    await page.waitForLoadState('networkidle');
    await fillForm();
  }

  await expect(page).toHaveURL(/\/(chat|welcome)/);

  if (page.url().includes('/welcome')) {
    const skipButton = page.getByRole('button', { name: /Pular configuração/i });
    if (await skipButton.isVisible()) {
      await skipButton.click();
      await expect(page).toHaveURL(/\/chat/);
    }
  }
};

export const loginUserViaUi = async (page: Page, user: E2EUser) => {
  await page.goto('/login');

  await page.getByLabel('Email').fill(user.email);
  await page.getByLabel('Senha', { exact: true }).fill(user.password);

  await page.getByRole('button', { name: 'Entrar' }).click();

  await expect(page).toHaveURL(/\/chat/);
  await expect(page.getByRole('heading', { name: 'Chat IA' })).toBeVisible();
};
