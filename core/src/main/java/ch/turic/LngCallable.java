package ch.turic;


import ch.turic.commands.ParameterList;
import ch.turic.exceptions.ExecutionException;

public interface LngCallable {
    @FunctionalInterface
    interface LngCallableClosure extends LngCallable {
    }

    @FunctionalInterface
    interface LngCallableMacro extends LngCallable {
    }

    Object call(Context context, Object[] arguments) throws ExecutionException;

    /**
     * Returns the optional Turicum parameter list declared for this Java-coded callable.
     * <p>
     * Implementations normally do not override this. Add {@link TuriParameters} to the
     * implementation class to opt into Turicum-style argument binding while keeping the
     * existing positional {@link #call(Context, Object[])} entry point.
     *
     * @return the parsed parameter list, or {@code null} for legacy positional-only calling
     */
    default ParameterList parameters() {
        return BuiltinSignatures.parametersFor(this);
    }
}
