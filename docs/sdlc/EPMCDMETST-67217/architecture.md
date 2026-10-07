# Architecture: EPMCDMETST-67217 (Product lookup by id, service layer)

## 1. Architecture Recommendation
Keep the existing layered Spring Boot design (controller -> service -> repository -> JPA/DB). Add the logic **in the backend service layer only**:
`ProductService.getProductById(Long id)` delegates to the inherited `JpaRepository.findById(id)` and converts an empty `Optional` into an unchecked `ProductNotFoundException`.
- Where logic lives: backend service. The lookup is a data-access concern that must be reusable by a future `GET /api/products/{id}` controller (out of scope); no frontend change (React filtering/sorting in `App.jsx` is untouched).
- No new dependencies, no new repository method (`findById` already exists on `JpaRepository<Product, Long>`), no schema or response-shape change (NFR-1).

## 2. Component Diagram
```
 (future, out of scope)
 ProductController --GET /api/products/{id}--+
                                             v
                                     +----------------+   findById(id)   +-------------------+     +----+
                                     | ProductService |----------------->| ProductRepository |---->| DB |
                                     | getProductById |<-- Optional -----| (JpaRepository)   |     +----+
                                     +-------+--------+                  +-------------------+
                                             | empty Optional
                                             v
                                 throws ProductNotFoundException
                                 (@ResponseStatus NOT_FOUND -> HTTP 404 when a controller exists)
```

## 3. Components and Responsibilities (EPMCDMETST-67217)
| Component | Layer / package | Responsibility |
|---|---|---|
| `ProductService.getProductById(Long)` (modified) | service `org.ecom.productcatalog.service` | Call `productRepository.findById(id)`; return the `Product` or throw `ProductNotFoundException`. |
| `ProductNotFoundException` (new) | `org.ecom.productcatalog.exception` | Unchecked (`RuntimeException`) domain exception; message contains the id (e.g. `Product not found with id: 999999`); annotated `@ResponseStatus(HttpStatus.NOT_FOUND)`. |
| `ProductRepository` (unchanged) | repository | Supplies `findById` via `JpaRepository`. |
| `ProductServiceTest` (new) | `src/test/java/org/ecom/productcatalog/service` | Mockito unit tests with mocked repository. |
| Controller, React frontend | - | Unchanged. |

## 4. Technology Choices
| Choice | Reason | Rejected |
|---|---|---|
| Reuse `findById` + `Optional.orElseThrow` | Satisfies 67219 with no new code surface; idiomatic. | New derived/`@Query` method (redundant); returning `Optional` or null from service (pushes not-found handling to every caller). |
| Unchecked exception with `@ResponseStatus(NOT_FOUND)` | Smallest mechanism that makes a future endpoint return 404 with zero extra mapping; no existing advice class to extend. | `@RestControllerAdvice` (more code and an error-body contract not requested; can be added later); checked exception (noisy signatures). |
| JUnit 5 + Mockito via existing `spring-boot-starter-test` (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`) | Already in `pom.xml`; fast, DB-independent (NFR-3). | `@SpringBootTest`/`@DataJpaTest` (slow, DB-dependent). |
| Keep field injection in `ProductService` | Out of scope to refactor; `@InjectMocks` works with field injection on the public field. | Constructor injection refactor. |

## 5. Data Flow
1. Caller invokes `productService.getProductById(id)`.
2. Service calls `productRepository.findById(id)` -> `Optional<Product>`.
3. Present: return the `Product` (AC1).
4. Empty: throw `ProductNotFoundException` with message "Product not found with id: <id>" (AC2); a future controller lets it propagate and Spring maps it to 404.
5. `id == null`: repository throws `IllegalArgumentException`, propagated unchanged (FR-5). Zero/negative ids follow steps 2-4 (FR-6).

## 6. Low-Level Design
Backend:
```java
// exception/ProductNotFoundException.java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) { super("Product not found with id: " + id); }
}
// ProductService
public Product getProductById(Long id) {
    return productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
}
```
- Entity: unchanged. Repository: unchanged. Controller: none added. Validation: none beyond FR-5/FR-6 (no null guard).
- Errors: `ProductNotFoundException` (404-mapped); `IllegalArgumentException` for null id.
- Frontend: no state, API module, components or `data-testid` changes.

Tests (`ProductServiceTest`, mocked `ProductRepository`):
- found: `when(repo.findById(1L)).thenReturn(Optional.of(product))` -> returns same product; `verify(repo).findById(1L)`.
- not found: `findById(999999L)` returns `Optional.empty()` -> `assertThrows(ProductNotFoundException.class, ...)`, message contains `999999`.
- Optional extras: zero/negative id not found; null id propagates `IllegalArgumentException` (stub the mock to throw).

## 7. API Contract
No change. No new endpoint; existing `GET /api/products` and category endpoints and JSON shapes are unaffected (backward compatible). Future contract: `GET /api/products/{id}` -> 200 `Product` or 404.

## 8. Wireframes
Not applicable (no UI change).

## 9. Error Handling
- Not found: `ProductNotFoundException` (unchecked, id in message, 404 via `@ResponseStatus`).
- Null id: `IllegalArgumentException` from Spring Data, intentionally unhandled.
- No swallowing or logging added in the service.

## 10. Risks
| # | Risk | Mitigation |
|---|---|---|
| R1 | **Product package/directory mismatch**: `model/Product.java` declares `package org.ecom.productcatalog;` (FQN `org.ecom.productcatalog.Product`) while `Category` is in `...model`. Javac/Maven accept this, but IDEs and tooling may misreport, and a future "fix" would break imports in 6 files (DataSeeder, ProductController, Category, ProductRepository, ProductService, ProductControllerIT). | Decision: do NOT move or rename in this Story. New code and tests import `org.ecom.productcatalog.Product` exactly as the repository does. Confirm with a clean `mvn compile` in Step 5; record as tech debt for a separate Story. |
| R2 | No existing service unit tests; Mockito availability. | `spring-boot-starter-test` (JUnit 5, Mockito) is already a test dependency; verify in Step 5. |
| R3 | Field injection on public `productRepository`. | `@InjectMocks` works; leave as is. |
| R4 | `@ResponseStatus` only gives a status/reason, not a structured error body. | Acceptable now; a `@RestControllerAdvice` can be added with the controller Story. |
| R5 | Coverage gate 85% (NFR-4) with small new code. | New code is fully covered by the unit tests; overall coverage checked in Step 7. |
| R6 | `docs/KNOWN-ISSUES.md` does not exist. | Nothing to reconcile. |

## 11. Traceability (FR/AC/Sub-task -> Design)
| Requirement | Design element |
|---|---|
| FR-1, AC1, 67219 | `ProductService.getProductById` using `ProductRepository.findById` (sections 3, 6) |
| FR-2, AC1 | Present `Optional` returns `Product` (section 5 step 3) |
| FR-3, AC2 | `orElseThrow(ProductNotFoundException)` (sections 5, 6) |
| FR-4, AC2 | Unchecked exception, id in message, `@ResponseStatus(NOT_FOUND)` (sections 3, 4, 6) |
| FR-5 | No null guard; `IllegalArgumentException` propagates (sections 5, 9) |
| FR-6 | Zero/negative ids use normal path (section 5) |
| FR-7, AC1/AC2, 67220 | `ProductServiceTest` with mocked repository (section 6) |
| NFR-1 | No schema/API change (sections 1, 7) |
| NFR-2 | Existing `service` package; new `exception` package (section 3) |
| NFR-3 | Mocked repository, ids 1L and 999999L (section 6) |
| NFR-4 | Section 10 R5 |
