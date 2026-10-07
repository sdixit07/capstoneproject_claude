# Requirements Specification: EPMCDMETST-67217

- **Jira**: EPMCDMETST-67217 (Story, status Open)
- **Sub-tasks**: EPMCDMETST-67219 (repository lookup by id), EPMCDMETST-67220 (unit tests)
- **Branch**: feature/EPMCDMETST-67217

## User Story
As a developer, I want a clean repository/service method to fetch a product by id, so that the Product Details API is simple, testable, and reusable.

## Scope
In scope: service-layer method `ProductService.getProductById(Long id)`, a not-found exception, unit tests.
Out of scope: the `GET /api/products/{id}` controller endpoint, response shape or schema changes, frontend changes.

## Clarifications
| # | Question | Answer (defaults accepted) | Effect on requirements |
|---|----------|---------------------------|------------------------|
| 1 | How should the service signal not-found, and should the endpoint be added? | Add unchecked `ProductNotFoundException` (id in message) thrown by `ProductService.getProductById(Long id)`, annotated `@ResponseStatus(NOT_FOUND)` or handled by a small `@RestControllerAdvice`. Do NOT add the controller endpoint. | FR-2, FR-3, FR-4; endpoint excluded (Scope). |
| 2 | Id type and edge-case ids? | Id is `Long`. Null id is left to the repository's `IllegalArgumentException`. Zero/negative ids follow the normal not-found path. Tests use seeded/saved ids plus 999999. | FR-5, FR-6, NFR-3. |

## Functional Requirements
- FR-1 (AC1, 67219): `ProductService` exposes `getProductById(Long id)` that retrieves the product using `ProductRepository.findById(id)`; no new repository method is required.
- FR-2 (AC1): When a product with the id exists, the method returns that `Product`.
- FR-3 (AC2): When no product exists with the id (e.g. 999999), the method throws `ProductNotFoundException`.
- FR-4 (AC2): `ProductNotFoundException` is unchecked, includes the requested id in its message, and is translatable to HTTP 404 (via `@ResponseStatus(HttpStatus.NOT_FOUND)` or a small `@RestControllerAdvice`) so a future controller needs no extra mapping.
- FR-5: A null id is not handled specially; the repository's `IllegalArgumentException` propagates.
- FR-6: Zero or negative ids are treated as ordinary ids and yield `ProductNotFoundException` when absent.
- FR-7 (67220): Unit tests with a mocked `ProductRepository` cover both scenarios (found, not found).

## Non-Functional Requirements
- NFR-1: No change to existing API response shapes or database schema.
- NFR-2: Follow existing package layout (`org.ecom.productcatalog.service`, exception in a suitable package) and code style.
- NFR-3: Tests are deterministic and independent of database state (mocked repository; ids 999999 and a sample saved/seeded id).
- NFR-4: Overall coverage must meet the 85% threshold, enforced in Step 7.

## Acceptance Criteria
- AC1 (Product is returned when it exists): Given a product exists with id `<id>`, when the service requests product `<id>`, then the service returns the product. Maps to FR-1, FR-2, FR-7; sub-tasks 67219, 67220.
- AC2 (Not found when product does not exist): Given no product exists with id 999999, when the service requests product 999999, then the service indicates the product was not found (`ProductNotFoundException`). Maps to FR-3, FR-4, FR-6, FR-7; sub-task 67220.

## Assumptions
- Service-layer only; `findById` is reused, which satisfies sub-task 67219.
- Unit tests mock `ProductRepository`.
- `docs/KNOWN-ISSUES.md`: Not Found (file does not exist).

## Risks / Items to Verify
- R1: Duplicate/misplaced `Product` class. The only `Product.java` sits in `org/ecom/productcatalog/model/` but declares `package org.ecom.productcatalog;`, while `Category` is in `...model`. Service and repository import `org.ecom.productcatalog.Product`. The package/directory mismatch must be verified (compiles under Maven, but is fragile); do not move or rename it in this Story unless the architect decides so. New code must import the same type the repository uses.
- R2: No existing service unit tests; only `ProductControllerIT` and a context-load test exist. Test dependencies (JUnit 5, Mockito) are to be confirmed in `pom.xml`.
- R3: `ProductService` uses field injection with a public field; Mockito `@InjectMocks` works but constructor injection is out of scope.

## Open Questions
None.
