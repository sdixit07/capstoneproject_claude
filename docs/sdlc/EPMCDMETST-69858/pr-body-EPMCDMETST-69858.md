## Summary

EPMCDMETST-69858: `GET /api/products` now supports pagination. It accepts `page` (default 0) and `size` (default 10, maximum 100) and returns a paged envelope instead of a bare array. Invalid values return `400`; a page past the end returns an empty page; products are ordered by ascending id. The seed data now has at least 25 products. All 10 acceptance criteria are covered by tests.

## Changes Made

- Backend: `PagedResponse` envelope, pagination in `ProductService` and `ProductController` with validation (400 on negative or non-numeric `page`, `size` outside 1..100 or non-numeric).
- `DataSeeder` seeds 25 products across 3 categories.
- Tests: `ProductPaginationTest`, `PagedResponseTest`, `ProductPaginationSeededIntegrationTest`, updated `ProductServiceTest` and `DataSeederTest`; Playwright API spec `tests/EPMCDMETST-69858.spec.js`.
- Front end tooling: `vite.config.js` excludes `tests/**` from vitest (resolves KI-1); `.gitignore` ignores Playwright `test-results`.
- Docs: README (pagination and local run commands without Docker), `CHANGELOG.md`, `docs/KNOWN-ISSUES.md` (KI-4 corrected to 3 failures), and the SDLC artifacts in `docs/sdlc/EPMCDMETST-69858/`.

## Test Evidence

From `docs/sdlc/EPMCDMETST-69858/test-report.md`:

| Suite | Passed | Failed | Skipped |
|---|---|---|---|
| Backend full (`mvnw clean test`) | 29 | 0 | 0 |
| Story backend tests (`ProductPaginationSeededIntegrationTest`) | 4 | 0 | 0 |
| Story API Playwright spec (`EPMCDMETST-69858.spec.js`) | 10 | 0 | 0 |
| Front-end unit (`npm test`) | 0 ran | 1 file failed (KI-2), baseline was 2 files | 0 |
| Front-end lint (`npm run lint`) | - | 6 errors (KI-3), same as baseline | - |
| E2E full (`npm run test:e2e`) | 10 | 3 | 1 |

- New failures versus baseline: 0.
- The 3 e2e failures are in `catalog-search-pagination.spec.js` (KI-4). They were verified on old `main` (091708d) with the same front end: the same 3 tests fail there. The skipped sort test is flaky between runs.
- Acceptance criteria: 10/10 verified. Document quality check: 0 errors.
- Coverage: UNMEASURED. There is no JaCoCo plugin in `pom.xml`, and by user decision it was not added. No coverage figure is claimed and the 85% gate is not shown as met.

## Known Limitations

- Coverage is UNMEASURED and the 85% gate is not shown as met (user decision not to add JaCoCo). The test report verdict is therefore not PASS.
- `App.jsx` expects a bare array and breaks against the new envelope. A follow-up front-end Story is needed.
- KI-2: `productsApi.test.js` imports a module that does not exist (baseline).
- KI-3: 6 baseline ESLint errors.
- KI-4: 3 e2e failures in `catalog-search-pagination.spec.js`, reproduced on old `main`; the sort test is flaky.
- `ProductControllerIT` is stale and is not run by Surefire.
- 400 responses carry no `message` field because `server.error.include-message` is not set.
- The API table in `CLAUDE.md` is out of date for `GET /api/products`.

## Reviewer Checklist

- [ ] `GET /api/products` defaults (page 0, size 10) and the size cap of 100 behave as described
- [ ] Invalid `page` or `size` returns 400; a page past the end returns an empty envelope
- [ ] Ordering is by ascending id with no duplicates across pages
- [ ] Breaking response shape is acknowledged and the front-end follow-up Story is raised
- [ ] Known issues KI-2, KI-3, KI-4 are baseline and not caused by this change
- [ ] Reviewer accepts the unmeasured coverage exception
- [ ] `CHANGELOG.md` and README are accurate

🤖 Generated with [Claude Code](https://claude.com/claude-code)
