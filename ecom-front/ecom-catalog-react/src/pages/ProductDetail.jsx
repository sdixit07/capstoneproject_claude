import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

const ProductDetail = () => {
  const { id } = useParams()

  const [loading, setLoading] = useState(true)
  const [product, setProduct] = useState(null)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState(null)

  const load = async () => {
    setLoading(true)
    setError(null)
    setNotFound(false)

    try {
      const res = await fetch(`http://localhost:8080/api/products/${id}`)
      if (res.status === 404) {
        setNotFound(true)
        setProduct(null)
        return
      }
      if (!res.ok) {
        throw new Error(`Request failed with status ${res.status}`)
      }
      const data = await res.json()
      setProduct(data)
    } catch (e) {
      setError(e?.message ?? 'Failed to load product')
      setProduct(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  if (loading) {
    return (
      <div className="container">
        <h1>Product Details</h1>
        <p>Loading...</p>
      </div>
    )
  }

  if (notFound) {
    return (
      <div className="container">
        <h1>Product not found</h1>
        <p>The product you are looking for does not exist.</p>
        <Link to="/" className="btn btn-secondary">Back to catalog</Link>
      </div>
    )
  }

  if (error) {
    return (
      <div className="container">
        <h1>Unable to load product</h1>
        <p>{error}</p>
        <div className="d-flex gap-2">
          <button className="btn btn-primary" onClick={load}>Retry</button>
          <Link to="/" className="btn btn-secondary">Back to catalog</Link>
        </div>
      </div>
    )
  }

  return (
    <div className="container">
      <h1>Product Details</h1>
      <div className="card">
        <img src={product.imageUrl} className="card-img-top" alt={product.name} />
        <div className="card-body">
          <h3 className="card-title">{product.name}</h3>
          <p className="card-text">{product.description}</p>
          <p className="card-text"><strong>${product.price}</strong></p>
          {product.category && (
            <p className="card-text">
              <strong>Category:</strong> {product.category.name}
            </p>
          )}
          <Link to="/" className="btn btn-secondary">Back to catalog</Link>
        </div>
      </div>
    </div>
  )
}

export default ProductDetail
