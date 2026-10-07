# Implementation Notes: EPMCDMETST-67217

## Baseline (before changes)
| Check | Result |
|-------|--------|
| `mvnw.cmd clean test` (ecom-project) | BUILD SUCCESS, 1 test run (EcomProjectApplicationTests), 0 failures, 0 errors |
| Note | `ProductControllerIT` is not run by Surefire (`*IT` naming). docs/KNOWN-ISSUES.md does not exist. Frontend not touched, no frontend baseline taken. |

## Tasks
- T-1 baseline recorded ✅
- T-2 ProductNotFoundException (unchecked, @ResponseStatus NOT_FOUND, message "Product not found with id: <id>") ✅
- T-3 ProductService.getProductById (findById.orElseThrow) ✅
- T-4 jacoco-maven-plugin 0.8.12 (prepare-agent, report at verify) ✅
- T-5 ProductServiceTest (3 tests) ✅
- T-6 optional, not done
- T-7 verification ✅ (coverage gate NOT met overall, see below)

## Files changed
- ecom-project/pom.xml
- ecom-project/src/main/java/org/ecom/productcatalog/exception/ProductNotFoundException.java (new)
- ecom-project/src/main/java/org/ecom/productcatalog/service/ProductService.java
- ecom-project/src/test/java/org/ecom/productcatalog/service/ProductServiceTest.java (new)

## Test results
- ProductServiceTest: 3 run, 0 failures, 0 errors.
- Full suite (`mvnw.cmd clean verify`): 4 run, 0 failures, 0 errors, 0 skipped. New failures vs baseline: 0.

## Build
- `clean compile`: success. `clean verify`: BUILD SUCCESS, JaCoCo report at ecom-project/target/site/jacoco/index.html.
- App start check (`/api/products`): not performed; no controller or config change was made, and the Spring context test passes.

## Coverage (JaCoCo, instruction)
- Overall: 67.4% (below the 85% target of NFR-4). Uncovered: entity getters/setters (Product, Category), ProductController, CategoryController, CategoryService, and getAllProducts/getProductByCategory in ProductService. ProductControllerIT is not executed by Surefire because of its `*IT` name.
- New code: ProductNotFoundException 100% (5/5); getProductById fully covered by tests.
- No gate was enforced in the build; raising overall coverage is out of plan scope.

## Deviations
- Overall 85% coverage not reached (pre-existing untested code); reported, not worked around.
- App-start check skipped (see above).

## Coverage follow-up (tests only, no production change)
- Added tests: ModelTest (Product/Category getters, setters, constructors), CategoryServiceTest, ProductControllerTest and CategoryControllerTest (standalone MockMvc), DataSeederTest, and getAllProducts/getProductByCategory cases in ProductServiceTest.
- `mvnw.cmd clean verify`: 20 tests run, 0 failures, 0 errors, 0 skipped. BUILD SUCCESS.
- JaCoCo overall: instruction 98.4% (311 covered / 5 missed), line 97.9% (95 / 2). Previously 67.4%. Target of 85% met. (No branches exist in the code.)
- ProductControllerIT remains unexecuted (`*IT` is skipped by Surefire). It was deliberately not enabled: it asserts price-filter and sort behaviour (minPrice/maxPrice) that the current ProductController does not implement, so it would fail. Out of scope for this story.
- The earlier deviation "85% coverage not reached" is resolved.
