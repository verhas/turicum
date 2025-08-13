package ch.turic.commands;


import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.HasFields;
import ch.turic.memory.LeftValue;
import ch.turic.utils.Unmarshaller;

/**
 * Command implementation for multi-assignments that handles both list and object assignments.
 */
public class MultiLetAssignment extends AbstractCommand {

    /**
     * Represents the command that is to be executed by the SetCommand to get the value to be assigned to the structure.
     */
    public final Command expression;
    /**
     * Represents a mapping that can be either an object mapping or a list mapping.
     * This serves as a key part of defining or updating mappings during execution.
     * It is used to determine the operations to apply based on whether the mapping
     * relates to an object or a list.
     */
    public final ObjectOrListMapping mapping;
    /**
     * Indicates whether the new variables defined are mutable or immutable.
     */
    public final boolean mut;

    public sealed interface ObjectOrListMapping permits ObjectMapping, ListMapping {
    }

    public record ObjectMapping(FieldMapping[] fieldMappings) implements ObjectOrListMapping {
        public static ObjectMapping factory(final Unmarshaller.Args args) {
            return new ObjectMapping(
                    args.get("fieldMappings", FieldMapping[].class)
            );
        }
    }

    public record ListMapping(ElementMapping[] elementMappings) implements ObjectOrListMapping {
        public static ListMapping factory(final Unmarshaller.Args args) {
            return new ListMapping(
                    args.get("elementMappings", ElementMapping[].class)
            );
        }
    }

    public record ElementMapping(String identifier, TypeDeclaration[] types,
                                 LeftValue leftValue,
                                 ObjectOrListMapping mapping,
                                 boolean pin) {
        public ElementMapping {
            if (identifier != null) {
                if (leftValue != null) {
                    throw new IllegalArgumentException("Cannot specify both identifier and leftValue for element mapping");
                }
                if (mapping != null) {
                    throw new IllegalArgumentException("Cannot specify both mapping and identifier for element mapping");
                }
            }
            if (leftValue != null) {
                if (mapping != null) {
                    throw new IllegalArgumentException("Cannot specify both leftValue and mapping for element mapping");
                }
                if (pin) {
                    throw new IllegalArgumentException("Cannot specify left value for let");
                }
            }
            if (identifier == null && leftValue == null && mapping == null) {
                throw new IllegalArgumentException("At least one of identifier, leftValue or mapping must be specified for element mapping");
            }
        }

        public static ElementMapping factory(final Unmarshaller.Args args) {
            return new ElementMapping(
                    args.str("identifier"),
                    args.get("types", TypeDeclaration[].class),
                    args.get("leftValue", LeftValue.class),
                    args.get("mapping", ObjectOrListMapping.class),
                    args.bool("pin")
            );
        }
    }

    public record FieldMapping(String fieldName,
                               ElementMapping elementMapping
    ) {
        public static FieldMapping factory(final Unmarshaller.Args args) {
            return new FieldMapping(
                    args.str("fieldName"),
                    args.get("elementMapping", ElementMapping.class)
            );
        }
    }

    public static MultiLetAssignment factory(final Unmarshaller.Args args) {
        return new MultiLetAssignment(
                args.get("expression", Command.class),
                args.get("mapping", ObjectOrListMapping.class),
                args.bool("mut")
        );
    }

    public MultiLetAssignment(Command expression, ObjectOrListMapping mapping, boolean mut) {
        this.expression = expression;
        this.mapping = mapping;
        this.mut = mut;
    }

    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        final var value = expression.execute(ctx);
        switch (mapping) {
            case ObjectMapping objectMapping -> executeObjectMap(ctx, objectMapping, value, !mut);
            case ListMapping listMapping -> executeListMap(ctx, listMapping, value, !mut);
        }
        return value;
    }

    private void executeObjectMap(final Context ctx, ObjectMapping mapper, Object value, boolean pin) throws ExecutionException {
        if (value instanceof HasFields feldHaber) {

            for (final var fieldMapping : mapper.fieldMappings) {
                final var mappedValue = feldHaber.getField(fieldMapping.fieldName());
                final var mapping = fieldMapping.elementMapping().mapping();
                final var leftValue = fieldMapping.elementMapping().leftValue();
                final var subPin = fieldMapping.elementMapping().pin;
                if (fieldMapping.elementMapping().identifier() != null) {
                    ctx.step();
                    final String[] typeNames = getTypeNames(ctx, fieldMapping.elementMapping().types());
                    defineOrUpdate(ctx, fieldMapping.elementMapping().identifier(), mappedValue, typeNames, subPin || pin);
                } else if (leftValue != null) {
                    if (!mut) {
                        throw new ExecutionException("Cannot have a left value in non-mutable ");
                    }
                    leftValue.reassign(ctx, ignore -> mappedValue);
                } else if (mapping != null) {
                    if (mapping instanceof ObjectMapping objectMapping) {
                        executeObjectMap(ctx, objectMapping, mappedValue, subPin || pin);
                    } else if (mapping instanceof ListMapping listMapping) {
                        executeListMap(ctx, listMapping, mappedValue, subPin || pin);
                    }
                } else {
                    throw new ExecutionException("Internal error Cannot map to object " + value.getClass().getName());
                }
            }
        } else {
            throw new ExecutionException("Cannot map to object " + value.getClass().getName());
        }
    }

    private void executeListMap(final Context ctx, ListMapping mapper, Object value, boolean pin) throws ExecutionException {
        if (value instanceof Iterable<?> iterable) {
            final var iterator = iterable.iterator();
            for (var elementMapping : mapper.elementMappings) {
                if (!iterator.hasNext()) {
                    throw new ExecutionException("[set] assignment right-hand side has too few values", value);
                }
                final var mappedValue = iterator.next();
                final var mapping = elementMapping.mapping();
                final var leftValue = elementMapping.leftValue();
                final var subPin = elementMapping.pin();
                if (elementMapping.identifier() != null) {
                    ctx.step();
                    final String[] typeNames = getTypeNames(ctx, elementMapping.types());
                    defineOrUpdate(ctx, elementMapping.identifier(), mappedValue, typeNames, subPin || pin);
                } else if (leftValue != null) {
                    if (!mut) {
                        throw new ExecutionException("Cannot have a left value in non-mutable ");
                    }
                    leftValue.reassign(ctx, ignore -> mappedValue);
                } else if (mapping != null) {
                    if (mapping instanceof ObjectMapping objectMapping) {
                        executeObjectMap(ctx, objectMapping, mappedValue, subPin || pin);
                    } else if (mapping instanceof ListMapping listMapping) {
                        executeListMap(ctx, listMapping, mappedValue, subPin || pin);
                    }
                } else {
                    throw new ExecutionException("Cannot map to list " + value.getClass().getName());
                }
            }
        }
    }

    private void defineOrUpdate(final Context ctx, final String identifier, Object value, String[] typeNames, boolean pin) {
        if (mut && !pin) {
            if (typeNames != null && typeNames.length > 0) {
                ctx.defineTypeChecked(identifier, value, typeNames);
            } else {
                if (ctx.contains(identifier)) {
                    ctx.update(identifier, value);
                } else {
                    ctx.defineTypeChecked(identifier, value, typeNames);
                }
            }
        } else {
            ctx.defineTypeChecked(identifier, value, typeNames);
            ctx.freeze(identifier);
        }
    }

    /**
     * Retrieves the type names of the given TypeDeclaration array using the provided context.
     *
     * @param ctx   the context used to calculate the type names
     * @param types an array of TypeDeclaration objects whose type names are to be calculated; can be null
     * @return an array of type names as strings, or null if the types parameter is null
     */
    private String[] getTypeNames(Context ctx, TypeDeclaration[] types) {
        final String[] typeNames;
        if (types == null) {
            typeNames = null;
        } else {
            typeNames = new String[types.length];
            for (int i = 0; i < types.length; i++) {
                final var type = types[i];
                typeNames[i] = type.calculateTypeName(ctx);
            }
        }
        return typeNames;
    }

}
