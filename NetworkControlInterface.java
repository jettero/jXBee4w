// This object interfaces with NEI to assign channels and keep track of vehicles

import java.util.*;

public class NetworkControlInterface extends LineOrientedServer {
    public static final int UNKNOWN_COMMAND_ERROR = 400;
    public static final int USAGE_ERROR           = 401;
    public static final int FORMAT_ERROR          = 402;

    public static final int HOST_ADDRESS       = 300;
    public static final int WALL               = 301;

    public static final int SUCCESS            = 200;
    public static final int CHANNEL_RESPONSE   = 210;
    public static final int CHANNEL_ASSIGNMENT = CHANNEL_RESPONSE;

    private Hashtable <String, Address64> mundane = new Hashtable <String, Address64>();
    private Hashtable <String, Address64> urgent  = new Hashtable <String, Address64>();
    private Hashtable <Address64, String> reverse = new Hashtable <Address64, String>();

    private String clientIP;

    NetworkControlInterface(int port) { super(port); }

    public CommandResponse handleCommand(String cmd) {
        String[] tokens = cmd.trim().split("\\s+");

        if( tokens.length < 1 )
            return new CommandResponse(UNKNOWN_COMMAND_ERROR, "unknown command, ... next time, say something");

        if( tokens[0].equals("register") )
            return handleRegistration(tokens);

        if( tokens[0].equals("wall") )
            return wall(tokens);

        if( tokens[0].equals("list") ) {
            listHosts();
            return new CommandResponse(SUCCESS, "listing complete");
        }

        if( tokens[0].equals("quit") || tokens[0].equals("exit") )
            return new CommandResponse(QUIT, "bye");

        return new CommandResponse(UNKNOWN_COMMAND_ERROR, "unknown command");
    }

    public CommandResponse wall(String _wall[]) {
        if( _wall.length < 2 )
            return new CommandResponse(USAGE_ERROR, "wall <message>");

        String wall = _wall[1];
        for(int i=2; i<_wall.length; i++)
            wall += " " + _wall[i];

        sendAll( new CommandResponse(WALL, wall) );

        return new CommandResponse(SUCCESS, "wall sent");
    }

    public void learnIP(String IP) {
        clientIP = IP;
    }

    public CommandResponse greeting() {
        return new CommandResponse(GREETINGS, "Hello, this is the NCI Server");
    }

    public CommandResponse buildHostAddress(String h, Address64 m, Address64 u) {
        return new CommandResponse(HOST_ADDRESS, String.format("%s %s %s", h, m, u));
    }

    public void listHosts() {
        Iterator i = mundane.keySet().iterator();

        // XXX: this should probably only list hosts in the same area

        while( i.hasNext() ) {
            String s = (String) i.next();

            send( buildHostAddress(s, mundane.get(s), urgent.get(s)) );
        }
    }

    public int[] channelAssignments(String host) {
        int ret[] = { 0x0c, 0x17 };

        // XXX: later on, presumably, this interface would return choices besides 0c and 17

        return ret;
    }

    public CommandResponse handleRegistration(String cmd[]) {
        if( cmd.length == 4 ) {
            Address64 m, u;

            try { m = new Address64(cmd[2]); } catch(Address64Exception e) {
                return new CommandResponse(FORMAT_ERROR, "argument #2 seems invalid: " + e.getMessage());
            }

            try { u = new Address64(cmd[3]); } catch(Address64Exception e) {
                return new CommandResponse(FORMAT_ERROR, "argument #3 seems invalid: " + e.getMessage());
            }

            // XXX: there should probably be some kind of check here on the
            // name... is it valid?  is it already taken?  OAUTH tokens? ssl
            // certificates.

            mundane.put(cmd[1], m);
             urgent.put(cmd[1], u);
            reverse.put(m, cmd[1]);
            reverse.put(u, "!" + cmd[1]);

            sendAll( buildHostAddress(cmd[1], m, u) ); // tell everyone about this registration

            int ch[] = channelAssignments(cmd[1]);

            // XXX: These channel assignment messages can be sent at any time.

            return new CommandResponse(CHANNEL_RESPONSE,
                String.format("registration success.  channels: %02x %02x",
                    ch[0], ch[1]));
        }

        return new CommandResponse(USAGE_ERROR, "usage: register <name> <mundane-address> <urgent-address>");
    }

}
