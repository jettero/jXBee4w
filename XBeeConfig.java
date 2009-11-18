import gnu.io.*;
import java.io.*;
import java.util.*;
import java.nio.*;

public class XBeeConfig {
    public boolean debug;

    private InputStream  in;
    private OutputStream out;
    private String s[];
    private CommPort commPort;

    protected void finalize() throws Throwable { this.close(); }
    public    void close() { commPort.close(); }

    XBeeConfig(String portName, int speed) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
        commPort = portIdentifier.open(this.getClass().getName(), 2000);

        if ( commPort instanceof SerialPort ) {
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

    public String[] config(String settings[]) throws IOException {
        try { Thread.sleep(1000); } catch (InterruptedException e) {} // just ignore it if it gets interrupted

        byte b[] = this.send_and_recv("+++");
        if( b[0] != 'O' || b[1] != 'K' )
            throw new XBeeConfigException("Coulnd't get the modem to drop into config mode.  Maybe bad linespeed.");

        String responses[] = new String[ settings.length ];

        for(int i=0; i<settings.length; i++)
            responses[i] = new String(this.send_and_recv(settings[i]+"\r"));

        b = this.send_and_recv("ATCN\r");
        if( b[0] != 'O' || b[1] != 'K' )
            throw new XBeeConfigException("Mostly the settings seemed to commit ok, but there was some problem exiting command mode");

        return responses;
    }
}
