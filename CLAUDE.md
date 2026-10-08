# CLAUDE.md

Project memory for Claude Code. Read this before making changes.

## What this project is

An e-commerce **product catalog** built as a Spring Boot REST API with a React front end.
It is the application under change for a governed, agent-driven SDLC pipeline: every change
enters through a Jira Story in the `EPMCDMETST` project and leaves through a GitHub pull request,
with a written artifact committed at each of eight stages.

## Repository layout

```
ecom-project/                     Spring Boot backend (Maven, module name "ecom-project")
  src/main/java/org/ecom/productcatalog/
    EcomProjectApplication.java   entry point
    config/DataSeeder.java        seeds the in-memory H2 database at startup
    controller/                   ProductController, CategoryController
    service/                      ProductService, CategoryService
    repository/                   Spring Data JPA repositories
    model/                        Product, Category entities
  src/main/resources/application.properties
  src/test/java/...               JUnit 5 + Spring Boot Test

ecom-front/
  ecom-catalog-react/             the actual front end (React 19 + Vite 6)
    src/                          App.jsx, ProductList.jsx, CategoryFilter.jsx,
                                  pages/ProductDetail.jsx, api/
    tests/                        Playwright end-to-end specs
  package.json                    vestigial manifest (bootstrap/react-bootstrap only, no scripts)

docs/sdlc/<STORY-ID>/             the eight SDLC artifacts for each Story
images/                           README screenshots
.claude/                          agents, skills, commands, templates, pipeline rules
```

The front end you run and edit is **`ecom-front/ecom-catalog-react`**. The outer
`ecom-front/package.json` declares two dependencies and no scripts; do not add code against it.

## Commands

Backend (from `ecom-project/`, Java 21 required):

```bash
./mvnw spring-boot:run      # start the API on :8080 (mvnw.cmd on Windows)
./mvnw test                 # full test suite
./mvnw clean verify         # build + test
```

Front end (from `ecom-front/ecom-catalog-react/`, Node 18+):

```bash
npm install                 # required: node_modules is not committed
npm run dev                 # Vite dev server
npm test                    # vitest, single run
npm run test:e2e            # Playwright
npm run lint                # eslint
npm run build               # production build
```

No Docker is needed. The backend uses an in-memory **H2** database seeded by `DataSeeder` at
startup; the MySQL connector is on the classpath but unused in the default profile.

## API

| Method | Path | Returns |
|---|---|---|
| GET | `/api/products` | all products |
| GET | `/api/products/{id}` | one product with its category; `404` if no such id, `400` if the id is not a number |
| GET | `/api/products/category/{categoryId}` | products in a category |
| GET | `/api/categories` | all categories |

`server.error.include-message=always` is set in `application.properties`, so error bodies carry a
`message` field. That is a **local-development setting** — do not rely on it in production.

## Test coverage

JaCoCo is wired for `prepare-agent` and `report` only — there is **no `check` goal and no
`<limit>`**, so `./mvnw test` will *not* fail on low coverage. The 85% threshold in
`.claude/pipeline/pipeline-config.md` is enforced by the **tester agent** reading the generated
report at `ecom-project/target/site/jacoco/`. Do not describe the gate as build-enforced.

## The SDLC pipeline

`orchestrator-agent` coordinates eight stages; each writes one artifact into
`docs/sdlc/<STORY-ID>/` and commits it before the next stage starts.

| Step | Agent | Artifact |
|---|---|---|
| 1 | `requirements-agent` | `requirements.md` |
| 2 | `architecture` | `architecture.md` |
| 3 | `design-review` | `design-review.md` |
| 4 | `planner-subagent` | `implementation-plan.md` |
| 5 | `developer-agent` | `implementation-notes.md` |
| 6 | `code-review` | `code-review.md` |
| 7 | `tester-agent` | `test-report.md` |
| 8 | `pr-agent` | `pr-body-<STORY-ID>.md` + the GitHub PR |

Templates for these documents live in `.claude/templates/`. Agents reference them by path.

## Conventions

- **Never commit directly to `main`.** Work on `feature/<STORY-ID>`, e.g.
  `feature/EPMCDMETST-67216`. Structure/tooling work goes on a `chore/<topic>` branch.
- Commit messages are conventional and carry the Story id:
  `feat(EPMCDMETST-67216): add product detail endpoint`.
- A PR body must contain all five sections: Summary, Changes Made, Test Evidence,
  Known Limitations, Reviewer Checklist.
- `pr-agent` opens the PR and never merges it.
- Keep the artifact filenames in the table above exactly as written — four agents cross-reference
  them by name.

## Dependencies and secrets

- **`node_modules/` is never committed.** It was purged from history; `.gitignore` covers it.
  Run `npm install` after cloning.
- **Never commit credentials.** Tokens belong in `.claude/settings.local.json` (git-ignored) or
  real environment variables. `.mcp.json` must only ever reference them as `${VAR}` placeholders.
- `.claude/settings.json` is the shared, secret-free config and is committed.
