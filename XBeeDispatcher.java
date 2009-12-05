// XBee Packet message dispatch (Rx and Tx) and config handling

import gnu.io.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class XBeeDispatcher implements PacketRecvEvent {
    private static Queue<CommPortIdentifier> ports;

    private static boolean debug = false;
    private static boolean dump_unhandled = false;

    private PacketQueueWriterDestinationMap PQW;

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

    static {
        debug = TestENV.test("DEBUG") || TestENV.test("XD_DEBUG");
        dump_unhandled = TestENV.test("XD_DUMP_UNHANDLED_PACKETS");
    }

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
            System.out.printf("XBeeDispatcher(%s) Looking for XBee on port %s%n", name, pid.getName());

            for(int i=0; i<speeds.length; i++) {
                int result = XBeeConfig.config(pid, speeds[i]); // try to config

                if( result == XBeeConfig.CONFIGURED ) {
                    try {
                        xh = new XBeeHandle(name, pid, 115200, this);
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

                    PQW = new PacketQueueWriterDestinationMap( String.format("%s:%s", name, addr().toText()), xh);

                    return; // it worked, great, return out of there
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
            System.out.printf("[debug] XBeeDispatcher(%s) Address found SH+SL => %s%n", name, a.toText());
    }
    // }}}
    // public void handleATResponse(XBeeATResponsePacket p) {{{
    public void handleATResponse(XBeeATResponsePacket p) {
        String cmd = p.cmd();

        if( p.statusOK() ) {
            byte b[] = p.responseBytes();

            if( debug )
                System.out.printf("[debug] XBeeDispatcher(%s) received valid AT%s response.%n", name, cmd);

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
            // else if( cmd.equals("CH") ) { System.out.printf("[debug] XBeeDispatcher(%s) channel changed%n", name); }

        } else {
            if( p.statusError() ) {
                System.err.printf("XBeeDispatcher(%s) Error sending %s command.  Bad params?%n", name, cmd);

            } else if( p.statusInvalidCommand() ) {
                System.err.printf("XBeeDispatcher(%s) Invlaid Command error sending %s command.  Bad command?%n", name, cmd);

            } else {
                System.err.printf("XBeeDispatcher(%s) Unhandled error sending %s command.%n", name, cmd);
            }
        }

    }
    // }}}

    // public void recvPacket(XBeePacket p) {{{
    public void recvPacket(XBeePacket p) {
        byte bType = p.type();
        XBeeRxPacket rx;
        XBeeTxStatusPacket st;

        if( debug )
            System.out.printf("[debug] XBeeDispatcher(%s) Rx'd a packet of type %02x, dispatching...%n", name, bType);

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

                    // XXX: Sadly, TxStatus packets *DO NOT* contain an address
                    // at all probably, the frameID could later be used to
                    // match the write pqw, but this is ok for now
                    PQW.receiveACK(st.frameID());

                    if( debug )
                        System.out.printf("[debug] XBeeDispatcher(%s) Tx packet-%d OK -- received on Rx side.%n", name, st.frameID());

                } else {
                    PQW.receiveNACK(st.frameID());

                    if( debug )
                        System.out.printf("[debug] XBeeDispatcher(%s) Rx did not say it received packet-%d.%n", name, st.frameID());
                }
                break;

            default:
                if( dump_unhandled )
                    p.fileDump(String.format("%s-recv-%02x-%s.pkt", name, bType, "%d"));

                System.err.printf("XBeeDispatcher(%s) Packet type: %02x ignored -- unhandled type.%n", name, bType);
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
        System.err.printf("XBeeDispatcher(%s) warning: some inconsistency found, discarding current message: %s%n", name, e.getMessage());

        Message m = new Message(payload);
        incoming.put( src, m );
    }

    private void handleIncomingMessage( XBeeRxPacket rx ) {
        if( debug )
            System.out.printf("[debug] XBeeDispatcher(%s) rx packet...%n", name);

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
                xh.send_packet(atp[i]); // this method is synchronized, no worries on timing

            } catch(IOException e) {
                System.err.printf("XBeeDispatcher(%s) error sending at packet(%d): %s%n", name, i, e.getMessage());
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
                System.out.printf("[debug] XBeeDispatcher(%s) sending ATSH and ATSL packets%n", name);

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
                System.out.printf("[debug] XBeeDispatcher(%s) sending ATVR packets%n", name);

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
                System.out.printf("[debug] XBeeDispatcher(%s) sending ATHV packets%n", name);

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
        PacketQueueWriter pw = PQW.get(dst);

        pw.append( xp.tx(dst, message) );
    }
    // }}}
    // public void close() {{{
    public void close() {
        PQW.closeAll();

        if( xh != null )
            xh.close();
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
    }

    public static XBeeDispatcher configuredDispatcher(String name, boolean announce) throws XBeeConfigException {
        XBeeDispatcher h = new XBeeDispatcher(name);
        h.locateAndConfigure();

        if( announce ) {
            byte hv[]  = h.hardwareVersion();
            byte vr[]  = h.firmwareVersion();
            byte ch[]  = h.channelRange();   // differs from radio to radio (used to calculate f[])
            double f[] = h.frequencyRange(); // differs from radio to radio

            System.out.printf("XBeeDispatcher(%s) Address: %s%n", name, h.addr().toText());
            System.out.printf("  Hardware version: %02x%02x%n", vr[0], vr[1]);
            System.out.printf("  Firmware version: %02x%02x%n", vr[0], vr[1]);
            System.out.printf("  XBee Type:        %s%n", h.hardwareTypeString());
            System.out.printf("  XBee Channels:    %02x - %02x%n", ch[0], ch[1]);
            System.out.printf("  XBee Frequencies: %.3f - %.3f GHz%n", f[0], f[1]);
        }

        return h;
    }

}
