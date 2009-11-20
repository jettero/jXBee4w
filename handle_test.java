import java.io.*;
import java.util.*;
import java.util.regex.*;

public class handle_test implements PacketRecvEvent {
    public void recvPacket(XBeePacket p) {
        System.out.println("wow, recved!!");
    }

    public void go() {
        XBeeHandle h  = XBeeHandle.newFromPortName("COM8", 115200, true, this);
        XBeePacket sl = XBeePacket.at("SL");
        XBeePacket sh = XBeePacket.at("SH");

        h.send_packet(sl);
        h.send_packet(sh);
    }

    public static void main(String[] args) {
        handle_test h = new handle_test();
        h.go();
    }
}
