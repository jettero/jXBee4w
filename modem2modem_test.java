import java.io.*;

public class modem2modem_test {
    public static void main(String args[]) {
        String debug = System.getenv("DEBUG");

        if( debug != null )
            if( !debug.isEmpty() )
                if( !debug.equals("0") )
                    NetworkEndpointHandle.debug = true;

        NetworkEndpointHandle lhs = NetworkEndpointHandle.configuredEndpoint("LHS", true);
        NetworkEndpointHandle rhs = NetworkEndpointHandle.configuredEndpoint("RHS", true);

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
}
