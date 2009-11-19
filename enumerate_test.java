
import gnu.io.*;
import java.util.*;

public class enumerate_test {
    public static void main(String[] args) {

        Enumeration portIdentifiers = CommPortIdentifier.getPortIdentifiers();
        while (portIdentifiers.hasMoreElements()) {
            CommPortIdentifier pid = (CommPortIdentifier) portIdentifiers.nextElement();

            if(pid.getPortType() == CommPortIdentifier.PORT_SERIAL )
                System.out.println("found serial port: " + pid + " aka " + pid.getName() );
        }

    }
}
