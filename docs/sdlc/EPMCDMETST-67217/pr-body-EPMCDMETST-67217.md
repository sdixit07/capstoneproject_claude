## Summary
Implements Jira story EPMCDMETST-67217 (sub-tasks 67219, 67220): a clean service-layer method to fetch a product by id so the future Product Details API is simple, testable and reusable. Service layer only; no controller endpoint, schema or response-shape change.

## Changes
- `ProductService.getProductById(Long id)` using `ProductRepository.findById`; returns the product or throws `ProductNotFoundException`.
- New unchecked `ProductNotFoundException` (id in message, `@ResponseStatus(NOT_FOUND)`).
- JaCoCo Maven plugin added to `ecom-project/pom.xml` (report on `verify`).
- Unit tests: ProductServiceTest, CategoryServiceTest, ModelTest, ProductControllerTest, CategoryControllerTest, DataSeederTest.
- README (local run without Docker, feature note), new CHANGELOG.md, SDLC artifacts in `docs/sdlc/EPMCDMETST-67217/`.

## Testing and Test Evidence
- `./mvnw clean verify` in `ecom-project`: BUILD SUCCESS, 20 tests run, 0 failures, 0 errors, 0 skipped.
- JaCoCo: instruction coverage 98.4% (311 covered / 5 missed), line 97.9% (95 / 2); threshold 85% met (was 67.4% before added tests).
- Code review (`code-review.md`): APPROVE.
- Frontend checks not run (dependencies not installed; no frontend changes).

## Traceability
- AC1 (product returned when it exists) -> FR-1, FR-2, FR-7; ProductServiceTest found case.
- AC2 (not found for id 999999) -> FR-3, FR-4, FR-6, FR-7; ProductServiceTest not-found case, `ProductNotFoundException`.
- NFR-1 no API/schema change; NFR-3 deterministic mocked tests; NFR-4 85% coverage met.
- Sub-task 67219 -> `getProductById`; 67220 -> unit tests.

## Known Limitations
- `ProductControllerIT` (13 tests, 10 failures) asserts price-filter/sort (minPrice/maxPrice) behaviour that `ProductController` does not implement. Pre-existing mismatch, out of scope; the test is excluded from the default build (`*IT` not run by Surefire). No KI id exists (`docs/KNOWN-ISSUES.md` is absent).
- Frontend checks not run (dependencies not installed).
- `Product.java` package/directory mismatch (R1) left unchanged.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
