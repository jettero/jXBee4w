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

    public static boolean debug;

    private InputStream  in;
    private OutputStream out;
    private CommPort commPort;

    protected void finalize() throws Throwable { this.close(); }
    public    void close() { commPort.close(); }

    XBeeConfig(CommPortIdentifier portIdentifier, int speed) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        commPort = portIdentifier.open(this.getClass().getName(), 2000);

        if ( commPort instanceof SerialPort ) {
            if( debug )
                System.out.println("[debug] opening port: " + portIdentifier.getName() + " at " + speed);

            SerialPort serialPort = (SerialPort) commPort;
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

        byte res[] = this.send_and_recv(toSend.getBytes());

        if( debug )
            System.out.println( "[debug] recv: \"" + (new String(res)).trim() + "\"" );

        return res;
    }

    public byte[] send_and_recv(byte bytesToSend[]) throws IOException {
        this.out.write(bytesToSend);

        int retries = 15;
        int bytes_available;
        while( (bytes_available = this.in.available()) < 1 && retries-->0 )
            try { Thread.sleep(100); }
            catch(InterruptedException e) { /* don't really care if it doesn't work... maybe a warning should go here */ }

        ByteBuffer b = ByteBuffer.wrap(new byte[1024]);

        int aByte;
        while( (aByte = this.in.read()) > -1 )
            try { b.put( (byte) aByte ); }
            catch(BufferOverflowException e) { /* huh... just ignore this and return what we can */ }

        byte[] res = new byte[ b.position() ];
        b.clear(); // go back to the start
        b.get(res); // because we get from 0

        return res;
    }

    public boolean mightAlreadyBeConfigured() {
        byte b[] = {
            0x7e, 0x00, 0x04, 0x08, 0x01, 0x41, 0x50, 0x65, // ~....APe
            0x7e, 0x00, 0x04, 0x08, 0x02, 0x42, 0x44, 0x6f, // ~....BDo
            0x7e, 0x00, 0x04, 0x08, 0x03, 0x56, 0x52, 0x4c  // ~....VRL
        };

        try {
            byte r[] = this.send_and_recv(b);
            byte e[] = {
                0x7e, 0x00, 0x06, (byte) 0x88, 0x01, 0x41, 0x50, 0x00, 0x01,
                (byte) 0xe4, 0x7e, 0x00, 0x09, (byte) 0x88, 0x02, 0x42, 0x44,
                0x00, 0x00, 0x00, 0x00, 0x07, (byte) 0xe8, 0x7e, 0x00, 0x07,
                (byte) 0x88, 0x03, 0x56, 0x52, 0x00, 0x10, (byte) 0xcd, (byte)
                0xef };

            if( r.length == e.length ) {
                for(int i=0; i<e.length; i++)
                    if( e[i] != r[i] )
                        return false; // poo

                if( debug )
                    System.out.println("[debug] modem is probably already configured...");

                return true; // woo hoo!
            }

            //// e[], the exemplar, is generated from this super secret byte dump
            // XBeePacket.bytesToFile("ap-mode-config-test-response.dat", r);
            // System.exit(1);
            ////
            // 0000000: 7e00 0688 0141 5000 01e4 7e00 0988 0242  ~....AP...~....B
            // 0000010: 4400 0000 0007 e87e 0007 8803 5652 0010  D......~....VR..
            // 0000020: cdef                                     ..

        }

        catch( IOException e ) { /* this doesn't seem configured, fall through and return false */ }

        return false;
    }

    public static int config(String port, int speed) { return config(port, speed, false); }
    public static int config(String port, int speed, boolean force) {
        int result = UNKNOWN;

        try {
            XBeeConfig c = XBeeConfig.newFromPortName(port, speed); // the last value is whether to print debugging info

            if( !force && speed == 115200 ) // only bother with this test when applicable
                if( c.mightAlreadyBeConfigured() ) {
                    c.close();
                    return XBeeConfig.CONFIGURED;
                }

            try {
                String conf[]    = { "ATRE", "ATBD7", "ATAP1", "ATVR" };
                Pattern expect[] = new Pattern[ conf.length ];

                Pattern _OK = Pattern.compile("^OK$");
                expect[0] = expect[1] = expect[2] = _OK;
                expect[conf.length-1] = Pattern.compile(FIRMWARE_REV_RE);

                String res[] = c.config(conf, expect);
                for(int i=0; i<conf.length; i++)
                    System.out.println(conf[i] + " result: " + res[i]);

                result = CONFIGURED; // used by the linespeed retry loop

                // not a debug message
                System.out.println("XBee running firmware revision " + conf[conf.length-1] + " configured successfully");

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

    public String[] config(String settings[], Pattern expect[]) throws IOException, XBeeConfigException {
        try { Thread.sleep(1000); } catch (InterruptedException e) {} // just ignore it if it gets interrupted

        byte b[] = this.send_and_recv("+++");
        if( !(new String(b)).trim().equals("OK") ) {
            XBeeConfigException x = new XBeeConfigException("coulnd't get the modem to drop into config mode ... linespeed issue?");
            x.probably_linespeed = true;
            throw x;
        }

        String responses[] = new String[ settings.length ];

        for(int i=0; i<settings.length; i++) {

            responses[i] = new String(this.send_and_recv(settings[i]+"\r"));

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

        b = this.send_and_recv("ATWR\r");
        if( !(new String(b)).trim().equals("OK") ) {
            XBeeConfigException x = new XBeeConfigException("there was some problem writing the new configs to NV ram");
            x.command_mode = true;
            throw x;
        }

                                // OK\n with:       modem status watchdog timer reset
        byte ok_with_status[] = { 0x4f, 0x4b, 0x0d, 0x7e, 0x00, 0x02, (byte) 0x8a, 0x01, 0x74 };
        b = this.send_and_recv("ATFR\r"); // restart under the new settings
        if( !(new String(b)).equals(new String(ok_with_status)) ) {
            XBeeConfigException x = new XBeeConfigException("there was some problem rebooting from command mode");
            x.command_mode = true;
            throw x;
        }

        return responses;
    }
}
