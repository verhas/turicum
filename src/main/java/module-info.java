import ch.turic.builtins.classes.*;
import ch.turic.builtins.functions.*;
import ch.turic.builtins.macros.Export;

module ch.turic {
    requires java.desktop;
    requires java.sql;
    exports ch.turic;
    opens turi;

    provides ch.turic.TuriFunction with Len, Type, Macro, Evaluate, Println,
            Print, Reclose, Keys, Import, JavaNewObject, JavaMethodCall,
            JavaMethodCallVararg, SysImport, Sleep, Set, SetGlobal,Throw;
    provides ch.turic.TuriMacro with Export;
    provides ch.turic.TuriClass with TuriString, TuriLong, TuriDouble, TuriFuture, TuriIterator;
}