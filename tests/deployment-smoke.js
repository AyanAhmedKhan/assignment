import { chromium, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { readFile, mkdir } from 'node:fs/promises';

// Optional: run only against an owned deployment with fictional assessment data.
// Keep the credential file outside the repository and pass its path through the environment.
if (!process.env.DEPLOY_ACCESS_FILE)
  throw new Error('Set DEPLOY_ACCESS_FILE to the private access JSON.');
const access = JSON.parse(
  (await readFile(process.env.DEPLOY_ACCESS_FILE, 'utf8')).replace(/^\uFEFF/, ''),
);
const browser = await chromium.launch();
const context = await browser.newContext({
  baseURL: access.url,
  viewport: { width: 1440, height: 1000 },
});
const page = await context.newPage();
const browserErrors = [];
page.on('pageerror', (error) => browserErrors.push(error.message));
let createdId;

async function authenticatedRequest(method, path, data) {
  await context.request.get('/api/csrf');
  const token = (await context.cookies()).find((cookie) => cookie.name === 'XSRF-TOKEN');
  return context.request.fetch(path, { method, data, headers: { 'X-XSRF-TOKEN': token.value } });
}

try {
  expect((await context.request.get('/api/employees')).status()).toBe(401);
  expect(await (await context.request.get('/actuator/health')).json()).toEqual({ status: 'UP' });
  await page.goto('/');
  await expect(page).toHaveURL(/\/login\.html$/);
  await expect(page.getByRole('button', { name: 'Sign in', exact: true })).toBeEnabled();
  await mkdir('docs/screenshots', { recursive: true });
  await page.screenshot({ path: 'docs/screenshots/login.png' });
  const loginAccessibility = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21aa'])
    .analyze();
  expect(loginAccessibility.violations).toEqual([]);
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(
    true,
  );
  await page.setViewportSize({ width: 1440, height: 1000 });

  await page.getByLabel('Email or username', { exact: true }).fill(access.username);
  await page.getByLabel('Password', { exact: true }).fill('invalid-test-password');
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.locator('#login-message')).toContainText('incorrect');
  await page.getByLabel('Email or username', { exact: true }).fill(access.username);
  await page.getByLabel('Password', { exact: true }).fill(access.password);
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.locator('#directory-title')).toHaveText('Employee directory.');
  await expect(page.locator('#sign-out')).toBeVisible();
  const session = (await context.cookies()).find((cookie) => cookie.name === 'JSESSIONID');
  expect(session).toMatchObject({ secure: true, httpOnly: true, sameSite: 'Lax' });
  expect((await context.request.delete('/api/employees/1')).status()).toBe(403);

  const invalid = await authenticatedRequest('POST', '/api/employees', { fullName: '' });
  expect(invalid.status()).toBe(400);
  expect((await invalid.json()).errors).toHaveProperty('fullName');
  await page.locator('#add-employee').click();
  const form = page.locator('#employee-form');
  const email = `deployment-check-${Date.now()}@example.com`;
  await form.getByLabel('Full name').fill('Deployment Check');
  await form.getByLabel('Email address').fill(email);
  await form.getByLabel('Phone number').fill('+91 98765 43210');
  await form.getByLabel('Department').selectOption('ENGINEERING');
  await form.getByLabel('Designation').fill('Backend Engineer');
  await form.getByLabel('Date of joining').fill('2024-06-10');
  await form.getByLabel('Annual salary').fill('650000.50');
  const created = page.waitForResponse(
    (response) =>
      response.url().endsWith('/api/employees') && response.request().method() === 'POST',
  );
  await form.getByRole('button', { name: 'Add employee', exact: true }).click();
  const response = await created;
  expect(response.status()).toBe(201);
  createdId = (await response.json()).id;
  await expect(page.locator('#employee-dialog')).not.toBeVisible();
  await page.getByRole('searchbox').fill(email);
  await page.getByRole('link', { name: 'Deployment Check', exact: true }).click();
  await page.getByRole('button', { name: 'Edit employee', exact: true }).click();
  await form.getByLabel('Designation').fill('Senior Backend Engineer');
  await form.getByRole('button', { name: 'Save changes', exact: true }).click();
  await expect(page.locator('#employee-dialog')).not.toBeVisible();
  await expect(page.locator('#employee-detail')).toContainText('Senior Backend Engineer');
  await page.getByRole('button', { name: 'Delete', exact: true }).click();
  await page.getByRole('button', { name: 'Delete employee', exact: true }).click();
  await expect(page.locator('#directory')).toBeVisible();
  expect((await context.request.get(`/api/employees/${createdId}`)).status()).toBe(404);
  createdId = undefined;
  const docs = await context.request.get('/v3/api-docs');
  expect(docs.status()).toBe(200);
  expect((await docs.json()).info.title).toBe('Klodspot Assignment API');
  await page.locator('#sign-out').click();
  await expect(page).toHaveURL(/\/login\.html\?logout$/);
  expect((await context.request.get('/api/employees')).status()).toBe(401);
  expect(browserErrors).toEqual([]);
  console.log(
    'Hosted checks passed: HTTPS, login/logout, protected API, secure cookies, CSRF, validation, CRUD, OpenAPI, accessible login and mobile layout.',
  );
} catch (error) {
  console.error(String(error).replaceAll(access.password, '[redacted]'));
  process.exitCode = 1;
} finally {
  if (createdId)
    await authenticatedRequest('DELETE', `/api/employees/${createdId}`).catch(() => {});
  await browser.close();
}
