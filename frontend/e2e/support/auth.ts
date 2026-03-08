import { expect, type Page } from '@playwright/test';
import {
  type E2EUser,
  createRandomUser,
  installProductMocks,
  type MockConversation,
} from './product-mocks';

type MockAuthOptions = {
  authenticated?: boolean;
  conversations?: MockConversation[];
};

export { createRandomUser, installProductMocks };
export { createConversationFixture } from './product-mocks';
export type { E2EUser, MockConversation };

export async function mockAuthApi(page: Page, user: E2EUser, options: MockAuthOptions = {}) {
  await installProductMocks(page, {
    authenticated: options.authenticated ?? false,
    user,
    conversations: options.conversations,
  });
}

export async function registerUserViaUi(page: Page, user: E2EUser) {
  await loginUserViaUi(page, user);
}

export async function loginUserViaUi(page: Page, user: E2EUser) {
  await page.goto('/login');
  await page.waitForLoadState('networkidle');

  await page.getByLabel(/E-mail/i).fill(user.email);
  await page.getByLabel(/^Senha$/i).fill(user.password);
  await page.getByRole('button', { name: /Entrar/i }).click();

  await expect(page).toHaveURL(/\/codex/);
  await expect(page.getByRole('heading', { name: /Cloud Tasks/i })).toBeVisible();
}
