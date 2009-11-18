import gnu.io.*;
import java.io.*;
import java.util.*;

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

        List<Integer>l = new ArrayList<Integer>();
        Integer aByte;
        while( (aByte = this.in.read()) > -1 )
            l.add( aByte );

        // NOTE: this is "hugely inefficient," but it does work.  ByteBuffer
        // was a suggestion, but lacks the functionality I desired. The lackof
        // dynamically expanding arrays in java is a sad limitation.

        byte b[] = new byte[ l.size() ];
        for(int i=0; i<l.size(); i++)
            b[i] = l.get(i).byteValue();

        return b;
    }

    public String[] config(String configs[]) throws IOException {
        System.out.println("[debug] sleep before send");
        try { Thread.sleep(1000); } catch (InterruptedException e) {} // just ignore it if it gets interrupted
        byte b[] = this.send_and_recv("+++");
        System.out.println( "[debug] got: " + new String(b) );

        ArrayList<String>s = new ArrayList<String>();

        // TODO: do configs here

        return s.toArray(new String[ s.size() ]);
    }
}
