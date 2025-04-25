import ch.turic.builtins.classes.*;
import ch.turic.builtins.functions.*;
import ch.turic.builtins.macros.Export;
import ch.turic.builtins.macros.IsDefined;

module ch.turic {
    requires java.desktop;
    requires java.sql;
    requires jdk.httpserver;
    exports ch.turic;
    opens turi;

    provides ch.turic.TuriFunction with Len, Type, Macro, Evaluate, Println,
            Print, Reclose, Keys, Import, JavaNewObject, JavaMethodCall,
            JavaMethodCallVararg, SysImport, Sleep, Set, SetGlobal,Throw,
            Que, TryYield,YieldIsClosed,Rx,RxMatch, TuriHttpServer;
    provides ch.turic.TuriMacro with Export, IsDefined;
    provides ch.turic.TuriClass with TuriString, TuriLong, TuriDouble, TuriIterator, TuriChannel;
}