import gnu.io.*;
import java.io.*;
import java.util.*;
import java.nio.*;

public class XBeeConfig {
    InputStream  in;
    OutputStream out;
    String s[];

    XBeeConfig(String portName, int speed) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
        CommPort commPort                 = portIdentifier.open(this.getClass().getName(), 2000);

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
        System.out.println("[debug] sending: " + toSend);
        this.out.write(toSend.getBytes());

        int retries = 10;
        while( this.in.available() < 1 && retries-->0 )
            try { Thread.sleep(250); }
            catch(InterruptedException e) { /* don't really care if it doesn't work... maybe a warning should go here */ }

        System.out.println("receiving maybe");

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

    public String[] config(String configs[]) throws IOException {
        System.out.println("[debug] sleep before send");
        try { Thread.sleep(1000); } catch (InterruptedException e) {} // just ignore it if it gets interrupted
        byte b[] = this.send_and_recv("+++");
        System.out.println( "[debug] got: " + new String(b) );

        String responses[] = new String[ configs.length ];

        // TODO: do configs here

        return responses;
    }
}
