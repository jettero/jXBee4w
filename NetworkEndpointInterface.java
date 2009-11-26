// This object interfaces with the XBee radios.


public class NetworkEndpointInterface {
    private XBeeHandle urgent;
    private XBeeHandle mundane;

    NetworkEndpointInterface() throws XBeeNotFoundException {
        urgent  = new NetworkEndpointHandle();
        mundane = new NetworkEndpointHandle();
    }
}
