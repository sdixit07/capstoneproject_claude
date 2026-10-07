# Implementation Plan - EPMCDMETST-67216

**Story:** View product details - `GET /api/products/{id}`
**Branch:** feature/EPMCDMETST-67216 | **Inputs:** requirements.md, architecture.md (section 13 applied), design-review.md (D-1..D-5 accepted, D-6 rejected)
**Sub-tasks:** EPMCDMETST-67221 (implementation), EPMCDMETST-67222 (verification) - mapping is an approved assumption
**Backend root:** `ecom-project/` (Spring Boot 3.4.4, Maven wrapper `mvnw`). Base package `org.ecom.productcatalog`. No frontend change.
**Note:** Plan template file not present in repo (Not Found); standard structure used. D-5 finding: `Product` has no `@Column(nullable=false)` on name/description/price (only `category` is non-null), so FR-3 holds only for populated data; fixtures must set all fields. No entity change (out of scope).

## 1. Tasks (ordered by dependency, then priority)
| ID | Pri | Jira | Description | Files | Depends on | Traces to |
|----|-----|------|-------------|-------|-----------|-----------|
| T-1 | P1 | 67222 | Baseline test run before any change: `cd ecom-project; ./mvnw test`; record pass/fail counts and pre-existing failures for implementation-notes.md | (run only) | - | NFR-4, FR-7 |
| T-2 | P1 | 67221 | Service: add `getProductById(Long id)` = `productRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with id " + id))`; imports `org.springframework.web.server.ResponseStatusException`, `org.springframework.http.HttpStatus`; keep field-injection style; read-only | `ecom-project/src/main/java/org/ecom/productcatalog/service/ProductService.java` | T-1 | FR-1..FR-4, FR-6, NFR-2, NFR-3, AC-1, AC-2 |
| T-3 | P1 | 67221 | Controller: add `@GetMapping("{id}") public Product getProductById(@PathVariable Long id)` delegating to service; plain `Long`, no regex, no `@Positive`; existing `""` and `category/{categoryId}` mappings and class-level `@CrossOrigin` untouched | `ecom-project/src/main/java/org/ecom/productcatalog/controller/ProductController.java` | T-2 | FR-1, FR-2, FR-3, FR-5, FR-7, FR-8, AC-1, AC-3 |
| T-4 | P1 | 67221 | Config (D-1): add only `server.error.include-message=always`; do NOT enable `include-stacktrace` / `include-binding-errors` | `ecom-project/src/main/resources/application.properties` | T-1 | FR-4, FR-5 (message key), NFR-4 |
| T-5 | P1 | 67221 | Compile check: `./mvnw -q compile` | (none) | T-2, T-3, T-4 | NFR-3 |
| T-6 | P1 | 67222 | MockMvc tests in `ProductControllerIT` (D-5: every fixture product has name, description, price, category; imageUrl set on the asserted product): (a) 200 for existing id asserting `$.id == requested`, `$.name`, `$.description`, `$.price`, `$.category.id`, `$.category.name`, JSON content type (AC-1); (b) 404 for `999999` (AC-2); (c) 404 for `0` and `-1` (FR-6); (d) 400 for `abc` (AC-3); (e) 400 for `99999999999999999999` (FR-5, F-4). For 404/400 assert status plus `resolvedException` type (`ResponseStatusException` / `MethodArgumentTypeMismatchException`) and reason, NOT the body (MockMvc has no error dispatch) | `ecom-project/src/test/java/org/ecom/productcatalog/controller/ProductControllerIT.java` | T-3, T-4 | AC-1..AC-3, FR-1..FR-6, NFR-4, D-2, D-5 |
| T-7 | P1 | 67222 | D-3 / FR-7 regression tests (MockMvc): `GET /api/products/category` -> 400 (documented behaviour change); `GET /api/products/category/{catId}` still returns the category list; `GET /api/products` unchanged; existing tests keep passing | `ecom-project/src/test/java/org/ecom/productcatalog/controller/ProductControllerIT.java` | T-3 | FR-7, D-3, AC-4 |
| T-8 | P1 | 67222 | D-2 body-shape tests: new class `ProductByIdErrorBodyIT` with `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`; seeds its own data (D-5 fixtures, reset in `@BeforeEach`, no reliance on `DataSeeder` counts). One test per error case: 404 (`/api/products/999999`) and 400 (`/api/products/abc`), each asserting status and JSON keys `timestamp`, `status`, `error`, `message`, `path`; `status` 404/400, `error` "Not Found"/"Bad Request", `message` non-empty (404 message contains "Product not found with id 999999"), `path` equals request path | `ecom-project/src/test/java/org/ecom/productcatalog/controller/ProductByIdErrorBodyIT.java` (new) | T-3, T-4 | NFR-4, FR-4, FR-5, D-1, D-2, AC-2, AC-3 |
| T-9 | P2 | 67222 | Service unit test (Mockito, no Spring context): returns product when repository returns present Optional; throws `ResponseStatusException` with 404 and reason "Product not found with id N" when empty; repository called once with the id | `ecom-project/src/test/java/org/ecom/productcatalog/service/ProductServiceTest.java` (new) | T-2 | FR-2, FR-4, FR-6, NFR-4 |
| T-10 | P2 | 67222 | CORS check: MockMvc request with `Origin: http://localhost:5173` to `/api/products/{id}` returns `Access-Control-Allow-Origin: http://localhost:5173` | `ecom-project/src/test/java/org/ecom/productcatalog/controller/ProductControllerIT.java` | T-6 | FR-8, AC-4 |
| T-11 | P1 | 67222 | Full test run after changes: `./mvnw test`; all new and existing tests green; compare with T-1 baseline; no regressions | (none) | T-6, T-7, T-8, T-9, T-10 | NFR-4, FR-7 |
| T-12 | P1 | 67222 | Build check: `./mvnw -DskipTests package` succeeds and produces the jar | (none) | T-11 | NFR-3 |
| T-13 | P1 | 67222 | App-start and live check: start app (port 8080, H2 in-memory); curl `/api/products/{existing id}` -> 200 with id/name/description/price/category; `/api/products/999999` -> 404 JSON with `message`; `/api/products/abc` -> 400 JSON; `/api/products/category/{id}` -> list; stop the app | (none) | T-12 | AC-1..AC-3, FR-7 |
| T-14 | P2 | 67222 | Frontend contract check (AC-4, read only): confirm `ProductDetail.jsx` fetches `http://localhost:8080/api/products/${id}`, 404 -> not found, other non-OK -> error, and uses fields present in the 200 body | `ecom-front/ecom-catalog-react/src/pages/ProductDetail.jsx` (read only) | T-13 | NFR-1, AC-4 |
| T-15 | P3 | 67222 | Write implementation-notes.md (baseline vs final results, test counts, commands, D-5 nullability confirmation, deviations); commit in logical steps (config; service+controller; tests); push per Step 5 | `docs/sdlc/EPMCDMETST-67216/implementation-notes.md` | T-14 | Process |

Note: T-2 and T-4 are independent of each other (both need only T-1). Tests may be written test-first, but T-6..T-10 can only pass after their dependencies exist.

## 2. Blocked tasks
| Task | Blocked until | Why |
|------|---------------|-----|
| T-2, T-4 | T-1 | Baseline must be recorded on unchanged code |
| T-3 | T-2 | Controller calls `ProductService.getProductById` (no compile otherwise) |
| T-5 | T-2, T-3, T-4 | Needs all production changes |
| T-6, T-8 | T-3, T-4 | Need the endpoint; message assertions need `include-message=always` |
| T-7 | T-3 | Needs the new route |
| T-9 | T-2 | Tests the service method |
| T-10 | T-6 | Reuses T-6 fixtures |
| T-11 | T-6..T-10 | Full run after all tests exist |
| T-12 | T-11 | Package only after green tests |
| T-13 | T-12 | Runs built artefact |
| T-14 | T-13 | Uses running backend for live check |
| T-15 | T-14 | Summarises all results |

Blocked task count: 14 (T-2..T-15). Only T-1 is unblocked.

## 3. Dependency graph and critical path
```
T-1 -> T-2 -> T-3 -> T-6 -> T-10 -> T-11 -> T-12 -> T-13 -> T-14 -> T-15
T-1 -> T-4 -> T-6, T-8      T-3 -> T-7, T-8      T-2 -> T-9      T-2,T-3,T-4 -> T-5
T-7, T-8, T-9 -> T-11
```
Acyclic: every edge points to a higher task number. **Critical path:** T-1 -> T-2 -> T-3 -> T-6 -> T-10 -> T-11 -> T-12 -> T-13 -> T-14 -> T-15.

## 4. Priority counts
P1: 11 (T-1..T-8, T-11, T-12, T-13) | P2: 3 (T-9, T-10, T-14) | P3: 1 (T-15) | Total 15.

## 5. Test tasks for Step 5
T-6, T-7, T-8, T-9, T-10. Rules: fixtures fully populated (D-5); MockMvc asserts status + `resolvedException`/reason only (D-2); body keys only in the `RANDOM_PORT` test (D-2); `/category` -> 400 documented (D-3); H2 in-memory, no DB mocking in IT tests.

## 6. Verification outline for Step 7 (EPMCDMETST-67222)
1. `./mvnw test` green in `ecom-project`; coverage check against the 85% threshold if tooling is configured.
2. AC-1: 200 + id/name/description/price (T-6a, T-13). AC-2: 999999 -> 404 (T-6b, T-8, T-13). AC-3: `abc` -> 400 (T-6d, T-8, T-13). AC-4: ProductDetail.jsx contract + CORS (T-10, T-14).
3. Edge cases: 0 and -1 -> 404; out-of-range -> 400; `/category` -> 400; `/category/{id}` unchanged.
4. Error body keys present including `message` (D-1, D-2).
5. Build and app start (T-12, T-13). No frontend change, so frontend tests are unaffected (optionally run the existing suite for no regression).

## 7. Out of scope / guardrails
No `@ControllerAdvice`, DTO, regex path constraint, `@Positive`, explicit `category` handler (D-6 rejected), entity/schema change, CORS change, frontend change, new dependency. Do not touch untracked `.claude/`, `.codemie/`, `.mcp.json`, `.vscode/`, `docs/codemie/`.

## 8. Definition of Done
- [ ] Baseline recorded (T-1); all tests green after (T-11), no regression vs baseline
- [ ] `GET /api/products/{id}` returns 200/404/400 per architecture sections 8-9
- [ ] `server.error.include-message=always` set; no other error properties
- [ ] Tests T-6..T-10 present, including D-2 body-shape, D-3 `/category`, D-5 populated fixtures
- [ ] Build succeeds, app starts, live curl checks pass (T-12, T-13)
- [ ] ProductDetail.jsx contract confirmed (T-14); CORS unchanged
- [ ] Layering and style consistent (NFR-3); read-only (NFR-2)
- [ ] implementation-notes.md written; logical commits; pushed (Step 5)
