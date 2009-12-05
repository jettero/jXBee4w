import java.io.*;
import java.util.*;

public class randm2m_test implements MessageRecvEvent, RawRecvEvent {
    XBeeDispatcher x[];

    public static final int MAX_WAITS = 3;
    public static final int WAIT_LEN  = 750;
    int idle_retries = MAX_WAITS;

    private HashMap <String, Integer> theCounts = new HashMap <String,Integer>();

    private String frobnicate(Address64 src, Address64 dst, String msg) {
        return String.format("< %s, %s->%s >", msg, src.toText(), dst.toText());
    }

    private void _alt(String frob, int diff) {
        if( theCounts.containsKey(frob) )
            theCounts.put( frob, theCounts.get(frob) + diff );

        else
            theCounts.put( frob, new Integer(diff) );
    }

    private void inc(Address64 src, Address64 dst, String msg) { _alt(frobnicate(src,dst,msg),  1); }
    private void dec(Address64 src, Address64 dst, String msg) { _alt(frobnicate(src,dst,msg), -1); }

    public void recvPacket(XBeeDispatcher handle, XBeeRxPacket rx) {
        System.out.printf("%s Rx %d byte(s) at %d dBm.%n", handle.getName(), rx.payloadLength(), rx.RSSI());
        idle_retries = MAX_WAITS;
    }

    public void recvMessage(XBeeDispatcher handle, Address64 src, byte message[]) {
        Address64 dst = handle.addr();

        String s = new String(message);

        dec(src, dst, s);

        // System.out.printf("%s Rx: \"%s\" (from: %s)%n", handle.getName(), s, src.toText());

        if( message[message.length-1] != 'R' ) {
            s = String.format("%s R", s);
            inc(dst, src, s);
            handle.send( src, s );
        }
    }

    public void run() {
        boolean announce = false;

        int radios = TestENV.numberize("RADIOS");
        if( radios < 1 || radios > 4 )
            radios = 4;

        x = new XBeeDispatcher[radios];

        for(int i=0; i<radios; i++) {
            x[i] = XBeeDispatcher.configuredDispatcher(String.format("X%d", i), announce);
            x[i].registerMessageReceiver(this);
            x[i].registerRawReceiver(this);
            x[i].setChannel(0xd);
        }

        // ok, this isn't very random ... meh.

        int epochs = TestENV.numberize("EPOCHS");
        if( epochs < 1 )
            epochs = 1;

        for( int epoch=0; epoch<epochs; epoch++ ) { int test = 0;
        for( XBeeDispatcher lhs : x ) {
        for( XBeeDispatcher rhs : x ) {
        if( lhs != rhs ) {
            String s = String.format("test-%d.%d", epoch, test++);

            Address64 source = lhs.addr();
            Address64 target = rhs.addr();

            inc(source, target, s);

            lhs.send( target, s );

        }}}}

        // XXX: testing bug... the close below doesn't seem to actually close
        // presumably because the sending queues aren't empty... but the close
        // should still work!

        System.out.println("done sending ... waiting around");
        while( idle_retries --> 0 )
            try { Thread.sleep(WAIT_LEN); } catch (InterruptedException e) {}

        System.out.println("done waiting ... closing everything");
        for(XBeeDispatcher _x : x)
            _x.close();

        System.out.println("done closing ... checking the counts");

        int c;
        for( String k : theCounts.keySet().toArray(new String[0]) )
            if( (c = theCounts.get(k).intValue()) != 0 )
                System.out.printf("found a non-zero (%d) key in the double-check hashmap: %s%n", c, k);
    }

    public static void main(String args[]) {
        randm2m_test m = new randm2m_test();

        m.run();
    }
}
