# Design Review: EPMCDMETST-69860 - Prev/Next catalog pagination

- **Reviewed**: `architecture.md` (commit 287dd8a) against `requirements.md`, plus `App.jsx`, `package.json`, `vite.config.js`, `tests/catalog-search-pagination.spec.js`, `src/api/productsApi.test.js`, and the backend `ProductController` (still unpaged).
- **Mode**: review; updated in record mode (see Agreed design decisions)

## Findings

| ID | Severity | Area | Risk or gap | Recommendation |
|----|----------|------|-------------|----------------|
| F-1 | BLOCKER | Prerequisite | Paged endpoint is assumed. The repo's backend is still unpaged (`GET /api/products` returns a bare array), so FR-2/FR-7 cannot be met or verified end to end today. The design has no fallback and no gate. | Make verification of the sibling Story a pre-development gate (D-1). Do not add dual-format handling. |
| F-2 | MAJOR | Error handling | Section 9 says Prev/Next "stay usable" after a failure, but the state model contradicts it. If the initial page-0 request fails, `totalPages` stays 0, so Next is disabled, Prev is disabled, and there is no retry path. For a failure after navigation, `page` has already moved, `totalPages` is stale, and the page number shown does not match the content shown. | Roll back `page` to the last successfully loaded page on failure, track it as `loadedPage`, and add a Retry button in the error region (D-2). |
| F-3 | MAJOR | Test runner (R4) | Facts: `package.json` has vitest 2 only, no jsdom/happy-dom, no `@testing-library/*`, no vitest config, so the default environment is node. Component tests (`Pagination.test.jsx`, `App.test.jsx`) named in R3 cannot run. R4's fallback is left to the developer. | Decide now (D-3): add no dependency; unit-test the API module with mocked fetch, and extract pure helpers (`isPrevDisabled`, `isNextDisabled`, `pageLabel`) tested in node. Cover AC-1..AC-3 through the existing Playwright spec plus one new e2e spec. Adding jsdom and Testing Library is the alternative if the user prefers. |
| F-4 | MAJOR | OQ-1 | Still open. Both options are workable. Option B has a defect when combined with NFR-3: reset-to-0 triggers a fetch and disables controls, while the filter is applied to the old page for a moment. Option A has a user trap: a search with no match on this page leaves an empty list and no hint. | Recommend Option A plus a hint when the filtered page is empty but other pages exist (D-4). User must rule. |
| F-5 | MINOR | Empty catalog | `totalPages = 0` produces "Page 1 of 0" per FR-6 literal. Design proposes hiding the label and disabling both buttons but leaves it to the reviewer. The e2e spec test 1 requires Prev/Next to be visible, so the buttons must still render (disabled), not be removed. | Keep buttons rendered but disabled, hide the label, show "No products available." (D-5). Treat as a documented deviation from FR-6 for the empty case. |
| F-6 | MINOR | NFR-3 | The design default is sound. Gaps: (a) initial `loading = true` disables buttons, which is correct; (b) the cancelled-flag guard becomes mostly redundant with disabled buttons but is still useful for unmount, keep it; (c) the list should show the loading indicator without leaving stale items visible as current. | Accept as designed; specify that the list area is replaced by the loading indicator and add `aria-busy` (D-6). |
| F-7 | MINOR | KI-2 | Creating `productsApi.js` makes the orphan `productsApi.test.js` load but still fail (calls with `search/categoryId/sort`, missing `vi` import, bad matcher `orOnlyCalled`). Also a trap: the test name implies a contract with query params that the design deliberately does not support. | Do not edit that file. Name new tests `productsApi.paging.test.js`. Record in the test report that the pre-existing failure is attributable to KI-2, not this Story (D-7). |
| F-8 | MINOR | Robustness | If `totalPages` shrinks below the current `page` (data changed), page shows an empty list with Next disabled. Also `d.items` missing is "treated as an error" but the code snippet does not validate; `setProducts(undefined)` would crash `.filter`. | Validate `Array.isArray(d.items)` in `fetchProducts` and throw (D-8). Optionally clamp page to `totalPages-1`. |
| F-9 | MINOR | Conventions | Design hardcodes `http://localhost:8080` in the new module, same as today. Error text "Could not load products." vs `error.message` raw is inconsistent between section 8 and the effect. Pre-existing typo "prodcuts" retained. | Keep base URL constant at the top of the API module; show the fixed friendly text and log `e.message` (D-8). |

## Proposed design decisions

| ID | Decision | Resolves | Changes architecture.md? |
|----|----------|----------|--------------------------|
| D-1 | Gate development on confirming the sibling paged endpoint (curl `?page=0&size=12` returns the documented shape). If absent, stop and report a blocker; add no front-end fallback. | F-1 | yes (add gate to section 1 and R1) |
| D-2 | On failed page request, restore `page` to the last loaded page and show an error with a Retry button. Pagination stays driven by loaded state. On initial failure, Retry re-requests page 0. | F-2 | yes (sections 6.2, 8, 9) |
| D-3 | No new dependencies. Test with: mocked-fetch API tests, pure helper functions for button state and label, and Playwright e2e for AC-1..AC-3. Revisit jsdom/Testing Library only on explicit user approval. | F-3 | yes (R3, R4) |
| D-4 | OQ-1: choose Option A (no reset). When the filtered list is empty while the page has items and `totalPages > 1`, show a hint that results are limited to the current page. User to rule A or B. | F-4 | yes (section 10 resolved) |
| D-5 | Empty catalog (`totalPages = 0`): both buttons rendered and disabled, label hidden, message "No products available." | F-5 | yes (section 6.2) |
| D-6 | NFR-3 confirmed as designed: `disabled={loading}` on both buttons; list area shows loading indicator with `aria-busy`; keep the cancellation flag. | F-6 | yes (minor wording) |
| D-7 | Leave `productsApi.test.js` untouched; new tests in separate files; tester reports KI-1/KI-2 failures separately. | F-7 | yes (R3 filenames) |
| D-8 | Harden API module: validate `items` array, base URL constant, fixed user-facing error text, clamp page if beyond `totalPages`. | F-8, F-9 | yes (sections 6.2, 9) |

## Traceability

| Requirement | Covered by design | Review note |
|-------------|-------------------|-------------|
| FR-1, NFR-2 | `Pagination` native buttons | OK; must render in all states (D-5) |
| FR-2, FR-7 | Mount effect, `fetchProducts` | Depends on F-1 / D-1 |
| FR-3, FR-4 | Disabled expressions | OK; testable as pure helpers (D-3) |
| FR-5 | `goPrev` / `goNext` + effect | OK; failure behaviour fixed by D-2 |
| FR-6 | `pagination-label` | Empty case deviation (D-5) |
| FR-8 | `filteredProducts` unchanged | OK; OQ-1 via D-4 |
| NFR-1 | size=12 request | OK |
| NFR-3 | `disabled={loading}`, loading/error | OK with D-2, D-6 |
| NFR-4 | New test files, lint | Needs D-3, D-7 |
| AC-1..AC-3 | Section 12 | OK; test strategy via D-3 |
| OQ-1 | Section 10 | Awaiting user ruling (D-4) |

## Agreed design decisions

The user accepted all of D-1..D-8 as proposed and ruled OQ-1 = Option A (no reset on filter/sort change, with the D-4 hint). The architecture agent applied them in commit `658b53b` (`architecture.md` section 13). Verified against this review's recommendations.

| Decision | Agreed | Applied in commit | Verification |
|----------|--------|-------------------|--------------|
| D-1 | agreed as proposed | 658b53b | Applied (section 1 gate, R1). Gate is documented only; it is not yet executed. |
| D-2 | agreed as proposed | 658b53b | Applied (`loadedPage`, `retryToken`, rollback, Retry, `catalog-retry`; sections 6.2, 8, 9). See note N-1. |
| D-3 | agreed as proposed | 658b53b | Applied (sections 4, 6.2, R3, R4; `paginationState.js` helpers, no new dependencies). |
| D-4 | agreed as proposed (OQ-1 = Option A) | 658b53b | Applied (section 10 resolved, hint with `catalog-limited-hint`, handlers do not reset page). |
| D-5 | agreed as proposed | 658b53b | Applied (buttons rendered but disabled, label hidden, `catalog-empty`; deviation from FR-6 recorded in section 12). |
| D-6 | agreed as proposed | 658b53b | Applied (`disabled={loading}`, list replaced by loading indicator, `aria-busy`, cancel flag kept). |
| D-7 | agreed as proposed | 658b53b | Applied (`productsApi.test.js` untouched; `productsApi.paging.test.js`; KI-2 attribution). |
| D-8 | agreed as proposed | 658b53b | Applied (`Array.isArray(items)`, `BASE_URL`, fixed error text with `e.message` logged, page clamp). |

### Residual notes (not blocking; for developer and tester)

- **N-1 (D-2, MINOR, partly applied)**: In the section 6.2 effect, the catch branch calls `setPage(loadedPage)`. When a Next/Prev request fails, this changes `page` and re-runs the effect, which calls `setError(null)` and refetches the loaded page. The error message and Retry button can therefore be cleared at once, or flash, instead of persisting, which contradicts section 9 ("Retry re-requests"). The initial-failure path is unaffected (`page` stays 0). The section 6.2 text lets the developer restructure the effect provided the D-2 behaviour holds. Developer must ensure the error stays visible after rollback (for example, do not clear `error` on a rollback-triggered run, or track the requested page separately from the fetched page), and the tester should cover "Next fails: error and Retry visible, label still shows the previous page".
- **N-2 (MINOR)**: `retryToken` and its setter are described in prose but missing from the section 6.2 state table. The effect also reads `loadedPage` without listing it in the dependency array, which the lint `react-hooks/exhaustive-deps` rule may flag. Do not add new lint errors beyond the 6 baseline ones (NFR-4).

## Verdict

**APPROVED WITH CHANGES.** All MAJOR findings (F-2, F-3, F-4) and the MINOR findings (F-5..F-9) are resolved in the design by D-2..D-8 as applied in `658b53b`, subject to notes N-1 and N-2.

**F-1 stays OPEN (BLOCKER on execution, not on design).** The design now carries the D-1 gate, but the backend in this repository is still unpaged. F-1 clears only when the sibling paged-endpoint Story is confirmed available by running the D-1 gate (`curl "http://localhost:8080/api/products?page=0&size=12"` returns `{items,page,size,totalItems,totalPages}`). If the gate fails, development stops and a blocker is reported; no front-end fallback is added.
