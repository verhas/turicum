package ch.turic;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AdHocTest {

    private void test(String input, String expected) throws Exception {
        final var result = new Interpreter(input).execute();
        Assertions.assertEquals("" + expected, "" + result);
    }

    @Test
    void test1() throws Exception {
        test("""
class P {
    fn init {
        println "init in P, cls='%s' this='%s'" % [cls, this]
    }
}
class C : P {
}
let o = C();
none
                """,null);
    }
    //@Test
    void test0() throws Exception {
        test("""
fn mapper(request, response, target: fn){
    let get_params = {};
    with request {
        let qm = uri.index_of("?");
        if qm > -1 {
            let query_string = request.uri[qm+1 .. inf];
            for each param in query_string.split("&",-1){
                if param.index_of("=") > -1 {
                    let pair = param.split("=",-1);
                    get_params[pair[0]] = pair[1];
                } else {
                    get_params[param] = none;
                }
            }
        }
    }
    let args = {}
    args.request = request
    args.response = response
    args.get_params = get_params
    for each z in keys(get_params) : args[z] = get_params[z]
    target(.. args);
}
class Router {
    fn init(routes=[]);
    fn method( path: str, @type: str= "GET", ^target){
        println type
        println path
        println routes
        routes = [..routes,  {
                    path : path,
                    handler: {|request, response| mapper(request, response, target)}
                    }
                ];
    }
    fn start_server( @host="localhost", @port=8080 ){
        server({
            host: host,
            port: port,
            routes : routes
            })
    }
}

let z = Router()
let counter:num=0;
@z.method(path="/")
fn echo(request,response, astala=none, vista=none, bika=none){
    println "ASTALA VISTA" + astala + " " +vista
    global counter;
    with request {
        println "Request uri: %s" % uri
        println "Request method: %s" % method
        println "Client host: %s" % client.host
        println "Client port: %s" % client.port
        println "Server host: %s" % server.host
        println "Server port: %s" % server.port
        println "Request protocol: %s" % protocol
        println()
        println "Headers:"
        for each h in keys(headers) {
            let header = headers[h];
            if len(header) == 1 :
                println h,": ", headers[h][0]
            else:
                println h,": ", headers[h]
        }
    }
    println "I was invoked %d" % counter
    counter = counter + 1;
    return "hello " + counter;
}
let s = z.start_server()
let i = 0
while {
    let request =  s.receive();
    //println "REQUEST: %s" % [request];
    //println "-"*60;
    println "Request %s" % {i = i + 1};
}

                """,null);
    }

    //@Test
    void httpServer() throws Exception {
        test("""
let s = server({
host : "localhost", port: 8080,
routes : [{
    path : "/",
    handler: { let counter:num=0;
                {|request,response|
                    with request {
                        println "Request uri: %s" % uri
                        println "Request method: %s" % method
                        println "Client host: %s" % client.host
                        println "Client port: %s" % client.port
                        println "Server host: %s" % server.host
                        println "Server port: %s" % server.port
                        println "Request protocol: %s" % protocol
                        println()
                        println "Headers:"
                        for each h in keys(headers) {
                            let header = headers[h];
                            if len(header) == 1 :
                                println h,": ", headers[h][0]
                            else:
                                println h,": ", headers[h]
                        }
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
    println "REQUEST: %s" % [request];
    println "-"*60;
    i = i + 1;
}

none
                """
                , null);
    }
}
