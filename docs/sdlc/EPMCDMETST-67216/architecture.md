# Architecture - EPMCDMETST-67216

**Story:** As a shopper, I can view a product details page so that I can see full information before purchasing
**Jira:** EPMCDMETST-67216 | **Sub-tasks:** EPMCDMETST-67221 (implementation), EPMCDMETST-67222 (verification) - mapping is an approved assumption
**Branch:** feature/EPMCDMETST-67216 | **Input:** `docs/sdlc/EPMCDMETST-67216/requirements.md` (approved)
**Note:** `docs/KNOWN-ISSUES.md` and an architecture template file do not exist in the repo (Not Found); standard structure used.

## 1. Architecture recommendation
Add one read-only endpoint `GET /api/products/{id}` to the existing Spring Boot layers (controller -> service -> repository). The lookup is a primary-key fetch done in the backend (`JpaRepository.findById`). No frontend change: `ProductDetail.jsx` already calls `http://localhost:8080/api/products/${id}` and treats 404 as "not found". No new dependency, entity field, schema change, DTO or exception-handler class.

**Where the logic lives:** backend. An id lookup must not require downloading the whole catalogue and filtering in the browser (the pattern used today in `src/App.jsx` for list filtering); a PK query is cheap and gives correct 404 semantics.

## 2. Component diagram
```
 Browser (React, :5173)
   ProductDetail.jsx --fetch GET /api/products/{id}--+
                                                      v
 +---------------- Spring Boot (:8080) ---------------------------+
 | DispatcherServlet / HandlerMapping (route precedence)          |
 |   ProductController  (/api/products, @CrossOrigin :5173)       |
 |     GET ""                      getAllProducts       (existing) |
 |     GET "category/{categoryId}" getProductByCategory (existing) |
 |     GET "{id}"                  getProductById       (NEW)      |
 |        v                                                        |
 |   ProductService.getProductById(Long)  (NEW)                    |
 |        v                                                        |
 |   ProductRepository.findById(Long)  (inherited JpaRepository)   |
 |        v                                                        |
 |   DB: Product (ManyToOne, EAGER) -> Category                    |
 | Default error handling: DefaultHandlerExceptionResolver (400),  |
 |   ResponseStatusExceptionResolver (404), BasicErrorController   |
 +----------------------------------------------------------------+
```

## 3. EPMCDMETST-67216 components and responsibilities
| Layer | Component | Change | Responsibility |
|-------|-----------|--------|----------------|
| Controller | `ProductController` | add `getProductById(@PathVariable Long id)` mapped `@GetMapping("{id}")` | Bind and type-convert id, delegate, return `Product` (200) |
| Service | `ProductService` | add `getProductById(Long id)` | `productRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id " + id))` |
| Repository | `ProductRepository` | none | `findById` inherited from `JpaRepository<Product, Long>` |
| Model | `Product`, `Category` | none | `Category.products` is already `@JsonIgnore`, so no serialisation recursion (resolves the requirements assumption) |
| Frontend | `ProductDetail.jsx` | none | Existing consumer; 404 -> not found, other non-OK -> error |
| Tests | `ProductControllerIT` (MockMvc, `@SpringBootTest`) | extend (67222) | Cover 200 / 404 / 400 / 0 / negative / out-of-range, plus category route regression |

## 4. Technology choices
| Decision | Choice | Reason | Rejected |
|----------|--------|--------|----------|
| 404 mechanism | `ResponseStatusException(NOT_FOUND)` in service | Built into Spring; yields default Boot error JSON (FR-4); no new class | Custom `NotFoundException` + `@ControllerAdvice` (violates "no custom error DTO/handler", scope creep); `ResponseEntity.notFound()` (empty body, not default error JSON) |
| 400 mechanism | Framework default: `@PathVariable Long` conversion failure -> `MethodArgumentTypeMismatchException` -> 400 | Zero code (FR-5); covers `abc` and out-of-Long-range | Manual `String` parse in controller; Bean Validation (`@Positive` would turn 0/-1 into 400, violating FR-6) |
| Return type | `Product` entity directly | Matches existing endpoints; full entity incl. category (clarification 3) | New DTO (unneeded; no new fields) |
| Lookup | `findById` | PK query, already available | New derived query; in-memory filter of `findAll()` |
| Dependencies | none added | Stack respected | - |

## 5. Data flow
1. Shopper opens the detail page; `ProductDetail.jsx` issues `GET /api/products/{id}`.
2. Spring routes the path (section 7) and converts `{id}` to `Long`.
3. Controller -> `ProductService.getProductById(id)` -> `ProductRepository.findById(id)` -> SQL select by PK (Category loaded eagerly, ManyToOne default).
4. Found: Jackson serialises Product (id, name, description, imageUrl, price, category{id,name}) -> 200.
5. Not found: `ResponseStatusException` -> 404 error JSON. Bad format: conversion exception before the controller body runs -> 400 error JSON.

```
Client -> Controller -> Service -> Repository -> DB
   200 <--- Product <--- Optional<Product> present
   404 <--- ResponseStatusException <--- Optional.empty
   400 <--- MethodArgumentTypeMismatchException (never reaches service)
```

## 6. Low-level design
**Backend**
- Controller:
  ```java
  @GetMapping("{id}")
  public Product getProductById(@PathVariable Long id) { return productService.getProductById(id); }
  ```
- Service: method per section 3, importing `org.springframework.web.server.ResponseStatusException` and `org.springframework.http.HttpStatus`. Keep the existing field-injection style (NFR-3).
- Validation: type conversion only; 0 and negatives are valid Longs and result in 404 (FR-6).
- Entity/repository: unchanged. Read-only, no write transaction (NFR-2).
- CORS: class-level `@CrossOrigin("http://localhost:5173")` already applies to the new method (FR-8).

**Frontend:** no state, API module, component or `data-testid` changes.

## 7. Route precedence vs `category/{categoryId}`
`GET /api/products/category/5` has two path segments after `/products`; `{id}` matches a single segment, so `"{id}"` cannot capture it. Mappings `""`, `"category/{categoryId}"` and `"{id}"` are disjoint by segment count, so there is no ambiguity (FR-7). Edge case: `GET /api/products/category` (single segment) matches `{id}` with value `category`, fails Long conversion and returns 400 (before: no handler, 404/500 per Boot defaults). This is acceptable and listed in the risks. A regression test must assert `/category/{id}` still returns the list.

## 8. API contract
`GET /api/products/{id}` (backward compatible; additive)
- 200 `application/json`: `{"id":1,"name":"..","description":"..","imageUrl":"..","price":9.99,"category":{"id":1,"name":".."}}`
- 404: default Boot error JSON `{timestamp,status:404,error:"Not Found",message,path}` for well-formed absent ids, including 0 and -1.
- 400: default Boot error JSON `{timestamp,status:400,error:"Bad Request",message,path}` for `abc` and values beyond `Long.MAX_VALUE`.
Existing endpoints unchanged.

## 9. Error handling
| Case | Example | Status | Source |
|------|---------|--------|--------|
| Exists | `/api/products/1` | 200 | controller |
| Missing | `999999`, `0`, `-1` | 404 | `ResponseStatusException` |
| Non-numeric | `abc` | 400 | `MethodArgumentTypeMismatchException` |
| Out of range | `99999999999999999999` | 400 | same |

No custom handler; the body shape is Boot's default. Caveat: Boot 3.x omits `message` by default unless `server.error.include-message` is set. The developer must check the effective config; if `message` is absent, either set only that property (minimal fix) or accept an empty message and record it in implementation notes.

## 10. Wireframe (consumer, unchanged)
```
+------------------------------+      +--------------------------+
| [image]  Product name        |      |  Product not found       |
|          Description         |      |  (404)                   |
|          Price   [Category]  |      +--------------------------+
+------------------------------+      Other non-OK: generic error
```

## 11. Risks
| Risk | Mitigation |
|------|-----------|
| Boot error JSON lacks `message` by default | Verify in test/app; section 9 |
| `/api/products/category` now 400 | Documented, low impact; add test |
| `Product` lives in root package, inconsistent with `model` | Do not move; out of scope |
| Future lazy-loading of Category could break serialisation | Currently EAGER; no change |
| Sub-task mapping is an assumption | Approved; adjust if Jira titles differ |

## 12. Traceability FR -> design
| Req | Design element |
|-----|----------------|
| FR-1 | `@GetMapping("{id}")` with `@PathVariable Long` |
| FR-2, FR-3 | Return `Product` entity from `findById` |
| FR-4, FR-6 | `ResponseStatusException(404)`; no `@Positive` validation |
| FR-5 | Framework type-mismatch -> 400 |
| FR-7 | Section 7 disjoint routes; regression test |
| FR-8 | Class-level `@CrossOrigin` unchanged |
| NFR-1 | No contract break; 200 JSON, distinct 404 |
| NFR-2 | `findById` only |
| NFR-3 | Same layering, injection and style |
| NFR-4 | `ProductControllerIT` additions (67222) |
| AC-1, AC-2, AC-3 | 67221: sections 3, 6, 8, 9 |
| AC-4 | 67222: verification against ProductDetail.jsx contract |
