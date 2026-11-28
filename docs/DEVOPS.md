# DevOps

What is automated, what is manual, and what cannot be checked yet.

---

## Pipeline

[`.github/workflows/ci.yml`](../.github/workflows/ci.yml) runs on every push
to `main`, every pull request, and on demand.

| Job | Does | Fails when |
| --- | --- | --- |
| **Logic tests** | `./gradlew :core:test` — 77 tests | Amount parsing, EAN-13, grouping, month windows or the breakdown breaks |
| **Android lint** | `./gradlew :app:lintDebug` | Lint reports an error-severity problem |
| **Instrumented tests** | `connectedDebugAndroidTest` on an API 34 emulator — 2 tests | The app does not start, or Room cannot open its file on a real runtime |
| **App build and unit tests** | `assembleDebug`, `testDebugUnitTest` — 60 tests — then `assembleRelease` | The app stops compiling, the store or its mapping breaks, or R8 chokes on the release variant |

Both run on `ubuntu-latest` and sign nothing, so no keystore and no
credentials live in the repository.

The logic job needs no Android SDK at all — `:core` is a plain JVM module —
which is why it is the fast one and the one that says whether a port is
faithful.

The app job installs `platforms;android-37.1` and `build-tools;36.0.0`
explicitly instead of trusting whatever the runner image happens to ship.
Both jobs print their toolchain first, so a runner image refresh fails
visibly rather than mysteriously.

The app job's unit tests include the Room database itself, run in memory on
the JVM under Robolectric. That is deliberate: see
[DECISIONS.md](DECISIONS.md).

Lint runs as its own job, and keeps its report as an artifact whether it
passes or fails. It is worth less here than it would be in a View codebase:
its accessibility checks are written for `ImageView` and `onTouchEvent`, and
have almost nothing to say about Compose. The semantics tests in the unit
suite are what actually hold accessibility in place.

Instrumented tests now run on an emulator in their own job, which the camera
work was always the trigger for. The suite is deliberately thin — the app
starts, and Room opens its file on a real runtime — because that is the half
the unit suites cannot reach. Compose UI tests stay under Robolectric in the
unit suite, where they cost seconds rather than minutes.

**Still needs a physical device:** the camera scanner itself. An emulator's
camera cannot be shown a loyalty card.

## Toolchain

Every version is in [`gradle/libs.versions.toml`](../gradle/libs.versions.toml)
except Gradle itself, which is in the wrapper.

| Tool | Version | Pinned where |
| --- | --- | --- |
| Gradle | 9.9.2 | `gradle/wrapper/gradle-wrapper.properties`, with a SHA-256 |
| Android Gradle plugin | 9.5.2 | `libs.versions.toml` |
| Kotlin | 2.4.21 | `libs.versions.toml` |
| Compose BOM | 2026.11.01 | `libs.versions.toml` |
| Room | 2.8.5, processed by KSP 2.3.14 | `libs.versions.toml` |
| JDK | 21 in CI, 17 minimum | `ci.yml`, `compileOptions` |
| compileSdk | 37.1 — Compose 1.13 requires it | `app/build.gradle.kts` |
| targetSdk | 37 | `app/build.gradle.kts` |
| minSdk | 26 | `app/build.gradle.kts` |

AGP 9 enables built-in Kotlin, so there is no `kotlin-android` plugin to keep
in step. See [DECISIONS.md](DECISIONS.md).

## State of verification

**It has now been built and run.** Android Studio on the development machine,
Gradle on JDK 25, the app installed on an API 37 emulator with a seeded month
of data, and every screen walked through.

Until that point CI was the only thing that had ever compiled this code, and
the gap was real rather than stylistic. Running it found two defects in one
sitting that 126 passing tests had not:

- An expense with no note rendered its category twice, once as the headline
  and once as the supporting line — "Airtime" above "Airtime".
- The donut filled the width, which made it as tall as it was wide and pushed
  the legend off the bottom of the screen. The legend is the half that names
  the colours.

Both are fixed and were confirmed fixed on the device. Neither is the kind of
thing a test would have caught, because both suites were asserting what the
code computes rather than what it draws.

**Still unverified:** a barcode read by a real till scanner, the brightness
override on real hardware, text at a 200% font scale, and performance with
more than a seeded month of data.

## Releases

[`.github/workflows/release.yml`](../.github/workflows/release.yml) runs when
a tag like `v1.0.0` is pushed.

1. **Runs the whole of CI by calling it**, not by copying the jobs. A release
   that skipped the tests would be the moment the tests stopped meaning
   anything, and a copied job is one that drifts.
2. **Checks the tag against `versionName`** before building. `v1.1.0` on a
   1.0.0 app fails here, with the fix in the error message, rather than
   producing artefacts that carry the wrong number.
3. Builds an **App Bundle and an APK**, through R8.
4. Keeps the bundle, the APK and `mapping.txt` as artefacts for 30 days.
   The mapping file is what turns a future crash report back into line
   numbers; without it a stack trace from a release build is noise.
5. Publishes a **GitHub Release** with notes generated from the pull requests
   merged since the last tag — which is the other reason every change goes
   through one.

```bash
git tag v1.0.0 && git push origin v1.0.0
```

### Signing

The release build looks for an upload key in two places, in order: a
`keystore.properties` at the repository root, or the environment. Finding
neither, **it builds unsigned rather than failing** — so anyone can clone this
and run `bundleRelease` without being handed a key, and CI can prove the
release variant assembles before any secret exists.

To sign locally, create the key once and tell Gradle where it is. Neither file
is committed; both are in `.gitignore`.

```bash
keytool -genkeypair -v -keystore upload.jks -alias zaka \
  -keyalg RSA -keysize 2048 -validity 10000
```

```properties
# keystore.properties — never commit this
storeFile=/absolute/path/to/upload.jks
storePassword=…
keyAlias=zaka
keyPassword=…
```

For CI, the same four values are repository secrets, with the keystore itself
base64-encoded:

| Secret | What it is |
| --- | --- |
| `ZAKA_KEYSTORE_BASE64` | `base64 -i upload.jks` |
| `ZAKA_KEYSTORE_PASSWORD` | The store password |
| `ZAKA_KEY_ALIAS` | `zaka` |
| `ZAKA_KEY_PASSWORD` | The key password |

> **Keep the keystore and back it up somewhere that is not this machine.**
> Google Play ties an app to its upload key. Lose it and the recovery is a
> support request, not a command.

### Release size

R8 earns its place: the debug APK is about 21 MB and the release APK is
**1.7 MB**. Most of that difference is `material-icons-extended`, which ships
roughly two thousand icons and of which this app references about two dozen.
That is the measurement behind the decision to use the frozen icon artifact at
all — see [DECISIONS.md](DECISIONS.md).

### Versioning

`versionName` and `versionCode` are both edited by hand in
`app/build.gradle.kts`. Play rejects an upload whose `versionCode` is not
higher than the last one, so it goes up on every release, including a release
that only fixes something. The tag check only guards `versionName`; nothing
yet guards the code, which is a gap worth closing the first time an upload is
rejected.

## Project board

The [board](https://github.com/users/Jabulile1704/projects/12) has a Sprint
field with one-week iterations and the same columns as the iOS board.

| Column | Meaning | Moved by |
| --- | --- | --- |
| Backlog | Known work, not yet shaped | Hand |
| Ready | Shaped and unblocked | Hand |
| In Progress | On a branch | Hand |
| In Review | Pull request open | Hand |
| Testing | CI green, being checked on a device | Hand |
| Done | Merged to `main` | Hand |

**Every move is by hand on this repository.** The iOS repo automates them with
`board.yml` and a `PROJECT_TOKEN` secret; porting that workflow is worth
doing and is not part of sprint 1.

## Environments

One, eventually: the app on the phone. There is no server, no dev, staging or
production split, and nothing to monitor.

The emulator stands in for a test environment and diverges in one way that
already matters: **no camera**, so the card scanner
([#9](../../issues/9)) cannot be exercised there at all. Treat a green
emulator run as necessary, not sufficient.

## Deploying to a phone

```bash
./gradlew :app:installDebug          # over USB, with adb on the path
adb shell am start -n com.jabulile.zaka.debug/com.jabulile.zaka.MainActivity
```

The debug build has its own application id (`…zaka.debug`), so it installs
alongside a release build rather than replacing it. Once there is real data in
a release install, that separation is what stops a debug session from
overwriting it.

> **Install over the top. Never uninstall first.**
> Uninstalling destroys the expense database. `installDebug` over an existing
> copy keeps it.

## Backups

The only copies of the data will be:

- The SQLite store in the app's data directory.
- Whatever Android's backup captured — `backup_rules.xml` includes the
  database domain deliberately.
- CSVs exported by hand, once [#10](../../issues/10) exists.

Backups must not live inside the working tree. `backup/` is in `.gitignore`
from the first commit, because on the iOS repository a folder of real expense
data was committed to a public repository once already.

## Secrets

None in the repository: no API keys, no tokens, no keystore. The commit
identity is a GitHub noreply address, so no personal email is exposed.

The only sensitive material this project handles is the expense data itself,
and it belongs nowhere near the repo.
