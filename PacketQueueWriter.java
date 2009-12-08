import java.io.*;
import java.util.*;

public class PacketQueueWriter implements Runnable {
    private static final int MAX_QUEUE_DEPTH = 5;
    private static final int POLLING_WAIT    = 150;
    private static final int POLLING_WAITS   = 30; // number of sleep(POLLING_WAIT)s before we give up and resend
                                                   // the XBEE keeps track of ACKs internally, so this should be really high
                                                   // -- by embedded timing standards, 30*150 is practically eternity

    private boolean closed = false;
    private XBeeHandle xh;
    private XTPQoQ outboundQueue = new XTPQoQ(); // synched, so the append and pop functions do not need to be
    private ACKQueue currentDatagram; // this is object synched so the ACK and send functions do not need to be
                                      // although lookForDatagram() and clearCurrentDatagram() do have to be synched,
                                      // see code below
    private String name;

    private static boolean debug = false;
    static { debug = TestENV.test("DEBUG") || TestENV.test("PQW_DEBUG"); }

    private static class XTPQoQ {
        private Queue <Queue <XBeeTxPacket>> Q = new ArrayDeque< Queue <XBeeTxPacket> >();

        public int size() { return Q.size(); }
        public synchronized void add(Queue <XBeeTxPacket> toAdd) { Q.add(toAdd); }
        public synchronized Queue <XBeeTxPacket> poll() { return Q.poll(); }
    }

    public String getName() { return name; }

    public void close() {
        if( debug )
            System.out.printf("[debug] PacketQueueWriter(%s) - close()%n", name);

        closed = true;
    }

    public void append(Queue <XBeeTxPacket> q) {
        if( debug )
            System.out.printf("[debug] PacketQueueWriter(%s) - append(IDs=[%s]) [%s]%n",
                name, (new ACKQueue(q)).IDsAsString(), outboundQueue.size() >= MAX_QUEUE_DEPTH ? "waiting" : "nowait");

        // block while we've already got enough to do
        while(outboundQueue.size() >= MAX_QUEUE_DEPTH) {
            try { Thread.sleep(POLLING_WAIT); }
            catch(InterruptedException e) {/* we go around again either way */}

            if( closed ) {
                if( debug )
                    System.out.printf("[debug] PacketQueueWriter(%s) - append(IDs=[%s]) [closed, aborting]%n",
                        name, (new ACKQueue(q)).IDsAsString());
                return;
            }
        }

        if( debug )
            System.out.printf("[debug] PacketQueueWriter(%s) - append(IDs=[%s]) [adding to Queue]%n",
                name, (new ACKQueue(q)).IDsAsString());

        outboundQueue.add(q);
    }

    public PacketQueueWriter(XBeeHandle _xh, String _name) {
        xh = _xh;
        name = _name;
    }

    public void receiveNACK(int frameID) {
        if( currentDatagram == null )
            return;

        boolean res = currentDatagram.NACK(frameID);

        if( debug )
            System.out.printf("[debug] PacketQueueWriter(%s) - receiveNACK(%d) [%s]%n", name, frameID, res ? "!" : " ");
    }

    public void receiveACK(int frameID) {
        if( currentDatagram == null )
            return;

        boolean res = currentDatagram.ACK(frameID);

        if( debug )
            System.out.printf("[debug] PacketQueueWriter(%s) - receiveACK(%d) [%s]%n", name, frameID, res ? "!" : " ");
    }

    private void sendCurrentDatagram() {
        boolean resetNACKCount = true;
        XBeePacket datagram[] = currentDatagram.packets(resetNACKCount);

        for( int i=0; i<datagram.length; i++ ) {
            if( closed )
                return;

            try {
                if( debug )
                    System.out.printf("[debug] PacketQueueWriter(%s) - sendCurrentDatagram(%d)%n", name,
                        ((XBeeTxPacket)datagram[i]).frameID());

                xh.send_packet(datagram[i]); // this method is synched, no worries on timing
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

        int waits = Integer.MAX_VALUE;

        while( !closed && currentDatagram.size() > 0 ) {
            if( debug )
                System.out.printf("[debug] PacketQueueWriter(%s) - dealWithCurrentDatagram(nack=%d, size=%d, [%s])%n",
                    name, currentDatagram.NACKCount(), currentDatagram.size(), currentDatagram.IDsAsString());

            if( (waits >= POLLING_WAITS) || (currentDatagram.NACKCount() >= currentDatagram.size()) ) {
                if( debug ) {
                    if( waits >= POLLING_WAITS && waits < Integer.MAX_VALUE )  {
                        System.out.printf("[debug] PacketQueueWriter(%s) - resending current datagram -- this should be fairly rare%n", name);
                        currentDatagram.dumpPackets(name + "-resending-%d-%x.pkt");
                    }
                }

                sendCurrentDatagram();
                waits = 0;
            }

            try { Thread.sleep(POLLING_WAIT); } catch(InterruptedException e) {/* we go around again either way */}
            waits ++;
        }

        clearCurrentDatagram();
    }

    private synchronized void clearCurrentDatagram() {
        if( debug )
            System.out.printf("[debug] PacketQueueWriter(%s) - clearCurrentDatagram()%n", name);

        currentDatagram = null;
    }

    private synchronized void lookForDatagram() {
        Queue <XBeeTxPacket> tmp = outboundQueue.poll();

        if( tmp != null )
            currentDatagram = new ACKQueue(tmp);

        if( debug )
            System.out.printf("[debug] PacketQueueWriter(%s) - lookForDatagram(qsize=%d, IDs=[%s])%n",
                 name, outboundQueue.size(), tmp == null ? "n/a" : currentDatagram.IDsAsString());
    }

    public boolean allClear() {
        if( currentDatagram != null )
            return false;

        if( outboundQueue.size() > 0 )
            return false;

        if( debug )
            System.out.printf("[debug] PacketQueueWriter(%s) - allClear()%n", name);

        return true;
    }

    public void run() {
        while(!closed) {
            if( debug )
                System.out.printf("[debug] PacketQueueWriter(%s) - run()%n", name);

            lookForDatagram();
            dealWithDatagram();

            try { Thread.sleep(POLLING_WAIT); } catch(InterruptedException e) {/* we go around again either way */}
        }
    }

}
