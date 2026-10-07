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
