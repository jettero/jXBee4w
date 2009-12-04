import java.io.*;
import java.util.*;

public class PacketQueueWriter implements Runnable {
    private static final int MAX_QUEUE_DEPTH = 5;
    private static boolean debug = false;

    private XBeeHandle xh;
    private Queue <Queue <XBeeTxPacket>> outboundQueue; // not synched, so the append and pop functions must be
    private ACKQueue currentDatagram; // this is object synched so the ACK and send functions do not need to be
    private boolean closed = false;

    private String name;

    public void close() {
        if( debug )
            System.out.printf("[debug] PacketWriter(%s) - close()%n", name);

        closed = true;
    }

    static {
        debug = TestENV.test("DEBUG") || TestENV.test("PQW_DEBUG");
    }

    public synchronized void append(Queue <XBeeTxPacket> q) {
        if( debug )
            System.out.printf("[debug] PacketWriter(%s) - append()%n", name);

        // block while we've already got enough to do
        while(outboundQueue.size() >= MAX_QUEUE_DEPTH)
            try { Thread.sleep(150); }
            catch(InterruptedException e) {/* we go around again either way */}

        outboundQueue.add(q);
    }

    public PacketQueueWriter(XBeeHandle _xh, String _name) {
        xh = _xh;
        name = _name;
        outboundQueue = new ArrayDeque< Queue <XBeeTxPacket> >();
    }

    public void receiveNACK(int frameID) {
        if( currentDatagram == null )
            return;

        if( debug )
            System.out.printf("[debug] PacketWriter(%s) - receiveNACK(%d)%n", name, frameID);

        currentDatagram.NACK(frameID);
    }

    public void receiveACK(int frameID) {
        if( currentDatagram == null )
            return;

        if( debug )
            System.out.printf("[debug] PacketWriter(%s) - receiveACK(%d)%n", name, frameID);

        currentDatagram.ACK(frameID);
    }

    private void sendCurrentDatagram() {
        boolean resetNACKCount = true;
        XBeePacket datagram[] = currentDatagram.packets(resetNACKCount);

        for( int i=0; i<datagram.length; i++ ) {
            if( closed )
                return;

            try {
                if( debug )
                    System.out.printf("[debug] PacketWriter(%s) - sendCurrentDatagram(%d)%n", name,
                        ((XBeeTxPacket)datagram[i]).frameID());

                xh.send_packet(datagram[i]); // this method is synchronized, no worries on timing
            }

            catch(IOException e) {
                // Ucky, try again in a couple seconds
                try { Thread.sleep(2 * 1000); }
                catch(InterruptedException f) {/* we go around again either way */}
            }
        }
    }

    private void dealWithDatagram() {
        if( closed || currentDatagram == null )
            return;

        boolean firstLoop = true;

        while( !closed && currentDatagram.size() > 0 ) {
            if( debug )
                System.out.printf("[debug] PacketWriter(%s) - dealWithCurrentDatagram()%n", name);

            if( firstLoop || (currentDatagram.NACKCount() >= currentDatagram.size()) ) {
                sendCurrentDatagram();
                firstLoop = false;
            }

            try { Thread.sleep(150); } catch(InterruptedException e) {/* we go around again either way */}
        }

        clearCurrentDatagram();
    }

    private synchronized void clearCurrentDatagram() {
        if( debug )
            System.out.printf("[debug] PacketWriter(%s) - clearCurrentDatagram()%n", name);

        currentDatagram = null;
    }

    private synchronized void lookForDatagram() {
        if( debug )
            System.out.printf("[debug] PacketWriter(%s) - lookForDatagram()%n", name);

        Queue <XBeeTxPacket> tmp = outboundQueue.poll();

        if( tmp != null )
            currentDatagram = new ACKQueue(tmp);
    }

    public boolean allClear() {
        if( currentDatagram != null )
            return false;

        if( outboundQueue.size() > 0 )
            return false;

        return true;
    }

    public void run() {
        while(!closed) {
            if( debug )
                System.out.printf("[debug] PacketWriter(%s) - run()%n", name);

            lookForDatagram();
            dealWithDatagram();

            try { Thread.sleep(150); } catch(InterruptedException e) {/* we go around again either way */}
        }
    }

}
