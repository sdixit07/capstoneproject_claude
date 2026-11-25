# Requirements Specification
- **User Story**: EPMCDMETST-67217 - As a developer, I want a clean repository/service method to fetch a product by id, so that the Product Details API is simple, testable, and reusable.

## Clarifications
| # | Question | Answer | Effect on requirements |
|---|----------|--------|------------------------|
| 1 | How is "not found" signalled? | Add a new unchecked `ProductNotFoundException` (extends `RuntimeException`), message `Product not found with id: <id>`. `getProductById(Long id)` returns `Product` and throws it when `findById` is empty. No 404 mapping in this Story. | FR-2, FR-3, FR-4; no exception handler requirement. |
| 2 | Is the `GET /api/products/{id}` controller in scope? | Out of scope. Deliver only the service method, the exception and unit tests for the two ACs. A null id relies on Spring Data's `IllegalArgumentException` and is not specified further. | Out of Scope; FR-5. |
| 3 | PR #1 referenced in Jira comments? | Not in the repo. Treat as a fresh implementation. | No reuse assumed. |

## Functional Requirements
- **FR-1**: `ProductService` shall expose `Product getProductById(Long id)`.
- **FR-2**: The method shall fetch the product via `ProductRepository.findById(id)` and return the product when present.
- **FR-3**: When no product exists for the id, the method shall throw `ProductNotFoundException` with message `Product not found with id: <id>`.
- **FR-4**: `ProductNotFoundException` shall be a new unchecked exception (extends `RuntimeException`), so a controller can later translate it to HTTP 404.
- **FR-5**: Null id behaviour is not specified; it relies on Spring Data's `IllegalArgumentException`.

## Out of Scope
- `GET /api/products/{id}` controller endpoint and any HTTP 404 mapping / exception handler.
- Known failing `ProductControllerIT` tests (see `docs/KNOWN-ISSUES.md`).

## Non-Functional Requirements
- **NFR-1**: Unit tests use a mocked repository (no Spring context or DB required).
- **NFR-2**: Coverage of new code meets the threshold in `.claude/pipeline/pipeline-config.md`.
- **NFR-3**: No new dependencies; existing tests are not weakened or deleted.

## Acceptance Criteria
- **AC-1**: Given a product exists with id `<id>`, when the service requests that id, then the service returns the product.
- **AC-2**: Given no product exists with id `999999`, when the service requests `999999`, then the service throws `ProductNotFoundException` ("Product not found with id: 999999").

## Open Questions
None.
