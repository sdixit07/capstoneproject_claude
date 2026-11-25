# Design Review: EPMCDMETST-67217

Reviewed: `architecture.md` (commit 7704730) against `requirements.md`, and against `ProductService`, `ProductServiceTest`, `ProductRepository`, `ProductController`.

## Findings
| ID | Severity | Area | Risk or gap | Recommendation |
|----|----------|------|-------------|----------------|
| F-1 | MINOR | Accuracy | Architecture says the Product package is `org.ecom.productcatalog` rather than `.model`. This is correct (the file is in `model/` but declares `package org.ecom.productcatalog`). The new exception goes under `...productcatalog.exception`, a new package that matches the `controller/service/repository` layout. No defect, but the folder/package mismatch is a trap for the implementer. | Keep as designed. State the import `org.ecom.productcatalog.Product` explicitly in the implementation notes. |
| F-2 | MINOR | Testing | Tests assert only the exception type and message via `assertThrows`. Nothing verifies delegation to `findById` or that the exception is a `RuntimeException`, so FR-2/FR-4 are only partly checked. | Add `verify(repo).findById(...)` to both tests. Optionally assert `RuntimeException.class.isAssignableFrom(ProductNotFoundException.class)`. |
| F-3 | MINOR | Design | The exception constructor takes only a `Long id`. It cannot carry a custom message, and a null id would give "...id: null". Requirements only need the fixed message. | Accept as is (simple, matches FR-3). Do not add more constructors (YAGNI). |
| F-4 | MINOR | Process / NFR-2 | The coverage threshold is inconsistent. `pipeline-config.md` and `pipeline-rules.md` say 85%, and `CLAUDE.md` says 75%. The architecture only says "the threshold" in NFR-2. | Treat 85% (`pipeline-config.md`, the authoritative source) as the target. Flag the CLAUDE.md discrepancy to the user. It is not an architecture change. |
| F-5 | MINOR | Risk | Until a handler exists, the exception would surface as HTTP 500. This is already documented and accepted, since no endpoint calls the method. | Accept. Make sure the follow-up Story for the controller and 404 mapping is recorded (Jira comment or the PR's Known Limitations). |
| F-6 | MINOR | Testing | The null-id behaviour (FR-5) has no test. It is explicitly unspecified, so this is consistent. A mocked repository would not throw `IllegalArgumentException` anyway, so a test would be meaningless. | Accept, no test. |

No BLOCKER or MAJOR findings. All FRs, NFRs and ACs are covered, and the design is backward compatible (no API or frontend change). It follows the existing field-injection and Mockito test style, and backend service placement is appropriate.

## Proposed design decisions
| ID | Decision | Resolves | Changes architecture.md? |
|----|----------|----------|--------------------------|
| D-1 | Keep the design as written: single-arg `ProductNotFoundException(Long id)`, `orElseThrow` in the service, no `@Transactional`, no handler. Accept / reject. | F-3, F-5, F-6 | no |
| D-2 | Strengthen the unit tests: add `verify(repo).findById(id)` in both tests, and assert the exception is unchecked. Accept / reject. | F-2 | yes (the Tests bullet gains the verify steps) |
| D-3 | Use 85% (`pipeline-config.md`) as the NFR-2 coverage target and note the CLAUDE.md "75%" inconsistency for separate cleanup. Accept / reject. | F-4 | no |
| D-4 | Record the import `org.ecom.productcatalog.Product` and the new `exception` package in the implementation notes, and record the follow-up controller/404 Story in the PR's Known Limitations. Accept / reject. | F-1, F-5 | no |

## Traceability
| Requirement | Design element | Covered |
|-------------|----------------|---------|
| FR-1 | `getProductById(Long)` signature | Yes |
| FR-2 | Delegation to `findById` | Yes (F-2 improves verification) |
| FR-3 | `orElseThrow` with message | Yes |
| FR-4 | `extends RuntimeException` | Yes |
| FR-5 | No explicit null handling | Yes |
| NFR-1 | Mockito-only tests | Yes |
| NFR-2 | Both branches tested | Yes (see F-4) |
| NFR-3 | No new dependencies | Yes |
| AC-1 / AC-2 | Two unit tests | Yes |

## Agreed design decisions
All four decisions were accepted by the user as proposed. Applied to `architecture.md` in commit `7f33e39`.

| Decision | Agreed | Applied in commit | Where applied |
|----------|--------|-------------------|---------------|
| D-1 | Agreed as proposed | 7f33e39 | No design change. Recorded in `architecture.md` > Changes after design review (Low-Level Design, Error Handling). |
| D-2 | Agreed as proposed | 7f33e39 | `architecture.md` > Low-Level Design > Tests: both tests `verify(repo).findById(id)`; AC-2 asserts the exception is unchecked. |
| D-3 | Agreed as proposed | 7f33e39 | No design change. NFR-2 target is 85%. Recorded in `architecture.md` > Changes after design review (NFR-2 traceability). |
| D-4 | Agreed as proposed | 7f33e39 | No design change. Carried forward to the implementation notes (import `org.ecom.productcatalog.Product`, new `exception` package) and to the PR's Known Limitations (follow-up controller/404 Story). |

### Correction to F-4
F-4 stated that `CLAUDE.md` says 75%. That was based on an uncommitted local edit, which is now stashed. The committed `CLAUDE.md` says 85%, which matches `pipeline-config.md` and `pipeline-rules.md`. There is no real inconsistency, so no cleanup is needed. D-3 stands as the 85% target, and its "note the discrepancy for cleanup" part is void.

## Verdict
APPROVED WITH CHANGES. Final: all changes agreed and applied in commit `7f33e39` (only D-2 touched `architecture.md`, and it was minor). No open BLOCKER or MAJOR findings.
