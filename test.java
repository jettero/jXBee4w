import java.io.*;

public class test {
    public static void main(String[] args) {
        XBeePacketizer p = new XBeePacketizer();
        XBeePacket q[]   = p.build_tx("supz");

        for(int i=0; i<q.length; i++) {
            try {
                FileWriter fstream = new FileWriter("dump-" + i + ".txt");
                BufferedWriter out = new BufferedWriter(fstream);

                out.write( q[i].serialize() );
                out.close();
            }

            catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
}
