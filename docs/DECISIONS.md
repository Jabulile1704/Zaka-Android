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
