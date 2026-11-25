# Architecture

The iOS app never had this document, because SwiftUI and SwiftData left it
little to say: views read `@Query` and wrote to the model context, and that
was the whole of it. Android needs the layers drawn, because Compose,
ViewModels and Room each have an opinion about where state lives.

---

## Two modules

```
        ┌──────────────────────────────────────────┐
        │  :app   Android, Compose, Room           │
        │                                          │
        │  ui/        screens, theme, components   │
        │  data/      Room entities, DAOs, mappers │
        └───────────────────┬──────────────────────┘
                            │  depends on
                            ▼
        ┌──────────────────────────────────────────┐
        │  :core   plain Kotlin on the JVM         │
        │                                          │
        │  domain records, money, barcodes,        │
        │  grouping, breakdowns                    │
        └──────────────────────────────────────────┘
```

The arrow never reverses. `:core` has no dependency on Android, Compose or
Room — not even on `androidx.annotation` — so:

- `./gradlew :core:test` needs a JDK and nothing else: no SDK, no emulator,
  seconds to run.
- The logic that decides what an amount *means* cannot quietly start
  depending on a `Context`.
- If this ever becomes Kotlin Multiplatform, `:core` is already the shared
  part.

This mirrors the iOS split, where a Swift package held the same pure logic so
`swift test` could run without Xcode.

## Where a new file goes

| It is… | It goes in | Test with |
| --- | --- | --- |
| A rule about money, dates, barcodes or grouping | `:core` | `:core:test` |
| A record the app reasons about | `:core` | `:core:test` |
| A Room entity, DAO or mapper | `:app/data` | `:app:testDebugUnitTest`, Robolectric |
| A screen, a component, the theme | `:app/ui` | Compose UI test |
| A state holder for one screen | `:app/ui`, as a ViewModel | `:app:testDebugUnitTest` |

If a rule is hard to test because it needs a `Context`, that is usually a
sign the rule belongs in `:core` and only its *input* needs Android.

## Layers inside `:app`

```
Composable  ──reads──▶  UiState (immutable)
    │                        ▲
    │ events                 │ maps domain → UI
    ▼                        │
ViewModel  ──────────▶  Repository  ──────▶  DAO  ──────▶  Room
                              │
                              └──▶ :core (Money, DayGroup, SpendBreakdown)
```

Four rules, decided now so sprint 2 does not have to argue about them:

1. **Composables take state and emit events.** No composable reads a DAO.
2. **One `UiState` per screen**, immutable, assembled by the ViewModel. The
   list screen gets `List<DayGroup>` already grouped and already totalled,
   not a raw list it has to fold while recomposing.
3. **DAOs return `Flow`.** SwiftData's `@Query` re-ran the view when the
   store changed; `Flow` plus `stateIn` is the same idea with the wiring
   visible.
4. **Room entities are not domain types.** `ExpenseEntity` maps to
   `core.Expense`. It costs a mapper and it buys the thing that saved the iOS
   app twice: the database column stays a string, so renaming or retiring a
   category cannot re-map rows that are already saved.

The repositories are where rules 2 and 4 meet. `ExpenseRepository.observeDays()`
returns `Flow<List<DayGroup>>` — already bucketed, already totalled — rather
than rows for a composable to fold on every recomposition.

## What the categories are, and are not

`ExpenseCategory` lives in `:core`, so it cannot hold a Compose `Color` or an
`ImageVector`. It holds data instead:

- `raw` — exactly what the database stores, and what iOS stores.
- `label` — what a person reads.
- `iconName` — a Material Symbols token.
- `colorHex` — a six-digit sRGB triple.

`:app/ui/theme/CategoryColor.kt` is the one place that turns `colorHex` into a
`Color`. The icon mapping arrives with the screens that draw icons.

## Threading

Room and the mappers run off the main thread; Compose state is read on it.
Nothing else is concurrent: there is no network, no sync and no background
work, so there is no scheduler, no WorkManager and nothing to retry.

## What is deliberately absent

| Not here | Why |
| --- | --- |
| Dependency injection framework | Two modules and one database. A constructor and one factory are cheaper than Hilt. |
| Navigation library | Sprint 2 decides this once there are screens to navigate between. |
| Repository interfaces with fake implementations | There is one implementation. An interface with one implementor is a comment with ceremony. |
| A `:feature:*` module per screen | Four screens. Module boundaries would cost more than they clarify. |

Each of these is a reasonable thing to add later. None of them is worth
adding before the thing it abstracts exists twice.
