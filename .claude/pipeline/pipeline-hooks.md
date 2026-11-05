# Pipeline Hooks

Only hooks that are actually configured are listed. They live in `.claude/settings.json`.

- `PreToolUse` (Bash): `.claude/hooks/block-main-commit.js` blocks commits on main/master and any `git push` that targets or originates from main/master. It does not see MCP tools, so `push_files` is gated by the `ask` list in `settings.json` instead.
- `PreToolUse` (Write|Edit|MultiEdit): `.claude/hooks/enforce-artifact-order.js` guards `docs/sdlc/<STORY-ID>/`. It blocks writing an artifact before the previous stage's artifact exists (requirements → architecture → design-review → implementation-plan → implementation-notes → code-review → test-report → pr-body-<STORY-ID>). It also blocks a full `Write` of `requirements.md`, `architecture.md`, `implementation-plan.md` or `test-report.md` that lacks the template's required sections. It checks that files exist, not that a stage was approved, and it does not see files written through Bash.
- `SessionStart` / `SessionEnd`: echo a status line only; they enforce nothing.

No git hooks (pre-commit, pre-push, post-merge) are installed and there is no Confluence sync.
