package javax0.turicum;

import org.junit.jupiter.api.Test;

public class AdHocTest {

    @Test
    void test() {
        TuriTest.input("""
            {
                a = "A12"-12;
                a;
            }"""
        ).shouldResultIn("A");
    }

}
