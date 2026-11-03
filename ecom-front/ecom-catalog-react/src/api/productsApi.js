const BASE_URL = 'http://localhost:8080'

export async function fetchProducts({ page, size }) {
  const response = await fetch(`${BASE_URL}/api/products?page=${page}&size=${size}`)
  if (!response.ok) {
    throw new Error(`Failed to load products: HTTP ${response.status}`)
  }
  const data = await response.json()
  if (!data || !Array.isArray(data.items)) {
    throw new Error('Unexpected products response: items is not an array')
  }
  return data
}

export async function fetchCategories() {
  const response = await fetch(`${BASE_URL}/api/categories`)
  if (!response.ok) {
    throw new Error(`Failed to load categories: HTTP ${response.status}`)
  }
  return response.json()
}
