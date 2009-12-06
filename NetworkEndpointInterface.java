// This object interfaces with the XBee radios.


public class NetworkEndpointInterface {
    private XBeeDispatcher urgent;
    private XBeeDispatcher mundane;

    private Hashtable <String, Address64> knownNeighbors   = new Hashtable <String, Address64>();
    private Hashtable <Address64, String> reverseNeighbors = new Hashtable <Address64, String>();
    private HashSet   <Address64> unknownNeighbors = new HashSet <Address64> ();

    private static class NCIHandler {
    }

    NetworkEndpointInterface(String name) throws XBeeNotFoundException {
        urgent  = new XBeeDispatcher();
        mundane = new XBeeDispatcher();
    }
}
