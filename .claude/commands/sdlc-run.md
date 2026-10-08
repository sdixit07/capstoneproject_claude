---
description: Run the governed eight-stage SDLC pipeline for one Jira Story
argument-hint: "<STORY-ID>, e.g. EPMCDMETST-67216"
---

Run the full capstone SDLC pipeline for Story **$1**.

Delegate to the `orchestrator-agent`, which owns stage sequencing. Do not perform the stages
yourself and do not skip ahead.

Before starting, confirm:

1. `$1` looks like an `EPMCDMETST-*` key. If no Story id was given, stop and ask for one.
2. The working tree is clean (`git status`). If it is not, stop and report what is uncommitted.
3. The current branch is `feature/$1`. If it does not exist, the requirements agent creates it in
   Step 1 — never start this pipeline on `main`.

Then hand off to `orchestrator-agent` with the Story id. Each stage must commit its artifact to
`docs/sdlc/$1/` before the next begins, per `.claude/pipeline/pipeline-rules.md`.

Report back the stage reached, the artifacts written, and any gate that blocked.
