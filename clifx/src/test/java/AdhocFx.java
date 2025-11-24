import ch.turic.clifx.Main;
import org.junit.jupiter.api.Test;

import java.io.File;

public class AdhocFx {

    @Test
    void testAdHocFx(){
        final var testResource = this.getClass().getResource("/adhoc_fx.turi");
        final var testPath = new File(testResource.getPath()).getAbsolutePath().replaceAll("target/test-classes", "src/test/resources");
        Main.main( new String[]{testPath});
    }

}
