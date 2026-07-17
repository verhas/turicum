# Turicum File I/O Built-ins — API Proposal & Implementation Plan

*Status: API proposal for discussion, 2026-07-13; updated 2026-07-16 after the
`bin` type (`BIN_TYPE.md`) was designed and implemented. `bin` settles the binary
representation questions D6/D9: every binary read returns `bin` and every binary
write accepts `bin` — the interim latin-1-string idea is dropped and the
"text-only first" deferral of `@binary` is void. None of the file I/O built-ins
themselves are implemented yet. Once the API is agreed, this document grows the
detailed design decisions.*

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
| **`bin` type** (new since the first draft) | raw `byte[]` values; `builtins.functions.Bin`, `builtins.classes.TuriBin`, `memory.IndexedBin`, `utils.BinUtils`; design in `BIN_TYPE.md` | The return/argument type for all binary I/O in this plan; `Declare` already supports `bin` parameter declarations, so built-ins can declare `content: str\|bin` |
| `GlobalContext.registerTask()` | `ch.turic.memory.GlobalContext` (new since the first draft) | Session-end resource tracking pattern next to `joinThreads()` — the model for the `registerCloseable()` handle registry of §5.3 |

One constraint discovered in `WithCommand.wrapCallingEntry()`: the resource object
must implement **both** `HasFields` and `HasContext`, even in the `as alias` form
where only `HasFields` is actually used. `LngObject` satisfies both; a lean
Java-implemented handle like `LngMutex` (only `HasFields`) currently cannot appear
in a `with` header. See decision D3.

## 3. Capability extension

Extend `ch.turic.Capability`:

| Capability | Gates | New? |
|---|---|---|
| `FILE_READ` | reading content and metadata: `file_read`, `file_lines`, `file_reader`, `file_random_reader`, `file_map_reader`, `file_exists`, `is_file`, `is_dir`, `file_stat`, `glob` (after the move), plus — unchanged — `import`/`sys_import`/`source_directory` and the input-stream classes | exists |
| `FILE_WRITE` | modifying content of *existing* files: `file_write`, `file_writer`, `file_random_editor`, `file_map_editor` (the first three also need `FILE_CREATE` at runtime when they would create the target) | **new** |
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
    -> str | bin                                            # FILE_READ

fn file_lines(path: str, @charset: str = "UTF-8")
    -> lst of str                                           # FILE_READ

fn file_write(path: str, content: str | bin,
              @append: bool = false,
              @overwrite: bool = true,     # false + existing file -> error
              @mkdirs: bool = false,       # create parent dirs (needs FILE_CREATE)
              @binary: bool = false,
              @charset: str = "UTF-8")
    -> none                                # FILE_WRITE (+ FILE_CREATE if target is new)
```

`file_read(@binary=true)` returns a `bin` — implementation-wise
`Files.readAllBytes()` returned directly, zero-copy, since a `bin` *is* a raw
`byte[]` (`BIN_TYPE.md` §3). In text mode (`@binary=false`, the default) the
result is a `str` decoded with `@charset`.

`file_write` in binary mode writes the `content` bytes verbatim; in text mode it
writes `str(content)` encoded with `@charset`. Which argument types each mode
accepts is D11.

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
| `read_line()` | next line without terminator, `none` at EOF (text mode only) |
| `read(n)` | up to `n` chars as `str` (binary mode: up to `n` bytes as `bin`), `none` at EOF |
| `read_all()` | the rest of the stream as one `str` (binary mode: `bin`) |
| `close()` | idempotent |
| `entry()` / `exit(e)` | `with` protocol; `exit` closes, returns `false` (never suppresses) |

Writer handle fields:

| method | behavior |
|---|---|
| `write(x)` | writes `str(x)` encoded with `@charset` (binary mode: writes the `bin` bytes verbatim; accepted types per D11) |
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

**Leak safety:** each open handle — sequential (§5.3), random-access (§5.4), and
mapped (§5.5) — registers with the session's `GlobalContext` and is force-closed
when the interpreter/session ends or is aborted — a sandboxed script that opens
files in a loop and never closes them must not leak host file descriptors past
the session. (Working name: `GlobalContext.registerCloseable()` / closed in the
same place `joinThreads()` runs. `GlobalContext` has meanwhile gained exactly
this shape of registry for async tasks — `registerTask()`, a concurrent set
drained at `joinThreads()` with self-removal on completion — so the handle
registry follows that established pattern. For mapped handles "closed" means
invalidated; see the Java 21 caveat in §5.5.)

### 5.4 Random access — `file_random_reader` / `file_random_editor`

Sequential handles (§5.3) cover the common case; random access needs position
queries, seeking, and in-place updates. Same split-by-capability principle as
§5.3 — a read-only sandbox must not even see a function that could open a file
for updating:

```text
fn file_random_reader(path: str)
    -> random-access read handle                            # FILE_READ

fn file_random_editor(path: str,
                      @create: bool = false,   # create if missing (needs FILE_CREATE)
                      @truncate: bool = false) # empty the file on open
    -> random-access read/write handle         # FILE_WRITE (+ FILE_CREATE if create=true
                                               #  and the target is new)
```

The editor handle can also read — write implies read (§3), and random-access
writing is read-modify-write by nature.

Reader handle methods:

| method | behavior |
|---|---|
| `position()` | current position as byte offset from the start of the file |
| `seek(pos)` | sets the position; `pos` past EOF is legal (reads there hit EOF) |
| `size()` | current file size in bytes |
| `read(n)` | up to `n` bytes as a `bin` from the current position, advancing it; `none` at EOF |
| `read_at(pos, n)` | absolute-positioned read, returns `bin`; does *not* move the position |
| `close()` | idempotent |
| `entry()` / `exit(e)` | `with` protocol; `exit` closes, returns `false` |

Editor handle: all of the above, plus

| method | behavior |
|---|---|
| `write(x)` | writes the `bin` `x` at the current position, advancing it; writing past EOF extends the file (whether `str` is also accepted: D11) |
| `write_at(pos, x)` | absolute-positioned write of a `bin`; does *not* move the position |
| `truncate(size)` | cuts the file to `size` bytes (position clamps to the new end) |
| `flush()` | forces pending writes to the file |

Positions and sizes are **byte offsets** — random access is inherently
byte-oriented. Seeking in a multibyte-encoded text file can land inside a
character, so these handles have **no text mode**; `read`/`read_at` return `bin`
and `write`/`write_at` accept `bin` (D6/D9, settled by the now-implemented `bin`
type — `bin`'s integer codec methods `u32_at` & co. are exactly the tools for
processing what these handles read). Backed by `SeekableByteChannel`
(`FileChannel.open(...)`), which maps one-to-one onto this method set — and a
`bin` being a raw `byte[]`, reads and writes wrap it in a `ByteBuffer` without
copying.

### 5.5 Memory-mapped files — `file_map_reader` / `file_map_editor`

For large files where seek-read loops are too slow, a mapped view. Same
capability split:

```text
fn file_map_reader(path: str, @offset: int = 0, @length: int|none = none)
    -> mapped read handle                                   # FILE_READ

fn file_map_editor(path: str, @offset: int = 0, @length: int|none = none)
    -> mapped read/write handle                             # FILE_WRITE
   # length=none maps from offset to the end of the file; the editor form
   # with an explicit length may extend the file to offset+length
```

Handle methods:

| method | behavior                                                                    |
|---|-----------------------------------------------------------------------------|
| `length()` | length of the mapped region in bytes                                        |
| `get(pos, n)` | `n` bytes at offset `pos` within the mapping, as a `bin`                    |
| `put(pos, x)` | writes the `bin` `x` at offset `pos` (editor only); cannot grow the mapping |
| `flush()` | `MappedByteBuffer.force()` — pushes changes to the file (editor only)       |
| `close()` | invalidates the handle (see the caveat below)                               |
| `entry()` / `exit(e)` | `with` protocol; `exit` calls `flush` + `close`, returns `false`            |

**Java 21 caveat — no deterministic unmap.** The project targets Java 21, and
`MappedByteBuffer` has no supported unmap operation; the mapping is released
only when the buffer is garbage-collected. The FFM API
(`FileChannel.map(..., Arena)` returning a `MemorySegment`, with
`Arena.close()` unmapping deterministically) is final only in Java 22.
Consequences we must document and design around:

- `close()` invalidates the *handle* (every later `get`/`put` is an error) and
  drops the buffer reference, but the pages stay mapped until GC. On Windows
  the file cannot be deleted or truncated while mapped.
- Session-end force-close (§5.3) can therefore not guarantee release of
  mappings — a real difference from stream handles that `EMBEDDING.md` must
  state. A mapping is host virtual address space held on the sandbox's behalf.
- Sandbox exposure: proposal to add `maxMappedBytes(long)` to
  `SandboxPolicy.Builder` — the running total of `length` over all live
  mappings of a session may not exceed it; untrusted default 0 (mmap built-ins
  effectively unusable unless the host opts in), trusted/unrestricted default
  unlimited.
- When the project moves to Java 22+, the implementation switches to
  `MemorySegment`/`Arena` internally and `close()` becomes a true unmap — no
  script-visible API change. Whether to ship mmap at all before that is D10.

### 5.6 `turi.io` library rewrite (follow-up)

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
    FileRandomReader.java @Name("file_random_reader")
    FileRandomEditor.java @Name("file_random_editor")
    FileMapReader.java   @Name("file_map_reader")
    FileMapEditor.java   @Name("file_map_editor")
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
   the write ⇒ read implication; repeatable `fileReadRoot`/`fileReadWriteRoot` on
   `SandboxPolicy.Builder` + untrusted validation rules; `GlobalContext` root-set
   fields + `TuriSession` wiring; `SafePath` helper with its own unit tests
   (`..`, absolute escapes, symlinks, multi-root resolution, relative-resolution
   rules).
2. **Phase B — whole-file functions.** §5.1 + §5.2 including the `@binary`
   modes from the start (the old "ship text-only first" idea predates the `bin`
   type and is void), tests per function: plain behavior, capability hiding
   (symbol undefined when not granted), runtime capability demands
   (create-on-write), root confinement, error texts, binary round-trip
   (`file_write(bin)` → `file_read(@binary=true)` content equality).
3. **Phase C — handles.** `file_reader`/`file_writer`, `with` integration
   (including the D3 decision if it touches `WithCommand`), session-end
   force-close, abort/grace-window behavior test.
4. **Phase C2 — random access & mmap.** `file_random_reader`/`file_random_editor`
   (§5.4) — no longer blocked: the byte representation (D6/D9) is the implemented
   `bin` type; `file_map_reader`/`file_map_editor` (§5.5) with `maxMappedBytes`
   policy plumbing, if D10 decides to ship it on Java 21.
5. **Phase D — consolidation.** Move `Glob.java` into `fileio` and switch it to
   the file root sets; narrow the `importRoot` javadoc; rewrite `turi/io.turi`;
   update `Capability` javadoc, `EMBEDDING.md`, `REFERENCE.adoc` snippets.

## 8. Open decisions (please pick / veto)

- **D1 — Do write-side capabilities imply read? — DECIDED: yes.** Granting any
  of `FILE_WRITE`/`FILE_CREATE`/`FILE_DELETE` automatically grants `FILE_READ`;
  there is no real use case for write-without-read (§3).
- **D2 — Split `Capability.IMPORT` out of `FILE_READ`?** Today granting
  `FILE_READ` for data files also enables `import` (confined to `importRoot`, but
  still). A separate `IMPORT` capability would let a host grant imports without
  data reads and vice versa. DECIDED: **yes, split**, while the embedding
  API is young; it also makes the root separation of §4 symmetric
  (IMPORT↔importRoot, FILE_READ↔file root sets).
- **D3 — Handle representation.** (a) `LngObject` with `entry`/`exit` fields,
  the proven `http_client` pattern — but the script can reassign `exit` and
  fields are open; (b) dedicated Java classes implementing `HasFields` with an
  immutable field map (the `LngMutex` pattern), which requires relaxing
  `WithCommand.wrapCallingEntry()` to accept a `HasFields`-only resource in the
  `as alias` form (`HasContext` is only used by the alias-less form).
  DECIDED: **(b)** — the relaxation is small, benefits every future
  built-in resource, and session-end force-close is cleaner with a real class.
- **D4 — Relative path resolution under sandbox roots** (§4 item 2). Root-relative
  is recommended over CWD-relative-then-confined (root-relative gives untrusted
  scripts a stable virtual tree; CWD-relative usually just fails the confinement
  check confusingly), but with root *sets* the rule needs a convention.
  DECIDED: for **reads**, try the roots in declaration order (read-only
  roots first, then read-write roots) and take the first candidate that exists —
  the APPIA-search flavor; when none exists, the error names all roots searched.
  For **mutations**, resolve against the *first declared read-write root* — a
  write target must be deterministic, never search-dependent. A script that
  wants a specific root spells out an absolute path (the host can hand the root
  paths in as frozen globals). Alternative, stricter: relative paths are only
  legal with exactly one root of the applicable kind; with several, absolute
  paths are mandatory.
- **D5 — Symlink policy.** DECIDED: follow symlinks but confine the
  *real* path (a symlink pointing outside the root is denied). Alternative:
  `NOFOLLOW` everywhere — stricter, but breaks legitimate in-root symlink
  layouts.
- **D6 — Binary data representation — DECIDED: the `bin` type** (what the
  first draft called option (d), working name `LngBytes`). Designed in
  `BIN_TYPE.md` and implemented 2026-07-16: a `bin` is a raw Java `byte[]`
  (symmetric with `str`/`String`), mutable, fixed-length, unsigned at the
  language surface, with encoding/digest/search methods and the `u8_at` …
  `u64_at` integer codecs. Every `@binary` read in this document returns `bin`
  and every binary write accepts `bin`; the "text-only first" deferral is void.
- **D9 — Byte representation for the random-access and mmap handles — DECIDED:
  `bin`** (option (a); the prerequisite feature now exists). The latin-1
  `str`-as-bytes interim (option (b)) is dropped. One follow-up question moved
  here from `BIN_TYPE.md` D-6 — see D11.
- **D11 — Do binary write methods also accept `str`?** Applies to `file_write`
  with `@binary=true`, the binary `file_writer`'s `write`, the random-access
  `write`/`write_at`, and the mapped `put`. Accepting a `str` would need an
  implicit encoding choice (DECIDED: UTF-8 is the handle's `@charset` when nothing else is defined. the random-access
  handles have none). DECIDED: **no — binary writes accept `bin` only**;
  a script spells out `s.bytes(charset)`, keeping the encoding explicit. This
  matches the `bin` design's no-implicit-coercion stance (`BIN_TYPE.md` §7).
  Text mode conversely accepts anything and writes `str(x)`, as today's
  proposal says.
- **D10 — Ship mmap on Java 21?** `MappedByteBuffer` cannot be deterministically
  unmapped on 21 (§5.5): `close()` is invalidate-only, pages release at GC, and
  Windows keeps the file locked while mapped. Options: (a) ship with the
  documented caveat and the `maxMappedBytes` policy cap; (b) specify now (this
  document), implement when the project targets Java 22+ and `Arena` gives true
  unmap; (c) implement on 21 behind trusted-mode only (deny in untrusted
  regardless of capabilities). DECIDED: **(a)** 
- **D7 — `file_delete(force=true)` scope.** Recursive directory deletion is
  the most destructive primitive here. Keep it (guarded by `FILE_DELETE` +
  read-write-root confinement), or restrict deletion to files and empty
  directories only? DECIDED: keep it — the root is the safety boundary,
  not the feature set — but it deserves an explicit call-out in `EMBEDDING.md`.
- **D8 — Temp files** (`tmp_file()`/`tmp_dir()`): inherently *outside* the
  read-write roots, so they would need their own rule (e.g. host pre-creates a
  scratch dir inside a root). DECIDED: new capability for temp file use, read and 
  write together (there is no need to read only temp files).
