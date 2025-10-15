import ch.turic.builtins.classes.*;
import ch.turic.builtins.functions.*;
import ch.turic.builtins.functions.debugger.DebugSessionFactory;
import ch.turic.builtins.macros.*;

module ch.turic {
    requires java.xml;
    requires jdk.httpserver;
    requires java.net.http;
    exports ch.turic;
    exports ch. turic.builtins.functions;
    exports ch. turic.builtins.macros;
    exports ch. turic.builtins.classes;
    exports ch.turic.memory;
    exports ch.turic.memory.debugger;
    exports ch.turic.utils;
    opens turi; // needed to read the sysimport files as resources

    provides ch.turic.TuriFunction with Len, Type, Macro, Evaluate,
            Reclose, Keys, JavaObject, JavaCall,
            Sleep, SetSymbol, SetSymbolForce, SetGlobal, SetCaller, SetCallerForce,
            Que, TryYield, YieldIsClosed, Rx, RxMatch, TuriHttpServer, XmlFormat,
            IsObj, IsType, ExportAll, Parents, AllParents, Rng, Time, NanoTime,
            MathFunctions.Abs, MathFunctions.Sin, MathFunctions.Cos, MathFunctions.ACos,
            MathFunctions.Asin, MathFunctions.Atan, MathFunctions.Cbrt, MathFunctions.Ceil,
            MathFunctions.Exp, MathFunctions.Floor, MathFunctions.Log, MathFunctions.Log10,
            MathFunctions.Sqrt, MathFunctions.Tan, MathFunctions.Tanh, MathFunctions.Sinh,
            MathFunctions.Cosh, MathFunctions.ToDegrees, MathFunctions.ToRadians,
            MathFunctions.Atan2, MathFunctions.CopySign,
            MathFunctions.GetExponent, MathFunctions.Hypot, MathFunctions.IEEERemainder,
            MathFunctions.NextAfter, MathFunctions.NextDown,
            MathFunctions.NextUp, MathFunctions.Random, MathFunctions.ToInt, MathFunctions.ToNumber, MathFunctions.ToFloat, MathFunctions.Rint, MathFunctions.Round,
            MathFunctions.SigNum, MathFunctions.Ulp, IdHash, Chr,
            Unthunk, Unwrap, BlockList, Command, Min, Max, MathFunctions.Pow, MathFunctions.Scalb, Env,
            TuriHttpClient, Jsonify, JsonifyBeauty, Str, Glob, SourceDirectory, Arity, Signature,
            Uncurry, IsCurried, CurriedArity, Enumerate, JavaClass,
            DebugSessionFactory, Pack, MathFunctions.ToJavaFloat, MathFunctions.ToJavaInt, MathFunctions.ToJavaShort,
            MathFunctions.ToJavaChar, MathFunctions.ToJavaLong,JavaType, MathFunctions.ToJavaByte, MathFunctions.ToJavaDouble,
            AddJavaClasses,JavaResources;
    provides ch.turic.TuriMacro with Export, IsDefined, Unlet, Thunk, Quote, Import, SysImport, Delete;
    provides ch.turic.TuriClass with TuriString, TuriLong, TuriDouble, TuriIterator, TuriChannel, TuriInputStream, TuriInputStreamReader;
}