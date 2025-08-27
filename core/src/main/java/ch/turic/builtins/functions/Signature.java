package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.ClosureLike;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;

/**
 * Return the signature of a macro, function, or closure.
 */
public class Signature implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var args = FunUtils.args(name(), arguments, Object.class);
        final var ctx = FunUtils.ctx(context);
        final var arg = args.at(0).get();
        final var result = LngObject.newEmpty(ctx);
        return switch (arg) {
            case ClosureLike closure -> {
                // snippet signature_doc
                // * `name` is the name of the function, closure, macro or `none` if it has no name
                result.setField("name", closure.name());
                // * `return_type` is a list of return type strings
                result.setField("return_type", LngList.ofStrings(closure.returnType()));
                // * `parameters` is a list of parameters. Each element is an object with the following fields:
                final var parameters = LngList.of();
                for (var parameter : closure.parameters().parameters()) {
                    final var p = LngObject.newEmpty(ctx);
                    // ** `identifier` is the name of the parameter
                    p.setField("identifier", parameter.identifier());
                    switch (parameter.type()) {
                        case NAMED_ONLY -> {
                            // ** `named` is `true` if the parameter can be specified by name, `false` otherwise
                            p.setField("named", true);
                            // ** `positional` is `true` if the parameter can be specified by position, `false` otherwise
                            p.setField("positional", false);
                        }
                        case POSITIONAL_ONLY -> {
                            p.setField("named", false);
                            p.setField("positional", true);
                        }
                        case POSITIONAL_OR_NAMED -> {
                            p.setField("named", true);
                            p.setField("positional", true);
                        }
                    }
                    final var types = LngList.of();
                    for( final var t : parameter.types()){
                        types.add(t.expression() == null ? t.identifier() : t.expression());
                    }
                    // * `types` is a list of types. Each element is a string or a command object
                    p.setField("types", types);
                    // * `default` is the default value expression command object or `none`.
                    p.setField("default",parameter.defaultExpression());
                    parameters.add(p);
                }
                result.setField("parameters", parameters);
                // * `rest` is the name of the `[rest]` parameter or `none`
                result.setField("rest",closure.parameters().rest());
                // * `meta` is the name of the `{meta}` parameter or `none`
                result.setField("meta",closure.parameters().meta());
                // * `closure` is the name of the `^closure` parameter or `none`
                result.setField("closure",closure.parameters().closure());
                // end snippet
                yield result;
            }
            case null -> throw new ExecutionException("Nihil aritatem non habet.");
            default -> throw new ExecutionException("Genus incognitum ad aritatem evocandam: '%s'", arg);
        };
    }
}
