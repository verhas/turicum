# Turicum Embedding & Sandboxing API — Design Notes

*Status: proposal / findings, 2026-07-10*

This document summarizes an analysis of the `core` module and proposes an embedding API
that lets a Java (enterprise) application run Turicum scripts sandboxed, with limits on
execution time, memory, CPU use, Java call-outs, and other resources.

`REFERENCE.adoc` already announces: *"Richer embedding and sandboxing APIs, including
limiting a program's access to the JDK and system resources, are under active
development."* This design fills in that promise.

---

## 1. What already exists

The codebase is halfway there and, importantly, has clean chokepoints.

| Mechanism | Where | Notes |
|---|---|---|
| Global step limit | `GlobalContext(stepLimit, graceSteps)` | Checked on every command |
| Per-thread step limit | `ThreadContext.setStepLimit()` | Independent of the global limit; used by the `steps` option of `async` |
| Step chokepoint | `LocalContext.step()` (`LocalContext.java:810`) | Calls both global and thread `step()`; every command executes it |
| Uncatchable halts | `InterpreterHalt` ← `StepLimitReached`, `ExecutionAborted` | Script-level `try`/`catch` cannot swallow them; converted to `ExecutionException` at the `Interpreter.execute` boundary |
| Bounded cleanup after halt | `Grace` (`memory/Grace.java`) | `finally`/`with` exit blocks get a bounded step allowance after a halt; cannot run forever |
| Cooperative abort | `ThreadContext.abort()` | Sets a flag checked at every command start **and** interrupts the thread (unblocks I/O and channel waits) — exactly the primitive a watchdog needs |
| Thread registry & join | `GlobalContext.contexts`, `joinThreads()` | Already aborts and joins all child threads at the end of `Interpreter.execute` |
| Per-interpreter class loader | `GlobalContext.classLoader` (`TuricumClassLoader`) | Nearly **all** reflection funnels through it: `java_class`, `java_call`, `java_object`, `is_type`, and type annotations in `Variable` resolve classes via `ctx.globalContext.classLoader.loadClass(...)` |
| Pluggable builtins | ServiceLoader SPI (`TuriFunction`, `TuriMacro`, `TuriClass`), registered per context in `BuiltIns.register()` | A natural place for capability filtering |
| Host → script data | `Interpreter.compileAndExecute(Map<String,Object>)` | Injects variables as **frozen** globals |
| Compile once / run many | `Program` is immutable; `Marshaller`/`Unmarshaller` serialize to `.turc` | Enables precompiled, reusable programs and even out-of-process execution |

## 2. Gaps

1. **`Interpreter` never exposes the limits.** `Interpreter.compile()` does
   `ctx = new LocalContext()` — unlimited steps, no grace. An embedder cannot set any
   limit without reaching into the internal `ch.turic.memory` package.
2. **No wall-clock timeout.** Steps are a poor proxy for time: `sleep()`, blocked
   channel reads, and slow Java call-outs consume essentially no steps.
3. **No memory accounting** of any kind. A script can OOM the host JVM with one
   growing list or string.
4. **No CPU throttling.**
5. **Java access is all-or-nothing.** With `java_class` registered, a script can reach
   `java.lang.Runtime`, `ProcessBuilder`, `java.nio.file.Files` — anything. It could
   even load `ch.turic.memory.GlobalContext` and lift its own limits.
6. **Side-effecting builtins are always on:** `env`, `glob`, `import`/`sys_import`
   (filesystem reads), `http_client`, `http_server`; `print` writes directly to
   `System.out` (`Print.java:41`).
7. **Unbounded thread spawning.** `AsyncEvaluation` uses one **static, JVM-wide**
   `Executors.newVirtualThreadPerTaskExecutor()` (`AsyncEvaluation.java:13`): no
   per-interpreter cap, no isolation between interpreters, no deterministic shutdown.
8. **No recursion-depth limit** — deep script recursion kills the host thread with a
   raw `StackOverflowError`.

## 3. Proposed public API

One new public package `ch.turic.embed` containing a policy object and a session
facade. Everything else stays internal.

```java
var policy = SandboxPolicy.builder()
        // ---- time ----
        .stepLimit(10_000_000)              // exists today, just not exposed
        .graceSteps(10_000)                 // exists today
        .timeout(Duration.ofSeconds(5))     // NEW: wall-clock watchdog -> abort()
        // ---- CPU ----
        .cpuThrottle(0.25)                  // NEW: max fraction of one core
        // ---- memory (approximate; see §4.3) ----
        .maxCollectionSize(1_000_000)       // NEW: cap on a single list/object/string
        .maxAllocatedCells(50_000_000)      // NEW: global allocation counter
        // ---- concurrency ----
        .maxThreads(8)                      // NEW: per-engine bounded executor
        .maxStackDepth(2_000)               // NEW: cap ThreadContext trace depth
        // ---- capabilities (sandbox default: all OFF) ----
        .allow(Capability.JAVA_REFLECTION)  // gates java_class / java_call / ...
        .javaClassFilter(name ->            // consulted even when reflection is on
                name.startsWith("com.mycorp.scripting.api."))
        .allow(Capability.FILE_READ)        // gates import, glob, sys_import
        .importRoot(Path.of("/opt/scripts"))// imports resolved only under this root
        // NETWORK, ENV, PROCESS, FILE_WRITE remain denied
        // ---- I/O redirection ----
        .stdout(myWriter).stderr(myWriter).stdin(myReader)
        .build();

try (TuriEngine engine = TuriEngine.create(policy)) {
    TuriProgram program = engine.compile(source);       // reusable, thread-safe
    try (TuriSession session = engine.newSession()) {
        session.set("request", requestData);            // frozen injected global
        Object result = session.eval(program);
        // throws TuriTimeoutException, TuriResourceException, BadSyntax, ...
    }
}
```

Design points:

- **`TuriEngine`** — holds the `SandboxPolicy`, the bounded executor, the watchdog
  scheduler, and the compile cache. Compile once, evaluate many times
  (`Program` is already immutable and serializable).
- **`TuriSession`** — wraps the lifetime of one `LocalContext`
  (`AutoCloseable`, like `Interpreter` today). Offers `set()` for frozen injected
  globals and `eval()` / `evalAsync()` (returning `CompletableFuture`) so the host
  never has to block on a runaway script.
- **Typed exceptions** — `TuriTimeoutException`, `TuriResourceException` (step,
  memory, thread, stack limits), distinguishable from ordinary script errors
  (`ExecutionException`) and syntax errors (`BadSyntax`).
- The existing `Interpreter` class stays as-is for CLI/tools usage; `TuriEngine`
  becomes the documented entry point for embedders.

## 4. How each limitation maps to the code

### 4.1 Time to execute (cheapest, highest value)

- Add a `deadline` (nanoseconds) to `GlobalContext`.
- A single shared daemon `ScheduledExecutorService` in `TuriEngine` schedules one task
  per evaluation that calls a new `GlobalContext.abortAll()` — iterate the existing
  `contexts` registry and call `ThreadContext.abort()` on each.
- Everything downstream already works: unwinding via `ExecutionAborted`, bounded
  `finally` cleanup via `Grace`, thread joining via `joinThreads()`.
- Belt-and-braces: check the deadline inside `LocalContext.step()` every ~1024 steps
  (counter mask — one amortized branch) in case the watchdog thread is delayed.

### 4.2 CPU percentage

- Piggyback on the same step chokepoint: a token bucket in `GlobalContext`.
- Every N steps compare elapsed wall time against the budget implied by
  `cpuThrottle`; if ahead of budget, `LockSupport.parkNanos(...)`.
- Limitation to document: this throttles interpreter work, not time spent inside a
  single Java call-out.
- Do **not** attempt `ThreadMXBean` per-thread CPU time — it is unsupported for
  virtual threads, which is what `AsyncEvaluation` uses.

### 4.3 Max memory (approximate accounting — be honest about it)

Hard in-process memory isolation does not exist on the JVM (the SecurityManager is
gone, JEP 411). What enterprise embedders actually need is to stop the
one-growing-list script from OOMing the host. Offer:

- An `AtomicLong` allocation counter on `GlobalContext`, incremented with rough size
  weights at the few allocation sites: `LngList` growth, `LngObject.setField`,
  string concatenation/interpolation, channel buffering.
- Per-object caps (`maxCollectionSize`, max string length) enforced at the same sites.
- A new `MemoryLimitReached extends InterpreterHalt` so it is uncatchable in-script
  and gets the same `Grace` cleanup treatment.
- Documentation: a *hard* guarantee requires process isolation — a separate JVM with
  `-Xmx` (the `.turc` serialization makes a subprocess runner module easy to add
  later) or a container.

### 4.4 Java call-out restriction (two layers)

**Layer 1 — capability-gated builtins.**
Add `default Set<Capability> capabilities() { return Set.of(); }` to the
`TuriFunction`/`TuriMacro`/`TuriClass` SPI (via `ServiceLoaded` or a sibling
interface) and tag the dangerous ones:

| Capability | Builtins |
|---|---|
| `JAVA_REFLECTION` | `java_class`, `java_call`, `java_object`, `java_import`, `java_callback`, `add_java_classes`, `java_resources`, `java_type`, `as_object` |
| `FILE_READ` | `import`, `sys_import`, `glob`, `source_directory`, `TuriInputStream`/`Reader` classes |
| `ENV` | `env` |
| `NETWORK` | `http_client`, `http_server` |

`BuiltIns.register(ctx, policy)` skips builtins whose capabilities are not granted;
the script then gets an ordinary "undefined symbol" error.

**Layer 2 — class filter at the loader.**
Even with reflection granted, give `TuricumClassLoader` a `Predicate<String>` filter;
`loadClass`/`findClass` throw `ClassNotFoundException` for denied names. Because all
class resolution funnels through `globalContext.classLoader`, this single change
covers `java_class`, `java_call`, `java_object`, `is_type`, and `Variable` type
annotations. Apply a built-in deny-list even in permissive mode:

- `java.lang.Runtime`, `java.lang.ProcessBuilder`, `java.lang.System` (partially)
- `java.lang.reflect.*`, `java.lang.invoke.*`
- `sun.*`, `jdk.internal.*`
- `ch.turic.*` internals — otherwise a script can load `GlobalContext` and lift its
  own limits.

### 4.5 Other reasonable limits

- **Thread cap.** Move the executor from the static field in `AsyncEvaluation` to
  `GlobalContext`: a virtual-thread executor guarded by `Semaphore(maxThreads)`;
  failure to acquire throws `ExecutionException`. Also fixes the current
  cross-interpreter sharing and gives `TuriEngine.close()` deterministic shutdown.
- **Stack depth.** `ThreadContext.push()` throws an uncatchable halt past
  `maxStackDepth` instead of letting a `StackOverflowError` hit the host thread.
- **I/O redirection.** Give `GlobalContext` `out`/`err`/`in` streams (defaulting to
  the `System` streams); `Print` and the input-stream builtins use them. Needed for
  any server-side embedding regardless of sandboxing.
- **Import root.** `Import` currently calls `Files.readString` on a resolved path;
  resolve against `policy.importRoot()`, reject `..` escapes, deny entirely without
  `FILE_READ`.

## 5. Explicit non-goals

- **SecurityManager** — removed from the JDK; not an option.
- **Exact CPU-% enforcement** — the token bucket is a throttle, not an OS scheduler
  guarantee; Java call-outs are not throttled mid-call.
- **Hard memory isolation in-process** — accounting is approximate; hard limits need
  a subprocess/container (possible later module using `.turc` + `-Xmx`).

## 6. Suggested phasing

1. **Phase 1 — plumbing (unblocks embedders immediately).** *(implemented 2026-07-10)*
   `SandboxPolicy` + `TuriEngine`/`TuriSession` exposing what already exists
   (step limit, grace steps, injected globals) **plus** the wall-clock timeout,
   the per-engine thread cap, and I/O redirection.
   Implementation notes: new package `ch.turic.embed` (`SandboxPolicy`, `TuriEngine`,
   `TuriProgram`, `TuriSession`, `TuriTimeoutException`); `GlobalContext` gained
   `out()`/`err()` redirection, a per-context `executor()` (default: JVM-shared
   virtual-thread executor), a `Semaphore`-based thread-permit pool shareable across
   sessions for an engine-wide cap, and `abortAll()`; `LocalContext(GlobalContext)`
   root constructor added; `AsyncEvaluation` and `FlowCommand` now use the
   per-context executor and acquire/release thread permits; `Print` writes to
   `globalContext.out()`. Tests: `core/src/test/java/ch/turic/embed/TestTuriEngine.java`.
2. **Phase 2 — capabilities.**
   Capability-gated builtin registration, the class filter on `TuricumClassLoader`
   with the default deny-list, and the import root.
3. **Phase 3 — resource accounting.**
   Memory accounting and per-object caps, the CPU token bucket, and the stack-depth
   limit.
