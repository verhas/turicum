package ch.turic.builtins.macros;

import ch.turic.memory.Context;

class ContextCopy {
    static void copyVariables(Context source, Context target) {
        for (final var key : source.keys()) {
            target.let0(key, source.get(key));
            target.freeze(key);
        }
    }
}
