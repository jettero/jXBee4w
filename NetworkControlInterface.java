// This object interfaces with NEI to assign channels and keep track of vehicles

import java.lang.*;
import java.net.*;
import java.io.*;

public class NetworkControlInterface extends LineOrientedServer {
    public static final int UNKNOWN_COMMAND_ERROR = 400;
    public static final int USAGE_ERROR           = 401;
    public static final int FORMAT_ERROR          = 402;

    HashTable <String, Address64> mundane = new HashTable <String, Address64>();
    HashTable <String, Address64> urgent  = new HashTable <String, Address64>();
    HashTable <Address64, String> reverse = new HashTable <Address64, String>();

    public CommandResponse handleCommand(String cmd) {
        String[] tokens = cmd.trim().split("\\s+");

        if( tokens[0].equals("register") )
            return handleRegistration(tokens);

        return new CommandResponse(UNKNOWN_COMMAND_ERROR, "unknown command");
    }

    public CommandResponse handleRegistration(String cmd[]) {
        if( cmd.length == 4 ) {
            Address64 m, u;
            try {
                m = new Address64(cmd[2]);
                u = new Address64(cmd[3]);

            } catch(Address64Exception e) {
                return new CommandResponse(FORMAT_ERROR, e.getMessage());
            }

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
