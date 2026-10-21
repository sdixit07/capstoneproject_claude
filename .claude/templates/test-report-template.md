# Test Report - <STORY-ID>

> Written by `tester-agent` (Step 7) to `docs/sdlc/<STORY-ID>/test-report.md`.

## 1. Environment

| Item | Value |
|---|---|
| Commit SHA | |
| Branch | `feature/<STORY-ID>` |
| Java / Maven | |
| Node / npm | |
| Database | H2 in-memory, seeded by `DataSeeder` |

## 2. Test execution results

Per suite, with the command used:

| Suite | Command | Passed | Failed | Skipped |
|---|---|---|---|---|
| Backend unit + integration | `./mvnw clean test` | | | |
| Front-end unit | `npm test` | | | |
| Front-end e2e | `npm run test:e2e` | | | |

## 3. Coverage

JaCoCo runs `prepare-agent` and `report` only — there is no `check` goal, so a passing build
does **not** imply the threshold was met. Report measured numbers from
`ecom-project/target/site/jacoco/`.

### 3.1 Module-wide

| Metric | Covered | Missed | % |
|---|---|---|---|
| Line | | | |
| Branch | | | |

### 3.2 Classes touched by this story

| Class | Line % | Branch % |
|---|---|---|

### 3.3 The 85% gate

State plainly whether the threshold is met, and for which scope (module-wide vs. touched classes).

### 3.4 Uncovered code

List the uncovered lines or branches worth a test, for the developer agent to act on.

## 4. Baseline comparison

Compare against the baseline recorded in `implementation-notes.md`.

| | Baseline | Now | New failures |
|---|---|---|---|
| Backend | | | |
| Front end | | | |

A pre-existing failure is a **known issue (KI)**, not a regression — give it a KI id and carry it
into the PR's Known Limitations. Any *new* failure blocks the PR.

## 5. Acceptance criterion verification

| AC | Scenario | Evidence | Result |
|---|---|---|---|

## 6. Document quality check

Artifacts of Steps 1-6 present, internally consistent, and free of dangling references.

| Document | Present | Errors |
|---|---|---|

## 7. Defects

| Id | Severity | Description | Status |
|---|---|---|---|

## 8. Verdict

**PASS** or **FAIL**, with the reason. A verdict of PASS requires: zero new failures versus
baseline, every AC verified, and zero document-check errors. If coverage misses the threshold,
say so explicitly — do not round up or describe it as "close".

## 9. Re-verification (addendum, if remediation followed)

Record the remediation commits, the re-run, the coverage after, and the revised verdict.
