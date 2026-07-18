# Installing Turicum with Homebrew

*This is the short operational readme; the full description of the packaging —
design, tap layout, verification record, release workflow — is in
[`../BREW.md`](../BREW.md).*

This directory contains the Homebrew packaging of the Turicum command-line
interpreter. The formula does not build anything: it downloads the released
`turicum-cli-<version>-distribution.zip` from Maven Central (the artifact the
normal Maven release already publishes, with its official sha256), installs the
three jars into the formula's `libexec`, and creates a `turi` wrapper script in
`bin` that starts `ch.turic.cli.Main` on the Homebrew-managed JDK.

## Installing directly from a checkout

```sh
brew install ./homebrew/turicum.rb
turi -version
turi -REPL
```

## Publishing as a tap (the `brew install verhas/tap/turicum` experience)

Homebrew distributes third-party formulae through *taps*: GitHub repositories
named `homebrew-<name>`. One-time setup:

1. Create the repository `github.com/verhas/homebrew-tap`.
2. Copy the formula into it as `Formula/turicum.rb` and push.

Users then install with:

```sh
brew install verhas/tap/turicum
```

(`brew` expands `verhas/tap` to `github.com/verhas/homebrew-tap` and taps it
automatically.)

## Releasing a new version

After a normal Maven release reaches Maven Central:

```sh
homebrew/update-formula.sh            # picks up the latest release automatically
# or: homebrew/update-formula.sh 1.4.3
brew style homebrew/*.rb              # lint
brew reinstall turicum                # smoke-test locally (after copying to the tap)
brew test turicum
```

Besides bumping `turicum.rb`, the script snapshots the **outgoing** version as
a versioned formula `turicum@<oldversion>.rb` (class `TuricumAT<digits>`, no
`livecheck`, `keg_only :versioned_formula`), so earlier releases stay
installable with `brew install verhas/tap/turicum@<version>`. Being keg-only,
a pinned version does not link `turi` into the PATH; run it via
`$(brew --prefix turicum@<version>)/bin/turi`.

Copy the updated `turicum.rb` **and** the new `turicum@…rb` into the tap
repository's `Formula/` directory and push. The main formula also carries a
`livecheck` block, so `brew livecheck turicum` reports when Maven Central has
a newer release than the formula.

## Notes

- The formula depends on Homebrew's `openjdk` (Turicum needs Java 21+; the
  current openjdk formula is always newer than that). Users do not need their
  own Java installation.
- The wrapper is called `turi`, matching the `.turi` source-file extension.
- The `test do` block runs a one-line Turicum program and checks
  `turi -version`, so `brew test turicum` verifies a working installation.
