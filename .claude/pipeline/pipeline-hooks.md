# Pipeline Hooks
- `pre-commit`: Linting and syntax check.
- `pre-push`: Run unit test suite.
- `post-merge`: Confluence documentation sync.

## Claude Code hooks (enforced in `.claude/settings.json`)
- `PreToolUse` (Bash): `.claude/hooks/block-main-commit.js` blocks commits on main/master and any push that targets or originates from main/master.
