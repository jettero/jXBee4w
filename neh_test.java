
public class neh_test {
    public static void main(String args[]) {
        String debug = System.getenv("DEBUG");

        if( debug != null )
            if( !debug.isEmpty() )
                if( !debug.equals("0") )
                    NetworkEndpointHandle.debug = true;

        NetworkEndpointHandle h = NetworkEndpointHandle.configuredEndpoint();

        System.out.println("Cool! All configured! Address: " + h.addr().toText());
        System.out.println("Hardware Version: " + h.hardwareVersion());
        System.out.println("Firmware Version: " + h.firmwareVersion());

        h.close();
    }
}
