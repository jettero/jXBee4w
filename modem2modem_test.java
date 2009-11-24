import java.io.*;
import java.util.*;
import java.util.regex.*;

public class modem2modem_test implements PacketRecvEvent, Runnable {
    private String name;
    private String port;
    private byte[] SH, SL;
    public Address64 a;

    public void println(String arg) { System.out.println(this.desc() + arg); }
    public String desc() { return "[" + name + "] "; }

    public void showAddress(byte []sh, byte[]sl) {
        Address64 a = new Address64(sh, sl);
        this.println("   SH+SL => " + a.toText());
    }

    public Address64 addr() {
        this.println("addr requested, waiting for it");

        while( a == null )
            try { Thread.sleep(30 * 1000); } catch (InterruptedException e) {}

        this.println("returning addr: " + a.toText());
        return a;
    }

    public void showResponse(XBeeATResponsePacket p) {
        String cmd = p.cmd();

        this.println("received AT" + p.cmd() + " response.");

        if( cmd.equals("SL") ) {
            SL = p.responseBytes();
            if( SH != null && SL != null )
                showAddress(SH, SL);
        }

        if( cmd.equals("SH") ) {
            SH = p.responseBytes();
            if( SH != null && SL != null )
                showAddress(SH, SL);
        }
    }

    public void showMessage(XBeeRxPacket p) {
        this.println(this.desc() + "rx"); // TODO: write this
    }

    public void recvPacket(XBeePacket p) {
        switch(p.type()) {
            case XBeePacket.AMT_AT_RESPONSE: showResponse( (XBeeATResponsePacket) p ); break;
            case XBeePacket.AMT_RX64:        showMessage(  (XBeeRxPacket)         p ); break;

            default:
                System.err.printf(this.desc() + "Packet type: %02x ignored — unhandled type");
        }

        p.fileDump(this.desc() + "recv-%d.pkt");
    }

    modem2modem_test(String _n, String _p) {
        name = _n;
        port = _p;
    }

    public void send(Address64 dst) {
        Address64 src = this.addr(); // blocks until we get our addr

        System.out.printf(this.desc() + "sending from %s to %s", src.toText(), dst.toText());
    }

    public void run() {
        XBeeHandle h;

        try {
            h = XBeeHandle.newFromPortName(name, port, 115200, false, this);

        } catch(Exception e) {
            e.printStackTrace();
            return;
        }

        String cmds[][] = { { "SH" }, { "SL" } };

        this.println("making a couple command packets");
        XBeePacket serials[] = (new XBeePacketizer()).at(cmds);

        try {
            this.println("sending first packet");
            h.send_packet(serials[0]);

            this.println("sending second packet");
            h.send_packet(serials[1]);

        } catch(IOException e) {
            System.err.println("error sending packet: " + e.getMessage());
            h.close();
            return;
        }

        this.println("waiting to see if anything happens");
        try { Thread.sleep(30 * 1000); } catch (InterruptedException e) {}

        this.println("bye");
        h.close();
    }

    public static void main(String[] args) {
        modem2modem_test target, source;

        Thread lhs = new Thread(target = new modem2modem_test("LHS", "COM7"));
        Thread rhs = new Thread(source = new modem2modem_test("RHS", "COM8"));

        System.out.println("starting lhs"); lhs.start();
        System.out.println("starting rhs"); rhs.start();

        source.send(target.addr()); // addr blocks until there's someting to return

        try { lhs.join(); } catch (InterruptedException e) { /* cool, but we're just collecting their exit anyway */ }
        try { rhs.join(); } catch (InterruptedException e) {}
    }
}
