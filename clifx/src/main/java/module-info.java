module ch.turic.clifx {
    requires ch.turic;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens turi.fx;
    exports ch.turic.clifx;
    exports ch.turic.clifx.builtins.functions;
    provides ch.turic.TuriFunction with ch.turic.clifx.builtins.functions.EventHandlerFactory;
}

