# Implementation Plan: EPMCDMETST-67217 (Product lookup by id, service layer)

Derived only from `architecture.md` (final, includes D-1). Design review: D-1 agreed; D-2, D-3, D-4 NOT agreed (keep original architecture: no null-id mock test, no mandatory `@ResponseStatus` test; an extra `@ResponseStatus` test is allowed but not required).
Sub-tasks: 67219 (repository lookup by id), 67220 (unit tests). Module root: `ecom-project/`.
Import rule (R1): use `org.ecom.productcatalog.Product` (NOT `...model.Product`); do not move or rename it.

## Tasks (ordered by dependency, then priority)
| ID | Pri | Task | Files | Req / AC / Sub-task | Depends on |
|----|-----|------|-------|---------------------|-----------|
| T-1 | P1 | Record baseline: `mvn clean test` in `ecom-project/` before changes | none | NFR-4 | - |
| T-2 | P1 | Create unchecked `ProductNotFoundException extends RuntimeException`, ctor `(Long id)`, message `"Product not found with id: " + id`, `@ResponseStatus(HttpStatus.NOT_FOUND)` | `ecom-project/src/main/java/org/ecom/productcatalog/exception/ProductNotFoundException.java` (new) | FR-4, AC2, NFR-2 / 67219 | T-1 |
| T-3 | P1 | Add `public Product getProductById(Long id)` = `productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id))`; no null guard; no new repository method | `ecom-project/src/main/java/org/ecom/productcatalog/service/ProductService.java` (modify) | FR-1, FR-2, FR-3, FR-5, FR-6, AC1, AC2 / 67219 | T-2 |
| T-4 | P2 | Add `jacoco-maven-plugin` (`prepare-agent`, `report` goals) to the build plugins so the 85% gate is measurable (D-1) | `ecom-project/pom.xml` (modify) | NFR-4 (D-1, R5) | T-1 |
| T-5 | P1 | Unit tests `ProductServiceTest` (`@ExtendWith(MockitoExtension.class)`, `@Mock ProductRepository`, `@InjectMocks ProductService`): (a) found: stub `findById(1L)` -> `Optional.of(product)`, assert same product, `verify(repo).findById(1L)`; (b) not found: `findById(999999L)` -> `Optional.empty()`, `assertThrows(ProductNotFoundException.class)`, message contains `999999`; (c) zero/negative id (e.g. 0L, -1L) not found -> exception | `ecom-project/src/test/java/org/ecom/productcatalog/service/ProductServiceTest.java` (new) | FR-7, FR-2, FR-3, FR-6, NFR-3, AC1, AC2 / 67220 | T-3 |
| T-6 | P3 | Optional (not required, D-4 not agreed): small test asserting `ProductNotFoundException` carries `@ResponseStatus(NOT_FOUND)` and message includes id | `ProductServiceTest.java` or `exception/ProductNotFoundExceptionTest.java` (new) | FR-4, AC2 / 67220 | T-2 |
| T-7 | P1 | Verify: `mvn clean compile`, `mvn test` (existing IT + context test still pass), `mvn verify`/`jacoco:report`; check overall coverage >= 85% in `target/site/jacoco`; confirm no controller/response-shape/schema change | none (reports in `target/`) | NFR-1, NFR-4, AC1, AC2 | T-4, T-5 (T-6 if done) |

Priority count: P1 = 5 (T-1, T-2, T-3, T-5, T-7), P2 = 1 (T-4), P3 = 1 (T-6). Total 7.

## Blocked tasks
| Task | Blocked until | Why |
|------|---------------|-----|
| T-3 | T-2 | Service references `ProductNotFoundException`; will not compile otherwise |
| T-5 | T-3 | Tests call `getProductById` |
| T-6 | T-2 | Needs the exception class |
| T-7 | T-4, T-5 | Needs JaCoCo configured and tests present to measure coverage |
| T-2, T-4 | T-1 | Baseline must be recorded before changes |

Blocked count: 6 (T-2, T-3, T-4, T-5, T-6, T-7 ... counting T-2/T-4 on baseline: 6 of 7 tasks have dependencies; T-1 is the only free task).
Dependency graph: T-1 -> T-2 -> T-3 -> T-5 -> T-7; T-1 -> T-4 -> T-7; T-2 -> T-6 -> T-7 (optional). Acyclic (verified by topological order T-1..T-7).
Critical path: T-1 -> T-2 -> T-3 -> T-5 -> T-7.

## Test tasks (Step 5)
T-5 (required) and T-6 (optional). Explicitly NOT planned (D-3 not agreed, F-4 accepted): mock-stubbed null-id test. Tests are deterministic, mock-only, use ids 1L and 999999L (NFR-3).

## Verification outline (Step 7)
1. `cd ecom-project && mvn clean compile` succeeds (confirms R1 package/directory mismatch still compiles).
2. `mvn test`: new `ProductServiceTest` passes; `ProductControllerIT` and `EcomProjectApplicationTests` still pass.
3. `mvn verify` produces JaCoCo report at `ecom-project/target/site/jacoco/index.html`; overall coverage >= 85% (new code 100% covered by T-5).
4. AC1: found test green. AC2: not-found test green with 999999 and id in message.
5. `git diff main` shows no controller, frontend, schema or response-shape change (NFR-1).

## Definition of Done
- T-1..T-5 and T-7 complete (T-6 optional); build and all tests green.
- AC1 and AC2 demonstrated by passing unit tests; sub-tasks 67219 and 67220 covered.
- JaCoCo configured; overall coverage >= 85% evidenced.
- No new endpoint, no schema/API change; `Product` not moved; R1 recorded as tech debt.
- Logical commits on `feature/EPMCDMETST-67217`; implementation notes written.
