# Implementation Plan: EPMCDMETST-69858 - Pagination for GET /api/products

- **Sources**: `requirements.md` (2b199e4), `architecture.md` final (1688b94, incl. section 12), `design-review.md` (2bb93b9, D-1..D-6).
- **Scope**: backend only. Tasks derive from `architecture.md`. Paths are relative to the repo root; Java base package `org.ecom.productcatalog` under `ecom-project/src/main/java/`.
- **Effort scale**: S under 30 min, M 30-90 min, L over 90 min.

## Out of scope (do not touch)
- `ecom-project/src/test/java/org/ecom/productcatalog/controller/ProductControllerIT.java` (known-broken, asserts a bare array; do not weaken, fix or delete; report as pre-existing, R2).
- `docs/KNOWN-ISSUES.md` KI-1..KI-3 (front end only).
- All of `ecom-front/` (including `App.jsx`).
- `CLAUDE.md` (D-6; gap goes under PR Known Limitations).
- `application.properties`: do not add `server.error.include-message` (D-2).
- `ProductService.getAllProducts()`: keep unchanged (D-5). `ProductRepository`, `Product`, `Category`, `getProductByCategory`: no change.
- Package note: the repo has `Product` under `model/` (architecture.md says the root package); developer should use whatever the current imports are and not move it.

## Tasks (ordered by dependency, then priority)

| ID | Pri | Task | Files | FR / AC | Depends on | Effort |
|---|---|---|---|---|---|---|
| T-1 | P1 | Create `PagedResponse<T>` record (`items`, `page`, `size`, `totalItems`, `totalPages`) with static `from(Page<T>)` in new package `dto`. | `ecom-project/src/main/java/org/ecom/productcatalog/dto/PagedResponse.java` (new) | FR-3, FR-4 | none | S |
| T-2 | P1 | Add `Page<Product> getProducts(Pageable)` -> `productRepository.findAll(pageable)` and `long countProducts()` -> `productRepository.count()` (D-1 guard). Leave `getAllProducts()` untouched (D-5). | `.../service/ProductService.java` | FR-4, FR-9, NFR-1 | none | S |
| T-3 | P1 | Extend `DataSeeder`: private helper (name, description, price, category) and `saveAll` of 25+ products (suggested 9 Electronics, 8 Clothing, 8 Home and Kitchen), keeping the original 4 first; keep the initial `deleteAll()` calls; no schema/entity change. | `.../config/DataSeeder.java` | FR-10, AC-10, NFR-4 | none | M |
| T-4 | P1 | Modify `ProductController.getAllProducts(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size)` returning `PagedResponse<Product>`. Order: (a) `page<0` -> `ResponseStatusException(400)`; (b) `size<1 || size>100` -> 400 (`MAX_SIZE=100`); (c) D-1 guard: if `(long) page * size > Integer.MAX_VALUE`, call `productService.countProducts()`, `totalPages = (total + size - 1) / size`, return `new PagedResponse<>(List.of(), page, size, total, totalPages)` (200); (d) else `PageRequest.of(page, size, Sort.by(ASC,"id"))` -> `productService.getProducts` -> `PagedResponse.from`. Leave `getProductByCategory` unchanged (FR-11). Add imports. | `.../controller/ProductController.java` | FR-1, FR-2, FR-5, FR-6, FR-7, FR-8, FR-9, FR-11 / AC-1..AC-9 | T-1, T-2 | M |
| T-5 | P1 | Unit test: `PagedResponse.from(Page)` maps content, number, size, totalElements, totalPages; empty page case. | `ecom-project/src/test/java/org/ecom/productcatalog/dto/PagedResponseTest.java` (new) | FR-3 | T-1 | S |
| T-6 | P1 | Unit test (Mockito) for `ProductService`: `getProducts` delegates to `findAll(pageable)`; `countProducts` delegates to `count()`; `getAllProducts` still works (regression, D-5). | `ecom-project/src/test/java/org/ecom/productcatalog/service/ProductServiceTest.java` (new) | FR-4, D-1, D-5 | T-2 | S |
| T-7 | P1 | MockMvc/SpringBootTest class `ProductPaginationTest` (named to match surefire; NOT `*IT`). Creates its own data in `@BeforeEach` (`deleteAll()` then save a known set of >= 25 products across 3 categories) and asserts against that data, not seed counts (D-3, R3); annotate `@DirtiesContext` (D-3). Cases, status asserted only, never `message` (D-2): see "Test cases" below. | `ecom-project/src/test/java/org/ecom/productcatalog/controller/ProductPaginationTest.java` (new) | AC-1..AC-9, FR-1..FR-9 | T-4 | L |
| T-8 | P1 | Seed verification test (AC-10, D-3): dedicated class, `@SpringBootTest` + `@DirtiesContext`, autowires `DataSeeder`, calls `dataSeeder.run()` explicitly, asserts `productRepository.count() >= 25` and that each of the 3 categories has at least one product (via `findByCategoryId`/existing repository method or by grouping `findAll()`). Kept separate from T-7 so destructive data setup in T-7 cannot affect it. | `ecom-project/src/test/java/org/ecom/productcatalog/config/DataSeederTest.java` (new) | FR-10, AC-10 | T-3 | M |
| T-9 | P2 | Regression test that `GET /api/products/category/{id}` still returns a bare JSON array (unpaginated) and `GET /api/categories` unchanged. Can live in `ProductPaginationTest` or a small separate class. | `.../controller/ProductPaginationTest.java` (or `ProductCategoryEndpointTest.java`) | FR-11 | T-4, T-7 | S |
| T-10 | P2 | Run `./mvnw clean verify` from `ecom-project/`; confirm new tests pass, `EcomProjectApplicationTests` still passes, and the JaCoCo report is generated at `ecom-project/target/site/jacoco/`. Record in `implementation-notes.md` (Step 5). Report `ProductControllerIT` as a pre-existing discrepancy only. | none (execution) | NFR-3 | T-5, T-6, T-7, T-8, T-9 | S |

### Test cases for T-7 (Step 5 unit/MockMvc tasks)
Using the data created in `@BeforeEach` (N >= 25 products, known N):
1. Default (no params): 200; `page=0`, `size=10`, `items` length 10, `totalItems=N`, `totalPages=ceil(N/10)` (AC-5, FR-2).
2. `page=0&size=10`: 200, `items<=10`, `totalItems>10`, `totalPages>1` (AC-1).
3. `page=2&size=10`: 200, `items<=10`, `page=2` (AC-2).
4. `page=-1&size=10` -> 400 (AC-3).
5. `page=0&size=0` -> 400 (AC-4); also `size=-1` -> 400.
6. `size=101` -> 400; `size=100` -> 200 (AC-6).
7. `page=abc` -> 400; `size=abc` -> 400 (AC-8). Also `page=99999999999` (beyond int) -> 400 (FR-8).
8. Past-the-end `page=9&size=10`: 200, `items` empty, `page=9`, real `totalItems`/`totalPages` (AC-7).
9. D-1 overflow: `page=2147483647&size=10`: 200, empty `items`, `page=2147483647` echoed, `size=10`, `totalItems=N`, `totalPages=ceil(N/10)`. Also a boundary check just below the guard (e.g. `page=2147483647&size=1` computes exactly Integer.MAX_VALUE, not greater, and goes through the normal path: assert no 500 and an empty 200 envelope).
10. Empty table (`deleteAll()` within the test): 200, `items` empty, `totalItems=0`, `totalPages=0`.
11. AC-9 / D-4: walk every page (0..totalPages-1) with size 10 and also with a different size (e.g. 7); assert ids strictly ascending within each page and across consecutive pages, the union of all pages equals the full id set with no duplicates and no gaps, and its size equals `totalItems`.
12. Response shape: JSON has exactly the keys `items`, `page`, `size`, `totalItems`, `totalPages`; body is an object, not an array (FR-3, FR-2).
13. NFR-1 sanity (optional, P3): `items` length never exceeds `size` for `size=5`.

## Priority summary
P1: T-1, T-2, T-3, T-4, T-5, T-6, T-7, T-8 (8). P2: T-9, T-10 (2). P3: 0 tasks (the optional case 13 is folded into T-7). Total 10.

## Dependency graph
```
T-1 --> T-4 --> T-7 --> T-9 --> T-10
T-1 --> T-5 ----------------------^
T-2 --> T-4
T-2 --> T-6 ------------------------^
T-3 --> T-8 ------------------------^
T-7 --------------------------------^
```
Cycle check: every edge points from a lower-numbered task to a higher-numbered one, so the graph is a DAG; no cycles.

**Critical path**: T-1 (or T-2) -> T-4 -> T-7 -> T-9 -> T-10 (S + M + L + S + S). T-3 -> T-8 -> T-10 runs in parallel and is shorter.

## Blocked tasks

| Task | Blocked until | Why |
|---|---|---|
| T-4 | T-1, T-2 | Controller returns `PagedResponse` and calls `getProducts`/`countProducts`. |
| T-5 | T-1 | Tests the DTO. |
| T-6 | T-2 | Tests the new service methods. |
| T-7 | T-4 | MockMvc tests need the finished endpoint, validation and overflow guard. |
| T-8 | T-3 | Asserts the extended seed (25+ products). |
| T-9 | T-4 (and T-7 if sharing the class) | Regression of the unchanged category endpoint against the new controller. |
| T-10 | T-5, T-6, T-7, T-8, T-9 | Full verify requires all code and tests. |

Unblocked at start (can run in parallel): T-1, T-2, T-3. Blocked count: 7 (T-4..T-10).

## Verification outline for Step 7 (tester-agent)
1. `./mvnw clean verify` in `ecom-project/` (Java 21); all new tests pass; report results as they are.
2. Read the JaCoCo report in `ecom-project/target/site/jacoco/`; check the 85% gate from `.claude/pipeline/pipeline-config.md` (tester-enforced, not build-enforced) for `ProductController`, `ProductService`, `PagedResponse`, `DataSeeder`.
3. Map each AC-1..AC-10 to a passing test (traceability table in `test-report.md`); confirm the D-1 overflow test (`page=2147483647&size=10`), the AC-10 seed test, and the D-4 ascending-ids-across-pages test exist and pass.
4. Manual smoke with `./mvnw spring-boot:run`: `GET /api/products`, `?page=2&size=10`, `?page=9`, `?size=101` (400), `?page=abc` (400), `?page=2147483647&size=10` (200, empty); confirm 25+ seeded products and that the category endpoint still returns a bare array.
5. Report as pre-existing, not defects of this Story: `ProductControllerIT` failures (R2), front-end breakage of `App.jsx` against the envelope (R1), `include-message` not set (D-2), and CLAUDE.md inaccuracies (D-6, R5).
6. Confirm no files changed under `ecom-front/`, `CLAUDE.md`, `application.properties`, `ProductControllerIT.java`, or `docs/KNOWN-ISSUES.md`.

## Definition of Done
- T-1..T-10 complete; code on `feature/EPMCDMETST-69858`, committed with `feat(EPMCDMETST-69858): ...` messages; nothing committed to `main`.
- `GET /api/products` returns the envelope with defaults page=0, size=10, id-ascending; validation, past-the-end and overflow behaviour as in the architecture validation table.
- `ProductService` has `getProducts(Pageable)` and `countProducts()`; `getAllProducts()` unchanged.
- `DataSeeder` seeds >= 25 products over the 3 existing categories; no entity/schema change.
- All new tests pass; existing `EcomProjectApplicationTests` passes; no existing test weakened or deleted; coverage >= 85% per tester check.
- Tests assert status codes only (no `message` assertions); data-mutating test classes isolated (`@DirtiesContext` / `@BeforeEach` recreation).
- Untouched: `CLAUDE.md`, `ProductControllerIT.java`, KI-1..KI-3, `ecom-front/`, `application.properties`.
- Artifacts `implementation-notes.md`, `code-review.md`, `test-report.md`, PR body produced by later steps; PR Known Limitations lists R1, R5/D-6 and the stale IT.
