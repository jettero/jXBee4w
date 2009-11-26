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

    public static int config(CommPortIdentifier port, int speed) {
        int result = UNKNOWN;

        try {
            XBeeConfig c = new XBeeConfig(port, speed, debug);

            try {
                String conf[]    = { "ATRE", "ATBD7", "ATAP1", "ATHV", "ATVR" };
                Pattern expect[] = new Pattern[ conf.length ];

                Pattern _OK = Pattern.compile("^OK$");
                expect[0] = expect[1] = expect[2] = _OK;
                expect[conf.length-1] = Pattern.compile("^10CD$");

                String res[] = c.config(conf, expect);
                for(int i=0; i<conf.length; i++)
                    System.out.println(conf[i] + " result: " + res[i]);

                result = CONFIGURED; // used by the linespeed retry loop

                // not a debug message
                System.out.println("XBee version " + conf[conf.length-2]
                    + " running firmware revision " + conf[conf.length-1] + " configured successfully");

            } catch( XBeeConfigException e ) {
                System.err.println("ERROR configuring modem: " + e.getMessage());
                result = CONFIG_ERR;

                if( e.probably_linespeed )
                    result = SPEED_ERR;
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

    private static void populatePortNames() {
        if( ports != null )
            return;

        ports = new ArrayDeque<CommPortIdentifier>();

        Enumeration portIdentifiers = CommPortIdentifier.getPortIdentifiers();
        while (portIdentifiers.hasMoreElements()) {
            CommPortIdentifier pid = (CommPortIdentifier) portIdentifiers.nextElement();

            if(pid.getPortType() == CommPortIdentifier.PORT_SERIAL )

                ports.offer(pid); // returns false if it's full, but who cares, we're expecting like 8 things tops
                System.out.println("in " + pid.getName());
        }
    }

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

    public void storeAddress(byte []sh, byte[]sl) {
        a = new Address64(sh, sl);

        if( debug )
            System.out.println("   SH+SL => " + a.toText());
    }

    public void showResponse(XBeeATResponsePacket p) {
        String cmd = p.cmd();

        if( debug )
            System.out.println("received AT" + p.cmd() + " response.");

        if( cmd.equals("SL") ) {
            SL = p.responseBytes();
            if( SH != null && SL != null )
                storeAddress(SH, SL);
        }

        if( cmd.equals("SH") ) {
            SH = p.responseBytes();
            if( SH != null && SL != null )
                storeAddress(SH, SL);
        }
    }

    public void showMessage(XBeeRxPacket p) {
        System.out.println("rx"); // TODO: write this
    }

    public void recvPacket(XBeePacket p) {
        if( debug )
            p.fileDump("recv-%d.pkt");

        switch(p.type()) {
            case XBeePacket.AMT_AT_RESPONSE: showResponse( (XBeeATResponsePacket) p ); break;
            case XBeePacket.AMT_RX64:        showMessage(  (XBeeRxPacket)         p ); break;

            default:
                System.err.printf("Packet type: %02x ignored — unhandled type");
        }
    }

    public NetworkEndpointHandle configuredEndpoint() throws XBeeConfigException {
        NetworkEndpointHandle h = new NetworkEndpointHandle();
        h.locateAndConfigure();

        return h;
    }
}
