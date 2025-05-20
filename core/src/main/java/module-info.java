import ch.turic.builtins.classes.*;
import ch.turic.builtins.functions.*;
import ch.turic.builtins.macros.*;

module ch.turic {
    requires java.desktop;
    requires java.sql;
    requires jdk.httpserver;
    exports ch.turic;
    opens turi;

    provides ch.turic.TuriFunction with Len, Type, Macro, Evaluate, Println,
            Print, Reclose, Keys, Import, JavaNewObject, JavaMethodCall,
            JavaMethodCallVararg, SysImport, Sleep, Set, SetGlobal,
            Que, TryYield, YieldIsClosed, Rx, RxMatch, TuriHttpServer, XmlFormat,
            IsObject, IsType, ExportAll, Parents, AllParents, Rng, Time, NanoTime,
            Abs, Unthunk, Unwrap,BlockList, Command;
    provides ch.turic.TuriMacro with Export, IsDefined, UnLet, Thunk;
    provides ch.turic.TuriClass with TuriString, TuriLong, TuriDouble, TuriIterator, TuriChannel;
}