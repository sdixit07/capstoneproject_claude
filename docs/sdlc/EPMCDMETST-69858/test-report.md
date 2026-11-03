# Test Report - EPMCDMETST-69858

## 1. Environment

| Item | Value |
|---|---|
| Commit SHA | 972427d (HEAD under test; first run was at 1913651/ab1f30b; re-run commit follows, see git log) |
| Branch | `feature/EPMCDMETST-69858` |
| Java / Maven | OpenJDK 21.0.5 / Maven wrapper (`mvnw.cmd`) |
| Node / npm | Node v24.19.0 |
| Database | H2 in-memory, seeded by `DataSeeder` (25 products, 3 categories) |

## 2. Test execution results

| Suite | Command | Passed | Failed | Skipped |
|---|---|---|---|---|
| Story backend tests (new `ProductPaginationSeededIntegrationTest`) | `.\mvnw.cmd test -Dtest=ProductPaginationSeededIntegrationTest` | 4 | 0 | 0 |
| Backend full | `.\mvnw.cmd clean test` | 29 | 0 | 0 |
| Story API-level Playwright | `npx playwright test tests/EPMCDMETST-69858.spec.js` (backend running) | 10 | 0 | 0 |
| Front-end unit | `npm test` | 0 (no tests ran) | 1 file failed (`productsApi.test.js`, KI-2) | 0 |
| Front-end lint | `npm run lint` | - | 6 errors | - |
| Front-end e2e (full) | `npm run test:e2e` (backend running) | 10 | 3 | 1 |

Backend breakdown (29): DataSeederTest 2, ProductPaginationSeededIntegrationTest 4 (new in Step 7), ProductPaginationTest 16, PagedResponseTest 2, EcomProjectApplicationTests 1, ProductServiceTest 4. `ProductControllerIT` is not run by Surefire (`*IT`; known stale, see implementation-notes R2).

### 2.1 Changes since the first run (ab1f30b)

- `npm test`: 3 failed files -> 1 failed file. ec3cf14 excludes `tests/**` from vitest (D-2 closed; KI-1 resolved). This is better than the baseline of 2 failed files.
- e2e: 4 failed -> 3 failed, 1 skipped (the `catalog-search-pagination.spec.js` sort test; its outcome varies between runs, see section 4).
- Backend (29 pass) and lint (6 errors, all KI-3) unchanged. Story API spec still 10/10 (included in the 10 passing e2e).

## 3. Coverage

**UNMEASURED.** `pom.xml` has no JaCoCo plugin and the decision for this Story was not to add it; `ecom-project/target/site/jacoco/` does not exist after `clean test`. No line or branch percentage can be stated.

### 3.1 Module-wide

| Metric | Covered | Missed | % |
|---|---|---|---|
| Line | n/a | n/a | UNMEASURED |
| Branch | n/a | n/a | UNMEASURED |

### 3.2 Classes touched by this story
Not measurable (no report).

### 3.3 The 85% gate
Cannot be shown as met. It is an open gate, neither passed nor failed on a number. Every new or changed class (`PagedResponse`, `ProductService`, the `ProductController` pagination path, `DataSeeder`) has direct tests, but that is a qualitative observation, not a coverage figure.

### 3.4 Uncovered code
Not determinable without a coverage report.

## 4. Baseline comparison

| | Baseline | Now | New failures |
|---|---|---|---|
| Backend | 1 test passing | 29 passing, 0 failing | 0 |
| Front end unit | 2 failed files (KI-1, KI-2) | 1 failed file (KI-2 only) | 0. Better than baseline (KI-1 resolved by ec3cf14) |
| Front end lint | 6 errors (KI-3) | 6 errors (identical files and lines) | 0 |
| E2E | not run at baseline | 10 pass (Story API spec), 3 fail + 1 skipped in `catalog-search-pagination.spec.js` | none attributable to this Story (see below) |

E2E failures (KI-4): the 3 failing tests are "loads initial products and shows pagination controls", "can search by keyword and resets to page 1 behavior" and "can paginate to next page and updates the list". The spec is from commit 091708d and `src/App.jsx` is unchanged by this Story (`git diff main -- ecom-front` touches only `.gitignore`, `vite.config.js` and the new spec).

**Verified, not merely inferred:** I created a temporary worktree of `main` (091708d), started its backend (bare-array `/api/products`), and ran the same spec with the same front end. Result on main: the same 3 tests fail and 1 passes (the sort test). On this branch: the same 3 fail and the sort test was skipped in the full run (4 failed in the first run). So the 3 failures reproduce without this Story's backend change and are pre-existing (KI-4). The worktree was removed afterwards. Caveats: the sort test's result differs between runs and between branches (pass / skip / fail), so its outcome is flaky and I do not attribute it either way; the e2e suite was only run once on each side for this spec.

Separately, the known limitation L-1 stands: `App.jsx` expects a bare array and will render nothing against the envelope. This does not change which tests fail today, because the cards/Prev/Next controls these tests need do not exist on main either.

## 5. Acceptance criterion verification

Gherkin: `docs/sdlc/EPMCDMETST-69858/EPMCDMETST-69858.feature`. Playwright API spec titles carry the `@AC-n` tag.

| AC | Scenario | Evidence | Result |
|---|---|---|---|
| AC-1 | First page of ten | `ProductPaginationTest.firstPage`, `ProductPaginationSeededIntegrationTest.seededDefaultsAndFirstPage`, spec `@AC-1` | PASS |
| AC-2 | Third page | `thirdPageIsPartial`, `seededThirdPageNonEmpty`, spec `@AC-2` | PASS |
| AC-3 | Negative page is rejected | `negativePageIs400`, spec `@AC-3` | PASS |
| AC-4 | Zero size is rejected | `invalidSizeIs400`, spec `@AC-4` | PASS |
| AC-5 | Defaults apply | `defaultsApply`, `responseShape`, spec `@AC-5` | PASS |
| AC-6 | Maximum page size is 100 | `sizeHundredIsOk`, `invalidSizeIs400`, spec `@AC-6` | PASS |
| AC-7 | Page past the end | `pastTheEndReturnsEmptyEnvelope`, `offsetOverflowReturnsEmptyEnvelope`, spec `@AC-7` | PASS |
| AC-8 | Non-numeric rejected | `nonNumericOrOutOfRangeIs400`, spec `@AC-8` | PASS |
| AC-9 | Ascending ids across pages | `walkingAllPagesYieldsAscendingGaplessIds`, `seededIdsAscendingAcrossPagesWithoutDuplicates`, spec `@AC-9` | PASS |
| AC-10 | Seeded dataset >= 25 | `DataSeederTest`, spec `@AC-10` | PASS |

AC coverage: 10/10. Edge scenarios (negative size, offset overflow, empty table, category endpoint unchanged) are covered by `ProductPaginationTest`.

## 6. Document quality check

| Document | Present | Errors |
|---|---|---|
| requirements.md | yes, non-empty | none |
| architecture.md | yes, non-empty | none |
| design-review.md | yes, non-empty | none |
| implementation-plan.md | yes, non-empty | none |
| implementation-notes.md | yes, non-empty | none (its note that no JaCoCo report was produced matches this report) |
| code-review.md | yes, non-empty | none |
| EPMCDMETST-69858.feature | yes | every AC-1..AC-10 has a tagged scenario |
| test-report.md | yes (this file) | none |

## 7. Defects

| Id | Severity | Description | Status |
|---|---|---|---|
| D-1 | High (gate) | Coverage unmeasurable: no JaCoCo plugin in `pom.xml`; the 85% gate cannot be shown as met | Open; exception decided by the user (not adding JaCoCo). Not a waiver of the gate: it stays unmeasured |
| D-2 | Low | New Playwright spec collected by vitest (`npm test` 3 failed files vs 2) | Closed: fixed in ec3cf14, `npm test` now 1 failed file (KI-2), better than baseline |
| KI-4 | Medium | `catalog-search-pagination.spec.js`: 3 tests fail (UI lacks Prev/Next etc.); reproduced on `main`. Recorded in docs/KNOWN-ISSUES.md | Known issue, carry to PR |
| L-1 | Known limitation | `App.jsx` expects a bare array and will break against the envelope | Follow-up front-end Story |

## 8. Verdict

**Not PASS: gates are not all met.**

| Gate | Status |
|---|---|
| Story backend tests (4) and Story API spec (10) | Met |
| Backend full suite (29/29), no new failures | Met |
| `npm test` at or better than baseline (2 failed files) | Met (1 failed file, KI-2) |
| `npm run lint` no more than 6 baseline errors | Met (6, all KI-3) |
| E2E: no new failures | Met (3 failures reproduced on `main`, KI-4) |
| AC coverage 10/10, documents complete | Met |
| Coverage >= 85% | UNMEASURED. Cannot be shown as met. Exception decided by the user (JaCoCo not added); no number is given |

Overall: functionally clean versus baseline, but the 85% gate is unmeasured, so a PASS is not issued. It is an open exception owned by the user.
