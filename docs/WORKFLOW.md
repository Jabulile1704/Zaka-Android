# Branching and pull requests

The iOS repository was built straight onto `main` and only adopted branches
late. This one starts with them.

---

## The short version

```bash
git switch -c feat/expenses-list       # branch
# ... work, commit as you go ...
git push -u origin feat/expenses-list  # push the branch
gh pr create --fill                    # open the PR
# CI runs. Review. Then:
gh pr merge --squash --delete-branch
```

## Branch names

`<type>/<short-description>`, lower case, hyphens.

| Prefix | For |
| --- | --- |
| `feat/` | New capability — `feat/expenses-list` |
| `fix/` | Something behaving incorrectly — `fix/comma-decimal-parsing` |
| `docs/` | Documentation only — `docs/architecture` |
| `chore/` | Tooling, CI, housekeeping — `chore/bump-compose-bom` |
| `refactor/` | Shape changes with no behaviour change |

Branch per issue, not per day. If a branch is growing a second unrelated
change, that is a second branch.

**Sprint 1 is the one exception, deliberately.** Its five issues all landed on
one branch, `chore/sprint-1-foundation`, because none of them can stand on
its own: there is nothing to put under test until the project exists, nothing
for CI to build until there is a build, and nothing to document until the
shape is decided. From sprint 2 onward, where each issue is a screen or a
layer that can be reviewed by itself, it is one branch per issue.

## Commits

- One reviewable unit per commit.
- Subject in the imperative, under about 70 characters.
- The body explains **why**, not what — the diff already says what.
- Reference the issue: `Closes #21` in the final commit, or in the PR body.

Unlike the iOS codebase, this one keeps code comments, so a commit body does
not have to carry reasoning that belongs next to a line. Use it for the
reasoning that spans files, and [DECISIONS.md](DECISIONS.md) for the
reasoning that outlives the commit.

## Pull requests

Every change that is not a typo goes through one, even working alone. The
value is not approval — it is the pause to read your own diff as a whole.

CI runs on every pull request, both jobs. **Do not merge red.**

Squash-merge by default, so `main` keeps one commit per change. Use a merge
commit only when the individual commits genuinely matter to later readers —
sprint 1 is a fair candidate, since its commits are one per issue.

Delete the branch on merge. `gh pr merge --squash --delete-branch` does both.

## The board

Every change starts as an issue and the pull request body says `Closes #N`.

Unlike the iOS repository, **nothing on the board moves by itself here**: the
`board.yml` automation and its `PROJECT_TOKEN` secret have not been ported.
Move the card by hand — In Progress when you cut the branch, In Review when
the pull request opens, Testing when CI passes, Done on merge — or port the
workflow and stop thinking about it.

## Releasing

Not yet. See [DEVOPS.md](DEVOPS.md#releases); the versioning and tagging
scheme is decided with [#13](../../issues/13) in sprint 4.

## When to skip the branch

Pushing straight to `main` is defensible for a typo in a document, or a
revert of something broken that is actively hurting you. Everything else gets
a branch; the overhead is about ten seconds.

## Worth adding later

**Branch protection on `main`** — require the CI checks to pass and block
direct pushes. Not enabled yet, deliberately: with a single committer it would
mostly get in the way, and a protected `main` cannot be fixed quickly when
something is on fire. Worth turning on the moment a second person commits, or
when the app has real users.

Settings → Branches → Add rule → require status checks `Logic tests` and
`App build and unit tests`.
