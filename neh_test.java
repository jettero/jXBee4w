
public class neh_test {
    public static void main(String args[]) {
        String debug = System.getenv("DEBUG");

        if( debug != null )
            if( !debug.isEmpty() )
                if( !debug.equals("0") )
                    NetworkEndpointHandle.debug = true;

        NetworkEndpointHandle lhs = NetworkEndpointHandle.configuredEndpoint("LHS", true);
        //NetworkEndpointHandle rhs = NetworkEndpointHandle.configuredEndpoint("RHS", true);

        lhs.close();
        //rhs.close();
    }
}
