
public class neh_test {
    public static void main(String args[]) {
        NetworkEndpointHandle.debug = true;
        NetworkEndpointHandle h = NetworkEndpointHandle.configuredEndpoint();

        System.out.println("Cool! All configured! Address: " + h.addr().toText());
        System.out.println("Hardware Version: " + h.hardwareVersion());
        System.out.println("Firmware Version: " + h.firmwareVersion());

        h.close();
    }
}
