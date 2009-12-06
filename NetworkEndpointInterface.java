// This object interfaces with the XBee radios.

public class NetworkEndpointInterface {
    private XBeeDispatcher urgent;
    private XBeeDispatcher mundane;
    String name;
    NCIClient NCI;

    private static class NCIClient extends LineOrientedClient {
        NetworkEndpointInterface NEI;

        NCIClient(String _sh, int _p, NetworkEndpointInterface _e) {
            super(_sh, _p);
            NEI = _e;
        }
    }

    NetworkEndpointInterface(String _n, String host, int port) {
        name    = _n;
        NCI     = new NCIClient( host, port, this );
        urgent  = XBeeDispatcher.configuredDispatcher(name + ":urgent",  true);
        mundane = XBeeDispatcher.configuredDispatcher(name + ":mundane", true);
    }
}
