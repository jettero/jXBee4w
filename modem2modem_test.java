
public class modem2modem_test {
    public static void main(String args[]) {
        String debug = System.getenv("DEBUG");

        if( debug != null )
            if( !debug.isEmpty() )
                if( !debug.equals("0") )
                    NetworkEndpointHandle.debug = true;

        NetworkEndpointHandle lhs = NetworkEndpointHandle.configuredEndpoint();
            System.out.println("LHS Address: " + lhs.addr().toText());
            System.out.println(" HV version: " + lhs.hardwareVersion());
            System.out.println(" VR version: " + lhs.firmwareVersion());

        NetworkEndpointHandle rhs = NetworkEndpointHandle.configuredEndpoint();
            System.out.println("RHS Address: " + rhs.addr().toText());
            System.out.println(" HV version: " + rhs.hardwareVersion());
            System.out.println(" VR version: " + rhs.firmwareVersion());

        lhs.send( rhs.addr(), "wassup?!?" );

        lhs.close();
        rhs.close();
    }
}
