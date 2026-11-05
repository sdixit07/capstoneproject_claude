# Requirements Specification
- **User Story**: EPMCDMETST-69858 - Backend: Add pagination support to GET /api/products.
  As a shopper, I want the product catalog API to support pagination, so that the UI can load and display products page-by-page for better performance.
  Labels: backend, capstone, pagination. Status: Open.

- **Functional Requirements**:
  - FR-1: `GET /api/products` accepts optional query parameters `page` (zero-based) and `size`.
  - FR-2: When `page` and `size` are omitted, defaults are `page=0` and `size=10`. The endpoint always returns the paged envelope, never a bare array.
  - FR-3: Response body (200) is a JSON object with: `items` (array of products), `page`, `size`, `totalItems`, `totalPages`.
  - FR-4: Paging uses Spring Data `Pageable`.
  - FR-5: Results are sorted by `id` ascending, so paging is deterministic.
  - FR-6: `page < 0` returns 400. `size < 1` returns 400.
  - FR-7: `size > 100` returns 400 (maximum page size is 100).
  - FR-8: Non-numeric `page` or `size` returns 400.
  - FR-9: A `page` beyond the last page returns 200 with empty `items` and correct `totalItems` and `totalPages`.
  - FR-10: `DataSeeder` seeds at least 25 products across the existing 3 categories, so the pagination scenarios (more than 10 products, at least 21 products) hold on the default dataset.
  - FR-11: `GET /api/products/category/{categoryId}` stays unpaginated (scope assumption).

- **Non-Functional Requirements**:
  - NFR-1: Paging is done at the database level (Pageable query), not by loading all products and slicing in memory.
  - NFR-2: Invalid-parameter errors return a 400 status consistent with existing error handling (`message` field present in local-dev config only).
  - NFR-3: Backend tests (JUnit 5 / Spring Boot Test) cover the scenarios below; the 85% coverage gate in `.claude/pipeline/pipeline-config.md` applies (enforced by the tester agent).
  - NFR-4: No change to the entity model or database schema.

- **Acceptance Criteria**:
  - AC-1: Given more than 10 products, `GET /api/products?page=0&size=10` returns 200; `items` has at most 10 products; `page`=0; `size`=10; `totalItems` > 10; `totalPages` > 1.
  - AC-2: Given at least 21 products, `GET /api/products?page=2&size=10` returns 200; `items` has at most 10 products; `page`=2.
  - AC-3: `GET /api/products?page=-1&size=10` returns 400.
  - AC-4: `GET /api/products?page=0&size=0` returns 400.
  - AC-5 (from clarifications): `GET /api/products` with no params returns the envelope with `page`=0 and `size`=10.
  - AC-6 (from clarifications): `size=101` returns 400; `size=100` returns 200.
  - AC-7 (from clarifications): a page past the end returns 200 with empty `items` and correct `totalItems`/`totalPages`.
  - AC-8 (from clarifications): `page=abc` or `size=abc` returns 400.
  - AC-9 (from clarifications): items are ordered by `id` ascending across pages.
  - AC-10: The seeded dataset contains at least 25 products across the 3 existing categories.

## Clarifications
| # | Question | Answer | Effect on requirements |
|---|---|---|---|
| 1 | Should `GET /api/products` without paging params keep returning a bare array, or return the paged envelope? | Always return the paged envelope with defaults page=0, size=10. Front end is not changed in this Story. | FR-2, FR-3, AC-5. Breaking change for the current front end (see Known Limitations). |
| 2a | Seed data has too few products to meet "more than 10" / "at least 21". | Extend `DataSeeder` to at least 25 products across the existing 3 categories. | FR-10, AC-10. |
| 2b | Max size, past-the-end page, non-numeric input? | Cap size at 100 (larger gives 400); past-the-end page gives 200 with empty items and correct totals; non-numeric page/size gives 400. | FR-7, FR-8, FR-9, AC-6, AC-7, AC-8. |
| 2c | Sort order? | `id` ascending. | FR-5, AC-9. |
| - | Scope assumption: category endpoint pagination? | `/api/products/category/{categoryId}` stays unpaginated. | FR-11. |

## Known Limitations / Out of Scope
- Front end breakage: `ecom-front/ecom-catalog-react/src/App.jsx` expects `/api/products` to return a bare array and will break against the envelope. Not fixed here; a follow-up front-end Story is required (consume `items` and add paging UI).
- `CLAUDE.md` lists `GET /api/products/{id}` in its API table, but the controller does not implement it. Treated as out of scope for this Story (documentation/code discrepancy to be handled separately).
- No pagination for the category endpoint.
- `Not Found`: no non-functional performance targets are stated in the Story.
