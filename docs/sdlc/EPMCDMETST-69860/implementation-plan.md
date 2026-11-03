# Implementation Plan: EPMCDMETST-69860 - Prev/Next catalog pagination

- **Story**: EPMCDMETST-69860 (Epic EPMCDMETST-69857)
- **Branch**: feature/EPMCDMETST-69860
- **Inputs**: `requirements.md`, `architecture.md` (final, section 13), `design-review.md` (APPROVED WITH CHANGES; D-1..D-8, OQ-1 = Option A, notes N-1, N-2)
- **Scope**: all code tasks are in the front end, `ecom-front/ecom-catalog-react/`. No `ecom-project/` (backend) change. No new dependencies, including test tooling (D-3).
- **Do not touch**: `src/api/productsApi.test.js` (D-7, KI-2), `tests/catalog-search-pagination.spec.js`, `ProductList.jsx`, `CategoryFilter.jsx`, KI-3 unused imports in `App.jsx`.

Paths below are relative to `ecom-front/ecom-catalog-react/` unless stated. Effort: S under 1h, M 1-3h.

## Tasks (ordered by dependency, then priority)

### T-0 - Gate D-1: confirm the paged endpoint (P1, layer: ecom-project/ runtime check only, no edits)
- **Description**: Start the backend (`./mvnw spring-boot:run` from `ecom-project/`, or confirm it is running) and run `curl "http://localhost:8080/api/products?page=0&size=12"`. The response must be JSON with `items` (array), `page`, `size`, `totalItems`, `totalPages`. Also check that a later page responds, for example `?page=1&size=12`, if `totalPages > 1`.
- **Pass**: documented shape returned. Record the command and the response excerpt in the dev notes.
- **Fail**: response is a bare array, 404, or any other shape. STOP. Do not start T-1..T-9. Report a BLOCKER (F-1 / R1: sibling paged-endpoint Story not available) to the caller. Add no fallback and no dual-format handling. Do not make backend changes.
- **Files**: none
- **Traces**: FR-2, FR-7, AC-1..AC-3; F-1, D-1, R1
- **Depends on**: none
- **Effort**: S

### T-1 - API module (P1, layer: ecom-front/)
- **Description**: Create `src/api/productsApi.js` with a top-level `BASE_URL = 'http://localhost:8080'`. `fetchProducts({page,size})` GETs `${BASE_URL}/api/products?page=<page>&size=<size>`, throws `Error` when `!response.ok`, parses JSON, throws when `Array.isArray(data.items)` is false (D-8), returns the parsed JSON. `fetchCategories()` GETs `/api/categories` with the same `response.ok` check. Named exports, matching the names imported by the legacy test.
- **Files**: `src/api/productsApi.js` (new)
- **Tests**: covered by T-6
- **Traces**: FR-2, FR-7, NFR-1; D-8
- **Depends on**: T-0
- **Effort**: S

### T-2 - Pure pagination helpers (P1, layer: ecom-front/)
- **Description**: Create `src/paginationState.js` exporting `isPrevDisabled(page, totalPages, disabled)` (`disabled || page <= 0`), `isNextDisabled(page, totalPages, disabled)` (`disabled || page + 1 >= totalPages`) and `pageLabel(page, totalPages)` (`Page ${page+1} of ${totalPages}`). No React or DOM imports, so they test in the node environment (D-3).
- **Files**: `src/paginationState.js` (new)
- **Tests**: covered by T-7
- **Traces**: FR-3, FR-4, FR-6, AC-1, AC-3; D-3
- **Depends on**: T-0
- **Effort**: S

### T-3 - `Pagination` component (P1, layer: ecom-front/)
- **Description**: Create a presentational, stateless component with props `page` (= `loadedPage`), `totalPages`, `disabled`, `onPrev`, `onNext`. Render a container (`data-testid="pagination"`), `<button type="button">Prev</button>` (`pagination-prev`), label (`pagination-label`) and `<button type="button">Next</button>` (`pagination-next`). Button disabled state comes from the T-2 helpers. When `totalPages === 0` (D-5) keep both buttons rendered and disabled and hide the label.
- **Files**: `src/Pagination.jsx` (new)
- **Tests**: no component tests (D-3); behaviour covered by T-7 helpers and T-9 e2e
- **Traces**: FR-1, FR-3, FR-4, FR-6, NFR-2, NFR-3; D-5, D-6
- **Depends on**: T-2
- **Effort**: S

### T-4 - Wire paging into `App.jsx` (P1, layer: ecom-front/)
- **Description**:
  1. Add `PAGE_SIZE = 12` and the state `products`, `page`, `loadedPage`, `totalPages`, `loading` (initial true), `error` (null), `retryToken` (0). Existing `categories`, `selectedCategory`, `searchTerm`, `sortorder` unchanged. Replace the bare `fetch` calls with `fetchProducts` / `fetchCategories` (categories fetched once on mount).
  2. Page effect with the cancel flag (`let cancelled`), per the architecture 6.2 sketch: on success store `items`, `totalPages`, set `loadedPage = page`; clamp when `page > max(totalPages-1,0)` by `setPage(last)` (D-8); on failure log `e.message` with `console.error`, set the fixed text "Could not load products." and roll `page` back to `loadedPage` (D-2); `finally` clears `loading` unless cancelled.
  3. **N-1**: the error and Retry must stay visible after a rollback. Do not let the rollback-triggered re-run clear `error`. Acceptable approach: separate the requested page from the fetched page, or skip `setError(null)` and the refetch when `page === loadedPage` and the run was caused by rollback (for example track the requested page in a ref). Verify the result: when Next fails, the error and Retry remain visible, the label still shows the previous page, and the content is the previous page's items.
  4. **N-2**: `retryToken` is state (`useState`), incremented by `retry = () => setRetryToken(t => t + 1)`, and listed in the effect deps with `page`. If the effect reads `loadedPage`, either list it correctly without causing a refetch loop or read it through a ref, so `react-hooks/exhaustive-deps` does not add a warning or error. Run `npm run lint` before and after: the error count must stay at the 6 baseline; no new lint errors.
  5. Handlers `goPrev = () => setPage(loadedPage - 1)`, `goNext = () => setPage(loadedPage + 1)`, `retry`. `handleSearchChange`, `handleCategorySelect`, `handleSortChange` only update their own state and never call `setPage` (OQ-1 Option A).
  6. Render: while `loading`, the list area shows the loading indicator (`catalog-loading`, text "Loading products...") instead of stale items and the container has `aria-busy="true"` (D-6). Error region `role="alert"` (`catalog-error`) with `catalog-retry` button. Render `<Pagination page={loadedPage} totalPages={totalPages} disabled={loading} .../>`. Hint `catalog-limited-hint` ("Search and filters apply to the current page only. Other pages may contain matches.") when `filteredProducts` is empty, `products.length > 0` and `totalPages > 1` (D-4). Empty catalog (`totalPages === 0` and not loading and no error): `catalog-empty` "No products available." (D-5). Keep the existing "No prodcuts to display." text (typo untouched).
- **Files**: `src/App.jsx` (modified)
- **Tests**: T-9 (e2e). Component tests not planned (D-3).
- **Traces**: FR-2, FR-5, FR-6, FR-8, NFR-1, NFR-3, AC-1..AC-3; D-2, D-4, D-5, D-6, D-8, OQ-1, N-1, N-2
- **Depends on**: T-1, T-3
- **Effort**: M

### T-5 - Styles for pagination and states (P3, layer: ecom-front/)
- **Description**: Minimal CSS in `src/App.css` for the pagination row (centered, spacing), disabled button look, error region, hint and loading text. No CSS framework.
- **Files**: `src/App.css` (modified)
- **Traces**: FR-1, NFR-2 (presentation only)
- **Depends on**: T-3 (class names)
- **Effort**: S

### T-6 - Unit tests: API module with mocked fetch (P1, Step 5, layer: ecom-front/)
- **Description**: Create `src/api/productsApi.paging.test.js`, importing `vi` from `vitest`, stubbing global `fetch`. Cases: `fetchProducts` builds `?page=0&size=12` and `?page=2&size=12`; returns parsed JSON; throws on non-2xx; throws when `items` is not an array (bare array and missing `items`); `fetchCategories` ok and non-ok. Do not edit `productsApi.test.js`.
- **Files**: `src/api/productsApi.paging.test.js` (new)
- **Traces**: FR-2, FR-7, NFR-1, NFR-4; D-3, D-7, D-8
- **Depends on**: T-1
- **Effort**: S

### T-7 - Unit tests: pure helpers (P1, Step 5, layer: ecom-front/)
- **Description**: Create `src/paginationState.test.js` (node environment, no DOM). Cases: Prev disabled at page 0, enabled at page 1; Next disabled when `page+1 >= totalPages` (last page, and `totalPages` 0 and 1), enabled otherwise; both disabled when `disabled` (loading) is true; `pageLabel(0,5)` is "Page 1 of 5", `pageLabel(4,5)` is "Page 5 of 5".
- **Files**: `src/paginationState.test.js` (new)
- **Traces**: FR-3, FR-4, FR-6, NFR-3, AC-1, AC-3; D-3
- **Depends on**: T-2
- **Effort**: S

### T-8 - Unit test: Next fails (rollback) logic (P2, Step 5, layer: ecom-front/)
- **Description**: N-1 coverage inside the D-3 limits. If the rollback/clamp decision is extracted as a pure function (for example `resolvePageResult(page, totalPages)` or a reducer in `paginationState.js`), test it in node: clamp when `page > totalPages-1`, no clamp otherwise, `totalPages` 0 gives 0. Cover "Next fails" end to end in T-9 using Playwright route mocking. If nothing is extractable, this task is subsumed by T-9 and is closed as such in the dev notes.
- **Files**: `src/paginationState.test.js` (extend), possibly `src/paginationState.js`
- **Traces**: FR-5, D-2, D-8, N-1
- **Depends on**: T-4, T-7
- **Effort**: S

### T-9 - Playwright e2e for AC-1..AC-3 and failure paths (P1, Step 5, layer: ecom-front/)
- **Description**: New spec `tests/catalog-pagination-nav.spec.js` (do not edit `tests/catalog-search-pagination.spec.js`). Use `page.route` to mock `/api/products` and `/api/categories` where determinism is needed; locate by role/name and the `data-testid`s. Cases:
  - AC-1: page 0 loaded, Prev disabled, label "Page 1 of N".
  - AC-2: click Next, request carries `page=1&size=12`, page 1 items shown, label "Page 2 of N".
  - AC-3: navigate to the last page, Next disabled.
  - Prev from page 1 returns to page 0 items.
  - Next fails (N-1): mock a 500 for page 1; `catalog-error` and `catalog-retry` stay visible, the label still shows "Page 1 of N", page 0 items are shown; Retry with a success mock loads page 1.
  - Initial failure: error and Retry visible, Prev/Next disabled; Retry loads page 0.
  - Buttons disabled while a delayed request is in flight, `aria-busy` set.
  - Search with no match on the page, `totalPages > 1`: `catalog-limited-hint` visible and no extra products request (OQ-1 Option A).
  - Empty catalog (`totalPages: 0`): buttons visible and disabled, no label, `catalog-empty` visible.
  - Clamp: mocked `totalPages` shrink causes a refetch of the last page.
- **Files**: `tests/catalog-pagination-nav.spec.js` (new)
- **Traces**: AC-1, AC-2, AC-3, FR-1..FR-6, FR-8, NFR-2, NFR-3; D-2, D-4, D-5, D-6, D-8, N-1
- **Depends on**: T-4 (T-5 optional)
- **Effort**: M

## Dependency graph (acyclic)

```
T-0 -> T-1 -> T-4 -> T-9
T-0 -> T-2 -> T-3 -> T-4
T-3 -> T-5
T-1 -> T-6
T-2 -> T-7 -> T-8 (also needs T-4)
```
Topological check: every edge goes from a lower to a higher position in the order T-0, T-1, T-2, T-3, T-4, T-5, T-6, T-7, T-8, T-9, so there is no cycle.

**Critical path**: T-0 -> T-2 -> T-3 -> T-4 -> T-9 (gate, helpers, component, App wiring, e2e). T-1 runs in parallel with T-2/T-3 and must finish before T-4.

## Task summary

| Priority | Tasks | Count |
|---|---|---|
| P1 | T-0, T-1, T-2, T-3, T-4, T-6, T-7, T-9 | 8 |
| P2 | T-8 | 1 |
| P3 | T-5 | 1 |
| Total | | 10 (T-0..T-9) |

## Blocked tasks

| Task | Blocked until | Why |
|---|---|---|
| T-1, T-2 (and therefore all later tasks) | T-0 passes | D-1 gate: without the paged endpoint FR-2/FR-7 cannot be met; no fallback allowed. If T-0 fails, all tasks stay blocked and a blocker is reported. |
| T-3 | T-2 | Uses the helpers for button state |
| T-4 | T-1, T-3 | Imports the API module and `Pagination` |
| T-5 | T-3 | Styles the component's classes |
| T-6 | T-1 | Tests the module |
| T-7 | T-2 | Tests the helpers |
| T-8 | T-4, T-7 | Needs the final effect/rollback logic and the helper test file |
| T-9 | T-4 | Needs the working UI and the data-testids |

Blocked-task count: 9 (T-1..T-9 directly or transitively on T-0).

## Verification outline (Step 7)

1. Gate: T-0 curl result recorded.
2. Unit: `npm test` from `ecom-front/ecom-catalog-react/`. New files (`productsApi.paging.test.js`, `paginationState.test.js`) must pass. `productsApi.test.js` is expected to fail (KI-2); report it as attributable to KI-2, not this Story.
3. Lint: `npm run lint`, no new errors beyond the 6 baseline (N-2).
4. Build: `npm run build` succeeds.
5. E2E: `npm run test:e2e` with the backend on :8080 running the paged endpoint. `tests/catalog-pagination-nav.spec.js` passes; the existing spec result is reported as is.
6. Manual check of the N-1 scenario (stop the backend between pages) if e2e mocking is judged insufficient.
7. Backend `./mvnw clean verify` is not required (no backend change); the 85% JaCoCo gate does not apply to front-end-only work. Note this in the test report.
8. Confirm `git diff` shows no change to `productsApi.test.js`, `tests/catalog-search-pagination.spec.js`, `package.json` or `package-lock.json`.

## Definition of Done

- T-0 gate passed and recorded (or blocker reported and work stopped).
- All of FR-1..FR-8, NFR-1..NFR-4 and AC-1..AC-3 traced to code and to a passing test; the FR-6 empty-catalog deviation (D-5) is documented.
- OQ-1 Option A implemented: filter and sort handlers never change the page.
- N-1: error and Retry stay visible after a failed Next/Prev, with the label and content from the previous page; test exists.
- N-2: `retryToken` is state and in the effect deps; no new lint errors beyond the 6 baseline.
- No new dependencies; `package.json` and the lock file unchanged (D-3).
- `productsApi.test.js` and the existing Playwright spec untouched (D-7).
- Conventional commits with the Story id on `feature/EPMCDMETST-69860`; no push to `main`.
