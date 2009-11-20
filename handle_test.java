import java.io.*;
import java.util.*;
import java.util.regex.*;

public class handle_test implements PacketRecvEvent {
    public static void recvPacket() {
        System.out.println("wow, recved!!");
    }

    public static void main(String[] args) {
        XBeeHandle h  = XBeeHandle();
        XBeePacket sl = XBeePacket.at("SL");
        XBeePacket sh = XBeePacket.at("SH");

        h.send_packet(sl);
        h.send_packet(sh);
    }
}
