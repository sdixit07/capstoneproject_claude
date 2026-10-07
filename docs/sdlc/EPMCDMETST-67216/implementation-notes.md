# Implementation Notes - EPMCDMETST-67216

GET /api/products/{id}. Branch feature/EPMCDMETST-67216. Backend only (`ecom-project/`).

## Baseline (before changes)
| Check | Result |
|-------|--------|
| `.\mvnw.cmd test` (ecom-project) | Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 (EcomProjectApplicationTests), BUILD SUCCESS |
| Frontend | Not run; no frontend change (T-14 is read-only) |

Note: `ProductControllerIT` (existing, `*IT`) is not executed by Surefire, so it is not part of the baseline count.

## Tasks
- T-1 baseline: done
- T-2 service `getProductById` (404 ResponseStatusException "Product not found with id N"): done
- T-3 controller `@GetMapping("{id}")`: done
- T-4 `server.error.include-message=always`: done
- T-5 compile: done (covered by test run)
- T-6, T-7, T-10 MockMvc tests: done, in `ProductByIdIntegrationTest` (see deviations)
- T-8 error body test (RANDOM_PORT): done, `ProductByIdErrorBodyIntegrationTest`
- T-9 service Mockito test: done, `ProductServiceTest`
- T-11 full run: done; T-12 package: done; T-13 live checks: done; T-14 frontend contract: confirmed; T-15 notes/commit/push: this file

## Files changed
- ecom-project/src/main/java/org/ecom/productcatalog/service/ProductService.java
- ecom-project/src/main/java/org/ecom/productcatalog/controller/ProductController.java
- ecom-project/src/main/resources/application.properties
- ecom-project/src/test/java/org/ecom/productcatalog/service/ProductServiceTest.java (new)
- ecom-project/src/test/java/org/ecom/productcatalog/controller/ProductByIdIntegrationTest.java (new)
- ecom-project/src/test/java/org/ecom/productcatalog/controller/ProductByIdErrorBodyIntegrationTest.java (new)

## New test results
ProductServiceTest 3/3, ProductByIdIntegrationTest 9/9, ProductByIdErrorBodyIntegrationTest 2/2. Total 14 new, 0 failures.
D-5: all fixtures set name, description, price, category (imageUrl on asserted product); `Product` nullability not changed.

## Full suite vs baseline
Baseline 1 test; after: Tests run: 15, Failures: 0, Errors: 0, Skipped: 0. New failures: 0.

## Build and app start
- `.\mvnw.cmd -DskipTests package`: BUILD SUCCESS (target/productcatalog-0.0.1-SNAPSHOT.jar)
- App started from jar on 8080; curl results:
  - GET /api/products -> 200 JSON list
  - GET /api/products/1 -> 200 with id, name, description, imageUrl, price, category
  - GET /api/products/999999 -> 404 JSON with timestamp/status/error/message "Product not found with id 999999"/path
  - GET /api/products/abc -> 400 JSON with message
  - GET /api/products/category/1 -> 200 list
  - App process stopped afterwards (port 8080 no longer responds).

## Frontend contract (T-14, read only)
`ProductDetail.jsx` fetches `http://localhost:8080/api/products/${id}`, treats 404 as not found, other non-OK as error, and uses name, description, price, imageUrl, category.name, all present in the 200 body. No frontend change.

## Deviations
1. The plan put T-6/T-7/T-10 in `ProductControllerIT` and T-8 in `ProductByIdErrorBodyIT`. Surefire skips `*IT` classes (per project rules), so the new tests were created as `*IntegrationTest` classes so they actually run. `ProductControllerIT` was left untouched (it tests features not present in the code and is not run by Surefire).
2. Frontend baseline/tests were not run since no frontend files changed.

---

## Step 7 remediation (fix mode): module-wide coverage gate

`test-report.md` (D-1) recorded module-wide line coverage of **77.08 % (74/96)** against the mandatory
85 % gate. The story's own classes (`ProductService`, `ProductController`) were already at 100 %; all 22
uncovered lines sat in pre-existing classes this story never touched. The user explicitly approved
adding tests for those classes to clear the gate.

### What was added (tests only - no production file, `application.properties` or `pom.xml` change)

| New test class | Tests | Covers |
|---|---:|---|
| `ecom-project/src/test/java/org/ecom/productcatalog/controller/CategoryControllerIntegrationTest.java` | 5 | `CategoryController.getCategoryService()` (`GET /api/categories`): happy path with two categories, empty-catalogue case, `products` not serialised (`@JsonIgnore`), configured CORS origin, unknown sub-path -> 404 |
| `ecom-project/src/test/java/org/ecom/productcatalog/service/CategoryServiceTest.java` | 2 | `CategoryService.getAllCategories()`: delegates to the repository; empty repository returns an empty list (Mockito, same style as `ProductServiceTest`) |
| `ecom-project/src/test/java/org/ecom/productcatalog/model/CategoryAndProductModelTest.java` | 7 | `Category` all-args constructor, no-args constructor, `setId` / `getProducts` / `setProducts`; `Product` all-args constructor and setters - each with a happy path plus a null / empty / negative-value case |

All three are named so Surefire's default includes pick them up (`*Test.java`). `ProductControllerIT`
was deliberately left completely untouched (known-dead, tracked as a separate Jira issue), and no
existing test file was modified or deleted.

### Why classes outside the original story scope are touched

The 85 % gate is applied to **module-wide** line coverage, not to the story's diff. The story could not
clear it by covering its own code (already 100 %), so the only route was covering the pre-existing
members listed in `test-report.md` section 3.6. The remediation is confined to the `src/test` tree, so
no shipped behaviour changes.

### Results after remediation (`.\mvnw.cmd clean test`, BUILD SUCCESS)

| Metric | Before | After |
|---|---|---|
| Tests run | 15 | **29** (14 new) |
| Failures / Errors / Skipped | 0 / 0 / 0 | **0 / 0 / 0** |
| Module line coverage | 77.08 % (74/96) | **97.92 % (94/96)** |
| Module instruction coverage | 82.08 % (261/318) | 98.43 % (313/318) |
| Module method coverage | 79.49 % (31/39) | 97.44 % (38/39) |
| 85 % line gate | FAIL | **PASS** |

Per-class line coverage after remediation: `ProductService`, `ProductController`, `CategoryService`,
`CategoryController`, `Category`, `Product`, `DataSeeder` all **100 %**; `EcomProjectApplication` 1/3.

### Remaining uncovered code

`EcomProjectApplication.main(String[])` - 2 lines. Left uncovered deliberately: covering it means
booting a second application context purely for coverage, which is awkward and adds build time for no
behavioural assurance. The gate passes comfortably without it (97.92 % vs 85 %). Branch coverage stays
n/a (the module still has 0 branch probes).

### Deviations in this remediation

None. No production code, configuration, build file or existing test was modified; three test classes
were added. `EcomProjectApplication.main` was skipped as the task explicitly allowed.
