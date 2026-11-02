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

No new frameworks or dependencies.

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
| `page` | `0` | requested page, 0-based |
| `totalPages` | `0` | from the response |
| `loading` | `true` | true while a page request is in flight |
| `error` | `null` | message string when a request fails |
| existing: `categories`, `selectedCategory`, `searchTerm`, `sortorder` | unchanged | |

The displayed page number comes from the requested `page`, with `totalPages` from the response. The response `page` field is not used to drive navigation, so the two cannot disagree.

**Effect**

```
useEffect(() => {
  let cancelled = false
  setLoading(true); setError(null)
  fetchProducts({ page, size: PAGE_SIZE })
    .then(d => { if (!cancelled) { setProducts(d.items); setTotalPages(d.totalPages) } })
    .catch(e => { if (!cancelled) setError(e.message) })
    .finally(() => { if (!cancelled) setLoading(false) })
  return () => { cancelled = true }
}, [page])
```

**Handlers**: `goPrev = () => setPage(p => p - 1)`, `goNext = () => setPage(p => p + 1)`. Guarded by the disabled state, so `page` never goes below 0 or at/after `totalPages`.

**Existing-state note**: `App.jsx` today has no loading or error handling (bare `fetch().then()`). NFR-3 says these states "remain handled", which does not match the baseline. This design adds a minimal loading indicator and error message for the product list; this is the least that makes NFR-3 meaningful. Flagged for the reviewer.

**`Pagination` props**: `page`, `totalPages`, `disabled` (loading), `onPrev`, `onNext`.
- Prev `disabled = disabled || page <= 0`
- Next `disabled = disabled || page + 1 >= totalPages`

**API module (`src/api/productsApi.js`)**
- `fetchProducts({ page, size })` GETs `http://localhost:8080/api/products?page=<page>&size=<size>`, throws `Error` if `!response.ok`, returns parsed JSON.
- `fetchCategories()` GETs `/api/categories` with the same handling.
- The existing `productsApi.test.js` (KI-2) imports these names. Adding the module lets the file load, but the test calls `fetchProducts` with `search`, `categoryId`, `sort` params, uses `vi` without import, and asserts a non-existent matcher `orOnlyCalled()`. It will still fail; per KNOWN-ISSUES it must not be "fixed" here. New tests for this Story go in a new file (see section 11).

**Rendering and `data-testid`s**

| Element | Attribute |
|---|---|
| Pagination container | `data-testid="pagination"` |
| Prev button (`<button type="button">Prev</button>`) | `data-testid="pagination-prev"` |
| Next button (`<button type="button">Next</button>`) | `data-testid="pagination-next"` |
| Label "Page X of Y" | `data-testid="pagination-label"` |
| Loading indicator | `data-testid="catalog-loading"` |
| Error message (`role="alert"`) | `data-testid="catalog-error"` |

Buttons are native `<button>` elements with visible text "Prev" and "Next" (NFR-2), reachable by role/name.

**Empty case**: if `totalPages` is 0, the label shows "Page 1 of 0" per FR-6 literal rule. Design proposes hiding the label and disabling both buttons when `totalPages === 0`; reviewer to confirm (requirements do not cover it).

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

Loading:   "Loading products..."  (Prev/Next disabled)
Error:     [!] Could not load products.  (Prev/Next remain usable once not loading)
```

## 9. Error Handling

| Situation | Behaviour |
|---|---|
| Request fails or non-2xx | `error` set; message shown with `role="alert"`; the previously loaded `products` are not shown as if current (list area shows the error). `loading` cleared so Prev/Next can retry. |
| Page request fails after a successful one | `page` has already changed. Prev/Next stay available, so the user can step back or retry. (Alternative: roll back `page`; not chosen, to keep the effect simple.) |
| Out-of-order responses | Ignored via cleanup flag. |
| Filtered page has no matches | Existing "No prodcuts to display." text (typo is pre-existing; not changed). |
| Backend not paged (bare array) | `d.items` undefined; treated as an error. Blocker, see section 1. |

## 10. Decisions Needing Resolution

### OQ-1 (OPEN, not decided): do search/category/sort changes reset to page 0?

Search, category and sort are client-side and operate on the loaded page only, which is what makes this question awkward.

| Option | Behaviour | Pros | Cons |
|---|---|---|---|
| A. No reset (stay on current page) | Filter/sort changes only re-derive `filteredProducts`; no fetch. | Matches client-side reality: dataset for the page is unchanged. No surprise navigation or extra request. Simplest. Preserves Prev/Next state. | A search that matches nothing on this page can leave the user on an empty page while matches exist elsewhere (already a documented Known Limitation). |
| B. Reset to page 0 | Any filter/sort change sets `page = 0`, triggering a fetch of page 0. | Predictable starting point; aligns with the eventual server-side model and the e2e spec title "resets to page 1 behavior". | Throws away the page the user is on, causes a fetch for a change that needs none, and still does not make results complete because filtering stays page-local. Would be rework when search moves server-side. |

**Recommendation: Option A** for this Story, with the decision revisited when server-side search/category/sort is built. The design isolates the choice to the three handlers (`handleSearchChange`, `handleCategorySelect`, `handleSortChange`); Option B is a one-line `setPage(0)` in each, so the design supports either with no structural change. The user (or design review) should rule on it before development. Note that the existing e2e spec passes under either option because it searches from page 0.

### NFR-3 (confirm in design): disable Prev/Next while a request is in flight

**Proposed and confirmed here as the design default**: `Pagination` receives `disabled={loading}` and disables both buttons during any in-flight page request. Rationale: prevents rapid double clicks from skipping pages and removes the need for request queueing, and it is cheap. Cost: a slow request visibly blocks navigation, acceptable at 12 items per page. This relies on the user accepting the "reasonable default" labelled in the requirement.

## 11. Risks

| # | Risk | Mitigation |
|---|---|---|
| R1 | Sibling paged endpoint is missing or has a different shape. | Stated as a blocker; contract fixed in section 7; developer verifies against the running API before coding. |
| R2 | Page-local filtering gives incomplete results (Known Limitation). | Documented; OQ-1 recommendation A. |
| R3 | Baseline failures KI-1/KI-2/KI-3 make `npm test` and lint red regardless of this Story. | Add no new lint errors beyond the 6 baseline ones. New unit tests go in a new file (for example `src/Pagination.test.jsx` and `src/App.test.jsx`) with `vi` imported from `vitest`. Do not edit the orphan test or the Playwright spec. Note KI-1 means vitest will still fail the Playwright suite; test-agent must report new tests separately. |
| R4 | A test runner for component tests (jsdom, Testing Library) is not confirmed in `package.json`. | Developer to check. If absent, prefer tests that need no new dependency (API module with mocked `fetch`); adding a dev dependency requires a requirement-level justification. |
| R5 | Existing unused imports in `App.jsx` (KI-3) remain. | Left untouched; not part of this Story. |

## 12. Traceability: FR / NFR / AC to Design

| Requirement | Design element |
|---|---|
| FR-1 | `Pagination` renders Prev and Next buttons (6.2, wireframe) |
| FR-2 | Mount effect, `page = 0`, `PAGE_SIZE = 12`, `fetchProducts` (5, 6.2) |
| FR-3 | Prev `disabled` when `page <= 0` (6.2) |
| FR-4 | Next `disabled` when `page + 1 >= totalPages` (6.2) |
| FR-5 | `goPrev`/`goNext` change `page`; effect refetches (5, 6.2) |
| FR-6 | `pagination-label` "Page {page+1} of {totalPages}" (6.2) |
| FR-7 | `productsApi.fetchProducts` reads `{items,...}` (6.2, 7) |
| FR-8 | `filteredProducts` pipeline retained over `products` (1, 5) |
| NFR-1 | Server-sliced request with `size=12` (1, 5) |
| NFR-2 | Native `<button>` with text "Prev"/"Next" (6.2) |
| NFR-3 | `loading`/`error` states; `disabled={loading}` (6.2, 9, 10) |
| NFR-4 | Front-end only; no new lint errors; new tests added separately (11, R3) |
| AC-1 | Prev disabled at page 0 after load |
| AC-2 | Next click requests page 1 and shows its items |
| AC-3 | Next disabled when `page + 1 >= totalPages` |
| OQ-1 | Open; options and recommendation in section 10 |
