
public class neh_test {
    public static void main(String args[]) {
        NetworkEndpointHandle.debug = false;
        NetworkEndpointHandle h = NetworkEndpointHandle.configuredEndpoint();

        h.close();
    }
}
