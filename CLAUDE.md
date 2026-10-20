# CLAUDE.md

E-commerce product catalog: Spring Boot REST API + React front end. Every change enters as a Jira
Story in `EPMCDMETST` and leaves as a GitHub PR, via the governed SDLC pipeline (`/sdlc-run <STORY-ID>`).

## Layout

- `ecom-project/` — Spring Boot backend (Maven, Java 21). In-memory H2 seeded by `DataSeeder`; the MySQL connector is on the classpath but unused.
- `ecom-front/ecom-catalog-react/` — **the real front end** (React 19 + Vite 6, Playwright e2e in `tests/`). The outer `ecom-front/package.json` is vestigial: no scripts, do not add code against it.
- `docs/sdlc/<STORY-ID>/` — the eight SDLC artifacts per Story.
- `.claude/` — agents, skills, commands, templates, and pipeline rules.

## Commands

Backend (from `ecom-project/`; use `mvnw.cmd` on Windows):

```bash
./mvnw spring-boot:run      # API on :8080
./mvnw clean verify         # build + test
```

Front end (from `ecom-front/ecom-catalog-react/`, Node 18+; run `npm install` first, `node_modules` is not committed):

```bash
npm run dev | test | test:e2e | lint | build
```

## API

`GET /api/products`, `/api/products/{id}` (404 unknown id, 400 non-numeric id),
`/api/products/category/{categoryId}`, `/api/categories`.

`server.error.include-message=always` is a local-dev setting; do not rely on it in production.

## Test coverage

JaCoCo only runs `prepare-agent` and `report` — no `check` goal, so `./mvnw test` never fails on low
coverage. The 85% threshold (`.claude/pipeline/pipeline-config.md`) is enforced by the **tester agent**
reading `ecom-project/target/site/jacoco/`. Do not describe it as build-enforced.

## Pipeline

Stage order, agents, and artifact names are defined in `.claude/commands/sdlc-run.md`; templates are in
`.claude/templates/`. Keep artifact filenames unchanged — agents cross-reference them by name.
Full guardrails: `.claude/pipeline/pipeline-rules.md`; enforceable part: `permissions` in `.claude/settings.json`.

## Conventions

- Never commit to `main`. Use `feature/<STORY-ID>` (e.g. `feature/EPMCDMETST-67216`), or `chore/<topic>` for tooling work.
- Conventional commits carrying the Story id: `feat(EPMCDMETST-67216): add product detail endpoint`.
- PR body needs five sections: Summary, Changes Made, Test Evidence, Known Limitations, Reviewer Checklist.
- Never merge a PR, force-push, hard-reset, or push to `main`. `pr-agent` opens the PR only.
- Never weaken or delete tests to pass the coverage gate; report failures as they are.
- Stay within approved requirements; scope changes go back to stage 1.

## Secrets

- Never commit credentials. Use `.claude/settings.local.json` (git-ignored) or environment variables; `.mcp.json` may only hold `${VAR}` placeholders.
- `.claude/settings.json` is shared and secret-free.
