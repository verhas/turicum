package ch.turic.commands;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.Context;
import ch.turic.memory.NameGen;
import ch.turic.memory.Sentinel;
import ch.turic.utils.Unmarshaller;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * {@code FlowCommand} implements a reactive evaluation block that executes a set of dependent
 * cell computations concurrently using virtual threads. Each cell has an id and an
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
 * @see Command
 * @see ch.turic.memory.Context
 */
public class FlowCommand extends AbstractCommand {
    /**
     * Represents a reactive cell in the flow. A cell consists of an id
     * (variable name) and an associated command that computes its value.
     * <p>
     * Cells are re-evaluated automatically when any of their input dependencies change.
     *
     * @param id      the variable name bound to the cell's value
     * @param command the command to compute the value of the cell
     */
    record Cell(String id, Command command) {
        public static Cell factory(Unmarshaller.Args args) {
            return new Cell(args.str("id"), args.command("command"));
        }
    }

    /**
     * Wraps a completed cell and its computed result. Used to transport evaluated
     * values across tasks in the flow executor.
     *
     * @param result the evaluated result of the cell
     * @param cell   the original cell whose command produced the result
     */
    record CellWithResult(Object result, Cell cell, Long counter) {
        public static CellWithResult factory(Unmarshaller.Args args) {
            return new CellWithResult(args.get("result", Object.class),
                    args.get("cell", Cell.class),
                    args.get("counter", Long.class));
        }
    }

    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final String flowId;
    private final Command exitCondition;
    private final Command limitExpression;
    private final Command timeoutExpression;
    private final Command resultExpression;
    private final Cell[] cells;
    private final Cell[] startCells;
    private final Map<String, Cell[]> dependentCells;


    private FlowCommand(
            final String flowId,
            final Command exitCondition,
            final Command limitExpression,
            final Command timeoutExpression,
            final Command resultExpression,
            final Cell[] cells,
            final Cell[] startCells,
            final Map<String, Cell[]> dependentCells) {
        this.flowId = flowId;
        this.exitCondition = exitCondition;
        this.limitExpression = limitExpression;
        this.timeoutExpression = timeoutExpression;
        this.resultExpression = resultExpression;
        this.cells = cells;
        this.startCells = startCells;
        this.dependentCells = dependentCells;
    }

    public static FlowCommand factory(final Unmarshaller.Args args) {
        return new FlowCommand(
                args.str("flowId"),
                args.command("exitCondition"),
                args.command("limitExpression"),
                args.command("timeoutExpression"),
                args.command("resultExpression"),
                args.get("cells", Cell[].class),
                args.get("startCells", Cell[].class),
                args.get("dependentCells", Map.class)
        );
    }


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
    public FlowCommand(String flowId, Command exitCondition, Command limitExpression, Command timeoutExpression, Command resultExpression, String[] cellIdentifiers, Command[] cellCommands) {
        this.dependentCells = new HashMap<>();
        this.flowId = Objects.requireNonNullElse(flowId, "#unnamed");
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
            startCells = determineEntryCells();
            validateSchedulingOrderFromDependencies();
            final var untouched = getUnreachableCells();
            if (untouched.length != 0) {
                final var sb = new StringBuilder("There are cells in flow '%s' which have no effect:\n".formatted(this.flowId));
                for (final var ut : untouched) {
                    sb.append("%s, ".formatted(ut.id));
                }
                throw new ExecutionException(sb.substring(0, sb.length() - 2));
            }
        } catch (IllegalAccessException e) {
            throw new ExecutionException(e, "Dependency analysis failed on flow '%s'", flowId);
        }
    }

    /**
     * Analyzes dependencies between cells to determine which cells depend on the values
     * of others. This is done by recursively scanning each cell’s command expression for
     * id references that match other cell identifiers.
     * <p>
     * The result of this analysis is stored in the {@code dependentCells} map, where
     * each key is a cell id, and the value is an array of cells that depend
     * on that id. This map is used during execution to determine which cells
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
            stateCellIds.add(value.id);
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

    private Cell[] determineEntryCells() {
        // Collect all cells that are depended upon some other cells
        Set<Cell> dependedUpon = new HashSet<>();
        for (Cell[] deps : dependentCells.values()) {
            dependedUpon.addAll(Arrays.asList(deps));
        }

        // Cells not depended upon by any others are starting points
        List<Cell> startCells = new ArrayList<>();
        for (Cell cell : cells) {
            if (!dependedUpon.contains(cell)) {
                startCells.add(cell);
            }
        }
        return startCells.toArray(Cell[]::new);
    }

    private void validateSchedulingOrderFromDependencies() throws ExecutionException {
        if (dependentCells.isEmpty()) {
            throw new RuntimeException("There are no dependencies defined for this flow command. It is an internal error.");
        }

        final var dependencyMap = buildDepencencyMap();

        // Check for each cell if its dependencies are satisfiable in some order
        for (Cell cell : cells) {
            Set<String> alreadyChecked = new HashSet<>();
            final var failurePath = dependencyMissing(cell.id, dependencyMap, alreadyChecked, new HashSet<>(), Arrays.stream(startCells).map(c -> c.id).collect(Collectors.toSet()));
            if (!failurePath.isEmpty()) {
                throw new ExecutionException("Invalid flow '%s': cell '%s' may depend on undefined state due to cyclic or misordered dependencies.\n" +
                        "[ %s ]", flowId, cell.id, String.join(" <- ", failurePath));
            }
        }
    }

    private HashMap<String, Set<String>> buildDepencencyMap() {
        final var dependencyMap = new HashMap<String, Set<String>>();
        for (final var entry : dependentCells.entrySet()) {
            String dependedOn = entry.getKey();
            for (Cell dependent : entry.getValue()) {
                dependencyMap.computeIfAbsent(dependent.id, k -> new HashSet<>()).add(dependedOn);
            }
        }
        return dependencyMap;
    }

    /**
     * Recursively checks if a cell's dependencies can be satisfied by tracing back to start cells.
     * This method detects cyclic dependencies and validates that all cells can be computed
     * from initial values.
     * <p>
     * The algorithm works by first ensuring all cells are reachable from start cells in
     * {@link #getUnreachableCells()}. This guarantees that every cell can eventually be
     * computed from initial values. Then this method looks for cycles that don't include
     * start cells. Such cycles are problematic because:
     * <ul>
     *   <li>If a cycle includes a start cell, the cycle can be broken since the start cell
     *       provides an initial value.</li>
     *   <li>If a cycle doesn't include a start cell, the cells in the cycle may have a path to
     *       start executing without getting their initial values.</li>
     * </ul>
     * Therefore, finding any cycle that excludes start cells indicates an invalid flow.
     * <p>
     * Without this analysis such flows can randomly throw undefined variable exceptions based on scheduling order.
     *
     * @param id             the identifier of the cell being checked
     * @param depMap         mapping of cell ids to their direct dependencies
     * @param alreadyChecked set of cells that have already been verified as valid
     * @param path           tracks the current dependency path to detect cycles
     * @param startCells     set of initial cell ids that have no dependencies
     * @return empty list if dependencies are satisfied, or a list containing the problematic
     * dependency path if validation fails (with the problematic cell ids in reverse order)
     */
    private List<String> dependencyMissing(String id,
                                           Map<String, Set<String>> depMap,
                                           Set<String> alreadyChecked,
                                           Set<String> path,
                                           Set<String> startCells) {
        if (startCells.contains(id)) return List.of();
        if (alreadyChecked.contains(id)) return List.of();
        if (!depMap.containsKey(id)) return List.of(); // no dependencies
        if (!path.add(id)) return new ArrayList<>(List.of(id)); // cycle detected

        for (String dep : depMap.get(id)) {
            final var list = dependencyMissing(dep, depMap, alreadyChecked, path, startCells);
            if (!list.isEmpty()) {
                list.add(id);
                return list;
            }
        }
        path.remove(id);
        alreadyChecked.add(id);
        return List.of();
    }

    private long nextCounter(String id, Map<String, Long> counters) {
        return counters.computeIfAbsent(id, k -> 0L);
    }

    /**
     * Find the cells that are not initial cells, because they depend on each other, probably in a cyclic way, but they
     * do not directly or transitively depend on the initial start cells.
     * <p>
     * If there is such a cell, that is an error, and the returned cell array will be used by the caller to create an
     * exception.
     *
     * @return the array of cells that are unreachable.
     */
    private Cell[] getUnreachableCells() {
        // Get all cells reachable from start cells
        final var reachableCells = new HashSet<Cell>();
        final var toVisit = new LinkedList<>(Arrays.asList(startCells));

        while (!toVisit.isEmpty()) {
            final var current = toVisit.poll();
            if (reachableCells.add(current)) {
                final var dependents = dependentCells.get(current.id);
                if (dependents != null) {
                    toVisit.addAll(Arrays.asList(dependents));
                }
            }
        }

        // Find cells that are not reachable
        return Arrays.stream(cells)
                .filter(cell -> !reachableCells.contains(cell))
                .toArray(Cell[]::new);
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
        for (final var field : getSubCommands(command)) {
            fields.addAll(getSubCommandsTransitive(field, commandsVisited));
        }
        return fields;
    }

    /**
     * Returns the immediate sub-commands of a given command object by inspecting
     * its declared fields. Only fields that belong to the {@code ch.turic} package
     * are considered (including arrays of such types).
     * <p>
     * This method is used during dependency analysis to find id references.
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
     * @param context the context in which to evaluate the command
     * @return the result of the {@code yield} expression or {@code null}
     * @throws ExecutionException if the execution fails, the task limit is exceeded, or the timeout is hit
     */
    @Override
    public Object _execute(Context context) throws ExecutionException {
        final var ctx = context.wrap();
        final var stateCounters = new HashMap<String, Long>();
        final var stoppedCells = new HashSet<Cell>();
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

        try {
            final var tasksRunning = new HashSet<CompletableFuture<CellWithResult>>();
            for (final var startCell : startCells) {
                final var startTask = startTask(ctx, startCell, exception, nextCounter(startCell.id, stateCounters));
                tasksRunning.add(startTask);
            }
            updateAndScheduleStart(ctx, tasksRunning, stateCounters, exception, stoppedCells);

            while (!tasksRunning.isEmpty()) {
                // we wait until at least one is done
                CompletableFuture.anyOf(tasksRunning.toArray(CompletableFuture[]::new)).join();
                final long currentTime = System.nanoTime();
                if (timeout >= 0 && timeout <= currentTime - startTime) {
                    doExit = true;
                    doException = new ExecutionException("Flow '%s' timed out after %s ms", flowId, timeout / 1_000_000);
                }
                while (true) {
                    final var e = exception.get();
                    if (e != null) {
                        if (e instanceof ExecutionException ee) {
                            throw ee;
                        } else {
                            throw new ExecutionException(exception.get(), "Exception while executing flow '%s'.", flowId);
                        }
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
                            if (cnR.result == Sentinel.FINI) {
                                stoppedCells.add(cnR.cell);
                                updateStateCounter(ctx, cnR, stateCounters);
                            } else {
                                if (cnR.result != Sentinel.NON_MUTAT) {
                                    final var nr = updateAndScheduleNewTasks(ctx, cnR, tasksRunning, exception, stateCounters, stoppedCells);
                                    totalScheduled += nr;
                                    if (limit >= 0) {
                                        if (nr >= limit) {
                                            doExit = true;
                                            doException = new ExecutionException("Task limit has been reached in flow '%s' command after %d tasks.", flowId, totalScheduled);
                                        } else {
                                            limit -= nr;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (ExecutionException e) {
            final var newException = new ExecutionException("While in flow '%s': %s", flowId, e.getMessage());
            newException.setStackTrace(e.getStackTrace());
            throw newException;
        } catch (Exception e) {
            throw new ExecutionException(e, "There was an exception while executing the flow '%s'", flowId);
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
     * Updates the context with results from initial tasks and schedules dependent tasks.
     * This method handles the startup phase of flow execution by:
     * <ol>
     *   <li>Waiting for all initial tasks to complete</li>
     *   <li>Collecting update operations that need to be performed</li>
     *   <li>Executing updates and scheduling dependent tasks in a controlled manner</li>
     * </ol>
     * <p>
     * The method uses a two-phase approach where updates are first collected into
     * {@code newTasksSchedulers} and then executed.
     * This ensures that all the initial cell state variables are updated when the first dependent task starts.
     *
     * @param ctx           the shared execution context
     * @param tasksRunning  set of currently executing tasks
     * @param stateCounters map tracking the version of each cell's state
     * @param exception     shared reference for propagating exceptions
     * @param stoppedCells  set of cells that have completed execution
     * @throws InterruptedException                    if task execution is interrupted
     * @throws java.util.concurrent.ExecutionException if a task fails
     */
    private void updateAndScheduleStart(Context ctx,
                                        HashSet<CompletableFuture<CellWithResult>> tasksRunning,
                                        HashMap<String, Long> stateCounters,
                                        AtomicReference<Exception> exception,
                                        HashSet<Cell> stoppedCells
    ) throws InterruptedException, java.util.concurrent.ExecutionException {
        // wait for all the start tasks to finish before let the hell get loose
        CompletableFuture.allOf(tasksRunning.toArray(CompletableFuture[]::new)).join();
        final var newTasksSchedulers = new ArrayList<Runnable>();
        for (final var task : tasksRunning) {
            final var cnR = task.get();
            if (exception.get() != null) {
                return;
            }
            final var updated = updateCellVariable(ctx, cnR, stateCounters);
            if (updated) {
                newTasksSchedulers.add(() -> scheduleNewTasks(ctx, cnR, tasksRunning, exception, stateCounters, stoppedCells));
            } else {
                throw new ExecutionException("Updating initial value '%s' failed. Probably double defined in initial state in flow '%s'", cnR.cell.id, flowId);
            }
        }
        for (final var schedule : newTasksSchedulers) {
            schedule.run();
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
     * Schedules new tasks for all dependent cells of the given cell if its result differs from
     * the previously stored value in the context.
     * <p>
     * This method also updates the cell state variable.
     * <p>
     * Only dependent cells are scheduled, and the new value is updated in the local context.
     * The method returns the number of new tasks that were started.
     *
     * @param ctx           the context for evaluation
     * @param cnR           the cell with its evaluated result
     * @param tasksRunning  the current set of running tasks to which new ones will be added
     * @param exception     a shared reference to report any thrown exception from tasks
     * @param stateCounters the state variable counters as they currently are
     * @param stoppedCells  is the set of cells that have returned fini signaling that they are done.
     * @return the number of new tasks scheduled
     */
    private int updateAndScheduleNewTasks(Context ctx, CellWithResult cnR, HashSet<CompletableFuture<CellWithResult>> tasksRunning, AtomicReference<Exception> exception, Map<String, Long> stateCounters, HashSet<Cell> stoppedCells) {
        final var updated = updateCellVariable(ctx, cnR, stateCounters);
        if (updated) {
            return scheduleNewTasks(ctx, cnR, tasksRunning, exception, stateCounters, stoppedCells);
        } else {
            return 0;
        }
    }


    /**
     * Schedules new computation tasks for all cells that depend on the given cell's value.
     * Only cells that have not stopped (not returned FINI) will be scheduled for execution.
     *
     * @param ctx           the context in which tasks will be executed
     * @param cnR           the cell with its newly computed result that may trigger dependent cells
     * @param tasksRunning  the set of currently running tasks to which new tasks will be added
     * @param exception     shared reference for propagating exceptions from tasks
     * @param stateCounters map tracking the version/counter of each cell's state
     * @param stoppedCells  set of cells that have signaled completion and should not be rescheduled
     * @return the number of new tasks that were scheduled
     */
    private int scheduleNewTasks(Context ctx,
                                 CellWithResult cnR,
                                 HashSet<CompletableFuture<CellWithResult>> tasksRunning,
                                 AtomicReference<Exception> exception,
                                 Map<String, Long> stateCounters,
                                 HashSet<Cell> stoppedCells) {
        int newTasksScheduled = 0;
        final var dCells = this.dependentCells.get(cnR.cell.id);
        if (dCells != null) {
            for (final var cell : dCells) {
                if (!stoppedCells.contains(cell)) {
                    tasksRunning.add(startTask(ctx, cell, exception, nextCounter(cell.id, stateCounters)));
                    newTasksScheduled++;
                }
            }
        }
        return newTasksScheduled;
    }

    /**
     * Updates the cell variable in the context if the calculated value is new and not stale.
     * A value is considered new if it differs from the current value in the context.
     * A value is considered stale if the cell's state counter has changed since the calculation started.
     *
     * @param ctx           the context in which to update the variable
     * @param cnR           the cell with its newly calculated result
     * @param stateCounters map tracking the version/counter of each cell's state
     * @return {@code true} if the value was updated in the context; {@code false} if the value was unchanged or stale
     */
    private boolean updateCellVariable(Context ctx, CellWithResult cnR, Map<String, Long> stateCounters) {
        final var oldValue = ctx.getLocal(cnR.cell.id);
        final var updated = (!ctx.contains0(cnR.cell.id) || calculatedValueIsNew(cnR, oldValue)) && notStale(cnR, stateCounters);
        if (updated) {
            saveState(ctx, cnR, stateCounters);
        }
        return updated;
    }

    /**
     * Save the new value for the state and update the counter increasing it.
     *
     * @param ctx           the context for evaluation
     * @param cnR           the cell with its evaluated result
     * @param stateCounters the state variable counters as they currently are
     */
    private void saveState(Context ctx, CellWithResult cnR, Map<String, Long> stateCounters) {
        ctx.let0(cnR.cell.id, cnR.result);
        updateStateCounter(ctx, cnR, stateCounters);
    }

    private void updateStateCounter(Context ctx, CellWithResult cnR, Map<String, Long> stateCounters) {
        stateCounters.computeIfPresent(cnR.cell.id, (k, v) -> v + 1);
    }

    /**
     * Checks if the cell was updated in the meantime or not.
     *
     * @param cnR           the Cell and the result
     * @param stateCounters the map containing all the counters
     * @return {@code true} if the counter of the cell is the same as it was when the calculation was started.
     * It means that the cell was not updated in the meantime. If it was updated, the caller drops the result as stale.
     */
    private boolean notStale(CellWithResult cnR, Map<String, Long> stateCounters) {
        return stateCounters.containsKey(cnR.cell.id) && stateCounters.get(cnR.cell.id).equals(cnR.counter);
    }

    /**
     * checks if the value calculated is new.
     *
     * @param cnR      the Cell and the result
     * @param oldValue the old value of the state
     * @return {@code true} if there was no old value, and there is one now, or if there was an old value
     * but the new is different.
     */
    private boolean calculatedValueIsNew(CellWithResult cnR, Object oldValue) {
        return (oldValue == null && cnR.result != null) || (oldValue != null && !oldValue.equals(cnR.result));
    }

    /**
     * start a new task
     *
     * @param ctx       the context in which the task will run. A new thread context is created from this context and the
     *                  context variables are copied there as read-only.
     * @param cell      the cell to execute the task
     * @param exception the exception holder to signal the exception
     * @param counter   the counter of the cell. Upon finish and update, it will be checked that it has not changed
     *                  to avoid update with stale calculation.
     * @return the comparable future running the task
     */
    private CompletableFuture<CellWithResult> startTask(Context ctx, Cell cell, AtomicReference<Exception> exception, long counter) {
        final var newThreadContext = ctx.thread();
        copyVariables(ctx, newThreadContext);
        return CompletableFuture.supplyAsync(() -> {
            Thread.currentThread().setName(cell.id + ":" + NameGen.generateName());
            try {
                return new CellWithResult(cell.command.execute(newThreadContext), cell, counter);
            } catch (ExecutionException e) {
                final var newException = new ExecutionException("Exception in flow '%s' in thread %s %s", flowId, Thread.currentThread().getName(), e.getMessage());
                newException.setStackTrace(e.getStackTrace());
                exception.compareAndSet(null, newException);
                return null;
            } catch (Exception t) {
                final var newException = new ExecutionException(t, "Exception in flow '%s' thread %s ", flowId, Thread.currentThread().getName());
                exception.compareAndSet(null, newException);
                return null;
            }
        }, executor);
    }

    private static void copyVariables(Context source, Context target) {
        for (final var key : source.allLocalKeys()) {
            target.let0(key, source.get(key));
            target.freeze(key);
        }
    }
}
