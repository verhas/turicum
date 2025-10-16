package ch.turic.analyzer;

import ch.turic.exceptions.BadSyntax;
import ch.turic.Command;
import ch.turic.commands.LetAssignment;
import ch.turic.commands.MultiLetAssignment;
import ch.turic.commands.TypeDeclaration;
import ch.turic.memory.LeftValue;

import java.util.ArrayList;
import java.util.Optional;

public class MultiLetAnalyzer extends AbstractAnalyzer {
    private final boolean mut;
    public static final MultiLetAnalyzer INSTANCE = new MultiLetAnalyzer(false);
    public static final MultiLetAnalyzer INSTANCE_MUT = new MultiLetAnalyzer(true);
    private static final DirectMapping NO_DIRECT_MAPPING = new DirectMapping(null, new TypeDeclaration[0], null);

    public MultiLetAnalyzer(boolean mut) {
        this.mut = mut;
    }

    @Override
    public Command _analyze(LexList lexes) throws BadSyntax {
        if (lexes.is("[", "{")) {
            final MultiLetAssignment.ObjectOrListMapping mapping;
            final var s = lexes.next().text;
            if ("{".equals(s)) {
                mapping = analyzeObjectMapping(lexes, !mut);
            } else {
                mapping = analyzeListMapping(lexes, !mut);
            }
            if (lexes.isNot("=")) {
                throw lexes.syntaxError("multi-let assignment '=' is missing");
            }
            lexes.next();
            final var rightHandSide = ExpressionAnalyzer.INSTANCE.analyze(lexes);
            return new MultiLetAssignment(rightHandSide, mapping, mut);
        } else {
            AssignmentList.Assignment[] assignments = AssignmentList.INSTANCE.analyze(lexes);
            if (assignments.length == 0) {
                throw lexes.syntaxError("%s with zero assignments", this == INSTANCE ? "let" : "mut");
            }
            return new LetAssignment(assignments, mut);
        }
    }

    /**
     * Analyzes the syntax of a list-based mapping structure from the given lexical list
     * and constructs a corresponding list mapping object. This method processes the
     * individual elements of the list, handling direct mappings, submappings, and their attributes.
     *
     * @param lexes the lexical list containing the tokens to be analyzed for list mappings
     * @param pin   a flag indicating whether the mapping elements are pinned, meaning they
     *              cannot be modified once mapped
     * @return a constructed instance of SetCommand.ListMapping containing the parsed list element mappings
     * @throws BadSyntax if the syntax of the lexical list is invalid or does not conform
     *                   to the expected list mapping rules
     */
    private MultiLetAssignment.ListMapping analyzeListMapping(LexList lexes, boolean pin) throws BadSyntax {
        final var elementMappings = new ArrayList<MultiLetAssignment.ElementMapping>();
        while (lexes.isNot("]")) {
            final boolean subPin = isSubmappingPinned(lexes, pin);
            final var submapping = getSubMappingIfAny(lexes, subPin);
            final var directMapping = submapping == null ? getDirectMappingIfAny(lexes, subPin) : NO_DIRECT_MAPPING;

            elementMappings.add(new MultiLetAssignment.ElementMapping(directMapping.identifier(),
                    directMapping.types(),
                    directMapping.leftValue(),
                    submapping, pin));

            stepOverTheComma(lexes);

        }
        lexes.next(); // step over the ']'
        return new MultiLetAssignment.ListMapping(elementMappings.toArray(MultiLetAssignment.ElementMapping[]::new));
    }

    /**
     * Analyzes the object mapping syntax from the given lexical list, extracting
     * field mappings and their associated attributes. This method recursively processes
     * nested object and list mappings to construct a comprehensive mapping structure.
     * <pre>
     * {@code
     *          mut { a -> b[3] , let[x, y:any, z] , let{a -> k:str, let t, p-> q}} =
     * }
     * </pre>
     *
     * @param lexes the lexical list containing the tokens to be analyzed for object mappings
     * @param pin   flag indicating whether the mapping is pinned, preventing modification of the mapped variable
     * @return a constructed instance of SetCommand.ObjectMapping containing the parsed field mappings
     * @throws BadSyntax if the syntax of the lexical list is invalid or does not conform to expected object mapping rules
     */
    private MultiLetAssignment.ObjectMapping analyzeObjectMapping(LexList lexes, boolean pin) throws BadSyntax {
        final var fieldMappings = new ArrayList<MultiLetAssignment.FieldMapping>();
        while (lexes.isNot("}")) {

            final boolean subPin = isSubmappingPinned(lexes, pin);
            final var explicitFieldName = getExplicitFieldName(lexes);
            final var submapping = getSubMappingIfAny(lexes, subPin);
            final var directMapping = submapping == null ? getDirectMappingIfAny(lexes, subPin) : NO_DIRECT_MAPPING;

            if (explicitFieldName.isEmpty() && directMapping.identifier() == null) {
                throw lexes.syntaxError("missing field name for mapping");
            }

            fieldMappings.add(new MultiLetAssignment.FieldMapping(explicitFieldName.orElse(directMapping.identifier()),
                    new MultiLetAssignment.ElementMapping(directMapping.identifier(),
                            directMapping.types(),
                            directMapping.leftValue(),
                            submapping, pin || subPin)));
            stepOverTheComma(lexes);
        }
        lexes.next(); // step over the '}'
        return new MultiLetAssignment.ObjectMapping(fieldMappings.toArray(MultiLetAssignment.FieldMapping[]::new));
    }

    /**
     * Retrieves a submapping, if present, by analyzing the given lexical list.
     * Depending on the starting token in the lexical list, it determines whether
     * the mapping is an object or a list and proceeds accordingly.
     *
     * @param lexes  the lexical list to analyze for submapping
     * @param subPin a flag indicating whether the submapping is pinned, preventing modification
     * @return an instance of SetCommand.ObjectOrListMapping if a submapping is detected,
     * or null if there is no submapping
     */
    private MultiLetAssignment.ObjectOrListMapping getSubMappingIfAny(LexList lexes, boolean subPin) {
        final MultiLetAssignment.ObjectOrListMapping submapping;
        if (lexes.is("{")) {
            lexes.next();
            submapping = analyzeObjectMapping(lexes, subPin);
        } else if (lexes.is("[")) {
            lexes.next();
            submapping = analyzeListMapping(lexes, subPin);
        } else {
            submapping = null;
        }
        return submapping;
    }

    /**
     * Retrieves a direct mapping from the given lexical list if one exists. A direct mapping can contain
     * either an identifier with optional type declarations, or a left value, but never both.
     * <p>
     * This method analyzes the lexical tokens following these rules:
     * 1. If the next token is an identifier followed by ':' or ',' - processes an identifier mapping:
     * - Sets identifier to the token's text
     * - Sets leftValue to null
     * - Optionally processes type declarations if ':' is present
     * 2. Otherwise - processes a left value mapping:
     * - Sets identifier to null
     * - Sets types to empty array
     * - Processes leftValue using LeftValueAnalyzer
     *
     * @param lexes the lexical list to analyze for a direct mapping
     * @return a DirectMapping containing either:
     * - an identifier with optional type declarations (leftValue will be null), or
     * - a left value (identifier and types will be null/empty)
     */
    private static DirectMapping getDirectMappingIfAny(LexList lexes,boolean pin) {
        final LeftValue leftValue;
        final TypeDeclaration[] types;
        final String identifier;
        if (lexes.isIdentifier() && lexes.isAt(1, ":", ",", "}", "]")) {
            leftValue = null;
            identifier = lexes.next().text();
            if (lexes.is(":")) {
                lexes.next();
                types = AssignmentList.getTheTypeDefinitions(lexes);
            } else {
                types = new TypeDeclaration[0];
            }
        } else {
            if (pin) {
                throw lexes.syntaxError("you cannot map to left value in let");
            }
            identifier = null;
            types = new TypeDeclaration[0];
            leftValue = LeftValueAnalyzer.INSTANCE.analyze(lexes);
        }
        return new DirectMapping(identifier, types, leftValue);
    }

    private record DirectMapping(String identifier, TypeDeclaration[] types, LeftValue leftValue) {
    }

    /**
     * Extracts the explicit field name from the given lexical list, if one exists.
     * Checks if the current token in the lexical list is an identifier followed
     * by a '->' symbol, and if so, retrieves the field name specified.
     * <p>
     * In object mappings, if no explicit field name is specified (this method returns empty),
     * the identifier from the direct mapping will be used as the field name instead.
     * For example:
     * - `a -> b` : 'a' is the explicit field name, 'b' is the identifier
     * - `b` : 'b' is both the field name and identifier since no explicit name was given
     *
     * @param lexes the lexical list to analyze for an explicit field name
     * @return an Optional containing the explicit field name if one exists (identifier followed by '->'),
     * or an empty Optional if no explicit field name is found (in which case the identifier
     * from the direct mapping will be used as the field name)
     */
    private static Optional<String> getExplicitFieldName(LexList lexes) {
        final String fieldName;
        if (lexes.isIdentifier() && lexes.isAt(1, "->")) {
            fieldName = lexes.next().text();
            lexes.next();// step over the '->'
            return Optional.of(fieldName);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Determines if a submapping is pinned based on the current state of the given lexical list.
     * If the current token in the lexical list is a `LET` keyword, it consumes the token and pins the submapping.
     * Otherwise, the pinned state is determined by the provided `pin` parameter.
     *
     * @param lexes the lexical list to examine for determining the pinned state of the submapping
     * @param pin   a flag indicating the initially pinned state to use if the `LET` keyword is not present
     * @return true if the sub mapping is determined to be pinned, otherwise false
     */
    private static boolean isSubmappingPinned(LexList lexes, boolean pin) {
        final boolean subPin;
        if (lexes.is(Keywords.LET)) {
            lexes.next();
            subPin = true;
        } else {
            subPin = pin;
        }
        return subPin;
    }

    /**
     * Advances over a comma in the lexical list if it exists, ensuring proper syntax formatting.
     * Throws a syntax error if a closing brace is found immediately after the comma without a field mapping.
     *
     * @param lexes the lexical list to analyze and advance through
     * @throws BadSyntax if there is no valid field mapping between a comma and a closing brace
     */
    private static void stepOverTheComma(LexList lexes) {
        if (lexes.is(",")) {
            lexes.next();
            if (lexes.is("}")) {
                throw lexes.syntaxError("missing field mapping between ',' and '}");
            }
        }
    }

}