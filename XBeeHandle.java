// Really low level IO stuff here, feeds packets to the layer above

import gnu.io.*;
import java.io.*;
import java.util.*;
import java.nio.*;

public class XBeeHandle {
    private static final int POLLING_WAIT = 150;

    private static boolean debug                 = false;
    private static boolean dump_outgoing_packets = false;

    private InputStream  in;
    private OutputStream out;
    private PacketReader packetReader;
    private Thread _prThread;
    private String name;
    private boolean closed = true;

    private CommPort   commPort;   // these two objects are just two views of the same thing
    private SerialPort serialPort; // we close the comm port, but we setRTS and isCTS on the serial port

    private static int bad_packet_no; // for debugging
    private static int unknownHandleNo; // for debugging

    static {
        debug = TestENV.test("DEBUG") || TestENV.test("XH_DEBUG");
        dump_outgoing_packets = TestENV.test("DUMP_OUTGOING_PACKETS") || TestENV.test("DUMP_PACKETS");
    }

    protected void finalize() throws Throwable { this.close(); }
    public void close() {
        if( debug )
            System.out.printf("[debug] XBeeHandle(%s) closing child packetReader%n", name);

        packetReader.close();

        if( debug )
            System.out.printf("[debug] XBeeHandle(%s) closing commPort%n", name);

        closed = true;
        commPort.close();
    }

    XBeeHandle(CommPortIdentifier p, int s, PacketRecvEvent c) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        this(String.format("Handle-%d", ++unknownHandleNo), p, s, c);
    }

    XBeeHandle(String _name, CommPortIdentifier portIdentifier, int speed, PacketRecvEvent callback) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        commPort = portIdentifier.open(name, 50);
        name = _name;

        if ( commPort instanceof SerialPort ) {
            serialPort = (SerialPort) commPort;
            serialPort.setSerialPortParams(speed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

            in  = serialPort.getInputStream();
            out = serialPort.getOutputStream();

            packetReader = new PacketReader(name, in, serialPort, callback);

            _prThread = new Thread(packetReader);
            _prThread.start();

            closed = false;

        } else {
            throw new UnsupportedCommOperationException("\"" + portIdentifier.getName() + "\" is probably not a serial port");
        }
    }

    public static XBeeHandle newFromPortName(String portName, int speed, PacketRecvEvent callback) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        return XBeeHandle.newFromPortName(String.format("Handle-%d", ++unknownHandleNo), portName, speed, callback);
    }

    public static XBeeHandle newFromPortName(String _name, String portName, int speed, PacketRecvEvent callback) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);

        return new XBeeHandle(_name, portIdentifier, speed, callback);
    }

    private static class PacketReader implements Runnable {
        private PacketRecvEvent packetReceiver;
        private InputStream in;
        private SerialPort serialPort;
        private ByteBuffer b;
        private boolean inPkt;

        private String name;
        private static boolean debug                 = false;
        private static boolean dump_incoming_packets = false;
        private static boolean dump_bad_packets      = false;

        static {
            debug                 = TestENV.test("DEBUG") || TestENV.test("XH_DEBUG");
            dump_incoming_packets = TestENV.test("DUMP_INCOMING_PACKETS") || TestENV.test("DUMP_PACKETS");
            dump_bad_packets      = debug || TestENV.test("DUMP_BAD_PACKETS") || TestENV.test("DUMP_PACKETS");
        }

        public PacketReader(String _name, InputStream _in, SerialPort _p, PacketRecvEvent callback) {
            in = _in;
            packetReceiver = callback;
            b = ByteBuffer.wrap(new byte[1024]);
            inPkt = false;
            name = _name;
            serialPort = _p;
        }

        public synchronized void close() { // synchronized so in=null doesn't sneak up on the packet reader
            if( debug )
                System.out.printf("[debug] packetReader(%s) s closing (sync/desync)%n", name);

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
                        System.out.printf("[debug] packetReader(%s) completed a XBeePacket, sending to packetReceiver%n", name);

                    if( dump_incoming_packets )
                        p.fileDump(name + "-recv-%d-%x.pkt");

                    packetReceiver.recvPacket(p.adapt());

                    if( debug )
                        System.out.printf("[debug] packetReader(%s) completed, adapted and sent.  Nolonger in packet%n", name);

                } else {
                    if( dump_bad_packets )
                        p.fileDump(name + "-bad-%d-%x.pkt");

                    System.err.printf("Warning: packetReader(%s) found a packet, but it didn't pass basic checks, tossing%n", name);
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
                    System.out.printf("[debug] packetReader(%s) found a frame delimiter, starting packet%n", name);

                try {
                    b.clear();
                    b.put( (byte) aByte );
                    inPkt = true;
                }

                catch(BufferOverflowException e) {
                    System.err.printf("packetReader(%s) internal error: buffer error storing packet delimiter%n", name);
                }

            } else {
                System.out.printf("packetReader(%s) internal error: something is out of sync, ignoring byte %02x while seeking start of packet%n", name, aByte);
            }
        }

        private synchronized void _step() { // synchronized so in=null doesn't sneak up on us
            int aByte;

            if( in == null )
                return;

            if( debug )
                System.out.printf("[debug] packetReader(%s) looking for packets for a step (sync)%n", name);

            try {

                serialPort.setRTS(true); // tell the DO to send -- this is usually disabled

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

                serialPort.setRTS(false);
            }

            catch(IOException e) {
                inPkt = false;
                // pfft.  Just start over.
            }

            if( debug )
                System.out.printf("[debug] packetReader(%s) finished looking for packets, this step (desync)%n", name);
        }

        public void run() { // not synchronized so the (this).lock is cleared every _step()
            while(in != null) {
                _step();

                try { Thread.sleep(POLLING_WAIT); }
                catch(InterruptedException e) { /* don't really care if it doesn't work... maybe a warning should go here */ }
            }
        }
    }

    public synchronized void send_packet(XBeePacket p) throws IOException {
        if( debug )
            System.out.printf("[debug] XBeeHandle(%s) sending packet (sync)%n", name);

        if( closed ) {
            if( debug )
                System.out.printf("[debug] XBeeHandle(%s) asked to send packet while handle closed, discarding%n", name);

            return;
        }

        if( dump_outgoing_packets )
            p.fileDump(name + "-send-%d-%x.pkt");

        byte b[] = p.getBytes();

        for(int i=0; i<b.length; i++) {

            while(!serialPort.isCTS()) {
                System.out.printf("[debug] --------------------------- XBeeHandle(%s) waiting for cts -------------------------%n", name);
                try { Thread.sleep(POLLING_WAIT); }
                catch(InterruptedException e) { /* don't really care if it doesn't work... maybe a warning should go here */ }
            }

            out.write(b[i]);
        }

        if( debug )
            System.out.printf("[debug] XBeeHandle(%s) sent packet (desync)%n", name);
    }
}
