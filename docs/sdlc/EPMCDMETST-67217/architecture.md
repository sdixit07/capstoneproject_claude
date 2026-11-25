# Architecture & Design: EPMCDMETST-67217

Story: add a clean service method to fetch a product by id (service method and exception only; no controller endpoint).

## Architecture Recommendation
Backend-only, service layer change. Add `ProductService.getProductById(Long)` that delegates to the existing `ProductRepository.findById` (inherited from `JpaRepository`, so no repository change) and throws a new unchecked `ProductNotFoundException` when the result is empty.

Where the logic lives: the backend service. Not-found handling belongs next to data access so any future caller (the Product Details controller, other services) gets one consistent behaviour. The frontend (`src/App.jsx` filtering/sorting) is untouched; there is no UI, API or contract change in this Story.

## High-Level Diagram
```
(future, out of scope)            this Story
ProductController  ----------->  ProductService.getProductById(id)
 (maps exception to 404)              |  findById(id)
                                      v
                               ProductRepository (JpaRepository<Product, Long>)
                                      |
                                      v
                                 H2 / products

empty Optional -> throw ProductNotFoundException("Product not found with id: <id>")
```

## EPMCDMETST-67217 Components & Responsibilities
| Layer | Component | Responsibility | Change |
|---|---|---|---|
| Service | `ProductService` | Expose `Product getProductById(Long id)`; return product or throw | Modify (add method) |
| Exception | `ProductNotFoundException` | Unchecked signal for "no product with id" | New |
| Repository | `ProductRepository` | `findById` inherited | None |
| Controller | `ProductController` | Not touched | None |
| Frontend | React app | Not touched | None |

## Technology Choices
- Spring Data `findById` + `Optional.orElseThrow`: already available, no new code in the repository. Rejected: a custom `@Query` or derived method (redundant); returning `Optional<Product>` or `null` (requirements fix a `Product` return and an exception).
- Unchecked `RuntimeException` subclass: lets a later `@ExceptionHandler`/`@ResponseStatus` map it to 404 without changing signatures. Rejected: checked exception (forces boilerplate on callers); `@ResponseStatus` on the exception now (would embed HTTP mapping, out of scope); `EntityNotFoundException` (JPA-specific, message not controlled).
- No new dependencies or frameworks (NFR-3).

## Data Flow
1. Caller invokes `getProductById(id)`.
2. Service calls `productRepository.findById(id)`.
3. Present: return the `Product`. Empty: throw `ProductNotFoundException("Product not found with id: " + id)`.
4. Null id: Spring Data throws `IllegalArgumentException`; not wrapped or specified (FR-5).

## Low-Level Design
### Backend
- Note: `Product` lives in package `org.ecom.productcatalog` (not `.model`); `ProductService` uses field injection via a public `productRepository` field, which the existing tests set directly. Keep that style; do not refactor.
- Entity / repository: unchanged.
- Exception: new file `ecom-project/src/main/java/org/ecom/productcatalog/exception/ProductNotFoundException.java`
  ```java
  package org.ecom.productcatalog.exception;
  public class ProductNotFoundException extends RuntimeException {
      public ProductNotFoundException(Long id) {
          super("Product not found with id: " + id);
      }
  }
  ```
- Service method in `ProductService`:
  ```java
  public Product getProductById(Long id) {
      return productRepository.findById(id)
              .orElseThrow(() -> new ProductNotFoundException(id));
  }
  ```
- Validation: none beyond Spring Data's null check. No `@Transactional` needed (single read).
- Errors: `ProductNotFoundException` only; no handler or HTTP mapping in this Story.
- Tests (extend `ProductServiceTest`, Mockito mock repo, no Spring context):
  - AC-1: `when(repo.findById(1L)).thenReturn(Optional.of(product))`; assert `assertSame(product, service.getProductById(1L))`.
  - AC-2: `when(repo.findById(999999L)).thenReturn(Optional.empty())`; `assertThrows(ProductNotFoundException.class, ...)` and assert message `Product not found with id: 999999`.

### Frontend
No change: no state, API module, components or `data-testid`s.

## API Contract
No change. Existing endpoints (`/api/products`, `/api/products/category/{categoryId}`, `/api/categories`) are unaffected; fully backward compatible. `GET /api/products/{id}` is not added here.

## Wireframes
Not applicable (no UI change).

## Error Handling
| Condition | Behaviour |
|---|---|
| Product exists | Returns `Product` |
| Product absent | Throws `ProductNotFoundException("Product not found with id: <id>")` |
| Null id | Spring Data `IllegalArgumentException` (unspecified) |
Until a controller advice exists, a caller would surface the exception as HTTP 500; acceptable since no endpoint uses it yet.

## Risks
- A future controller must map the exception to 404, or callers see 500 (mitigation: follow-up Story; exception is unchecked and dedicated to make this simple).
- `Long` vs primitive: boxed `Long` allows null, hence the documented null behaviour.
- Existing `ProductControllerIT` failures are known and out of scope.

## FR to Design Traceability
| Requirement | Design element |
|---|---|
| FR-1 | `ProductService.getProductById(Long)` signature |
| FR-2 | Delegation to `findById`, return on present |
| FR-3 | `orElseThrow` with message `Product not found with id: <id>` |
| FR-4 | `ProductNotFoundException extends RuntimeException` |
| FR-5 | No explicit null handling; Spring Data behaviour |
| NFR-1 | Mockito-only tests in `ProductServiceTest` |
| NFR-2 | Two tests cover both branches of the new method; exception constructor covered via AC-2 |
| NFR-3 | No dependency changes; existing tests kept |
| AC-1 / AC-2 | The two unit tests above |
