import java.io.*;
import java.util.*;

public class checksum_test {
    public static void main(String[] args) {
        byte b[]     = {(byte)0x7E, (byte)0x00, (byte)0x02, (byte)0x23, (byte)0x11, (byte)0xCB};
        XBeePacket p = new XBeePacket(b);

        if( p.check_checksum() ) System.out.println("Packet from manual passes checksum check.");
        else                     System.out.println("Packet from manual does not pass checksum check.");

        String check = Integer.toHexString(b[5] & 0xff); // java has no unsigned type, so squash the "negative" bits for printing
        System.out.println("Checksum from manual: " + check);

        check = Integer.toHexString(p.calculate_checksum() & 0xff);
        System.out.println("Checksum from calculator: " + check);
    }
}
