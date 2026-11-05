import { test, expect } from '@playwright/test';

const CATS = [{ id: 1, name: 'Electronics' }];

function makeItems(page, count = 12) {
  return Array.from({ length: count }, (_, i) => ({
    id: page * 100 + i + 1,
    name: `Item P${page + 1}-${i + 1}`,
    description: 'desc',
    imageUrl: 'https://placehold.co/60x40',
    price: 10 + i,
    category: { id: 1, name: 'Electronics' },
  }));
}

function body(page, totalPages, count) {
  return { items: makeItems(page, count), page, size: 12, totalItems: totalPages * 12, totalPages };
}

// handler(pageNumber, route) may fulfil the route itself and return true; otherwise a default success is sent.
async function mockApi(page, { totalPages = 3, handler } = {}) {
  const requests = [];
  await page.route('**/api/categories', route => route.fulfill({ json: CATS }));
  await page.route('**/api/products?*', async route => {
    const url = new URL(route.request().url());
    const p = Number(url.searchParams.get('page'));
    requests.push(url.search);
    if (handler && (await handler(p, route))) return;
    await route.fulfill({ json: body(p, totalPages) });
  });
  return requests;
}

const label = page => page.getByTestId('pagination-label');
const prev = page => page.getByTestId('pagination-prev');
const next = page => page.getByTestId('pagination-next');
const item = (page, name) => page.getByText(name, { exact: true });

test.describe('Prev/Next pagination', () => {
  test('AC-1: first page has Prev disabled and label Page 1 of N', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');
    await expect(item(page, 'Item P1-1')).toBeVisible();
    await expect(prev(page)).toBeDisabled();
    await expect(next(page)).toBeEnabled();
    await expect(label(page)).toHaveText('Page 1 of 3');
  });

  test('AC-2: Next requests page=1&size=12 and shows page 2 items', async ({ page }) => {
    const requests = await mockApi(page);
    await page.goto('/');
    await expect(item(page, 'Item P1-1')).toBeVisible();
    await next(page).click();
    await expect(item(page, 'Item P2-1')).toBeVisible();
    await expect(label(page)).toHaveText('Page 2 of 3');
    expect(requests).toContain('?page=1&size=12');
  });

  test('AC-3: Next is disabled on the last page', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');
    await expect(label(page)).toHaveText('Page 1 of 3');
    await next(page).click();
    await expect(label(page)).toHaveText('Page 2 of 3');
    await next(page).click();
    await expect(label(page)).toHaveText('Page 3 of 3');
    await expect(next(page)).toBeDisabled();
    await expect(prev(page)).toBeEnabled();
  });

  test('Prev returns to the previous page items', async ({ page }) => {
    await mockApi(page);
    await page.goto('/');
    await next(page).click();
    await expect(item(page, 'Item P2-1')).toBeVisible();
    await prev(page).click();
    await expect(item(page, 'Item P1-1')).toBeVisible();
    await expect(label(page)).toHaveText('Page 1 of 3');
  });

  test('N-1: failed Next keeps error and Retry visible with the previous page shown', async ({ page }) => {
    let failPage1 = true;
    await mockApi(page, {
      handler: async (p, route) => {
        if (p === 1 && failPage1) {
          await route.fulfill({ status: 500, body: 'boom' });
          return true;
        }
        return false;
      },
    });
    await page.goto('/');
    await expect(item(page, 'Item P1-1')).toBeVisible();
    await next(page).click();
    await expect(page.getByTestId('catalog-error')).toBeVisible();
    await expect(page.getByTestId('catalog-retry')).toBeVisible();
    await expect(label(page)).toHaveText('Page 1 of 3');
    await expect(item(page, 'Item P1-1')).toBeVisible();
    // the error must persist, i.e. not be cleared by the rollback re-run
    await page.waitForTimeout(500);
    await expect(page.getByTestId('catalog-error')).toBeVisible();
    await expect(page.getByTestId('catalog-retry')).toBeVisible();

    // Retry re-requests the (rolled back) current page; Next then loads page 2 successfully
    failPage1 = false;
    await page.getByTestId('catalog-retry').click();
    await expect(page.getByTestId('catalog-error')).toHaveCount(0);
    await expect(item(page, 'Item P1-1')).toBeVisible();
    await next(page).click();
    await expect(item(page, 'Item P2-1')).toBeVisible();
    await expect(label(page)).toHaveText('Page 2 of 3');
  });

  test('Initial failure: error, Retry and disabled buttons; Retry loads page 0', async ({ page }) => {
    let fail = true;
    await mockApi(page, {
      handler: async (p, route) => {
        if (fail) {
          await route.fulfill({ status: 500, body: 'boom' });
          return true;
        }
        return false;
      },
    });
    await page.goto('/');
    await expect(page.getByTestId('catalog-error')).toBeVisible();
    await expect(page.getByTestId('catalog-retry')).toBeVisible();
    await expect(prev(page)).toBeDisabled();
    await expect(next(page)).toBeDisabled();
    fail = false;
    await page.getByTestId('catalog-retry').click();
    await expect(item(page, 'Item P1-1')).toBeVisible();
    await expect(page.getByTestId('catalog-error')).toHaveCount(0);
    await expect(label(page)).toHaveText('Page 1 of 3');
  });

  test('Buttons are disabled and aria-busy is set while a request is in flight', async ({ page }) => {
    await mockApi(page, {
      handler: async (p, route) => {
        if (p === 1) {
          await new Promise(resolve => setTimeout(resolve, 1500));
          await route.fulfill({ json: body(1, 3) });
          return true;
        }
        return false;
      },
    });
    await page.goto('/');
    await expect(item(page, 'Item P1-1')).toBeVisible();
    await next(page).click();
    await expect(page.getByTestId('catalog-loading')).toBeVisible();
    await expect(page.locator('[aria-busy="true"]')).toHaveCount(1);
    await expect(prev(page)).toBeDisabled();
    await expect(next(page)).toBeDisabled();
    await expect(item(page, 'Item P2-1')).toBeVisible();
    await expect(next(page)).toBeEnabled();
  });

  test('OQ-1 A: search with no match shows the limited hint and triggers no request', async ({ page }) => {
    const requests = await mockApi(page);
    await page.goto('/');
    await expect(item(page, 'Item P1-1')).toBeVisible();
    const before = requests.length;
    await page.getByPlaceholder('Search for products').fill('zzz-no-match');
    await expect(page.getByTestId('catalog-limited-hint')).toBeVisible();
    await page.waitForTimeout(300);
    expect(requests.length).toBe(before);
    await expect(label(page)).toHaveText('Page 1 of 3');
  });

  test('Empty catalog: buttons disabled, no label, empty message', async ({ page }) => {
    await mockApi(page, {
      handler: async (p, route) => {
        await route.fulfill({ json: { items: [], page: 0, size: 12, totalItems: 0, totalPages: 0 } });
        return true;
      },
    });
    await page.goto('/');
    await expect(page.getByTestId('catalog-empty')).toBeVisible();
    await expect(prev(page)).toBeVisible();
    await expect(next(page)).toBeVisible();
    await expect(prev(page)).toBeDisabled();
    await expect(next(page)).toBeDisabled();
    await expect(label(page)).toHaveCount(0);
  });

  test('Clamp: shrinking totalPages refetches the last page', async ({ page }) => {
    let page1Calls = 0;
    const requests = await mockApi(page, {
      handler: async (p, route) => {
        if (p === 0) {
          await route.fulfill({ json: body(0, 3) });
        } else if (p === 1) {
          page1Calls += 1;
          // first call: 3 pages; after the catalog shrinks: 2 pages
          await route.fulfill({ json: body(1, page1Calls === 1 ? 3 : 2) });
        } else {
          await route.fulfill({ json: { items: [], page: p, size: 12, totalItems: 20, totalPages: 2 } });
        }
        return true;
      },
    });
    await page.goto('/');
    await expect(label(page)).toHaveText('Page 1 of 3');
    await next(page).click();
    await expect(label(page)).toHaveText('Page 2 of 3');
    await next(page).click();
    await expect(label(page)).toHaveText('Page 2 of 2');
    await expect(item(page, 'Item P2-1')).toBeVisible();
    expect(requests.filter(r => r === '?page=1&size=12').length).toBe(2);
  });
});
