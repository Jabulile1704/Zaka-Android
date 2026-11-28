# Software development lifecycle

How Zaka for Android is being built, phase by phase. Where a phase was
skipped or done informally, it says so — an honest record is more useful
later than a tidy one.

This is a port, which changes the shape of the lifecycle: requirements and
design were largely settled by the iOS app, and testing starts from an
existing specification rather than from nothing.

---

## 1. Requirements

**Done, and unusually well, because they already existed.**

The requirement is "the iOS app, on Android". That gives something the
original build never had: a working reference implementation, a tested
specification of the logic, and four screens whose behaviour is already
decided.

What was actually decided for this repository:

| Decision | Choice | Why |
| --- | --- | --- |
| Language and UI | Kotlin, Jetpack Compose | Declarative, closest to the SwiftUI the port comes from |
| Storage | Room, on device | Matches SwiftData's role; no account, no server |
| Design language | Material 3 Expressive | Android's own; Liquid Glass does not exist here |
| `minSdk` | 26 | `java.time` without desugaring, adaptive icons |
| Shared logic | A plain JVM module | Tests run without an SDK or emulator |

The scope was then cut into 15 issues across 4 one-week sprints before any
code was written, which is more planning than the iOS app got.

**Gap:** still no written acceptance criteria. "Done" means the ported tests
pass and the screen looks right next to the iPhone.

## 2. Design

**Technical design done up front for once.**
[ARCHITECTURE.md](ARCHITECTURE.md) was written as part of sprint 1 rather
than reconstructed afterwards: the module split, the dependency direction,
the four rules about where state lives, and the decision to keep Room
entities separate from domain records.

That is a direct response to the iOS retrospective, which found that the
category rework would have been cheaper with a schema sketch.

**Visual design is inherited and then deliberately diverged from.** The
information architecture — four screens, sixteen categories, a month-to-date
total always in view — carries over unchanged. The surface does not:
[#11](../../issues/11).

**Gap:** no mockups for the Android screens. Sprint 2 will design them in
code, which risks drifting into a translated iOS layout rather than an
Android one.

## 3. Implementation

**Sprints 1 and 2 complete; two to go.**

| Issue | State |
| --- | --- |
| [#1](../../issues/1) Scaffold the Gradle project | Done |
| [#2](../../issues/2) Port the pure logic and its tests | Done |
| [#3](../../issues/3) Continuous integration | Done |
| [#11](../../issues/11) Material 3 Expressive theme | Done |
| [#15](../../issues/15) SDLC, DevOps and architecture documentation | Done |
| [#4](../../issues/4) Room database for expenses and cards | Done |
| [#5](../../issues/5) Expenses list grouped by day | Done |
| [#6](../../issues/6) Add and edit an expense | Done |
| [#7](../../issues/7) Monthly insights with a category breakdown | Done |
| [#14](../../issues/14) Accessibility from the start | Done |

The port went test-first: the Swift test suite was translated before the code
it tests, so the Kotlin had a specification to satisfy rather than a reference
to paraphrase. Two behaviours that would have been easy to get subtly wrong —
the comma-decimal rule and the EAN-13 check digit — are pinned by tests
translated directly from the originals.

Unlike the iOS codebase, this one keeps comments. The iOS app had them
stripped, which pushed all the reasoning into commit messages and documents.
Here the *why* that belongs next to a line stays next to it, and the documents
cover the decisions that span files.

## 4. Testing

**Stronger at this stage than iOS ever was, and still narrow.**

`:core:test` — 42 tests, plain JVM, no SDK, no emulator:

| Area | Coverage |
| --- | --- |
| Amount parsing | Separators, grouping, partial input, rejection, formatting |
| EAN-13 | Check digits, validity, guard patterns, encode/decode round trip |
| Categories | Live values, retired values, unknown fallback, uniqueness, colour format |
| Day grouping | Bucketing, per-day totals, newest first, the zone's day boundary |
| Spend breakdown | Slice ranking, ties, the merge boundary, totals preserved, the sweep |
| Cards | Symbology detection, acceptance, last four, stored round trip |

Three areas were deliberately left behind: the SwiftData store
round trip and the CSV export belong to sprints 2 and 3, and the one that
asserts a QR image is generated needs a renderer that does not exist yet.

Two tests were added that iOS did not have: day grouping across a time-zone
boundary, and the colour format of every category.

**Gap, and it is the whole of it:** nothing above touches a screen, a
database or a device. There are no instrumented tests, no Compose UI tests,
and — as recorded in [DEVOPS.md](DEVOPS.md) — the app has not yet been run by
a human at all. Sprint 1 is verified entirely by CI.

## 5. Deployment

**Not started.** [#13](../../issues/13), sprint 4. Until then the only
artefact is an unsigned debug APK built by CI and discarded.

No equivalent of the iOS account ceiling: no seven-day provisioning expiry and
no entitlement that cannot be signed. Google Play registration is a one-off
fee.

## 6. Maintenance

**Not applicable yet.** No releases, no users, no incidents.

The one incident inherited in spirit from the iOS repository is the committed
expense data, which is why `backup/` is in `.gitignore` in the very first
commit here rather than added after the fact.

---

## Traceability

Each closed issue carries a comment listing the commits that delivered it, so
the chain runs **issue → commit → code** in both directions:

```bash
gh issue list --repo Jabulile1704/Zaka-Android --state all
gh issue view 2 --repo Jabulile1704/Zaka-Android
```

## Honest summary

| Phase | State |
| --- | --- |
| Requirements | Strong — inherited from a working app, then cut into sprints |
| Design | Technical written up front; Android screen design still to do |
| Implementation | Sprints 1 and 2 of 4 complete |
| Testing | Logic, store, state holders and screen semantics covered; appearance not |
| Deployment | Not started |
| Maintenance | Nothing to maintain yet |
