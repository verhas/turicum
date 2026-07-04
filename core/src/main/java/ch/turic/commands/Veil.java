package ch.turic.commands;

import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LocalContext;
import ch.turic.utils.Unmarshaller;

/*snippet veil_command

The `veil` command makes fields inaccessible from the outside:

[source]
----
class Account {
    fn init(opening) {
        mut balance = opening
        veil balance                    // instance field, veiled per object
    }
    fn deposit(d) { balance = balance + d }
    fn helper() { 1 }
    veil helper                         // class level method
}
----

A veiled name cannot be read or written through the field access of the object or the class
(`a.balance`, `a["balance"]`, calling `a.helper()`), the access reports an error.
Inside the methods of the object, the name is used as a variable the usual way; the veil does not
restrict the lexical name resolution.
Methods of a subclass also see the veiled names of the parent.

The veil is a guardrail, not a security boundary.
The `with` command and macros see through it deliberately.
For example, a method comparing two instances can read the veiled field of the other instance:

[source]
----
fn equals(other) { v == { with other : v } }
----

The names must be defined before they are veiled.
The `keys()` function does not list veiled names; `keys_all()` lists all of them.
Do not veil `init`, `entry`, `exit`, `to_string`, or operator methods: they are invoked through
the field access, and veiling them makes the object unusable for the corresponding purpose.

end snippet*/

/**
 * Marks names as veiled in the context where the command executes (the names are registered in
 * the context of the chain where they are defined). Veiled names are rejected by the field
 * access of {@code LngObject} and {@code LngClass}; the lexical variable resolution used inside
 * methods is not affected.
 */
public class Veil extends AbstractCommand {
    public final Identifier[] identifiers;

    public static Veil factory(final Unmarshaller.Args args) {
        return new Veil(args.get("identifiers", Identifier[].class));
    }

    public Veil(Identifier[] identifiers) {
        this.identifiers = identifiers;
    }

    @Override
    public Object _execute(final LocalContext ctx) throws ExecutionException {
        ctx.step();
        for (final var identifier : identifiers) {
            ctx.veil(identifier.name());
        }
        return null;
    }
}
