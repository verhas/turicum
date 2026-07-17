# Turicum `bin` Type — Design Proposal

*Status: Phase 1 implemented, 2026-07-16 — all open decisions of §12 are settled
and the type itself (§11 Phase 1) is in the code base with tests.
This document is the precursor of the binary half of the file I/O work: it resolves
the open decisions D6/D9 of `FILE_IO.md` by introducing a first-class byte-array
type instead of the interim options considered there.*

## 1. Why

Turicum has no byte-sequence type today. Everything that touches binary data is a
workaround:

- `TuriInputStream.read_all_bytes` returns a raw Java `byte[]`, which a script can
  do nothing with except pass back into reflection — useless in a sandbox that
  denies `JAVA_REFLECTION`.
- `str.bytes()` and `str.from_base64()` return a `lst` of numbers, one boxed
  `Long`-ish element per byte — workable, but memory-heavy and semantically wrong
  (a list of numbers is not a byte string; you cannot search it, slice it into a
  byte string, decode it, or hash it).
- `FILE_IO.md` §5.4/§5.5 (random access and mmap handles) are byte-oriented by
  nature and are explicitly blocked on "a bytes story" (decisions D6 and D9).

The `bin` type gives scripts a compact, indexable, sliceable byte sequence with
the operations one expects from a modern scripting language (Python `bytes`/
`bytearray`, Ruby packed strings), and gives the file I/O plan its return and
argument type for binary reads and writes.

## 2. What exists today (relevant pieces)

| Piece | Where | Relevance |
|---|---|---|
| `str` runtime value is a plain `java.lang.String` | everywhere | The model to copy: a native Java value *is* the language value, no wrapper |
| `TuriClass` SPI | `ch.turic.TuriClass`, registered in `module-info.java` + `META-INF/services` | Method dispatch on non-`LngObject` values; `TuriString` implements all `str` methods this way |
| TuriClass lookup | `GlobalContext.getTuriClass(Class)`, walk in `FunctionCallOrCurry.getTuriClass()` | Keyed by Java class; `byte[].class` is a perfectly good key, so `bin` methods dispatch exactly like `str` methods |
| Raw-value wrapping | `LeftValue.toObject()` default branch → `JavaObject` | A raw `byte[]` already flows through the same dispatch path as a raw `String` |
| Type names | `ch.turic.analyzer.Types`, `builtins.functions.Type`, `Variable.getTypeFromName()`, `utils.parameter.Declare` | The four places a new type name must be known |
| Indexing/slicing | `HasIndex`, `IndexedString`, `LeftValue.toIndexable()`/`toIterable()`, `ArrayElementLeftValue` | `IndexedString` wraps an *immutable* value, forcing the copy-back dance in `ArrayElementLeftValue`; a `byte[]` is mutable, so its wrapper writes in place |
| `len()` | `builtins.functions.Len` | **Already handles `byte[]`** — no change needed |
| Operators | `commands.operators.Add`, `Multiply`, `Compare`, `Contains` | String cases are pattern-matched explicitly; `bin` needs its own cases (`byte[]` has identity `equals` and is not `Comparable`) |
| Value printing | `Cast.toString()`, string interpolation, `str + x` in `Add` | All funnel through `Object.toString()`; `byte[].toString()` prints `[B@1a2b3c` — must be special-cased |
| Existing byte producers | `TuriInputStream.read_all_bytes`, `str.bytes()`, `str.from_base64()` | First one starts returning a useful value for free; the `lst`-returning methods switch to returning `bin` (D-4, breaking change accepted) |

## 3. Core decision: the runtime value is a raw Java `byte[]`

Exactly as `str` is a raw `java.lang.String`, a `bin` value is a raw `byte[]`.
No wrapper class (`LngBin` as a *value holder* does not exist; the name is reused
only for helper code if needed).

Why raw and not wrapped:

1. **Symmetry with `str`.** Method dispatch, `len()`, indexing, and printing all
   have an established pattern for raw Java values; `bin` rides the same rails.
2. **Free Java interop.** Any Java method invoked through reflection that returns
   `byte[]` — starting with the already-shipped `read_all_bytes` — instantly
   yields a first-class `bin` with all its methods, no conversion layer. Any Java
   method taking `byte[]` accepts a `bin` argument as-is.
3. **The file I/O handles get zero-copy semantics.** `file_read(@binary=true)`
   is `Files.readAllBytes()` returned directly.

Consequences to accept and document:

- **`bin` is mutable and fixed-length.** A Java array cannot grow; its elements
  can be assigned. So `b[0] = 255` writes in place (unlike `str`, where index
  assignment rebuilds the string), and two variables referring to the same `bin`
  see each other's writes — *aliasing is observable*. Growing operations
  (`+`, `*`, slicing) always produce a **new** `bin`. This is the
  Python-`bytearray` model and it is the right one for the binary-file use case
  (fill a buffer, patch a header, write it out) — an immutable variant would
  force a full copy per byte written.
- **Equality and ordering need operator work.** `byte[].equals()` is identity;
  `==` must use `Arrays.equals()` and ordering must use
  `Arrays.compareUnsigned()` (see §7).
- **Printing needs a `Cast.toString()` case** (see §9).

### Byte values are unsigned at the language surface

Java bytes are signed (−128..127); scripts think in 0..255. Every place a byte
crosses into the language it is presented **unsigned**: `b[i]` yields an `int`
in 0..255, iteration yields 0..255, `to_list()` yields 0..255, comparisons order
by unsigned value. Every place a number becomes a byte (`b[i] = x`, `bin(lst)`,
`fill`), values in −128..255 are accepted and stored as the low 8 bits; anything
outside that range is an error (silently masking a typo'd `1000` to `232` helps
nobody).

## 4. Type name and declarations

- `Types.BIN = "bin"` in `ch.turic.analyzer.Types`.
- `type(b)` returns `"bin"`: add `case byte[] ignore -> Types.BIN;` in
  `builtins.functions.Type` (before the generic `default`; note the existing
  `Long`/`Number` cases show the pattern-order sensitivity).
- `let b : bin = ...` works: `Variable.getTypeFromName()` gets
  `case Types.BIN -> new Variable.Type(byte[].class, null, new Identifier(name));`.
- Builtin parameter declarations: `Declare` gains a `bin` type constant next to
  `str`/`int`/`num`/`any`, so the fileio built-ins can declare
  `content: bin` parameters.

## 5. Construction

### 5.1 The `bin()` built-in function (`builtins.functions.Bin`)

Modeled on Python's `bytes()`/`bytearray()` constructor, dispatching on the
argument type:

```text
bin()                          -> empty bin
bin(n: int)                    -> n zero bytes                    (buffer allocation)
bin(l: lst)                    -> one byte per element; each element must be an
                                  integer in -128..255 (stored unsigned)
bin(s: str, @charset="UTF-8")  -> the string encoded to bytes
bin(b: bin)                    -> a copy (defensive-copy idiom, mirrors list())
```

No capability is required — `bin` is pure memory. (A malicious
`bin(2_000_000_000)` allocates 2 GB, but so does `"x" * 2_000_000_000` today;
resource capping is a sandbox-wide concern, not a `bin` concern.)

### 5.2 From strings: encoding and decoding pairs

The design principle: **`bin` → `str` directions live on `bin`; `str` → `bin`
directions live on `str`.** Each conversion has an inverse with a predictable
name:

| direction | method | inverse |
|---|---|---|
| encode text | `str.bytes(@charset="UTF-8") -> bin` (same job as `bin(s)`, method form) | `bin.text(@charset="UTF-8") -> str` |
| base64 | `bin.base64() -> str`, `bin.base64_url() -> str` | `str.from_base64() -> bin` |
| hex | `bin.hex() -> str` (lowercase, two digits per byte, no separator) | `str.from_hex() -> bin` (accepts optional `0x` prefix, even digit count required) |

`bin.text()` decoding of invalid byte sequences follows the Java default
(replacement character), matching what `new String(bytes, charset)` does; a
strict mode is not offered in phase 1.

`str.bytes()` and `str.from_base64()` are the **existing** methods with their
return type changed from `lst`-of-numbers to `bin` — a deliberate breaking
change (D-4, decided). Scripts that iterated the result or indexed single
bytes keep working shape-wise, with one value-level difference that is really
a fix: the old lists held *signed* Java bytes (0xC3 appeared as −61), the new
`bin` surface is uniformly unsigned (§3), so byte values ≥ 0x80 change from
negative to their 0..255 form. Code that used list-specific operations
(`append`, list `+`) adjusts or calls `to_list()`. `str.from_base64_str()` is
unaffected.

### 5.3 No literal syntax (D-2, decided)

No lexer change. A hex constant is written `"48656c6c6f".from_hex()` or
`bin([0x48, 0x65, ...])`. The `hex()`/`from_hex` pair is deliberately chosen so
that the *textual representation* (§9) round-trips through it. Note that the
lexer does already support a prefixed string form (`$"..."`,
`Lexer.java`/`Lex.string`), so a `bin"48656c"` literal would be technically
easy — the decision against it is deliberate, not a limitation: the method
spelling is explicit about the hex interpretation and keeps the token grammar
smaller. Nothing in this design blocks adding a literal later, and because no
literal exists, the `Marshaller`/`Unmarshaller` (compiled-code serialization)
needs no change.

## 6. Indexing, slicing, iteration — `IndexedBin`

New class `ch.turic.memory.IndexedBin implements HasIndex`, wired into
`LeftValue.toIndexable()` and `LeftValue.toIterable()` with
`case byte[] b -> new IndexedBin(b);`.

Semantics mirror `IndexedString` exactly, with bytes instead of one-character
strings:

- `b[i]` — `int` in 0..255; `i` must satisfy `0 <= i < len(b)` (same
  out-of-range error style as strings).
- `b[i..j]` — a **new** `bin` copy of the range; infinite and negative range
  endpoints behave as they do for `str` (`b[2..inf]`, `b[0..-1]`).
- `b[i] = x` — in-place write of the low 8 bits of `x` (range check per §3).
- `b[i..j] = other_bin` — replaces the range; because the replacement may have
  a different length, this produces a **new array**, so it needs the same
  copy-back treatment `ArrayElementLeftValue` already implements for
  `IndexedString` (pattern extended from `IndexedString(StringBuilder)` to
  `IndexedBin`). Single-index assignment, by contrast, is a true in-place write
  visible through aliases — implementation must ensure the copy-back path
  triggers **only** for the length-changing case, otherwise the aliasing
  semantics of §3 silently break.
- `for each x in b` — iterates the byte values as `int` 0..255.

## 7. Operators

Explicit cases added to the operator classes, following the existing
string-first pattern (`op1 instanceof ...`):

| operator | semantics |
|---|---|
| `bin + bin` | new concatenated `bin` (`Add`, case before the numeric fallback) |
| `bin + other` / `other + bin` (non-`str`) | error — no implicit coercion; `str + bin` keeps the generic string-concatenation rule and appends the textual form (§9) |
| `bin * int` | new repeated `bin` (`Multiply`, alongside the `CharSequence` case) |
| `bin == bin` / `!=` | content equality via `Arrays.equals()` (`Compare.Equal` gets a `byte[]`/`byte[]` case next to `numericEquality`) |
| `bin === bin` | identity — same array (works today, no change) |
| `bin < bin` etc. | lexicographic **unsigned** ordering via `Arrays.compareUnsigned()` (`Compare` comparator path) |
| `x in b` | `Contains`: `int in bin` — byte value present; `bin in bin` — contiguous subsequence present |
| boolean context | error, like `lst` — a `bin` is not a boolean; test `len(b) == 0` or use `is_empty()` |

Per-byte bitwise operators (`bin & bin`, `~bin`, shifts) are **not** proposed;
`xor(other)` exists as a method (§8) because it is the one genuinely common
operation (checksums, masking), and the rest can follow the same method pattern
later if demand appears. Rationale: the bitwise operators are defined on
integers today, and overloading them for unequal-length buffers raises
questions (pad? error?) that methods can answer with named options.

## 8. Methods — `TuriBin implements TuriClass`

New class `ch.turic.builtins.classes.TuriBin`, `forClass() -> byte[].class`,
registered in `module-info.java` and `META-INF/services/ch.turic.TuriClass`,
written in the exact style of `TuriString` (a `switch` over the identifier
returning `TuriMethod` instances, with `// snippet`-commented documentation for
`REFERENCE.adoc`).

Read `b` for the receiver below. "new bin" always means a fresh array.

### Conversion

| method | behavior |
|---|---|
| `to_string()` | the textual form of §9 (exists on every value) |
| `text(@charset="UTF-8")` | decode to `str` |
| `to_list()` | `lst` of `int` 0..255 |
| `base64()` / `base64_url()` | Base64 / URL-safe Base64 encoding as `str` |
| `hex()` | lowercase hex string, two digits per byte |
| `copy()` | new bin with the same content (explicit de-aliasing) |

### Digests

| method | behavior |
|---|---|
| `md5()`, `sha_1()`, `sha_256()`, `sha_512()`, `digest(name)` | hash of the bytes, returned as **`bin`** (chain `.hex()` for display) |

Note the deliberate difference from the `str` digest methods, which return a
formatted string (via `StringUtils.digest`): a digest *is* bytes, and returning
`bin` makes HMAC-style compositions (`key.xor(pad).sha_256()`) natural. The
`str` methods stay as they are.

### Search and tests

| method | behavior |
|---|---|
| `index_of(x, from?)` / `last_index_of(x, from?)` | position of a byte value (`int`) or subsequence (`bin`); −1 when absent — same contract as `str.index_of` |
| `contains(x)` | byte value or subsequence — same as the `in` operator |
| `starts_with(other)` / `ends_with(other)` | prefix/postfix test against a `bin` |
| `count(x)` | non-overlapping occurrences of a byte value or subsequence |
| `is_empty()` / `is_not_empty()` | length test |
| `length()` | convenience alias, like `str.length()`; the Turicum way is `len(b)` |

### Producing new buffers

| method | behavior |
|---|---|
| `left(n)` / `right(n)` | at most `n` leading/trailing bytes — same over-indexing tolerance as `str.left`/`str.right` |
| `reverse()` | new bin, bytes reversed |
| `xor(other)` | new bin, byte-wise XOR; lengths must be equal |

### In-place mutation (returns the receiver for chaining)

| method | behavior |
|---|---|
| `fill(value, from?, to?)` | set a range (default: all) to a byte value |
| `set(pos, src)` | copy the whole `bin` `src` into `b` starting at `pos`; must fit — this is the bulk in-place counterpart of `b[i] = x` (range-assignment `b[i..j] = src` re-allocates, §6) |

### Binary structure access (the file-I/O payoff)

The reason `bin` exists is parsing and building binary file content, so minimal
integer codec methods are part of phase 1, not an afterthought. The width is
part of the method name — cryptic, but this is a technical surface, and `u32_at`
reads like the wire-format specs these scripts will be implementing:

| method | behavior |
|---|---|
| `u8_at(pos)` / `i8_at(pos)` | read one byte, unsigned / two's-complement signed |
| `u16_at(pos, @order="be")` / `i16_at(pos, @order="be")` | read 2 bytes |
| `u32_at(pos, @order="be")` / `i32_at(pos, @order="be")` | read 4 bytes |
| `u64_at(pos, @order="be")` / `i64_at(pos, @order="be")` | read 8 bytes; a `u64` value above the signed-long range is an error (Turicum `int` is a signed 64-bit long) |
| `set_u8_at(pos, value)` … `set_u64_at(pos, value, @order="be")` | in-place write; `value` must be in 0..2^width−1 (for `set_u64_at`: non-negative) |
| `set_i8_at(pos, value)` … `set_i64_at(pos, value, @order="be")` | in-place write; `value` must be in −2^(width−1)..2^(width−1)−1 |

**`@order`** accepts three forms (the 8-bit methods take no `@order`):

- `"be"` — big endian, most significant byte first;
- `"le"` — little endian, least significant byte first;
- a **permutation string**: one digit per byte of the width, each of `1`..`N`
  (`N` = width/8) appearing exactly once, listing the bytes in memory order
  from `pos`, where digit `1` is the *least* significant byte. So for 32-bit
  values `"1234"` ≡ `"le"` and `"4321"` ≡ `"be"`, and any other permutation is
  legal — e.g. `"3412"` is the PDP-11 middle-endian layout. A string of the
  wrong length, or with a repeated or out-of-range digit, is an error.

Float codecs, varints, and a Python-`struct`-style format-string `pack`/`unpack`
(name clash with the existing `pack()` built-in — would need another name
anyway) are explicitly deferred; the methods above cover magic numbers, lengths,
offsets, and checksums, which is what binary file handling needs first.

## 9. Textual representation

`Cast.toString()` (and therefore string interpolation, `str(b)`, `println`)
renders a `bin` as:

```text
bin"48656c6c6f"        # 5 bytes
bin""                  # empty
```

i.e. `bin"` + `hex()` + `"`. Properties: compact (2 chars/byte), lossless,
unambiguous, and forward-compatible — if D-2 later introduces a literal, this
exact form is the natural literal syntax, making `str(b)` output valid source
code (the same round-trip property `str.quote()` provides for strings).

Implementation: a `byte[]` case in `Cast.toString()` covers `str + bin` (the
`Add` string case already calls `Cast.toString`). Implementation revealed three
more printing paths that append raw `Object.toString()` and had to be touched:
the `str()` built-in (`Str.java`), the `print`/`println` command (`Print.java`),
and string interpolation (`StringConstant._execute`) — plus `LngList.toString()`
so a bin inside a printed list renders correctly. All now route through
`Cast.toString()`. Error messages that embed values via `%s` formatting go
through their own paths; they will show `[B@...` until touched, which is
cosmetic and not worth a sweep in phase 1.

`jsonify()` of a `bin` is an **error** with a helpful message ("convert
explicitly with .base64(), .hex() or .to_list()") — D-3, decided. JSON has no
byte type; silently choosing base64 or a number list would bake a lossy,
asymmetric convention into the JSON layer. The script author knows which
representation the receiving side expects.

## 10. Interplay with existing code

- **`len(b)`** — already works (`Len` has a `byte[]` case).
- **`TuriInputStream.read_all_bytes`** — no code change; its return value
  simply *becomes* a useful `bin` the moment `TuriBin` is registered. Same for
  any reflection call returning `byte[]`.
- **`str.bytes()` / `str.from_base64()`** — return type changes from `lst` to
  `bin` (D-4, breaking change accepted; details and the signed-to-unsigned
  value fix in §5.2). Their `REFERENCE.adoc` snippets must be updated in the
  same commit.
- **`FILE_IO.md` alignment** — this document resolves D6 as option (d) and D9
  as option (a): `file_read(@binary=true)` returns `bin`; `file_write` accepts
  `bin` content in binary mode; the §5.3 binary reader's `read(n)`/`read_all()`
  return `bin`; the §5.4 random-access handles' `read`/`read_at` return `bin`
  and `write`/`write_at` accept `bin` (whether they *also* accept `str` is a
  file-I/O API question, deferred to `FILE_IO.md` D11 — see D-6); the §5.5
  mapped handles' `get`/`put` likewise. The latin-1-string workaround (D9(b))
  is dropped.
- **Sandboxing** — no capability, no policy surface. `bin` neither reads nor
  writes anything outside interpreter memory.
- **Threading** — a `bin` is mutable shared state like `lst`; scripts that
  share one across tasks synchronize with the existing `mtx` machinery. No
  internal locking (same stance as `lst`).

## 11. Implementation inventory

```
core/src/main/java/ch/turic/
    analyzer/Types.java                       + BIN = "bin"
    builtins/functions/Type.java              + case byte[]
    builtins/functions/Bin.java               NEW  built-in constructor (§5.1)
    builtins/classes/TuriBin.java             NEW  methods (§8), snippet docs
    builtins/classes/TuriString.java          "bytes"/"from_base64" return bin (breaking, D-4); + "from_hex"
    memory/IndexedBin.java                    NEW  HasIndex impl (§6)
    memory/LeftValue.java                     + byte[] cases in toIndexable/toIterable
    memory/ArrayElementLeftValue.java         + copy-back for range-assignment (§6)
    memory/Variable.java                      + case Types.BIN
    utils/parameter/Declare.java              + bin parameter type
    commands/operators/Add.java               + bin + bin
    commands/operators/Multiply.java          + bin * int
    commands/operators/Compare.java           + content equality, unsigned ordering
    commands/operators/Contains.java          + int/bin in bin
    commands/operators/Cast.java              + toString case (§9)
    builtins/functions/Str.java               str() routed through Cast.toString (§9)
    commands/Print.java                       print/println byte[] case (§9)
    commands/StringConstant.java              interpolation routed through Cast.toString (§9)
    memory/LngList.java                       toString routed through Cast.toString (§9)
    utils/BinUtils.java                       NEW  hex/display/byte coercion/search/codec helpers
    builtins/functions/Jsonify*.java          + explicit error for byte[] (§9)
    module-info.java                          + Bin (TuriFunction), TuriBin (TuriClass)
    META-INF/services/…TuriFunction, …TuriClass
```

Plus `REFERENCE.adoc.jam` snippet references for the new chapter (regenerate
with Jamal, `--open='{%' --close='%}'`).

### Test plan

- Constructor dispatch (`bin()`, int, lst incl. range errors, str+charset, copy).
- Unsigned surface: `b[i]` on bytes ≥ 0x80, iteration, `to_list`, ordering of
  `bin([0x80]) > bin([0x7f])`.
- Mutation and aliasing: `b[i] = x` visible through a second reference;
  `copy()` de-aliases; range-assignment re-allocation does **not** alias.
- Operators: concat, repeat, `==` content vs `===` identity, `in`, boolean
  context error.
- Round-trips: `text`/`bytes`, `hex`/`from_hex`, `base64`/`from_base64`,
  `str(b)` equals `'bin"' + b.hex() + '"'`.
- Migration of D-4: `str.bytes()` and `str.from_base64()` return `bin`;
  existing tests asserting `lst` results are updated, including the
  signed-to-unsigned value change for bytes ≥ 0x80.
- Codecs: `u8_at`…`u64_at`, `i8_at`…`i64_at` and their setters for every
  width; `"be"`/`"le"` and permutation orders (round-trip `"3412"`), rejected
  malformed orders (wrong length, repeated digit, digit out of range); sign
  edge cases (0x80.., `u64` overflow error, setter range validation).
- Interop: reflection method returning `byte[]` answers `type()` as `"bin"`
  and accepts method calls; `read_all_bytes` end-to-end.
- `len()`, `let b : bin` type check, `bin` parameter declaration in a
  user-defined function.

### Phasing

1. **Phase 1 — the type.** Everything in this document except the deferred
   items. Self-contained; no dependency on the file I/O work.
2. **Phase 2 — file I/O binary mode.** The `FILE_IO.md` built-ins adopt `bin`
   per §10; unblocks §5.4 random access.
3. **Deferred.** Float/varint/struct codecs, bitwise operators. (A literal
   syntax and a growable buffer were considered and decided against — D-2,
   D-5.)

## 12. Decisions (settled 2026-07-16)

- **D-1 — Name of the decode method — DECIDED: `text(@charset)`.** `decode`
  invites "decode from what?" confusion with base64/hex, and a method named
  `str` shadows the built-in function name in readers' minds. The encoding
  partner is the existing `str.bytes(@charset)` (see D-4).
- **D-2 — Literal syntax `bin"48656c"` — DECIDED: no literal.** Turicum's
  lexer does support prefixed strings (`$"..."`), so this is a design choice,
  not a technical constraint (§5.3). The §9 textual representation keeps the
  form available should the decision ever be revisited.
- **D-3 — `jsonify(bin)` — DECIDED: error** with guidance to convert
  explicitly (`.base64()`, `.hex()`, `.to_list()`) — §9.
- **D-4 — `str.bytes()` and `str.from_base64()` — DECIDED: change them to
  return `bin`,** accepting the compatibility break (§5.2). No parallel
  `str.bin()`/`from_base64_bin()` methods are introduced; the established
  names simply gain the right return type, and byte values ≥ 0x80 switch from
  signed to unsigned presentation as part of the same break.
- **D-5 — Growable buffer — DECIDED: no; `bin` stays fixed-length** with
  copy-on-grow operations. `lst` accumulation + `bin(l)` covers construction
  loops, and the file-writer handles stream anyway.
- **D-6 — Whether the random-access `write` accepts `str` — moved out:** this
  is a file-I/O API question and belongs to `FILE_IO.md` (tracked there as
  D11), not to this document. This design only provides the `bin` type the
  handles will use.
