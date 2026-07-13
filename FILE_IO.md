# Turicum File I/O Built-ins — API Proposal & Implementation Plan

*Status: API proposal for discussion, 2026-07-13. Nothing here is implemented yet.
Once the API is agreed, this document grows the detailed design decisions.*

The plan: a family of file-access built-in functions in a new package
`ch.turic.builtins.functions.fileio`, gated by file capabilities and confined by
sandbox roots. `Glob.java` moves into this package later.

## 1. Why

Two reasons, in order of importance:

1. **Convenience.** Today a script reads or writes a file through Java reflection
   (`java_class("java.nio.file.Files")` …), directly or via the `turi.io` system
   library, which is itself a thin reflection wrapper. That works but is clumsy and
   verbose for the most common scripting task there is.
2. **Controlled access for untrusted code.** A sandboxed script with
   `JAVA_REFLECTION` denied currently has *no* way to touch a file except `import`.
   There is no middle ground between "no file access" and "reflection, i.e. nearly
   everything". Purpose-built file built-ins give the host a way to grant *scoped*
   read and/or write access to a directory tree without opening reflection — and
   without the host having to write and allowlist its own facade class.

A third, structural benefit: once the built-ins exist, `turi/io.turi` can be
rewritten to delegate to them instead of reflection, so existing scripts using
`turi.io` start working in sandboxes that deny `JAVA_REFLECTION`.

## 2. What exists today (relevant pieces)

| Piece | Where | Relevance |
|---|---|---|
| `Capability.FILE_READ` | `ch.turic.Capability` | Gates `import`, `sys_import`, `glob`, `source_directory`, input-stream classes |
| `@RequiresCapability` registration gating | `BuiltIns` / ServiceLoader | A built-in whose capability is not granted is simply *not registered* — calling it is an "undefined symbol" error |
| Runtime capability query | `GlobalContext.grantedCapabilities()` | Allows argument-dependent checks inside a built-in (needed for e.g. create-on-write) |
| `importRoot` | `SandboxPolicy.Builder.importRoot()`, enforced in `AppiaHandler.confineToImportRoot()` | The confinement pattern to replicate: absolutize + normalize + prefix check, denial reported as a policy error, root is a ceiling, not a search path |
| `with` command resource protocol | `WithCommand` | Resource object must provide `entry`/`exit`; exit runs in reverse order, in `finally`, under the grace-step window |
| `with`-compatible built-in resource | `TuriHttpClient` (`http_client`) | Existing pattern: returns an `LngObject` with `entry`/`exit` fields holding `TuriMethod`s |
| Java-object method binding | `TuriClass` SPI, e.g. `TuriInputStream` | Alternative pattern for handle objects |
| `turi/io.turi` | system library | Current reflection-based `files.read_all_lines` / `files.write` |

One constraint discovered in `WithCommand.wrapCallingEntry()`: the resource object
must implement **both** `HasFields` and `HasContext`, even in the `as alias` form
where only `HasFields` is actually used. `LngObject` satisfies both; a lean
Java-implemented handle like `LngMutex` (only `HasFields`) currently cannot appear
in a `with` header. See decision D3.

## 3. Capability extension

Extend `ch.turic.Capability`:

| Capability | Gates | New? |
|---|---|---|
| `FILE_READ` | reading content and metadata: `file_read`, `file_lines`, `file_reader`, `file_exists`, `is_file`, `is_dir`, `file_stat`, `glob` (after the move), plus — unchanged — `import`/`sys_import`/`source_directory` and the input-stream classes | exists |
| `FILE_WRITE` | modifying content of *existing* files: `file_write`, `file_writer` (both also need `FILE_CREATE` at runtime when the target does not exist) | **new** |
| `FILE_CREATE` | bringing new file-system entries into existence: `mkdir`, and the create-a-new-file half of `file_write`/`file_writer`/`file_copy`/`file_move` | **new** |
| `FILE_DELETE` | removing entries: `file_delete`, and the remove-the-source half of `file_move` | **new** |

**Write implies read.** Granting any of `FILE_WRITE`/`FILE_CREATE`/`FILE_DELETE`
automatically grants `FILE_READ` — there is no real use case for a script that
may write files but not read them, and pretending to support it would only
produce half-tested capability combinations. The implication is normalized at
`build()`: an untrusted `allow()` of a write-family capability adds `FILE_READ`;
a trusted `deny(FILE_READ)` that leaves any write-family capability granted is a
build-time error (deny the whole family instead).

Gating stays registration-first, per the existing model: a built-in is registered
only when *all* its `@RequiresCapability` capabilities are granted. Functions whose
capability need depends on arguments or file-system state (create-on-write,
copy/move) declare the capability that is *always* needed and check the
situational ones at runtime through `GlobalContext.grantedCapabilities()`, failing
with a policy-denial `ExecutionException` that names the missing capability.

Concretely:

- `file_write` / `file_writer` require `FILE_WRITE` for registration; when the
  target file does not exist they additionally demand `FILE_CREATE` at runtime.
  So `FILE_WRITE` alone means "may update existing files only" and
  `FILE_WRITE + FILE_CREATE` means "may also create new ones". A host that wants
  "create new files but never touch existing ones" grants both and we honor an
  `overwrite=false`-style option — capabilities stay coarse, options stay per call.
- `file_copy` requires `FILE_READ` (registration) and at runtime `FILE_WRITE`
  (target exists) or `FILE_CREATE` (target does not).
- `file_move` requires `FILE_DELETE` (registration; it always removes the source)
  and at runtime `FILE_WRITE`/`FILE_CREATE` for the target side.

## 4. Root separation in `SandboxPolicy`

Today `importRoot` does double duty: it confines imports *and* is documented to
confine `glob`. Import confinement and data-file confinement are different
concerns — a host typically wants imports from a curated library directory and
data access somewhere else entirely. Proposal:

There are **two kinds of file roots, and both are sets** — the builder methods
are repeatable (and varargs), and the implementation confines against the whole
set, not a single path:

```java
SandboxPolicy.untrusted()
        .allow(Capability.FILE_WRITE, Capability.FILE_CREATE)   // FILE_READ implied, §3
        .importRoot(Path.of("/opt/scripts/lib"))       // imports only (meaning narrows)
        .fileReadRoot(Path.of("/srv/jobs/data"),       // read-only trees, repeatable
                      Path.of("/srv/shared/lookup"))
        .fileReadWriteRoot(Path.of("/srv/jobs/out"))   // full-access trees, repeatable
        .build();
```

- `importRoot(Path)` — **meaning narrows**: confines only `import`/`sys_import`
  (the `AppiaHandler` path). `glob` moves out from under it. Stays a single root.
- `fileReadRoot(Path...)` — repeatable; trees where `FILE_READ` built-ins
  (including the relocated `glob`) may operate, read-only.
- `fileReadWriteRoot(Path...)` — repeatable; trees with full access: every file
  built-in may operate here — read, write, create, delete. There is deliberately
  **one** root kind for all mutating capabilities; per-capability
  `fileCreateRoot`/`fileDeleteRoot` sets would be overkill (the *capabilities*
  already say which operations exist; the roots say *where*). And since write
  implies read (§3), a read-write root is naturally also readable — so the name
  says read-write, not "write".
- A read-write root may be disjoint from, identical to, or nested inside a
  read-only root; no relationship is required or checked.
- Both sets live on the shared `Builder`, like `importRoot` today;
  `GlobalContext` gets `fileReadRoots`/`fileReadWriteRoots` (as
  `List<Path>`) next to `importRoot`, set by `TuriSession` from the policy.

Effective confinement per operation:

- **read/metadata** — the path must fall under *any* root of either set;
- **write/create/delete** — the path must fall under *any* read-write root.

Untrusted-mode `build()` validation (extending the existing `FILE_READ ⇒
importRoot` rule):

- `FILE_READ` granted (directly or by implication) ⇒ `importRoot` required, and
  at least one root in `fileReadRoot ∪ fileReadWriteRoot` required.
- Any of `FILE_WRITE`/`FILE_CREATE`/`FILE_DELETE` granted ⇒ at least one
  `fileReadWriteRoot` required.

In trusted and unrestricted mode the roots stay optional guardrails, exactly like
`importRoot` today.

### Path confinement semantics

One shared helper (working name `ch.turic.builtins.functions.fileio.SafePath`)
used by *every* fileio built-in, mirroring `AppiaHandler.confineToImportRoot()`:

1. Accept the script's path string, resolve to a `Path`.
2. **Relative paths resolve against the configured roots** when any are set (the
   sandboxed script sees a stable virtual tree independent of the host's CWD);
   against the process CWD when no roots are set (trusted/unrestricted — today's
   behavior of `glob`). With multiple roots the resolution rule needs a
   convention — see D4.
3. Absolutize, normalize (collapsing `..`), and require the result to start with
   one of the applicable roots (read set ∪ read-write set for reads, read-write
   set for mutations). For reads/deletes of existing paths, run the check on
   `toRealPath()` so a symlink inside a root cannot alias a file outside it
   (see D5). For creation, the check runs on the real path of the deepest
   existing ancestor plus the remaining segments.
4. A path outside every applicable root fails with a policy-denial
   `ExecutionException` ("resolves to '…', outside the sandbox file roots […]"),
   never a plain "not found" — same wording style as the import denial.

## 5. Proposed Turicum API

All functions take paths as **strings** (`/`-separated; also accepting a Java
`Path`/`File` object for trusted interop is harmless since those can only be
obtained via reflection). All errors are ordinary `ExecutionException`s carrying
the underlying I/O message, so scripts can `try`/`catch` them. All functions are
implemented with the `Declare.params(...)` parameter machinery like `Glob`.

### 5.1 Whole-file convenience functions

```text
fn file_read(path: str, @binary: bool = false, @charset: str = "UTF-8")
    -> str | bytes                                          # FILE_READ

fn file_lines(path: str, @charset: str = "UTF-8")
    -> lst of str                                           # FILE_READ

fn file_write(path: str, content,
              @append: bool = false,
              @overwrite: bool = true,     # false + existing file -> error
              @mkdirs: bool = false,       # create parent dirs (needs FILE_CREATE)
              @binary: bool = false,
              @charset: str = "UTF-8")
    -> none                                # FILE_WRITE (+ FILE_CREATE if target is new)
```

`file_write` with `overwrite=false` uses `CREATE_NEW` atomically — no
check-then-create race deciding which capability applies; the runtime
`FILE_CREATE` demand is made whenever the open options *may* create the file.

### 5.2 Metadata and directory functions

```text
fn file_exists(path: str) -> bool                           # FILE_READ
fn is_file(path: str)     -> bool                           # FILE_READ
fn is_dir(path: str)      -> bool                           # FILE_READ

fn file_stat(path: str) -> obj                              # FILE_READ
   # { size: int, modified: int, created: int,   (epoch milliseconds, integer)
   #   is_file: bool, is_dir: bool, is_symlink: bool,
   #   readable: bool, writable: bool }

fn mkdir(path: str, @recurse: bool = true) -> none          # FILE_CREATE
   # recurse=true creates missing parent directories as well

fn file_delete(path: str, @force: bool = false,
               @must_exist: bool = false) -> bool           # FILE_DELETE
   # returns whether something was deleted; deleting a non-empty
   # directory without force=true is an error

fn file_copy(src: str, dst: str, @overwrite: bool = false)  # FILE_READ (+ runtime
    -> none                                                 #  FILE_WRITE/FILE_CREATE)

fn file_move(src: str, dst: str, @overwrite: bool = false)  # FILE_DELETE (+ runtime
    -> none                                                 #  FILE_WRITE/FILE_CREATE)
```

`glob` keeps its current signature and moves into the `fileio` package; its
confinement switches from `importRoot` to the file read/read-write root sets.

### 5.3 Streaming handles — `file_reader` / `file_writer`

Two functions rather than one `file_open(path, mode)`: the whole point of the
capability model is that an unavailable operation is an *unregistered symbol*, not
a runtime denial (`Capability` javadoc: "hiding the built-in is exactly the
point"). A mode string would force `file_open` to exist under `FILE_READ` alone
and deny `"w"` at runtime. Splitting keeps registration-gating honest:

```text
fn file_reader(path: str, @binary: bool = false, @charset: str = "UTF-8")
    -> file_reader handle                                   # FILE_READ

fn file_writer(path: str,
               @append: bool = false, @overwrite: bool = true,
               @mkdirs: bool = false,
               @binary: bool = false, @charset: str = "UTF-8")
    -> file_writer handle                  # FILE_WRITE (+ FILE_CREATE if target is new)
```

Reader handle fields (all methods; the handle is read-only):

| method | behavior |
|---|---|
| `read_line()` | next line without terminator, `none` at EOF |
| `read(n)` | up to `n` chars (or bytes in binary mode), `none` at EOF |
| `read_all()` | the rest of the stream as one `str`/bytes |
| `close()` | idempotent |
| `entry()` / `exit(e)` | `with` protocol; `exit` closes, returns `false` (never suppresses) |

Writer handle fields:

| method | behavior |
|---|---|
| `write(x)` | writes `str(x)` (or bytes in binary mode) |
| `write_line(x)` | `write(x)` + newline |
| `flush()` | flushes |
| `close()` | flush + close, idempotent |
| `entry()` / `exit(e)` | `with` protocol; `exit` closes, returns `false` |

Usage — the reason the handles exist:

```text
with file_writer("report.txt") as out {
    out.write_line("header")
    for each line in file_lines("data.txt") {
        out.write_line(process(line))
    }
}   // closed here, also on exception, also within the grace window after a halt
```

Every operation on a closed handle is an error (except `close`). Any I/O error
inside `exit` surfaces through the existing `WithCommand` close-exception
collection.

**Leak safety:** each open handle registers with the session's `GlobalContext`
and is force-closed when the interpreter/session ends or is aborted — a sandboxed
script that opens files in a loop and never closes them must not leak host file
descriptors past the session. (Working name: `GlobalContext.registerCloseable()` /
closed in the same place `joinThreads()` runs.)

### 5.4 `turi.io` library rewrite (follow-up)

`turi/io.turi`'s `files` class is rewritten to delegate to the new built-ins
(`files.read_all_lines` → `file_lines`, `files.write` → `file_write`), removing
its `java_class` dependency. The exported surface stays source-compatible.

## 6. Package layout & registration

```
core/src/main/java/ch/turic/builtins/functions/fileio/
    FileRead.java        @Name("file_read")
    FileLines.java       @Name("file_lines")
    FileWrite.java       @Name("file_write")
    FileExists.java      @Name("file_exists")   (+ IsFile, IsDir — possibly one class per name)
    FileStat.java        @Name("file_stat")
    Mkdir.java           @Name("mkdir")
    FileDelete.java      @Name("file_delete")
    FileCopy.java        @Name("file_copy")
    FileMove.java        @Name("file_move")
    FileReaderFn.java    @Name("file_reader")   (name avoids java.io.FileReader clash)
    FileWriterFn.java    @Name("file_writer")
    SafePath.java        shared root-confinement helper (§4)
    Glob.java            moved here later, unchanged name "glob"
```

Each class: `TuriFunction`, `@RequiresCapability(...)`, `Declare.params(...)`,
a `/*snippet builtinNNNN*/` documentation block for `REFERENCE.adoc` (regenerated
with Jamal), registered in `META-INF/services/ch.turic.TuriFunction` and
`module-info.java`. The handle classes live where D3 decides (either built as
`LngObject`s inside the functions, or as `LngFileReader`/`LngFileWriter` classes).

## 7. Implementation plan (once the API is agreed)

1. **Phase A — plumbing.** `Capability.FILE_WRITE/FILE_CREATE/FILE_DELETE` with
   the write⇒read implication; repeatable `fileReadRoot`/`fileReadWriteRoot` on
   `SandboxPolicy.Builder` + untrusted validation rules; `GlobalContext` root-set
   fields + `TuriSession` wiring; `SafePath` helper with its own unit tests
   (`..`, absolute escapes, symlinks, multi-root resolution, relative-resolution
   rules).
2. **Phase B — whole-file functions.** §5.1 + §5.2, tests per function: plain
   behavior, capability hiding (symbol undefined when not granted), runtime
   capability demands (create-on-write), root confinement, error texts.
3. **Phase C — handles.** `file_reader`/`file_writer`, `with` integration
   (including the D3 decision if it touches `WithCommand`), session-end
   force-close, abort/grace-window behavior test.
4. **Phase D — consolidation.** Move `Glob.java` into `fileio` and switch it to
   the file root sets; narrow the `importRoot` javadoc; rewrite `turi/io.turi`;
   update `Capability` javadoc, `EMBEDDING.md`, `REFERENCE.adoc` snippets.

## 8. Open decisions (please pick / veto)

- **D1 — Do write-side capabilities imply read? — DECIDED: yes.** Granting any
  of `FILE_WRITE`/`FILE_CREATE`/`FILE_DELETE` automatically grants `FILE_READ`;
  there is no real use case for write-without-read (§3).
- **D2 — Split `Capability.IMPORT` out of `FILE_READ`?** Today granting
  `FILE_READ` for data files also enables `import` (confined to `importRoot`, but
  still). A separate `IMPORT` capability would let a host grant imports without
  data reads and vice versa. Recommendation: **yes, split**, while the embedding
  API is young; it also makes the root separation of §4 symmetric
  (IMPORT↔importRoot, FILE_READ↔file root sets).
- **D3 — Handle representation.** (a) `LngObject` with `entry`/`exit` fields,
  the proven `http_client` pattern — but the script can reassign `exit` and
  fields are open; (b) dedicated Java classes implementing `HasFields` with an
  immutable field map (the `LngMutex` pattern), which requires relaxing
  `WithCommand.wrapCallingEntry()` to accept a `HasFields`-only resource in the
  `as alias` form (`HasContext` is only used by the alias-less form).
  Recommendation: **(b)** — the relaxation is small, benefits every future
  built-in resource, and session-end force-close is cleaner with a real class.
- **D4 — Relative path resolution under sandbox roots** (§4 item 2). Root-relative
  is recommended over CWD-relative-then-confined (root-relative gives untrusted
  scripts a stable virtual tree; CWD-relative usually just fails the confinement
  check confusingly), but with root *sets* the rule needs a convention.
  Recommendation: for **reads**, try the roots in declaration order (read-only
  roots first, then read-write roots) and take the first candidate that exists —
  the APPIA-search flavor; when none exists, the error names all roots searched.
  For **mutations**, resolve against the *first declared read-write root* — a
  write target must be deterministic, never search-dependent. A script that
  wants a specific root spells out an absolute path (the host can hand the root
  paths in as frozen globals). Alternative, stricter: relative paths are only
  legal with exactly one root of the applicable kind; with several, absolute
  paths are mandatory.
- **D5 — Symlink policy.** Recommendation: follow symlinks but confine the
  *real* path (a symlink pointing outside the root is denied). Alternative:
  `NOFOLLOW` everywhere — stricter, but breaks legitimate in-root symlink
  layouts.
- **D6 — Binary data representation.** Turicum has no byte-array type;
  `TuriInputStream.read_all_bytes` returns a raw Java `byte[]`, which is only
  useful with reflection. Options: (a) same raw `byte[]` (cheap, useless to
  untrusted code); (b) `lst` of numbers 0–255 (sandbox-friendly, memory-heavy);
  (c) defer — ship text-only first, add `@binary` in a second step once a bytes
  story exists. Recommendation: **(c)**, keeping `@binary` in the signatures
  reserved (declared but rejected as "not yet supported") or omitted until then.
- **D7 — `file_delete(force=true)` scope.** Recursive directory deletion is
  the most destructive primitive here. Keep it (guarded by `FILE_DELETE` +
  read-write-root confinement), or restrict deletion to files and empty
  directories only? Recommendation: keep it — the root is the safety boundary,
  not the feature set — but it deserves an explicit call-out in `EMBEDDING.md`.
- **D8 — Temp files** (`tmp_file()`/`tmp_dir()`): inherently *outside* the
  read-write roots, so they would need their own rule (e.g. host pre-creates a
  scratch dir inside a root). Recommendation: out of scope for now; a sandboxed
  script can make its own scratch area under a read-write root with `mkdir`.
