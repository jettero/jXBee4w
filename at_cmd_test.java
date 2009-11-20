import java.io.*;
import java.util.*;

public class at_cmd_test {
    public static void main(String[] args) {
        String s[][] = {
            {"DB"}, {"BD", "7"}, {"AP", "1"}
        };
        XBeePacketizer z = new XBeePacketizer();
        XBeePacket p[] = z.at(s);

        for(int i=0; i<p.length; i++) {
            System.out.println("writing packet-" + s[i][0]);

            try {
                FileOutputStream out = new FileOutputStream("packet-" + s[i][0] + ".txt");

                out.write( p[i].getBytes() );
                out.close();
            }

            catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
}
