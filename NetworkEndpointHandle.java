import gnu.io.*;
import java.util.*;

public class NetworkEndpointHandle extends XBeeHandle {
    private static List<CommPortIdentifier> ports;

    private static void populatePortNames() {
        if( ports != null )
            return;

        ports = new ArrayList<CommPortIdentifier>();

        Enumeration portIdentifiers = CommPortIdentifier.getPortIdentifiers();
        while (portIdentifiers.hasMoreElements()) {
            CommPortIdentifier pid = (CommPortIdentifier) portIdentifiers.nextElement();

            if(pid.getPortType() == CommPortIdentifier.PORT_SERIAL )
                p.add(pid);
        }
    }

    private static void locateAndConfigure() {
        populatePortNames();

        while( !ports.isEmpty() ) {
    }

}
