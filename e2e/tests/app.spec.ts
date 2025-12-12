import { test, expect, Page } from '@playwright/test';

const waitForRow = async (page: Page, text: string) => {
  await expect(page.locator('.row', { hasText: text })).toBeVisible({ timeout: 15000 });
};

const openFolder = async (page: Page, name: string) => {
  const row = page.locator('.row', { hasText: name }).first();
  await expect(row).toBeVisible({ timeout: 15000 });
  await row.getByRole('button', { name: 'Open' }).first().click();
};

const login = async (page: Page, username: string, password: string) => {
  await page.goto('/');
  await page.waitForURL('**/login');
  await expect(page.locator('input[name="username"]')).toBeVisible();
  await page.fill('input[name="username"]', username);
  await page.fill('input[name="password"]', password);
  await page.getByRole('button', { name: 'Login' }).click();
  await page.waitForURL('**/');
  await expect(page.getByText(`Logged in as ${username}`)).toBeVisible();
};

test.describe('LDAP access control', () => {
  test('read-only user can browse but cannot write', async ({ page }) => {
    await login(page, 'alice', 'password1');
    await expect(page.getByRole('banner').getByText('Access: Read-only')).toBeVisible();
    await expect(page.getByText('read-only mode', { exact: false })).toBeVisible();

    await page.waitForSelector('select#bucket');
    const bucketOptions = page.locator('select#bucket option');
    const firstId = await bucketOptions.first().getAttribute('value');
    if (firstId) {
      await page.selectOption('select#bucket', firstId);
    }

    await waitForRow(page, 'app');
    await openFolder(page, 'app');
    await openFolder(page, '2025');
    await openFolder(page, '01');
    await openFolder(page, '02');
    await waitForRow(page, 'trade_2025_01.csv');

    // Search and download still work
    await page.fill('input[name="query"]', 'trade_2025_*.csv');
    await page.getByRole('button', { name: 'Search' }).click();
    await waitForRow(page, 'trade_2025_01.csv');
    const downloadPromise = page.waitForEvent('download');
    const downloadRow = page.locator('.row', { hasText: 'trade_2025_01.csv' }).first();
    await downloadRow.getByRole('button', { name: 'Download' }).first().click();
    expect(await (await downloadPromise).path()).toBeTruthy();

    // Write actions hidden/disabled
    await expect(page.getByRole('button', { name: 'Copy', exact: true })).toBeDisabled();
    await expect(page.getByRole('button', { name: 'Bulk delete' })).toHaveCount(0);

    // Force a write call and ensure backend rejects it
    const status = await page.evaluate(async () => {
      const res = await fetch('http://localhost:9080/api/buckets/logs/objects/copy', {
        method: 'POST',
        credentials: 'include',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ sourceKey: 'root/readme.txt', targetKey: 'root/readme-copy.txt', overwrite: true })
      });
      return res.status;
    });
    expect(status).toBe(403);
  });

  test('read-write user can perform object operations', async ({ page }) => {
    await login(page, 'bob', 'password1');
    await expect(page.getByRole('banner').getByText('Access: Read & write')).toBeVisible();

    await page.waitForSelector('select#bucket');
    await page.selectOption('select#bucket', { index: 0 });
    await waitForRow(page, 'app');

    await openFolder(page, 'app');
    await openFolder(page, '2025');
    await openFolder(page, '01');
    await openFolder(page, '02');
    await waitForRow(page, 'trade_2025_01.csv');

    // Bulk copy
    const debugRow = page.locator('.row', { hasText: 'debug.json' });
    await debugRow.locator('input[type="checkbox"]').check();
    const tradeRow = page.locator('.row', { hasText: 'trade_2025_01.csv' });
    await tradeRow.locator('input[type="checkbox"]').check();
    await page.fill('input[name="bulkTarget"]', 'app/2025/01/02/copied/');
    await page.getByRole('button', { name: 'Bulk copy' }).click();
    await waitForRow(page, 'copied');

    // Navigate to app/2025/01/01 and copy/move/delete object
    await page.locator('.breadcrumb .crumb', { hasText: 'root' }).click();
    await openFolder(page, 'app');
    await openFolder(page, '2025');
    await openFolder(page, '01');
    await openFolder(page, '01');
    await waitForRow(page, 'app.log');

    const appLogRow = page.locator('.row', { hasText: 'app.log' });
    await appLogRow.locator('input[type="checkbox"]').check();
    await page.fill('input[name="targetKey"]', 'app/2025/01/01/app-copy.log');
    await page.locator('.panel').getByRole('button', { name: 'Copy', exact: true }).click();
    await waitForRow(page, 'app-copy.log');

    const copyRow = page.locator('.row', { hasText: 'app-copy.log' });
    await copyRow.locator('input[type="checkbox"]').check();
    await page.fill('input[name="targetKey"]', 'app/2025/01/01/app-moved.log');
    await page.locator('.panel').getByRole('button', { name: 'Move', exact: true }).click();
    await waitForRow(page, 'app-moved.log');

    const movedRow = page.locator('.row', { hasText: 'app-moved.log' });
    await movedRow.locator('input[type="checkbox"]').check();
    await page.getByRole('button', { name: 'Bulk delete' }).click();
    await expect(page.locator('.row', { hasText: 'app-moved.log' })).toHaveCount(0, { timeout: 10000 });

    // Folder size and move
    await page.fill('input[name="folderPrefix"]', 'app/2025/01/01/');
    await page.getByRole('button', { name: 'Folder size' }).click();
    const sizeCard = page.locator('.size-card');
    await expect(sizeCard).toBeVisible({ timeout: 5000 });
    await expect(sizeCard.getByText('objects ·', { exact: false })).toBeVisible({ timeout: 10000 });

    const crumbToMonth = page.locator('.breadcrumb .crumb', { hasText: /^01$/ }).first();
    await crumbToMonth.click();
    await waitForRow(page, '01');
    const folderRow = page.locator('.row', { hasText: '01' }).first();
    await folderRow.locator('input[type="checkbox"]').check();
    await page.fill('input[name="bulkTarget"]', 'app/2025/05/');
    await page.getByRole('button', { name: 'Bulk move' }).click();

    await page.goto('/');
    await waitForRow(page, 'app');
    await openFolder(page, 'app');
    await openFolder(page, '2025');
    await openFolder(page, '05');
    await waitForRow(page, 'app.log');
  });

  test('invalid credentials show error', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[name="username"]', 'alice');
    await page.fill('input[name="password"]', 'badpass');
    await page.getByRole('button', { name: 'Login' }).click();
    await expect(page.getByText('Invalid username or password')).toBeVisible();
  });

  test('user without role is denied', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[name="username"]', 'guest');
    await page.fill('input[name="password"]', 'password1');
    await page.getByRole('button', { name: 'Login' }).click();
    await expect(page.getByText('Invalid username or password')).toBeVisible();
  });
});
