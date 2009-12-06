// This object interfaces with the XBee radios.


public class NetworkEndpointInterface {
    private XBeeDispatcher urgent;
    private XBeeDispatcher mundane;

    private static class NCIClient extends LineOrientedClient {
        NetworkEndpointInterface NEI;
        Socket server;
        PrintWriter out;

        NCIClient(String _sh, int _p, NetworkEndpointInterface _n) throws UnknownHostException, IOException {
            NEI = _n;

            try {
                server            = new Socket(_sh, _p);
                out               = new PrintWriter(server.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()));

            } catch (UnknownHostException e) {
                System.err.println("ERROR connecting to NCI: " + e.getMessage());
                System.exit(1);

            } catch (IOException e) {
                System.err.println("ERROR building streams from connection to NCI: " + e.getMessage());
                System.exit(1);
            }   

        }
    }



    NetworkEndpointInterface(String name) {
        NCIClient = new NCIClient( "ranger", 4000, this );
        try {
            urgent  = new XBeeDispatcher();
            mundane = new XBeeDispatcher();

        } catch(XBeeNotFoundException e) {
        }
    }
}
