import { test, expect } from '@playwright/test';

const payload = (name, email, extra = {}) => ({
  fullName: name,
  email,
  phoneNumber: '+91 98765 43210',
  department: 'ENGINEERING',
  designation: 'Software Engineer',
  dateOfJoining: '2024-06-10',
  salary: '650000.50',
  status: 'ACTIVE',
  ...extra,
});

test('create, view, edit, cancel deletion and delete an employee', async ({ page, request }) => {
  const email = `lifecycle-${Date.now()}@example.com`;
  let employeeId;
  const browserErrors = [];
  page.on('pageerror', (error) => browserErrors.push(error.message));
  try {
    await page.goto('/');
    await page.getByRole('button', { name: 'Add employee', exact: true }).first().click();
    const form = page.locator('#employee-form');
    await form.getByLabel('Full name').fill('Browser Test Employee');
    await form.getByLabel('Email address').fill(email);
    await form.getByLabel('Phone number').fill('+91 98765 43210');
    await form.getByLabel('Department').selectOption('ENGINEERING');
    await form.getByLabel('Designation').fill('Software Engineer');
    await form.getByLabel('Date of joining').fill('2024-06-10');
    await form.getByLabel('Annual salary').fill('650000.50');
    const created = page.waitForResponse(
      (response) =>
        response.url().endsWith('/api/employees') && response.request().method() === 'POST',
    );
    await form.getByRole('button', { name: 'Add employee', exact: true }).click();
    const response = await created;
    expect(response.status()).toBe(201);
    employeeId = (await response.json()).id;
    await expect(page.locator('#employee-dialog')).not.toBeVisible();
    await page.getByRole('searchbox').fill(email);
    await page.getByRole('link', { name: 'Browser Test Employee', exact: true }).click();
    await expect(page.locator('#employee-detail')).toContainText('₹6,50,000.50');
    await page.getByRole('button', { name: 'Edit employee', exact: true }).click();
    await form.getByLabel('Designation').fill('Senior Software Engineer');
    await form.getByLabel('Employment status').selectOption('INACTIVE');
    await form.getByRole('button', { name: 'Save changes' }).click();
    await expect(page.locator('#employee-dialog')).not.toBeVisible();
    await expect(page.locator('#employee-detail')).toContainText('Senior Software Engineer');
    await expect(page.locator('#employee-detail .status')).toHaveText('Inactive');
    await page.getByRole('button', { name: 'Delete', exact: true }).click();
    await page.getByRole('button', { name: 'Keep employee' }).click();
    expect((await request.get(`/api/employees/${employeeId}`)).status()).toBe(200);
    await page.getByRole('button', { name: 'Delete', exact: true }).click();
    await page.getByRole('button', { name: 'Delete employee', exact: true }).click();
    await expect(page.locator('#delete-dialog')).not.toBeVisible();
    await expect(page.locator('#directory')).toBeVisible();
    expect((await request.get(`/api/employees/${employeeId}`)).status()).toBe(404);
    expect(browserErrors).toEqual([]);
  } finally {
    if (employeeId) await request.delete(`/api/employees/${employeeId}`);
  }
});

test('shows field errors and rejects duplicate email without closing the form', async ({
  page,
  request,
}) => {
  const email = `duplicate-${Date.now()}@example.com`;
  const response = await request.post('/api/employees', {
    data: payload('Existing Employee', email),
  });
  expect(response.status()).toBe(201);
  const { id } = await response.json();
  try {
    await page.goto('/');
    await page.locator('#add-employee').click();
    const form = page.locator('#employee-form');
    await form.getByRole('button', { name: 'Add employee', exact: true }).click();
    await expect(form.locator('[aria-invalid=true]')).toHaveCount(7);
    await form.getByLabel('Full name').fill('New Employee');
    await form.getByLabel('Email address').fill(email.toUpperCase());
    await form.getByLabel('Phone number').fill('abcdefg');
    await form.getByLabel('Department').selectOption('DESIGN');
    await form.getByLabel('Designation').fill('Designer');
    await form.getByLabel('Date of joining').fill('2099-01-01');
    await form.getByLabel('Annual salary').fill('-20');
    await form.getByRole('button', { name: 'Add employee', exact: true }).click();
    await expect(form.locator('[aria-invalid=true]')).toHaveCount(3);
    await expect(page.locator('#error-phoneNumber')).toContainText('7 to 15 digits');
    await form.getByLabel('Phone number').fill('+91 98765 43210');
    await form.getByLabel('Date of joining').fill('2024-06-10');
    await form.getByLabel('Annual salary').fill('600000');
    await form.getByRole('button', { name: 'Add employee', exact: true }).click();
    await expect(page.locator('#error-email')).toContainText('already exists');
    await expect(page.locator('#employee-dialog')).toBeVisible();
    await expect(form.getByLabel('Full name')).toHaveValue('New Employee');
  } finally {
    await request.delete(`/api/employees/${id}`);
  }
});

test('search, combined filters and pagination return matching records', async ({
  page,
  request,
}) => {
  const prefix = `BrowserFilter${Date.now()}`;
  const ids = [];
  try {
    for (let i = 0; i < 7; i++) {
      const response = await request.post('/api/employees', {
        data: payload(`${prefix} ${i}`, `${prefix}-${i}@example.com`),
      });
      expect(response.status()).toBe(201);
      ids.push((await response.json()).id);
    }
    await page.goto('/');
    await page.getByRole('searchbox').fill(prefix);
    await page.getByLabel('Filter by department').selectOption('ENGINEERING');
    await page.getByLabel('Filter by status').selectOption('ACTIVE');
    await page.getByLabel('Rows per page').selectOption('5');
    await expect(page.locator('#range-label')).toHaveText('Showing 1–5 of 7 employees');
    await expect(page.locator('#employees-body tr')).toHaveCount(5);
    await page.getByRole('button', { name: 'Next page' }).click();
    await expect(page.locator('#range-label')).toHaveText('Showing 6–7 of 7 employees');
    await expect(page.locator('#employees-body tr')).toHaveCount(2);
    await page.getByLabel('Filter by status').selectOption('INACTIVE');
    await expect(page.getByRole('heading', { name: 'No matching employees' })).toBeVisible();
  } finally {
    for (const id of ids) await request.delete(`/api/employees/${id}`);
  }
});

test('server errors can be retried and mobile layout stays within the viewport', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.route('**/api/employees?*', (route) =>
    route.fulfill({
      status: 503,
      contentType: 'application/problem+json',
      body: JSON.stringify({ detail: 'Temporarily unavailable.' }),
    }),
  );
  await page.goto('/');
  await expect(page.getByRole('heading', { name: "Couldn't load employees" })).toBeVisible();
  await page.unroute('**/api/employees?*');
  await page.getByRole('button', { name: 'Try again' }).click();
  await expect(page.locator('#range-label')).toContainText('employees');
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(
    true,
  );
  await page.locator('#add-employee').click();
  await expect(page.locator('#employee-dialog')).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(page.locator('#employee-dialog')).not.toBeVisible();
});

test('renders employee input as text instead of executable markup', async ({ page, request }) => {
  const response = await request.post('/api/employees', {
    data: payload('<img src=x onerror=alert(1)>', `xss-${Date.now()}@example.com`),
  });
  expect(response.status()).toBe(201);
  const { id } = await response.json();
  let dialogOpened = false;
  page.on('dialog', async (dialog) => {
    dialogOpened = true;
    await dialog.dismiss();
  });
  try {
    await page.goto(`/#employees/${id}`);
    await expect(page.locator('#employee-detail h1')).toHaveText('<img src=x onerror=alert(1)>');
    await expect(page.locator('#employee-detail img')).toHaveCount(0);
    expect(dialogOpened).toBe(false);
  } finally {
    await request.delete(`/api/employees/${id}`);
  }
});
