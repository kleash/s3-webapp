const fs = require('fs');
const path = require('path');
const { chromium } = require('@playwright/test');

const BASE_URL = process.env.BASE_URL || 'http://localhost:9080';
const USERNAME = process.env.S3_WEBAPP_USER || 'bob';
const PASSWORD = process.env.S3_WEBAPP_PASSWORD || 'password1';
const OUTPUT_DIR = path.join(__dirname, '..', 'docs', 'screenshots');

const waitForRow = async (page, text) => {
  await page.locator('.row', { hasText: text }).first().waitFor({ state: 'visible', timeout: 15000 });
};

const openFolder = async (page, name) => {
  const row = page.locator('.row', { hasText: name }).first();
  await row.waitFor({ state: 'visible', timeout: 15000 });
  await row.getByRole('button', { name: 'Open' }).first().click();
};

const ensureBucketSelected = async (page) => {
  await page.waitForSelector('select#bucket');
  const firstValue = await page.locator('select#bucket option').first().getAttribute('value');
  if (firstValue) {
    await page.selectOption('select#bucket', firstValue);
  }
};

const login = async (page) => {
  await page.goto(`${BASE_URL}/login`);
  await page.waitForSelector('form');
  await page.fill('input[name="username"]', USERNAME);
  await page.fill('input[name="password"]', PASSWORD);
  await page.getByRole('button', { name: 'Login' }).click();
  await page.waitForURL('**/');
};

(async () => {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true });

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1400, height: 900 } });

  await page.goto(`${BASE_URL}/login`);
  await page.waitForSelector('form');
  await page.screenshot({ path: path.join(OUTPUT_DIR, 'login.png'), fullPage: true });

  await login(page);
  await ensureBucketSelected(page);
  await waitForRow(page, 'app');

  await openFolder(page, 'app');
  await openFolder(page, '2025');
  await openFolder(page, '01');
  await openFolder(page, '02');
  await waitForRow(page, 'trade_2025_01.csv');

  await page.screenshot({ path: path.join(OUTPUT_DIR, 'browser.png'), fullPage: true });

  await page.fill('input[name="folderPrefix"]', 'app/2025/01/01/');
  await page.getByRole('button', { name: 'Folder size' }).click();
  const sizeCard = page.locator('.size-card');
  await sizeCard.waitFor({ state: 'visible', timeout: 10000 });
  await sizeCard.scrollIntoViewIfNeeded();
  await page.waitForTimeout(500);

  await page.screenshot({ path: path.join(OUTPUT_DIR, 'folder-size.png'), fullPage: true });

  await browser.close();
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
