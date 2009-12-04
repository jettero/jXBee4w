import java.util.*;

public class PacketQueueWriterDestinationMap {
    private HashMap<Address64, PacketQueueWriter> hm;
    String name;
    XBeeHandle xh;

    private static boolean debug = false;
    static { debug = TestENV.test("DEBUG") || TestENV.test("PQW_DEBUG"); }

    PacketQueueWriterDestinationMap(String _n, XBeeHandle _xh) {
        hm = new HashMap<Address64,PacketQueueWriter>();
        xh = _xh;
        name = _n;
    }

    private PacketQueueWriter[] allPQW() {
        return hm.values().toArray(new PacketQueueWriter[hm.size()]);
    }

    private Address64[] allAddresses() {
        return hm.keySet().toArray(new Address64[hm.size()]);
    }

    public synchronized void closeAll() {
        for( PacketQueueWriter pw : allPQW() )
            pw.close();

        hm.clear();
    }

    public void receiveNACK(int frameID) {
        for( PacketQueueWriter pw : allPQW() )
            pw.receiveACK(frameID);
    }

    public void receiveACK(int frameID) {
        for( PacketQueueWriter pw : allPQW() )
            pw.receiveACK(frameID);
    }

    private synchronized void checkForStaleEntries(Address64 skip) {
        // NOTE: we have to go through the hash every now and again even if the
        // pqws exit gracefully on their own.  If they exit cleanly, we pretty
        // much still have go through and ask if they exited ... or else have
        // them callback home to clean up the hash or something.

        // This is probably fine for now.  If the hash gets too big the
        // frameIDs are going to roll-over and collide anyway.

        for( Address64 a : allAddresses() ) {
            if( !a.equals(skip) ) {
                PacketQueueWriter pw = hm.get(a);

                if( pw.allClear() ) {
                    pw.close();
                    hm.remove(a); // NOTE: this class was created while hunting a bug here, remove(skip) rather than remove(a)
                }
            }
        }
    }

    public synchronized PacketQueueWriter get(Address64 a) {
        checkForStaleEntries(a);

        if( hm.containsKey(a) )
            return hm.get(a);

        // otherwise, start new pqw for this address

        String tn = String.format("%s -> %s", name, a.toText());
        PacketQueueWriter pw = new PacketQueueWriter(xh, tn);

        hm.put(a, pw);

        (new Thread(pw)).start();

        return pw;
    }
}
