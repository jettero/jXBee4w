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

    public void dumpPackets(String s) {
        for( XBeeTxPacket p : packets(false) )
            p.fileDump(s);
    }

    public XBeeTxPacket[] packets() { return packets(true); }

    public synchronized XBeeTxPacket[] packets(boolean resetNackCount) {
        if( resetNackCount )
            N.clear();

        return Q.values().toArray(new XBeeTxPacket[Q.size()]);
    }

    public synchronized String IDsAsString() {
        XBeeTxPacket d[] = packets(false);
        String IDs = "" + d[0].frameID();

        for(int i=1; i<d.length; i++)
            IDs += ", " + d[i].frameID();

        return IDs;
    }

    public synchronized boolean ACK(int frameID) {
        Integer F = new Integer(frameID);

        N.remove(F);
        return Q.remove(F) == null ? false : true;
    }

    public synchronized boolean NACK(int frameID) {
        Integer F = new Integer(frameID);

        if( Q.containsKey(F) ) {
            N.add(new Integer(F));
            return true;
        }

        return false;
    }
}
