
public class neh_test {
    public static void main(String args[]) {
        String debug = System.getenv("DEBUG");

        if( debug != null )
            if( !debug.isEmpty() )
                if( !debug.equals("0") )
                    NetworkEndpointHandle.debug = true;

        NetworkEndpointHandle lhs = NetworkEndpointHandle.configuredEndpoint();
            System.out.println("LHS Address: " + h.addr().toText());
            System.out.println(" HV version: " + h.hardwareVersion());
            System.out.println(" VR version: " + h.firmwareVersion());

        NetworkEndpointHandle rhs = NetworkEndpointHandle.configuredEndpoint();
            System.out.println("RHS Address: " + h.addr().toText());
            System.out.println(" HV version: " + h.hardwareVersion());
            System.out.println(" VR version: " + h.firmwareVersion());

        lhs.close();
        rhs.close();
    }
}
