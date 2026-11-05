import { useEffect, useRef, useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'
import './App.css'
import 'bootstrap/dist/css/bootstrap.min.css'
import ProductList from './ProductList'
import CategoryFilter from './CategoryFilter'
import Pagination from './Pagination'
import { fetchCategories, fetchProducts } from './api/productsApi'
import { resolvePageResult } from './paginationState'

const PAGE_SIZE = 12

function App() {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [sortorder, setSortOrder] = useState("asc");
  const [page, setPage] = useState(0);
  const [loadedPage, setLoadedPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [retryToken, setRetryToken] = useState(0);

  // Mirrors loadedPage so the effect can read it without being a dependency.
  const loadedPageRef = useRef(0);
  // Set when a failed request rolls page back; the re-run must not clear the error (N-1).
  const skipNextRunRef = useRef(false);

  useEffect(() => {
    fetchCategories()
      .then(data => setCategories(data))
      .catch(e => console.error(e.message))
  }, []);

  useEffect(() => {
    if (skipNextRunRef.current) {
      skipNextRunRef.current = false;
      setLoading(false);
      return undefined;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    fetchProducts({ page, size: PAGE_SIZE })
      .then(data => {
        if (cancelled) return;
        const result = resolvePageResult(page, data.totalPages);
        if (result.clamp) {
          setPage(result.page);
          return;
        }
        setProducts(data.items);
        setTotalPages(data.totalPages);
        setLoadedPage(page);
        loadedPageRef.current = page;
        setLoading(false);
      })
      .catch(e => {
        if (cancelled) return;
        console.error(e.message);
        setError('Could not load products.');
        if (page !== loadedPageRef.current) {
          skipNextRunRef.current = true;
          setPage(loadedPageRef.current);
        }
        setLoading(false);
      });
    return () => { cancelled = true; };
  }, [page, retryToken]);

  const goPrev = () => setPage(loadedPage - 1);
  const goNext = () => setPage(loadedPage + 1);
  const retry = () => setRetryToken(t => t + 1);

  const handleSearchChange = (event) =>{
    setSearchTerm(event.target.value);
  };
  
  const handleSortChange = (event) =>{
    setSortOrder(event.target.value);
  };

  const handleCategorySelect = (categoryId) =>{
    setSelectedCategory(categoryId ? Number(categoryId) : null);
  };

  const filteredProducts = products
        .filter( product => {
          return(
            (selectedCategory ? product.category.id === selectedCategory : true)
          ) && product.name.toLowerCase().includes(searchTerm.toLowerCase())
        })
        .sort((a,b) => {
          if(sortorder === "asc"){
            return a.price - b.price
          } else{
            return b.price - a.price
          }
        });

  const isEmptyCatalog = !loading && !error && totalPages === 0;

  return (
    <div className='container'>
      <h1>Product Catalog</h1><br/>

      <div className='row align-items-center mb-4'>
        <div className='col-md-3 col-sm-12 mb-2'>
          <CategoryFilter categories={categories}  onSelect={handleCategorySelect}/>
        </div>

        <div className='col-md-5 col-sm-12 mb-2'>
        <input 
          type='text'
          className='form-control'
          placeholder='Search for products'
          onChange={handleSearchChange} />
        </div>

        <div className='col-md-4 col-sm-12 mb-2'>
          <select className='form-control' onChange={handleSortChange}>
            <option value="asc">Sort by Price: Low to High</option>
            <option value="desc">Sort by Price: High to Low</option>
          </select>
        </div>
      </div>


      {error && (
        <div className='catalog-error' role='alert' data-testid='catalog-error'>
          <span>{error}</span>
          <button type='button' className='btn btn-outline-danger btn-sm' data-testid='catalog-retry' onClick={retry}>
            Retry
          </button>
        </div>
      )}

      <div aria-busy={loading ? 'true' : 'false'}>
        {loading ? (
          <p data-testid='catalog-loading'>Loading products...</p>
        ) : filteredProducts.length ? (
          //Display products
          <ProductList products={filteredProducts} />
        ) : isEmptyCatalog ? (
          <p data-testid='catalog-empty'>No products available.</p>
        ) : error && products.length === 0 ? null : (
          <p>No prodcuts to display.</p>
        )}
        {!loading && filteredProducts.length === 0 && products.length > 0 && totalPages > 1 && (
          <p className='catalog-limited-hint' data-testid='catalog-limited-hint'>
            Search and filters apply to the current page only. Other pages may contain matches.
          </p>
        )}
      </div>

      <Pagination
        page={loadedPage}
        totalPages={totalPages}
        disabled={loading}
        onPrev={goPrev}
        onNext={goNext}
      />

    </div>
  )
}

export default App
