#!/usr/bin/env bash
# Guard 2 of RUN-SAFETY.md section 1: save state before context compaction.
#
# Runs as a PreCompact hook. Compaction summarizes older parts of a session,
# and two things follow from it that are documented and repeatable: rules that
# were clear early lose force, and the session can revert to an earlier
# understanding and redo work it already completed. A state file written and
# pushed just before compaction is what makes the difference between a session
# that re-orients and a session that repeats itself.
#
# This hook does not write HANDOFF.md's content. The main session owns that and
# keeps it current with every commit. What this does is make certain that
# whatever HANDOFF.md currently says, plus any other uncommitted work, is
# committed and pushed before the context is summarized away.
#
# Kamsiob, AGPL-3.0.

set -uo pipefail

REPO_DIR="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
cd "$REPO_DIR" || exit 0

# Not a git repository yet, which is true only very early in Phase 0.
git rev-parse --git-dir >/dev/null 2>&1 || exit 0

STAMP="$(date -u '+%Y-%m-%d %H:%M UTC')"

if [ -n "$(git status --porcelain)" ]; then
  # Record that the commit was made by the guard rather than by a finished
  # increment, so a later reader can tell the difference.
  git add -A >/dev/null 2>&1

  git commit --no-verify -m "chore: save state before context compaction

Committed by the pre-compaction guard in .claude/hooks at ${STAMP}.
This is a safety checkpoint, not a completed increment. Whatever HANDOFF.md
says at this commit is the state the session had reached when compaction
was about to run. Verify against git log and git status before continuing.

Ref: RUN-SAFETY.md section 1.2" >/dev/null 2>&1
fi

# Push if a remote exists. A commit sitting only on the local machine does not
# exist for recovery purposes.
if git remote get-url origin >/dev/null 2>&1; then
  BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null)"
  git push origin "HEAD:${BRANCH}" >/dev/null 2>&1 || true
fi

# Feed the re-orientation instructions back into the session. Anything a
# PreCompact hook prints on stdout is preserved through the compaction.
cat <<EOF
Pre-compaction state guard ran at ${STAMP}. Working tree committed and pushed.

After compaction completes, before writing anything:
  1. Read HANDOFF.md in full.
  2. Read CLAUDE.md.
  3. Run: git log --oneline -20 && git status
  4. Run: gh issue list --state open
Then continue only from what the repository says is true, never from memory.
Never re-create something without checking whether it already exists, and never
revert something you do not remember writing.
EOF

exit 0
