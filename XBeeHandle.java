import gnu.io.*;
import java.io.*;
import java.util.*;
import java.nio.*;

public class XBeeHandle {
    private InputStream  in;
    private OutputStream out;
    private CommPort commPort;

    protected void finalize() throws Throwable { this.close(); }
    public    void close() { commPort.close(); }

    XBeeConfig(CommPortIdentifier portIdentifier, int speed, boolean _debug) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        debug = _debug;
        commPort = portIdentifier.open(this.getClass().getName(), 2000);

        if ( commPort instanceof SerialPort ) {
            SerialPort serialPort = (SerialPort) commPort;
            serialPort.setSerialPortParams(speed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

            in  = serialPort.getInputStream();
            out = serialPort.getOutputStream();

        } else {
            throw new UnsupportedCommOperationException("\"" + portIdentifier.getName() + "\" is probably not a serial port");
        }
    }

    public static XBeeConfig newFromPortName(String portName, int speed, boolean debug) throws NoSuchPortException, PortInUseException, UnsupportedCommOperationException, IOException {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);

        return new XBeeConfig(portIdentifier, speed, debug);
    }

    public byte[] send_packet(XBeePacket p) throws IOException {
    }
}
