# Zaka for Android

<img src="docs/icon.png" alt="The Zaka launcher icon: a two-tone rand coin with a nickel ring, a milled edge and a brass centre carrying an R, shown under a circle mask, a squircle mask, at small sizes and as a themed monochrome icon." width="620">

A personal expense tracker for Android, in Kotlin and Jetpack Compose.
Everything is stored on the device — no account, no network, nothing leaves
the phone.

This is a port of [Zaka for iPhone](https://github.com/Jabulile1704/Zaka).
Same app, same information architecture, Android's own design language.

[![CI](https://github.com/Jabulile1704/Zaka-Android/actions/workflows/ci.yml/badge.svg)](https://github.com/Jabulile1704/Zaka-Android/actions/workflows/ci.yml)

## What it looks like

| Expenses | Add | Insights | Cards |
|:--:|:--:|:--:|:--:|
| <img src="docs/screenshots/expenses.png" width="200" alt="A list of expenses grouped by day. Today and Yesterday are named rather than dated, each heading carries that day's total, and each row shows a coloured category mark, the note, the category and the amount in rand."> | <img src="docs/screenshots/add.png" width="200" alt="The add-expense form. A large rand amount field at the top showing 45,50, then date and note, then a grid of sixteen category chips, two to a row, with Transport selected."> | <img src="docs/screenshots/insights.png" width="200" alt="Monthly insights. November 2026 with a total of R10,381.50 above a donut chart, and a legend listing Rent at 36 percent, Groceries at 21 percent and four more rows including a merged plus-six-more."> | <img src="docs/screenshots/cards.png" width="200" alt="A loyalty card shown full screen as an EAN-13 barcode in black on white, with the card number printed underneath and a Barcode and QR code switch."> |
| Grouped by day with a per-day total. Tap a row to edit, swipe to delete. The strip above the tab bar always shows the month to date. | Amount, date, note and one of sixteen categories. Both a comma and a dot are the decimal point. | Month total, spend per day so far, and where the money went — as a donut or as bars. | Shop cards, so the plastic can stay at home. Full brightness while the code is up. |

Screenshots are from a seeded emulator, not real spending.

## Where this is

**Sprint 1 of 4 — Foundation.** The project builds, the logic that the two
platforms share is ported and under test, and the theme is in place. There
are no screens yet: launching the app shows a placeholder.

| Sprint | What it delivers |
| --- | --- |
| 1 — Foundation | Gradle project, ported logic and its tests, CI, theme, documentation |
| 2 — Data and UI | Room, the Expenses, Add and Insights screens, accessibility |
| 3 — Cards | Loyalty cards, the camera scanner, CSV export, icon and splash |
| 4 — Release | Google Play |

Work is tracked as [issues](https://github.com/Jabulile1704/Zaka-Android/issues)
on the [board](https://github.com/users/Jabulile1704/projects/12), one week
per sprint.

## Documentation

| Document | What's in it |
| --- | --- |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | The two modules, the layering, and where a new file goes |
| [docs/SDLC.md](docs/SDLC.md) | Each lifecycle phase, what was done and what was skipped |
| [docs/DEVOPS.md](docs/DEVOPS.md) | Pipeline, toolchain versions, environments, releases |
| [docs/DECISIONS.md](docs/DECISIONS.md) | Why the non-obvious choices are the way they are |
| [docs/WORKFLOW.md](docs/WORKFLOW.md) | Branching, commits and pull requests |

## Running it

Open the project in Android Studio and press Run, or from the terminal:

```bash
./gradlew :app:assembleDebug
```

Tests:

```bash
./gradlew :core:test          # the ported logic: plain JVM, no SDK, no emulator
./gradlew :app:testDebugUnitTest
```

You need **JDK 17 or newer** and an Android SDK with **API 37.1**. Android
Studio installs both. `minSdk` is 26 (Android 8.0).

## Layout

```
tools/
  generate_icon.py          Draws the launcher icon and writes the vector drawables
settings.gradle.kts         The two modules
gradle/libs.versions.toml   Every dependency version, in one place
core/                       Pure Kotlin. No Android, no Compose, no Room.
  Money.kt                  Rand formatting and keypad parsing
  Ean13.kt                  Check digit, validation, the 95 modules
  Expense.kt                The domain record
  ExpenseCategory.kt        The sixteen categories, as data
  DayGroup.kt               Buckets expenses into calendar days
  SpendBreakdown.kt         Ranks and merges a month's categories
  BarcodeSymbology.kt       EAN-13, Code 128 or QR, and what each accepts
  CardDisplayMode.kt        Which form a card offers, and which it opens as
  CardNumber.kt             The last four digits
app/                        The Android app
  MainActivity.kt           Entry point, edge to edge
  ui/theme/                 Colour, shape, type, and the theme itself
```

The dependency runs one way: `app` sees `core`, and `core` sees neither
Android nor Compose. That is what lets `./gradlew :core:test` run in seconds
on any machine with a JDK.

## Notes

Amounts accept either separator: on a South African keypad the decimal key is
`,`, so `45,50` and `45.50` both mean 45.50, while `1,234` is read as grouping
and means 1234. Display is always `R1,234.56` regardless of device locale.

Currency is fixed to Rand in `Money.kt`; change `SYMBOL` there to switch it.
