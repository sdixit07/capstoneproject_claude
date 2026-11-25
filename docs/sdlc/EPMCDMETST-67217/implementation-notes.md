# Implementation Notes - EPMCDMETST-67217

Branch: `feature/EPMCDMETST-67217`

## Baseline (before changes)

| Item | Result |
|---|---|
| Backend `.\mvnw.cmd test` | 29 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS |
| `ProductControllerIT` | Did not run (Surefire skips `*IT.java`) |
| JaCoCo (9 classes) | instruction 93.7%, branch 100%, line 82.7% (105 covered, 22 missed) |
| Front end | No baseline: this Story has no front-end changes |

Line coverage was already below the 85% threshold before this change.

## Tasks

- T-1 done: `ProductNotFoundException` (commit 47f1cbb)
- T-2 done: `ProductService.getProductById` (commit 422aef6)
- T-3 / T-4 done: unit tests for found and not-found (commit 2c8f9c7)

## Files changed

- `ecom-project/src/main/java/org/ecom/productcatalog/exception/ProductNotFoundException.java` (new)
- `ecom-project/src/main/java/org/ecom/productcatalog/service/ProductService.java` (modified)
- `ecom-project/src/test/java/org/ecom/productcatalog/service/ProductServiceTest.java` (modified)
- `docs/sdlc/EPMCDMETST-67217/implementation-notes.md` (this file)

## D-4 items

- The `Product` import is `org.ecom.productcatalog.Product` (not `.model`).
- The new package is `org.ecom.productcatalog.exception`.

## Dependencies

No new dependencies.

## New tests

`ProductServiceTest`: 6 tests, 0 failures (includes the new found and not-found cases).

## Full suite vs. baseline

| | Baseline | After |
|---|---|---|
| Tests | 29 | 31 |
| Failures / errors | 0 / 0 | 0 / 0 |

New failures: 0. `ProductControllerIT` still does not run (skipped by Surefire, out of scope).

Coverage after (`clean verify`, 10 classes): instruction 803 covered / 53 missed (93.8%), branch 10 covered / 0 missed (100%), line 108 covered / 22 missed (83.1%). Line coverage is still below 85%. It was below 85% at baseline too, and this change did not cause it.

## Build and app start

- `.\mvnw.cmd clean verify`: BUILD SUCCESS, 31 tests, 0 failures.
- App started from the packaged jar. `GET http://localhost:8080/api/products` returned 200 with `application/json`. The app was then stopped.

## Front end

Not built or tested. The front end is untouched by this Story, so there was nothing to verify.

## Deviations from plan

None.

## Known limitations and follow-up

- `getProductById` throws `ProductNotFoundException`, but no controller endpoint maps it yet. Until then an unknown id surfaces as HTTP 500, not 404.
- A follow-up Story is needed for the controller endpoint and the 404 handling.
