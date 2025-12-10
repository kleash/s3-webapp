import { test, expect, Page } from '@playwright/test';

const waitForRow = async (page: Page, text: string) => {
  await expect(page.locator('.row', { hasText: text })).toBeVisible({ timeout: 15000 });
};

const openFolder = async (page: Page, name: string) => {
  const row = page.locator('.row', { hasText: name }).first();
  await row.getByRole('button', { name: 'Open' }).click();
};

test('full S3 browser flow', async ({ page }) => {
  await page.goto('/');

  // Bucket list and initial load
  await page.waitForSelector('select#bucket');
  const bucketOptions = page.locator('select#bucket option');
  const firstId = await bucketOptions.first().getAttribute('value');
  expect(await bucketOptions.count()).toBeGreaterThan(0);
  if (firstId) {
    await page.selectOption('select#bucket', firstId);
  }

  await waitForRow(page, 'app');

  // Navigate deep folder path app/2025/01/02
  await openFolder(page, 'app');
  await waitForRow(page, '2025');
  await openFolder(page, '2025');
  await waitForRow(page, '01');
  await openFolder(page, '01');
  await waitForRow(page, '02');
  await openFolder(page, '02');
  await waitForRow(page, 'trade_2025_01.csv');

  // Search within current folder
  await page.fill('input[name="query"]', 'trade_2025_*.csv');
  await page.getByRole('button', { name: 'Search' }).click();
  await waitForRow(page, 'trade_2025_01.csv');

  // Download object
  const downloadPromise = page.waitForEvent('download');
  const tradeRow = page.locator('.row', { hasText: 'trade_2025_01.csv' });
  await tradeRow.getByRole('button', { name: 'Download' }).click();
  const download = await downloadPromise;
  const path = await download.path();
  expect(path).toBeTruthy();

  // Reset search
  await page.getByRole('button', { name: 'Reset' }).click();

  // Bulk copy two files into copied/ subfolder
  const debugRow = page.locator('.row', { hasText: 'debug.json' });
  await debugRow.locator('input[type="checkbox"]').check();
  const tradeRow2 = page.locator('.row', { hasText: 'trade_2025_01.csv' });
  await tradeRow2.locator('input[type="checkbox"]').check();
  await page.fill('input[name="bulkTarget"]', 'app/2025/01/02/copied/');
  await page.getByRole('button', { name: 'Bulk copy' }).click();
  await waitForRow(page, 'copied');

  // Navigate to app/2025/01/01
  await page.locator('.breadcrumb .crumb', { hasText: 'root' }).click();
  await waitForRow(page, 'app');
  await openFolder(page, 'app');
  await openFolder(page, '2025');
  await openFolder(page, '01');
  await waitForRow(page, '01'); // day folder
  await openFolder(page, '01');
  await waitForRow(page, 'app.log');

  // Copy object
  const appLogRow = page.locator('.row', { hasText: 'app.log' });
  await appLogRow.locator('input[type="checkbox"]').check();
  await page.fill('input[name="targetKey"]', 'app/2025/01/01/app-copy.log');
  await page.locator('.panel').getByRole('button', { name: 'Copy', exact: true }).click();
  await waitForRow(page, 'app-copy.log');

  // Move copied object
  const copyRow = page.locator('.row', { hasText: 'app-copy.log' });
  await copyRow.locator('input[type="checkbox"]').check();
  await page.fill('input[name="targetKey"]', 'app/2025/01/01/app-moved.log');
  await page.locator('.panel').getByRole('button', { name: 'Move', exact: true }).click();
  await waitForRow(page, 'app-moved.log');

  // Delete moved object via bulk delete
  const movedRow = page.locator('.row', { hasText: 'app-moved.log' });
  await movedRow.locator('input[type="checkbox"]').check();
  await page.getByRole('button', { name: 'Bulk delete' }).click();
  await expect(page.locator('.row', { hasText: 'app-moved.log' })).toHaveCount(0, { timeout: 10000 });

  // Folder size
  await page.fill('input[name="folderPrefix"]', 'app/2025/01/01/');
  await page.getByRole('button', { name: 'Folder size' }).click();
  await expect(page.locator('.size-card')).toBeVisible({ timeout: 5000 });

  // Folder move scenario: move 01/ to new root/app/2025/05/
  const crumbToMonth = page.locator('.breadcrumb .crumb', { hasText: /^01$/ }).first();
  await crumbToMonth.click();
  await waitForRow(page, '01');
  const folderRow = page.locator('.row', { hasText: '01' }).first();
  await folderRow.locator('input[type="checkbox"]').check();
  await page.fill('input[name="bulkTarget"]', 'root/app/2025/05/');
  await page.getByRole('button', { name: 'Bulk move' }).click();

  // Verify moved content at new location
  await page.locator('.breadcrumb .crumb', { hasText: 'root' }).click();
  await waitForRow(page, 'root');
  await openFolder(page, 'root');
  await waitForRow(page, 'app');
  await openFolder(page, 'app');
  await waitForRow(page, '2025');
  await openFolder(page, '2025');
  await waitForRow(page, '05');
  await openFolder(page, '05');
  await waitForRow(page, 'app.log');
});
