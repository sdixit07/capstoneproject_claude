#!/usr/bin/env node
// PreToolUse hook (matcher: Bash). Blocks `git commit` while on main/master and
// any `git push` that targets main/master. Exit 2 = block, stderr is shown to Claude.
const { execSync } = require('child_process');

let raw = '';
process.stdin.on('data', (d) => (raw += d));
process.stdin.on('end', () => {
  let command = '';
  try {
    command = JSON.parse(raw).tool_input.command || '';
  } catch {
    process.exit(0); // unreadable input: do not block
  }

  const block = (why) => {
    process.stderr.write(
      `Blocked: ${why}. Work on feature/<STORY-ID> or chore/<topic> (see .claude/pipeline/pipeline-rules.md).\n`
    );
    process.exit(2);
  };

  // Ignore text that is only data: heredoc bodies and quoted strings.
  command = command
    .replace(/<<-?\s*(['"]?)(\w+)\1[\s\S]*?\n\s*\2\b/g, ' ')
    .replace(/"(?:[^"\\]|\\.)*"|'[^']*'/g, '""');

  if (/\bgit\s+commit\b/.test(command) || /\bgit\s+push\b/.test(command)) {
    let branch = '';
    try {
      branch = execSync('git rev-parse --abbrev-ref HEAD', { encoding: 'utf8' }).trim();
    } catch {
      process.exit(0);
    }
    const protectedBranch = /^(main|master)$/;
    if (/\bgit\s+commit\b/.test(command) && protectedBranch.test(branch)) {
      block(`git commit on protected branch '${branch}'`);
    }
    if (/\bgit\s+push\b/.test(command)) {
      const pushArgs = command.split(/\bgit\s+push\b/)[1].split(/&&|\|\||[;|\n]/)[0];
      if (/(^|[\s:])(main|master)(\s|$)/.test(pushArgs)) {
        block('git push targeting main/master');
      }
      if (protectedBranch.test(branch)) {
        block(`git push from protected branch '${branch}'`);
      }
    }
  }
  process.exit(0);
});
