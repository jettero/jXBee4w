import gnu.io.*;
import java.io.*;
import java.util.*;
import java.nio.*;
import java.util.regex.*;

public class XBeeConfig {
    public static final int UNKNOWN    = 0;
    public static final int CONFIGURED = 1;
    public static final int CONFIG_ERR = 2;
    public static final int SPEED_ERR  = 4;
    public static final int PORT_ERR   = 8;

    public static final String FIRMWARE_REV_RE = "^10CD$";

    private static boolean debug;

    private InputStream  in;
    private OutputStream out;
    private CommPort commPort;
    private SerialPort serialPort;

    private static XBeePacketizer packetizer;

    protected void finalize() throws Throwable { close(); }
    public    void close() { commPort.close(); }

    XBeeConfig(CommPortIdentifier portIdentifier, int speed) throws PortInUseException, UnsupportedCommOperationException, IOException {
        commPort = portIdentifier.open(getClass().getName(), 50);

        debug = TestENV.test("DEBUG") || TestENV.test("XBEECONFIG_DEBUG");

        if( packetizer == null )
            packetizer = new XBeePacketizer();

        if ( commPort instanceof SerialPort ) {
            if( debug )
                System.out.println("[debug] opening port: " + portIdentifier.getName() + " at " + speed);

            serialPort = (SerialPort) commPort;
            serialPort.setSerialPortParams(speed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

            in  = serialPort.getInputStream();
            out = serialPort.getOutputStream();

        } else {
            throw new UnsupportedCommOperationException("\"" + portIdentifier.getName() + "\" is probably not a serial port");
        }
    }

    public byte[] send_and_recv(String toSend) throws IOException {
        if( debug )
            System.out.println("[debug] send: " + toSend);

        byte res[] = send_and_recv(toSend.getBytes());

        if( debug )
            System.out.println( "[debug] recv: \"" + (new String(res)).trim() + "\"" );

        return res;
    }

    public byte[] send_and_recv(byte bytesToSend[]) throws IOException {
        int bLen = 0;

        if( bytesToSend != null ) {
            bLen =bytesToSend.length;

            serialPort.setRTS(true);

            for(int i=0; i<bLen; i++) {

                while(!serialPort.isCTS()) {
                    System.out.println("[debug] ----------------------------------- waiting for cts ----------------------------- ");
                    try { Thread.sleep(100); }
                    catch(InterruptedException e) { /* don't really care if it doesn't work... maybe a warning should go here */ }
                }

                out.write(bytesToSend[i]);
            }

            serialPort.setRTS(false);
        }

        if( debug )
            System.out.printf("[debug] wrote %d bytes to port.%n", bLen);

        int retries = 15;
        int bytes_available;
        while( (bytes_available = in.available()) < 1 && retries-->0 )
            try { Thread.sleep(100); }
            catch(InterruptedException e) { /* don't really care if it doesn't work... maybe a warning should go here */ }

        ByteBuffer b = ByteBuffer.wrap(new byte[1024]);

        int aByte;
        while( (aByte = in.read()) > -1 )
            try { b.put( (byte) aByte ); }
            catch(BufferOverflowException e) { /* huh... just ignore this and return what we can */ }

        byte[] res = new byte[ b.position() ];
        b.clear(); // go back to the start
        b.get(res); // because we get from 0

        return res;
    }

    public void clearInput() throws IOException {
        // just write nothing a couple times:
        send_and_recv((byte[]) null); send_and_recv((byte[]) null);
    }

    public boolean mightAlreadyBeConfigured() {
        String cmds[][] = { {"AP"}, {"BD"}, {"MY"} };
        byte val[][] = { {0x01}, {0x07}, {(byte)0xff, (byte)0xff} };

        XBeePacket configs[] = packetizer.at(cmds);
        int retries = 5;
        int cur = 0;

        System.out.print("Checking QuickConfig... ");
        System.out.flush();

        try {
            while( retries --> 0 && cur < configs.length ) {
                if( debug )
                    System.out.printf("[debug] sending %s command %n", cmds[cur][0]);

                byte response[] = send_and_recv(configs[cur].getBytes());

                if( response.length >= 4 ) {
                    XBeePacket p = new XBeePacket(response);
                    if( p.checkPacket() ) {
                        p = p.adapt();

                        if( p instanceof XBeeATResponsePacket ) {
                            XBeeATResponsePacket r = (XBeeATResponsePacket) p;

                            if( r.cmd().equals(cmds[cur][0]) && r.statusOK() ) {
                                byte rB[] = r.responseBytes();
                                int j = rB.length - val[cur].length;
                                boolean match = true;

                                if( j < 0 )
                                    match = false;

                                for(int i=0; match && i<val[cur].length; i++)
                                    if( rB[j+i] != val[cur][i] )
                                        match = false;

                                if( match ) {
                                    cur++;
                                    continue;

                                } else {
                                    if( debug )
                                        System.out.printf("[debug] bad config result (%d vs %d), recommending reconfigure%n", r.responseBytes()[1], val[cur]);

                                    return false;
                                }

                            } else {
                                if( debug )
                                    System.out.printf("[debug] incorrect or invalid command response (%s), retrying%n", r.cmd());
                            }

                        } else {
                            if( debug )
                                System.out.printf("[debug] received packet wasn't an AT Response, retrying%n");
                        }

                    } else {
                        if( debug )
                            System.out.printf("[debug] didn't receive a valid packet (%d bytes), retrying%n", response.length);
                    }

                } else {
                    if( debug )
                        System.out.printf("[debug] didn't receive enough bytes from the modem (%d actually), retrying%n", response.length);
                }
            }
        }

        catch( IOException e ) { /* this doesn't seem configured, fall through and return false */ }

        if( cur == configs.length ) {
            System.out.println("OK");
            return true;
        }

        System.out.println("FAIL");

        return false;
    }

    public static int config(String portName, int speed) {
        CommPortIdentifier port;
        try { port = CommPortIdentifier.getPortIdentifier(portName); }

        catch(gnu.io.NoSuchPortException e) {
            System.err.println("ERROR opening port: No Such Port Error");
            return PORT_ERR;
        }

        boolean force = false;
        String _force = System.getenv("FORCE_CONFIG");
        if( _force != null )
            if( !_force.isEmpty() )
                if( !_force.equals("0") )
                    force = true;

        return config(port, speed, force);
    }

    public static int config(String portName, int speed, boolean force) {
        CommPortIdentifier port;
        try { port = CommPortIdentifier.getPortIdentifier(portName); }

        catch(gnu.io.NoSuchPortException e) {
            System.err.println("ERROR opening port: No Such Port Error");
            return PORT_ERR;
        }

        return config(port, speed, force);
    }

    public static int config(CommPortIdentifier port, int speed) {
        boolean force = false;
        String _force = System.getenv("FORCE_CONFIG");
        if( _force != null )
            if( !_force.isEmpty() )
                if( !_force.equals("0") )
                    force = true;

        return config(port, speed, force);
    }

    public static int config(CommPortIdentifier port, int speed, boolean force) {
        int result = UNKNOWN;


        try {
            XBeeConfig c = new XBeeConfig(port, speed);

            if( !force && speed == 115200 ) { // only bother with this test when applicable
                if( c.mightAlreadyBeConfigured() ) {
                    if( debug )
                        System.out.println("[debug] XBee seems to have been previously configured.");

                    c.close();
                    return XBeeConfig.CONFIGURED;
                }
            }

            System.out.print("Issueing LongConfig... ");
            System.out.flush();

            try {
                String conf[]    = { "ATRE", "ATBD7", "ATAP1", "ATMYFFFF", "ATVR" };
                Pattern expect[] = new Pattern[ conf.length ];

                Pattern _OK = Pattern.compile("^OK$");
                expect[0] = expect[1] = expect[2] = _OK;
                expect[conf.length-1] = Pattern.compile(FIRMWARE_REV_RE);

                String res[] = c.config(conf, expect);
                if( debug )
                    for(int i=0; i<conf.length; i++)
                        System.out.println(conf[i] + " result: " + res[i]);

                result = CONFIGURED; // used by the linespeed retry loop

                // not a debug message
                System.out.print("firmware revision: " + conf[conf.length-1] + " ");

            } catch( XBeeConfigException e ) {
                System.err.println("ERROR configuring modem: " + e.getMessage());
                result = CONFIG_ERR;
                if( e.probably_linespeed )
                    result = SPEED_ERR;
            }

            c.close();
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

        if( result == CONFIGURED )
            System.out.println("OK");

        return result;
    }

    public String[] config(String settings[], Pattern expect[]) throws IOException, XBeeConfigException {
        try { Thread.sleep(1000); } catch (InterruptedException e) {} // just ignore it if it gets interrupted

        byte b[] = send_and_recv("+++");
        if( !(new String(b)).trim().equals("OK") ) {
            XBeeConfigException x = new XBeeConfigException("coulnd't get the modem to drop into config mode ... linespeed issue?");
            x.probably_linespeed = true;
            throw x;
        }

        String responses[] = new String[ settings.length ];

        for(int i=0; i<settings.length; i++) {

            responses[i] = new String(send_and_recv(settings[i]+"\r"));

            if( expect[i] != null ) {
                Matcher m = expect[i].matcher(responses[i]);

                if( !m.find() ) {
                    XBeeConfigException x = new XBeeConfigException("unexpected config command result");
                    x.command_mode = true;
                    x.user_expect = true;
                    throw x;
                }
            }
        }

        b = send_and_recv("ATWR\r");
        if( !(new String(b)).trim().equals("OK") ) {
            XBeeConfigException x = new XBeeConfigException("there was some problem writing the new configs to NV ram");
            x.command_mode = true;
            throw x;
        }

                                // OK\n with:       modem status watchdog timer reset
        byte ok_with_status[] = { 0x4f, 0x4b, 0x0d, 0x7e, 0x00, 0x02, (byte) 0x8a, 0x01, 0x74 };
        b = send_and_recv("ATFR\r"); // restart under the new settings
        if( !(new String(b)).equals(new String(ok_with_status)) ) {
            XBeeConfigException x = new XBeeConfigException("there was some problem rebooting from command mode");
            x.command_mode = true;
            throw x;
        }

        return responses;
    }
}
