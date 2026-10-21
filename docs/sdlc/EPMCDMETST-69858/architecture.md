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
| Service | `service/ProductService` | modify | Add `Page<Product> getProducts(Pageable)` -> `productRepository.findAll(pageable)`. Existing `getAllProducts()` may stay. |
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
| Range validation | Explicit checks in controller throwing `ResponseStatusException(BAD_REQUEST, msg)` | No new dependency (`spring-boot-starter-validation` is absent from `pom.xml`); `message` shows under existing `server.error.include-message=always` (NFR-2) | Bean Validation starter (new dependency, not forced); `@ControllerAdvice` (extra surface for three checks) |
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
Edge: `PageRequest` computes offset as `long`, so a huge `int` page (e.g. `Integer.MAX_VALUE`) returns an empty page rather than overflowing. A value beyond int range (e.g. `page=99999999999`) fails binding and returns 400, which satisfies FR-8.

**Validation and errors**

| Input | Result |
|---|---|
| omitted page/size | 200, page 0, size 10 |
| `page<0`, `size<1`, `size>100` | 400, `message` states the rule |
| `page=abc` / `size=abc` | 400 (type mismatch, Spring default) |
| `page` past last | 200, `items: []`, correct totals |
| empty table | 200, `items: []`, `totalItems 0`, `totalPages 0` |

Error body is Spring Boot's standard JSON (`timestamp, status, error, message, path`); `message` appears only under the local-dev setting (NFR-2).

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
- No 404/500 paths are introduced.

## 10. Risks

| # | Risk | Mitigation |
|---|---|---|
| R1 | Front end (`App.jsx`) breaks against the envelope | Accepted and documented (requirements Known Limitations); follow-up Story |
| R2 | Existing `src/test/.../controller/ProductControllerIT.java` asserts a bare array (`hasSize(27)`) and uses `search`, `sort`, `minPrice`, `maxPrice`, `categoryId` params that the current controller does not implement. It is not part of the baseline run (1 test; surefire does not match `*IT`) and mostly fails against current code already. | Do not weaken it. Add pagination tests in a separate class named so surefire runs it (e.g. `ProductPaginationTest`). Tester should report the stale IT as a pre-existing discrepancy, not a defect of this Story. |
| R3 | Tests calling `deleteAll()` in a shared `@SpringBootTest` context contaminate `DataSeeder` data | New tests create their own data in `@BeforeEach` and assert on it, not on seed counts. AC-10 verified with a dedicated check of the seeded state (e.g. invoking `DataSeeder.run` then counting, or `@DirtiesContext`). |
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
| FR-9 | Spring Data empty page with real totals |
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
