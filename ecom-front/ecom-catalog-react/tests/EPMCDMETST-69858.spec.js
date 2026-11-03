// API-level checks only: the UI still expects a bare array (known limitation), so no page-level tests.
// Requires the backend on http://localhost:8080.
import { test, expect } from '@playwright/test';

const API = 'http://localhost:8080';

test.describe('@EPMCDMETST-69858 paginated products API', () => {
  test('@AC-1 first page of ten', async ({ request }) => {
    const r = await request.get(`${API}/api/products?page=0&size=10`);
    expect(r.status()).toBe(200);
    const j = await r.json();
    expect(j.items.length).toBeLessThanOrEqual(10);
    expect(j.page).toBe(0);
    expect(j.size).toBe(10);
    expect(j.totalItems).toBeGreaterThan(10);
    expect(j.totalPages).toBeGreaterThan(1);
  });

  test('@AC-2 third page', async ({ request }) => {
    const r = await request.get(`${API}/api/products?page=2&size=10`);
    expect(r.status()).toBe(200);
    const j = await r.json();
    expect(j.page).toBe(2);
    expect(j.items.length).toBeLessThanOrEqual(10);
  });

  test('@AC-3 negative page is rejected', async ({ request }) => {
    expect((await request.get(`${API}/api/products?page=-1&size=10`)).status()).toBe(400);
  });

  test('@AC-4 zero size is rejected', async ({ request }) => {
    expect((await request.get(`${API}/api/products?page=0&size=0`)).status()).toBe(400);
  });

  test('@AC-5 defaults apply', async ({ request }) => {
    const j = await (await request.get(`${API}/api/products`)).json();
    expect(Array.isArray(j)).toBe(false);
    expect(Object.keys(j).sort()).toEqual(['items', 'page', 'size', 'totalItems', 'totalPages']);
    expect(j.page).toBe(0);
    expect(j.size).toBe(10);
  });

  test('@AC-6 maximum page size is 100', async ({ request }) => {
    expect((await request.get(`${API}/api/products?size=100`)).status()).toBe(200);
    expect((await request.get(`${API}/api/products?size=101`)).status()).toBe(400);
  });

  test('@AC-7 page past the end is empty', async ({ request }) => {
    const r = await request.get(`${API}/api/products?page=50&size=10`);
    expect(r.status()).toBe(200);
    const j = await r.json();
    expect(j.items).toEqual([]);
    expect(j.totalItems).toBeGreaterThanOrEqual(25);
  });

  test('@AC-8 non-numeric parameters are rejected', async ({ request }) => {
    expect((await request.get(`${API}/api/products?page=abc`)).status()).toBe(400);
    expect((await request.get(`${API}/api/products?size=abc`)).status()).toBe(400);
  });

  test('@AC-9 ids ascending across pages', async ({ request }) => {
    const first = await (await request.get(`${API}/api/products?page=0&size=10`)).json();
    const ids = [];
    for (let p = 0; p < first.totalPages; p++) {
      const j = await (await request.get(`${API}/api/products?page=${p}&size=10`)).json();
      ids.push(...j.items.map((i) => i.id));
    }
    expect(ids.length).toBe(first.totalItems);
    expect(ids).toEqual([...ids].sort((a, b) => a - b));
    expect(new Set(ids).size).toBe(ids.length);
  });

  test('@AC-10 seeded dataset has at least 25 products', async ({ request }) => {
    const j = await (await request.get(`${API}/api/products?size=100`)).json();
    expect(j.totalItems).toBeGreaterThanOrEqual(25);
  });
});
