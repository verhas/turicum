package ch.turic.dap;

// Main Debug Adapter Server
import org.eclipse.lsp4j.debug.Module;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Debug Adapter Protocol server implementation for the Turicum language
 */
public class TuricumDebugServer implements IDebugProtocolServer {

    private IDebugProtocolClient client;
    private final AtomicInteger nextVariableReference = new AtomicInteger(1);
    private final Map<Integer, Object> variables = new ConcurrentHashMap<>();
    private final Map<String, List<SourceBreakpoint>> breakpoints = new ConcurrentHashMap<>();
    private boolean isRunning = false;
    private TuricumDebugSession debugSession;

    public void connect(IDebugProtocolClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
        Capabilities capabilities = new Capabilities();

        // Set supported features
        capabilities.setSupportsConfigurationDoneRequest(true);
        capabilities.setSupportsEvaluateForHovers(true);
        capabilities.setSupportsStepBack(false);
        capabilities.setSupportsSetVariable(true);
        capabilities.setSupportsRestartFrame(false);
        capabilities.setSupportsGotoTargetsRequest(false);
        capabilities.setSupportsStepInTargetsRequest(false);
        capabilities.setSupportsCompletionsRequest(true);
        capabilities.setSupportsModulesRequest(false);
        capabilities.setSupportsRestartRequest(true);
        capabilities.setSupportsExceptionOptions(false);
        capabilities.setSupportsValueFormattingOptions(true);
        capabilities.setSupportsExceptionInfoRequest(false);
        capabilities.setSupportTerminateDebuggee(true);
        capabilities.setSupportsDelayedStackTraceLoading(false);
        capabilities.setSupportsLoadedSourcesRequest(false);
        capabilities.setSupportsLogPoints(false);
        capabilities.setSupportsTerminateThreadsRequest(false);
        capabilities.setSupportsSetExpression(false);
        capabilities.setSupportsTerminateRequest(true);
        capabilities.setSupportsDataBreakpoints(false);
        capabilities.setSupportsReadMemoryRequest(false);
        capabilities.setSupportsDisassembleRequest(false);
        capabilities.setSupportsCancelRequest(false);
        capabilities.setSupportsBreakpointLocationsRequest(true);
        capabilities.setSupportsClipboardContext(false);

        // Exception breakpoint filters
        ExceptionBreakpointsFilter[] exceptionFilters = new ExceptionBreakpointsFilter[0];
        capabilities.setExceptionBreakpointFilters(exceptionFilters);

        return CompletableFuture.completedFuture(capabilities);
    }

    @Override
    public CompletableFuture<Void> configurationDone(ConfigurationDoneArguments args) {
        // Configuration is complete, debugger is ready to start
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> launch(Map<String, Object> args) {
        // Extract launch configuration
        String program = (String) args.get("program");
        Boolean stopOnEntry = (Boolean) args.getOrDefault("stopOnEntry", false);
        String[] arguments = (String[]) args.getOrDefault("args", new String[0]);

        try {
            // Initialize debug session
            debugSession = new TuricumDebugSession(program, arguments, this);

            // Send initialized event
            client.initialized();

            if (stopOnEntry) {
                // Stop at entry point
                debugSession.stopAtEntry();
            } else {
                // Continue execution
                debugSession.start();
            }

            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<Void> attach(Map<String, Object> args) {
        // Attach to running Turicum process
        String processId = (String) args.get("processId");
        // Implementation depends on how you want to attach to processes
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> restart(RestartArguments args) {
        if (debugSession != null) {
            debugSession.restart();
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> disconnect(DisconnectArguments args) {
        if (debugSession != null) {
            debugSession.terminate();
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> terminate(TerminateArguments args) {
        if (debugSession != null) {
            debugSession.terminate();
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
        String sourcePath = args.getSource().getPath();
        List<SourceBreakpoint> requestedBreakpoints = Arrays.asList(args.getBreakpoints());

        // Store breakpoints
        breakpoints.put(sourcePath, requestedBreakpoints);

        // Create response with actual breakpoints
        Breakpoint[] actualBreakpoints = requestedBreakpoints.stream()
                .map(bp -> {
                    Breakpoint actualBp = new Breakpoint();
                    actualBp.setVerified(true);
                    actualBp.setLine(bp.getLine());
                    actualBp.setSource(args.getSource());
                    return actualBp;
                })
                .toArray(Breakpoint[]::new);

        SetBreakpointsResponse response = new SetBreakpointsResponse();
        response.setBreakpoints(actualBreakpoints);

        // Notify debug session about breakpoints
        if (debugSession != null) {
            debugSession.updateBreakpoints(sourcePath, requestedBreakpoints);
        }

        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<SetFunctionBreakpointsResponse> setFunctionBreakpoints(SetFunctionBreakpointsArguments args) {
        // Implementation for function breakpoints
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<SetExceptionBreakpointsResponse> setExceptionBreakpoints(SetExceptionBreakpointsArguments args) {
        // Implementation for exception breakpoints
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<DataBreakpointInfoResponse> dataBreakpointInfo(DataBreakpointInfoArguments args) {
        // Implementation for data breakpoints
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<SetDataBreakpointsResponse> setDataBreakpoints(SetDataBreakpointsArguments args) {
        // Implementation for data breakpoints
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
        if (debugSession != null) {
            debugSession.continue_();
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> next(NextArguments args) {
        if (debugSession != null) {
            debugSession.next(args.getThreadId());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stepIn(StepInArguments args) {
        if (debugSession != null) {
            debugSession.stepIn(args.getThreadId());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stepOut(StepOutArguments args) {
        if (debugSession != null) {
            debugSession.stepOut(args.getThreadId());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> stepBack(StepBackArguments args) {
        // Step back not supported
        return CompletableFuture.failedFuture(new RuntimeException("Step back not supported"));
    }

    @Override
    public CompletableFuture<Void> reverseContinue(ReverseContinueArguments args) {
        // Reverse continue not supported
        return CompletableFuture.failedFuture(new RuntimeException("Reverse continue not supported"));
    }

    @Override
    public CompletableFuture<Void> restartFrame(RestartFrameArguments args) {
        // Restart frame not supported
        return CompletableFuture.failedFuture(new RuntimeException("Restart frame not supported"));
    }

    @Override
    public CompletableFuture<Void> goto_(GotoArguments args) {
        // Goto not supported
        return CompletableFuture.failedFuture(new RuntimeException("Goto not supported"));
    }

    @Override
    public CompletableFuture<Void> pause(PauseArguments args) {
        if (debugSession != null) {
            debugSession.pause(args.getThreadId());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
        if (debugSession == null) {
            return CompletableFuture.completedFuture(new StackTraceResponse());
        }

        List<StackFrame> frames = debugSession.getStackTrace(args.getThreadId());
        StackTraceResponse response = new StackTraceResponse();
        response.setStackFrames(frames.toArray(new StackFrame[0]));
        response.setTotalFrames(frames.size());

        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
        List<Scope> scopes = new ArrayList<>();

        // Local variables scope
        Scope localScope = new Scope();
        localScope.setName("Local");
        localScope.setVariablesReference(nextVariableReference.getAndIncrement());
        localScope.setExpensive(false);
        scopes.add(localScope);

        // Global variables scope
        Scope globalScope = new Scope();
        globalScope.setName("Global");
        globalScope.setVariablesReference(nextVariableReference.getAndIncrement());
        globalScope.setExpensive(false);
        scopes.add(globalScope);

        ScopesResponse response = new ScopesResponse();
        response.setScopes(scopes.toArray(new Scope[0]));

        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<VariablesResponse> variables(VariablesArguments args) {
        List<Variable> vars = new ArrayList<>();

        if (debugSession != null) {
            vars = debugSession.getVariables(args.getVariablesReference());
        }

        VariablesResponse response = new VariablesResponse();
        response.setVariables(vars.toArray(new Variable[0]));

        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<SetVariableResponse> setVariable(SetVariableArguments args) {
        SetVariableResponse response = new SetVariableResponse();

        if (debugSession != null) {
            String newValue = debugSession.setVariable(
                    args.getVariablesReference(),
                    args.getName(),
                    args.getValue()
            );
            response.setValue(newValue);
        }

        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<SourceResponse> source(SourceArguments args) {
        SourceResponse response = new SourceResponse();

        if (debugSession != null) {
            String sourceContent = debugSession.getSourceContent(args.getSource());
            response.setContent(sourceContent);
        }

        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<ThreadsResponse> threads() {
        ThreadsResponse response = new ThreadsResponse();

        if (debugSession != null) {
            List<Thread> threads = debugSession.getThreads();
            response.setThreads(threads.toArray(new Thread[0]));
        } else {
            response.setThreads(new Thread[0]);
        }

        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<Void> terminateThreads(TerminateThreadsArguments args) {
        // Terminate threads not supported
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ModulesResponse> modules(ModulesArguments args) {
        // Modules not supported
        ModulesResponse response = new ModulesResponse();
        response.setModules(new Module[0]);
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<LoadedSourcesResponse> loadedSources(LoadedSourcesArguments args) {
        // Loaded sources not supported
        LoadedSourcesResponse response = new LoadedSourcesResponse();
        response.setSources(new Source[0]);
        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<EvaluateResponse> evaluate(EvaluateArguments args) {
        EvaluateResponse response = new EvaluateResponse();

        if (debugSession != null) {
            String result = debugSession.evaluate(
                    args.getExpression(),
                    args.getFrameId(),
                    args.getContext()
            );
            response.setResult(result);
            response.setVariablesReference(0); // No sub-variables for now
        } else {
            response.setResult("Debug session not active");
        }

        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<StepInTargetsResponse> stepInTargets(StepInTargetsArguments args) {
        // Step in targets not supported
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<GotoTargetsResponse> gotoTargets(GotoTargetsArguments args) {
        // Goto targets not supported
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<CompletionsResponse> completions(CompletionsArguments args) {
        CompletionsResponse response = new CompletionsResponse();

        if (debugSession != null) {
            List<CompletionItem> items = debugSession.getCompletions(
                    args.getText(),
                    args.getColumn(),
                    args.getFrameId()
            );
            response.setTargets(items.toArray(new CompletionItem[0]));
        }

        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<ExceptionInfoResponse> exceptionInfo(ExceptionInfoArguments args) {
        // Exception info not supported
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<ReadMemoryResponse> readMemory(ReadMemoryArguments args) {
        // Read memory not supported
        return CompletableFuture.failedFuture(new RuntimeException("Read memory not supported"));
    }

    @Override
    public CompletableFuture<DisassembleResponse> disassemble(DisassembleArguments args) {
        // Disassemble not supported
        return CompletableFuture.failedFuture(new RuntimeException("Disassemble not supported"));
    }

    @Override
    public CompletableFuture<BreakpointLocationsResponse> breakpointLocations(BreakpointLocationsArguments args) {
        BreakpointLocationsResponse response = new BreakpointLocationsResponse();

        if (debugSession != null) {
            List<BreakpointLocation> locations = debugSession.getBreakpointLocations(
                    args.getSource(),
                    args.getLine(),
                    args.getEndLine()
            );
            response.setBreakpoints(locations.toArray(new BreakpointLocation[0]));
        }

        return CompletableFuture.completedFuture(response);
    }

    @Override
    public CompletableFuture<SetExpressionResponse> setExpression(SetExpressionArguments args) {
        // Set expression not supported
        return CompletableFuture.failedFuture(new RuntimeException("Set expression not supported"));
    }

    // Client notification methods
    public IDebugProtocolClient getClient() {
        return client;
    }

    public void sendStoppedEvent(String reason, int threadId) {
        if (client != null) {
            StoppedEventArguments args = new StoppedEventArguments();
            args.setReason(reason);
            args.setThreadId(threadId);
            client.stopped(args);
        }
    }

    public void sendContinuedEvent(int threadId) {
        if (client != null) {
            ContinuedEventArguments args = new ContinuedEventArguments();
            args.setThreadId(threadId);
            client.continued(args);
        }
    }

    public void sendTerminatedEvent() {
        if (client != null) {
            client.terminated(new TerminatedEventArguments());
        }
    }

    public void sendExitedEvent(int exitCode) {
        if (client != null) {
            ExitedEventArguments args = new ExitedEventArguments();
            args.setExitCode(exitCode);
            client.exited(args);
        }
    }

    public void sendThreadEvent(String reason, int threadId) {
        if (client != null) {
            ThreadEventArguments args = new ThreadEventArguments();
            args.setReason(reason);
            args.setThreadId(threadId);
            client.thread(args);
        }
    }

    public void sendOutputEvent(String category, String output) {
        if (client != null) {
            OutputEventArguments args = new OutputEventArguments();
            args.setCategory(category);
            args.setOutput(output);
            client.output(args);
        }
    }
}