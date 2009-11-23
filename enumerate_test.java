
import gnu.io.*;
import java.util.*;

public class enumerate_test {
    public static void configure(CommPortIdentifier pid, String portName) {
        System.out.printf("found serial port: %s aka %s%n", pid, portName);
    }

    public static void main(String[] args) {
        enumerate_test r = new enumerate_test();

        Enumeration portIdentifiers = CommPortIdentifier.getPortIdentifiers();
        while (portIdentifiers.hasMoreElements()) {
            CommPortIdentifier pid = (CommPortIdentifier) portIdentifiers.nextElement();

            if(pid.getPortType() == CommPortIdentifier.PORT_SERIAL )
                configure(pid, pid.getName());
        }

    }
}
