// XBee Packet message dispatch (Rx and Tx) and config handling

import gnu.io.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class XBeeDispatcher implements PacketRecvEvent {
    private static Queue<CommPortIdentifier> ports;

    private static boolean debug = false;
    private static boolean dump_unhandled = false;

    private PacketQueueWriter pw;
    private XBeeHandle xh;
    private XBeePacketizer xp;
    private Address64 a;

    private String name;
    private byte[] SH, SL;
    private byte hardwareVersion[];
    private byte firmwareVersion[];

    private HashMap <Address64, Message> incoming;
    private MessageRecvEvent messageReceiver;
    private RawRecvEvent rawReceiver;

    private Thread _qThread; // keep a ref to the pw thread here

    // --------------------------- modem locator stuff -------------------------

    // private static void populatePortNames() {{{
    private static void populatePortNames() {
        String skip    = System.getenv("XD_SKIP_PORT");
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

                    pw = new PacketQueueWriter(xh); // start our pw writer

                    _qThread = new Thread(pw);
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
                firmwareVersion = b;

            } else if( cmd.equals("HV") ) {
                hardwareVersion = b;

            } else if( cmd.equals("SL") ) {
                SL = b;
                if( SH != null )
                    storeAddress(SH, SL);

            } else if( cmd.equals("SH") ) {
                SH = b;
                if( SL != null )
                    storeAddress(SH, SL);
            }

            // this doesn't like confirm the new channel or anything... we'd
            // probably get a status error or something if it didn't work
            // else if( cmd.equals("CH") ) { System.out.println("[debug] channel changed"); }

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

    // public void recvPacket(XBeePacket p) {{{
    public void recvPacket(XBeePacket p) {
        byte bType = p.type();
        XBeeRxPacket rx;
        XBeeTxStatusPacket st;

        switch(bType) {
            case XBeePacket.AMT_AT_RESPONSE: handleATResponse( (XBeeATResponsePacket) p ); break;
            case XBeePacket.AMT_RX64:
                rx = (XBeeRxPacket) p;

                if( rawReceiver != null )
                    rawReceiver.recvPacket(this, rx);

                handleIncomingMessage( rx );

                break;

            case XBeePacket.AMT_TX_STATUS:
                st = (XBeeTxStatusPacket) p;

                if( st.statusOK() ) {

                    pw.receiveACK(st.frameID());

                    if( debug )
                        System.out.printf("[debug] Tx packet-%d OK -- received on Rx side.%n", st.frameID());

                } else {
                    if( debug )
                        System.out.printf("[debug] Rx did not say it received packet-%d.%n", st.frameID());

                    pw.receiveNACK(st.frameID());
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
    // public void registerRawReceiver(MessageRecvEvent callback) {{{
    public void registerRawReceiver(RawRecvEvent callback) {
        rawReceiver = callback;
    }
    // }}}

    // private void handleIncomingMessage( XBeeRxPacket rx ) {{{
    private void _handleIncomingMessageException( IOException e, Address64 src, byte payload[] ) {
        System.err.println("warning: some inconsistency found, discarding current message: " + e.getMessage());
        Message m = new Message(payload);
        incoming.put( src, m );
    }

    private void handleIncomingMessage( XBeeRxPacket rx ) {
        if( debug )
            System.out.println("[debug] rx packet...");

        if( messageReceiver != null ) {
            if( incoming == null )
                incoming = new HashMap<Address64, Message>();

            Address64 src  = rx.getSourceAddress();
            byte payload[] = rx.getPayloadBytes();

            Message m;

            if( incoming.containsKey( src ) ) {
                m = incoming.get(src);

                try                  { m.addBlock( payload ); }
                catch(IOException e) { _handleIncomingMessageException(e, src, payload); }

            } else {
                m = new Message(payload);
                incoming.put( src, m );
            }

            boolean whole = false;
            try { whole = m.wholeMessage(); }
            catch(IOException e) {
                _handleIncomingMessageException(e, src, payload);
                return;
            }

            if( whole ) {
                try {
                    byte msgBytes[] = m.reconstructMessage();
                    messageReceiver.recvMessage(this, src, msgBytes);
                    incoming.remove(src);
                }
                catch(IOException e) {
                    _handleIncomingMessageException(e, src, payload);
                }
            }
        }
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

    // public byte[] firmwareVersion() {{{
    public byte[] firmwareVersion() {
        if( firmwareVersion == null )
            getFirmwareVersion();

        return firmwareVersion;
    }
    // }}}
    // public byte[] hardwareVersion() {{{
    public byte[] hardwareVersion() {
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
        pw.append( xp.tx(dst, message) );
    }
    // }}}
    // public void close() {{{
    public void close() {
        if( pw != null ) pw.close();
        if( xh != null ) xh.close();
    }
    // }}}

    // -------------------- Outbound Queue Delivery (pw) -------------------

    // private static class PacketQueueWriter implements Runnable {{{
    private static class PacketQueueWriter implements Runnable {
        XBeeHandle xh;
        Queue <Queue <XBeeTxPacket>> OutboundQueue; // not synched, so the append and pop functions must be
        ACKQueue currentDatagram; // this is object synched so the ACK and send functions do not need to be
        boolean closed = false;

        public void close() { closed = true; }

        public synchronized void append(Queue <XBeeTxPacket> q) {
            // block while we've already got enough to do
            while(OutboundQueue.size() > 50)
                try { Thread.sleep(150); }
                catch(InterruptedException e) {/* we go around again either way */}

            OutboundQueue.add(q);
        }

        public PacketQueueWriter(XBeeHandle _xh) {
            xh = _xh;
            OutboundQueue = new ArrayDeque< Queue <XBeeTxPacket> >();
        }

        public void receiveNACK(int frameID) {
            if( currentDatagram == null )
                return;

            if( debug )
                System.out.printf("[debug] PacketWriter - receiveNACK(%d)%n", frameID);

            currentDatagram.NACK(frameID);
        }

        public void receiveACK(int frameID) {
            if( currentDatagram == null )
                return;

            if( debug )
                System.out.printf("[debug] PacketWriter - receiveACK(%d)%n", frameID);

            currentDatagram.ACK(frameID);
        }

        private void sendCurrentDatagram() {
            boolean resetNACKCount = true;
            XBeePacket datagram[] = currentDatagram.packets(resetNACKCount);

            for( int i=0; i<datagram.length; i++ ) {
                if( closed )
                    return;

                try {
                    if( debug )
                        System.out.printf("[debug] PacketWriter - sendCurrentDatagram(%d)%n", i);

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
            if( closed || currentDatagram == null )
                return;

            if( debug )
                System.out.println("[debug] PacketWriter - dealWithCurrentDatagram()");

            boolean firstLoop = true;

            while( !closed && currentDatagram.size() > 0 ) {
                if( firstLoop || (currentDatagram.NACKCount() >= currentDatagram.size()) ) {
                    sendCurrentDatagram();
                    firstLoop = false;
                }

                try { Thread.sleep(250); } catch(InterruptedException e) {/* we go around again either way */}
            }

            clearCurrentDatagram();
        }

        private synchronized void clearCurrentDatagram() {
            if( debug )
                System.out.println("[debug] PacketWriter - clearCurrentDatagram()");

            currentDatagram = null;
        }

        private synchronized void lookForDatagram() {
            Queue <XBeeTxPacket> tmp = OutboundQueue.poll();

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
    // }}}

    // --------------------------- Radio Type Information -------------------

    // public byte[] channelRange() {{{
    public byte[] channelRange() {
        byte XBee[]    = { 0xb, 0x1a };
        byte XBeePro[] = { 0xc, 0x17 };

        if( isPro() )
            return XBeePro;

        return XBee;
    }
    // }}}
    // public double[] frequencyRange() {{{
    public double[] frequencyRange() {
        byte ch[] = channelRange();
        double ret[] = new double[2];

        ret[0] = CH2Freq(ch[0]);
        ret[1] = CH2Freq(ch[1]);

        return ret;
    }
    // }}}
    // public boolean isPro() {{{
    public boolean isPro() {
        return isPro(hardwareVersion());
    }
    // }}}
    // public String hardwareTypeString() {{{
    public String hardwareTypeString() {
        return HW2RadioTypeString(hardwareVersion());
    }
    // }}}

    // --------------------------- Static Converters ----------------------

    // public static boolean isPro(byte b[]) {{{
    public static boolean isPro(byte b[]) {
        if( b[0] == 0x17 || b[0] == 0x19 )
            return false; // nope -- oddly, this has the wider range of channel choices

        // if( b[0] == 0x18 || b[0] == 0x1a )
        return true; // techincally unknown, but the Pro has a narrower frequency range, so it's a safer guess
    }
    // }}}
    // public static String HW2RadioTypeString(byte b[]) {{{
    public static String HW2RadioTypeString(byte b[]) {
        switch(b[0]) {
            case 0x17: return "XBee24 (series 1)";    // this is what I have (paul.e.miller@wmich.edu)
            case 0x18: return "XBeePro24 (series 1)"; // this is what Dr. Fuqaha has

            case 0x19: return "XBee24 (series 2)";    // I gather these are being made now, who knows
            case 0x1a: return "XBeePro24 (series 2)"; //
        }

        return "unknown";
    }
    // }}}
    // public static double CH2Freq(byte CH) {{{
    public static double CH2Freq(byte CH) {
        int ch = CH & 0xff;

            // GHz        MHz             1GHz/1000MHz
        return 2.405 + (((ch - 11) * 5) / 1000.0);
    }
    // }}}

    // --------------------------- Handle Factories -------------------------

    public void setChannel(int ch) throws IllegalArgumentException { setChannel( (byte) ch ); }
    public void setChannel(byte ch) throws IllegalArgumentException {
        byte _c[] = channelRange();
        if( ch < _c[0] ) throw new IllegalArgumentException("channel number is too low");
        if( ch > _c[1] ) throw new IllegalArgumentException("channel number is too high");

        byte b[] = new byte[1]; b[0] = ch;
        String cmds[][] = { { "CH", new String(b) } };
        sendATcmds(cmds);
    }

    XBeeDispatcher(String _n) {
        name = _n;

        debug = TestENV.test("DEBUG") || TestENV.test("XD_DEBUG");
        dump_unhandled = TestENV.test("XD_DUMP_UNHANDLED_PACKETS");
    }

    public static XBeeDispatcher configuredDispatcher(String name, boolean announce) throws XBeeConfigException {
        XBeeDispatcher h = new XBeeDispatcher(name);
        h.locateAndConfigure();

        if( announce ) {
            byte hv[]  = h.hardwareVersion();
            byte vr[]  = h.firmwareVersion();
            byte ch[]  = h.channelRange();   // differs from radio to radio (used to calculate f[])
            double f[] = h.frequencyRange(); // differs from radio to radio

            System.out.printf("%s Address: %s%n", name, h.addr().toText());
            System.out.printf("  Hardware version: %02x%02x%n", vr[0], vr[1]);
            System.out.printf("  Firmware version: %02x%02x%n", vr[0], vr[1]);
            System.out.printf("  XBee Type:        %s%n", h.hardwareTypeString());
            System.out.printf("  XBee Channels:    %02x - %02x%n", ch[0], ch[1]);
            System.out.printf("  XBee Frequencies: %.3f - %.3f GHz%n", f[0], f[1]);
        }

        return h;
    }

}
