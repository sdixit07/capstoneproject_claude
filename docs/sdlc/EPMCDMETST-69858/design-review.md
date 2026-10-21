# Design Review: EPMCDMETST-69858 - Pagination for GET /api/products

- Reviewed: `requirements.md` (2b199e4), `architecture.md` (b615be4), and the code in `ProductController`, `ProductService`, `ProductRepository`, `DataSeeder`, `Product`, `Category`, `application.properties`, `pom.xml` (Spring Boot 3.4.4).
- Overall: the design is sound, minimal and consistent with the Controller -> Service -> Repository layering. Backend placement of paging is correct. One real runtime gap (huge `page` value) and a few accuracy issues.

## Findings

| ID | Severity | Area | Risk or gap | Recommendation |
|---|---|---|---|---|
| F-1 | MAJOR | Edge handling (FR-8, FR-9) | Section 6.1 claims `PageRequest` uses a `long` offset so `page=Integer.MAX_VALUE` "returns an empty page". Spring Data JPA 3.4 `findAll(Pageable)` converts the offset to `int` for `setFirstResult` (`PageableUtils.getOffsetAsInteger`) and throws `InvalidDataAccessApiUsageException` when it exceeds `Integer.MAX_VALUE`. E.g. `page=2147483647&size=10` is a valid int but likely yields HTTP 500, violating FR-9 (empty 200) and the "no 500 paths" claim in section 9. | Decide behaviour explicitly: either compute `(long) page * size` in the controller and return 400 (or an empty envelope with real totals) when it exceeds `Integer.MAX_VALUE`, or cap `page`. Add a test for it. Correct the claim in 6.1 and section 9. |
| F-2 | MINOR | NFR-2 / config accuracy | Design (section 4) says `message` appears "under existing `server.error.include-message=always`". `application.properties` as committed does not contain that property (CLAUDE.md says it does). Without it, 400 bodies have no `message`. | State the discrepancy in architecture; tests must assert status only (not `message`), or the property is added deliberately under a decision. Do not assume `message` in tests. |
| F-3 | MINOR | Test design (R3, AC-10) | Seed data is created at startup by `CommandLineRunner`; tests that `deleteAll()` in a shared context destroy it. The suggested AC-10 check (invoke `DataSeeder.run` then count) is itself fine but unspecified; `DataSeeder.run` is destructive and non-transactional. | Specify one approach: a dedicated test calling `dataSeeder.run()` then asserting `count() >= 25` and 3 categories with products, placed so other tests re-create their own data in `@BeforeEach`. Prefer `@DirtiesContext` on the data-mutating classes. |
| F-4 | MINOR | Traceability / AC-9 | AC-9 (id ordering across pages) is mapped to the sort clause only; tests need rows whose insertion order could differ from id order or at least assert strictly ascending ids across consecutive pages. | In test plan, assert ids strictly ascending within and across pages 0..n and that page union equals the full set with no duplicates. |
| F-5 | MINOR | Fit with conventions | Existing `ProductService.getAllProducts()` becomes unused by the controller. Section 3 says it "may stay". Leaving dead code is acceptable but ambiguous. | Keep it (non-breaking, avoids touching unrelated callers); state this as a decision so the planner does not remove it. |
| F-6 | MINOR | Documentation | CLAUDE.md API table is already inaccurate (`/api/products/{id}`) and will also not describe the envelope. Design records this as R5 but assigns no owner. | Keep out of scope; note in the PR "Known Limitations". No architecture change needed. |
| F-7 | MINOR | DTO placement | New `dto` package diverges from the existing flat `controller/service/repository/model` layout, but is reasonable. Generic record with `from(Page<T>)` couples DTO to Spring Data, acceptable. | Accept as designed. |

No BLOCKERs. All FR/NFR/AC items are covered by a design element (see Traceability).

## Proposed design decisions

| ID | Decision | Resolves | Changes architecture.md? |
|---|---|---|---|
| D-1 | Guard against `(long) page * size > Integer.MAX_VALUE` in the controller (return 400 with a clear rule, or empty envelope with real totals; choose one and document it), and correct the incorrect overflow claim in 6.1 and section 9. Add a unit/MockMvc test. | F-1 | yes |
| D-2 | Document that `include-message` is not currently set in `application.properties`; tests assert status code only; do not add the property in this Story. | F-2 | yes |
| D-3 | Specify the AC-10 verification: dedicated test invoking `DataSeeder.run` and counting; data-mutating test classes use `@DirtiesContext` or recreate data in `@BeforeEach`. | F-3 | yes |
| D-4 | Test plan for AC-9 asserts strictly ascending ids across pages and no duplicates/gaps. | F-4 | no (planner/tester guidance) |
| D-5 | Retain `ProductService.getAllProducts()` unchanged. | F-5 | no |
| D-6 | Leave CLAUDE.md untouched; list the doc gap under PR Known Limitations. | F-6 | no |

## Traceability

| Requirement | Design element | Covered? |
|---|---|---|
| FR-1, FR-2 | `@RequestParam` with defaults, always `PagedResponse` | Yes |
| FR-3 | `PagedResponse` record | Yes |
| FR-4, NFR-1 | `PageRequest` -> `findAll(Pageable)` | Yes |
| FR-5, AC-9 | `Sort.by(ASC,"id")` | Yes (see F-4) |
| FR-6, FR-7, AC-3, AC-4, AC-6 | Explicit range checks -> 400 | Yes |
| FR-8, AC-8 | `int` binding type mismatch -> 400 | Yes |
| FR-9, AC-7 | Spring Data empty page | Partly (F-1: huge page may yield 500) |
| FR-10, AC-10 | `DataSeeder` extended to 25 | Yes (see F-3) |
| FR-11 | Category endpoint untouched | Yes |
| NFR-2 | `ResponseStatusException` | Yes (see F-2) |
| NFR-3 | New test class, R2/R3 | Yes |
| NFR-4 | No entity change | Yes |
| AC-1, AC-2, AC-5 | Envelope, defaults, 25 seeded rows | Yes |

## Verdict

**APPROVED WITH CHANGES** - no BLOCKERs; apply D-1, D-2, D-3 to `architecture.md` (one MAJOR, two MINOR) before planning.
