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

    XBeeHandle(CommPortIdentifier portIdentifier, int speed, boolean _debug, PacketRecvEvent callback) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        debug = _debug;
        commPort = portIdentifier.open(this.getClass().getName(), 2000);

        if ( commPort instanceof SerialPort ) {
            SerialPort serialPort = (SerialPort) commPort;
            serialPort.setSerialPortParams(speed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

            in  = serialPort.getInputStream();
            out = serialPort.getOutputStream();

            (new Thread(new PacketReader(in, callback))).start();

        } else {
            throw new UnsupportedCommOperationException("\"" + portIdentifier.getName() + "\" is probably not a serial port");
        }
    }

    public static XBeeHandle newFromPortName(String portName, int speed, boolean debug, PacketRecvEvent callback) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);

        return new XBeeHandle(portIdentifier, speed, debug, callback);
    }

    private static class PacketReader implements Runnable {
        private PacketRecvEvent ev;
        private InputStream in;
        
        public PacketReader (InputStream in, PacketRecvEvent callback) {
            this.in = in;
            this.ev = callback;
        }
        
        public void run() {
            ByteBuffer b = ByteBuffer.wrap(new byte[1024]);
            boolean inPkt = false;
            int aByte;

            while(true) {
                try {
                    while( (aByte = this.in.read()) > -1 ) {

                        if( inPkt ) {
                            try { b.put( (byte) aByte ); }
                            catch(BufferOverflowException e) { /* huh... just ignore this for now */ }

                            // TODO: do something once we have enough for a packet
                            // probably build a packet and notify

                            if( XBeePacket.enoughForPacket(b) ) {
                                byte[] pktbytes = new byte[ b.position() ];
                                XBeePacket p = new XBeePacket(pktbytes);

                                if( p.check_checksum() )
                                    ev.recvPacket(p);

                                // else
                                    // log this or something ... we got a packet, but apparently it's bad. :(

                                inPkt = false;
                            }

                        } else {
                            if( aByte == 0x7e ) {
                                // hey, a new frame (hopefully)
                                b.clear();
                                try { b.put( (byte) aByte ); }
                                catch(BufferOverflowException e) { /* ignore */ }
                                inPkt = true;
                            }
                        }

                    }
                } 

                catch(IOException e) {
                    inPkt = false;
                    // pfft.  Just start over.
                }
            }
        }
    }

    public void send_packet(XBeePacket p) throws IOException {
        /// send a packet... this is pretty straight forward
    }
}
