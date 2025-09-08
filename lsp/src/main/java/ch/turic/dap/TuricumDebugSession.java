package ch.turic.dap;

import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.Thread;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Debug session that manages the execution of a Turicum program
 */
public class TuricumDebugSession {

    private final String program;
    private final String[] arguments;
    private final TuricumDebugServer server;
    private final AtomicInteger nextThreadId = new AtomicInteger(1);
    private final AtomicInteger nextFrameId = new AtomicInteger(1);

    private Process debuggeeProcess;
    private ExecutorService executorService;
    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;

    // Debug state
    private final Map<String, List<SourceBreakpoint>> breakpoints = new ConcurrentHashMap<>();
    private final Map<Integer, TuricumThread> threads = new ConcurrentHashMap<>();
    private final Map<Integer, TuricumStackFrame> stackFrames = new ConcurrentHashMap<>();
    private int mainThreadId;

    public TuricumDebugSession(String program, String[] arguments, TuricumDebugServer server) {
        this.program = program;
        this.arguments = arguments;
        this.server = server;
        this.executorService = Executors.newCachedThreadPool();
    }

    public void start() {
        try {
            // Create main thread
            mainThreadId = nextThreadId.getAndIncrement();
            TuricumThread mainThread = new TuricumThread(mainThreadId, "Main Thread");
            threads.put(mainThreadId, mainThread);

            // Notify about thread creation
            server.sendThreadEvent("started", mainThreadId);

            // Start the Turicum program
            startDebuggeeProcess();

            isRunning = true;
            server.sendContinuedEvent(mainThreadId);

        } catch (Exception e) {
            server.sendOutputEvent("stderr", "Failed to start debug session: " + e.getMessage() + "\n");
            server.sendTerminatedEvent();
        }
    }

    public void stopAtEntry() {
        try {
            // Create main thread
            mainThreadId = nextThreadId.getAndIncrement();
            TuricumThread mainThread = new TuricumThread(mainThreadId, "Main Thread");
            threads.put(mainThreadId, mainThread);

            // Start the process but immediately pause
            startDebuggeeProcess();

            isRunning = true;
            isPaused = true;

            // Create initial stack frame
            createInitialStackFrame();

            // Send stopped event for entry point
            server.sendStoppedEvent("entry", mainThreadId);

        } catch (Exception e) {
            server.sendOutputEvent("stderr", "Failed to start debug session: " + e.getMessage() + "\n");
            server.sendTerminatedEvent();
        }
    }

    private void startDebuggeeProcess() throws IOException {
        // Build command to run Turicum program
        List<String> command = new ArrayList<>();

        // Assume you have a Turicum interpreter/compiler
        command.add("turicum"); // or path to your Turicum runtime
        command.add("--debug"); // Debug mode flag
        command.add(program);
        command.addAll(Arrays.asList(arguments));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        debuggeeProcess = pb.start();

        // Monitor process output
        executorService.submit(this::monitorProcessOutput);

        // Monitor process termination
        executorService.submit(this::monitorProcessTermination);
    }

    private void monitorProcessOutput() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(debuggeeProcess.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                handleDebugOutput(line);
            }
        } catch (IOException e) {
            server.sendOutputEvent("stderr", "Error reading process output: " + e.getMessage() + "\n");
        }
    }

    private void monitorProcessTermination() {
        try {
            int exitCode = debuggeeProcess.waitFor();
            isRunning = false;

            server.sendExitedEvent(exitCode);
            server.sendTerminatedEvent();

        } catch (InterruptedException e) {
            java.lang.Thread.currentThread().interrupt();
        }
    }

    private void handleDebugOutput(String line) {
        // Parse debug protocol messages from your Turicum runtime
        // This is where you'd implement communication with your debuggee

        if (line.startsWith("BREAKPOINT_HIT:")) {
            // Parse breakpoint hit information
            String[] parts = line.split(":");
            if (parts.length >= 4) {
                String file = parts[1];
                int lineNum = Integer.parseInt(parts[2]);
                int threadId = Integer.parseInt(parts[3]);

                isPaused = true;
                updateStackTrace(threadId, file, lineNum);
                server.sendStoppedEvent("breakpoint", threadId);
            }
        } else if (line.startsWith("STEP_COMPLETE:")) {
            // Handle step completion
            String[] parts = line.split(":");
            if (parts.length >= 4) {
                String file = parts[1];
                int lineNum = Integer.parseInt(parts[2]);
                int threadId = Integer.parseInt(parts[3]);

                isPaused = true;
                updateStackTrace(threadId, file, lineNum);
                server.sendStoppedEvent("step", threadId);
            }
        } else if (line.startsWith("OUTPUT:")) {
            // Program output
            String output = line.substring(7) + "\n";
            server.sendOutputEvent("stdout", output);
        } else if (line.startsWith("ERROR:")) {
            // Error output
            String error = line.substring(6) + "\n";
            server.sendOutputEvent("stderr", error);
        }
        // Add more debug event handling as needed
    }

    public void updateBreakpoints(String sourcePath, List<SourceBreakpoint> bps) {
        breakpoints.put(sourcePath, bps);

        // Send breakpoint information to debuggee process
        if (debuggeeProcess != null && debuggeeProcess.isAlive()) {
            try {
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(debuggeeProcess.getOutputStream())
                );

                // Clear existing breakpoints for this file
                writer.println("CLEAR_BREAKPOINTS:" + sourcePath);

                // Set new breakpoints
                for (SourceBreakpoint bp : bps) {
                    writer.println("SET_BREAKPOINT:" + sourcePath + ":" + bp.getLine());
                }
                writer.flush();

            } catch (Exception e) {
                server.sendOutputEvent("stderr", "Failed to set breakpoints: " + e.getMessage() + "\n");
            }
        }
    }

    public void continue_() {
        if (isPaused && debuggeeProcess != null && debuggeeProcess.isAlive()) {
            try {
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(debuggeeProcess.getOutputStream())
                );
                writer.println("CONTINUE");
                writer.flush();

                isPaused = false;
                server.sendContinuedEvent(mainThreadId);

            } catch (Exception e) {
                server.sendOutputEvent("stderr", "Failed to continue: " + e.getMessage() + "\n");
            }
        }
    }

    public void next(int threadId) {
        if (isPaused && debuggeeProcess != null && debuggeeProcess.isAlive()) {
            try {
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(debuggeeProcess.getOutputStream())
                );
                writer.println("STEP_OVER:" + threadId);
                writer.flush();

                server.sendContinuedEvent(threadId);

            } catch (Exception e) {
                server.sendOutputEvent("stderr", "Failed to step over: " + e.getMessage() + "\n");
            }
        }
    }

    public void stepIn(int threadId) {
        if (isPaused && debuggeeProcess != null && debuggeeProcess.isAlive()) {
            try {
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(debuggeeProcess.getOutputStream())
                );
                writer.println("STEP_INTO:" + threadId);
                writer.flush();

                server.sendContinuedEvent(threadId);

            } catch (Exception e) {
                server.sendOutputEvent("stderr", "Failed to step into: " + e.getMessage() + "\n");
            }
        }
    }

    public void stepOut(int threadId) {
        if (isPaused && debuggeeProcess != null && debuggeeProcess.isAlive()) {
            try {
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(debuggeeProcess.getOutputStream())
                );
                writer.println("STEP_OUT:" + threadId);
                writer.flush();

                server.sendContinuedEvent(threadId);

            } catch (Exception e) {
                server.sendOutputEvent("stderr", "Failed to step out: " + e.getMessage() + "\n");
            }
        }
    }

    public void pause(int threadId) {
        if (!isPaused && debuggeeProcess != null && debuggeeProcess.isAlive()) {
            try {
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(debuggeeProcess.getOutputStream())
                );
                writer.println("PAUSE:" + threadId);
                writer.flush();

                isPaused = true;
                server.sendStoppedEvent("pause", threadId);

            } catch (Exception e) {
                server.sendOutputEvent("stderr", "Failed to pause: " + e.getMessage() + "\n");
            }
        }
    }

    public void terminate() {
        isRunning = false;

        if (debuggeeProcess != null) {
            debuggeeProcess.destroy();

            // Force kill if needed
            try {
                if (!debuggeeProcess.waitFor(5, TimeUnit.SECONDS)) {
                    debuggeeProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                java.lang.Thread.currentThread().interrupt();
                debuggeeProcess.destroyForcibly();
            }
        }

        if (executorService != null) {
            executorService.shutdown();
        }
    }

    public void restart() {
        terminate();

        // Wait a bit for cleanup
        try {
            java.lang.Thread.sleep(1000);
        } catch (InterruptedException e) {
            java.lang.Thread.currentThread().interrupt();
        }

        // Restart
        executorService = Executors.newCachedThreadPool();
        start();
    }

    public List<Thread> getThreads() {
        List<Thread> result = new ArrayList<>();

        for (TuricumThread thread : threads.values()) {
            Thread t = new Thread();
            t.setName(thread.getName());
            t.setName(thread.getName());
            result.add(t);
        }

        return result;
    }

    public List<StackFrame> getStackTrace(int threadId) {
        List<StackFrame> frames = new ArrayList<>();

        // Get stack frames for the thread
        // This would come from your Turicum runtime's stack trace
        TuricumThread thread = threads.get(threadId);
        if (thread != null && thread.getCurrentFrame() != null) {
            TuricumStackFrame currentFrame = thread.getCurrentFrame();

            StackFrame frame = new StackFrame();
            frame.setId(currentFrame.getId());
            frame.setName(currentFrame.getFunctionName());
            frame.setLine(currentFrame.getLine());
            frame.setColumn(currentFrame.getColumn());

            Source source = new Source();
            source.setName(currentFrame.getSourceName());
            source.setPath(currentFrame.getSourcePath());
            frame.setSource(source);

            frames.add(frame);

            // Add parent frames if available
            TuricumStackFrame parent = currentFrame.getParent();
            while (parent != null) {
                StackFrame parentFrame = new StackFrame();
                parentFrame.setId(parent.getId());
                parentFrame.setName(parent.getFunctionName());
                parentFrame.setLine(parent.getLine());
                parentFrame.setColumn(parent.getColumn());

                Source parentSource = new Source();
                parentSource.setName(parent.getSourceName());
                parentSource.setPath(parent.getSourcePath());
                parentFrame.setSource(parentSource);

                frames.add(parentFrame);
                parent = parent.getParent();
            }
        }

        return frames;
    }

    public List<Variable> getVariables(int variablesReference) {
        List<Variable> variables = new ArrayList<>();

        // Request variables from debuggee process
        if (debuggeeProcess != null && debuggeeProcess.isAlive()) {
            try {
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(debuggeeProcess.getOutputStream())
                );
                writer.println("GET_VARIABLES:" + variablesReference);
                writer.flush();

                // In a real implementation, you'd wait for the response
                // For now, return some mock variables
                Variable var1 = new Variable();
                var1.setName("x");
                var1.setValue("42");
                var1.setType("number");
                var1.setVariablesReference(0);
                variables.add(var1);

                Variable var2 = new Variable();
                var2.setName("message");
                var2.setValue("\"Hello, World!\"");
                var2.setType("string");
                var2.setVariablesReference(0);
                variables.add(var2);

            } catch (Exception e) {
                server.sendOutputEvent("stderr", "Failed to get variables: " + e.getMessage() + "\n");
            }
        }

        return variables;
    }

    public String setVariable(int variablesReference, String name, String value) {
        if (debuggeeProcess != null && debuggeeProcess.isAlive()) {
            try {
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(debuggeeProcess.getOutputStream())
                );
                writer.println("SET_VARIABLE:" + variablesReference + ":" + name + ":" + value);
                writer.flush();

                // In a real implementation, you'd wait for confirmation
                return value;

            } catch (Exception e) {
                server.sendOutputEvent("stderr", "Failed to set variable: " + e.getMessage() + "\n");
            }
        }
        return value;
    }

    public String getSourceContent(Source source) {
        try {
            return Files.readString(Paths.get(source.getPath()));
        } catch (IOException e) {
            return "// Error reading source: " + e.getMessage();
        }
    }

    public String evaluate(String expression, Integer frameId, String context) {
        if (debuggeeProcess != null && debuggeeProcess.isAlive()) {
            try {
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(debuggeeProcess.getOutputStream())
                );
                writer.println("EVALUATE:" + frameId + ":" + context + ":" + expression);
                writer.flush();

                // In a real implementation, you'd wait for the result
                // For now, return a mock evaluation
                return "Evaluation result for: " + expression;

            } catch (Exception e) {
                return "Error evaluating expression: " + e.getMessage();
            }
        }
        return "Debug session not active";
    }

    public List<CompletionItem> getCompletions(String text, int column, Integer frameId) {
        List<CompletionItem> completions = new ArrayList<>();

        // Request completions from debuggee process
        if (debuggeeProcess != null && debuggeeProcess.isAlive()) {
            try {
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(debuggeeProcess.getOutputStream())
                );
                writer.println("GET_COMPLETIONS:" + frameId + ":" + column + ":" + text);
                writer.flush();

                // Mock completions for now
                CompletionItem item1 = new CompletionItem();
                item1.setLabel("variable1");
                item1.setText("variable1");
                item1.setType(CompletionItemType.VARIABLE);
                completions.add(item1);

                CompletionItem item2 = new CompletionItem();
                item2.setLabel("function1()");
                item2.setText("function1()");
                item2.setType(CompletionItemType.FUNCTION);
                completions.add(item2);

            } catch (Exception e) {
                server.sendOutputEvent("stderr", "Failed to get completions: " + e.getMessage() + "\n");
            }
        }

        return completions;
    }

    public List<BreakpointLocation> getBreakpointLocations(Source source, Integer line, Integer endLine) {
        List<BreakpointLocation> locations = new ArrayList<>();

        // For simplicity, assume any line can have a breakpoint
        int startLine = line != null ? line : 1;
        int lastLine = endLine != null ? endLine : startLine;

        for (int i = startLine; i <= lastLine; i++) {
            BreakpointLocation location = new BreakpointLocation();
            location.setLine(i);
            location.setColumn(1);
            locations.add(location);
        }

        return locations;
    }

    private void createInitialStackFrame() {
        TuricumStackFrame frame = new TuricumStackFrame(
                nextFrameId.getAndIncrement(),
                "main",
                program,
                Paths.get(program).getFileName().toString(),
                1,
                1
        );

        stackFrames.put(frame.getId(), frame);

        TuricumThread mainThread = threads.get(mainThreadId);
        if (mainThread != null) {
            mainThread.setCurrentFrame(frame);
        }
    }

    private void updateStackTrace(int threadId, String file, int line) {
        TuricumThread thread = threads.get(threadId);
        if (thread != null) {
            TuricumStackFrame currentFrame = thread.getCurrentFrame();
            if (currentFrame != null) {
                currentFrame.setLine(line);
                currentFrame.setSourcePath(file);
                currentFrame.setSourceName(Paths.get(file).getFileName().toString());
            } else {
                // Create new frame
                TuricumStackFrame frame = new TuricumStackFrame(
                        nextFrameId.getAndIncrement(),
                        "main",
                        file,
                        Paths.get(file).getFileName().toString(),
                        line,
                        1
                );
                stackFrames.put(frame.getId(), frame);
                thread.setCurrentFrame(frame);
            }
        }
    }

}