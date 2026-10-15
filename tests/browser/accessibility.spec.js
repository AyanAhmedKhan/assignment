import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

test('directory, form and details have no detected WCAG A/AA violations', async ({ page }) => {
  test.setTimeout(60000);
  await page.goto('/');
  await expect(page.locator('#employees-body')).toHaveAttribute('aria-busy', 'false');
  const audit = async () => {
    const result = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
      .analyze();
    expect(result.violations, JSON.stringify(result.violations, null, 2)).toEqual([]);
  };
  await audit();
  await page.locator('#add-employee').click();
  await audit();
  await page.locator('#save-employee').click();
  await audit();
  await page.locator('#cancel-form').click();
  const firstEmployee = page.locator('#employees-body .person-name').first();
  if (await firstEmployee.count()) {
    await firstEmployee.click();
    await expect(page.locator('#employee-detail h1')).toBeVisible();
    await audit();
  }
});
