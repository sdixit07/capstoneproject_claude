#!/usr/bin/env node
// PreToolUse hook (matcher: Write|Edit|MultiEdit). Guards docs/sdlc/<STORY-ID>/ artifacts:
//   1. Order    - an artifact may only be written once the artifact of the previous stage exists.
//   2. Template - a full Write of an artifact that has a template must contain its required sections.
// Exit 2 = block, stderr is shown to Claude. Edits to files outside docs/sdlc/<STORY-ID>/ pass through.
const fs = require('fs');
const path = require('path');

// artifact -> prerequisite artifact (stage order from .claude/commands/sdlc-run.md)
const PREREQ = {
  'architecture.md': 'requirements.md',
  'design-review.md': 'architecture.md',
  'implementation-plan.md': 'design-review.md',
  'implementation-notes.md': 'implementation-plan.md',
  'code-review.md': 'implementation-notes.md',
  'test-report.md': 'code-review.md',
  PR_BODY: 'test-report.md',
};

// Required sections from .claude/templates/. Each entry is a list of accepted spellings
// (case-insensitive), so agents keep some wording freedom but cannot drop a section.
const TEMPLATE = {
  'requirements.md': [
    ['user story'],
    ['functional requirements'],
    ['non-functional requirements'],
    ['acceptance criteria'],
  ],
  'architecture.md': [
    ['high-level diagram', 'component diagram', 'architecture diagram'],
    ['component breakdown', 'components & responsibilities', 'components and responsibilities'],
    ['data flow'],
  ],
  'implementation-plan.md': [
    ['task id', 'T-1'],
    ['description', 'files'],
    ['dependencies', 'depends on'],
    ['estimated effort', 'effort'],
  ],
  'test-report.md': [
    ['environment'],
    ['test execution results'],
    ['coverage'],
    ['baseline comparison'],
    ['acceptance criterion verification'],
    ['verdict'],
  ],
};

let raw = '';
process.stdin.on('data', (d) => (raw += d));
process.stdin.on('end', () => {
  let input;
  try {
    input = JSON.parse(raw).tool_input || {};
  } catch {
    process.exit(0); // unreadable input: do not block
  }

  const file = String(input.file_path || '').replace(/\\/g, '/');
  const m = file.match(/(?:^|\/)docs\/sdlc\/([^/]+)\/([^/]+)$/);
  if (!m) process.exit(0);
  const [, storyId, name] = m;

  const block = (why) => {
    process.stderr.write(
      `Blocked: ${why}. Run the stages in order and follow .claude/templates/ ` +
        `(see .claude/commands/sdlc-run.md).\n`
    );
    process.exit(2);
  };

  const isPrBody = /^pr-body-.+\.md$/.test(name);
  const key = isPrBody ? 'PR_BODY' : name;

  const prereq = PREREQ[key];
  if (prereq) {
    const prereqPath = path.join(path.dirname(file), prereq);
    if (!fs.existsSync(prereqPath)) {
      block(`${name} for ${storyId} needs ${prereq} first (docs/sdlc/${storyId}/${prereq} is missing)`);
    }
  }

  // Template check only on a full Write; an Edit only carries a fragment of the file.
  const sections = TEMPLATE[key];
  if (sections && typeof input.content === 'string') {
    const text = input.content.toLowerCase();
    const missing = sections.filter((opts) => !opts.some((o) => text.includes(o.toLowerCase())));
    if (missing.length) {
      block(
        `${name} does not follow its template; missing section(s): ` +
          missing.map((opts) => `"${opts[0]}"`).join(', ')
      );
    }
  }
  process.exit(0);
});
