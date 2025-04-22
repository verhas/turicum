package ch.turic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AdHocTest {

    private void test(String input, String expected) throws Exception {
        final var result = new Interpreter(input).execute();
        Assertions.assertEquals("" + expected, "" + result);
    }

    @Test
    void test() throws Exception {
        test("""
fn printer(){
    let n = 0
    while {
        let s = try_yield();
        return { println "we are done" } if yield_is_closed();
        if s == none {
            println "not ready %s" % n
            n = n + 1
            sleep 0.003
        } else {
            n = 0
            println "received %s" % s;
        }
    }
}

let task : task = async printer()
for i=1 ; i < 4 ; i = i +1 {
 println "sending ",i
 task.send(i);
 sleep 0.009
 }
println "closing the channel"
task.close();
println "channel is closed"
println "is done %s" % task.is_done()
await task
println "is done %s" % task.is_done()

none
                """
                , null);
    }
}
