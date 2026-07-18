# Releasing Turicum

*Process record, written 2026-07-18. Companion documents: `BREW.md` (Homebrew
packaging, updated per release) and `homebrew/README.md`.*

This document describes how to publish a Turicum release from source to Maven
Central, and the follow-up steps afterwards.

## 1. How the version machinery works

There is a **single version source**:

    core/src/main/resources/turi/version.turi

`turicum_versions.turi` (project root) imports it as `VERSION`; `pom.turi`
consumes that; and the `turicum-maven-extension` — pinned in
`.mvn/extensions.xml` — regenerates every module's `pom.xml` from `pom.turi`
at the start of any `mvn` run. **Never edit a `pom.xml` by hand**; after
changing `version.turi`, the next Maven invocation rewrites all poms (they are
generated files but checked into git, so expect them in `git status`).

The `release` profile (defined in `pom.turi`) adds two things on top of the
normal build:

- `maven-gpg-plugin` — signs every artifact in the `verify` phase;
- `central-publishing-maven-plugin` (`org.sonatype.central`) with
  `publishingServerId: central` and `autoPublish: true` — uploads the bundle
  to the Central Portal, which validates and **releases automatically**; there
  is no manual "publish" click.

All seven modules deploy: `core` (the `turicum` artifact), `maven`,
`maven4` (the maven extensions), `cli` (including the
`turicum-cli-<v>-distribution.zip` that the Homebrew formula consumes),
`clifx`, `lsp`, and `testjar`.

## 2. Prerequisites (one-time per machine)

- `~/.m2/settings.xml` contains a `<server><id>central</id>…</server>` entry
  with the Central Portal user token.
- A GPG secret key usable by `maven-gpg-plugin` (agent-based pinentry
  configured so signing works non-interactively enough for a long build).
- `jamal` on the PATH (for regenerating `REFERENCE.adoc`).

## 3. Release steps

1. **Set the version.** Edit `core/src/main/resources/turi/version.turi`:

   ```
   let `turicum.version` = "2.0.0";
   ```

   Semantic versioning: breaking language or embedding-API changes bump the
   major (e.g. the `bin` return-type change of `str.bytes()` and the
   `IMPORT`/`FILE_READ` capability split → 2.0.0).

2. **Regenerate the version-carrying documentation.**

   - `REFERENCE.adoc` — regenerate with Jamal (the default `{`/`}` delimiters
     collide with sample code, hence the explicit markers):

     ```sh
     jamal --open='{%' --close='%}' REFERENCE.adoc.jam REFERENCE.adoc
     ```

     Do this *after* a build, so sample outputs (e.g. `-version`) show the new
     version. The regeneration also rewrites nondeterministic sample outputs
     (timings, thread interleavings) — that churn is normal and committed.

   - `EMBEDDING.md` — the Maven dependency snippet in §1.1 is **hand-written**
     (outside the mdship-managed blocks) and must be bumped manually:

     ```xml
     <version>2.0.0</version>
     ```

3. **Verification build** — everything the release does except the upload:

   ```sh
   mvn clean verify -Prelease
   ```

   Regenerates the poms to the new version, runs the full test suite, builds
   the source/javadoc jars and the CLI distribution zip, and GPG-signs it all.
   Javadoc errors and signing problems surface here, not mid-deploy.

4. **Commit and tag.** Commit everything, including the regenerated poms and
   reference outputs. Tag with the bare version number — the repository's tag
   pattern is `1.4.0`-style, no `v` prefix:

   ```sh
   git tag 2.0.0
   git push origin main 2.0.0
   ```

   (1.4.1 and 1.4.2 were released without tags; better to keep tagging.)

5. **Deploy:**

   ```sh
   mvn clean deploy -Prelease
   ```

   With `autoPublish: true` the Central Portal publishes without further
   interaction. The artifacts appear on `repo1.maven.org` typically within
   15–60 minutes; check:

   ```sh
   curl -s https://repo1.maven.org/maven2/ch/turic/turicum-cli/maven-metadata.xml | grep release
   ```

## 4. Post-release follow-ups

Once the artifacts are visible on Maven Central:

1. **Homebrew** (details in `BREW.md` §6):

   ```sh
   homebrew/update-formula.sh        # snapshots turicum@<old>.rb, bumps turicum.rb
   brew style homebrew/*.rb
   cp homebrew/turicum.rb homebrew/turicum@*.rb \
      "$(brew --repository)/Library/Taps/verhas/homebrew-tap/Formula/"
   brew reinstall turicum && brew test turicum
   cd "$(brew --repository)/Library/Taps/verhas/homebrew-tap"
   git add Formula && git commit -m "turicum <version>" && git push
   ```

   Commit the changed formulae in this repository as well.

2. **Bootstrap bump.** `.mvn/extensions.xml` pins the *previous released*
   `turicum-maven-extension`, which is what the build bootstraps with. After
   the release is on Central, bump it to the new version in a follow-up
   commit, so the next cycle builds with the freshly released extension.

3. **GitHub release** (optional):

   ```sh
   gh release create 2.0.0 --title "Turicum 2.0.0" --notes "..."
   ```

   optionally attaching `cli/target/turicum-cli-<v>-distribution.zip` for a
   non-Maven download.

## 5. Cautions

- **A released version cannot be re-deployed — but a deployment that fails
  Central's validation is discarded** and the version number stays free, so a
  validation failure can be retried with the same version.
- **Known failure: "Invalid signature for file: ….pom.asc".** The maven
  extension's `TuricumModelParser.locatePom()` regenerates `pom.xml` (with a
  fresh `Generated on:` timestamp and `<time>` property) every time Maven
  resolves a project mid-build — also *after* `maven-gpg-plugin` signed the
  pom, so the staged pom can be newer than its signature (observed on the
  2.0.0 release for `turicum` and `turicum-maven-extension`). Repair without
  a re-deploy:

  ```sh
  # re-sign the staged poms whose signature is stale
  gpg --batch --yes -ab target/central-staging/ch/turic/<artifact>/<v>/<artifact>-<v>.pom
  # verify every signature in the staging tree
  find target/central-staging -name '*.asc' | while read a; do
      gpg --verify "$a" "${a%.asc}" 2>&1 | grep -q "Good" || echo "BAD $a"; done
  # rebuild the bundle (same layout: the ch/ tree, no directory entries)
  ( cd target/central-staging && zip -qrD ../central-publishing/central-bundle-fixed.zip ch )
  ```

  then upload the bundle manually on central.sonatype.com ("Publish" →
  upload bundle) or via the Publisher API. The pom checksums need no repair:
  they are computed at bundle time over the staged (already-rewritten) poms.
  The structural fix shipped with the 2.0.0 extension: `PomXmlCreator`
  generates each pom **once per Maven session**, emits **no timestamp** (the
  output is deterministic), and skips the write entirely when the content is
  unchanged. The 2.0.0 release itself was built with the extension
  temporarily downgraded to 1.4.0 (which never wrote timestamps) and with the
  unused `time` property removed from `pom.turi`/`maven/pom.turi`; from the
  post-release bootstrap bump onward, `.mvn/extensions.xml` uses the fixed
  extension and the problem class is gone.
- `deploy` runs the whole test suite again; the reference tests rewrite
  `core/src/test/resources/references_output/*.txt` with nondeterministic
  content — commit or discard that churn deliberately, don't be surprised
  by it.
- The generated poms carry a generation timestamp comment, so every build
  touches them; this is expected.
