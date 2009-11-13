import java.io.*;
import java.util.*;

public class test {
    public static void main(String[] args) {
        String payload = "";
        XBeePacketizer p = new XBeePacketizer();

        for(int i=0; i<557; i++)
            payload += ( (i%2)==0 ? "x" : "o");

        List q = p.build_tx(payload);

        try {
            // all as one packet
            XBeePacket test = XBeePacket.tx((char)972, payload);
            q.add(test);
        }

        catch(Exception e) {
            System.err.println("problem building packet: " + e.getMessage());
        }

        for(int i=0; i<q.size(); i++) {
            System.out.println("writing packet-" + i);

            try {
                FileWriter fstream = new FileWriter("packet-" + i + ".txt");
                BufferedWriter out = new BufferedWriter(fstream);

                out.write( ( (XBeePacket) q.get(i) ).serialize() );
                out.close();
            }

            catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
}
