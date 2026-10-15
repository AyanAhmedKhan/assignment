import { chromium } from '@playwright/test';
import { mkdir } from 'node:fs/promises';

// Run against the application with the demo profile and an unfiltered directory.
const browser = await chromium.launch();
const page = await browser.newPage({
  viewport: { width: 1440, height: 1400 },
  deviceScaleFactor: 1,
});
const baseURL = process.env.APP_URL || 'http://127.0.0.1:8080';
await mkdir('docs/screenshots', { recursive: true });
try {
  await page.goto(baseURL);
  await page.locator('#employees-body tr').first().waitFor();
  await page.screenshot({ path: 'docs/screenshots/directory.png' });
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.locator('#add-employee').click();
  await page.screenshot({ path: 'docs/screenshots/add-employee.png' });
  await page.locator('#save-employee').click();
  await page.screenshot({ path: 'docs/screenshots/validation.png' });
  await page.locator('#cancel-form').click();
  await page.locator('#employees-body .person-name').first().click();
  await page.locator('#employee-detail h1').waitFor();
  await page.screenshot({ path: 'docs/screenshots/employee-details.png' });
  await page.getByRole('button', { name: 'Edit employee', exact: true }).click();
  await page.screenshot({ path: 'docs/screenshots/edit-employee.png' });
  await page.locator('#cancel-form').click();
  await page.getByRole('button', { name: 'Delete', exact: true }).click();
  await page.screenshot({ path: 'docs/screenshots/delete-confirmation.png' });
  await page.locator('#cancel-delete').click();
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto(baseURL);
  await page.locator('#employees-body tr').first().waitFor();
  await page.screenshot({ path: 'docs/screenshots/mobile.png', fullPage: true });
  console.log('Saved seven screenshots under docs/screenshots.');
} finally {
  await browser.close();
}
