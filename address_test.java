import java.io.*;
import java.util.*;

public class address_test {
    public static void main(String[] args) {
        Address64 a = new Address64("00:11:22:33:44:55:66:77");
        Address64 b = new Address64("99:88:77:66:55:44:33:22");
        Address64 c = new Address64("aa:bb:cc:dd:ee:ff:00:99");

        System.out.println( a.toText() );
        System.out.println( b.toText() );
        System.out.println( c.toText() );

        try {
            FileWriter fstream = new FileWriter("addr_check.txt");
            BufferedWriter out = new BufferedWriter(fstream);

            out.write( a.serialize() ); out.write("\n");
            out.write( b.serialize() ); out.write("\n");
            out.write( c.serialize() ); out.write("\n");

            out.close();
        }

        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
