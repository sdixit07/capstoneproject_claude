---
description: Run the governed eight-stage SDLC pipeline for one Jira Story
argument-hint: "<STORY-ID>, e.g. EPMCDMETST-67216"
---

Run the full capstone SDLC pipeline for Story **$1**. You (the main session) sequence the stages
and delegate each one to its agent. There is no separate orchestrator agent. Do not perform the
stage work yourself and do not skip ahead.

## Before starting

1. `$1` looks like an `EPMCDMETST-*` key. If no Story id was given, stop and ask for one.
2. The working tree is clean (`git status`). If it is not, stop and report what is uncommitted.
3. The current branch is `feature/$1`. If it does not exist, `requirements-agent` creates it in
   Step 1. Never start this pipeline on `main`.

## Stages

| Step | Agent | Artifact / outcome |
|---|---|---|
| 1 | `requirements-agent` | `requirements.md`, feature branch created |
| 2 | `architecture` | `architecture.md` |
| 3 | `design-review`, then `architecture` (apply-review), then `design-review` (record) | `design-review.md`, architecture updated |
| 4 | `planner-subagent` | `implementation-plan.md` |
| 5 | `developer-agent` | code + unit tests, `implementation-notes.md`, branch pushed |
| 6 | `code-review` (blockers fixed by `developer-agent` in fix mode) | `code-review.md` |
| 7 | `tester-agent` | `test-report.md`, coverage of at least 85% |
| 8 | `pr-agent` | README/CHANGELOG, PR opened, review comment posted, Jira linked |

## Rules for every stage

- Run stages strictly in order, one at a time. Never in parallel, merged or reordered.
- Each stage commits its artifact to `docs/sdlc/$1/` before the next begins.
- After each stage print a completion report (Story, stage, agent, summary, artifacts and commit
  hashes, open issues, next stage), ask whether to proceed, then stop and wait. Accepted
  approvals: `approve`, `yes`, `y`, or a clear instruction to continue.
- If the user rejects a stage, re-run only that stage's agent, update its artifact and ask again.
- Never claim Jira data was fetched, a branch pushed, a PR opened or a comment posted unless the
  tool call succeeded. If a tool or agent is unavailable, say which and ask how to proceed.

## Stage-specific rules

- **Step 1:** ask `requirements-agent` for clarifying questions first, relay them to the user,
  then run it in final mode with the answers.
- **Step 3:** after the user picks which recommendations to accept, run `architecture` in
  apply-review mode, then `design-review` in record mode.
- **Step 5:** require a baseline test run, unit tests, passing builds and a push of
  `feature/$1`. Report the pushed commit range.
- **Step 6:** if the verdict lists blockers, send them to `developer-agent` in fix mode, push,
  then re-run `code-review` before seeking approval.
- **Step 7:** if coverage is below 85% or tests fail, report it and return to `developer-agent`.
  Do not proceed to Step 8.
- **Step 8:** `pr-agent` opens the PR with its five required sections. Never merge it.

Report back the stage reached, the artifacts written, and any gate that blocked. All other rules
are in `.claude/pipeline/pipeline-rules.md`.
