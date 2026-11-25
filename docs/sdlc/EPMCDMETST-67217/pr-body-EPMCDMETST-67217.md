## Summary

EPMCDMETST-67217 adds a service-layer lookup of a product by id. `ProductService.getProductById(Long)` returns the product, or throws the new unchecked `ProductNotFoundException` ("Product not found with id: <id>") when none exists. No endpoint is exposed and no front-end code changed.

Note: the coverage gate was NOT met (83.08% vs 85%). It was accepted by explicit user waiver; see Known Limitations. This PR does not fully pass the gate.

## Changes Made

- New `ProductNotFoundException` (unchecked) in `org.ecom.productcatalog.exception`.
- New `ProductService.getProductById(Long)` using the inherited `findById`.
- `ProductServiceTest`: unit tests for the found and not-found cases.
- `ProductServiceIntegrationTest`: AC-1 and AC-2 against seeded H2.
- README section and CHANGELOG entries (Unreleased / Added).
- SDLC artifacts in `docs/sdlc/EPMCDMETST-67217/` (requirements through test report).
- No pom, controller, repository or front-end changes; no new dependency.

## Test Evidence

From `docs/sdlc/EPMCDMETST-67217/test-report.md` (`mvnw.cmd clean verify`):

- 33 tests, 0 failures, 0 skipped (baseline: 29 tests, 0 failures). 0 new failures vs baseline.
- AC-1 (existing id returns the product): PASS.
- AC-2 (id 999999 throws `ProductNotFoundException`): PASS.
- Coverage (JaCoCo): line 83.08% (108 of 130), instruction 93.8%, branch 100%.
- Touched classes: `ProductService` 100% (6/6), `ProductNotFoundException` 100% (2/2).
- Front-end suites not run (no front-end change). No Gherkin feature file was generated (service-level Story).
- Code review verdict: APPROVE (0 Blocker, 0 Major, 3 Minor optional).

## Known Limitations

- (a) **Coverage gate NOT met.** Line coverage is 83.08% against the 85% target. The user explicitly accepted this by waiver on 2026-11-25. The shortfall predates this Story (82.7% at baseline). The code this Story touched is at 100%. All 22 missed lines are in `Product` (10), `Category` (10) and `EcomProjectApplication` (2). No tests were added, weakened or deleted to reach the number.
- (b) A follow-up Story is needed for the controller endpoint and the 404 mapping. Until then the exception surfaces as HTTP 500 if called from a controller.
- (c) A null id relies on Spring Data's `IllegalArgumentException` (not wrapped).
- (d) `ProductControllerIT` is skipped by Surefire; its known issues are out of scope.
- (e) The existing dependency tree received no CVE scan because the offline Maven dependency plugin was unavailable. This change adds no dependency (no pom diff).
- No Gherkin feature file (informational defect D-2).

## Reviewer Checklist

- [ ] `getProductById` matches FR-1..FR-5 and `architecture.md`
- [ ] Exception message format is "Product not found with id: <id>"
- [ ] AC-1 and AC-2 tests are meaningful and pass locally (`./mvnw clean verify` in `ecom-project/`)
- [ ] Coverage waiver (83.08% < 85%) is acceptable, or a coverage follow-up is filed
- [ ] Follow-up Story for controller endpoint and 404 mapping is filed
- [ ] No secrets, `target/` or `node_modules/` committed
- [ ] README and CHANGELOG entries are accurate

🤖 Generated with [Claude Code](https://claude.com/claude-code)
