import gnu.io.*;
import java.io.*;
import java.util.*;
import java.nio.*;

public class XBeeHandle {
    boolean debug;

    private InputStream  in;
    private OutputStream out;
    private CommPort commPort;
    private PacketReader packetReader;
    private Thread _prThread;

    protected void finalize() throws Throwable { this.close(); }
    public void close() {
        packetReader.close();
        commPort.close();
    }

    XBeeHandle(CommPortIdentifier portIdentifier, int speed, boolean _debug, PacketRecvEvent callback) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        debug = _debug;
        commPort = portIdentifier.open(this.getClass().getName(), 2000);

        if ( commPort instanceof SerialPort ) {
            SerialPort serialPort = (SerialPort) commPort;
            serialPort.setSerialPortParams(speed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

            in  = serialPort.getInputStream();
            out = serialPort.getOutputStream();

            packetReader = new PacketReader(in, callback);
            _prThread    = new Thread(packetReader);
            _prThread.start();

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
        private ByteBuffer b;
        private boolean inPkt;

        public PacketReader (InputStream _in, PacketRecvEvent callback) {
            in = _in;
            ev = callback;
            b = ByteBuffer.wrap(new byte[1024]);
            inPkt = false;
        }

        public synchronized void close() { // synchronized so in=null doesn't sneak up on the packet reader
            System.out.println("[debug] packetReader closing");
            in = null; // this is synchronized (see below)
        }

        private void inPkt(int aByte) {
            try { b.put( (byte) aByte ); }
            catch(BufferOverflowException e) { /* huh... just ignore this for now */ }

            // TODO: do something once we have enough for a packet
            // probably build a packet and notify

            if( XBeePacket.enoughForPacket(b) ) {
                byte[] pktbytes = new byte[ b.position() ];

                b.clear();
                b.get(pktbytes);

                XBeePacket p = new XBeePacket(pktbytes);
                           p.fileDump("wtf-%d.pkt");

                if( p.checkPacket() ) {
                    System.out.println("[debug] packetReader completed XBeePacket, sending to ev");
                    ev.recvPacket(p);
                }

                // else
                    // log this or something ... we got a packet, but apparently it's bad. :(

                inPkt = false;
            }
        }

        private void seekDelimiter(int aByte) {
            if( aByte == 0x7e ) {
                System.out.println("[debug] packetReader found a frame delimiter, starting packet");

                try {
                    b.clear();
                    b.put( (byte) aByte );
                    inPkt = true;
                }

                catch(BufferOverflowException e) {
                    System.err.println("internal error storing packet delimiter");
                }
            }
        }

        private synchronized void _step() { // synchronized so in=null doesn't sneak up on us
            int aByte;

            if( in == null )
                return;

            System.out.println("[debug] packetReader looking for packets");
            try {

                if( in.available() >= 1 ) {
                    while( (aByte = in.read()) > -1 ) {
                        System.out.printf("got aByte=%02x%n", aByte);

                        if( inPkt )
                            inPkt(aByte);

                        else
                            seekDelimiter(aByte);
                    }
                }
            }

            catch(IOException e) {
                inPkt = false;
                // pfft.  Just start over.
            }

        }

        public void run() { // not synchronized so the (this).lock is cleared every _step()
            while(in != null) {
                _step();

                try { Thread.sleep(250); }
                catch(InterruptedException e) { /* don't really care if it doesn't work... maybe a warning should go here */ }
            }
        }
    }

    public void send_packet(XBeePacket p) throws IOException {
        System.out.println("[debug] XBeeHandle sending packet");
        out.write(p.getBytes());
    }
}
