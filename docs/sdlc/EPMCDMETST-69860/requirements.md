# Requirements Specification
- **User Story**: EPMCDMETST-69860 - As a shopper, I want to navigate the product catalog using Prev/Next controls, so that I can browse products without loading the entire catalog at once. (Epic EPMCDMETST-69857; labels: capstone, frontend, pagination)

## Clarifications
| # | Question | Answer | Effect on requirements |
|---|----------|--------|------------------------|
| 1 | Backend paged endpoint: in scope? | Out of scope. Prerequisite from a sibling Story under EPMCDMETST-69857. Assume `GET /api/products?page=<0-based>&size=<n>` returns `{items,page,size,totalItems,totalPages}`. If unavailable, record as blocker; no backend work. | FR-7, Assumptions, Blockers; no backend FRs. |
| 2 | Scope of UI? | Prev/Next plus page loading only, default page size 12. Search, category and sort stay client-side and apply to the loaded page only (known limitation). Minimal "Page X of Y" label. Full server-side search/category/sort out of scope. | FR-5, FR-6, Out of Scope, Known Limitations. |
| 3 | Do filter/sort changes reset to page 0? | No ruling given. | Recorded as Open Question OQ-1; not decided. |

## Functional Requirements
- FR-1: The product catalog page shows "Prev" and "Next" buttons.
- FR-2: On initial load the page requests page 0 with size 12 and displays the returned `items`.
- FR-3: "Prev" is disabled when the current page is 0.
- FR-4: "Next" is disabled when the current page is the last page (page + 1 >= totalPages).
- FR-5: Clicking "Next" requests page current+1; clicking "Prev" requests page current-1; the displayed products are those returned for the requested page.
- FR-6: A label "Page X of Y" is shown (X = page + 1, Y = totalPages).
- FR-7: The front end reads products from the paged response shape `{items,page,size,totalItems,totalPages}` of `GET /api/products?page=&size=`.
- FR-8: Existing client-side search, category filter and sort remain, operating only on the currently loaded page.

## Non-Functional Requirements
- NFR-1: Only one page of products (12) is fetched per request; the whole catalog is not loaded at once.
- NFR-2: Buttons are real, keyboard-accessible button elements with the visible text "Prev" and "Next" (usable by Playwright/unit tests by role/name).
- NFR-3: Loading and error states of the existing list remain handled; pagination controls are not enabled for navigation while a request is in flight (Not Found in Story; reasonable default, confirm in design).
- NFR-4: Front-end tests (`npm test`) and lint remain passing; scope limited to `ecom-front/ecom-catalog-react/`.

## Acceptance Criteria
- AC-1: Given I open the Product Catalog page, when products are loaded for page 0, then "Prev" is disabled.
- AC-2: Given I am on page 0, when I click "Next", then products for page 1 are displayed.
- AC-3: Given I navigate to the last page, then "Next" is disabled.

## Assumptions and Blockers
- Assumption: the paged backend endpoint (sibling Story) exists with the shape above. Blocker if unavailable (current API `GET /api/products` is unpaged); no backend work in this Story.

## Out of Scope
- Backend changes; server-side search, category filtering and sorting; page-number links, page-size selector, URL-synced page state.

## Known Limitations
- Search, category and sort apply to the loaded page only, so results can be incomplete across the catalog.

## Open Questions
- OQ-1: Should changing search, category or sort reset the view to page 0? No user ruling; to be decided.
