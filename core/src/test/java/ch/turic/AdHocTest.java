package ch.turic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AdHocTest {

    private void test(String input, String expected) throws Exception {
        final var result = new Interpreter(input).execute();
        Assertions.assertEquals("" + expected, "" + result);
    }

    @Test
    void test0() throws Exception {
        test("""
class A {}
class B : A {}
let K = class : B {}
let z = K()
println is_type(z,K)
                """,null);
    }

    //@Test
    void test() throws Exception {
        test("""
let s = server({
host : "localhost", port: 8080,
routes : [{
    path : "/",
    handler: { let counter:num=0;
                {|request,response|
                    println "Request uri: %s" % request.uri
                    println "Request method: %s" % request.method
                    println "Client host: %s" % request.client.host
                    println "Client port: %s" % request.client.port
                    println "Server host: %s" % request.server.host
                    println "Server port: %s" % request.server.port
                    println "Request protocol: %s" % request.protocol
                    println()
                    println "Headers:"
                    for each h in keys(request.headers) {
                        let header = request.headers[h];
                        if len(header) == 1 :
                            println h,": ", request.headers[h][0]
                        else:
                            println h,": ", request.headers[h]
                    }
                    println "I was invoked %d" % counter
                    counter = counter + 1;
                    return "hello " + counter;
                }
             }
    }]
}
);

let i = 0
while {
    let request =  s.receive();
    println "REQUEST: %s" % [request]
    println "-"*60
}

none
                """
                , null);
    }
}
