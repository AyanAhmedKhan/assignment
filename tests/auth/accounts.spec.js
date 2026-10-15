import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const password = 'Browser account test 93!';

async function csrfHeaders(context) {
  await context.request.get('/api/csrf');
  const cookie = (await context.cookies()).find((value) => value.name === 'XSRF-TOKEN');
  return { 'X-XSRF-TOKEN': cookie.value };
}

async function signIn(page, username, accountPassword = password) {
  await page.goto('/login.html');
  await page.getByLabel('Email or username', { exact: true }).fill(username);
  await page.getByLabel('Password', { exact: true }).fill(accountPassword);
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.locator('#directory')).toBeVisible();
  await expect(page.locator('#sign-out')).toBeVisible();
}

test('registration validates fields, creates a private account, and rejects an existing email', async ({
  page,
}) => {
  const email = process.env.REGISTRATION_TEST_EMAIL || `browser-signup-${Date.now()}@example.com`;
  await page.goto('/register.html');
  await page.getByRole('button', { name: 'Create account' }).click();
  await expect(page.locator('#error-fullName')).toContainText('Enter your name');
  await expect(page.locator('#error-email')).toContainText('valid email');
  await expect(page.locator('#error-password')).toContainText('12 to 64');
  await page.getByLabel('Full name', { exact: true }).fill('New Workspace Owner');
  await page.getByLabel('Email address', { exact: true }).fill(email);
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page.getByLabel('Confirm password', { exact: true }).fill('not-the-same');
  await page.getByRole('button', { name: 'Create account' }).click();
  await expect(page.locator('#error-confirmPassword')).toContainText('do not match');
  await page.getByLabel('Confirm password', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Create account' }).click();
  await expect(page).toHaveURL(/login\.html\?registered$/);
  await signIn(page, email);
  await expect(page.locator('#stat-total')).toHaveText('0');
  await expect(page.locator('#workspace-name')).toHaveText('New Workspace Owner');
  await page.locator('#sign-out').click();
  await page.goto('/register.html');
  await page.getByLabel('Full name', { exact: true }).fill('Duplicate Account');
  await page.getByLabel('Email address', { exact: true }).fill(email.toUpperCase());
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page.getByLabel('Confirm password', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Create account' }).click();
  await expect(page.locator('#error-email')).toContainText('already exists');
});

test('demo editor can change only its own records and demo viewer cannot write', async ({
  page,
  browser,
  baseURL,
}) => {
  await page.goto('/login.html');
  await page.getByRole('button', { name: 'Demo editor', exact: true }).click();
  await expect(page.locator('#account-notice')).toContainText('Demo editor');
  await expect(page.locator('#add-employee')).toBeVisible();
  const editor = page.context();
  const payload = {
    fullName: 'Private Demo Test',
    email: `isolation-${Date.now()}@example.com`,
    phoneNumber: '+91 98765 43210',
    department: 'ENGINEERING',
    designation: 'Engineer',
    dateOfJoining: '2024-01-01',
    salary: '500000',
    status: 'ACTIVE',
  };
  const created = await editor.request.post('/api/employees', {
    data: payload,
    headers: await csrfHeaders(editor),
  });
  expect(created.status()).toBe(201);
  const { id } = await created.json();
  const viewer = await browser.newContext({ baseURL });
  try {
    const viewerPage = await viewer.newPage();
    await viewerPage.goto('/login.html');
    await viewerPage.getByRole('button', { name: 'Demo viewer', exact: true }).click();
    await expect(viewerPage.locator('#account-notice')).toContainText('read-only');
    await expect(viewerPage.locator('#add-employee')).not.toBeVisible();
    await expect(viewerPage.locator('[data-edit]')).toHaveCount(0);
    expect((await viewer.request.get(`/api/employees/${id}`)).status()).toBe(404);
    const search = await viewer.request.get('/api/employees', {
      params: { search: payload.email },
    });
    expect((await search.json()).totalElements).toBe(0);
    expect(
      (
        await viewer.request.post('/api/employees', {
          data: payload,
          headers: await csrfHeaders(viewer),
        })
      ).status(),
    ).toBe(403);
    expect(
      (
        await viewer.request.delete(`/api/employees/${id}`, { headers: await csrfHeaders(viewer) })
      ).status(),
    ).toBe(403);
    await viewerPage.locator('#employees-body .person-name').first().click();
    await expect(viewerPage.locator('#detail-edit')).toHaveCount(0);
    await expect(viewerPage.locator('#detail-delete')).toHaveCount(0);
  } finally {
    await editor.request.delete(`/api/employees/${id}`, { headers: await csrfHeaders(editor) });
    await viewer.close();
  }
});

test('login and registration stay accessible on a mobile screen', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  for (const path of ['/login.html', '/register.html']) {
    await page.goto(path);
    await expect(
      page.locator('form button[type="submit"], #sign-in, #create-account').first(),
    ).toBeEnabled();
    expect(
      await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth),
    ).toBe(true);
    const scan = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
      .analyze();
    expect(scan.violations).toEqual([]);
  }
});
