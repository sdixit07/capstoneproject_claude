# Design Review - EPMCDMETST-67216

**Mode:** review | **Branch:** feature/EPMCDMETST-67216 | **Reviewed:** architecture.md vs requirements.md, plus `ProductController`, `application.properties`, `pom.xml` (Spring Boot 3.4.4), `ProductControllerIT`.

## Findings
| ID | Severity | Area | Risk or gap | Recommendation |
|----|----------|------|-------------|----------------|
| F-1 | MAJOR | FR-4 / error handling | FR-4 requires default error JSON incl. `message`. `application.properties` has no `server.error.*`; Boot 3.4.4 default is `include-message=never`, so `message` will be empty/absent for the 404 and 400 bodies. Architecture section 9 leaves this to the developer ("set it or accept") - the requirement and design are ambiguous. | Decide explicitly now: set `server.error.include-message=always` (see D-1) or amend FR-4 to drop `message`. |
| F-2 | MAJOR | Testing (NFR-4) | `ProductControllerIT` uses MockMvc. MockMvc does not perform the container error dispatch to `BasicErrorController`, so the default error JSON body (timestamp, message, path) is NOT present; only status (and `resolvedException`) can be asserted. The design says tests cover the "error JSON shape", which MockMvc cannot verify. | Assert status + `resolvedException` type/reason in MockMvc; verify body shape with one `@SpringBootTest(webEnvironment=RANDOM_PORT)` + `TestRestTemplate` test (D-2). |
| F-3 | MINOR | Route edge case | `GET /api/products/category` (no id) now matches `{id}` -> 400 (type mismatch) instead of 404. Architecture accepts this but the message for `category` would read as a conversion error. Behaviour change on an existing URL (FR-7 says existing behaviour kept). | Accept and add a test documenting 400; alternatively constrain with regex `{id:\d+}`-style, which would break FR-5/FR-6 (negatives, out-of-range -> must stay 400/404). Do not use regex (D-3). |
| F-4 | MINOR | FR-5 correctness | Architecture claims out-of-Long-range -> 400 via type mismatch. Correct for Spring 6 (`NumberFormatException` wrapped in `MethodArgumentTypeMismatchException`), but unverified in this repo. `-1` is a path segment that binds fine. | Include out-of-range test (already planned); no design change. |
| F-5 | MINOR | Error semantics | `ResponseStatusException` thrown in service couples service layer to web (`org.springframework.web.server`). Existing layering has no precedent. | Accept for minimal scope (matches architecture decision); note as tech debt (D-4). |
| F-6 | MINOR | Data / contract | Product `price` is `double` and `category` EAGER; serialisation incl. category confirmed OK (`Category.products` `@JsonIgnore` per architecture). `imageUrl` may be null; FR-3 only requires id/name/description/price non-null - `description` nullability is not guaranteed by the entity. | Check `@Column(nullable=false)` on name/description/price or ensure test data sets them; no code change otherwise (D-5). |
| F-7 | MINOR | Documentation | Architecture risk table says "Future lazy-loading could break serialisation" and sub-task mapping is assumed. Informational. | None. |

No BLOCKERs.

## Proposed design decisions
| ID | Decision | Resolves | Severity | Recommended default | Changes architecture.md? |
|----|----------|----------|----------|---------------------|--------------------------|
| D-1 | Add `server.error.include-message=always` to `application.properties` so `message` ("Product not found with id N") is present in 404/400 bodies, fulfilling FR-4 literally. Only this property; do not enable `include-stacktrace`/`include-binding-errors`. Note: it exposes exception messages app-wide (acceptable for this local demo app; for 400 it exposes the conversion message). Alternative: `include-message=on_param` is not suitable for the frontend. | F-1 | MAJOR | ACCEPT | yes (section 9, 6, 11) |
| D-2 | Test strategy: MockMvc tests assert status and `resolvedException`/reason only; add one `RANDOM_PORT` + `TestRestTemplate` test per error case (404, 400) asserting the default JSON keys (`status`, `error`, `message`, `path`). | F-2, F-4 | MAJOR | ACCEPT | yes (section 3 Tests row, NFR-4 note) |
| D-3 | Keep `@GetMapping("{id}")` with plain `Long`; no regex constraint, no `@Positive`. Document and test that `/api/products/category` returns 400 and that `/api/products/category/{id}` is unaffected. | F-3 | MINOR | ACCEPT | yes (section 7: add explicit test requirement and note on message) |
| D-4 | Keep `ResponseStatusException` in the service (no `@ControllerAdvice`), recorded as accepted tech debt. | F-5 | MINOR | ACCEPT | no |
| D-5 | Developer confirms Product field nullability (name, description, price) and uses fully populated fixtures in the 200 test asserting `id`, `name`, `description`, `price`. | F-6 | MINOR | ACCEPT | no |
| D-6 | (Optional) Treat `/api/products/category` as 404 via an explicit `@GetMapping("category")` handler throwing 404 to preserve prior behaviour. Adds code beyond scope. | F-3 | MINOR | REJECT (scope creep; 400 is acceptable) | yes if accepted |

## Traceability
| Req | Design coverage | Review result |
|-----|-----------------|---------------|
| FR-1 | `@GetMapping("{id}")`, `Long` | Covered |
| FR-2, FR-3 | Return `Product` | Covered (D-5) |
| FR-4 | `ResponseStatusException(404)` | Partially - `message` gap (F-1, D-1) |
| FR-5 | Type-mismatch 400 | Covered (F-4) |
| FR-6 | No `@Positive` | Covered |
| FR-7 | Disjoint routes; regression test | Covered with noted `/category` change (F-3, D-3) |
| FR-8 | Class-level `@CrossOrigin` | Covered |
| NFR-1..3 | Layering, no frontend change | Covered |
| NFR-4 | `ProductControllerIT` additions | Gap on body assertions (F-2, D-2) |
| AC-1..AC-4 | 67221 / 67222 | Covered subject to D-1, D-2 |

## Agreed design decisions
| Decision ID | Outcome | Applied in |
|-------------|---------|------------|
| D-1 | Agreed as proposed (ACCEPTED) | architecture.md commit `442e867` |
| D-2 | Agreed as proposed (ACCEPTED) | architecture.md commit `442e867` |
| D-3 | Agreed as proposed (ACCEPTED) | architecture.md commit `442e867` |
| D-4 | Agreed as proposed (ACCEPTED); no architecture change | n/a |
| D-5 | Agreed as proposed (ACCEPTED); no architecture change, developer confirms nullability and fixtures | n/a |
| D-6 | REJECTED by the user (scope creep; `/api/products/category` returns 400) | n/a |

## Verdict
**APPROVED** - D-1..D-5 accepted (D-1..D-3 applied in architecture.md commit `442e867`), D-6 rejected. No open BLOCKER or MAJOR findings.
