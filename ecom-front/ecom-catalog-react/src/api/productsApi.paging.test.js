import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { fetchCategories, fetchProducts } from './productsApi'

const paged = { items: [{ id: 1 }], page: 0, size: 12, totalItems: 25, totalPages: 3 }

describe('productsApi paging', () => {
  const origFetch = globalThis.fetch

  beforeEach(() => {
    globalThis.fetch = vi.fn()
  })

  afterEach(() => {
    globalThis.fetch = origFetch
  })

  it('fetchProducts requests page 0 with size 12', async () => {
    globalThis.fetch.mockResolvedValue({ ok: true, json: async () => paged })
    await fetchProducts({ page: 0, size: 12 })
    expect(globalThis.fetch).toHaveBeenCalledWith('http://localhost:8080/api/products?page=0&size=12')
  })

  it('fetchProducts requests page 2 with size 12', async () => {
    globalThis.fetch.mockResolvedValue({ ok: true, json: async () => paged })
    await fetchProducts({ page: 2, size: 12 })
    expect(globalThis.fetch).toHaveBeenCalledWith('http://localhost:8080/api/products?page=2&size=12')
  })

  it('fetchProducts returns the parsed JSON', async () => {
    globalThis.fetch.mockResolvedValue({ ok: true, json: async () => paged })
    await expect(fetchProducts({ page: 0, size: 12 })).resolves.toEqual(paged)
  })

  it('fetchProducts throws on a non-2xx response', async () => {
    globalThis.fetch.mockResolvedValue({ ok: false, status: 500, json: async () => ({}) })
    await expect(fetchProducts({ page: 0, size: 12 })).rejects.toThrow()
  })

  it('fetchProducts throws when the body is a bare array', async () => {
    globalThis.fetch.mockResolvedValue({ ok: true, json: async () => [{ id: 1 }] })
    await expect(fetchProducts({ page: 0, size: 12 })).rejects.toThrow()
  })

  it('fetchProducts throws when items is missing', async () => {
    globalThis.fetch.mockResolvedValue({ ok: true, json: async () => ({ page: 0, totalPages: 1 }) })
    await expect(fetchProducts({ page: 0, size: 12 })).rejects.toThrow()
  })

  it('fetchCategories returns the parsed JSON when ok', async () => {
    const cats = [{ id: 1, name: 'Electronics' }]
    globalThis.fetch.mockResolvedValue({ ok: true, json: async () => cats })
    await expect(fetchCategories()).resolves.toEqual(cats)
    expect(globalThis.fetch).toHaveBeenCalledWith('http://localhost:8080/api/categories')
  })

  it('fetchCategories throws when not ok', async () => {
    globalThis.fetch.mockResolvedValue({ ok: false, status: 500, json: async () => ({}) })
    await expect(fetchCategories()).rejects.toThrow()
  })
})
