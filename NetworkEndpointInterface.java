// This object interfaces with the XBee radios.

public class NetworkEndpointInterface {
    XBeeHandle urgent;
    XBeeHandle mundane;

    NetworkEndpointInterface() {
        XBeeHandle.findRadio();
    }
}
