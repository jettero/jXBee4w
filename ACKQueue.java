import java.util.*;

public class ACKQueue {
    private TreeMap <Integer, XBeePacket> Q = new TreeMap<Integer, XBeePacket>();

    ACKQueue(Queue packets) {
        XBeePacket p;

        while( (p = (XBeePacket) packets.poll()) != null )

            Q.put( new Integer(p.frameID()), p );
    }

    public int size() {
        return Q.size();
    }

    public XBeePacket[] packets() {
        return (XBeePacket[]) Q.values().toArray();
    }

    public synchronized void ACK(int frameID) {
        Q.remove(new Integer(frameID));
    }
}
