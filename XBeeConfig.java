import gnu.io.*;
import java.io.*;
import java.util.*;
import java.nio.*;
import java.util.regex.*;

public class XBeeConfig {
    public boolean debug;

    private InputStream  in;
    private OutputStream out;
    private String s[];
    private CommPort commPort;

    protected void finalize() throws Throwable { this.close(); }
    public    void close() { commPort.close(); }

    XBeeConfig(String portName, int speed, boolean d) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        debug = d;
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
        commPort = portIdentifier.open(this.getClass().getName(), 2000);

        if ( commPort instanceof SerialPort ) {
            if( debug )
                System.out.println("[debug] opening port: " + portName + " at " + speed);

            SerialPort serialPort = (SerialPort) commPort;
            serialPort.setSerialPortParams(speed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

            in  = serialPort.getInputStream();
            out = serialPort.getOutputStream();

        } else {
            throw new UnsupportedCommOperationException("\"" + portName + "\" is probably not a serial port");
        }
    }

    public byte[] send_and_recv(String toSend) throws IOException {
        if( debug )
            System.out.println("[debug] send: " + toSend);

        this.out.write(toSend.getBytes());

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

        if( debug )
            System.out.println( "[debug] recv: \"" + (new String(res)).trim() +
            "\"        bytes available before read: " + bytes_available + " retries @ 100ms: " + (15-retries) );

        return res;
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

        b = this.send_and_recv("ATCN\r");
        if( !(new String(b)).trim().equals("OK") ) {
            XBeeConfigException x = new XBeeConfigException("there was some problem exiting command mode");
            x.command_mode = true;
            throw x;
        }

        return responses;
    }
}
