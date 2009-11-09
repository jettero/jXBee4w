
// this is from http://en.wikibooks.org/wiki/Serial_Programming/Serial_Java

import javax.comm.*;
import java.util.*;
...
//
// Platform specific port name, here a Unix name
//
// NOTE: On at least one Unix JavaComm implementation JavaComm 
//       enumerates the ports as "COM1" ... "COMx", too, and not
//       by their Unix device names "/dev/tty...". 
//       Yet another good reason to not hard-code the wanted
//       port, but instead make it user configurable.
//
String wantedPortName = "/dev/ttya";
 
//
// Get an enumeration of all ports known to JavaComm
//
Enumeration portIdentifiers = CommPortIdentifier.getPortIdentifiers();
//
// Check each port identifier if 
//   (a) it indicates a serial (not a parallel) port, and
//   (b) matches the desired name.
//
CommPortIdentifier portId = null;  // will be set if port found
while (portIdentifiers.hasMoreElements())
{
    CommPortIdentifier pid = (CommPortIdentifier) portIdentifiers.nextElement();
    if(pid.getPortType() == CommPortIdentifier.PORT_SERIAL &&
       pid.getName().equals(wantedPortName)) 
    {
        portId = pid;
        break;
    }
}
if(portId == null)
{
    System.err.println("Could not find serial port " + wantedPortName);
    System.exit(1);
}
//
// Use port identifier for acquiring the port
//
...

