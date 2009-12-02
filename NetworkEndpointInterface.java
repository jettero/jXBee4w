// This object interfaces with the XBee radios.


public class NetworkEndpointInterface {
    private NetworkEndpointHandle urgent;
    private NetworkEndpointHandle mundane;

    NetworkEndpointInterface() throws XBeeNotFoundException {
        urgent  = new NetworkEndpointHandle();
        mundane = new NetworkEndpointHandle();
    }
}
