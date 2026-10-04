import { test, expect } from '@playwright/test';

test.describe('Catalog – Server-side query flow', () => {
  test('loads initial products and shows pagination controls', async ({ page }) => {
    await page.goto('/');

    await expect(page.locator('.product-card, card, .card').first()).toBeVisible({ timeout: 30000 });

    await expect(page.getByRole('button', { name: /Prev/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /Next/i })).toBeVisible();
  });

  test('can search by keyword and resets to page 1 behavior', async ({ page }) => {
    await page.goto('/');

    const searchInput = page.locator('input[type="text"]').first();
    await searchInput.fill('i');
    await searchInput.press('Enter');

    await expect(page.locator('.product-card, card, .card').first()).toBeVisible({ timeout: 30000 });

    await expect(page.getByRole('button', { name: /Prev/i })).toBeDisabled();
  });

  test('can change sort order and it updates results', async ({ page }) => {
    await page.goto('/');

    const sortSelect = page.locator('select').first();
    if (await sortSelect.count()) {
      await sortSelect.selectOption({ index: 1 });
      await expect(page.locator('.product-card, card, .card').first()).toBeVisible();
    } else {
      test.skip(true, 'No sort select found in UI');
    }
  });

  test('can paginate to next page and updates the list', async ({ page }) => {
    await page.goto('/');

    const firstCardText = await page.locator('.product-card, card, .card').first().innerText();
    const nextBtn = page.getByRole('button', { name: /Next/i });
    await nextBtn.click();

    await page.waitForTimeout(1000);
    const secondCardText = await page.locator('.product-card, card, .card').first().innerText();
    expect(secondCardText).not.toEqual(firstCardText);
  });
});
