import java.io.*;
import java.util.*;
import java.util.regex.*;

public class handle_test implements PacketRecvEvent {
    public static void recvPacket() {
        System.out.println("wow, recved!!");
    }

    public static void main(String[] args) {
        XBeeHandle h = XBeeHandle();
        XBeePacket p = XBeePacket.at("DB");

        h.send_packet(p);
    }
}
