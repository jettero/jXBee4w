import java.io.*;
import java.util.*;

public class at_cmd_test {
    public static void main(String[] args) {
        String s[][] = {
            {"DB"}, {"BD", "7"}, {"AP", "1"}, {"AP"}, {"BD"}, {"VR"}
        };
        XBeePacketizer z = new XBeePacketizer();
        XBeePacket p[] = z.at(s);

        for(int i=0; i<p.length; i++)
            p[i].fileDump("packet-%d.pkt");
    }
}
