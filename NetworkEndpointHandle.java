import gnu.io.*;
import java.util.*;

public class NetworkEndpointHandle {
    private static Queue<CommPortIdentifier> ports;
    private XBeeHandle xh;

    private static void populatePortNames() {
        if( ports != null )
            return;

        ports = new ArrayDeque<CommPortIdentifier>();

        Enumeration portIdentifiers = CommPortIdentifier.getPortIdentifiers();
        while (portIdentifiers.hasMoreElements()) {
            CommPortIdentifier pid = (CommPortIdentifier) portIdentifiers.nextElement();

            if(pid.getPortType() == CommPortIdentifier.PORT_SERIAL )

                ports.offer(pid); // returns false if it's full, but who cares, we're expecting like 8 things tops
        }
    }

    private static CommPortIdentifier locateAndConfigure() {
        CommPortIdentifier pid;

        populatePortNames();

        while( (pid = ports.poll()) != null )
            System.out.println("kk: " + pid.getName());

        return null;
    }

    NetworkEndpointHandle() {
        CommPortIdentifier meh = locateAndConfigure();
        // XBeeConfig = new XBeeConfig();
        // xh = new XBeeHandle();
    }

}
