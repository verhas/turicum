import javax0.turicum.TuriClass;
import javax0.turicum.TuriFunction;
import javax0.turicum.builtins.classes.TuriDouble;
import javax0.turicum.builtins.classes.TuriLong;
import javax0.turicum.builtins.classes.TuriString;
import javax0.turicum.builtins.functions.*;

module com.javax0.turicum {
    exports javax0.turicum;
    provides TuriFunction with Len, Type, Macro, Evaluate, Print;
    provides TuriClass with TuriString, TuriLong, TuriDouble;
}