# EPMCDMETST-67216: Add GET /api/products/{id} product detail endpoint

**Story:** As a shopper, I can view a product details page so that I can see full information before purchasing
**Jira:** EPMCDMETST-67216
**Base:** `main` (`92e93fb`) &larr; **Head:** `feature/EPMCDMETST-67216`

## Summary

The React catalog already had a product details page (`ProductDetail.jsx`) that fetched
`http://localhost:8080/api/products/${id}`, but the Spring Boot back-end exposed only
`GET /api/products` and `GET /api/products/category/{categoryId}` - so the page could never load.
This PR adds the missing endpoint.

`GET /api/products/{id}` now returns `200` with the full product (`id`, `name`, `description`,
`imageUrl`, `price`, `category`) for an existing id, `404` for a well-formed id that does not exist,
and `400` for a non-numeric or out-of-`Long`-range id. `server.error.include-message=always` is
enabled so both error bodies carry a `message`, which is what lets the front-end tell "no such
product" apart from "malformed request".

**No frontend code changed.** The existing component already consumed exactly this contract; the
change is backend-only plus tests and documentation. The work is read-only at the data layer: no
schema change, no new entity field, no mutation.

## Changes Made

**Production code (3 files, 15 lines added)**

| File | Change |
|------|--------|
| `ecom-project/src/main/java/org/ecom/productcatalog/controller/ProductController.java` | Added `@GetMapping("{id}") getProductById(@PathVariable Long id)`, delegating to the service. Declared after the existing `category/{categoryId}` mapping so the more specific path still wins. |
| `ecom-project/src/main/java/org/ecom/productcatalog/service/ProductService.java` | Added `getProductById(Long)`: `productRepository.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Product not found with id " + id))`. Reuses the inherited `JpaRepository.findById`; no new repository method. |
| `ecom-project/src/main/resources/application.properties` | Added `server.error.include-message=always` (design decision D-1) so the default error body includes `message`. |

The `400` path needs no code: Spring's own `MethodArgumentTypeMismatchException` handling produces it
when `{id}` cannot be bound to `Long`.

**Build**

| File | Change |
|------|--------|
| `ecom-project/pom.xml` | Added `jacoco-maven-plugin` (`prepare-agent` + `report` bound to `test`) to produce the coverage report the SDLC gate is measured against. See Known Limitations, item 4. |

**Tests (6 new classes, 28 new tests - all new files, no existing test modified)**

| Class | Tests | Covers |
|-------|------:|--------|
| `service/ProductServiceTest` | 3 | `getProductById` with Mockito: found, not found (`ResponseStatusException` 404), message content. |
| `controller/ProductByIdIntegrationTest` | 9 | MockMvc: `200` with all detail fields, `404` for `999999`, `404` for `0` and `-1`, `400` for `abc`, `400` out-of-range, `category/{id}` regression, list regression, configured CORS origin. |
| `controller/ProductByIdErrorBodyIntegrationTest` | 2 | `RANDOM_PORT` + `TestRestTemplate`: asserts the `404` and `400` JSON bodies contain `timestamp`, `status`, `error`, `message`, `path`. |
| `controller/CategoryControllerIntegrationTest` | 5 | Pre-existing `GET /api/categories` (coverage remediation). |
| `service/CategoryServiceTest` | 2 | Pre-existing `CategoryService.getAllCategories()` (coverage remediation). |
| `model/CategoryAndProductModelTest` | 7 | Pre-existing `Category` and `Product` constructors/accessors (coverage remediation). |

The last three classes exist only to clear the module-wide 85% line-coverage gate: the story's own
classes were already at 100%, and every uncovered line sat in pre-existing code this story never
touched. They add no production change.

**Documentation**

- `README.md`: product endpoint table, a dedicated `GET /api/products/{id}` section with sample
  request/response and a status-code table, Docker-free local run steps for back-end and front-end,
  and a "Running the Tests" section.
- `CHANGELOG.md`: created (Keep a Changelog), `## [Unreleased]` &rarr; Added / Changed, each entry
  ending with `(EPMCDMETST-67216)`.
- `docs/sdlc/EPMCDMETST-67216/`: requirements, architecture, design review, implementation plan,
  implementation notes, code review, test report.

## Test Evidence

Final run of `./mvnw.cmd clean test` in `ecom-project`, **BUILD SUCCESS**:

```
CategoryControllerIntegrationTest        5
ProductByIdErrorBodyIntegrationTest      2
ProductByIdIntegrationTest               9
EcomProjectApplicationTests              1
CategoryAndProductModelTest              7
CategoryServiceTest                      2
ProductServiceTest                       3

Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Check | Result |
|-------|--------|
| Backend suite | **PASS** - 29 tests, 0 failures, 0 errors, 0 skipped |
| Baseline on `main` | 1 test (`EcomProjectApplicationTests`), 0 failures |
| Regressions | **0 new failures vs. baseline** |
| Acceptance criteria | **4 / 4 verified** (AC-1 200 with details, AC-2 404 for `999999`, AC-3 400 for `abc`, AC-4 `ProductDetail.jsx` contract + CORS) |
| Module-wide **line** coverage | **97.92% (94/96)** against the 85% gate - +12.92 pp headroom |
| Instruction coverage | 98.43% (313/318) |
| Branch coverage | **n/a - the module has 0 branch probes** (no production source contains a conditional), so there is no branch percentage to report either way |
| Coverage of classes touched by this story | 100% (`ProductService`, `ProductController`) |
| Code review verdict | **APPROVE** - 0 blockers, 0 majors, 2 minors |

Per-class line coverage: `ProductService`, `ProductController`, `CategoryService`,
`CategoryController`, `Category`, `Product`, `DataSeeder` all 100%; `EcomProjectApplication` 1/3.

Manual verification from the packaged jar on port 8080 (recorded in `implementation-notes.md`):
`GET /api/products` &rarr; 200 list; `GET /api/products/1` &rarr; 200 with all detail fields;
`GET /api/products/999999` &rarr; 404 with `"Product not found with id 999999"`;
`GET /api/products/abc` &rarr; 400 with a conversion message; `GET /api/products/category/1` &rarr; 200 list.

## Known Limitations

1. **`server.error.include-message=always` is global.** It is a server-wide setting, so exception
   text - including Spring's internal type-conversion messages - is now exposed in the error body of
   *every* endpoint, not just this one. Accepted for this local demo per design decision **D-1** and
   recorded as minor finding **C-1** in the code review. **This is not production-ready**: a real
   deployment should use `include-message=on_param` or a `@ControllerAdvice` scoped to this endpoint.
2. **New integration tests are named `*IntegrationTest`, not `*IT`.** Maven's default Surefire
   includes skip `*IT` classes, and this project adds no Failsafe execution, so tests named `*IT`
   never run. The new classes were named `*IntegrationTest` so they actually execute. Documented
   deviation; code-review minor finding **C-2**.
3. **The pre-existing `ProductControllerIT` never executes, and 10 of its 13 tests fail when forced
   to run.** It targets endpoints that do not exist in this codebase. It was left byte-identical by
   this story - out of scope here, tracked as **EPMCDMETST-68366**.
4. **The `jacoco-maven-plugin` added in `cc660e0` is outside the original story scope**, and it is
   configured with `prepare-agent` + `report` only - **no `check` goal**, so the build does **not**
   fail if coverage drops below any threshold. The 85% gate is currently enforced by the SDLC
   process, not by the build.
5. **`EcomProjectApplication.main(String[])` (2 lines) is deliberately left uncovered.** Covering it
   means booting a second Spring context purely for the metric. These are the only 2 uncovered lines
   in the module.
6. **21 pre-existing npm vulnerabilities in `ecom-front`** (2 critical, in dev-only dependencies -
   mainly `tinypool`/`vitest`) are untouched by this story. No `package.json` or lock file change was
   made and no new dependency was introduced; the Maven side added no new dependency either.
7. **No automated frontend or end-to-end test was added.** The frontend contract was verified by
   source review of `ProductDetail.jsx` plus manual `curl` against the running jar. No frontend file
   changed, so no frontend test run was performed.

## Reviewer Checklist

- [ ] `GET /api/products/{id}` returns 200 with `id`, `name`, `description`, `imageUrl`, `price` and
      `category` for an existing id (AC-1).
- [ ] `GET /api/products/999999` returns 404, and `0` / `-1` also return 404 rather than 400 (AC-2,
      FR-6).
- [ ] `GET /api/products/abc` and an out-of-`Long`-range id return 400 (AC-3).
- [ ] Both error bodies carry `timestamp`, `status`, `error`, `message`, `path`, and you accept the
      global reach of `server.error.include-message=always` for this demo (limitation 1 / D-1).
- [ ] `{id}` does not shadow `category/{categoryId}`: `GET /api/products/category/1` still returns a
      list and `GET /api/products` is unchanged (FR-7).
- [ ] CORS origin `http://localhost:5173` is unchanged and the details page loads end to end (FR-8,
      AC-4).
- [ ] You accept the `*IntegrationTest` naming instead of `*IT` (limitation 2 / C-2).
- [ ] You accept the `jacoco-maven-plugin` addition and that it has no `check` goal (limitation 4).
- [ ] You accept the three coverage-remediation test classes that cover pre-existing code outside the
      story's diff.
- [ ] `./mvnw clean test` in `ecom-project` reproduces 29 tests, 0 failures.
- [ ] README and CHANGELOG read accurately.

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
