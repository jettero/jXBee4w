import gnu.io.*;
import java.io.*;
import java.util.*;
import java.nio.*;

public class XBeeHandle {
    boolean debug;

    private InputStream  in;
    private OutputStream out;
    private CommPort commPort;

    protected void finalize() throws Throwable { this.close(); }
    public    void close() { commPort.close(); }

    XBeeHandle(CommPortIdentifier portIdentifier, int speed, boolean _debug, PacketEvent callback) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        debug = _debug;
        commPort = portIdentifier.open(this.getClass().getName(), 2000);

        if ( commPort instanceof SerialPort ) {
            SerialPort serialPort = (SerialPort) commPort;
            serialPort.setSerialPortParams(speed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

            in  = serialPort.getInputStream();
            out = serialPort.getOutputStream();

            (new Thread(new PacketReader(in, callback)).start();

        } else {
            throw new UnsupportedCommOperationException("\"" + portIdentifier.getName() + "\" is probably not a serial port");
        }
    }

    public static XBeeHandle newFromPortName(String portName, int speed, boolean debug) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);

        return new XBeeHandle(portIdentifier, speed, debug);
    }

    private static class PacketReader implements Runnable {
        private PacketEvent ev;
        private InputStream in;
        
        public PacketReader (InputStream in, PacketEvent callback) {
            this.in = in;
            this.ev = callback;
        }
        
        public void run() {
            ByteBuffer b = ByteBuffer.wrap(new byte[1024]);
            boolean inPkt = false;

            while(true) {
                while( (aByte = this.in.read()) > -1 ) {
                    if( inPkt ) {
                        try { b.put( (byte) aByte ); }
                        catch(BufferOverflowException e) { /* huh... just ignore this see if we get any more */ }

                        // TODO: do something once we have enough for a packet
                        // probably build a packet and notify

                        if( 

                    } else {
                    }
                }
            }
        }
    }


    public byte[] send_packet(XBeePacket p) throws IOException {
        /// send a packet... this is pretty straight forward
    }
}
