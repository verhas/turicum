package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.Context;
import ch.turic.memory.NameGen;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * {@code FlowCommand} implements a reactive evaluation block that executes a set of dependent
 * cell computations concurrently using virtual threads. Each cell has an identifier and an
 * associated command expression.
 * <p>
 * The command supports controlled termination via:
 * <ul>
 *   <li>{@code until} — an exit condition evaluated in the shared context</li>
 *   <li>{@code limit} — a maximum number of cell reschedulings</li>
 *   <li>{@code timeout} — a wall-clock duration limit in seconds</li>
 * </ul>
 * Cells are re-evaluated whenever their dependencies change. If a cell produces a different
 * value than before, all cells depending on it are rescheduled for execution.
 * <p>
 * Once all tasks have completed and either the exit condition is met or a hard limit
 * (timeout or task cap) is triggered, the flow terminates. If a {@code yield} expression
 * is present, its result is returned; otherwise, {@code null} is returned.
 * <p>
 * Flow execution is thread-safe and based on {@code CompletableFuture} using virtual threads.
 *
 * @see ch.turic.commands.Command
 * @see ch.turic.memory.Context
 */
public class FlowCommand extends AbstractCommand {
    /**
     * Represents a reactive cell in the flow. A cell consists of an identifier
     * (variable name) and an associated command that computes its value.
     * <p>
     * Cells are re-evaluated automatically when any of their input dependencies change.
     *
     * @param identifier the variable name bound to the cell's value
     * @param command    the command to compute the value of the cell
     */
    private record Cell(String identifier, Command command) {
    }

    /**
     * Wraps a completed cell and its computed result. Used to transport evaluated
     * values across tasks in the flow executor.
     *
     * @param result the evaluated result of the cell
     * @param cell   the original cell whose command produced the result
     */
    private record CellWithResult(Object result, Cell cell) {
    }

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Command exitCondition;
    private final Command limitExpression;
    private final Command timeoutExpression;
    private final Command resultExpression;
    private final Cell[] cells;

    /**
     * Constructs a {@code FlowCommand} with optional termination criteria and a list of reactive cells.
     *
     * @param exitCondition     the command to evaluate the exit condition (may be {@code null})
     * @param limitExpression   the command that evaluates to a long value representing the maximum number of dependent cell executions (may be {@code null} for unlimited)
     * @param timeoutExpression the command that evaluates to a double value representing the maximum allowed execution time in seconds (may be {@code null} for no timeout)
     * @param resultExpression  the expression whose result is returned after flow completes (may be {@code null})
     * @param cellIdentifiers   an array of variable names representing reactive cells
     * @param cellCommands      an array of command expressions corresponding to the cells
     * @throws IllegalArgumentException if the arrays are empty or of unequal length
     * @throws ExecutionException       if the internal dependency analysis fails
     */
    public FlowCommand(Command exitCondition, Command limitExpression, Command timeoutExpression, Command resultExpression, String[] cellIdentifiers, Command[] cellCommands) {
        this.exitCondition = exitCondition;
        this.limitExpression = limitExpression;
        this.timeoutExpression = timeoutExpression;
        this.resultExpression = resultExpression;
        if (cellIdentifiers.length == 0) {
            throw new IllegalArgumentException("Invalid arguments for flow command");
        }
        if (cellCommands.length != cellIdentifiers.length) {
            throw new IllegalArgumentException("Invalid arguments for flow command, different number of identifiers and commands.");
        }
        cells = new Cell[cellIdentifiers.length];
        for (int i = 0; i < cellIdentifiers.length; i++) {
            cells[i] = new Cell(cellIdentifiers[i], cellCommands[i]);
        }
        try {
            dependencyAnalysis();
        } catch (IllegalAccessException e) {
            throw new ExecutionException(e, "Dependency analysis failed");
        }
    }

    private final Map<String, Cell[]> dependentCells = new HashMap<>();

    /**
     * Analyzes dependencies between cells to determine which cells depend on the values
     * of others. This is done by recursively scanning each cell’s command expression for
     * identifier references that match other cell identifiers.
     * <p>
     * The result of this analysis is stored in the {@code dependentCells} map, where
     * each key is a cell identifier, and the value is an array of cells that depend
     * on that identifier. This map is used during execution to determine which cells
     * need to be re-executed when a value changes.
     * <p>
     * Reflection is used to traverse the command graph, and cyclic references are handled gracefully.
     *
     * @throws IllegalAccessException if reflective access to command fields fails
     * @see #getSubCommandsTransitive(Object, Set)
     */
    private void dependencyAnalysis() throws IllegalAccessException {
        final var dependencies = new HashMap<String, List<Cell>>();
        final var stateCellIds = new HashSet<String>();
        for (Cell value : cells) {
            stateCellIds.add(value.identifier);
        }
        for (Cell cell : cells) {
            final var identifiers = getSubCommandsTransitive(cell.command, new HashSet<>())
                    .stream()
                    .filter(c -> c instanceof Identifier)
                    .map(c -> (Identifier) c)
                    .map(Identifier::name)
                    .filter(stateCellIds::contains)
                    .collect(Collectors.toSet());
            for (final var id : identifiers) {
                if (!dependencies.containsKey(id)) {
                    dependencies.put(id, new ArrayList<>());
                }
                dependencies.get(id).add(cell);
            }
        }
        for (final var entry : dependencies.entrySet()) {
            this.dependentCells.put(entry.getKey(), entry.getValue().toArray(Cell[]::new));
        }
    }

    /**
     * Recursively collects all sub-commands reachable from the given command,
     * including the command itself. This traversal is based on reflection and
     * includes any fields that are part of the {@code ch.turic} package.
     * <p>
     * Cycles in the command graph are detected and avoided.
     *
     * @param command         the starting command
     * @param commandsVisited a set used to track visited commands and avoid cycles
     * @return a set of all transitively reachable sub-commands
     * @throws IllegalAccessException if reflective field access fails
     */
    private static Set<Object> getSubCommandsTransitive(Object command, Set<Object> commandsVisited) throws IllegalAccessException {
        // just in case there is a loop, which can only be the case of some retrospect process tricked up the command tree
        if (commandsVisited.contains(command)) {
            return Set.of();
        }
        commandsVisited.add(command);
        final var fields = new HashSet<>();
        fields.add(command);
        for (var field : getSubCommands(command)) {
            fields.addAll(getSubCommandsTransitive(field, commandsVisited));
        }
        return fields;
    }

    /**
     * Returns the immediate sub-commands of a given command object by inspecting
     * its declared fields. Only fields that belong to the {@code ch.turic} package
     * are considered (including arrays of such types).
     * <p>
     * This method is used during dependency analysis to find identifier references.
     *
     * @param command the command whose fields are to be examined
     * @return a set of direct sub-command objects
     * @throws IllegalAccessException if reflective access to the fields fails
     */
    private static Set<Object> getSubCommands(Object command) throws IllegalAccessException {
        final var fields = new HashSet<>();
        for (final var f : command.getClass().getDeclaredFields()) {
            if (!f.isSynthetic() &&
                    (f.getType().getPackageName().startsWith("ch.turic") ||
                            (f.getType().isArray() && f.getType().getComponentType().getPackageName().startsWith("ch.turic"))
                    )) {
                if (f.getType().isArray()) {
                    f.setAccessible(true);
                    final var array = f.get(command);
                    if (array != null) {
                        final int length = Array.getLength(array);
                        for (int i = 0; i < length; i++) {
                            fields.add(Array.get(array, i));
                        }
                    }
                } else {
                    f.setAccessible(true);
                    final var value = f.get(command);
                    if (value != null) {
                        fields.add(value);
                    }
                }
            }
        }
        return fields;
    }

    /**
     * Executes the flow command using the given context. The execution starts with the first defined cell
     * and proceeds by spawning tasks for dependent cells whenever a value changes.
     * <p>
     * The execution will stop under one of the following conditions:
     * <ul>
     *   <li>The exit condition (if present) evaluates to {@code true}</li>
     *   <li>The task limit is reached (if {@code limitExpression} is provided)</li>
     *   <li>The execution timeout is exceeded (if {@code timeoutExpression} is provided)</li>
     * </ul>
     * <p>
     * If a {@code yield} expression is defined, its result is returned after flow completion.
     * If no {@code yield} is defined, {@code null} is returned.
     *
     * @param ctx the context in which to evaluate the command
     * @return the result of the {@code yield} expression or {@code null}
     * @throws ExecutionException if the execution fails, the task limit is exceeded, or the timeout is hit
     */
    @Override
    public Object _execute(Context ctx) throws ExecutionException {
        final var exception = new AtomicReference<Exception>(null);
        long totalScheduled = 0;
        boolean doExit = false;
        ExecutionException doException = null;
        long limit;
        if (limitExpression == null) {
            limit = -1;
        } else {
            limit = Cast.toLong(limitExpression.execute(ctx));
        }
        final long timeout;
        if (timeoutExpression != null) {
            final var dT = Cast.toDouble(timeoutExpression.execute(ctx));
            timeout = Double.valueOf(1_000_000_000 * dT).longValue();
        } else {
            timeout = -1;
        }
        final long startTime = System.nanoTime();
        // start with cell[0]
        final var startTask = startTask(ctx, cells[0], exception);
        // add the future to the set of running futures
        final var tasksRunning = new HashSet<CompletableFuture<CellWithResult>>();
        tasksRunning.add(startTask);
        try {
            while (!tasksRunning.isEmpty()) {
                // we wait until at least one is done
                CompletableFuture.anyOf(tasksRunning.toArray(CompletableFuture[]::new)).join();
                final long currentTime = System.nanoTime();
                if (timeout >= 0 && timeout <= currentTime - startTime) {
                    doExit = true;
                    doException = new ExecutionException("Timed out after " + timeout / 1_000_000 + " ms");
                }
                while (true) {
                    if (exception.get() != null) {
                        throw new ExecutionException(exception.get(), "Exception while executing flow.");
                    }
                    // get one task that is ready
                    final var task = tasksRunning.stream().filter(CompletableFuture::isDone).findAny();
                    if (task.isEmpty()) {
                        break;
                    }
                    tasksRunning.remove(task.get());
                    final var cnR = task.get().get();
                    // do not schedule new tasks if we started to exit or start to exit now
                    if (!doExit && !(doExit = isExitConditionMet(ctx))) {
                        if (cnR != null) {
                            final var nr = scheduleNewTasks(ctx, cnR, tasksRunning, exception);
                            totalScheduled += nr;
                            if (limit >= 0) {
                                if (nr >= limit) {
                                    doExit = true;
                                    doException = new ExecutionException("Task limit has been reached in flow command after %d tasks.", totalScheduled);
                                } else {
                                    limit -= nr;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new ExecutionException(e, "There was an exception while executing flow command");
        }
        if (doException != null) {
            throw doException;
        }
        if (resultExpression != null) {
            return resultExpression.execute(ctx);
        } else {
            return null;
        }
    }

    /**
     * Safely evaluates the {@code exitCondition} using the provided context.
     * Returns {@code false} if the condition is {@code null} or throws an exception during evaluation.
     *
     * @param ctx the context to use for evaluation
     * @return {@code true} if the exit condition evaluates to true; {@code false} otherwise
     */
    private boolean isExitConditionMet(Context ctx) {
        if (exitCondition == null) {
            return false;
        }
        try {
            return Cast.toBoolean(exitCondition.execute(ctx));
        } catch (ExecutionException e) {
            return false;
        }
    }

    /**
     * Schedules new tasks for all dependent cells of the given cell, if its result differs from
     * the previously stored value in the context.
     * <p>
     * Only dependent cells are scheduled, and the new value is updated in the local context.
     * The method returns the number of new tasks that were started.
     *
     * @param ctx          the context for evaluation
     * @param cnR          the cell with its evaluated result
     * @param tasksRunning the current set of running tasks to which new ones will be added
     * @param exception    a shared reference to report any thrown exception from tasks
     * @return the number of new tasks scheduled
     */
    private int scheduleNewTasks(Context ctx, CellWithResult cnR, HashSet<CompletableFuture<CellWithResult>> tasksRunning, AtomicReference<Exception> exception) {
        int counter = 0;
        final var oldValue = ctx.getLocal(cnR.cell.identifier);
        if ((oldValue == null && cnR.result != null) || (oldValue != null && !oldValue.equals(cnR.result))) {
            ctx.let0(cnR.cell.identifier, cnR.result);
            final var dCells = this.dependentCells.get(cnR.cell.identifier);
            if (dCells != null) {
                for (final var cell : dCells) {
                    tasksRunning.add(startTask(ctx, cell, exception));
                    counter++;
                }
            }
        }
        return counter;
    }


    private CompletableFuture<CellWithResult> startTask(Context ctx, Cell cell, AtomicReference<Exception> exception) {
        final var newThreadContext = ctx.thread();
        copyVariables(ctx, newThreadContext);
        return CompletableFuture.supplyAsync(() -> {
            Thread.currentThread().setName(NameGen.generateName());
            try {
                return new CellWithResult(cell.command.execute(newThreadContext), cell);
            } catch (Exception t) {
                exception.compareAndSet(null, t);
                return null;
            }
        }, executor);
    }

    private static void copyVariables(Context source, Context target) {
        for (final var key : source.keys()) {
            target.let0(key, source.get(key));
            target.freeze(key);
        }
    }
}
