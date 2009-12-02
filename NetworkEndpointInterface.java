// This object interfaces with the XBee radios.


public class NetworkEndpointInterface {
    private XBeeDispatcher urgent;
    private XBeeDispatcher mundane;

    NetworkEndpointInterface() throws XBeeNotFoundException {
        urgent  = new XBeeDispatcher();
        mundane = new XBeeDispatcher();
    }
}
