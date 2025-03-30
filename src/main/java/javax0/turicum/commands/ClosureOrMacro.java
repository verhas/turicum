package javax0.turicum.commands;

import javax0.turicum.memory.Context;

public sealed interface ClosureOrMacro extends Command permits Closure,Macro {
    String[] parameters();
    Context wrapped();
}
