#!/usr/bin/env bash
#
# Renders the HTML sources in src/ to PDFs in this folder.
#
# The PDFs are committed, but they are build output: the sources are what you
# edit. Same reasoning as the iOS app icon being a script — a binary with no
# source is a document nobody can correct.
#
# Needs Google Chrome, which prints HTML and inline SVG to PDF without any
# further tooling. Override with CHROME=/path/to/binary if yours lives
# elsewhere, or point it at Brave or Edge, which take the same flags.
#
#   ./build.sh
#
set -euo pipefail

cd "$(dirname "$0")"

CHROME=${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}
if [ ! -x "$CHROME" ]; then
    echo "Chrome not found at: $CHROME" >&2
    echo "Set CHROME to a Chromium-based browser binary and run again." >&2
    exit 1
fi

# A throwaway profile, so rendering never touches the real browser's state.
PROFILE=$(mktemp -d)
trap 'rm -rf "$PROFILE"' EXIT

render() {
    local source="src/$1" out="$2"
    "$CHROME" \
        --headless \
        --disable-gpu \
        --no-pdf-header-footer \
        --user-data-dir="$PROFILE" \
        --virtual-time-budget=6000 \
        --print-to-pdf="$out" \
        "file://$PWD/$source" >/dev/null 2>&1
    printf '  %-44s %6s KB\n' "$out" "$(( $(wc -c < "$out") / 1024 ))"
}

echo "Rendering Zaka for Android documentation:"
render 01-requirements.html         "Zaka-Android-01-Requirements.pdf"
render 02-test-report.html          "Zaka-Android-02-Test-Report.pdf"
render 03-wbs.html                  "Zaka-Android-03-Work-Breakdown-Structure.pdf"
render 04-user-manual.html          "Zaka-Android-04-User-Manual.pdf"
render 05-erd.html                  "Zaka-Android-05-ERD.pdf"
render 06-use-cases.html            "Zaka-Android-06-Use-Case-Diagram.pdf"
render 07-low-fidelity-sketch.html  "Zaka-Android-07-Low-Fidelity-Sketch.pdf"
echo "Done."
