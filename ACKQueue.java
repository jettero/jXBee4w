import java.util.*;

public class ACKQueue {
    private TreeMap <Integer, XBeeTxPacket> Q = new TreeMap<Integer, XBeeTxPacket>();
    private HashSet <Integer> N = new HashSet<Integer>();

    ACKQueue(Queue <XBeeTxPacket> packets) {
        XBeeTxPacket p;

        while( (p = (XBeeTxPacket) packets.poll()) != null )

            Q.put( new Integer(p.frameID()), p );
    }

    public int size() {
        return Q.size();
    }

    public int NACKCount() {
        return N.size();
    }

    public XBeeTxPacket[] packets() { return packets(true); }

    public synchronized XBeeTxPacket[] packets(boolean resetNackCount) {
        if( resetNackCount )
            N.clear();

        return Q.values().toArray(new XBeeTxPacket[Q.size()]);
    }

    public synchronized void ACK(int frameID) {
        Integer F = new Integer(frameID);

        Q.remove(F);
        N.remove(F);
    }

    public synchronized void NACK(int frameID) {
        Integer F = new Integer(frameID);

        if( Q.containsKey(F) )
            N.add(new Integer(F));
    }
}
