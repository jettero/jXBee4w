import java.io.*;
import java.util.*;
import java.util.regex.*;

public class handle_test implements PacketRecvEvent {
    public void recvPacket(XBeePacket p) {
        System.out.println("wow, recved!!");
    }

    public void go() {
        XBeeHandle h;
        try {
            h = XBeeHandle.newFromPortName("COM8", 115200, true, this);

        } catch(Exception e) {
            e.printStackTrace();
            return;
        }

        XBeePacket sl = XBeePacket.at((byte) 1, "SL");
        XBeePacket sh = XBeePacket.at((byte) 2, "SH");

        try {
            h.send_packet(sl);
            h.send_packet(sh);

        } catch(IOException e) {
            System.out.println("error sending packet: " + e.getMessage());
            return;
        }
    }

    public static void main(String[] args) {
        handle_test h = new handle_test();
        h.go();
    }
}
