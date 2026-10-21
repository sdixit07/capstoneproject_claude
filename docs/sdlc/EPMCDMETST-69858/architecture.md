# Architecture & Design: EPMCDMETST-69858 - Pagination for GET /api/products

- **Story**: EPMCDMETST-69858 (backend, pagination). Source: `requirements.md` (commit 2b199e4).
- **Baseline issues**: KI-1..KI-3 in `docs/KNOWN-ISSUES.md` are front-end only; not touched here.

## 1. Architecture Recommendation

**Pagination lives in the backend**, in the database query, via Spring Data `Pageable` (FR-4, NFR-1).

Justification:
- Today the browser downloads the whole catalog and filters/sorts in `src/App.jsx`. That does not scale and defeats the Story's purpose ("better performance"). Slicing in the browser would still transfer every row.
- Only the server can return `totalItems`/`totalPages` cheaply (one `COUNT` query) and keep ordering deterministic (`ORDER BY id`).
- The front end is explicitly not changed in this Story. Front-end paging UI, and the client-side filter/sort in `App.jsx`, are left for a follow-up Story.

Approach: keep the existing layers (Controller -> Service -> Repository). The controller validates and parses `page`/`size`, builds a `PageRequest` sorted by `id ASC`, the service calls `ProductRepository.findAll(Pageable)` (already inherited from `JpaRepository`), and the controller maps the `Page<Product>` into a small response DTO (`PagedResponse<T>`) so the JSON contract is explicit and does not depend on Spring's `PageImpl` serialization.

## 2. Component Diagram

```
 Browser / HTTP client
        |  GET /api/products?page=&size=
        v
 +-------------------------------+
 | ProductController             |  parse + validate page/size (400)
 |  getAllProducts(page,size)    |  build PageRequest(page,size,Sort id ASC)
 +---------------+---------------+
                 | Pageable
                 v
 +-------------------------------+
 | ProductService                |  getProducts(Pageable): Page<Product>
 +---------------+---------------+
                 v
 +-------------------------------+
 | ProductRepository             |  findAll(Pageable)  (inherited)
 +---------------+---------------+
                 v
   H2: SELECT ... ORDER BY id LIMIT ? OFFSET ?   +   SELECT COUNT(*)

 Controller maps Page<Product> -> PagedResponse<Product>
   {items, page, size, totalItems, totalPages}
 DataSeeder (startup) -> seeds >= 25 products in 3 categories
```

## 3. EPMCDMETST-69858 Components & Responsibilities

| Layer | Component | Change | Responsibility |
|---|---|---|---|
| Controller | `controller/ProductController` | modify | `getAllProducts` takes `@RequestParam(defaultValue="0") int page`, `@RequestParam(defaultValue="10") int size`; validates ranges; returns `PagedResponse<Product>`. `getProductByCategory` unchanged (FR-11). |
| Service | `service/ProductService` | modify | Add `Page<Product> getProducts(Pageable)` -> `productRepository.findAll(pageable)` and `long countProducts()` -> `productRepository.count()` (D-1 guard). Existing `getAllProducts()` stays unchanged (D-5). |
| Repository | `repository/ProductRepository` | none | Inherited `findAll(Pageable)` is sufficient. |
| DTO | `dto/PagedResponse<T>` (new package `org.ecom.productcatalog.dto`) | new | Immutable envelope with `items`, `page`, `size`, `totalItems`, `totalPages`. A Java `record` (Java 21) with static factory `from(Page<T>)`. |
| Config | `config/DataSeeder` | modify | Seed >= 25 products across Electronics, Clothing, Home and Kitchen (FR-10). Keep the existing 4 products; add 21+ more. |
| Model | `Product`, `Category` | none | NFR-4: no entity/schema change. Note `Product` lives in package `org.ecom.productcatalog`, not `model`; do not move it. |
| Tests | new pagination test class | add | See section 10 (R2, R3). |
| Front end | all | none | Out of scope. |

## 4. Technology Choices

| Decision | Choice | Reason | Rejected |
|---|---|---|---|
| Paging mechanism | Spring Data `Pageable` + `findAll(Pageable)` | Required by FR-4; DB-level `LIMIT/OFFSET` + `COUNT` (NFR-1) | Slicing `findAll()` in memory (violates NFR-1) |
| Response shape | Custom `PagedResponse` record | Exact contract of FR-3; avoids `PageImpl` JSON (`content`, `number`, `pageable`, ...) and its unstable-serialization warning | Returning `Page<Product>` directly; `PagedModel` (needs HATEOAS dependency) |
| Param binding | `@RequestParam int` with `defaultValue` | Non-numeric input raises `MethodArgumentTypeMismatchException`, which Spring Boot maps to 400 (FR-8). Missing params get defaults (FR-2) | `Pageable` argument resolver / `@PageableDefault`: clamps oversized values and falls back on bad input instead of returning 400, so it cannot meet FR-6/7/8 |
| Range validation | Explicit checks in controller throwing `ResponseStatusException(BAD_REQUEST, msg)` | No new dependency (`spring-boot-starter-validation` is absent from `pom.xml`); `message` appears only if `server.error.include-message=always` is set; it is NOT currently in `application.properties` despite CLAUDE.md (D-2), so tests assert status only (NFR-2) | Bean Validation starter (new dependency, not forced); `@ControllerAdvice` (extra surface for three checks) |
| Sorting | `Sort.by(ASC, "id")` fixed in controller | FR-5; client cannot override, paging is deterministic | Client-supplied `sort` param (out of scope) |

No new dependencies or frameworks.

## 5. Data Flow

1. Client sends `GET /api/products?page=2&size=10`.
2. Spring binds `page`, `size` to `int` (defaults 0 / 10). Non-numeric -> 400 before the controller body runs.
3. Controller checks `page >= 0` and `1 <= size <= 100`; otherwise throws `ResponseStatusException(400)`.
4. Controller builds `PageRequest.of(page, size, Sort.by(ASC, "id"))` and calls `productService.getProducts(pageable)`.
5. Repository runs the paged `SELECT ... ORDER BY id LIMIT/OFFSET` and a `COUNT`.
6. Controller wraps the result with `PagedResponse.from(page)` and returns 200.
7. Past-the-end page: Spring Data returns an empty page with correct totals; envelope has `items: []` (FR-9).

## 6. Low-Level Design

### 6.1 Backend

**Entity**: unchanged. `Product.category` is `@ManyToOne` (eager by default), so page queries also load categories; acceptable for at most 100 rows (R4).

**Repository**: no code change.

**Service**
```java
public Page<Product> getProducts(Pageable pageable) {
    return productRepository.findAll(pageable);
}
```

**DTO**
```java
public record PagedResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    public static <T> PagedResponse<T> from(Page<T> p) {
        return new PagedResponse<>(p.getContent(), p.getNumber(), p.getSize(),
                                   p.getTotalElements(), p.getTotalPages());
    }
}
```
`page` and `size` echo the requested values, so a past-the-end request reports the requested page, and `size` is the page size, not the item count.

**Controller**
```java
private static final int MAX_SIZE = 100;

@GetMapping
public PagedResponse<Product> getAllProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    if (page < 0)
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0");
    if (size < 1 || size > MAX_SIZE)
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and 100");
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
    return PagedResponse.from(productService.getProducts(pageable));
}
```
Edge (revised, D-1): `PageRequest` holds a `long` offset, but Spring Data JPA 3.4 `findAll(Pageable)` converts it to `int` for `setFirstResult` and throws `InvalidDataAccessApiUsageException` when it exceeds `Integer.MAX_VALUE`. So the controller guards before querying: if `(long) page * size > Integer.MAX_VALUE`, it skips the paged query and returns **200 with an empty envelope** (`items: []`, requested `page`/`size` echoed, real `totalItems`/`totalPages`), consistent with FR-9. Totals come from `productService.countProducts()` (new, `productRepository.count()`), with `totalPages = ceil(totalItems / size)`. A value beyond int range (e.g. `page=99999999999`) fails binding and returns 400 (FR-8).
```java
if ((long) page * size > Integer.MAX_VALUE) {
    long total = productService.countProducts();
    int totalPages = (int) ((total + size - 1) / size);
    return new PagedResponse<>(List.of(), page, size, total, totalPages);
}
```
This guard goes after the range checks and before `PageRequest` is built. A test is required (MockMvc, e.g. `page=2147483647&size=10` -> 200, empty `items`, `totalItems` equal to the row count, `page` echoed).

**Validation and errors**

| Input | Result |
|---|---|
| omitted page/size | 200, page 0, size 10 |
| `page<0`, `size<1`, `size>100` | 400 (exception message states the rule; appears in body only if `include-message` is enabled, see D-2) |
| `(long) page * size > Integer.MAX_VALUE` | 200, `items: []`, requested page/size echoed, real totals (D-1) |
| `page=abc` / `size=abc` | 400 (type mismatch, Spring default) |
| `page` past last | 200, `items: []`, correct totals |
| empty table | 200, `items: []`, `totalItems 0`, `totalPages 0` |

Error body is Spring Boot's standard JSON (`timestamp, status, error, path`, plus `message` only if `server.error.include-message` is enabled). The committed `application.properties` does not set it, contrary to CLAUDE.md. This Story does not add it; tests assert HTTP status only, never `message` (D-2, NFR-2).

**DataSeeder**: add a small private helper (name, description, price, category) and `saveAll` 25 or more products. Suggested split: 9 Electronics, 8 Clothing, 8 Home and Kitchen = 25, keeping the original 4 as the first rows. IDENTITY ids ascend in insertion order. The initial `deleteAll()` calls stay.

### 6.2 Frontend
No changes (clarification 1). No `data-testid`s added. Known consequence: `App.jsx` calls `products.filter(...)` on an object and will fail until a follow-up Story consumes `items`.

## 7. API Contract

`GET /api/products?page={int >= 0}&size={1..100}` (both optional)

200 response:
```json
{ "items": [ { "id": 1, "name": "...", "description": "...", "imageUrl": "...", "price": 599.99,
               "category": { "id": 1, "name": "Electronics" } } ],
  "page": 0, "size": 10, "totalItems": 25, "totalPages": 3 }
```
400: invalid `page`/`size` as tabled above.

Backward compatibility: **intentionally breaking** for `/api/products` (bare array -> envelope), per clarification 1. `GET /api/products/category/{id}` and `GET /api/categories` are unchanged.

## 8. Wireframes

No UI change. Wire-level behaviour only:
```
GET /api/products            -> { items:[10], page:0, size:10, totalItems:25, totalPages:3 }
GET /api/products?page=2     -> { items:[5],  page:2, size:10, totalItems:25, totalPages:3 }
GET /api/products?page=9     -> { items:[],   page:9, size:10, totalItems:25, totalPages:3 }
GET /api/products?size=101   -> 400
```

## 9. Error Handling Summary
- Validation failures use `ResponseStatusException(400)`; type-binding failures use Spring's default 400.
- No new exception types and no global advice.
- No 404 paths are introduced. No 500 path for large `page` values: the controller guard in 6.1 returns an empty 200 envelope when `(long) page * size > Integer.MAX_VALUE`, avoiding the Spring Data offset-overflow exception (D-1).

## 10. Risks

| # | Risk | Mitigation |
|---|---|---|
| R1 | Front end (`App.jsx`) breaks against the envelope | Accepted and documented (requirements Known Limitations); follow-up Story |
| R2 | Existing `src/test/.../controller/ProductControllerIT.java` asserts a bare array (`hasSize(27)`) and uses `search`, `sort`, `minPrice`, `maxPrice`, `categoryId` params that the current controller does not implement. It is not part of the baseline run (1 test; surefire does not match `*IT`) and mostly fails against current code already. | Do not weaken it. Add pagination tests in a separate class named so surefire runs it (e.g. `ProductPaginationTest`). Tester should report the stale IT as a pre-existing discrepancy, not a defect of this Story. |
| R3 | Tests calling `deleteAll()` in a shared `@SpringBootTest` context contaminate `DataSeeder` data | New tests create their own data in `@BeforeEach` and assert on it, not on seed counts; data-mutating test classes use `@DirtiesContext` (or recreate data in `@BeforeEach`). AC-10 is verified by one dedicated test that calls `dataSeeder.run()` and asserts `count() >= 25` and that all 3 categories have products (D-3). |
| R4 | Eager `@ManyToOne` category may cause extra selects for up to 100 rows | Acceptable at this scale (3 categories, session cache). Revisit if catalog grows |
| R5 | `CLAUDE.md` lists `GET /api/products/{id}`, which does not exist | Out of scope per instruction; not addressed. `CLAUDE.md` should later document the envelope |
| R6 | Offset paging slows on very deep pages | Not relevant at this scale; keyset paging is a future option |

## 11. FR -> Design Traceability

| Requirement | Design element |
|---|---|
| FR-1 | `@RequestParam page,size` on `ProductController.getAllProducts` |
| FR-2 | `defaultValue` "0"/"10"; always returns `PagedResponse` |
| FR-3 | `PagedResponse` record and `from(Page)` |
| FR-4 | `PageRequest` -> `ProductService.getProducts(Pageable)` -> `findAll(Pageable)` |
| FR-5 | `Sort.by(ASC,"id")` in `PageRequest` |
| FR-6 | explicit `page<0`, `size<1` checks -> 400 |
| FR-7 | `size>100` check -> 400 |
| FR-8 | `int` binding, Spring type-mismatch 400 |
| FR-9 | Spring Data empty page with real totals; overflow guard returns empty envelope (D-1) |
| FR-10 | `DataSeeder` extended to 25 products / 3 categories |
| FR-11 | category method untouched |
| NFR-1 | DB `LIMIT/OFFSET` + `COUNT` via Pageable |
| NFR-2 | `ResponseStatusException` + existing `include-message` setting |
| NFR-3 | new tests (R2, R3); each AC maps to at least one test case |
| NFR-4 | no entity/schema change |
| AC-1, AC-2, AC-5, AC-9 | envelope, defaults, id ordering, using test data of >= 25 rows |
| AC-3, AC-4, AC-6, AC-8 | validation matrix in 6.1 |
| AC-7 | past-the-end row in 6.1 |
| AC-10 | `DataSeeder` change |

## 12. Changes after design review

All six decisions in `design-review.md` (D-1..D-6) were accepted by the user.

| Decision | What changed | Where |
|---|---|---|
| D-1 | Removed the incorrect claim that a huge `page` safely yields an empty page. Added controller guard: `(long) page * size > Integer.MAX_VALUE` returns 200 with an empty envelope (empty `items`, real `totalItems`/`totalPages`, requested `page`/`size` echoed), consistent with FR-9. Added `ProductService.countProducts()`. A MockMvc test is required (`page=2147483647&size=10`). | 3 (Service row), 6.1 (Edge paragraph, validation table), 9, 11 (FR-9) |
| D-2 | Documented that `server.error.include-message` is not set in `application.properties` (CLAUDE.md is inaccurate); property not added in this Story; tests assert status only. | 4 (range validation row), 6.1 (validation table, error body note) |
| D-3 | Specified AC-10 verification: one dedicated test calls `dataSeeder.run()` and asserts `count() >= 25` and 3 categories with products; data-mutating test classes use `@DirtiesContext` or recreate data in `@BeforeEach`. | 10 (R3) |
| D-4 | Agreed; no architecture change. Tests for AC-9 assert strictly ascending ids within and across pages, no duplicates or gaps. | planner/tester guidance |
| D-5 | Agreed; no architecture change beyond stating `getAllProducts()` is retained unchanged (clarifying note only). | 3 (Service row) |
| D-6 | Agreed; no architecture change. CLAUDE.md untouched; gap listed under PR "Known Limitations". | n/a (R5 unchanged) |
