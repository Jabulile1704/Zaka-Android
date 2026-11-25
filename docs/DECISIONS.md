# Decisions

The choices that would otherwise look arbitrary — and would get "simplified"
back into bugs.

Decisions carried over from iOS are marked **(ported)**; the reasoning is in
[the iOS DECISIONS.md](https://github.com/Jabulile1704/Zaka/blob/main/docs/DECISIONS.md)
and is not repeated here unless Android changed it.

---

### The shared logic is a plain JVM module, not an Android library

`:core` applies `org.jetbrains.kotlin.jvm`, not `com.android.library`. An
Android library module would drag the SDK into the one part of the codebase
that has no business needing it, and its tests would need a `Context` or
Robolectric to run.

The cost is that `:core` cannot use `androidx.annotation`, Parcelize or
anything else from the platform. For money, dates and barcodes that is not a
cost at all.

See [ARCHITECTURE.md](ARCHITECTURE.md).

### Material 3 Expressive, and no attempt at Liquid Glass

[#11](../../issues/11)

The iOS app is built on Liquid Glass: `glassEffect`, a glass tab bar, a
background wash for the glass to refract. None of that exists on Android, and
approximating it with blurs and translucency is the clearest signal that
someone has not used the platform.

So the port keeps the information architecture and drops the surface.
`ZakaTheme` is a `MaterialExpressiveTheme` with `MotionScheme.expressive()`,
a slightly rounder shape scale than the baseline, and Material's own type
scale unchanged.

`MaterialExpressiveTheme` is stable as of `material3` 1.5.0, so none of this
sits behind an opt-in annotation.

### Dynamic colour is on by default

Android 12 and up gets the wallpaper's palette; older devices get Zaka's own
brass-and-nickel scheme, taken from the rand coin the app is marked with.

This is the opposite of the iOS app, which has one fixed palette. On Android
the platform convention is that the system's colour wins, and a tracker the
user opens several times a day is exactly the kind of app that should look
like it belongs on their phone. `ZakaTheme(dynamicColor = false)` forces the
brand scheme, which is what the previews use so they stay comparable.

### Categories are stored as raw strings **(ported)**

The database column is `ExpenseCategory.raw`, never the enum's name or
ordinal, and `ExpenseCategory.stored()` maps retired values onto their
nearest replacement **on read** without rewriting anything.

Android adds a second reason: Room would happily persist an enum by name, and
a rename refactor in the IDE would then change what the database means
without touching a migration.

### `ExpenseCategory` holds an icon *name*, not an icon

`:core` cannot reference `ImageVector`, so the enum carries a Material Symbols
token (`takeout_dining`) and a hex colour, and `:app` turns them into Compose
types. The alternative — a parallel map in the UI layer — splits one fact
across two files that drift.

The icons themselves are not the iOS SF Symbols. `takeoutbag.and.cup.and.straw.fill`
has no Material equivalent, and borrowing Apple's glyph set would be both
wrong-looking and a licensing problem.

### Amount parsing accepts ASCII digits only

Swift's `Character.isNumber` is true for Arabic-Indic and other Unicode
digits; Kotlin's `Char.isDigit()` is too. Both would then hand those digits to
a `Double` parser that cannot read them, and `toDoubleOrNull()` would return
null for input that looked valid on screen.

`isAsciiDigit()` makes the restriction explicit and the failure honest. A
South African keypad types ASCII.

### Formatting is pinned to `Locale.US`, not the device locale

`DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))`. The app always
shows `R1,234.56`, whatever the phone is set to, exactly as iOS does — a
screenshot means the same thing on every device, and the CSV export will not
suddenly emit comma decimals that a spreadsheet reads as text.

Input is the opposite: it takes whichever separator the keyboard offers.

### EAN-13 is implemented by hand **(ported)**

Android's barcode generators are in ML Kit or ZXing, neither of which is worth
a dependency for one symbology, and the modules are needed as data anyway —
they are drawn as vector bars so the code stays sharp at any size, because a
scaled bitmap barcode scans badly at the till.

Verified the same way as on iOS: encode, then decode through an independent
decoder written in the test, so a shared mistake cannot pass.

### Kotlin 2.4.21 rather than the version AGP ships with

AGP 9.5.2 has a runtime dependency on Kotlin Gradle plugin 2.2.10 and uses it
for built-in Kotlin. Declaring 2.4.21 in `libs.versions.toml` raises the whole
build to it, which is the upgrade path Google documents.

The reason to bother: androidx releases from late 2026 are compiled with
Kotlin 2.4, and a 2.2 compiler reading 2.4 metadata is a class of failure
with no good workaround.

### No `kotlin-android` plugin

AGP 9 enables built-in Kotlin by default, and `org.jetbrains.kotlin.android`
is **not compatible** with the new DSL — applying it fails the build with
`Cannot add extension with name 'kotlin'`. The app module applies only
`com.android.application` and the Compose compiler plugin.

`:core` still applies `org.jetbrains.kotlin.jvm`, which is unaffected: it is
not an Android module.

### `minSdk` 26, `compileSdk` 37.1

API 26 is Android 8.0, which covers effectively every phone still in use in
South Africa and is the floor for adaptive icons and `java.time` without
desugaring — and `java.time` is what `DayGroup` is built on.

API 37 is the highest major AGP 9.5 supports, and the minor is not cosmetic:
Compose 1.13 declares that anything consuming it must compile against 37.1 or
newer, so `compileSdk = 37` alone fails the build at `checkDebugAarMetadata`.
Hence `compileSdkMinor = 1`.

`targetSdk` stays at 37. Raising what the app compiles against is a separate
decision from opting in to a release's runtime behaviour.

CI installs `platforms;android-37.1` explicitly rather than trusting the
runner image to ship it, so a missing platform fails as a missing platform
and not as a mysterious build error.

### CI builds the release variant as well as debug

R8 only runs on release. A debug-only pipeline would not discover a shrinking
or keep-rule problem until the day of a release, which is the worst day to
discover it. The release APK CI produces is unsigned and thrown away.

### The Gradle wrapper jar came from Gradle's own tag, checksum-verified

`gradle/wrapper/gradle-wrapper.jar` is the file published with Gradle 9.9.2.
Its SHA-256 is `621008af…cdaf8`, which is the `wrapperChecksum` Gradle
publishes for that version at
`services.gradle.org/versions/current`. `gradle-wrapper.properties` also
carries `distributionSha256Sum`, so the distribution the wrapper downloads is
checked too.

Worth stating because a wrapper jar is the one binary in the repository, and
"a jar someone committed" is a reasonable thing to be suspicious of.

### The database is included in device backups

`backup_rules.xml` and `data_extraction_rules.xml` include the `database`
domain and nothing else. The iOS lesson was that the store is the only copy of
the data, so a device backup is the only safety net short of exporting CSV by
hand.

Nothing else in the app is worth restoring, and nothing in it is a secret:
loyalty cards only, never payment cards.

### The database is tested with Robolectric, not an emulator

[#4](../../issues/4)

The store tests assert things about SQL and about mapping — that a retired
category is read through its replacement without the row being rewritten, that
a month's boundary falls at local midnight, that saving a record with an id
updates it instead of adding another. None of that is about a device.

Robolectric runs the real Room database in memory on the JVM, so those
assertions live in `:app:testDebugUnitTest` and take seconds. An emulator job
would turn the same suite into a five-minute one and tell us nothing more. It
is pinned to `sdk=34` in `app/src/test/resources/robolectric.properties`
rather than following `targetSdk`, because Robolectric only emulates the API
levels it ships images for.

Instrumented tests are still the right place for what actually needs a device,
which starts with the camera scanner ([#9](../../issues/9)).

### Dates are stored as epoch milliseconds

A plain `INTEGER` column sorts correctly in SQL, needs no `@TypeConverter`,
and lets the month query be a `WHERE date >= ? AND date < ?` that an index can
serve. An ISO string would sort correctly too and be four times the size; a
Room converter for `Instant` would add a layer for no gain.

The half-open range matters more than the storage: `MonthWindow` in `:core`
turns a `YearMonth` into `[start, endExclusive)` at local midnight, so nobody
has to reason about whether the last millisecond of the month is included.

### `exportSchema` is off at version 1

A schema JSON exists to validate migrations against. There are no migrations
yet, and an exported schema that nobody has committed is worse than none at
all — it looks like a record and is not one.

Turn it on with the first migration, through the Room Gradle plugin rather
than the bare `room.schemaLocation` processor argument, which does not play
well with Gradle's configuration cache.

### Every ordering has a tiebreak

`ORDER BY date DESC, id DESC`, never `ORDER BY date DESC` alone. Two expenses
logged in the same millisecond — which the add form makes easy, since the date
defaults to now — would otherwise come back in whatever order SQLite felt like,
and the list would reshuffle between recompositions. The same bug as the chart
slices that sorted by dictionary order on iOS.

### No navigation library

[#5](../../issues/5)

`ZakaApp` holds the selected tab in `rememberSaveable` and switches on it.
That is the whole of navigation.

Four destinations, no deep links, no third level of screen, and one modal form
still to come. A `NavHost` would buy a back stack this app has nothing to put
on, and type-safe routes would pull in the serialization plugin to describe
four constants. `rememberSaveable` already survives rotation and process
death, which was the only real requirement.

The moment there is a deep link, a second level under a tab, or a screen worth
restoring by URL, this becomes Navigation Compose and the tab enum becomes its
start destinations. Until then it would be ceremony.

### Icons come from `material-icons-extended`, pinned at 1.7.8 by the BOM

Compose stopped publishing new versions of the icon artifacts after 1.7.8, and
Material Symbols are not shipped as a Compose library at all. The Compose BOM
still manages `material-icons-extended`, so that frozen version is the
sanctioned way to get the glyphs — and sixteen categories need sixteen
distinguishable ones.

The alternative was sixteen hand-written vector drawables, which on a machine
that cannot render them would be sixteen guesses.

`ZakaIcons` is the only file that names an icon, and
`ExpenseCategory.iconName` in `:core` still records which Material Symbol each
category *wants*. Replacing the set later is a change to one file.

R8 strips the icons the app does not reference, so the frozen artifact costs
release size nothing; the debug APK carries the lot.

### The month-to-date total belongs to the shell, not to a screen

The iOS app keeps it in the tab bar's bottom accessory, visible from every
tab, and that is the right call: it is the one number worth seeing while you
are doing something else. So it lives in `ZakaApp` above the `NavigationBar`,
reading the same `ExpensesViewModel` state the list reads.

It also means the total is announced as one thing — "This month, R8,568" —
rather than as a label and a number a screen reader has to join up.

### Rows delete in one direction only

`enableDismissFromStartToEnd = false`. A row that deletes whichever way it is
pushed is too easy to lose an expense to, and there is no undo yet. Swiping
the other way does nothing, which is a cheap way of making the gesture
deliberate.

### Add and edit are one form in two modes **(ported)**

[#6](../../issues/6)

`ExpenseFormViewModel` takes a nullable id: null adds, an id edits. One screen,
one set of validation rules, one place to change when a field is added.

### Editing keeps the expense's time of day

The form shows a date, never a time, but the record stores an instant. If an
edit rewrote the instant to "now", changing an amount would move the row to the
top of its day — the list is ordered `date DESC`, and the time is what orders
expenses *within* a day.

So the view model remembers the time of day it loaded and writes it back. A new
expense gets the clock instead, which is what makes "newest first" mean
anything inside today.

### The amount goes back into the field as plain digits

`String.format(Locale.US, "%.2f", amount)` — `249.90`, not `R249.90`. The field
is a text input that `Money.parse` has to read again on every keystroke, and
feeding it the display format would mean parsing a currency symbol and a
thousands separator back out.

### The date picker is driven in UTC

`DatePickerState` works in milliseconds that it interprets as UTC, so a
`LocalDate` is carried in at UTC midnight and read back the same way. Using the
device zone for that conversion shifts the selection by a day either side of
midnight — the picker would open on the wrong date for anyone east of
Greenwich, which includes everyone using this app.

### The Add tab resets its form instead of getting a new one

After a save the Add tab calls `reset()` on the same view model. The obvious
alternative — a new `viewModel(key = "add-$n")` per save — leaves every
previous form in the activity's `ViewModelStore` until the process dies.

Resetting also fixes the thing that would be worse than a leak: coming back to
the Add tab and finding the expense you just saved sitting there, ready to be
saved again.

### Sixteen chips, two to a row, not a lazy grid

`LazyVerticalGrid` inside a vertically scrolling column is a nested-scroll
fight, and sixteen chips are not worth laziness. `chunked(2)` with
`Modifier.weight(1f)` gives two equal chips per row and leaves room for the
label next to the icon, which is what makes the grid readable without relying
on the glyphs alone.

### The daily rate divides by the days that have happened

[#7](../../issues/7)

`total / today.dayOfMonth` for the current month, `total / month.lengthOfMonth()`
for any other. Dividing a month still running by all of its days understates
the rate every day except the last, which makes the number useless for the one
thing it is for: telling you, mid-month, whether you are spending faster than
usual.

### The donut's reveal is `SpendBreakdown.sweep`, not a view-layer animation

The sweep arithmetic already exists in `:core` and already has tests saying the
circle stays whole at every step of the animation. The chart animates a single
float from 0 to 1 and asks `:core` what to draw at that point, so the thing
that could be wrong — the geometry — is the thing that is tested.

The animation restarts when the slice ids change, which is what makes stepping
to another month redraw rather than morph. On iOS this needed an explicit
32-millisecond sleep so SwiftUI had a frame at zero to animate from; Compose
gets that for free, because `remember(key)` resets the target and the first
composition renders it.

### `SharedPreferences`, not DataStore

One enum, read once when a screen opens. DataStore would add a dependency, a
coroutine and a flow to remembering whether the breakdown was last seen as a
donut or as bars.

This is the only preference the app has. If a second screenful of them
appears, or any of them needs to be observed rather than read, that is the
moment to change.

### Every slice announces as one thing

A legend row is a coloured mark, a name, a percentage and an amount — four
nodes a screen reader would read as four unrelated fragments. Each row is
`clearAndSetSemantics` with "Groceries, 37%, R3,170" instead. The same applies
to the bars, to the day headings in the list, and to the month strip.

This is accessibility as a consequence of how the screen is built rather than
something to retrofit, which is what [#14](../../issues/14) is for.

### Swiping to delete has an accessible twin

[#14](../../issues/14)

A swipe gesture does not exist for someone using TalkBack, so every expense
row carries a `CustomAccessibilityAction` labelled "Delete this expense" that
calls exactly the same lambda. A test invokes the action and asserts the
deletion, so the two cannot drift apart.

This is the gap that a lint run would never have found, and the reason
accessibility belongs in the screen that creates it rather than in a pass at
the end.

### Compose tests run under Robolectric, not on an emulator

`createComposeRule()` works in the unit suite, which means the semantics a
screen reader is handed can be asserted in seconds: that a day heading is one
announcement and carries `heading()`, that a breakdown row reads "Groceries,
75%, R150.00" rather than as four fragments, that the month arrows are not
unlabelled icons.

Android lint is the belt and these are the braces — and the braces do more of
the work, because lint's accessibility checks are written for Views.

An emulator job still waits for what genuinely needs a device: the camera
scanner ([#9](../../issues/9)).
