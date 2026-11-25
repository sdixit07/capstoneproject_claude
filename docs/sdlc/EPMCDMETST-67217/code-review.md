# Code Review: EPMCDMETST-67217

Branch `feature/EPMCDMETST-67217`, commits `master..HEAD` (10 commits: 5 docs before implementation, 3 code commits, 1 plan, 1 notes). Reviewer read all three changed source files in full and compared them against `requirements.md`, `architecture.md`, `design-review.md`, `implementation-plan.md` and `implementation-notes.md`.

Change size: 3 source files (+35 lines): new `ProductNotFoundException`, `ProductService.getProductById`, two tests in `ProductServiceTest`. Backend only.

## Context checked
- Callers: `getProductById` has no callers yet (controller is out of scope, FR/Out of Scope). `ProductController` is untouched.
- Duplication: no existing by-id helper in `ProductService`; `findById` is inherited from `JpaRepository`. No other exception classes exist to reuse. The in-browser filtering in `App.jsx` is unrelated and untouched.

## 7 review areas
| # | Area | Question | Result | Notes |
|---|------|----------|--------|-------|
| 1 | Correctness | Does the code meet FR-1..FR-5 and AC-1/AC-2? | ✅ | Signature `Product getProductById(Long)`; returns on present; throws `ProductNotFoundException("Product not found with id: <id>")` on empty; exception extends `RuntimeException`; no null handling (FR-5). Matches the architecture snippet exactly. |
| 2 | Security | Any new attack surface or data leak? | ✅ | No endpoint, no input parsing, no SQL. Message only echoes a `Long`. No secrets. |
| 3 | Error handling | Are failures signalled correctly? | ✅ | Unchecked dedicated exception, as designed. Known, accepted limitation: a future caller sees HTTP 500 until a 404 mapping exists (documented in notes, D-4/F-5). |
| 4 | Test coverage | Are both branches tested, without weakening existing tests? | ⚠️ | Both ACs tested with `verify(findById)` and unchecked check (D-2). 31 tests, 0 failures, no existing test changed. Line coverage 83.1% vs 85% target (baseline 82.7%): gap predates this change; Stage 7 owns the verdict. See F-1. |
| 5 | Code clarity | Is it readable and consistent with the codebase? | ✅ | Follows existing style (field injection, no space before `{`, matches neighbouring methods). New `exception` package fits the layer layout. |
| 6 | DRY | Any duplicated logic or missed reuse? | ✅ | Uses inherited `findById`; no duplicated logic. |
| 7 | Dependency safety | New or risky dependencies? | ✅ | None. See below. |

## Findings
| ID | Area | Severity | File:line | Issue | Recommendation |
|----|------|----------|-----------|-------|----------------|
| F-1 | Test coverage | MINOR | `ecom-project/src/test/java/org/ecom/productcatalog/service/ProductServiceTest.java:75` | `assertTrue(RuntimeException.class.isAssignableFrom(ProductNotFoundException.class))` is a compile-time-true check; it cannot fail unless the class hierarchy changes. It matches the agreed D-2, but adds little. | Optional: leave as is (agreed design). Could instead assert `assertInstanceOf(RuntimeException.class, ex)`. No action required. |
| F-2 | Code clarity | MINOR | `ecom-project/src/main/java/org/ecom/productcatalog/exception/ProductNotFoundException.java:5` | No `serialVersionUID`; compiler lint may warn for Serializable classes. | Optional; consistent with the YAGNI decision in D-1. |
| F-3 | Error handling | MINOR | `ecom-project/src/main/java/org/ecom/productcatalog/service/ProductService.java:34` | A null id yields Spring Data `IllegalArgumentException` (not wrapped). Specified as FR-5 and accepted in F-6 of the design review. | None; track in the follow-up controller Story. |

No BLOCKER. No MAJOR. 3 MINOR (all optional, none need a fix in this Story).

## Plan compliance
| Item | Status | Evidence |
|------|--------|----------|
| T-1 `ProductNotFoundException` | ✅ Done | commit 47f1cbb; single `Long` constructor, no `@ResponseStatus` |
| T-2 `getProductById` | ✅ Done | commit 422aef6; imports as planned |
| T-3 AC-1 test | ✅ Done | commit 2c8f9c7; `assertSame` + `verify` |
| T-4 AC-2 test | ✅ Done | commit 2c8f9c7; type, message, `verify`, unchecked |
| T-5 implementation notes | ✅ Done | commit 8396200; records the `org.ecom.productcatalog.Product` import and the `exception` package |
| T-6 build/coverage | ✅ Done (reported honestly) | `clean verify` 31 tests, 0 failures; line 83.1% (below 85%) |
| T-7 PR Known Limitations | Pending | Belongs to Step 8 (`pr-agent`); notes already record the follow-up Story |
| Scope creep | None | No controller, handler, repository, front-end or pom changes |
| Commit format | ✅ | All carry `(EPMCDMETST-67217)` with feat/test/docs types |
| Protected paths | ✅ | No changes under `.claude/` or `ecom-front/` |
| Known issues KI-1..KI-3 | ✅ Untouched | `ProductControllerIT` not modified |
| New failures vs baseline | 0 | Baseline 29 tests / 0 failures; after 31 / 0 |

Note: coverage 83.1% vs 85% pre-dates this change (82.7% baseline). It is not raised as a finding; Stage 7 decides the coverage verdict. The new code itself is fully covered (both branches of the method and the exception constructor).

## Dependency safety
- Frontend: `npm audit` not run. There is no front-end change (no `ecom-front/` files in the diff, no `package.json` change).
- Backend: `ecom-project/pom.xml` has no diff against master. No new dependencies and no explicit version overrides of BOM-managed artifacts (only the parent version is set). Spring Boot version: 3.4.4 (`spring-boot-starter-parent`). Declared dependencies: data-jpa, web, mysql-connector-j (runtime), h2 (runtime), starter-test (test); lombok in the compiler plugin.
- Limitation: an attempted `mvnw -o dependency:list` could not run because the dependency plugin is not in the offline local repository. No CVE scan of the existing tree was performed. Since nothing changed in the pom, this change introduces no new dependency risk.

## Verdict
**APPROVE**. No BLOCKER or MAJOR findings; 3 optional MINOR findings. Coverage gap (83.1% vs 85%) is pre-existing and deferred to Stage 7.
