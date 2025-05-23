import ch.turic.builtins.classes.*;
import ch.turic.builtins.functions.*;
import ch.turic.builtins.macros.Export;
import ch.turic.builtins.macros.IsDefined;
import ch.turic.builtins.macros.Thunk;
import ch.turic.builtins.macros.UnLet;

module ch.turic {
    requires java.desktop;
    requires java.sql;
    requires jdk.httpserver;
    exports ch.turic;
    opens turi;

    provides ch.turic.TuriFunction with Len, Type, Macro, Evaluate,
            Reclose, Keys, Import, JavaNewObject, JavaMethodCall,
            JavaMethodCallVararg, SysImport, Sleep, Set, SetGlobal,
            Que, TryYield, YieldIsClosed, Rx, RxMatch, TuriHttpServer, XmlFormat,
            IsObject, IsType, ExportAll, Parents, AllParents, Rng, Time, NanoTime,
            Abs, Unthunk, Unwrap, BlockList, Command, Min, Max;
    provides ch.turic.TuriMacro with Export, IsDefined, UnLet, Thunk;
    provides ch.turic.TuriClass with TuriString, TuriLong, TuriDouble, TuriIterator, TuriChannel;
}