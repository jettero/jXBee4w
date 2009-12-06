// This object interfaces with the XBee radios.


public class NetworkEndpointInterface {
    private XBeeDispatcher urgent;
    private XBeeDispatcher mundane;

    NetworkEndpointInterface(String name) throws XBeeNotFoundException {
        urgent  = new XBeeDispatcher();
        mundane = new XBeeDispatcher();
    }
}
