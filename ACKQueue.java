
public class ACKQueue {
    private HashMap <Integer, XBeePacket> Q = new TreeMap<Integer, XBeePacket>();

    ACKQueue(Queue packets) {
        XBeePacket p;
        while( (p = packets.poll) != null )
            Q.put( new Integer(p.frameID()), p );
    }

    public int size() {
        return q.size();
    }

    public XBeePacket[] packets() {
        return (XBeePacket[]) Q.values().toArray();
    }

    public void ACK(int frameID) {
        Q.remove(new Integer(frameID));
    }
}
