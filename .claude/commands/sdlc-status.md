---
description: Show which SDLC artifacts exist for a Story and which stage is next
argument-hint: "<STORY-ID>, e.g. EPMCDMETST-67216"
---

Report the SDLC progress for Story **$1** without changing anything.

List `docs/sdlc/$1/` and check it against the eight expected artifacts, in order:

| Step | Agent | Artifact |
|---|---|---|
| 1 | `requirements-agent` | `requirements.md` |
| 2 | `architecture` | `architecture.md` |
| 3 | `design-review` | `design-review.md` |
| 4 | `planner-subagent` | `implementation-plan.md` |
| 5 | `developer-agent` | `implementation-notes.md` |
| 6 | `code-review` | `code-review.md` |
| 7 | `tester-agent` | `test-report.md` |
| 8 | `pr-agent` | `pr-body-$1.md` |

For each: present or missing, and the commit that added it (`git log --oneline -1 -- <path>`).

Then report:

- the next stage to run, and which agent owns it
- the verdict recorded in `code-review.md`, if that file exists
- whether a PR is already open for `feature/$1` (`gh pr list --head feature/$1`)

Read only. Do not create artifacts or run any stage.
