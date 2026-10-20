# Pipeline Hooks

Only hooks that are actually configured are listed. They live in `.claude/settings.json`.

- `PreToolUse` (Bash): `.claude/hooks/block-main-commit.js` blocks commits on main/master and any `git push` that targets or originates from main/master. It does not see MCP tools, so `push_files` is gated by the `ask` list in `settings.json` instead.
- `SessionStart` / `SessionEnd`: echo a status line only; they enforce nothing.

No git hooks (pre-commit, pre-push, post-merge) are installed and there is no Confluence sync.
