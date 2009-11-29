import java.io.*;

public class modem2modem_test implements MessageRecvEvent {
    NetworkEndpointHandle lhs, rhs;

    public void recvMessage(NetworkEndpointHandle handle, Address64 src, byte message[]) {
        System.out.printf("%s Received message from %s, \"%s\"%n",
            handle.getName(), src.toText(), new String(message));
    }

    public void run() {
        boolean announce = true;

        lhs = NetworkEndpointHandle.configuredEndpoint("LHS", announce);
        rhs = NetworkEndpointHandle.configuredEndpoint("RHS", announce);

        // tell the RHS, that received messages should go to this object:

        rhs.registerMessageReceiver(this);

        try {
            System.out.println("sending messages");
            for(int i=0; i<10; i++)
                lhs.send( rhs.addr(), String.format("This is a test message: test #%d.", i) );
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
        modem2modem_test m = new modem2modem_test();

        m.run();
    }
}
