package javax0.turicum.commands;

import javax0.turicum.memory.Context;

public sealed interface HasParametersWrapped extends Command permits Closure,Macro {
    ParameterList parameters();
    Context wrapped();
}
