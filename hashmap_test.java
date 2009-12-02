import java.util.*;

public class hashmap_test {
    public static void main(String args[]) {
        byte a[] = { 0x01, 0x02 };
        byte b[] = { 0x03, 0x04 };

        Address64 addr[] = {
            new Address64(a, b),
            new Address64(a, b),
            new Address64(a, a),
            new Address64(b, b)
        };

        HashMap <Address64, String> h = new HashMap <Address64, String>();

        for(int i=0; i<addr.length; i++)
            h.put(addr[i], addr[i].toText() + " - " + i);

        for( Address64 _a : h.keySet().toArray(new Address64[0]) )
            System.out.printf("[keys-vals] %s: %s%n", _a.toText(), h.get(_a));

        for( Address64 _a : addr )
            System.out.printf("[keys-hashcodes] %s: %d%n", _a.toText(), _a.hashCode());

    }
}
