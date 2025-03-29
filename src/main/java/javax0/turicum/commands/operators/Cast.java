package javax0.turicum.commands.operators;

import javax0.turicum.ExecutionException;

public class Cast {

    /**
     * Test for conversion to long possibility.
     *
     * @param obj the object to check
     * @return {@code true} if the object can be converted to Long
     */
    public static boolean isLong(Object obj) {
        return switch (obj) {
            case Long ignore -> true;
            case Integer ignore -> true;
            case Short ignore -> true;
            case Byte ignore -> true;
            case Character ignore -> true;
            case Double ignore -> false;
            case Float ignore -> false;
            case Boolean ignore -> false;
            case CharSequence cs -> {
                int start = cs.charAt(0) == '+' || cs.charAt(0) == '-' ? 1 : 0;
                for (int i = start; i < cs.length(); i++) {
                    if (!Character.isDigit(cs.charAt(i))) {
                        yield false;
                    }
                }
                yield true;
            }
            default -> false;
        };
    }

    public static boolean isDouble(Object obj) {
        return switch (obj) {
            case Long ignore -> true;
            case Integer ignore -> true;
            case Short ignore -> true;
            case Byte ignore -> true;
            case Character ignore -> true;
            case Double ignore -> true;
            case Float ignore -> true;
            case Boolean ignore -> false;
            case CharSequence cs -> {
                int i = cs.charAt(0) == '+' || cs.charAt(0) == '-' ? 1 : 0;
                while (i < cs.length()) {
                    if (cs.charAt(i) == '.') break;
                    if (!Character.isDigit(cs.charAt(i))) {
                        yield false;
                    }
                    i++;
                }
                if (i >= cs.length()) {
                    yield true;
                }
                if (cs.charAt(i) != '.') {
                    yield false;
                }
                i++; // step over the '.'
                while (i < cs.length()) {
                    if (cs.charAt(i) == 'e' || cs.charAt(i) == 'E') break;
                    if (!Character.isDigit(cs.charAt(i))) {
                        yield false;
                    }
                    i++;
                }
                if (i < cs.length() && cs.charAt(i) != 'e' && cs.charAt(i) != 'E') {
                    yield false;
                }
                i++; // step over the 'e' or 'E'
                if (i >= cs.length()) {
                    yield false;
                }
                i += cs.charAt(i) == '+' || cs.charAt(i) == '-' ? 1 : 0;
                if (i >= cs.length()) {
                    yield false;
                }
                while (i < cs.length()) {
                    if (!Character.isDigit(cs.charAt(i))) {
                        yield false;
                    }
                    i++;
                }
                yield true;
            }
            default -> false;
        };
    }

    public static boolean isBoolean(final Object test) throws ExecutionException {
        return switch (test) {
            case null -> true;
            case Boolean ignored1 -> true;
            case Long ignored -> true;
            case Integer ignored -> true;
            case Short ignored -> true;
            case Byte ignored -> true;
            case Character ignored -> true;
            case Double ignored -> true;
            case Float ignored -> true;
            case String ignored -> true;
            default -> false;
        };
    }

    public static boolean toBoolean(final Object test) throws ExecutionException {
        switch (test) {
            case null -> {
                return false;
            }
            case Boolean b -> {
                return b;
            }
            case Long l -> {
                return l != 0;
            }
            case Integer i -> {
                return i != 0;
            }
            case Short s -> {
                return s != 0;
            }
            case Byte b -> {
                return b != 0;
            }
            case Character c -> {
                return c != 0;
            }
            case Double d -> {
                return d != 0;
            }
            case Float f -> {
                return f != 0;
            }
            case String s -> {
                return s.equals("true");
            }
            default -> {
                // roll over and handle special cases
            }
        }
        throw new ExecutionException("Value '" + test + "' cannot be used as a boolean");
    }

    public static Long toLong(Object obj) throws ExecutionException {
        return switch (obj) {
            case Long l -> l;
            case Integer i -> Long.valueOf(i);
            case Short sh -> Long.valueOf(sh);
            case Byte b -> Long.valueOf(b);
            case Character c -> Long.valueOf(c);
            case Double ignore -> throw new ExecutionException("Cannot cast double to long");
            case Float ignore -> throw new ExecutionException("Cannot cast float to long");
            case Boolean ignore -> throw new ExecutionException("Cannot cast boolean to long");
            case CharSequence cs -> {
                try {
                    yield Long.parseLong(cs.toString());
                } catch (NumberFormatException e) {
                    throw new ExecutionException("Cannot cast string to long");
                }
            }
            default ->
                    throw new ExecutionException("Cannot cast object of type '" + obj.getClass().getName() + "' to long");
        };
    }

    public static Double toDouble(Object obj) throws ExecutionException {
        return switch (obj) {
            case Long l -> Double.valueOf(l);
            case Integer i -> Double.valueOf(i);
            case Short sh -> Double.valueOf(sh);
            case Byte b -> Double.valueOf(b);
            case Character c -> Double.valueOf(c);
            case Double d -> d;
            case Float f -> Double.valueOf(f);
            case Boolean ignore -> throw new ExecutionException("Cannot cast boolean to long");
            case CharSequence cs -> {
                try {
                    yield Double.parseDouble(cs.toString());
                } catch (NumberFormatException e) {
                    throw new ExecutionException("Cannot cast string to long");
                }
            }
            default ->
                    throw new ExecutionException("Cannot cast object of type '" + obj.getClass().getName() + "' to long");
        };
    }


}
