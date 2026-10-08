# Pipeline Rules
1. All PRs require automated test verification.
2. Zero critical security vulnerabilities permitted.
3. PR descriptions must include Summary, Changes Made, Test Evidence, Known Limitations, and Reviewer Checklist.

## Process rules
4. One Jira Story (`EPMCDMETST-<n>`) per run; every artifact lives in `docs/sdlc/<STORY-ID>/` under its fixed filename.
5. Stages run strictly in order (1-8). A stage starts only after the previous artifact is committed and the user has approved it (human gate, run by the `/sdlc-run` command).
6. Work happens on `feature/<STORY-ID>` (or `chore/<topic>` for tooling). Never commit to `main`.
7. Commit messages are conventional and carry the Story id: `feat(EPMCDMETST-67216): ...`.
8. Code review (stage 6) must return a verdict with no open Blocker/Critical findings before testing (stage 7) starts.
9. Test coverage must be at least the threshold in `pipeline-config.md` (85%); `tester-agent` enforces it from the JaCoCo report, the build does not.
10. `pr-agent` opens the PR and never merges it; a human reviewer merges.
11. Every agent works only within the approved requirements. Scope changes go back to stage 1 via a Jira comment, not into code.

## Guardrails
- **Secrets**: never commit tokens, passwords or keys. `.mcp.json` may only reference `${VAR}` placeholders; real values go in `.claude/settings.local.json` or environment variables.
- **Destructive git**: no force-push, no `reset --hard` on shared branches, no history rewrites, no deleting remote branches.
- **Dependencies**: `node_modules/` and `target/` are never committed. New dependencies must be justified in `implementation-notes.md` and checked by `code-review` (`npm audit`, Maven dependency review).
- **Jira**: agents may read the Story, add comments and link the PR. They do not delete issues, change other Stories, or transition a Story without the user's say-so.
- **GitHub**: agents may create branches, push feature branches and open PRs. They never merge, approve their own PR, or push to `main`.
- **Read-only stages**: `code-review` and `design-review` never modify production code or `architecture.md`.
- **Local-only config**: `server.error.include-message=always` is a development setting; do not carry it into production profiles.
- **Honest reporting**: failing tests, skipped steps and coverage gaps are reported as they are in the artifact. Do not weaken or delete tests to pass a gate.
- **Human approval**: ask the user before any outward-facing or hard-to-reverse action not listed above.
