import java.io.*;

public class modem2modem_test implements MessageRecvEvent {
    public void recvMessage(Address64 src, byte message[]) {
        System.out.printf(" ***** Received message from %s, \"%s\"%n", src.toText(), new String(message));
    }

    public void run() {
        boolean announce = true;

        NetworkEndpointHandle lhs = NetworkEndpointHandle.configuredEndpoint("LHS", announce);
        NetworkEndpointHandle rhs = NetworkEndpointHandle.configuredEndpoint("RHS", announce);

        // tell the RHS, that received messages should go to this object:

        rhs.registerMessageReceiver(this);

        try {
            System.out.println("sending message");
            lhs.send( rhs.addr(), "wassup?!?" );
        }

        catch(IOException e) {
            System.err.println("ERROR sending message: " + e.getMessage());
        }

        System.out.println("wating around for 10 seconds");
        try { Thread.sleep(10 * 1000); } catch (InterruptedException e) {}

        lhs.close();
        rhs.close();
    }

    public static void main(String args[]) {
        String debug = System.getenv("DEBUG");

        if( debug != null )
            if( !debug.isEmpty() )
                if( !debug.equals("0") )
                    NetworkEndpointHandle.debug = true;

        modem2modem_test m = new modem2modem_test();

        m.run();
    }
}
