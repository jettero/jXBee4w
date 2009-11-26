import gnu.io.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

public class NetworkEndpointHandle implements PacketRecvEvent {
    public static final int UNKNOWN    = 0;
    public static final int CONFIGURED = 1;
    public static final int CONFIG_ERR = 2;
    public static final int SPEED_ERR  = 4;
    public static final int PORT_ERR   = 8;

    private static Queue<CommPortIdentifier> ports;

    public static boolean debug = false;
    private XBeeHandle xh;
    private Address64 a;
    private byte[] SH, SL;
    private String hardwareVersion;
    private String firmwareVersion;

    public void close() { xh.close(); }

    // --------------------------- modem locator stuff -------------------------

    // public static int config(CommPortIdentifier port, int speed) {{{
    public static int config(CommPortIdentifier port, int speed) {
        int result = UNKNOWN;

        System.out.println("Trying to configure modem on port " + port.getName() + " using linespeedspeed=" + speed);

        try {
            XBeeConfig c = new XBeeConfig(port, speed, debug);

            try {
                String conf[]    = { "ATRE", "ATBD7", "ATAP1" };
                Pattern expect[] = new Pattern[ conf.length ];

                Pattern _OK = Pattern.compile("^OK$");
                expect[0] = expect[1] = expect[2] = _OK;

                String res[] = c.config(conf, expect);

                if( debug )
                    for(int i=0; i<conf.length; i++)
                        System.out.println("[debug] " + conf[i] + " result: " + res[i]);

                result = CONFIGURED; // used by the linespeed retry loop

            } catch( XBeeConfigException e ) {
                if( debug )
                    System.err.println("[debug] ERROR configuring modem: " + e.getMessage());

                result = e.probably_linespeed ? SPEED_ERR : CONFIG_ERR;
            }

            c.close();
        }

        catch(gnu.io.NoSuchPortException e) {
            System.err.println("ERROR opening port: No Such Port Error");
            result = PORT_ERR;
        }

        catch(gnu.io.PortInUseException e) {
            System.err.println("ERROR opening port: port in use");
            result = PORT_ERR;
        }

        catch(gnu.io.UnsupportedCommOperationException e) {
            System.err.println("ERROR opening port: unsupported operation ... " + e.getMessage());
            result = PORT_ERR;
        }

        catch(IOException e) {
            System.err.println("IO ERROR opening port: " + e.getMessage());
            result = PORT_ERR;
        }

        return result;
    }
    // }}}
    // private static void populatePortNames() {{{
    private static void populatePortNames() {
        if( ports != null )
            return;

        ports = new ArrayDeque<CommPortIdentifier>();

        Enumeration portIdentifiers = CommPortIdentifier.getPortIdentifiers();
        while (portIdentifiers.hasMoreElements()) {
            CommPortIdentifier pid = (CommPortIdentifier) portIdentifiers.nextElement();

            if(pid.getPortType() == CommPortIdentifier.PORT_SERIAL )

                ports.offer(pid); // returns false if it's full, but who cares, we're expecting like 8 things tops
        }
    }
    // }}}
    // private void locateAndConfigure() throws XBeeConfigException {{{
    private void locateAndConfigure() throws XBeeConfigException {
        CommPortIdentifier pid;

        populatePortNames();
        int speeds[] = {115200, 9600};

        while( (pid = ports.poll()) != null ) {

            for(int i=0; i<speeds.length; i++) {
                int result = config(pid, speeds[i]); // try to config

                if( result == CONFIGURED ) {
                    try {
                        xh = new XBeeHandle(pid, 115200, debug, this);
                    }

                    catch(Exception e) {
                        String msg = e.getMessage();

                        if( msg.length() < 1 ) {
                            e.printStackTrace(); // no error message, dump a trace instead
                            throw new XBeeConfigException("Unexpected error creating XBeeHandle on configured port (dumped trace)");
                        }

                        throw new XBeeConfigException("Unexpected error creating XBeeHandle on configured port: " + msg);
                    }

                    return; // if it worked, great, return out of there
                }

                if( result != SPEED_ERR )
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
        if( debug )
            p.fileDump("recv-%d.pkt");

        switch(p.type()) {
            case XBeePacket.AMT_AT_RESPONSE: handleATResponse( (XBeeATResponsePacket) p ); break;
            case XBeePacket.AMT_RX64:        showMessage(      (XBeeRxPacket)         p ); break;

            default:
                System.err.printf("Packet type: %02x ignored — unhandled type");
        }
    }
    // }}}

    // --------------------------- Modem Accessors Support -------------------------

    // private void sendATcmds(String cmds[][]) {{{
    private void sendATcmds(String cmds[][]) {
        XBeePacket atp[] = (new XBeePacketizer()).at(cmds);

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
            retries = 8;
            String cmds[][] = { { "SH" }, { "SL" } };

            if( debug )
                System.out.println("[debug] sending ATSH and ATSL packets");

            sendATcmds(cmds);

            while( a == null && (retries--) > 0)
                try { Thread.sleep(1500); } catch (InterruptedException e) {}
        }
    }
    // }}}
    // private void getFirmwareVersion() {{{
    private void getFirmwareVersion() {
        int retries;

        while( firmwareVersion == null ) {
            retries = 8;
            String cmds[][] = { { "VR" } };

            if( debug )
                System.out.println("[debug] sending ATVR packets");

            sendATcmds(cmds);

            while( firmwareVersion == null && (retries--) > 0)
                try { Thread.sleep(1500); } catch (InterruptedException e) {}
        }
    }
    // }}}
    // private void getHardwareVersion() {{{
    private void getHardwareVersion() {
        int retries;

        while( hardwareVersion == null ) {
            retries = 8;
            String cmds[][] = { { "HV" } };

            if( debug )
                System.out.println("[debug] sending ATHV packets");

            sendATcmds(cmds);

            while( hardwareVersion == null && (retries--) > 0)
                try { Thread.sleep(1500); } catch (InterruptedException e) {}
        }
    }
    // }}}

    // --------------------------- Modem Accessors -------------------------

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

    // --------------------------- Handle Factories -------------------------

    public static NetworkEndpointHandle configuredEndpoint() throws XBeeConfigException {
        NetworkEndpointHandle h = new NetworkEndpointHandle();
        h.locateAndConfigure();

        return h;
    }
}
