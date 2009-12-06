// This object interfaces with NEI to assign channels and keep track of vehicles

import java.util.*;

public class NetworkControlInterface extends LineOrientedServer {
    public static final int UNKNOWN_COMMAND_ERROR = 400;
    public static final int USAGE_ERROR           = 401;
    public static final int FORMAT_ERROR          = 402;

    public static final int SUCCESS          = 200;
    public static final int CHANNEL_RESPONSE = 210;

    private Hashtable <String, Address64> mundane = new Hashtable <String, Address64>();
    private Hashtable <String, Address64> urgent  = new Hashtable <String, Address64>();
    private Hashtable <Address64, String> reverse = new Hashtable <Address64, String>();

    private String clientIP;

    NetworkControlInterface(int port) { super(port); }

    public CommandResponse handleCommand(String cmd) {
        String[] tokens = cmd.trim().split("\\s+");

        if( tokens[0].equals("register") )
            return handleRegistration(tokens);

        if( tokens[0].equals("quit") || tokens[0].equals("exit") )
            return new CommandResponse(QUIT, "bye");

        return new CommandResponse(UNKNOWN_COMMAND_ERROR, "unknown command");
    }

    public void learnIP(String IP) {
        clientIP = IP;
    }

    public CommandResponse greeting() {
        return new CommandResponse(GREETINGS, "Hello, this is the NCI Server");
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
            reverse.put(u, cmd[1]);
            reverse.put(m, cmd[1]);

            // XXX: later on, presumably, this interface would return choices besides 0c and 17

            return new CommandResponse(CHANNEL_RESPONSE, "registration success.  channels: 0c 17");
        }

        return new CommandResponse(USAGE_ERROR, "usage: register <name> <mundane-address> <urgent-address>");
    }

}
