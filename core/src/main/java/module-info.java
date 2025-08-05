import ch.turic.builtins.classes.*;
import ch.turic.builtins.functions.*;
import ch.turic.builtins.macros.*;

module ch.turic {
    requires java.xml;
    requires jdk.httpserver;
    requires java.net.http;
    exports ch.turic;
    opens turi; // needed to read the sysimport files as resources

    provides ch.turic.TuriFunction with Len, Type, Macro, Evaluate,
            Reclose, Keys, JavaNewObject, JavaMethodCall,
            JavaMethodCallVararg, Sleep, Set, SetGlobal, SetCaller,
            Que, TryYield, YieldIsClosed, Rx, RxMatch, TuriHttpServer, XmlFormat,
            IsObject, IsType, ExportAll, Parents, AllParents, Rng, Time, NanoTime,
            MathFunctions.Abs, MathFunctions.Sin, MathFunctions.Cos, MathFunctions.ACos,
            MathFunctions.Asin, MathFunctions.Atan, MathFunctions.Cbrt, MathFunctions.Ceil,
            MathFunctions.Exp, MathFunctions.Floor, MathFunctions.Log, MathFunctions.Log10,
            MathFunctions.Sqrt, MathFunctions.Tan, MathFunctions.Tanh, MathFunctions.Sinh,
            MathFunctions.Cosh, MathFunctions.ToDegrees, MathFunctions.ToRadians,
            MathFunctions.Atan2, MathFunctions.CopySign,
            MathFunctions.GetExponent, MathFunctions.Hypot, MathFunctions.IEEERemainder,
            MathFunctions.NextAfter, MathFunctions.NextDown,
            MathFunctions.NextUp, MathFunctions.Random, MathFunctions.Rint, MathFunctions.Round,
            MathFunctions.SigNum, MathFunctions.Ulp, IdHash,
            Unthunk, Unwrap, BlockList, Command, Min, Max, MathFunctions.Pow, MathFunctions.Scalb, Env,
            TuriHttpClient, Jsonify, JsonifyBeauty,Str,Glob, SourceDirectory,Arity, Signature,
            UnCurry,IsCurried,CurriedArity, Enumerate;
    provides ch.turic.TuriMacro with Export, IsDefined, UnLet, Thunk, Import, SysImport;
    provides ch.turic.TuriClass with TuriString, TuriLong, TuriDouble, TuriIterator, TuriChannel, TuriInputStream, TuriInputStreamReader;
}