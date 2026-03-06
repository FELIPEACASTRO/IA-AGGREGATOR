import { expect, Page, test } from '@playwright/test';

const MOCK_USER = {
  id: 'quality-user',
  email: 'quality@lume.local',
  fullName: 'Quality Gate',
  role: 'admin',
  status: 'active',
};

const MOCK_REPORTS = [
  {
    id: 'r-1',
    source: 'frontend-web',
    generatedAt: '2026-03-05T09:00:00Z',
    receivedAt: '2026-03-05T09:01:00Z',
    totalEvents: 180,
    counters: {
      onboarding_start: 52,
      onboarding_complete: 39,
      chat_send_start: 120,
    },
  },
  {
    id: 'r-2',
    source: 'frontend-mobile',
    generatedAt: '2026-03-05T10:00:00Z',
    receivedAt: '2026-03-05T10:01:00Z',
    totalEvents: 94,
    counters: {
      onboarding_start: 28,
      onboarding_complete: 17,
      chat_send_start: 60,
    },
  },
];

async function seedAuthenticatedSession(page: Page) {
  await page.context().addCookies([
    {
      name: 'access_token',
      value: 'quality-token',
      url: 'http://127.0.0.1:3001',
    },
  ]);

  await page.addInitScript(() => {
    window.localStorage.clear();
    window.localStorage.setItem('access_token', 'quality-token');
    window.localStorage.setItem('refresh_token', 'quality-refresh');
    window.localStorage.setItem('ia-aggregator-chat-store', JSON.stringify({
      state: {
        conversations: [],
        activeConversationId: null,
        selectedModel: 'gpt-4o-mini',
        availableModels: [
          { id: 'gpt-4o-mini', label: 'GPT-4o Mini', provider: 'OpenAI' },
          { id: 'claude-3-5-haiku', label: 'Claude 3.5 Haiku', provider: 'Anthropic' },
          { id: 'gemini-1.5-flash', label: 'Gemini 1.5 Flash', provider: 'Google' },
        ],
      },
      version: 0,
    }));
  });
}

async function installApiMocks(page: Page) {
  await page.route('**/api/v1/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ data: MOCK_USER }),
    });
  });

  await page.route('**/api/v1/analytics/reports**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ data: MOCK_REPORTS }),
    });
  });

  await page.route('**/api/v1/analytics/reports/*/events**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ data: [] }),
    });
  });
}

function createConsoleCollector(page: Page) {
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

  return issues;
}

async function assertNoConsoleErrors(page: Page) {
  const issues = createConsoleCollector(page);
  await page.waitForTimeout(300);
  expect(issues, 'console errors should be empty').toEqual([]);
}

test.describe('Quality gates', () => {
  test('visual baseline desktop', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1024 });

    await page.goto('/');
    await expect(page).toHaveScreenshot('landing-desktop.png', { fullPage: true });

    await page.goto('/login');
    await expect(page).toHaveScreenshot('login-desktop.png', { fullPage: true });

    await page.goto('/register');
    await expect(page).toHaveScreenshot('register-desktop.png', { fullPage: true });
  });

  test('visual baseline workspace routes', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1024 });
    await seedAuthenticatedSession(page);
    await installApiMocks(page);

    await page.goto('/home');
    await expect(page.getByRole('heading', { name: 'Home' })).toBeVisible();
    await expect(page).toHaveScreenshot('home-desktop.png', { fullPage: true });
    await assertNoConsoleErrors(page);

    await page.goto('/settings/analytics');
    await expect(page.getByText('Participação relativa do volume de eventos')).toBeVisible();
    await expect(page).toHaveScreenshot('analytics-desktop.png', { fullPage: true });
    await assertNoConsoleErrors(page);
  });

  test('keyboard smoke on login', async ({ page }) => {
    await page.goto('/login');

    await page.keyboard.press('Tab');
    await expect(page.getByRole('link', { name: 'Pular para o conteudo' })).toBeFocused();

    for (let attempt = 0; attempt < 12; attempt += 1) {
      if (await page.getByLabel('Email').evaluate((el) => el === document.activeElement)) break;
      await page.keyboard.press('Tab');
    }
    await expect(page.getByLabel('Email')).toBeFocused();

    await page.keyboard.press('Tab');
    await expect(page.getByLabel(/^Senha$/i)).toBeFocused();
  });
});

