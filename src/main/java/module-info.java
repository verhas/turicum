import ch.turic.builtins.classes.TuriDouble;
import ch.turic.builtins.classes.TuriLong;
import ch.turic.builtins.classes.TuriString;
import ch.turic.builtins.functions.*;

module ch.turic {
    requires java.desktop;
    requires java.sql;
    exports ch.turic;
    opens turi;

    provides ch.turic.TuriFunction with Len, Type, Macro, Evaluate, Println,
            Print, Reclose, Keys, Import, JavaNewObject, JavaMethodCall, JavaMethodCallVararg, SysImport;
    provides ch.turic.TuriClass with TuriString, TuriLong, TuriDouble;
}