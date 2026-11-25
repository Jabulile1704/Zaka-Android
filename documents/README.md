# Documents

Project documentation as PDFs. Built from the HTML in [`src/`](src), which is
what you edit — the PDFs are output.

| Document | What is in it |
| --- | --- |
| [Requirements](Zaka-Android-01-Requirements.pdf) | 45 functional and 26 non-functional requirements, each traced to its issue and to the test that holds it |
| [Test Report](Zaka-Android-02-Test-Report.pdf) | The suites and their results, requirement coverage, the defect log, and an honest list of what is not covered |
| [Work Breakdown Structure](Zaka-Android-03-Work-Breakdown-Structure.pdf) | 25 work packages across four sprints, with dependencies, a day-by-day schedule and the critical path |
| [User Manual](Zaka-Android-04-User-Manual.pdf) | How to use the app, what it does with your data, and what it cannot do |
| [ERD](Zaka-Android-05-ERD.pdf) | The SQLite schema, and why it deliberately has no foreign keys |
| [Use Case Diagram](Zaka-Android-06-Use-Case-Diagram.pdf) | Eleven use cases, one human actor, three external systems |
| [Low-Fidelity Sketch](Zaka-Android-07-Low-Fidelity-Sketch.pdf) | Wireframes of the four screens, with the layout decisions they settled |

## Rebuilding

```bash
./build.sh
```

Needs Google Chrome, which prints HTML and inline SVG to PDF with no further
tooling. Any Chromium-based browser takes the same flags:

```bash
CHROME="/Applications/Brave Browser.app/Contents/MacOS/Brave Browser" ./build.sh
```

Rendering uses a throwaway profile, so it never touches your browser's state.

## Why the sources are committed

The same reason the iOS app icon is a script rather than an exported bitmap: a
binary with no source is a document nobody can correct. Edit the HTML, run
`build.sh`, and commit both.

Every number in these documents — test counts, requirement counts, defect
states — was taken from the repository at the time of writing. When the code
moves, the documents are stale until someone rebuilds them.
