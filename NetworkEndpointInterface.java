// This object interfaces with the XBee radios.

import java.util.regex.*;
import java.util.*;
import java.io.*;

public class NetworkEndpointInterface implements Runnable, MessageRecvEvent {
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

                System.out.printf("NCI(%d) channel assignment: %s%n", s.code, s.msg);

                if( m.find() )
                    NEI.assignChannels(m.group(1), m.group(2));

            } else if( s.code == NetworkControlInterface.HOST_ADDRESS ) {
                Matcher m = Pattern.compile("^(\\S+)\\s+([0-9a-fA-F:]+)\\s+([0-9a-fA-F:]+)$").matcher(s.msg);

                System.out.printf("NCI(%d) host address: %s%n", s.code, s.msg);

                if( m.find() )
                    NEI.learnHostAddress(m.group(1), m.group(2), m.group(3));

                else
                    System.out.printf("Internal Error processing NCI(%d): %s%n", s.code, s.msg);

            } else if( s.code == NetworkControlInterface.QUIT ) {
                System.out.printf("NCI(%d): %s%n", s.code, s.msg);
                System.exit(0); // we asked for it...

            } else if( s.code >= 400 ) {
                System.out.printf("NCI ERROR(%d): %s%n", s.code, s.msg);

            } else {
                System.out.printf("NCI(%d): %s%n", s.code, s.msg);
            }
        }
    }
    // }}}

    private XBeeDispatcher urgent;
    private XBeeDispatcher mundane;
    private Hashtable <String,Address64> hostmap_m = new Hashtable <String,Address64>();
    private Hashtable <String,Address64> hostmap_u = new Hashtable <String,Address64>();
    private Hashtable <Address64,String> reverse   = new Hashtable <Address64,String>();

    boolean resolvedUrgent;
    Address64 resolved;
    String    reversed;

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

    public void learnHostAddress(String h, String _m, String _u) {
        Address64 m,u;

        try {
            m = new Address64(_m);
            u = new Address64(_u);

        } catch(Address64Exception e) {
            System.out.printf("ERROR Learning Host: %s %s %s -- %s%n",
                h, _m, _u, e.getMessage()
            );
            return;
        }

        // System.out.printf("Host: %s %s %s%n", h, m, u);

        hostmap_m.put(h, m);
        hostmap_u.put(h, u);
        reverse.put(m, h);
        reverse.put(u, "!" + h);
    }

    private boolean resolv(Address64 r) {
        if( reverse.containsKey(r) ) {
            reversed = (String) reverse.get(r);

            return true;
        }

        return false;
    }

    private boolean resolv(String s) {
        Hashtable m;

        if( s.equals(".") )
            if( resolved != null )
                return true;

        if( s.equals("r") )
            if( reversed != null )
                return resolv(reversed);

        if( s.startsWith("!") ) {
            s = s.substring(1);
            m = hostmap_u;
            resolvedUrgent = true;

        } else {
            m = hostmap_m;
            resolvedUrgent = false;
        }

        if( m.containsKey(s) ) {
            resolved = (Address64) m.get(s);

            return true;
        }

        resolved = null;

        return false;
    }

    private String[] subarr(String a[], int i) { return subarr(a, i, a.length-1); }
    private String[] subarr(String a[], int i, int j) {
        if( i<0 || i > a.length-1 ) return new String[0];
        if( j<i || j > a.length-1 ) return new String[0];

        int k = (j-i) + 1;

        String ret[] = new String[k];
        for(int w=0; w<k; w++)
            ret[w] = a[w+i];

        return ret;
    }

    private String cat(String s[]) {
        if( s.length < 1 )
            return "";

        String ret = s[0];
        for( String _s : subarr(s, 1) )
                ret += " " + _s;

        return ret;
    }

    public void recvMessage(XBeeDispatcher handle, Address64 src, byte message[]) {
        String prefix = String.format("Message from %s", resolv(src) ? reversed : src);
        String status = handle == urgent ? "!" : " ";

        System.out.printf("%s: %s [%s]%n", prefix, new String(message), status);
    }

    public void run() {
        String line;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        try {
            while( (line = in.readLine()) != null ) {
                if( (line = line.trim()).length() > 0 ) {

                    String[] tokens = line.split("\\s+");

                    if( tokens[0].startsWith("/") ) {
                        NCI.send(line.substring(1));

                    } else {
                        if( resolv(tokens[0]) )

                            (resolvedUrgent ? urgent : mundane)

                                .send(resolved, cat(subarr(tokens, 1)));

                        else
                            System.out.println("host " + tokens[0] + " not found. :(");
                    }

                }
            }

        } catch(IOException e) {
            System.err.println("IOException using stdin, weird: " + e.getMessage());
            System.exit(1);
        }
    }

    NetworkEndpointInterface(String _n, String host, int port) {
        name    = _n;
        urgent  = XBeeDispatcher.configuredDispatcher(name + ":urgent",  true);
        mundane = XBeeDispatcher.configuredDispatcher(name + ":mundane", true);

        Address64 u = urgent.addr();
        Address64 m = mundane.addr();

         urgent.registerMessageReceiver(this);
        mundane.registerMessageReceiver(this);

        System.out.println("\nLogging into NCI...");

        NCI = new NCIClient( host, port, this ); // connect
        NCI.register(name, u, m); // register
        NCI.send("list"); // learn all known hosts
    }
}
