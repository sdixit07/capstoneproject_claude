# Architecture & Design: EPMCDMETST-69860 - Prev/Next catalog pagination

- **Story**: EPMCDMETST-69860 (Epic EPMCDMETST-69857), labels: capstone, frontend, pagination
- **Inputs**: `requirements.md` (approved), `docs/KNOWN-ISSUES.md` (KI-1..KI-3), `ecom-front/ecom-catalog-react/src/App.jsx`
- **Scope**: front end only (`ecom-front/ecom-catalog-react/`). No backend work.

## 1. Architecture Recommendation

**Where the logic lives**

| Concern | Location | Justification |
|---|---|---|
| Slicing the catalog into pages | Backend (sibling Story, assumed) | NFR-1 requires that only 12 products are fetched per request. Slicing in the browser would still download everything. |
| Current page number, Prev/Next enable state, "Page X of Y" | Front end (`App.jsx` state + new `Pagination` component) | These are view state derived from the response metadata (`page`, `totalPages`). |
| Search, category filter, sort | Stay in the browser in `App.jsx` (FR-8) | Clarification 2. They run over the loaded page only (Known Limitation). Moving them server-side is out of scope. |

Today `App.jsx` fetches the whole list once and does everything in memory. The design changes only the fetch: it becomes a per-page fetch that re-runs when the page number changes. The existing `filteredProducts` pipeline is unchanged and is fed `items` instead of the full array.

**Assumed prerequisite (blocker if absent)**: `GET /api/products?page=<0-based>&size=<n>` returns `{items,page,size,totalItems,totalPages}`. The current API is unpaged and returns a bare array. If the sibling Story has not shipped, the front end cannot satisfy FR-2/FR-7. The design does not add a fallback for the bare array, to avoid hidden dual-format behaviour. The reviewer/tester should treat that as a blocker.

**Pre-development gate (D-1)**: before any code is written, the developer confirms the sibling endpoint, for example `curl "http://localhost:8080/api/products?page=0&size=12"` must return the documented shape. If it does not, development stops and a blocker is reported. No front-end fallback and no dual-format handling is added.

## 2. Component Diagram

```
+-------------------------------------------------------------+
| Browser: ecom-catalog-react                                 |
|                                                             |
|  App.jsx  (state: products, pageInfo, page, loading, error, |
|            categories, selectedCategory, searchTerm, sort)  |
|    |-- CategoryFilter  (existing, unchanged)                |
|    |-- search <input>, sort <select>  (existing)            |
|    |-- ProductList     (existing, unchanged)                |
|    '-- Pagination      (NEW: Prev | Page X of Y | Next)     |
|                                                             |
|  src/api/productsApi.js (NEW: fetchProducts, fetchCategories)|
+------------------------------|------------------------------+
                               | HTTP (JSON)
                               v
+-------------------------------------------------------------+
| Spring Boot :8080  (UNCHANGED in this Story)                |
|  GET /api/products?page=&size=  -> {items,page,size,        |
|                                     totalItems,totalPages}  |
|  GET /api/categories            -> [ {id,name}, ... ]       |
+-------------------------------------------------------------+
```

## 3. EPMCDMETST-69860 Components and Responsibilities

### Backend (Spring layers)
No change. Controller, service, repository and entities are untouched. The paged endpoint is owned by the sibling Story.

### Front end (React)

| Component / module | Status | Responsibility |
|---|---|---|
| `src/App.jsx` | Modified | Owns `page`, `pageInfo`, `loading`, `error`. Fetches the page when `page` changes. Applies the existing client-side filter/sort to `items`. Renders `Pagination`. |
| `src/Pagination.jsx` | New | Presentational. Renders Prev button, "Page X of Y" label, Next button. Receives props only; holds no state. |
| `src/api/productsApi.js` | New | `fetchProducts({page,size})` and `fetchCategories()`. Centralises the base URL and `response.ok` checking. |
| `ProductList.jsx`, `CategoryFilter.jsx` | Unchanged | - |

## 4. Technology Choices

| Choice | Reason | Rejected options |
|---|---|---|
| Plain React state (`useState`/`useEffect`) | Matches existing code; no new dependency. | React Query / SWR (new dependency, not justified for one list). Redux (overkill). |
| `fetch` wrapped in a small API module | Matches existing `fetch` use; gives a seam for unit tests with a mocked `fetch`. | Axios (new dependency). Inline `fetch` in `App.jsx` (harder to test). |
| Local component state for `page` | URL-synced page state is out of scope. | `react-router` query params (new dependency, out of scope). |
| Stale-response guard via an effect cleanup flag (`let cancelled`) | Prevents an out-of-order response from overwriting a newer page. No dependency. | `AbortController` (also valid; slightly more code. Either is acceptable to the developer). |

No new frameworks or dependencies. Per D-3 this holds for test tooling too: no jsdom, happy-dom or Testing Library unless the user later approves it explicitly.

## 5. Data Flow

1. `App` mounts with `page = 0`. Effect runs `fetchProducts({page:0, size:12})`, sets `loading = true`.
2. Response `{items,page,size,totalItems,totalPages}` is stored: `products = items`, `pageInfo = {page,totalPages}`; `loading = false`.
3. `App` computes `filteredProducts` from `products` with the existing search/category/sort logic and renders `ProductList`.
4. `Pagination` shows "Page {page+1} of {totalPages}". Prev is disabled when `page === 0`. Next is disabled when `page + 1 >= totalPages`.
5. Click Next sets `page = page + 1` (Prev: `page - 1`). The effect re-runs for the new page, back to step 1.
6. Categories are fetched once on mount, independent of paging.

## 6. Low-Level Design

### 6.1 Backend
Not applicable (no entity, repository, service, controller, validation or error changes).

### 6.2 Frontend

**Constants**: `PAGE_SIZE = 12` (in `App.jsx`; page-size selector is out of scope).

**State in `App.jsx`**

| State | Initial | Notes |
|---|---|---|
| `products` | `[]` | `items` of the current page |
| `page` | `0` | requested page, 0-based (what the effect fetches) |
| `loadedPage` | `0` | last page that loaded successfully (D-2); drives the label and button state |
| `totalPages` | `0` | from the last successful response |
| `loading` | `true` | true while a page request is in flight |
| `error` | `null` | fixed user-facing text when a request fails (D-8) |
| existing: `categories`, `selectedCategory`, `searchTerm`, `sortorder` | unchanged | |

The displayed page number comes from `loadedPage`, with `totalPages` from the response, so the label always matches the content shown. The response `page` field is not used to drive navigation.

**Effect (D-2, D-6, D-8)**

```
useEffect(() => {
  let cancelled = false
  setLoading(true); setError(null)
  fetchProducts({ page, size: PAGE_SIZE })   // validates Array.isArray(items), throws otherwise
    .then(d => {
      if (cancelled) return
      const last = Math.max(d.totalPages - 1, 0)
      if (page > last) { setPage(last); return }   // clamp: totalPages shrank (D-8); effect re-runs
      setProducts(d.items); setTotalPages(d.totalPages); setLoadedPage(page)
    })
    .catch(e => {
      if (cancelled) return
      console.error(e.message)               // log detail, show fixed text (D-8)
      setError('Could not load products.')
      setPage(loadedPage)                    // roll back to last loaded page (D-2)
    })
    .finally(() => { if (!cancelled) setLoading(false) })
  return () => { cancelled = true }          // kept for unmount and stale responses (D-6)
}, [page, retryToken])
```

`retryToken` is a counter incremented by the Retry button so a retry re-requests the current `page` even when rollback leaves `page` unchanged (on initial failure, `page` stays 0 and Retry re-requests page 0). The clamp branch leaves `loading` true until the re-run completes. The developer may restructure the effect, provided the D-2 and D-8 behaviours hold.

**Handlers**: `goPrev = () => setPage(loadedPage - 1)`, `goNext = () => setPage(loadedPage + 1)`, `retry = () => setRetryToken(t => t + 1)`. Guarded by the disabled state, so `page` never goes below 0 or at/after `totalPages`.

**Filter/sort handlers (OQ-1, Option A)**: `handleSearchChange`, `handleCategorySelect` and `handleSortChange` only update their own state. They do not call `setPage` and trigger no fetch.

**Limited-results hint (D-4)**: when `filteredProducts` is empty, `products.length > 0` and `totalPages > 1`, render a hint with `data-testid="catalog-limited-hint"`: "Search and filters apply to the current page only. Other pages may contain matches." It is shown in addition to the existing empty-list text.

**Loading region (D-6)**: while `loading`, the list area is replaced by the loading indicator (no stale items shown as current) and the container carries `aria-busy="true"`.

**Existing-state note**: `App.jsx` today has no loading or error handling (bare `fetch().then()`). NFR-3 says these states "remain handled", which does not match the baseline. This design adds a minimal loading indicator and error message for the product list; this is the least that makes NFR-3 meaningful. Flagged for the reviewer.

**`Pagination` props**: `page` (= `loadedPage`), `totalPages`, `disabled` (loading), `onPrev`, `onNext`.
- Prev `disabled = isPrevDisabled(...)`: `disabled || page <= 0`
- Next `disabled = isNextDisabled(...)`: `disabled || page + 1 >= totalPages`
- Label text from `pageLabel(page, totalPages)`.
- Pure helpers `isPrevDisabled`, `isNextDisabled`, `pageLabel` are exported from a plain module (for example `src/paginationState.js`) so they can be unit-tested in the node environment without a DOM (D-3).

**API module (`src/api/productsApi.js`)**
- A `BASE_URL = 'http://localhost:8080'` constant at the top of the module (D-8).
- `fetchProducts({ page, size })` GETs `${BASE_URL}/api/products?page=<page>&size=<size>`, throws `Error` if `!response.ok`, parses JSON, and throws if `Array.isArray(data.items)` is false (D-8). Returns the parsed JSON.
- `fetchCategories()` GETs `/api/categories` with the same `response.ok` handling.
- The existing `productsApi.test.js` (KI-2) imports these names. Adding the module lets the file load, but the test calls `fetchProducts` with `search`, `categoryId`, `sort` params, uses `vi` without import, and asserts a non-existent matcher `orOnlyCalled()`. It will still fail; per KNOWN-ISSUES it must not be "fixed" here. Do not edit that file (D-7). New tests go in `src/api/productsApi.paging.test.js` (see section 11); the tester reports the pre-existing `productsApi.test.js` failure as attributable to KI-2, not this Story.

**Rendering and `data-testid`s**

| Element | Attribute |
|---|---|
| Pagination container | `data-testid="pagination"` |
| Prev button (`<button type="button">Prev</button>`) | `data-testid="pagination-prev"` |
| Next button (`<button type="button">Next</button>`) | `data-testid="pagination-next"` |
| Label "Page X of Y" | `data-testid="pagination-label"` |
| Loading indicator | `data-testid="catalog-loading"` |
| Error message (`role="alert"`) | `data-testid="catalog-error"` |
| Retry button in the error region | `data-testid="catalog-retry"` |
| Limited-results hint (D-4) | `data-testid="catalog-limited-hint"` |
| Empty-catalog message (D-5) | `data-testid="catalog-empty"` |

Buttons are native `<button>` elements with visible text "Prev" and "Next" (NFR-2), reachable by role/name.

**Empty case (D-5, deviates from literal FR-6)**: if `totalPages === 0`, both Prev and Next stay rendered but disabled (the e2e spec requires them visible), the `pagination-label` is hidden (FR-6 literal text would give "Page 1 of 0"), and the message "No products available." is shown. This is a documented deviation from FR-6 for the empty catalog only; FR-6 applies unchanged whenever `totalPages >= 1`.

## 7. API Contract

Consumed, not provided. Backward compatibility is the sibling Story's concern.

```
GET /api/products?page=0&size=12
200 -> { "items":[Product...], "page":0, "size":12, "totalItems":N, "totalPages":M }
```

`Product` fields used: `id, name, description, price, imageUrl, category.id`. `GET /api/categories` is unchanged.

## 8. Wireframes

```
Product Catalog
[All Categories v] [ Search for products      ] [Sort by Price: Low..v]

+--------+ +--------+ +--------+
| card   | | card   | | card   |   (up to 12 on the page)
+--------+ +--------+ +--------+

        [ Prev ]   Page 1 of 5   [ Next ]
        (disabled)

Loading:   "Loading products..."  (list area replaced, aria-busy, Prev/Next disabled)
Error:     [!] Could not load products.  [ Retry ]
           (page rolled back to last loaded page; Prev/Next follow loaded state)
Hint:      "Search and filters apply to the current page only..."
           (shown when filtered list is empty, page has items, totalPages > 1)
Empty:     [ Prev ] [ Next ]  both disabled, no label
           "No products available."
```

## 9. Error Handling

| Situation | Behaviour |
|---|---|
| Request fails or non-2xx | Fixed text "Could not load products." shown with `role="alert"` and a Retry button; `e.message` is logged to the console only (D-8). `loading` is cleared. |
| Page request fails after a successful one (D-2) | `page` is rolled back to `loadedPage`; the label and content stay consistent. Retry re-requests; Prev/Next work from loaded state. |
| Initial page-0 request fails (D-2) | `totalPages` stays 0, Prev/Next disabled; Retry re-requests page 0. |
| `totalPages` shrinks below current `page` (D-8) | Page is clamped to `totalPages - 1` (or 0) and refetched. |
| Out-of-order responses | Ignored via cleanup flag (kept for unmount, D-6). |
| Filtered page has no matches | Existing "No prodcuts to display." text (typo is pre-existing; not changed), plus the limited-results hint when applicable (D-4). |
| Empty catalog (`totalPages = 0`) | "No products available."; buttons disabled; label hidden (D-5). |
| Backend not paged (bare array) | `fetchProducts` throws because `items` is not an array; shown as the error above. Blocker, see the D-1 gate in section 1. |

## 10. Decisions Needing Resolution

### OQ-1 (RESOLVED by user: Option A, with the D-4 hint): do search/category/sort changes reset to page 0?

**Ruling**: Option A. Search, category and sort changes do not reset the page and trigger no fetch. The "results limited to current page" hint (D-4, section 6.2) is shown when the filtered list is empty while the page has items and `totalPages > 1`. The options table and original analysis below are kept for history.

Search, category and sort are client-side and operate on the loaded page only, which is what makes this question awkward.

| Option | Behaviour | Pros | Cons |
|---|---|---|---|
| A. No reset (stay on current page) | Filter/sort changes only re-derive `filteredProducts`; no fetch. | Matches client-side reality: dataset for the page is unchanged. No surprise navigation or extra request. Simplest. Preserves Prev/Next state. | A search that matches nothing on this page can leave the user on an empty page while matches exist elsewhere (already a documented Known Limitation). |
| B. Reset to page 0 | Any filter/sort change sets `page = 0`, triggering a fetch of page 0. | Predictable starting point; aligns with the eventual server-side model and the e2e spec title "resets to page 1 behavior". | Throws away the page the user is on, causes a fetch for a change that needs none, and still does not make results complete because filtering stays page-local. Would be rework when search moves server-side. |

**Recommendation: Option A** for this Story, with the decision revisited when server-side search/category/sort is built. The design isolates the choice to the three handlers (`handleSearchChange`, `handleCategorySelect`, `handleSortChange`); Option B is a one-line `setPage(0)` in each, so the design supports either with no structural change. The user (or design review) should rule on it before development. Note that the existing e2e spec passes under either option because it searches from page 0.

### NFR-3 (confirm in design): disable Prev/Next while a request is in flight

**Confirmed (D-6)**: `Pagination` receives `disabled={loading}` and disables both buttons during any in-flight page request; the list area shows the loading indicator with `aria-busy`. Rationale: prevents rapid double clicks from skipping pages and removes the need for request queueing, and it is cheap. Cost: a slow request visibly blocks navigation, acceptable at 12 items per page. This relies on the user accepting the "reasonable default" labelled in the requirement.

## 11. Risks

| # | Risk | Mitigation |
|---|---|---|
| R1 | Sibling paged endpoint is missing or has a different shape. | Pre-development gate (D-1): confirm with curl before coding; if absent, stop and report a blocker. No fallback. |
| R2 | Page-local filtering gives incomplete results (Known Limitation). | Documented; OQ-1 resolved as Option A plus the D-4 hint. |
| R3 | Baseline failures KI-1/KI-2/KI-3 make `npm test` and lint red regardless of this Story. | Add no new lint errors beyond the 6 baseline ones. New tests: `src/api/productsApi.paging.test.js` (mocked `fetch`), a node-environment test of the pure helpers (for example `src/paginationState.test.js`), with `vi` imported from `vitest`. AC-1..AC-3 are covered by the existing Playwright spec plus one new e2e spec file. Do not edit `productsApi.test.js` or the existing Playwright spec (D-7). The tester reports KI-1/KI-2 failures separately. |
| R4 | No DOM test environment (vitest 2 only; no jsdom/happy-dom/Testing Library; default environment is node). | Resolved by D-3: no new dependency; no component tests (`Pagination.test.jsx`, `App.test.jsx` are not planned). Adding jsdom and Testing Library requires explicit user approval. |
| R5 | Existing unused imports in `App.jsx` (KI-3) remain. | Left untouched; not part of this Story. |

## 12. Traceability: FR / NFR / AC to Design

| Requirement | Design element |
|---|---|
| FR-1 | `Pagination` renders Prev and Next buttons (6.2, wireframe) |
| FR-2 | Mount effect, `page = 0`, `PAGE_SIZE = 12`, `fetchProducts` (5, 6.2) |
| FR-3 | Prev `disabled` when `page <= 0` (6.2) |
| FR-4 | Next `disabled` when `page + 1 >= totalPages` (6.2) |
| FR-5 | `goPrev`/`goNext` change `page`; effect refetches (5, 6.2) |
| FR-6 | `pagination-label` "Page {loadedPage+1} of {totalPages}" (6.2); empty catalog deviates, see D-5 |
| FR-7 | `productsApi.fetchProducts` reads `{items,...}` (6.2, 7) |
| FR-8 | `filteredProducts` pipeline retained over `products` (1, 5) |
| NFR-1 | Server-sliced request with `size=12` (1, 5) |
| NFR-2 | Native `<button>` with text "Prev"/"Next" (6.2) |
| NFR-3 | `loading`/`error` states; `disabled={loading}` (6.2, 9, 10) |
| NFR-4 | Front-end only; no new lint errors; new tests added separately (11, R3) |
| AC-1 | Prev disabled at page 0 after load |
| AC-2 | Next click requests page 1 and shows its items |
| AC-3 | Next disabled when `page + 1 >= totalPages` |
| OQ-1 | Resolved: Option A + D-4 hint (section 10, 6.2) |

## 13. Changes after design review

Source: `design-review.md`. The user accepted D-1..D-8 and ruled OQ-1 = Option A.

| Decision | What changed | Where |
|---|---|---|
| D-1 | Added pre-development gate (curl the paged endpoint; stop and report a blocker if absent; no fallback). | Section 1, R1 |
| D-2 | Added `loadedPage` and `retryToken`; failed requests roll `page` back to `loadedPage`; Retry button and `catalog-retry`; initial failure retries page 0. | 6.2, 8, 9 |
| D-3 | No new dependencies, including test tooling. Pure helpers tested in node, mocked-fetch API tests, Playwright for AC-1..AC-3. Component tests dropped. Adding jsdom/Testing Library needs explicit user approval. | 4, 6.2, R3, R4 |
| D-4 | OQ-1 resolved as Option A; limited-results hint with `catalog-limited-hint`. | 6.2, 8, 9, 10 |
| D-5 | Empty catalog: buttons rendered but disabled, label hidden, "No products available." This deviates from literal FR-6 text ("Page 1 of 0") for the empty case only. | 6.2, 9, 12 |
| D-6 | List area replaced by loading indicator, `aria-busy`, `disabled={loading}`, cancel flag kept. | 6.2, 8, 10 |
| D-7 | `productsApi.test.js` untouched; new file `productsApi.paging.test.js`; KI-2 failure attributed to KI-2. | 6.2, R3 |
| D-8 | `Array.isArray(items)` validation, `BASE_URL` constant, fixed error text with `e.message` logged, page clamped to `totalPages - 1`. | 6.2, 9 |
| OQ-1 | RESOLVED: Option A (no reset), hint per D-4. | 10, 12 |
