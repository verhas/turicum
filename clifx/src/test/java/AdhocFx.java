import ch.turic.clifx.Main;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import org.junit.jupiter.api.Test;

import java.io.File;

public class AdhocFx {

    @Test
    void testAdHocFx() {
        new MenuItem().setOnAction(e -> {
        });
        final var testResource = this.getClass().getResource("/adhoc_fx.turi");
        final var testPath = new File(testResource.getPath()).getAbsolutePath().replaceAll("target/test-classes", "src/test/resources");
        Main.main(new String[]{testPath});
    }

    @Test
    void startDebugger() {
        new MenuItem().setOnAction(e -> {
        });
        final var testResource = this.getClass().getResource("/debugger_fx.turi");
        final var testPath = new File(testResource.getPath()).getAbsolutePath().replaceAll("target/test-classes", "src/test/resources");
        Main.main(new String[]{testPath});
    }


    @Test
    void testAdHocFx2() {
        final var tab = new Tab();
        tab.setContent(new TextArea());
        ((TextArea) tab.getContent()).setText("Hello World");
    }
}
