import java.io.*;
import java.util.*;

public class packetizer_test {
    public static void main(String[] args) {
        String payload = "";
        XBeePacketizer packetizer = new XBeePacketizer();
        Address64 dst = new Address64("99:11:ff:cc:11:05:19:73");
        XBeePacket packet;

        for(int i=0; i<557; i++)
            payload += ( (i%2)==0 ? "x" : "o");

        Queue q = packetizer.tx(dst, payload);


        while( (packet = (XBeePacket) q.poll()) != null )
            packet.fileDump("packet-%d.pkt");
    }
}
