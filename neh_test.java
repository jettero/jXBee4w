
public class neh_test {
    public static void main(String args[]) {
        NetworkEndpointHandle.debug = true;
        NetworkEndpointHandle h = NetworkEndpointHandle.configuredEndpoint();

        System.out.println("Cool! All configured! Address: " + h.addr().toText());

        h.close();
    }
}
