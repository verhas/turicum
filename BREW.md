# Turicum Homebrew Packaging

*Status: implemented and verified locally, 2026-07-17, for release 1.4.2. One
publishing step remains open — pushing the tap repository to GitHub, see §5.*

This document records how the Homebrew packaging of the Turicum CLI works, what
was set up, what state exists on the development machine, and what to do on
every release. The operational short version lives in `homebrew/README.md`;
this is the full description.

## 1. Design: what gets installed, from where

The packaging deliberately builds **nothing**. Every Maven release already
publishes a self-contained CLI distribution to Maven Central:

    https://repo1.maven.org/maven2/ch/turic/turicum-cli/<version>/turicum-cli-<version>-distribution.zip

The zip is produced by `cli/src/main/assembly/bin.xml` (maven-assembly-plugin,
`dist` execution) and contains exactly three flat jars:

| Jar | Role |
|---|---|
| `turicum-cli-<version>.jar` | the CLI, entry point `ch.turic.cli.Main` |
| `turicum-<version>.jar` | the core interpreter |
| `jline-<version>.jar` | terminal handling for the REPL |

Maven Central serves an official `.sha256` next to the artifact, which is what
the formula pins. The Homebrew formula:

1. downloads the zip (the formula uses the `search.maven.org/remotecontent`
   form of the URL because `brew audit` prefers it; it serves the identical
   file as `repo1.maven.org`);
2. installs the three jars into the formula's `libexec` (so they are not on
   anyone's classpath by accident);
3. writes a `turi` wrapper script into `bin`:

   ```bash
   #!/bin/bash
   exec "<openjdk>/bin/java" -cp "<libexec>/*" ch.turic.cli.Main "$@"
   ```

   The explicit `-cp` glob is needed because the cli jar has no `Main-Class`
   manifest entry. The wrapper runs on Homebrew's `openjdk` formula
   (dependency), so users do not need their own Java; Turicum needs Java 21+
   and the Homebrew `openjdk` is always newer than that.

The command is called **`turi`**, matching the `.turi` source-file extension:

    turi program.turi     # run a program
    turi -REPL            # interactive REPL
    turi -compile x.turi  # compile to .turc
    turi -version
    turi -help

## 2. Files in this repository

Everything lives under `homebrew/`:

| File | Purpose |
|---|---|
| `homebrew/turicum.rb` | The formula. Clean under `brew style` and `brew audit --strict`. Carries a `livecheck` block (Maven Central `maven-metadata.xml`) and a `test do` block that runs a one-line program and checks `turi -version`. |
| `homebrew/update-formula.sh` | Release helper: determines the latest release from Maven Central (or takes a version argument), fetches Central's official sha256, verifies it by downloading and hashing the artifact, and rewrites the formula's `url`/`sha256` lines in place. Before bumping, it snapshots the outgoing version as a versioned formula `turicum@<oldversion>.rb` (see §7). |
| `homebrew/README.md` | Short operational readme: install, publish as tap, release steps. |

Formula details worth remembering:

- There is **no explicit `version` line** — brew scans the version from the
  URL, and an explicit line is flagged as redundant by `brew audit --strict`.
  `update-formula.sh` therefore only rewrites `url` and `sha256`.
- The file starts with `# typed: strict` / `# frozen_string_literal: true` and
  a class documentation comment — all required by `brew style`.
- `desc` must not start with the formula name (another style rule), hence
  "Programming language: interpreter, compiler, and REPL".

## 3. The tap

Homebrew 6 refuses to install a formula from a bare file path
(`Error: Homebrew requires formulae to be in a tap`), so distribution happens
through a **tap** — a git repository named `homebrew-<name>` on GitHub.

The tap was created locally with `brew tap-new verhas/tap`. It lives at:

    $(brew --repository)/Library/Taps/verhas/homebrew-tap

It is a normal git repository, scaffolded by brew with standard CI workflows
(`.github/workflows/tests.yml`, `publish.yml` for test-bot/bottling — usable
later, ignorable for now), and contains the formula as
`Formula/turicum.rb`, committed as "turicum 1.4.2 (new formula)".

The formula exists in two places by design: `homebrew/turicum.rb` in this repo
is the **source of truth** that is versioned and updated together with
Turicum; the tap's `Formula/turicum.rb` is the **published copy**. Every
release copies the former over the latter (§6).

## 4. What was verified (2026-07-17)

- `brew style homebrew/turicum.rb` — no offenses.
- `brew audit --strict verhas/tap/turicum` — no problems.
- `brew install verhas/tap/turicum` — installs; `which turi` →
  `/opt/homebrew/bin/turi`.
- `turi -version` → "Turicum Version 1.4.2 (Built: 2026-06-02…)".
- A hello-world `.turi` program runs.
- `brew test turicum` — passes (runs the formula's `test do` block).
- The sha256 in the formula equals Maven Central's official
  `…distribution.zip.sha256` **and** the locally computed hash of the
  downloaded artifact:
  `c766df5cc426fd8511f6e36c5b5ec3d59fe0690587864b3dac50e665401a9c3c`.

`turi` 1.4.2 is installed on this machine through the local tap
(`brew uninstall turicum` removes it).

## 5. Remaining step: publish the tap (one-time)

1. Create the GitHub repository **`verhas/homebrew-tap`** (public).
2. Push the local tap repo to it:

   ```sh
   cd "$(brew --repository)/Library/Taps/verhas/homebrew-tap"
   git remote add origin git@github.com:verhas/homebrew-tap.git
   git push -u origin main
   ```

From then on, anyone installs Turicum with:

```sh
brew install verhas/tap/turicum
```

(`brew` expands `verhas/tap` to `github.com/verhas/homebrew-tap` and taps it
automatically.) Worth adding to `README.adoc` as the macOS/Linux install
instruction once the tap is public.

## 6. Per-release workflow

This is step "Homebrew" of the overall release process described in
`RELEASE.md`. After a normal Maven release has reached Maven Central:

```sh
homebrew/update-formula.sh            # or: homebrew/update-formula.sh 1.4.3
brew style homebrew/*.rb              # lint the bumped and the snapshot formula
cp homebrew/turicum.rb homebrew/turicum@*.rb \
   "$(brew --repository)/Library/Taps/verhas/homebrew-tap/Formula/"
brew reinstall turicum                # smoke-test the new version locally
brew test turicum
cd "$(brew --repository)/Library/Taps/verhas/homebrew-tap"
git add Formula && git commit -m "turicum <version>"
git push
```

Also commit the updated/added formulae under `homebrew/` in this repository.

`brew livecheck turicum` compares the formula against Maven Central's
`<release>` metadata and reports when the formula lags a release.

## 7. Multiple installable versions

Homebrew's model is one formula file per installable version: `turicum.rb`
always tracks the latest release, and *versioned formulae* named
`turicum@<version>.rb` keep specific older releases installable:

```sh
brew install verhas/tap/turicum          # latest
brew install verhas/tap/turicum@1.4.2    # pinned older release
```

`update-formula.sh` creates these automatically: on every bump it snapshots
the outgoing version first (skipping the snapshot if the file already
exists). A versioned formula differs from the main one in three ways:

- file and class name: `turicum@1.4.2.rb` must contain class `TuricumAT142`
  (brew's mapping: `@` → `AT`, dots dropped);
- no `livecheck` block — a pinned version has nothing to check;
- `keg_only :versioned_formula` — pinned versions install but do not link
  `turi` into the PATH, so any number of them can coexist with the main
  formula. A pinned version is run via
  `$(brew --prefix turicum@1.4.2)/bin/turi`, or force-linked with
  `brew link --overwrite turicum@1.4.2` after unlinking the others.

Versions that predate the snapshot mechanism remain recoverable from the
tap's git history with `brew extract turicum verhas/tap --version=<v>`, which
generates the corresponding `turicum@<v>.rb` from a past commit. Users who
only want to stop upgrading need none of this: `brew pin turicum` blocks
upgrades until unpinned.

## 8. Possible future improvements

- **Automate the release bump**: a GitHub Actions workflow in the tap repo
  (scheduled, or triggered by the Turicum release) that runs the equivalent of
  `update-formula.sh`, commits, and pushes. Not set up yet.
- **Bottles**: the tap scaffolding contains brew's test-bot workflows; since
  the formula only unpacks jars and writes a script, bottling brings little —
  installation is already fast — but it removes the Java-less install-time
  dependency on curl reaching Maven Central.
- **`brew install turicum` without the tap prefix** would require acceptance
  into homebrew-core; their bar (notability, no pre-built binaries policy
  exceptions for Java are common, though — many Java tools ship jars) can be
  attempted once the language has more users.
