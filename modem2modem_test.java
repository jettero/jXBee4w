import java.io.*;

public class modem2modem_test implements MessageRecvEvent, RawRecvEvent {
    XBeeDispatcher lhs, rhs;
    public static final int MAX_WAITS = 2;
    public static final int WAIT_LEN  = 750;
    int idle_retries = MAX_WAITS;

    public void recvPacket(XBeeDispatcher handle, XBeeRxPacket rx) {
        System.out.printf("%s Rx %d byte(s) at %d dBm.%n", handle.getName(), rx.payloadLength(), rx.RSSI());
        idle_retries = MAX_WAITS;
    }

    public void recvMessage(XBeeDispatcher handle, Address64 src, byte message[]) {
        System.out.printf("%s Received message from %s, \"%s\" [ %s ]%n",
            handle.getName(), src.toText(), new String(message),

             ( // This is a fairly weak test of the message consistencey, but there's also the visual check
               // looks good.

               (     message[message.length-1] == '-' && message[0] == '-'
                  && message[message.length-2] == '=' && message[1] == '=' )

               ||

               (     message[message.length-1] == '>' && message[0] == '<'
                  && message[message.length-2] == '>' && message[1] == '<' )

             )

             ? "OK" : "  "

            );

       if( handle == rhs )
           rhs.send( lhs.addr(), String.format("<<< reply test: %s >>>", new String(message)) );
    }

    public static int numberize(String var) {
        String res = System.getenv(var);
        if( res == null )
            return 0;
        return (new Integer(res)).intValue();
    }

    public boolean booleanize(String var) {
        int i = numberize(var);

        if( i != 0 ) return true;
        return false;
    }

    public void run() {
        boolean announce = true;

        lhs = XBeeDispatcher.configuredDispatcher("LHS", announce);
        rhs = XBeeDispatcher.configuredDispatcher("RHS", announce);

        // tell the LHS, that received messages should go to this object:
        lhs.registerMessageReceiver(this);
        lhs.registerRawReceiver(this);

        // tell the RHS, that received messages should go to this object:
        rhs.registerMessageReceiver(this);
        rhs.registerRawReceiver(this);

        String extra = System.getenv("EXTRA_MSG");
        if( extra == null ) extra = "";

        System.out.println("\nsending messages...\n");

        Address64 target = rhs.addr();
        if( booleanize("FLUB_TARGET_ADDR") )
            target.addr[3] ++;

        for(int i=0; i<numberize("MESSAGES_TO_SEND"); i++)
            lhs.send( target, String.format("-=: This is a test message: test #%d.%s :=-", i, extra) );

        System.out.println("\nwaiting for everything to finish up...\n\n");

        while( idle_retries --> 0 )
            try { Thread.sleep(WAIT_LEN); } catch (InterruptedException e) {}

        lhs.close();
        rhs.close();
    }

    public static void main(String args[]) {
        modem2modem_test m = new modem2modem_test();

        m.run();
    }
}
