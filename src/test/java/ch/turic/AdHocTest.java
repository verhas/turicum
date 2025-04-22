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
sys_import "turi.re"

fn print_match(m){
    for i=0 ; i < len(m.group) ; i = i + 1 {
        let {index, start, end } = m.group[i];
        println "%s. \\"%s\\".substring(%s,%s)=\\"%s\\"" % [index,s,start, end,s[start..end]];
    }
}

let s = "abrakadabra";
let rx =Re("a(b)ra(ka)(dabra)")
let m = rx.match(s);
println m
print_match(m);

s = "xxx"+s+"yyy";
m = rx.match(s);
if m.group == none :
    println "does not match because of xxx and yyy"
println m, " is an empty object"
m = rx.find(s)
if m.group != none :
    println "matches because we find and not match"
print_match(m);

none
                """
                , null);
    }
}
