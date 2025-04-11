import javax0.turicum.TuriClass;
import javax0.turicum.TuriFunction;
import javax0.turicum.builtins.classes.TuriDouble;
import javax0.turicum.builtins.classes.TuriLong;
import javax0.turicum.builtins.classes.TuriString;
import javax0.turicum.builtins.functions.*;

module com.javax0.turicum {
    requires java.desktop;
    requires java.sql;
    exports javax0.turicum;
    provides TuriFunction with Len, Type, Macro, Evaluate, Println,Print,Reclose,Keys, Import;
    provides TuriClass with TuriString, TuriLong, TuriDouble;
}