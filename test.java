import java.io.*;

public class test {
    public static void main(String[] args) {
        XBeePacketizer p = new XBeePacketizer();
        XBeePacket q[]   = p.build_tx("supz");

        try {
        // Create file 
            FileWriter fstream = new FileWriter("out.txt");
            BufferedWriter out = new BufferedWriter(fstream);

            out.write("Hello Java");
            out.close();
        }

        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
