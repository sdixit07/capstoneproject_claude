---
description: Run both test suites and report coverage against the 85% threshold
argument-hint: "<STORY-ID> (optional)"
---

Verify the current branch: run the tests, then report coverage honestly.

1. Backend — from `ecom-project/`:
   - `./mvnw clean test` (use `mvnw.cmd` on Windows)
   - read the JaCoCo report under `ecom-project/target/site/jacoco/`
2. Front end — from `ecom-front/ecom-catalog-react/`:
   - `npm install` if `node_modules/` is absent (it is never committed)
   - `npm test`
   - `npm run lint`

Then report:

- totals per suite: passed / failed / skipped
- line and branch coverage for the classes this change touched, and module-wide
- whether coverage clears **85%**

Remember that the threshold is **not** enforced by Maven — JaCoCo runs `prepare-agent` and
`report` only, with no `check` goal. A green `mvnw test` therefore says nothing about coverage.
State the measured number; never infer it from a passing build.

If a Story id was given as `$1`, compare against the baseline recorded in
`docs/sdlc/$1/implementation-notes.md` and call out any **new** failure versus that baseline.

Report failures with their output. Do not describe the run as clean if anything failed.
