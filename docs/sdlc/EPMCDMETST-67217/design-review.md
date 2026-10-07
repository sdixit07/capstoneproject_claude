# Design Review: EPMCDMETST-67217

Reviewed: `requirements.md`, `architecture.md` (commit 6d756f5), code under `ecom-project/`.
Verified facts: `Product.java` lives in `model/` but declares `package org.ecom.productcatalog;`; `ProductService` uses field injection; `pom.xml` (Spring Boot 3.4.4, Java 21) has `spring-boot-starter-test` and H2 but **no JaCoCo plugin**; only test files are `ProductControllerIT` and `EcomProjectApplicationTests`.

## Findings
| ID | Severity | Area | Risk / gap | Recommendation |
|----|----------|------|-----------|----------------|
| F-1 | MAJOR | NFR-4 / build | No JaCoCo (or other coverage tool) in `ecom-project/pom.xml`; the 85% gate in Step 7 cannot be measured, and the architecture (R5) assumes it can. | Add `jacoco-maven-plugin` (prepare-agent + report) to the pom, or explicitly agree how Step 7 measures coverage. |
| F-2 | MINOR | Exception design | `@ResponseStatus` on a `RuntimeException` yields a default Spring error body; acceptable, but the package name `exception` is new and the constructor takes `Long` only. Null id never reaches it, so fine. | Keep as designed; optionally add a `(String message)`-free single constructor only. No change needed. |
| F-3 | MINOR | Fit / R1 | `Product` package/directory mismatch. Architecture correctly defers; but tests must import `org.ecom.productcatalog.Product` (not `...model.Product`). | Keep decision; state the import explicitly in the implementation plan; log tech debt. |
| F-4 | MINOR | Testing | Null-id test (optional) stubs a mock to throw `IllegalArgumentException`, which only tests Mockito, not behaviour; low value. Also `@InjectMocks` on a public field-injected service is fine but ordering of Mockito strict stubs applies. | Make null-id test optional/omit; keep found, not-found (999999, message check), and zero/negative cases. |
| F-5 | MINOR | Testability | `ProductNotFoundException` and its `@ResponseStatus` mapping are not verified by any test (annotation could be dropped silently). | Add a small assertion that the exception class carries `@ResponseStatus(NOT_FOUND)` and message contains the id. |
| F-6 | MINOR | Scope | Requirements allow `@ResponseStatus` or `@RestControllerAdvice`; architecture chooses `@ResponseStatus` with rationale. Consistent, no gap. | None. |

No BLOCKERs. Requirement coverage is complete (see traceability).

## Proposed design decisions
| ID | Decision | Resolves | Changes architecture.md? |
|----|----------|----------|--------------------------|
| D-1 | Add `jacoco-maven-plugin` (prepare-agent, report) to `ecom-project/pom.xml` during implementation so the 85% gate is measurable; amend R5 accordingly. | F-1 | yes |
| D-2 | Tests import `org.ecom.productcatalog.Product`; no move/rename; record package mismatch as tech debt for a separate story. | F-3 | no (already stated; plan should repeat) |
| D-3 | Drop the mock-based null-id test; keep found, not-found with message check, and zero/negative id tests. | F-4 | yes (minor, section 6) |
| D-4 | Add a test asserting `ProductNotFoundException` is annotated `@ResponseStatus(NOT_FOUND)` and message contains the id. | F-5 | yes (minor, section 6) |

## Traceability
| Requirement | Design element | Covered |
|---|---|---|
| FR-1, AC1, 67219 | `getProductById` via `findById` | Yes |
| FR-2 | orElseThrow returns Product | Yes |
| FR-3, FR-4, AC2 | `ProductNotFoundException` + `@ResponseStatus` | Yes (D-4 adds verification) |
| FR-5, FR-6 | No guard; normal path | Yes |
| FR-7, 67220 | `ProductServiceTest` | Yes |
| NFR-1..3 | No API/schema change; package layout; mocked tests | Yes |
| NFR-4 | R5 | Partial until D-1 |

## Verdict
APPROVED WITH CHANGES (pending user agreement on D-1 to D-4).
