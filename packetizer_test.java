import java.io.*;
import java.util.*;

public class packetizer_test {
    public static void main(String[] args) {
        String payload = "";
        XBeePacketizer p = new XBeePacketizer();
        Address64 dst = new Address64("99:11:ff:cc:11:05:19:73");

        for(int i=0; i<557; i++)
            payload += ( (i%2)==0 ? "x" : "o");

        List q = p.tx(dst, payload);

        for(int i=0; i<q.size(); i++) {
            System.out.println("writing packet-" + i);

            try {
                FileOutputStream out = new FileOutputStream("packet-" + i + ".txt");

                out.write( ( (XBeePacket) q.get(i) ).getBytes() );
                out.close();
            }

            catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
}
