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
_content_generated_: 1515:md5:ab3e1f2b16314de3915941be5d1a0caa
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
| `SandboxPolicy` | Immutable set of limits and redirections, created with a builder. `SandboxPolicy.UNRESTRICTED` applies no limits.                                                  |
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
    <version>1.4.2</version>
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
_content_generated_: 390:md5:1f0940612e457a74abe05838a2d375e2
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var captured = new ByteArrayOutputStream();
final var policy = SandboxPolicy.builder()
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
merely unpredictable scripts, build a `SandboxPolicy` and create the engine with it. All
limits are independent and optional:

| Limit | Builder method | What it bounds                                          |
|---|---|---------------------------------------------------------|
| Step limit | `stepLimit(int)` | Total interpreter steps (commands executed) per session |
| Cleanup grace | `graceSteps(int)` | Extra steps `finally`/exit blocks may run after a halt  |
| Timeout | `timeout(Duration)` | Wall-clock time of one `eval` call                      |
| Thread cap | `maxThreads(int)` | Concurrently running interpreter threads per engine     |
| Output | `stdout(...)`, `stderr(...)` | Where `print`/`println` and errors go                   |

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
_content_generated_: 550:md5:d4e5bdad5b4ee84eaff33209def1c0c7
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var policy = SandboxPolicy.builder()
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
_content_generated_: 637:md5:89e63946f59ef498c662eb37fdb98d84
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var policy = SandboxPolicy.builder()
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
_content_generated_: 853:md5:5bb32f898c41aeeaf60faa295c5decff
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var policy = SandboxPolicy.builder()
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
_content_generated_: 705:md5:536f86d3815792e699e2ecccb8605144
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var policy = SandboxPolicy.builder()
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
_content_generated_: 645:md5:faf4cecd5ebc2d1347ae32c6c23210dc
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var policy = SandboxPolicy.builder()
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
_content_generated_: 1154:md5:e462adf41f5fa1b5983651035a494136
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var policy = SandboxPolicy.builder()
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
_content_generated_: 644:md5:ccc04a16f4166b476989540598e3a962
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var policy = SandboxPolicy.builder()
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
_content_generated_: 548:md5:af214c58ebedad40a39a89aba7d1f697
# ⚠️ MANAGED CONTENT: Edits will be lost.
# danger zone: Delete _content_generated_ to override.
-->
```java
final var policy = SandboxPolicy.builder()
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

The current API bounds execution (steps, time, threads) and redirects output. It does
**not** yet restrict what the script can *reach*: the Java interoperability built-ins
(`java_class`, `java_call`, …), file imports, environment access, and networking built-ins
are all still available to the script, and there is no memory or CPU-share accounting.
Restricting those is designed and planned — see `EMBEDDING-SANDBOX-DESIGN.md` (capability
gating and class filtering in Phase 2, memory/CPU accounting in Phase 3). Until then, do
not treat the sandbox as a security boundary for actively hostile code; for that, combine
it with process-level isolation.
