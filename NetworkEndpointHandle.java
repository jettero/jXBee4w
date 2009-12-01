import gnu.io.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class NetworkEndpointHandle implements PacketRecvEvent {
    private static Queue<CommPortIdentifier> ports;

    private static boolean debug = false;
    private static boolean dump_unhandled = false;

    private PacketQueueWriter QoQ;
    private XBeeHandle xh;
    private XBeePacketizer xp;
    private Address64 a;

    private String name;
    private byte[] SH, SL;
    private String hardwareVersion;
    private String firmwareVersion;

    private MessageRecvEvent messageReceiver;

    private Thread _qThread; // keep a ref to the QoQ thread here

    // --------------------------- modem locator stuff -------------------------

    // private static void populatePortNames() {{{
    private static void populatePortNames() {
        String skip    = System.getenv("NEH_SKIP_PORT");
        String skips[] = new String[0];
        if( skip != null )
            skips = skip.split(",\\s*");

        if( ports != null )
            return;

        ports = new ArrayDeque<CommPortIdentifier>();

        Enumeration portIdentifiers = CommPortIdentifier.getPortIdentifiers();
        while (portIdentifiers.hasMoreElements()) {
            CommPortIdentifier pid = (CommPortIdentifier) portIdentifiers.nextElement();

            if(pid.getPortType() == CommPortIdentifier.PORT_SERIAL ) {
                boolean ok = true;
                for(int i=0; i<skips.length; i++)
                    if( skips[i].equals(pid.getName()) )
                        ok = false;

                if( ok )
                    ports.offer(pid); // returns false if it's full, but who cares, we're expecting like 8 things tops
            }
        }
    }
    // }}}
    // private void locateAndConfigure() throws XBeeConfigException {{{
    private void locateAndConfigure() throws XBeeConfigException {
        CommPortIdentifier pid;

        populatePortNames();
        int speeds[] = {115200, 9600};

        while( (pid = ports.poll()) != null ) {
            System.out.println("Looking for XBee on port " + pid.getName());

            for(int i=0; i<speeds.length; i++) {
                int result = XBeeConfig.config(pid, speeds[i]); // try to config

                if( result == XBeeConfig.CONFIGURED ) {
                    try {
                        xh = new XBeeHandle(name, pid, 115200, debug, this);
                        xp = new XBeePacketizer();
                    }

                    catch(Exception e) {
                        String msg = e.getMessage();

                        if( msg.length() < 1 ) {
                            e.printStackTrace(); // no error message, dump a trace instead
                            throw new XBeeConfigException("Unexpected error creating XBeeHandle on configured port (dumped trace)");
                        }

                        throw new XBeeConfigException("Unexpected error creating XBeeHandle on configured port: " + msg);
                    }

                    QoQ = new PacketQueueWriter(xh); // start our QoQ writer

                    _qThread = new Thread(QoQ);
                    _qThread.start();

                    return; // if it worked, great, return out of there
                }

                if( result != XBeeConfig.SPEED_ERR )
                    break; // as long as it's not a speed error, try the next speed
            }

        }

        throw new XBeeConfigException("Couldn't find a modem to configure or some fatal error occured during the configuration");
    }
    // }}}

    // --------------------------- Rx handlers -------------------------

    // public void storeAddress(byte []sh, byte[]sl) {{{
    public void storeAddress(byte []sh, byte[]sl) {
        a = new Address64(sh, sl);

        if( debug )
            System.out.println("Address found SH+SL => " + a.toText());
    }
    // }}}
    // public void handleATResponse(XBeeATResponsePacket p) {{{
    public void handleATResponse(XBeeATResponsePacket p) {
        String cmd = p.cmd();

        if( p.statusOK() ) {
            byte b[] = p.responseBytes();

            if( debug )
                System.out.println("[debug] received valid AT" + cmd + " response.");

            if( cmd.equals("VR") ) {
                firmwareVersion = String.format("%02x%02x", b[0], b[1]);

            } else if( cmd.equals("HV") ) {
                hardwareVersion = String.format("%02x%02x", b[0], b[1]);

            } else if( cmd.equals("SL") ) {
                SL = b;
                if( SH != null )
                    storeAddress(SH, SL);

            } else if( cmd.equals("SH") ) {
                SH = b;
                if( SL != null )
                    storeAddress(SH, SL);
            }

        } else {
            if( p.statusError() ) {
                System.err.println("Error sending " + cmd + " command.  Bad params?");

            } else if( p.statusInvalidCommand() ) {
                System.err.println("Invlaid Command error sending " + cmd + " command.  Bad command?");

            } else {
                System.err.println("Unhandled error sending " + cmd + " command.");
            }
        }

    }
    // }}}
    // public void showMessage(XBeeRxPacket p) {{{
    public void showMessage(XBeeRxPacket p) {
        System.out.println("rx"); // TODO: write this
    }
    // }}}

    // public void recvPacket(XBeePacket p) {{{
    public void recvPacket(XBeePacket p) {
        byte bType = p.type();
        XBeeRxPacket rx;
        XBeeTxStatusPacket st;

        switch(bType) {
            case XBeePacket.AMT_AT_RESPONSE: handleATResponse( (XBeeATResponsePacket) p ); break;
            case XBeePacket.AMT_RX64:
                if( messageReceiver != null ) {
                    rx = (XBeeRxPacket) p;
                    messageReceiver.recvMessage(this, rx.getSourceAddress(), rx.getPayloadBytes());
                }
                break;

            case XBeePacket.AMT_TX_STATUS:
                st = (XBeeTxStatusPacket) p;
                if( st.statusOK() ) {
                    if( debug )
                        System.out.printf("[debug] Tx packet-%d OK -- received on Rx side.%n", st.frameID());

                } else {
                    System.err.printf("ERROR Rx did not say it received packet-%d%n", st.frameID());
                }
                break;

            default:
                if( dump_unhandled )
                    p.fileDump(String.format("%s-recv-%02x-%s.pkt", name, bType, "%d"));

                System.err.printf("%s Packet type: %02x ignored -- unhandled type.%n", name, bType);
        }
    }
    // }}}
    // public void registerMessageReceiver(MessageRecvEvent callback) {{{
    public void registerMessageReceiver(MessageRecvEvent callback) {
        messageReceiver = callback;
    }
    // }}}

    // --------------------------- Modem Accessors Support -------------------------

    // private void sendATcmds(String cmds[][]) {{{
    private void sendATcmds(String cmds[][]) {
        XBeePacket atp[] = xp.at(cmds);

        for(int i=0; i<atp.length; i++) {
            try {
                xh.send_packet(atp[i]);

            } catch(IOException e) {
                System.err.println("error sending at packet(" + i + "): " + e.getMessage());
                return;
            }
        }
    }
    // }}}

    // private void getAddress() {{{
    private void getAddress() {
        int retries;

        while( a == null ) {
            retries = 15;
            String cmds[][] = { { "SH" }, { "SL" } };

            if( debug )
                System.out.println("[debug] sending ATSH and ATSL packets");

            sendATcmds(cmds);

            while( a == null && (retries--) > 0)
                try { Thread.sleep(150); } catch (InterruptedException e) {}
        }
    }
    // }}}
    // private void getFirmwareVersion() {{{
    private void getFirmwareVersion() {
        int retries;

        while( firmwareVersion == null ) {
            retries = 15;
            String cmds[][] = { { "VR" } };

            if( debug )
                System.out.println("[debug] sending ATVR packets");

            sendATcmds(cmds);

            while( firmwareVersion == null && (retries--) > 0)
                try { Thread.sleep(150); } catch (InterruptedException e) {}
        }
    }
    // }}}
    // private void getHardwareVersion() {{{
    private void getHardwareVersion() {
        int retries;

        while( hardwareVersion == null ) {
            retries = 15;
            String cmds[][] = { { "HV" } };

            if( debug )
                System.out.println("[debug] sending ATHV packets");

            sendATcmds(cmds);

            while( hardwareVersion == null && (retries--) > 0)
                try { Thread.sleep(150); } catch (InterruptedException e) {}
        }
    }
    // }}}

    // --------------------------- Object Accessors -------------------------

    // public String firmwareVersion() {{{
    public String firmwareVersion() {
        if( firmwareVersion == null )
            getFirmwareVersion();

        return firmwareVersion;
    }
    // }}}
    // public String hardwareVersion() {{{
    public String hardwareVersion() {
        if( hardwareVersion == null )
            getHardwareVersion();

        return hardwareVersion;
    }
    // }}}
    // public Address64 addr() {{{
    public Address64 addr() {
        if( a == null )
            getAddress();

        return a;
    }
    // }}}
    // public name getName() {{{
    public String getName() {
        return name;
    }
    // }}}

    // public void send(Address64 dst, String message) {{{
    public void send(Address64 dst, String message) {
        QoQ.append( xp.tx(dst,message) );
    }
    // }}}
    // public void close() {{{
    public void close() {
        if( QoQ != null ) QoQ.close();
        if( xh  != null ) xh.close();
    }
    // }}}

    // -------------------- Outbound Queue Delivery (QoQ) -------------------

    private static class PacketQueueWriter implements Runnable {
        XBeeHandle xh;
        Queue <Queue <XBeePacket>> OutboundQueue; // not synched, so the append and pop functions must be
        ACKQueue currentDatagram; // this is object synched so the ACK and send functions do not need to be
        boolean closed = false;

        public void close() { closed = true; }

        public synchronized void append(Queue <XBeePacket> q) {
            // block while we've already got enough to do
            while(OutboundQueue.size() > 50)
                try { Thread.sleep(150); }
                catch(InterruptedException e) {/* we go around again either way */}

            OutboundQueue.add(q);
        }

        public PacketQueueWriter(XBeeHandle _xh) {
            xh = _xh;
            OutboundQueue = new ArrayDeque< Queue <XBeePacket> >();
        }

        public void receiveACK(int frameID) {
            if( currentDatagram == null )
                return;

            currentDatagram.ACK(frameID);
        }

        private void sendCurrentDatagram() {
            XBeePacket datagram[] = currentDatagram.packets();

            for( int i=0; i<datagram.length; i++ ) {
                if( closed )
                    return;

                try {
                    xh.send_packet(datagram[i]);
                }

                catch(IOException e) {
                    // Ucky, try again in a couple seconds
                    try { Thread.sleep(2 * 1000); }
                    catch(InterruptedException f) {/* we go around again either way */}
                }
            }
        }

        private void dealWithDatagram() {
            if( currentDatagram == null )
                return;

            while( currentDatagram.size() > 0 ) {
                sendCurrentDatagram();

                try { Thread.sleep(250); } catch(InterruptedException e) {/* we go around again either way */}
            }

            clearCurrentDatagram();
        }

        private synchronized void clearCurrentDatagram() {
            currentDatagram = null;
        }

        private synchronized void lookForDatagram() {
            Queue <XBeePacket> tmp = OutboundQueue.poll();

            if( tmp != null )
                currentDatagram = new ACKQueue(tmp);
        }

        public void run() {
            while(!closed) {
                lookForDatagram();
                dealWithDatagram();

                try { Thread.sleep(150); } catch(InterruptedException e) {/* we go around again either way */}
            }
        }

    }

    // --------------------------- Handle Factories -------------------------

    NetworkEndpointHandle(String _n) {
        name = _n;

        debug = TestENV.test("DEBUG") || TestENV.test("NEH_DEBUG");
        dump_unhandled = TestENV.test("NEH_DUMP_UNHANDLED_PACKETS");
    }

    public static NetworkEndpointHandle configuredEndpoint(String name, boolean announce) throws XBeeConfigException {
        NetworkEndpointHandle h = new NetworkEndpointHandle(name);
        h.locateAndConfigure();

        if( announce ) {
            System.out.println(name + " Address: " + h.addr().toText());
            System.out.println("  Hardware version: " + h.hardwareVersion());
            System.out.println("  Firmware version: " + h.firmwareVersion());
        }

        return h;
    }

}
