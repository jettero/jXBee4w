import java.io.*;
import java.util.*;

public class address_test {
    public static void main(String[] args) {
        Address64 a = new Address64("00:11:22:33:44:55:66:77");
        Address64 b = new Address64("88:99:aa:bb:cc:dd:ee:ff");
        Address64 c = new Address64("11:05:19:73:11:05:19:73");

        System.out.println( a.toText() );
        System.out.println( b.toText() );
        System.out.println( c.toText() );

        try {
            FileOutputStream out = new FileOutputStream("dump.txt");

            out.write( a.serialize() );
            out.write( b.serialize() );
            out.write( c.serialize() );

            out.close();
        }

        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
