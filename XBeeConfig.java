import gnu.io.*;
import java.io.*;

public class XBeeConfig {
    InputStream  in;
    OutputStream out;

    private void connect(String portName, int speed) {
        try {
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);

        } catch(NoSuchPortException e) {
            System.out.println("Error: " + e.getMessage());
        }


        if ( portIdentifier.isCurrentlyOwned() ) {
            System.out.println("Error: Port is currently in use");

        } else {
            CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);
            
            if ( commPort instanceof SerialPort ) {
                SerialPort serialPort = (SerialPort) commPort;
                serialPort.setSerialPortParams(speed, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                
                in  = serialPort.getInputStream();
                out = serialPort.getOutputStream();

            } else {
                System.out.println("Error: Only serial ports are handled by this example.");
            }
        }     
    }

    public void config(String portName, int speed, String configs[]) {
        Thread.sleep(1);
        this.out.write("+++".getBytes());
    }
}
