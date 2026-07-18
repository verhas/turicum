# 1. Embedding Turicum in a Java Application

<!-- All Java examples in this document are working unit tests, included verbatim from
     core/src/test/java/ch/turic/embed/ by mdship. Do not edit the code blocks here;
     edit the tests and run: mdship update EMBEDDING.md -->

Turicum is both a scripting language and an embeddable engine.
This document describes the `ch.turic.embed` API: how to run Turicum scripts from Java,
exchange data with them, and — most importantly for enterprise use — how to sandbox them so
that a script cannot run forever, spawn unbounded threads, or write to your process output.

Every code example below is a working unit test from
`core/src/test/java/ch/turic/embed/`, included verbatim, so the documentation cannot drift
from the behavior of the released code.

<!--TOC min-level: 2
max-level: 3
_content_generated_: 1834:md5:9ecf08ed2aec7d7f3b5cc3313824e214
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
- [1.1. The interface at a glance](#11-the-interface-at-a-glance)
- [1.2. Getting started](#12-getting-started)
  - [1.2.1. Hello, world](#121-hello-world)
  - [1.2.2. Result types](#122-result-types)
- [1.3. Exchanging data with the script](#13-exchanging-data-with-the-script)
  - [1.3.1. Passing values in](#131-passing-values-in)
  - [1.3.2. Reading values out](#132-reading-values-out)
  - [1.3.3. Sessions keep state](#133-sessions-keep-state)
  - [1.3.4. Capturing the script's output](#134-capturing-the-scripts-output)
- [1.4. Sandboxing](#14-sandboxing)
  - [1.4.1. Limiting execution: the step limit](#141-limiting-execution-the-step-limit)
  - [1.4.2. Letting cleanup code run: grace steps](#142-letting-cleanup-code-run-grace-steps)
  - [1.4.3. Limiting time: the wall-clock timeout](#143-limiting-time-the-wall-clock-timeout)
  - [1.4.4. Limiting concurrency: the thread cap](#144-limiting-concurrency-the-thread-cap)
  - [1.4.5. Metering](#145-metering)
  - [1.4.6. Capabilities: what a script may reach](#146-capabilities-what-a-script-may-reach)
  - [1.4.7. The class-access filter and the deny floor](#147-the-class-access-filter-and-the-deny-floor)
  - [1.4.8. Scoping imports](#148-scoping-imports)
  - [1.4.9. Scoping data-file access](#149-scoping-data-file-access)
- [1.5. Scaling up](#15-scaling-up)
  - [1.5.1. Compile once, run many times](#151-compile-once-run-many-times)
  - [1.5.2. Sessions are isolated](#152-sessions-are-isolated)
  - [1.5.3. Storing precompiled programs](#153-storing-precompiled-programs)
  - [1.5.4. Concurrency inside the script](#154-concurrency-inside-the-script)
- [1.6. Error handling](#16-error-handling)
- [1.7. Lifecycle and thread-safety rules](#17-lifecycle-and-thread-safety-rules)
- [1.8. What the sandbox does not limit (yet)](#18-what-the-sandbox-does-not-limit-yet)
<!--/TOC-->

## 1.1. The interface at a glance

The API consists of four public classes and one exception in the package `ch.turic.embed`:

| Class | Role                                                                                                                                                               |
|---|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SandboxPolicy` | Immutable set of limits, capability grants, and redirections. Built with `SandboxPolicy.trusted()` or `.untrusted()`; `SandboxPolicy.UNRESTRICTED` applies nothing.  |
| `TuriEngine` | The entry point. Pairs a policy with the shared resources (executor, thread permits, watchdog). Compiles programs, creates sessions. Thread-safe. `AutoCloseable`. |
| `TuriProgram` | A compiled, immutable program. Holds no execution state; evaluate it any number of times, in any session. Can be serialized to the binary `.turc` format.          |
| `TuriSession` | One isolated interpreter instance with its own global variables and limits. **Not** thread-safe — use each session from a single thread. `AutoCloseable`.          |
| `TuriTimeoutException` | Thrown when the wall-clock timeout fires. Extends `ExecutionException`.                                                                                            |

The typical lifecycle is: create one engine per application (or per tenant class), compile
programs once, and create one cheap, short-lived session per evaluation:

```
SandboxPolicy  ──▶  TuriEngine  ──▶  TuriProgram   (compile once)
                        │
                        └─────────▶  TuriSession   (one per evaluation, isolated)
```

To use the API, add the `core` module to your dependencies:

```xml
<dependency>
    <groupId>ch.turic</groupId>
    <artifactId>core</artifactId>
    <version>2.0.0</version>
</dependency>
```

## 1.2. Getting started

### 1.2.1. Hello, world

Create an engine, open a session, evaluate a script. The result of the evaluation is the
value of the last expression. Both the engine and the session are `AutoCloseable`:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingBasics.java"
prefix: "```java"
postfix: "```"
start: "// snippet hello_world"
end: "// end snippet"
margin: 0
_content_generated_: 190:md5:a648608765fba097b1169f481357dafb
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
try (final var engine = TuriEngine.create();
     final var session = engine.newSession()) {
    final var result = session.eval("2 + 2 * 20");
    assertEquals(42L, result);
}
```
<!--/INCLUDE-->

### 1.2.2. Result types

Evaluation results arrive as plain Java values. Turicum integers are `Long`, floats are
`Double`, strings are `String`, booleans are `Boolean`, and `none` is `null`. Lists and
objects arrive as `LngList` (its elements in the public `array` field) and `LngObject`
(fields readable with `getField`):

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingBasics.java"
prefix: "```java"
postfix: "```"
start: "// snippet result_types"
end: "// end snippet"
margin: 0
_content_generated_: 626:md5:f91c05763e92088fbb571bb261d0c2aa
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
assertEquals(42L, session.eval("42"));                  // integers are Long
assertEquals(3.14, session.eval("3.14"));               // floats are Double
assertEquals("ab", session.eval("\"a\" + \"b\""));      // strings are String
assertEquals(true, session.eval("1 < 2"));              // conditions are Boolean
assertNull(session.eval("none"));                       // none is null

final var list = (LngList) session.eval("[1, 2, 3]");
assertEquals(List.of(1L, 2L, 3L), list.array);

final var obj = (LngObject) session.eval("{name: \"turicum\", version: 1}");
assertEquals("turicum", obj.getField("name"));
```
<!--/INCLUDE-->

## 1.3. Exchanging data with the script

### 1.3.1. Passing values in

`TuriSession.set(name, value)` injects a value as a global variable before evaluation:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingBasics.java"
prefix: "```java"
postfix: "```"
start: "// snippet inject_variables"
end: "// end snippet"
margin: 0
_content_generated_: 388:md5:474a85990d060c32d744183ba724716c
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
try (final var engine = TuriEngine.create();
     final var session = engine.newSession()) {
    session.set("greeting", "Hello");
    session.set("count", 3);
    assertEquals("Hello Hello Hello ", session.eval("""
            mut text = ""
            for i = 0 ; i < count ; i++ {
                text += greeting + " "
            }
            text
            """));
}
```
<!--/INCLUDE-->

Injected variables are frozen: the script can read them but cannot reassign them, so a
script cannot corrupt the data contract with its host:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingBasics.java"
prefix: "```java"
postfix: "```"
start: "// snippet frozen_variables"
end: "// end snippet"
margin: 0
_content_generated_: 286:md5:2e88bd8ed9877d202fa4bb19b070447a
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
try (final var engine = TuriEngine.create();
     final var session = engine.newSession()) {
    session.set("limit", 100);
    // the script can read the injected value, but cannot reassign it
    assertThrows(ExecutionException.class, () -> session.eval("limit = 0"));
}
```
<!--/INCLUDE-->

### 1.3.2. Reading values out

Besides the return value of `eval`, the host can read any global variable the script has
defined using `TuriSession.get(name)`:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingBasics.java"
prefix: "```java"
postfix: "```"
start: "// snippet read_globals"
end: "// end snippet"
margin: 0
_content_generated_: 313:md5:946382f9a2987ad6af5a29d4d7c27bfa
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
try (final var engine = TuriEngine.create();
     final var session = engine.newSession()) {
    session.eval("""
            global answer = 6 * 7
            global name = "turicum"
            """);
    assertEquals(42L, session.get("answer"));
    assertEquals("turicum", session.get("name"));
}
```
<!--/INCLUDE-->

### 1.3.3. Sessions keep state

Global variables persist between `eval` calls of the same session, so a host can drive a
script in several steps:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingBasics.java"
prefix: "```java"
postfix: "```"
start: "// snippet session_state"
end: "// end snippet"
margin: 0
_content_generated_: 280:md5:0e8829fad5182e6dac72936d5e1963c6
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
try (final var engine = TuriEngine.create();
     final var session = engine.newSession()) {
    session.eval("global counter = 0");
    session.eval("counter = counter + 1");
    session.eval("counter = counter + 1");
    assertEquals(2L, session.get("counter"));
}
```
<!--/INCLUDE-->

### 1.3.4. Capturing the script's output

By default, `print` and `println` write to `System.out`. A server application almost always
wants to redirect that — per engine, via the policy:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingBasics.java"
prefix: "```java"
postfix: "```"
start: "// snippet capture_output"
end: "// end snippet"
margin: 0
_content_generated_: 390:md5:2a39cdf51a1ce4cbe77b32796f48ab69
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var captured = new ByteArrayOutputStream();
final var policy = SandboxPolicy.trusted()
        .stdout(captured)
        .build();
try (final var engine = TuriEngine.create(policy);
     final var session = engine.newSession()) {
    session.eval("println \"hello from the script\"");
}
assertEquals("hello from the script\n", captured.toString(StandardCharsets.UTF_8));
```
<!--/INCLUDE-->

## 1.4. Sandboxing

Everything above used `TuriEngine.create()`, which applies no limits. For untrusted or
merely unpredictable scripts, build a `SandboxPolicy` and create the engine with it.

A policy is built through one of two entry points that declare, in the first line, how much
you trust the *script* — a distinction that decides the defaults for everything that
follows:

- **`SandboxPolicy.trusted()`** — for in-house scripts, build steps, configuration. Every
  capability is granted; the `deny…` methods trim. Guardrails, not a security boundary.
- **`SandboxPolicy.untrusted()`** — for user-supplied or multi-tenant scripts. Nothing is
  granted; the `allow…` methods add. A security posture.

The two return different builder types, so `allow…` and `deny…` cannot be mixed by accident:
calling the wrong one is a compile-time error. `SandboxPolicy.UNRESTRICTED` is a third option
— no gating at all, exactly the plain interpreter — and is the default of
`TuriEngine.create()`.

The **resource limits** below are mode-independent: they read the same and behave the same in
either mode, and all are optional.

| Limit | Builder method | What it bounds                                          |
|---|---|---------------------------------------------------------|
| Step limit | `stepLimit(int)` | Total interpreter steps (commands executed) per session |
| Cleanup grace | `graceSteps(int)` | Extra steps `finally`/exit blocks may run after a halt  |
| Timeout | `timeout(Duration)` | Wall-clock time of one `eval` call                      |
| Thread cap | `maxThreads(int)` | Concurrently running interpreter threads per engine     |
| Output | `stdout(...)`, `stderr(...)` | Where `print`/`println` and errors go                   |

What a script may *reach* — Java classes, the file system, the environment, the network — is
governed separately by **capabilities** and the **class-access filter**, covered in §1.4.6
onward. The examples in the limit sections below use `trusted()` because they are about
bounding a script's runtime, not restricting its reach.

### 1.4.1. Limiting execution: the step limit

The step limit bounds the total work a session may do, independent of how fast the host
machine is. When the limit is reached, `eval` throws an `ExecutionException` whose cause is
`StepLimitReached`:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingLimits.java"
prefix: "```java"
postfix: "```"
start: "// snippet step_limit"
end: "// end snippet"
margin: 0
_content_generated_: 550:md5:d1b1be276792afd9a806d132d4ebe0fa
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var policy = SandboxPolicy.trusted()
        .stepLimit(10_000)
        .build();
try (final var engine = TuriEngine.create(policy);
     final var session = engine.newSession()) {
    final var e = assertThrows(ExecutionException.class, () -> session.eval("""
            global mut n = 0
            while true {
                n = n + 1
            }
            """));
    assertInstanceOf(StepLimitReached.class, e.getCause());
    final var n = (long)session.get("n");
    Assertions.assertTrue(10_000 > n && n > 1_000 );
}
```
<!--/INCLUDE-->

The halt is enforced by the interpreter, not by the script's cooperation — a script cannot
catch it, not even with a `try`/`catch` around the whole program:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingLimits.java"
prefix: "```java"
postfix: "```"
start: "// snippet uncatchable_halt"
end: "// end snippet"
margin: 0
_content_generated_: 637:md5:aa683f0261d87d4b95c035fd2582f882
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var policy = SandboxPolicy.trusted()
        .stepLimit(10_000)
        .build();
try (final var engine = TuriEngine.create(policy);
     final var session = engine.newSession()) {
    // the script wraps the runaway loop into try/catch; the halt fires anyway,
    // because a halt is not a Turicum exception and cannot be swallowed in-script
    assertThrows(ExecutionException.class, () -> session.eval("""
            try {
                mut n = 0
                while true {
                    n = n + 1
                }
            } catch e {
                "swallowed"
            }
            """));
}
```
<!--/INCLUDE-->

The limit caps the *total* steps of the session, deliberately: a script must not be able to
earn itself a fresh budget by returning and being evaluated again. When the embedder — who
is outside the sandbox — wants per-evaluation budgets instead, it calls
`TuriSession.resetSteps()` between evaluations, which also returns the steps consumed since
the last reset:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingLimits.java"
prefix: "```java"
postfix: "```"
start: "// snippet reset_steps"
end: "// end snippet"
margin: 0
_content_generated_: 853:md5:632412542da30801de56a43d8d7f3557
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var policy = SandboxPolicy.trusted()
        .stepLimit(10_000)
        .build();
try (final var engine = TuriEngine.create(policy);
     final var session = engine.newSession()) {
    // the limit caps the TOTAL steps of the session; once it is hit, every
    // further evaluation halts immediately ...
    assertThrows(ExecutionException.class, () -> session.eval("""
            mut n = 0
            while true {
                n = n + 1
            }
            """));
    assertThrows(ExecutionException.class, () -> session.eval("1 + 1"));

    // ... until the embedder - and only the embedder - grants a fresh budget;
    // resetSteps() also reports the steps consumed, for per-evaluation metering
    final long used = session.resetSteps();
    assertTrue(used >= 10_000);
    assertEquals(2L, session.eval("1 + 1"));
}
```
<!--/INCLUDE-->

### 1.4.2. Letting cleanup code run: grace steps

A hard halt has one downside: a well-behaved script holding a resource never gets to release
it. `graceSteps` grants `finally` and `with`-exit blocks a small, bounded, one-shot budget of
extra steps after a halt. The cleanup can never suppress the halt, and a hostile cleanup
(an infinite loop in `finally`) is cut off when the budget is spent:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingLimits.java"
prefix: "```java"
postfix: "```"
start: "// snippet grace_steps"
end: "// end snippet"
margin: 0
_content_generated_: 705:md5:56d25099e5f4086878297b300245764c
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var policy = SandboxPolicy.trusted()
        .stepLimit(10_000)
        .graceSteps(1_000)
        .build();
try (final var engine = TuriEngine.create(policy);
     final var session = engine.newSession()) {
    assertThrows(ExecutionException.class, () -> session.eval("""
            global resource = "open"
            try {
                mut n = 0
                while true {
                    n = n + 1
                }
            } finally {
                global resource
                resource = "closed"
            }
            """));
    // the halt fired, but the finally block got its bounded grace budget
    assertEquals("closed", session.get("resource"));
}
```
<!--/INCLUDE-->

### 1.4.3. Limiting time: the wall-clock timeout

Steps are a poor proxy for time: a script blocked in `sleep()`, a channel read, or slow I/O
consumes almost no steps. The timeout is enforced by a watchdog timer that aborts every
thread of the session — interrupting blocked ones — when it fires:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingLimits.java"
prefix: "```java"
postfix: "```"
start: "// snippet timeout"
end: "// end snippet"
margin: 0
_content_generated_: 645:md5:e7b6354788a251d51e88d1f1cbef37b9
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var policy = SandboxPolicy.trusted()
        .timeout(Duration.ofMillis(200))
        .build();
try (final var engine = TuriEngine.create(policy);
     final var session = engine.newSession()) {
    // sleep() consumes no interpreter steps; only the wall-clock watchdog stops it,
    // and it does so by interrupting the thread, without waiting out the sleep
    assertThrows(TuriTimeoutException.class, () -> session.eval("sleep(60)"));

    // a timed-out session is aborted for good and cannot be used anymore
    assertTrue(session.isTimedOut());
    assertThrows(IllegalStateException.class, () -> session.eval("1"));
}
```
<!--/INCLUDE-->

Use the step limit to bound *work* and the timeout to bound *latency*; in a server, you
usually want both.

### 1.4.4. Limiting concurrency: the thread cap

Turicum scripts can start asynchronous tasks (`async`) and reactive flows, each running on
its own virtual thread. `maxThreads` caps how many of these *additional* threads may run
concurrently across all sessions of the engine. The main interpreter thread of a session
never counts against the limit: `maxThreads(1)` means the main thread plus at most one
task, and `maxThreads(0)` means strictly single-threaded scripts.

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingLimits.java"
prefix: "```java"
postfix: "```"
start: "// snippet thread_cap"
end: "// end snippet"
margin: 0
_content_generated_: 1154:md5:03b4c976e49de0faffe4efbe24dc0165
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var policy = SandboxPolicy.trusted()
        .maxThreads(2)
        .build();
try (final var engine = TuriEngine.create(policy);
     final var session = engine.newSession()) {
    // two concurrent tasks fit into the two permits and run to completion
    assertEquals(3L, session.eval("""
            let t1 = async { 1 }
            let t2 = async { 2 }
            let r1 = await t1
            let r2 = await t2
            r1 + r2
            """));

    // a third task while two are still running does not fit; it fails
    // immediately - the cap never blocks the script waiting for a permit
    final var e = assertThrows(ExecutionException.class, () -> session.eval("""
            let s1 = async { sleep(10) }
            let s2 = async { sleep(10) }
            let s3 = async { sleep(10) }
            """));
    assertEquals("Thread limit reached, cannot start a new thread", e.getMessage());

    // the permits belong to running tasks, not to the session: they were released
    // when the failed evaluation aborted its tasks, so the session keeps working
    assertEquals(42L, session.eval("await async { 42 }"));
}
```
<!--/INCLUDE-->

#### Why the cap is a hard limit, not a throttle

When the cap is reached, starting a further task **fails immediately** instead of waiting
for a running task to finish. This is deliberate: the cap is a defense against runaway
scripts, not a work-scheduling device, and a waiting cap would be neither.

Consider `maxThreads(2)` with a *waiting* cap and this innocent-looking script:

```turi
let t1 = async { let c = async { 1 }; await c }
let t2 = async { let c = async { 2 }; await c }
```

The two outer tasks take both permits, then each starts a child and waits for it. The
children wait for a permit that only their own parents could release — a permanent
deadlock, four lines into a well-behaved script. Turicum makes this composition natural:
tasks start tasks, flows start a task per cell, and channel-connected parents and children
routinely wait for each other. Worse, a thread blocked on a permit executes no interpreter
steps, so the step limit — the sandbox's primary defense — would never fire; only a
wall-clock timeout could rescue the session, and only if one is configured. And against a
hostile script, a waiting cap is no limit at all, merely a delay.

Failing fast instead yields an attributable error (`Thread limit reached, cannot start a
new thread`) at the exact spawn site, keeps the halt machinery effective, and makes cap
sizing observable during development. If a script legitimately needs to process a large
batch with bounded concurrency, the batching belongs in the script (start `n`, `await`
them, start the next `n`) where the dependency structure is known.

#### Single-threaded scripts

For embeddings that want plain, predictable, sequential scripting — validation rules,
configuration expressions, formulas — `singleThread()` (an alias for `maxThreads(0)`)
switches concurrency off entirely while leaving the rest of the language untouched:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingLimits.java"
prefix: "```java"
postfix: "```"
start: "// snippet single_thread"
end: "// end snippet"
margin: 0
_content_generated_: 644:md5:410d2a142d23dad49a4f2fb8301c8376
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var policy = SandboxPolicy.trusted()
        .singleThread()             // alias for maxThreads(0)
        .build();
try (final var engine = TuriEngine.create(policy);
     final var session = engine.newSession()) {
    // the main interpreter thread does not count against the limit;
    // ordinary sequential code is unaffected
    assertEquals(42L, session.eval("6 * 7"));

    // but no additional thread can be started at all
    final var e = assertThrows(ExecutionException.class,
            () -> session.eval("async { 1 }"));
    assertEquals("Thread limit reached, cannot start a new thread", e.getMessage());
}
```
<!--/INCLUDE-->

### 1.4.5. Metering

`TuriSession.stepsUsed()` reports the interpreter steps a session has consumed. Use it to
meter tenants, or to measure representative workloads before choosing a `stepLimit`:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingLimits.java"
prefix: "```java"
postfix: "```"
start: "// snippet metering"
end: "// end snippet"
margin: 0
_content_generated_: 460:md5:1eb8812b461eb3a560f34af39ed414a7
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
try (final var engine = TuriEngine.create();
     final var session = engine.newSession()) {
    session.eval("""
            mut sum = 0
            for i = 1 ; i <= 100 ; i++ {
                sum += i
            }
            sum
            """);
    // stepsUsed() reports the total interpreter steps of the session; use it to
    // meter tenants and to calibrate stepLimit() for real workloads
    assertTrue(session.stepsUsed() > 100);
}
```
<!--/INCLUDE-->

### 1.4.6. Capabilities: what a script may reach

Resource limits bound how *hard* a script runs; capabilities bound *what it can touch*.
A capability gates a whole family of built-in functions. When a capability is not granted,
its built-ins are simply not registered, and a script that names one gets an ordinary
"undefined symbol" error — hiding the built-in is exactly the point.

| Capability | Built-ins it gates |
|---|---|
| `JAVA_REFLECTION` | `java_class`, `java_call`, `java_object`, `java_import`, `java_callback`, `add_java_classes`, `java_resources`, `java_type`, `as_object` |
| `IMPORT` | `import`, `sys_import`, `source_directory` |
| `FILE_READ` | `file_read`, `file_lines`, `file_reader`, `file_random_reader`, `file_map_reader`, `file_exists`, `is_file`, `is_dir`, `file_stat`, `file_copy`, `glob`, the input-stream classes |
| `FILE_WRITE` | `file_write`, `file_writer`, `file_random_editor`, `file_map_editor` |
| `FILE_CREATE` | `mkdir`, and — checked at runtime — the create-a-new-file half of `file_write`/`file_writer`/`file_copy`/`file_move` |
| `FILE_DELETE` | `file_delete`, `file_move` (moving always removes the source) |
| `FILE_TEMP` | `tmp_file`, `tmp_dir` |
| `ENV` | `env` |
| `NETWORK` | `http_client`, `http_server` |

The two trust modes differ only in their default: `trusted()` grants all capabilities and
`untrusted()` grants none.

Two conventions tie the file family together. **Write implies read**: granting any of
`FILE_WRITE`/`FILE_CREATE`/`FILE_DELETE` in untrusted mode automatically grants
`FILE_READ`, and a trusted policy that denies `FILE_READ` while leaving a write-family
capability granted is a build-time error — deny the whole family instead.
**`FILE_TEMP` implies the whole family**: temp files are read-write by nature, so granting
`FILE_TEMP` also grants `FILE_WRITE`/`FILE_CREATE`/`FILE_DELETE` (and, through them,
`FILE_READ`), confined to the session's scratch directory when no other file root is
configured. Some capability needs depend on the arguments and the file-system state and are
therefore checked at runtime rather than at registration: creating a file that does not
exist yet demands `FILE_CREATE` even though `file_write` itself is registered under
`FILE_WRITE`, and the target side of `file_copy`/`file_move` demands `FILE_WRITE` or
`FILE_CREATE` depending on whether the target exists.

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingCapabilities.java"
prefix: "```java"
postfix: "```"
start: "// snippet trust_modes"
end: "// end snippet"
margin: 0
_content_generated_: 673:md5:dd5f0bf6259ef8aac9925adaf3ca8628
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
// trusted mode: all capabilities granted; java_class is available
try (final var engine = TuriEngine.create(SandboxPolicy.trusted().build());
     final var session = engine.newSession()) {
    assertDoesNotThrow(() -> session.eval("java_class(\"java.lang.Math\")"));
}

// untrusted mode: nothing granted; java_class is not even registered, so the script
// gets an ordinary "undefined symbol" error - the built-in is simply absent
try (final var engine = TuriEngine.create(SandboxPolicy.untrusted().build());
     final var session = engine.newSession()) {
    assertThrows(ExecutionException.class, () -> session.eval("java_class(\"java.lang.Math\")"));
}
```
<!--/INCLUDE-->

In untrusted mode you add exactly the capabilities the script needs:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingCapabilities.java"
prefix: "```java"
postfix: "```"
start: "// snippet grant_capability"
end: "// end snippet"
margin: 0
_content_generated_: 563:md5:1c059152dc324cb7353c16de3bc9370f
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
// env is gated by the ENV capability; without it the built-in is absent
try (final var engine = TuriEngine.create(SandboxPolicy.untrusted().build());
     final var session = engine.newSession()) {
    assertThrows(ExecutionException.class, () -> session.eval("env(\"PATH\")"));
}

// granting ENV registers it
final var policy = SandboxPolicy.untrusted().allow(Capability.ENV).build();
try (final var engine = TuriEngine.create(policy);
     final var session = engine.newSession()) {
    assertDoesNotThrow(() -> session.eval("env(\"PATH\")"));
}
```
<!--/INCLUDE-->

### 1.4.7. The class-access filter and the deny floor

Granting `JAVA_REFLECTION` decides *whether* the reflection built-ins exist; a second,
finer decision governs *which Java classes* they may load. In untrusted mode the allowed
classes form an allowlist, and everything else — including a class you did not think to
name — is denied with an attributable message:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingCapabilities.java"
prefix: "```java"
postfix: "```"
start: "// snippet class_allowlist"
end: "// end snippet"
margin: 0
_content_generated_: 680:md5:606df21dda0b02cfca1c452acc0b11a0
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var policy = SandboxPolicy.untrusted()
        .allow(Capability.JAVA_REFLECTION)
        .allowJavaClasses("java.lang.Math")   // only this class
        .build();
try (final var engine = TuriEngine.create(policy);
     final var session = engine.newSession()) {
    // the allowlisted class loads
    assertDoesNotThrow(() -> session.eval("java_class(\"java.lang.Math\")"));

    // any other class is denied, with an attributable message
    final var e = assertThrows(ExecutionException.class,
            () -> session.eval("java_class(\"java.util.ArrayList\")"));
    assertTrue(e.getMessage().contains("denied by the sandbox policy"), e.getMessage());
}
```
<!--/INCLUDE-->

Independent of mode, a **mandatory deny floor** always blocks the classes a script could use
to escape the sandbox: the interpreter's own `ch.turic.*` internals (a script that could load
`GlobalContext` would lift its own limits), the JDK internals (`jdk.internal.*`, `sun.*`), the
reflection and method-handle escape hatches (`java.lang.reflect.*`, `java.lang.invoke.*`), and
the process/exit/raw-thread classes (`Runtime`, `ProcessBuilder`, `System`, `Thread`). In
untrusted mode the floor is absolute — allowlisting a floored class does not open it:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingCapabilities.java"
prefix: "```java"
postfix: "```"
start: "// snippet deny_floor"
end: "// end snippet"
margin: 0
_content_generated_: 587:md5:bf20d8294fd8bc5b3a94594145722c8e
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
// even explicitly allowlisting Runtime cannot open it in untrusted mode:
// the deny floor is absolute
final var policy = SandboxPolicy.untrusted()
        .allow(Capability.JAVA_REFLECTION)
        .allowJavaClasses("java.lang.Runtime")
        .build();
try (final var engine = TuriEngine.create(policy);
     final var session = engine.newSession()) {
    final var e = assertThrows(ExecutionException.class,
            () -> session.eval("java_class(\"java.lang.Runtime\")"));
    assertTrue(e.getMessage().contains("denied by the sandbox policy"), e.getMessage());
}
```
<!--/INCLUDE-->

> **Why the interpreter's own `ch.turic.*` classes are floored.** Those classes *are* the
> sandbox: `GlobalContext` holds the step counter, the granted capabilities, and the import
> root; `TuricumClassLoader` holds this very filter. A script that reached their live instances
> could disarm its own limits, so they are the last thing a restricted script may touch. The
> whole package is blocked rather than audited class by class — several of these classes also
> carry process-wide state that could be corrupted or used to reach across sessions — and it
> costs legitimate scripts nothing, since no Turicum script has any reason to reflect on
> interpreter internals. (It is defense-in-depth: `java.lang.reflect.*` is floored too, so
> loading a `ch.turic.*` class by name does not by itself hand over a live instance.)

In trusted mode the floor is on by default too, but a trusted embedder can pierce it
deliberately. The method is named `unsafeAllowJavaClasses` so the decision stands out in a
code review:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingCapabilities.java"
prefix: "```java"
postfix: "```"
start: "// snippet unsafe_allow"
end: "// end snippet"
margin: 0
_content_generated_: 768:md5:119d0cf9d3b8613ca80402d4f6396da0
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
// trusted mode also installs the floor, so a plain trusted policy denies Runtime ...
try (final var engine = TuriEngine.create(SandboxPolicy.trusted().build());
     final var session = engine.newSession()) {
    assertThrows(ExecutionException.class, () -> session.eval("java_class(\"java.lang.Runtime\")"));
}

// ... but a trusted embedder can deliberately pierce it; the 'unsafe' prefix is a
// signal to reviewers that this class can defeat the sandbox
final var policy = SandboxPolicy.trusted()
        .unsafeAllowJavaClasses("java.lang.Runtime")
        .build();
try (final var engine = TuriEngine.create(policy);
     final var session = engine.newSession()) {
    assertDoesNotThrow(() -> session.eval("java_class(\"java.lang.Runtime\")"));
}
```
<!--/INCLUDE-->

> **Granularity.** The enforceable chokepoint is class loading, so the unit of permission is
> the *class*: an allowlisted class exposes all of its public members. Do not allowlist broad
> utility classes. Instead, write a small facade class in your host application exposing
> exactly the operations scripts may perform, and allowlist that.

The filter governs more than class lookups by name. A script can also obtain a Java object
*without* naming a class — most importantly, from an object the embedder injects with
`session.set(...)`. Method and field access on such an object is filtered too, by the object's
own runtime class, so injecting a facade does not hand the script a reflective foothold. The
facade is reachable only when its class is allowlisted:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingCapabilities.java"
prefix: "```java"
postfix: "```"
start: "// snippet injected_object"
end: "// end snippet"
margin: 0
_content_generated_: 857:md5:df05f4828833f4ddcc6345b7db3678e8
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var facade = new HostFacade();

// untrusted with the facade's class NOT allowlisted: even a method on the injected
// object is denied - the allowlist governs which classes a script may touch reflectively
try (final var engine = TuriEngine.create(SandboxPolicy.untrusted().build());
     final var session = engine.newSession()) {
    session.set("host", facade);
    assertThrows(ExecutionException.class, () -> session.eval("host.greet()"));
}

// allowlisting the facade class makes exactly its API reachable
final var policy = SandboxPolicy.untrusted()
        .allowJavaClasses("com.example.host.HostFacade")
        .build();
try (final var engine = TuriEngine.create(policy);
     final var session = engine.newSession()) {
    session.set("host", facade);
    assertEquals("hello from the host", session.eval("host.greet()"));
}
```
<!--/INCLUDE-->

This closes the classic escape from a held object,
`host.getClass().getClassLoader().loadClass("java.lang.Runtime")`: `getClass()` returns an
inert `java.lang.Class`, and because `Class` and `ClassLoader` are on the deny floor, any
reflective method on them is refused — even when the facade itself is allowlisted:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingCapabilities.java"
prefix: "```java"
postfix: "```"
start: "// snippet reflective_escape_blocked"
end: "// end snippet"
margin: 0
_content_generated_: 1134:md5:121f84ce7713b5e3802a3d7671a83842
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
// The classic escape from a held Java object is
//   host.getClass().getClassLoader().loadClass("java.lang.Runtime")
// Method dispatch on a Java object is filtered too (not only class lookups by name), and
// java.lang.Class / java.lang.ClassLoader are on the deny floor, so the pivot is blocked
// even though the facade itself is allowlisted.
final var policy = SandboxPolicy.untrusted()
        .allow(Capability.JAVA_REFLECTION)
        .allowJavaClasses("com.example.host.HostFacade")
        .build();
try (final var engine = TuriEngine.create(policy);
     final var session = engine.newSession()) {
    session.set("host", new HostFacade());

    // the facade's own method works
    assertEquals("hello from the host", session.eval("host.greet()"));

    // getClass() returns an inert Class, but any reflective method on it is denied
    final var e = assertThrows(ExecutionException.class,
            () -> session.eval("host.getClass().getClassLoader()"));
    assertTrue(e.getMessage().contains("java.lang.Class")
            && e.getMessage().contains("denied by the sandbox policy"), e.getMessage());
}
```
<!--/INCLUDE-->

### 1.4.8. Scoping imports

When a script may import source files (`IMPORT` granted in untrusted mode), it must be told
where from. `importRoot(Path)` sets a **ceiling** on `import`/`sys_import`: every file the
normal `APPIA` search resolves must lie within that directory, and one that resolves outside
it (via `..`, or via an `APPIA` entry pointing elsewhere) is denied. Data-file access is
scoped separately — by the file roots of the next section, not by the import root. Granting
an import or file capability untrusted without its root is a configuration error caught when
the policy is built:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingCapabilities.java"
prefix: "```java"
postfix: "```"
start: "// snippet import_root_required"
end: "// end snippet"
margin: 0
_content_generated_: 636:md5:2fb484b555a39f73b27313662fe5863d
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
// granting IMPORT in untrusted mode without scoping it to an import root is a
// configuration error, caught when the policy is built
assertThrows(IllegalStateException.class,
        () -> SandboxPolicy.untrusted().allow(Capability.IMPORT).build());
// the same holds for FILE_READ and the file roots ...
assertThrows(IllegalStateException.class,
        () -> SandboxPolicy.untrusted().allow(Capability.FILE_READ).build());
// ... and for the write-family capabilities and the read-write file roots
assertThrows(IllegalStateException.class,
        () -> SandboxPolicy.untrusted().allow(Capability.FILE_WRITE).build());
```
<!--/INCLUDE-->

The root is a *ceiling*, not a search path: it does not change how imports are resolved, only
which results are permitted. The normal `APPIA` search still runs — driven by the script's own
`global APPIA` or the host environment — and whatever it finds is then required to lie under
the root. A script therefore cannot widen its reach by pointing `APPIA` elsewhere; the file may
be found, but reading it is denied. A practical consequence: because the root does not itself
contribute a search location, make sure `APPIA` includes at least one directory under the root,
or nothing will resolve.

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingCapabilities.java"
prefix: "```java"
postfix: "```"
start: "// snippet import_root$"
end: "// end snippet"
margin: 0
_content_generated_: 1351:md5:d43643cac8440d3d5422523541d5ce1b
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var importRoot = Files.createDirectory(workDir.resolve("scripts"));
// a library under the root ...
Files.writeString(importRoot.resolve("lib.turi"), "let answer = 42\nexport_all()\n");
// ... and a file OUTSIDE the root, one directory above it
Files.writeString(workDir.resolve("secret.turi"), "let stolen = 1\nexport_all()\n");

final var policy = SandboxPolicy.untrusted()
        .allow(Capability.IMPORT)
        .importRoot(importRoot)
        .build();
try (final var engine = TuriEngine.create(policy);
     final var session = engine.newSession()) {
    // The script drives resolution with APPIA exactly as usual; importRoot does not
    // replace the search path, it only caps the result. An import that resolves under
    // the root works:
    session.set("root", importRoot.toString());
    assertEquals(42L, session.eval("global APPIA = [root]\nimport \"lib\"\nanswer"));

    // Pointing APPIA at a directory outside the root cannot widen the reach: the file
    // is found there, but denied because it resolves above the ceiling.
    session.set("above", workDir.toString());
    final var e = assertThrows(ExecutionException.class,
            () -> session.eval("global APPIA = [above]\nimport \"secret\"\nstolen"));
    assertTrue(e.getMessage().contains("outside the sandbox import root"), e.getMessage());
}
```
<!--/INCLUDE-->

### 1.4.9. Scoping data-file access

The file built-ins (`file_read`, `file_write`, `file_delete`, the handles, `glob`, …) are
confined by **file roots**, configured independently of the import root. There are two kinds,
and both builder methods are repeatable — the built-ins confine against the whole set:

- `fileReadRoot(Path...)` — trees the script may only read;
- `fileReadWriteRoot(Path...)` — trees with full access: read, write, create, and delete.

A read or metadata operation must resolve under *any* root of either set; a mutation must
resolve under a read-write root:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestFileIo.java"
prefix: "```java"
postfix: "```"
start: "// snippet file_roots"
end: "// end snippet"
margin: 0
_content_generated_: 1095:md5:c0ab2f073d0efd04248bf78a50503e64
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var readOnly = Files.createDirectory(workDir.resolve("ro"));
final var rw = Files.createDirectory(workDir.resolve("rw"));
Files.writeString(readOnly.resolve("data.txt"), "protected");
final var policy = SandboxPolicy.untrusted()
        .allow(Capability.FILE_WRITE, Capability.FILE_CREATE, Capability.FILE_DELETE)
        .fileReadRoot(readOnly)      // data the script may only read
        .fileReadWriteRoot(rw)       // the tree it may modify
        .build();
try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
    session.set("protected_file", readOnly.resolve("data.txt").toString());
    // reading from the read-only root works
    assertEquals("protected", session.eval("file_read(protected_file)"));
    // writing and deleting there is denied
    assertThrows(ExecutionException.class, () -> session.eval("file_write(protected_file, \"x\")"));
    assertThrows(ExecutionException.class, () -> session.eval("file_delete(protected_file)"));
    assertEquals("protected", Files.readString(readOnly.resolve("data.txt")));
}
```
<!--/INCLUDE-->

Relative script paths see a stable virtual tree independent of the host's working directory:
a relative *read* is searched against the roots in declaration order (read-only roots first,
then read-write roots), taking the first candidate that exists; a relative *mutation*
resolves against the first declared read-write root — a write target is deterministic, never
search-dependent. Symbolic links are followed, but the confinement check runs on the *real*
path, so a symlink inside a root cannot alias a file outside it; `glob` silently omits such
results from its listings.

The capability split is enforced both at registration and at runtime. A read-only grant does
not even register the mutating built-ins; and a built-in whose capability need depends on
the file-system state checks it when it runs:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestFileIo.java"
prefix: "```java"
postfix: "```"
start: "// snippet file_create_demand"
end: "// end snippet"
margin: 0
_content_generated_: 740:md5:cd02a857ae339e7905e0dea74a72f6c2
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
Files.writeString(dir.resolve("existing.txt"), "old");
final var policy = SandboxPolicy.untrusted()
        .allow(Capability.FILE_WRITE) // no FILE_CREATE
        .fileReadWriteRoot(dir)
        .build();
try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
    // updating an existing file is fine
    session.eval("file_write(\"existing.txt\", \"new\")");
    assertEquals("new", session.eval("file_read(\"existing.txt\")"));
    // creating a new one demands FILE_CREATE at runtime
    final var e = assertThrows(ExecutionException.class,
            () -> session.eval("file_write(\"fresh.txt\", \"x\")"));
    assertTrue(e.getMessage().contains("FILE_CREATE"), e.getMessage());
}
```
<!--/INCLUDE-->

A deliberate design point to be aware of: `file_delete(path, force=true)` deletes a
directory tree **recursively**. The root confinement — not the feature set — is the safety
boundary, so grant `FILE_DELETE` and choose the read-write roots accordingly.

**Temp files.** The `FILE_TEMP` capability gives a script a scratch area without granting
access to any host directory: `tmp_file()`/`tmp_dir()` create entries in a per-session
scratch directory, which acts as an implicit read-write root and is deleted — with
everything in it — when the session closes:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestFileIo.java"
prefix: "```java"
postfix: "```"
start: "// snippet file_temp"
end: "// end snippet"
margin: 0
_content_generated_: 1404:md5:b6d201f715a04436182a53e810a161f5
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var policy = SandboxPolicy.untrusted()
        .allow(Capability.FILE_TEMP)
        .build(); // no roots needed: the scratch dir is the implicit read-write root
assertTrue(policy.grantedCapabilities().containsAll(java.util.Set.of(
        Capability.FILE_READ, Capability.FILE_WRITE, Capability.FILE_CREATE, Capability.FILE_DELETE)));
final Path tmpPath;
try (final var engine = TuriEngine.create(policy); final var session = engine.newSession()) {
    final var tmp = (String) session.eval("tmp_file(prefix=\"job-\", suffix=\".txt\")");
    tmpPath = Path.of(tmp);
    assertTrue(Files.exists(tmpPath));
    assertTrue(tmpPath.getFileName().toString().startsWith("job-"));
    session.set("p", tmp);
    session.eval("file_write(p, \"scratch\")");
    assertEquals("scratch", session.eval("file_read(p)"));
    // a directory, and a file created relative to the scratch root
    final var d = (String) session.eval("tmp_dir()");
    assertTrue(Files.isDirectory(Path.of(d)));
    session.eval("file_write(\"relative.txt\", \"under the scratch root\")");
    // absolute paths outside the scratch area stay denied
    session.set("outside", "/etc/passwd");
    assertThrows(ExecutionException.class, () -> session.eval("file_read(outside)"));
}
// the scratch area is deleted when the session closes
assertFalse(Files.exists(tmpPath));
assertFalse(Files.exists(tmpPath.getParent()));
```
<!--/INCLUDE-->

**Handles and leak safety.** The handle built-ins (`file_reader`, `file_writer`,
`file_random_reader`, `file_random_editor`, `file_map_reader`, `file_map_editor`) return
resources that a script should close, preferably through the `with` command. A handle the
script never closes is force-closed when the session is closed, so a sandboxed script that
opens files in a loop cannot leak host file descriptors past the session.

One caveat is inherited from Java 21: a **memory-mapped** handle's `close()` invalidates the
handle, but the mapped pages are released only when the buffer is garbage-collected (on
Windows the file stays locked while mapped). A mapping is therefore host virtual address
space held on the sandbox's behalf, and the policy caps it: `maxMappedBytes(long)` bounds
the running total of all live mappings of a session. The untrusted default is `0` — the
mmap built-ins are effectively unusable unless the host opts in — while trusted and
unrestricted policies default to no limit.

## 1.5. Scaling up

### 1.5.1. Compile once, run many times

Compilation is the expensive part, and a `TuriProgram` is immutable: compile each script
once and evaluate it in a fresh session per request. This is the intended pattern for
servers evaluating the same user-defined script against many inputs:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingAdvanced.java"
prefix: "```java"
postfix: "```"
start: "// snippet compile_once"
end: "// end snippet"
margin: 0
_content_generated_: 675:md5:5e3853fe22c2d9804c84fb0c3c44ed3a
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
try (final var engine = TuriEngine.create()) {
    // a compiled program is immutable and holds no execution state;
    // compile it once and evaluate it in as many sessions as needed
    final var program = engine.compile("price * (1 + vat_rate)");

    try (final var session = engine.newSession()) {
        session.set("price", 100.0);
        session.set("vat_rate", 0.081);
        assertEquals(108.1, (double) session.eval(program), 0.0001);
    }
    try (final var session = engine.newSession()) {
        session.set("price", 250.0);
        session.set("vat_rate", 0.026);
        assertEquals(256.5, (double) session.eval(program), 0.0001);
    }
}
```
<!--/INCLUDE-->

### 1.5.2. Sessions are isolated

Sessions of the same engine share nothing but the engine's executor and thread-permit pool.
Globals defined in one session are invisible to every other, which makes a session the unit
of multi-tenancy:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingAdvanced.java"
prefix: "```java"
postfix: "```"
start: "// snippet session_isolation"
end: "// end snippet"
margin: 0
_content_generated_: 355:md5:3ede11c4e717f68de99eb4156dd49985
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
try (final var engine = TuriEngine.create();
     final var tenantA = engine.newSession();
     final var tenantB = engine.newSession()) {
    tenantA.eval("global secret = \"tenant A data\"");
    // sessions share nothing; tenant B does not see tenant A's globals
    assertThrows(ExecutionException.class, () -> tenantB.eval("secret"));
}
```
<!--/INCLUDE-->

### 1.5.3. Storing precompiled programs

A compiled program serializes to the compact binary `.turc` format. Store it in a file, a
database, or a cache, and load it later — for example at application startup — without
paying for compilation again:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingAdvanced.java"
prefix: "```java"
postfix: "```"
start: "// snippet serialize"
end: "// end snippet"
margin: 0
_content_generated_: 422:md5:55aa612f5adc1ae5959aa0dcb14a2af6
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
try (final var engine = TuriEngine.create()) {
    // serialize the compiled program to the binary .turc format ...
    final byte[] turc = engine.compile("6 * 7").serialize();

    // ... store it in a file, a database, a cache ... and load it later,
    // skipping compilation entirely
    try (final var session = engine.newSession()) {
        assertEquals(42L, session.eval(engine.load(turc)));
    }
}
```
<!--/INCLUDE-->

### 1.5.4. Concurrency inside the script

Sandboxing does not dumb the language down: scripts keep the full concurrency toolbox
(`async`, `await`, channels, flows). The policy only decides how many interpreter threads
may run at once:

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingAdvanced.java"
prefix: "```java"
postfix: "```"
start: "// snippet async_scripts"
end: "// end snippet"
margin: 0
_content_generated_: 548:md5:d7579ca38b08e5828135d4762eec59d5
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var policy = SandboxPolicy.trusted()
        .maxThreads(4)
        .build();
try (final var engine = TuriEngine.create(policy);
     final var session = engine.newSession()) {
    // scripts may use the language's full concurrency toolbox; the sandbox only
    // caps how many interpreter threads may run at the same time
    assertEquals(3L, session.eval("""
            let t1 = async { 1 }
            let t2 = async { 2 }
            let r1 = await t1
            let r2 = await t2
            r1 + r2
            """));
}
```
<!--/INCLUDE-->

## 1.6. Error handling

Three kinds of failures reach the embedder, all unchecked:

- **`BadSyntax`** — the source does not parse; thrown by `compile` (and by `eval(String)`),
  before any script code runs.
- **`ExecutionException`** — the script failed at runtime (a `die`, an undefined variable,
  a type error) or hit a resource limit (step limit, thread cap). The stack trace points
  into the *script*, not into the interpreter's Java internals.
- **`TuriTimeoutException`** — the wall-clock timeout fired. A subtype of
  `ExecutionException`, so a single catch clause handles both if you do not need to
  distinguish them.

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingAdvanced.java"
prefix: "```java"
postfix: "```"
start: "// snippet compile_errors"
end: "// end snippet"
margin: 0
_content_generated_: 205:md5:6db3df8c11dcf0be578a9b3337a96795
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
try (final var engine = TuriEngine.create()) {
    // syntax errors surface at compile(), before any script code runs
    assertThrows(BadSyntax.class, () -> engine.compile("let let let"));
}
```
<!--/INCLUDE-->

<!--INCLUDE
from: "core/src/test/java/ch/turic/embed/TestEmbeddingAdvanced.java"
prefix: "```java"
postfix: "```"
start: "// snippet runtime_errors"
end: "// end snippet"
margin: 0
_content_generated_: 495:md5:f040b51702456214440d4e31255bc7b7
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
try (final var engine = TuriEngine.create();
     final var session = engine.newSession()) {
    final var e = assertThrows(ExecutionException.class, () -> session.eval("""
            fn explode() {
                die "something went wrong"
            }
            explode()
            """));
    // the exception message and stack trace point into the script, not into
    // the interpreter's Java internals
    assertTrue(e.getMessage().contains("something went wrong"));
}
```
<!--/INCLUDE-->

## 1.7. Lifecycle and thread-safety rules

- A `TuriEngine` is thread-safe and long-lived; one per application or tenant class is the
  norm. Closing it stops its executors; finish the sessions first.
- A `TuriSession` is single-threaded and cheap; create one per evaluation or per
  conversation. Close it when done (try-with-resources).
- A `TuriProgram` is immutable and safe to share between threads and sessions.
- A session that hit the **timeout** is aborted for good: every further call throws
  `IllegalStateException`. Create a new session — its globals may be in an inconsistent
  state after a mid-flight abort, so reuse would be unsafe anyway.
- A session that hit the **step limit** keeps its state readable (`get` works), but the
  step counter is not reset automatically, so further `eval` calls halt immediately —
  until the embedder grants a fresh budget with `resetSteps()`.

## 1.8. What the sandbox does not limit (yet)

The current API bounds execution (steps, time, threads), redirects output, gates the
capability families a script may use, filters which Java classes it may load, and scopes its
file access (§1.4.6–§1.4.8). What remains unimplemented is **resource accounting**: there is
no memory cap and no CPU-share throttle, and deep recursion still relies on the host thread's
stack. These are designed as Phase 3 — see `EMBEDDING-SANDBOX-DESIGN.md`.

Two limits are inherent to running in-process on the JVM and are not solved by this API:
a script granted `JAVA_REFLECTION` plus a broad class allowlist can still consume unbounded
memory or CPU, and hard memory isolation is not achievable in-process at all. For genuinely
hostile code, combine the sandbox with process-level isolation (a separate JVM with `-Xmx`,
or a container); the `.turc` serialization makes running a compiled program in a fresh
process straightforward.
