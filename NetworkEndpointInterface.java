// This object interfaces with the XBee radios.

import java.util.regex.*;

public class NetworkEndpointInterface {
    // private static class NCIClient extends LineOrientedClient {{{
    private static class NCIClient extends LineOrientedClient {
        NetworkEndpointInterface NEI;

        NCIClient(String _sh, int _p, NetworkEndpointInterface _e) {
            super(_sh, _p);
            NEI = _e;
        }

        public void register(String name, Address64 mundane, Address64 urgent) {
            String command = String.format("register %s %s %s", 
                name, urgent.toText(), mundane.toText() );

            send(command);
        }

        public void handleServerResponse(ServerResponse s) {
            if( s.code == NetworkControlInterface.CHANNEL_ASSIGNMENT ) {
                Matcher m = Pattern.compile("channels:\\s+([0-9a-fA-F]+)\\s+([0-9a-fA-F]+)$").matcher(s.msg);
                if( m.find() )
                    NEI.assignChannels(m.group(1), m.group(2));

            } else if( s.code >= 400 ) {
                System.out.printf("NCI ERROR(%d): %s%n", s.code, s.msg);

            } else {
                System.out.printf("NCI: %s%n", s.msg);
            }
        }
    }
    // }}}

    private XBeeDispatcher urgent;
    private XBeeDispatcher mundane;

    String name;
    NCIClient NCI;

    public void assignChannels(String _m, String _u) {
        int m =  Integer.valueOf(_m, 16).intValue();
        int u =  Integer.valueOf(_u, 16).intValue();

        System.out.printf("Setting channels on XBee Radios: mundane=%02x urgent=%02x%n", m, u);

        try {
             urgent.setChannel(m);
            mundane.setChannel(u);

        } catch(IllegalArgumentException e) {
            System.err.println("Problem setting channels: " + e.getMessage());
        }

    }

    NetworkEndpointInterface(String _n, String host, int port) {
        name    = _n;
        urgent  = XBeeDispatcher.configuredDispatcher(name + ":urgent",  true);
        mundane = XBeeDispatcher.configuredDispatcher(name + ":mundane", true);

        Address64 u = urgent.addr();
        Address64 m = mundane.addr();

        System.out.println("\nLogging into NCI...");

        NCI = new NCIClient( host, port, this );
        NCI.register(name, u, m);
    }
}
