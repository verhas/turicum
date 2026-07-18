#!/bin/bash
#
# Updates homebrew/turicum.rb to a new released version.
#
# Usage:  homebrew/update-formula.sh [version]
#
# Without an argument, the latest release is read from the Maven Central metadata.
# The script fetches the official sha256 of the distribution zip from Maven Central,
# verifies it by downloading the artifact and hashing it locally, and rewrites the
# url/sha256 lines of the formula in place.
#
# Before bumping, the current formula is snapshot as a versioned formula
# (turicum@<oldversion>.rb): the class is renamed to TuricumAT<digits>, the
# livecheck block is dropped (a pinned version has nothing to check), and
# `keg_only :versioned_formula` is added so pinned versions do not collide with
# the main formula on bin/turi. Copy the versioned formula into the tap next to
# the updated turicum.rb; see homebrew/README.md.
set -euo pipefail

DIR="$(dirname "$0")"
FORMULA="$DIR/turicum.rb"
BASE="https://repo1.maven.org/maven2/ch/turic/turicum-cli"

if [ $# -ge 1 ]; then
    VERSION="$1"
else
    VERSION=$(curl -fsS "$BASE/maven-metadata.xml" | sed -n 's:.*<release>\(.*\)</release>.*:\1:p')
    [ -n "$VERSION" ] || { echo "cannot determine the latest release from Maven Central" >&2; exit 1; }
fi

OLD_VERSION=$(sed -n 's|^  url ".*/turicum-cli-\(.*\)-distribution.zip"|\1|p' "$FORMULA")
[ -n "$OLD_VERSION" ] || { echo "cannot determine the current version from $FORMULA" >&2; exit 1; }

if [ "$OLD_VERSION" = "$VERSION" ]; then
    echo "$FORMULA is already at version $VERSION; nothing to do"
    exit 0
fi

# snapshot the outgoing version as a versioned formula, unless it already exists
SNAPSHOT="$DIR/turicum@$OLD_VERSION.rb"
if [ -e "$SNAPSHOT" ]; then
    echo "snapshot $SNAPSHOT already exists, leaving it untouched"
else
    awk -v cls="TuricumAT$(echo "$OLD_VERSION" | tr -d '.-')" -v old="$OLD_VERSION" '
        # drop the livecheck block and the blank line after it
        /^  livecheck do$/       { inlivecheck = 1; next }
        inlivecheck && /^  end$/ { inlivecheck = 0; skipblank = 1; next }
        inlivecheck              { next }
        skipblank && /^$/        { skipblank = 0; next }
                                 { skipblank = 0 }
        /^class Turicum < Formula$/ {
            print "# Turicum pinned at " old "; the unversioned `turicum` formula tracks the latest release."
            print "class " cls " < Formula"
            next
        }
        # a versioned formula is keg-only, so several versions can be installed
        # side by side without colliding on bin/turi
        /^  license / {
            print
            print ""
            print "  keg_only :versioned_formula"
            next
        }
        { print }
    ' "$FORMULA" > "$SNAPSHOT"
    echo "created $SNAPSHOT (class TuricumAT$(echo "$OLD_VERSION" | tr -d '.-'))"
fi

ZIP="turicum-cli-$VERSION-distribution.zip"
URL="$BASE/$VERSION/$ZIP"
# the formula uses the search.maven.org form of the same artifact URL, which the
# brew style/audit cops prefer; both serve the identical file
FORMULA_URL="https://search.maven.org/remotecontent?filepath=ch/turic/turicum-cli/$VERSION/$ZIP"

CENTRAL_SHA=$(curl -fsS "$URL.sha256")
LOCAL_SHA=$(curl -fsS "$URL" | shasum -a 256 | cut -d' ' -f1)
if [ "$CENTRAL_SHA" != "$LOCAL_SHA" ]; then
    echo "sha256 mismatch: Maven Central says $CENTRAL_SHA, the downloaded artifact hashes to $LOCAL_SHA" >&2
    exit 1
fi

# no explicit `version` line: brew scans the version from the URL, and an explicit
# line would be flagged as redundant by `brew audit --strict`
sed -i '' \
    -e "s|^  url \".*\"|  url \"$FORMULA_URL\"|" \
    -e "s|^  sha256 \".*\"|  sha256 \"$CENTRAL_SHA\"|" \
    "$FORMULA"

echo "updated $FORMULA from $OLD_VERSION to $VERSION (sha256 $CENTRAL_SHA)"
echo "remember to copy the changed formulae into the tap repository (see homebrew/README.md)"
