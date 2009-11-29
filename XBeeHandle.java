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
    private String handleName;

    static int bad_packet_no; // for debugging

    protected void finalize() throws Throwable { this.close(); }
    public void close() {
        packetReader.close();
        commPort.close();
    }

    XBeeHandle(CommPortIdentifier p, int s, boolean d, PacketRecvEvent c) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this("<blarg>", p, s, d, c);
    }

    XBeeHandle(String name, CommPortIdentifier portIdentifier, int speed, boolean _debug, PacketRecvEvent callback) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        debug = _debug;
        commPort = portIdentifier.open(name, 50);
        handleName = name;

        if ( commPort instanceof SerialPort ) {
            SerialPort serialPort = (SerialPort) commPort;
            serialPort.setSerialPortParams(speed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

            in  = serialPort.getInputStream();
            out = serialPort.getOutputStream();

            packetReader = new PacketReader(in, callback);
            packetReader.setDebug(debug);

            _prThread = new Thread(packetReader);
            _prThread.start();

        } else {
            throw new UnsupportedCommOperationException("\"" + portIdentifier.getName() + "\" is probably not a serial port");
        }
    }

    public static XBeeHandle newFromPortName(String portName, int speed, boolean debug, PacketRecvEvent callback) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        return XBeeHandle.newFromPortName("<blarg>", portName, speed, debug, callback);
    }

    public static XBeeHandle newFromPortName(String handleName, String portName, int speed, boolean debug, PacketRecvEvent callback) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);

        return new XBeeHandle(handleName, portIdentifier, speed, debug, callback);
    }

    private static class PacketReader implements Runnable {
        private PacketRecvEvent ev;
        private InputStream in;
        private ByteBuffer b;
        private boolean inPkt;
        private boolean debug;

        public void setDebug(boolean d) { debug = d; }

        public PacketReader (InputStream _in, PacketRecvEvent callback) {
            in = _in;
            ev = callback;
            b = ByteBuffer.wrap(new byte[1024]);
            inPkt = false;
        }

        public synchronized void close() { // synchronized so in=null doesn't sneak up on the packet reader
            if( debug )
                System.out.println("[debug] packetReader closing");

            in = null; // this is synchronized (see below)
        }

        private boolean inPkt(int aByte) {
            try { b.put( (byte) aByte ); }
            catch(BufferOverflowException e) { /* huh... just ignore this for now */ }

            // TODO: do something once we have enough for a packet
            // probably build a packet and notify

            if( XBeePacket.enoughForPacket(b) ) {
                byte[] pktbytes = new byte[ b.position() ];

                b.clear();
                b.get(pktbytes);

                XBeePacket p = new XBeePacket(pktbytes);

                if( p.checkPacket() ) {
                    if( debug )
                        System.out.println("[debug] packetReader completed a XBeePacket, sending to ev");

                    ev.recvPacket(p.adapt());

                } else {
                    if( debug )
                        p.fileDump( String.format("bad-packet-%d", bad_packet_no ++) + "-%d");

                    System.err.println("warning: found a packet, but it didn't pass basic checks, tossing");
                }

                // else
                    // log this or something ... we got a packet, but apparently it's bad. :(

                inPkt = false;
                return true; // return true when we find a packet
            }

            return false; // return false until we find a packet
        }

        private void seekDelimiter(int aByte) {
            if( aByte == 0x7e ) {
                if( debug )
                    System.out.println("[debug] packetReader found a frame delimiter, starting packet");

                try {
                    b.clear();
                    b.put( (byte) aByte );
                    inPkt = true;
                }

                catch(BufferOverflowException e) {
                    System.err.println("internal error storing packet delimiter");
                }

            } else {
                System.err.printf("warning: ignoring byte %02x while seeking start of packet%n", aByte);
            }
        }

        private synchronized void _step() { // synchronized so in=null doesn't sneak up on us
            int aByte;

            if( in == null )
                return;

            if( debug )
                System.out.println("[debug] packetReader looking for packets for a step (sync)");

            try {

                if( in.available() >= 1 ) {
                    while( (aByte = in.read()) > -1 ) {
                        if( inPkt ) {
                            if( inPkt(aByte) )
                                break;

                        } else {
                            seekDelimiter(aByte);
                        }
                    }
                }
            }

            catch(IOException e) {
                inPkt = false;
                // pfft.  Just start over.
            }

            if( debug )
                System.out.println("[debug] packetReader finished looking for packets, this step (desync)");
        }

        public void run() { // not synchronized so the (this).lock is cleared every _step()
            while(in != null) {
                _step();

                try { Thread.sleep(150); }
                catch(InterruptedException e) { /* don't really care if it doesn't work... maybe a warning should go here */ }
            }
        }
    }

    public synchronized void send_packet(XBeePacket p) throws IOException {
        if( debug )
            System.out.println("[debug] XBeeHandle sending packet");

        String _dump = System.getenv("DUMP_OUTGOING_PACKETS");
        if( _dump != null )
            if( !_dump.isEmpty() )
                if( !_dump.equals("0") )
                    p.fileDump(handleName + "-send-%d.pkt");

        byte b[] = p.getBytes();
        out.write(b);
        out.flush(); // make sure we send this right away, which I think is automatic with serial, but who knows

        // make sure we don't get too far ahead of the serial port
        int txWait = (int) Math.ceil((b.length * 8) * (1/115.2));
        if( txWait < 100 )
            txWait = 100; // wait at least this long

        try { Thread.sleep(txWait); }
        catch(InterruptedException e) { /* don't really care if it doesn't work... maybe a warning should go here */ }
    }
}
