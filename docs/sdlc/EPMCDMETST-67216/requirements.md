# Requirements Analysis - EPMCDMETST-67216

**Story:** As a shopper, I can view a product details page so that I can see full information before purchasing
**Jira:** EPMCDMETST-67216 (Story, status Open)
**Sub-tasks:** EPMCDMETST-67221, EPMCDMETST-67222 (titles/scope: Not Found in Story payload; mapped below by coverage of FRs)
**Branch:** feature/EPMCDMETST-67216

## 1. Summary
Add backend endpoint `GET /api/products/{id}` returning a single Product as JSON. Unknown id -> 404; malformed id -> 400. Must stay compatible with the existing frontend call in `ecom-front/ecom-catalog-react/src/pages/ProductDetail.jsx` (fetches `http://localhost:8080/api/products/${id}`, treats 404 as "not found", other non-OK as error).

## 2. Current state (read-only findings)
- `ProductController` (`/api/products`, `@CrossOrigin("http://localhost:5173")`) has only `GET /` and `GET /category/{categoryId}`; no get-by-id.
- `Product` entity (package `org.ecom.productcatalog`): id (Long), name, description, imageUrl, price (double), category (ManyToOne, non-null).
- `ProductService`/`ProductRepository` exist; no custom exception handler (`@ControllerAdvice`) found, so Spring Boot default error JSON applies.
- `docs/KNOWN-ISSUES.md`: Not Found.
- Requirements template file: Not Found; standard structure used.

## 3. Clarifications
| # | Question | Answer | Effect on requirements |
|---|----------|--------|------------------------|
| 1 | Error body shape for 404/400 | Reuse existing error JSON structure; since none is customised, Spring Boot default (timestamp, status, error, message, path) | FR-4, FR-5: no custom error DTO; no new handler format |
| 2 | Which ids are 400 vs 404 | Non-numeric and out-of-Long-range -> 400; other well-formed numbers (0, negatives) -> 404 | FR-5, FR-6 |
| 3 | Response content | Full Product entity including category | FR-2 |
| 4 | CORS | Unchanged | FR-8 / out of scope |
| 5 | Scope | GET only; no frontend changes; no new fields | Out of scope list |

## 4. Functional Requirements
- FR-1: Expose `GET /api/products/{id}` on `ProductController`, `{id}` bound as `Long`.
- FR-2: If a product with the id exists, respond 200 with JSON of the full Product (id, name, description, imageUrl, price, category).
- FR-3: The response `id` equals the requested id; `name`, `description`, `price` fields are always present in the body.
- FR-4: If no product exists with the id (e.g. 999999, 0, -1), respond 404 with the default Spring Boot error JSON (timestamp, status, error, message, path).
- FR-5: If `{id}` is non-numeric (e.g. `abc`) or outside the Long range, respond 400 with the default error JSON.
- FR-6: Well-formed numeric ids that do not exist, including 0 and negatives, are never 400; they yield 404.
- FR-7: Existing endpoints (`GET /api/products`, `GET /api/products/category/{categoryId}`) keep their behaviour; `category/...` path must not be captured by `{id}`.
- FR-8: CORS configuration unchanged (origin `http://localhost:5173`).

## 5. Non-Functional Requirements
- NFR-1: Compatibility - contract satisfies ProductDetail.jsx (200 JSON; 404 distinguishable; no frontend change).
- NFR-2: Read-only operation; no data mutation, no new fields or schema changes.
- NFR-3: Consistency - follows existing controller/service/repository layering and code style.
- NFR-4: Testability - covered by automated tests (controller/integration, cf. existing `ProductControllerIT`) for all three scenarios plus 0/negative/out-of-range ids.

## 6. Acceptance Criteria (from Jira) and mapping
| AC | Gherkin scenario | Requirements | Sub-task |
|----|------------------|--------------|----------|
| AC-1 | Retrieve product by id successfully: 200, body has id, name, description, price | FR-1, FR-2, FR-3 | EPMCDMETST-67221 (implementation) |
| AC-2 | Product id does not exist (999999): 404 | FR-4, FR-6 | EPMCDMETST-67221 |
| AC-3 | Invalid id format (`abc`): 400 | FR-5 | EPMCDMETST-67221 |
| AC-4 | Compatibility with ProductDetail.jsx (Notes) | NFR-1, FR-7, FR-8 | EPMCDMETST-67222 (verification/tests) |

Sub-task mapping is an assumption (implementation vs. verification); sub-task titles were not available in the fetched Story.

## 7. Out of Scope
POST/PUT/DELETE, frontend changes, new Product fields, CORS changes, custom error DTO, pagination/search.

## 8. Assumptions
- Boot's default conversion failure (`MethodArgumentTypeMismatchException`) yields 400; missing product raises `ResponseStatusException(404)` or equivalent.
- Category serialisation causes no recursion (Category has no back-reference to Product - to be confirmed in design).

## 9. Open Questions
None.
