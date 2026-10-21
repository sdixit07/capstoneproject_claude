# Known Issues

Failures that exist on the baseline, before any Story work. Agents record them as `KI-n` and must not
raise them as new defects or "fix" them as part of a Story. Leave these tests and files alone unless a
Story explicitly covers them.

Measured on `chore/align-capstone-structure` (2026-10-21): backend `./mvnw test`, front end
`npm test` and `npm run lint`. The Playwright e2e suite (`npm run test:e2e`) was not run.

## Baseline summary

| Suite | Result |
|---|---|
| Backend `./mvnw test` | 1 test, 0 failures, BUILD SUCCESS |
| Front end `npm test` (vitest) | 2 failed suites, 0 tests run (KI-1, KI-2) |
| Front end `npm run lint` | 6 errors, 0 warnings (KI-3) |

## KI-1: vitest collects a Playwright spec

- **Where:** `ecom-front/ecom-catalog-react/tests/catalog-search-pagination.spec.js`
- **Symptom:** `npm test` fails the suite with "Playwright Test did not expect test.describe() to be called here."
- **Cause:** `package.json` runs plain `vitest run` and no config excludes `tests/`, so vitest loads the Playwright e2e spec.
- **Likely fix (outside any Story):** exclude `tests/**` from vitest.

## KI-2: `productsApi.test.js` imports a module that does not exist

- **Where:** `ecom-front/ecom-catalog-react/src/api/productsApi.test.js`
- **Symptom:** `npm test` fails with "Failed to load url ./productsApi. Does the file exist?"
- **Cause:** `src/api/` contains only the test file; there is no `productsApi.js`. The test also uses `vi` without importing it (see KI-3).
- **Likely fix:** a Story that adds the API module, or removing the orphan test.

## KI-3: ESLint errors

`npm run lint` reports 6 errors:

| File | Rule | Detail |
|---|---|---|
| `playwright.config.js` (lines 8, 11, 13) | `no-undef` | `process` is not defined (no Node globals configured) |
| `src/App.jsx` (lines 2, 3) | `no-unused-vars` | `reactLogo` and `viteLogo` imported but unused |
| `src/api/productsApi.test.js` (line 9) | `no-undef` | `vi` is not imported from `vitest` |

A Story must add no lint errors beyond these 6.
