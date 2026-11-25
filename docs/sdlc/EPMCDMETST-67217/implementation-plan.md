# Implementation Plan: EPMCDMETST-67217

Scope (from `architecture.md`, final): backend service method `ProductService.getProductById(Long)` and new `ProductNotFoundException` only. No controller endpoint, no HTTP 404 mapping, no frontend change, no repository change, no new dependencies.

Paths are relative to `ecom-project/src/`. Package for `Product` is `org.ecom.productcatalog` (the file sits in `model/`).

## Tasks (ordered by dependency, then priority)

### T-1: Create `ProductNotFoundException`
- **Priority**: P1
- **Area**: backend (`ecom-project/`)
- **Files**: `main/java/org/ecom/productcatalog/exception/ProductNotFoundException.java` (new; new package `org.ecom.productcatalog.exception`)
- **Description**: Unchecked exception extending `RuntimeException`, single constructor `ProductNotFoundException(Long id)` calling `super("Product not found with id: " + id)`. No extra constructors, no `@ResponseStatus`.
- **Traces to**: FR-3, FR-4, AC-2
- **Depends on**: none
- **Tests**: covered by T-3 and T-4
- **Effort**: XS

### T-2: Add `ProductService.getProductById(Long id)`
- **Priority**: P1
- **Area**: backend (`ecom-project/`)
- **Files**: `main/java/org/ecom/productcatalog/service/ProductService.java` (modify)
- **Description**: `return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));`. Add imports `org.ecom.productcatalog.Product` (if not present) and `org.ecom.productcatalog.exception.ProductNotFoundException`. Keep the existing public-field injection style; no `@Transactional`, no null handling, no refactor.
- **Traces to**: FR-1, FR-2, FR-3, FR-5, AC-1, AC-2
- **Depends on**: T-1
- **Tests**: T-3, T-4
- **Effort**: XS

### T-3: Unit test for AC-1 (product found)
- **Priority**: P1
- **Area**: backend (`ecom-project/`)
- **Files**: `test/java/org/ecom/productcatalog/service/ProductServiceTest.java` (modify)
- **Description**: Mockito mock repo, no Spring context. `when(repo.findById(1L)).thenReturn(Optional.of(product))`; `assertSame(product, service.getProductById(1L))`; `verify(repo).findById(1L)` (D-2).
- **Traces to**: AC-1, FR-1, FR-2, NFR-1
- **Depends on**: T-2
- **Effort**: XS

### T-4: Unit test for AC-2 (product not found)
- **Priority**: P1
- **Area**: backend (`ecom-project/`)
- **Files**: `test/java/org/ecom/productcatalog/service/ProductServiceTest.java` (modify)
- **Description**: `when(repo.findById(999999L)).thenReturn(Optional.empty())`; `assertThrows(ProductNotFoundException.class, ...)`; assert message `Product not found with id: 999999`; `verify(repo).findById(999999L)`; assert unchecked via `assertTrue(RuntimeException.class.isAssignableFrom(ProductNotFoundException.class))` (D-2).
- **Traces to**: AC-2, FR-3, FR-4, NFR-1
- **Depends on**: T-2
- **Effort**: XS

### T-5: Write implementation notes (D-4 carry-forward)
- **Priority**: P2
- **Area**: documentation (`docs/sdlc/EPMCDMETST-67217/implementation-notes.md`)
- **Description**: Must record: (a) the `Product` import is `org.ecom.productcatalog.Product` (folder `model/`, package differs); (b) the new package `org.ecom.productcatalog.exception`; (c) no new dependencies (NFR-3); (d) no controller/frontend change and the follow-up Story needed for the controller and 404 mapping.
- **Traces to**: D-4, F-1, NFR-3
- **Depends on**: T-1, T-2 (T-3, T-4 for test status)
- **Effort**: XS

### T-6: Run build and tests, check coverage (verification for Steps 5-7)
- **Priority**: P2
- **Area**: backend (`ecom-project/`)
- **Description**: `mvnw.cmd clean verify` from `ecom-project/`. Confirm new tests pass and existing tests are unchanged. Read `ecom-project/target/site/jacoco/` for coverage of new code against the 85% target in `.claude/pipeline/pipeline-config.md` (D-3). Pre-existing `ProductControllerIT` failures (`docs/KNOWN-ISSUES.md`) are out of scope and are reported as they are, not fixed or weakened.
- **Traces to**: NFR-1, NFR-2, NFR-3
- **Depends on**: T-3, T-4
- **Effort**: S

### T-7: PR body carry-forward (D-4) for Step 8
- **Priority**: P3
- **Area**: documentation / PR (`pr-agent`)
- **Description**: The PR's Known Limitations section must state: no `GET /api/products/{id}` endpoint and no 404 mapping in this Story, so the exception would surface as HTTP 500 if called from a controller today; a follow-up Story for the controller and 404 handling is required. Also note null id relies on Spring Data's `IllegalArgumentException` (FR-5) and the known `ProductControllerIT` failures.
- **Traces to**: D-4, F-5, Out of Scope
- **Depends on**: T-5, T-6
- **Effort**: XS

## Summary by priority
| Priority | Count | Tasks |
|---|---|---|
| P1 | 4 | T-1, T-2, T-3, T-4 |
| P2 | 2 | T-5, T-6 |
| P3 | 1 | T-7 |
| Total | 7 | |

Frontend (`ecom-front/`) tasks: none (design: no change).

## Blocked tasks
| Task | Blocked until | Why |
|---|---|---|
| T-2 | T-1 | Service imports and throws `ProductNotFoundException` |
| T-3 | T-2 | Tests call `getProductById` |
| T-4 | T-2 | Tests call `getProductById` and need the exception |
| T-5 | T-1, T-2 | Notes describe the implemented import and package |
| T-6 | T-3, T-4 | Coverage is only meaningful with the tests present |
| T-7 | T-5, T-6 | PR body uses notes and test evidence |

Blocked count: 6 (only T-1 is unblocked at the start). T-3 and T-4 are independent of each other and can run in parallel after T-2.

## Dependency check
Graph: T-1 -> T-2 -> {T-3, T-4} -> T-6 -> T-7; T-1/T-2 -> T-5 -> T-7. All edges point forward in id order, so there are no cycles.

**Critical path**: T-1 -> T-2 -> T-3 (or T-4) -> T-6 -> T-7.

## Unit-test tasks for Step 5
T-3 and T-4 (extend `ProductServiceTest`, Mockito only). No new test files, no Spring context, no frontend tests.

## Verification outline for Step 7
1. `mvnw.cmd clean verify` in `ecom-project/`: `ProductServiceTest` passes, including the two new tests.
2. JaCoCo report: `getProductById` and `ProductNotFoundException` fully covered; threshold 85% (`pipeline-config.md`) enforced by tester agent, not by the build.
3. Confirm no existing tests were weakened or deleted; list `ProductControllerIT` failures as known and out of scope.
4. Confirm no dependency changes in `pom.xml` and no changes under `ecom-front/`.

## Definition of Done
- T-1 to T-4 implemented as designed; both ACs have passing tests that also verify `findById` delegation and unchecked-ness.
- No controller, handler, repository, frontend or dependency changes.
- Coverage of new code at or above 85%, reported honestly.
- `implementation-notes.md` records the `org.ecom.productcatalog.Product` import and the new `org.ecom.productcatalog.exception` package.
- PR Known Limitations records the follow-up controller/404 Story.
- Conventional commits carrying `EPMCDMETST-67217`; nothing on `main`.
