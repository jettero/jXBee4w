import java.io.*;
import java.util.*;
import java.util.regex.*;

public class handle_test implements PacketRecvEvent {
    public void showResponse(XBeeATResponsePacket p) {
        System.out.println("ATr"); // TODO: write this
    }

    public void showMessage(XBeeRxPacket p) {
        System.out.println("rx"); // TODO: write this
    }

    public void recvPacket(XBeePacket p) {
        switch(p.type()) {
            case XBeePacket.AMT_AT_RESPONSE: showResponse( (XBeeATResponsePacket) p ); break;
            case XBeePacket.AMT_RX64:        showMessage(  (XBeeRxPacket)         p ); break;

            default:
                System.err.printf("Packet type: %02x ignored — unhandled type");
        }

        p.fileDump("recv-%d.pkt");
    }

    public void go() {
        XBeeHandle h;
        try {
            h = XBeeHandle.newFromPortName("COM8", 115200, true, this);

        } catch(Exception e) {
            e.printStackTrace();
            return;
        }

        String cmds[][] = { { "Sl" }, { "SH" } };

        System.out.println("making a couple command packets");
        XBeePacket serials[] = (new XBeePacketizer()).at(cmds);

        try {
            System.out.println("sending first packet");
            h.send_packet(serials[0]);

            System.out.println("sending second packet");
            h.send_packet(serials[1]);

        } catch(IOException e) {
            System.err.println("error sending packet: " + e.getMessage());
            h.close();
            return;
        }

        System.out.println("waiting to see if anything happens");
        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        System.out.println("bye");
        h.close();
    }

    public static void main(String[] args) {
        handle_test h = new handle_test();
        h.go();
    }
}
