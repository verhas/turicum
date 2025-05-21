package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.analyzer.WithAnalyzer;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.Context;
import ch.turic.memory.LngException;
import ch.turic.memory.LngObject;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class WithCommand extends AbstractCommand {
    private static final Object[] NO_PARAMS = new Object[0];
    public static final String METHOD_NAME_ENTRY = "entry";
    public static final String METHOD_NAME_EXIT = "exit";
    public final WithAnalyzer.WithPair[] pairs;

    public Command body() {
        return body;
    }

    public WithCommand(WithAnalyzer.WithPair[] pairs, Command body) {
        this.body = body;
        this.pairs = pairs;
    }

    public final Command body;

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        context.step();
        var ctx = context;

        final var supressExceptions = new AtomicBoolean(false);
        final var closeExceptions = new ArrayList<RuntimeException>();
        ExecutionException exception = null;
        final var objects = new ArrayList<LngObject>();
        try {
            for (final WithAnalyzer.WithPair pair : pairs) {
                final var obj = pair.command().execute(context);
                ctx = wrapCallingEntry(context, pair, obj, objects, ctx);
            }
            return body.execute(ctx);
        } catch (ExecutionException e) {
            exception = e;
        } finally {
            callExitMethods(context, exception, objects, supressExceptions, closeExceptions);
            throwClosingOnlyExceptionsIfAny(exception, closeExceptions);
        }
        return nullOrThrowTheExceptions(closeExceptions, exception, supressExceptions.get());
    }

    /**
     * Wraps a context by processing an object in a {@code with} statement and binding its entry result.
     * <p>
     * This method evaluates the result of an expression in a {@code with} statement. If the result is a
     * {@link LngObject} and an alias is provided, it attempts to invoke the {@code entry} method on the object.
     * The return value of this method must also be a {@link LngObject}. The result is then bound to the alias
     * in a wrapped version of the context.
     * <p>
     * If no alias is provided, the context is updated with the context of the {@link LngObject} directly.
     * <p>
     * Any violation—such as a missing or invalid {@code entry} method when 'as' alias is defined,
     * or an unexpected type—results in an {@link ExecutionException}.
     *
     * @param context the current execution context, used for calling entry methods
     * @param pair    the {@link WithAnalyzer.WithPair} representing the expression and optional alias
     * @param obj     the evaluated result of the expression in the {@code with} clause
     * @param objects a list to which processed {@link LngObject}s with aliases are added
     * @param ctx     the current context to be wrapped or updated
     * @return a new {@link Context} instance that includes any alias bindings or updated context information
     * @throws ExecutionException if the object is not a {@link LngObject}, has no valid {@code entry} method,
     *                            or the {@code entry} method returns a non-object
     */
    private static Context wrapCallingEntry(Context context, WithAnalyzer.WithPair pair, Object obj, ArrayList<LngObject> objects, Context ctx) {
        if (obj instanceof LngObject lngObject) {
            if (pair.alias() == null) {
                ctx = ctx.with(lngObject.context());
            } else {
                objects.add(lngObject);
                final var entry = lngObject.getField(METHOD_NAME_ENTRY);
                final LngObject resourceHandle;
                if (entry instanceof Closure closure) {
                    final var entryResult = closure.call(context, NO_PARAMS);
                    if (entryResult == null) {
                        resourceHandle = lngObject;
                    } else {
                        if (entryResult instanceof LngObject lngObjectResult) {
                            resourceHandle = lngObjectResult;
                        } else {
                            throw new ExecutionException("entry for object '%s' returned a non object '%s'", closure, entryResult);
                        }
                    }
                } else {
                    throw new ExecutionException("Resource in a 'with' statement without proper '%s' method", METHOD_NAME_ENTRY);
                }
                ctx = ctx.wrap();
                ctx.let0(pair.alias(), resourceHandle);
            }
        } else {
            throw new ExecutionException("expression '%s' in 'with' resulted a non-object '%s'", pair, obj);
        }
        return ctx;
    }

    /**
     * Either returns {@code null} to indicate suppressed failure, or throws the given exception.
     * <p>
     * If any {@link RuntimeException}s are present in {@code closeExceptions}, they are added as
     * suppressed exceptions to the provided {@link ExecutionException}.
     * <p>
     * If {@code supressExceptions} is {@code true}, the method returns {@code null}, indicating that
     * the execution should continue without signaling an error. Otherwise, it throws the enriched
     * {@link ExecutionException}.
     *
     * @param closeExceptions   a list of {@link RuntimeException}s thrown during resource cleanup
     * @param exception         the {@link ExecutionException} to throw if exceptions are not suppressed
     * @param supressExceptions whether exceptions should be suppressed
     * @return {@code null} if exceptions are suppressed
     * @throws ExecutionException if {@code supressExceptions} is {@code false}
     */
    private static Object nullOrThrowTheExceptions(final ArrayList<RuntimeException> closeExceptions,
                                                   final ExecutionException exception,
                                                   final boolean supressExceptions) {
        if (!closeExceptions.isEmpty()) {
            for (final var ce : closeExceptions) {
                exception.addSuppressed(ce);
            }
        }
        if (supressExceptions) {
            return null;
        }
        throw exception;
    }

    /**
     * Throws exceptions that occurred during resource closure if no primary execution exception is present.
     * <p>
     * If {@code exception} is {@code null} and {@code closeExceptions} contains one or more exceptions,
     * the method throws an appropriate exception:
     * <ul>
     *   <li>If exactly one exception is present, it is rethrown directly.</li>
     *   <li>If multiple exceptions are present, a new {@link ExecutionException} is thrown with
     *       all of them added as suppressed exceptions.</li>
     * </ul>
     * <p>
     * If {@code exception} is not {@code null}, this method does nothing.
     * In that case the exceptions will be thrown later by the upper layers that handle the non-null exception.
     *
     * @param exception       the primary {@link ExecutionException}, or {@code null} if none occurred
     * @param closeExceptions a list of {@link RuntimeException}s thrown during resource closure
     * @throws ExecutionException if multiple closure exceptions occurred and no primary exception is present
     * @throws RuntimeException   if a single closure exception occurred and no primary exception is present
     */
    private static void throwClosingOnlyExceptionsIfAny(final ExecutionException exception,
                                                        final ArrayList<RuntimeException> closeExceptions) {
        if (exception == null && !closeExceptions.isEmpty()) {
            if (closeExceptions.size() == 1) {
                throw closeExceptions.getFirst();
            }
            final var e = new ExecutionException("Exceptions closing resources");
            for (final var ce : closeExceptions) {
                e.addSuppressed(ce);
            }
            throw e;
        }
    }

    /**
     * Invokes the {@code exit} methods on a list of {@link LngObject}s in reverse order.
     * <p>
     * Each object is inspected for a field named {@code METHOD_NAME_EXIT}. If such a field exists and is a
     * {@link Closure}, it is invoked with a single parameter: a wrapped {@link LngException} if an
     * {@link ExecutionException} is provided, or {@code null} otherwise.
     * <p>
     * If any closure returns {@code true}, it indicates that the exception should be suppressed;
     * this intent is reflected by updating the {@code supressExceptions} flag.
     * <p>
     * Any exceptions thrown during the retrieval or invocation of the {@code exit} method are wrapped
     * in a {@link RuntimeException} and collected into the {@code closeExceptions} list.
     *
     * @param context           the current execution {@link Context}, used for building exceptions and calling closures
     * @param exception         the {@link ExecutionException} that caused the termination, or {@code null}
     * @param objects           a list of {@link LngObject}s whose {@code exit} methods are to be called
     * @param supressExceptions an {@link AtomicBoolean} flag indicating whether exceptions should be suppressed;
     *                          updated based on the return values of the {@code exit} closures
     * @param closeExceptions   a list to collect any {@link RuntimeException}s thrown during method execution
     */
    private static void callExitMethods(final Context context,
                                        final ExecutionException exception,
                                        final ArrayList<LngObject> objects,
                                        final AtomicBoolean supressExceptions,
                                        final ArrayList<RuntimeException> closeExceptions) {
        final var param = exception != null ? LngException.build(context, exception, context.threadContext.getStackTrace()) : null;
        for (final var object : objects.reversed()) {
            try {
                final var entry = object.getField(METHOD_NAME_EXIT);
                if (entry instanceof Closure closure) {
                    final var exitValue = Cast.toBoolean(closure.call(context, param));
                    supressExceptions.set(supressExceptions.get() || exitValue);
                }
            } catch (Exception e) {
                closeExceptions.add(new RuntimeException(e));
            }
        }
    }
}
