import java.util.*;

public class ACKQueue {
    private TreeMap <Integer, XBeeTxPacket> Q = new TreeMap<Integer, XBeeTxPacket>();

    ACKQueue(Queue <XBeeTxPacket> packets) {
        XBeeTxPacket p;

        while( (p = (XBeeTxPacket) packets.poll()) != null )

            Q.put( new Integer(p.frameID()), p );
    }

    public int size() {
        return Q.size();
    }

    public synchronized XBeeTxPacket[] packets() {
        return Q.values().toArray(new XBeeTxPacket[Q.size()]);
    }

    public synchronized void ACK(int frameID) {
        Q.remove(new Integer(frameID));
    }
}
