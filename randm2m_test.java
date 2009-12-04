import java.io.*;

public class randm2m_test implements MessageRecvEvent, RawRecvEvent {
    XBeeDispatcher x[] = new XBeeDispatcher[4];

    public static final int MAX_WAITS = 2;
    public static final int WAIT_LEN  = 750;
    int idle_retries = MAX_WAITS;

    public void recvPacket(XBeeDispatcher handle, XBeeRxPacket rx) {
        System.out.printf("%s Rx %d byte(s) at %d dBm.%n", handle.getName(), rx.payloadLength(), rx.RSSI());
        idle_retries = MAX_WAITS;
    }

    public void recvMessage(XBeeDispatcher handle, Address64 src, byte message[]) {
        String s = new String(message);

        System.out.printf("%s Rx: \"%s\"%n", handle.getName(), src.toText(), s);

        if( message[message.length-1] != 'R' )
            handle.send( src, String.format("%s R", s) );
    }

    // public static int numberize(String var) {{{
    public static int numberize(String var) {
        String res = System.getenv(var);

        if( res == null )
            return 0;

        return (new Integer(res)).intValue();
    }
    // }}}
    // public boolean booleanize(String var) {{{
    public boolean booleanize(String var) {
        int i = numberize(var);

        if( i != 0 ) return true;
        return false;
    }
    // }}}

    public void run() {
        boolean announce = false;

        for(int i=0; i<4; i++) {
            x[i] = XBeeDispatcher.configuredDispatcher(String.format("X%d", i), announce);
            x[i].registerMessageReceiver(this);
            x[i].registerRawReceiver(this);
            x[i].setChannel(0xd);
        }

        // ok, this isn't very random ... meh.

        int test = 0;

        for( XBeeDispatcher lhs : x ) {
        for( XBeeDispatcher rhs : x ) {
        if( lhs != rhs ) {

            Address64 target = rhs.addr();

            lhs.send( target, String.format("test-%d", test++) );

        }}}

        // XXX: testing bug... the close below doesn't seem to actually close
        // presumably because the sending queues aren't empty... but the close
        // should still work!

        // while( idle_retries --> 0 )
        //     try { Thread.sleep(WAIT_LEN); } catch (InterruptedException e) {}

        for(XBeeDispatcher _x : x)
            _x.close();
    }

    public static void main(String args[]) {
        randm2m_test m = new randm2m_test();

        m.run();
    }
}
